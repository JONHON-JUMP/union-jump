package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.module.rm.recipechange.controller.mpm.vo.RecipeChangeNoticeReqVO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeNoticeDO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeOperationLogDO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeStatusLogDO;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeNoticeMapper;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeOperationLogMapper;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeStatusLogMapper;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeNoticeStatusEnum;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeOperationResultEnum;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeOperationTypeEnum;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeTriggerTypeEnum;
import cn.jonhon.jump.module.rm.recipechange.mq.event.RecipeChangeNoticeReceivedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 工艺变更通知接收服务实现
 *
 * 在一个事务内完成通知幂等落库、初始状态流水和 MPM 调用流水记录，随后发布分发事件
 */
@Service
public class RecipeChangeNoticeReceiveServiceImpl implements RecipeChangeNoticeReceiveService {

    /** MPM 系统标识，用于记录接收链路创建数据的创建者和操作人 */
    private static final String MPM = "MPM";

    /** 工艺变更通知主表数据访问对象 */
    @Resource
    private RecipeChangeNoticeMapper recipeChangeNoticeMapper;
    /** 工艺变更操作流水表数据访问对象 */
    @Resource
    private RecipeChangeOperationLogMapper recipeChangeOperationLogMapper;
    /** 工艺变更状态流水表数据访问对象 */
    @Resource
    private RecipeChangeStatusLogMapper recipeChangeStatusLogMapper;
    /** JSON 序列化与空 JSON 对象创建工具 */
    @Resource
    private ObjectMapper objectMapper;
    /** Spring 应用事件发布器，用于通知后续分发模块 */
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * 校验接收工艺变更通知所必需的业务字段
     *
     * @param reqVO MPM 推送的工艺变更通知内容
     * @return 校验失败信息；校验通过时返回 {@code null}
     */
    @Override
    public String validateRequiredFields(RecipeChangeNoticeReqVO reqVO) {
        if (reqVO == null) {
            return "请求参数不能为空";
        }
        // notifyId 是幂等键和全链路追踪键，必须先于后续业务处理校验
        if (!StringUtils.hasText(reqVO.getNotifyId())) {
            return "notifyId不能为空";
        }
        // workshopCode 决定后续消息路由的目标队列，缺失时不能继续接收
        if (!StringUtils.hasText(reqVO.getWorkshopCode())) {
            return "workshopCode不能为空";
        }
        return null;
    }

    /**
     * 接收 MPM 通知并创建初始数据
     *
     * 先按 {@code notifyId} 查询以避免重复处理；并发重复请求则由数据库唯一约束兜底
     * 只有首次成功插入后才会创建日志和发布分发事件
     *
     * @param reqVO MPM 推送的工艺变更通知内容
     * @return 已接收通知的唯一标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String receiveRecipeChangeNotice(RecipeChangeNoticeReqVO reqVO) {
        // 第一层幂等控制：已有记录代表该通知已经被接收，直接返回且不再触发分发
        RecipeChangeNoticeDO existedNotice = recipeChangeNoticeMapper.selectByNotifyId(reqVO.getNotifyId());
        if (existedNotice != null) {
            return existedNotice.getNotifyId();
        }

        // 构造通知主记录，并设置接收阶段的初始状态和重试计数
        RecipeChangeNoticeDO notice = new RecipeChangeNoticeDO();
        notice.setNotifyId(reqVO.getNotifyId());
        notice.setWorkshopCode(reqVO.getWorkshopCode());
        notice.setCreator(MPM);
        notice.setChangeContent(getChangeContent(reqVO));
        notice.setStatus(RecipeChangeNoticeStatusEnum.RECEIVED_SUCCESS.getStatus());
        notice.setRetryCount(0);
        notice.setMaxRetry(3);
        // 第二层幂等控制：依赖数据库 notify_id 唯一约束处理并发重复请求
        if (recipeChangeNoticeMapper.insertIgnoreDuplicate(notice) == 0) {
            return reqVO.getNotifyId();
        }

        // 获取数据库生成的主键，供两类初始流水和后续分发事件关联使用
        RecipeChangeNoticeDO createdNotice = recipeChangeNoticeMapper.selectByNotifyId(reqVO.getNotifyId());
        LocalDateTime now = LocalDateTime.now();
        // 记录从无状态到“接收成功”的首次状态流转，保证通知状态可追溯
        recipeChangeStatusLogMapper.insertStatusLog(buildStatusLog(createdNotice, now));
        // 保存 MPM 调用请求及成功响应快照，便于排查接口交互问题
        recipeChangeOperationLogMapper.insertOperationLog(buildOperationLog(createdNotice, reqVO, now));
        // 在事务提交后由后续监听器执行 RabbitMQ 分发，避免未提交数据被分发流程读取
        applicationEventPublisher.publishEvent(new RecipeChangeNoticeReceivedEvent(createdNotice.getId()));
        return createdNotice.getNotifyId();
    }

    /**
     * 获取待存储的工艺变更内容
     *
     * 按需求不校验 {@code changeContent} 是否为空；为空时保存为空 JSON 对象，确保 JSONB 字段可用
     *
     * @param reqVO MPM 推送的请求参数
     * @return 原始工艺变更内容或空 JSON 对象
     */
    private JsonNode getChangeContent(RecipeChangeNoticeReqVO reqVO) {
        // 未传变更内容时以空 JSON 对象落库，保持 JSONB 字段格式一致
        return reqVO.getChangeContent() != null ? reqVO.getChangeContent() : objectMapper.createObjectNode();
    }

