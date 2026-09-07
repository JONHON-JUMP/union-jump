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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        try {
            parseWorkshopCodes(reqVO.getWorkshopCode());
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
        return null;
    }

    /**
     * 接收 MPM 通知并创建初始数据
     *
     * 将逗号分隔的目标车间拆分为独立通知；每个 {@code notifyId + workshopCode} 组合独立幂等。
     * 只有首次成功插入的车间记录才会创建日志和发布分发事件。
     *
     * @param reqVO MPM 推送的工艺变更通知内容
     * @return 已接收通知的唯一标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String receiveRecipeChangeNotice(RecipeChangeNoticeReqVO reqVO) {
        JsonNode changeContent = getChangeContent(reqVO);
        for (String workshopCode : parseWorkshopCodes(reqVO.getWorkshopCode())) {
            RecipeChangeNoticeDO notice = buildNotice(reqVO.getNotifyId(), workshopCode, changeContent);
            // 数据库组合唯一约束保障并发请求不会为同一通知和车间重复创建记录。
            if (recipeChangeNoticeMapper.insertIgnoreDuplicate(notice) == 0) {
                continue;
            }
            // 获取数据库生成的主键，供两类初始流水和后续分发事件关联使用。
            RecipeChangeNoticeDO createdNotice = recipeChangeNoticeMapper.selectByNotifyIdAndWorkshopCode(reqVO.getNotifyId(), workshopCode);
            if (createdNotice == null) {
                throw new IllegalStateException("工艺变更通知创建后未找到对应车间记录");
            }
            LocalDateTime now = LocalDateTime.now();
            // 每个车间独立记录接收状态和 MPM 调用流水，后续状态、重试和人工处理也彼此隔离。
            recipeChangeStatusLogMapper.insertStatusLog(buildStatusLog(createdNotice, now));
            recipeChangeOperationLogMapper.insertOperationLog(buildOperationLog(createdNotice, reqVO, now));
            // 在事务提交后由后续监听器按车间发送 RabbitMQ 消息，避免 MES 收到未提交数据。
            applicationEventPublisher.publishEvent(new RecipeChangeNoticeReceivedEvent(createdNotice.getId()));
        }
        return reqVO.getNotifyId();
    }

    /**
     * 将 MPM 传入的逗号分隔车间编码规范化为有序且不重复的单车间集合。
     */
    private List<String> parseWorkshopCodes(String workshopCode) {
        Set<String> workshopCodes = new LinkedHashSet<>();
        for (String value : workshopCode.split(",", -1)) {
            String normalizedWorkshopCode = value.trim();
            if (!StringUtils.hasText(normalizedWorkshopCode)) {
                throw new IllegalArgumentException("workshopCode中不能包含空车间编码");
            }
            workshopCodes.add(normalizedWorkshopCode);
        }
        return new ArrayList<>(workshopCodes);
    }

    /**
     * 构造单个目标车间的通知主记录。
     */
    private RecipeChangeNoticeDO buildNotice(String notifyId, String workshopCode, JsonNode changeContent) {
        RecipeChangeNoticeDO notice = new RecipeChangeNoticeDO();
        notice.setNotifyId(notifyId);
        notice.setWorkshopCode(workshopCode);
        notice.setCreator(MPM);
        notice.setChangeContent(changeContent);
        notice.setStatus(RecipeChangeNoticeStatusEnum.RECEIVED_SUCCESS.getStatus());
        notice.setRetryCount(0);
        notice.setMaxRetry(3);
        return notice;
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