    /**
     * 创建首次接收成功的状态流转记录
     *
     * @param notice 已落库的工艺变更通知主记录
     * @param changeTime 状态变更时间
     * @return 待写入的状态流水记录
     */
    private RecipeChangeStatusLogDO buildStatusLog(RecipeChangeNoticeDO notice, LocalDateTime changeTime) {
        // 首次接收没有前置状态，因此仅记录流转后的“接收成功”状态
        RecipeChangeStatusLogDO statusLog = new RecipeChangeStatusLogDO();
        statusLog.setNoticeId(notice.getId());
        statusLog.setNotifyId(notice.getNotifyId());
        statusLog.setCreator(MPM);
        statusLog.setToStatus(RecipeChangeNoticeStatusEnum.RECEIVED_SUCCESS.getStatus());
        statusLog.setChangeTime(changeTime);
        statusLog.setTriggerType(RecipeChangeTriggerTypeEnum.SYSTEM.getType());
        return statusLog;
    }

    /**
     * 创建 MPM 调用成功的操作流水记录
     *
     * 请求和严格约定的成功响应均以 JSON 快照保存，便于后续追溯
     *
     * @param notice 已落库的工艺变更通知主记录
     * @param reqVO MPM 推送的原始请求参数
     * @param operationTime 操作发生时间
     * @return 待写入的操作流水记录
     */
    private RecipeChangeOperationLogDO buildOperationLog(RecipeChangeNoticeDO notice, RecipeChangeNoticeReqVO reqVO, LocalDateTime operationTime) {
        // 操作流水保存请求快照及返回给 MPM 的成功响应，用于全链路审计
        RecipeChangeOperationLogDO operationLog = new RecipeChangeOperationLogDO();
        operationLog.setNoticeId(notice.getId());
        operationLog.setNotifyId(notice.getNotifyId());
        operationLog.setWorkshopCode(notice.getWorkshopCode());
        operationLog.setOperationType(RecipeChangeOperationTypeEnum.MPM_CALL.getType());
        operationLog.setOperationTime(operationTime);
        operationLog.setOperator(MPM);
        operationLog.setCreator(MPM);
        operationLog.setOperationResult(RecipeChangeOperationResultEnum.SUCCESS.getResult());
        operationLog.setRequestParams(objectMapper.valueToTree(reqVO));
        operationLog.setResponseParams(objectMapper.createObjectNode().put("code", 200).put("msg", "success").put("data", notice.getNotifyId()));
        return operationLog;
    }

}
