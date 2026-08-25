package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.framework.security.core.LoginUser;
import cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticeRetryRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticeRetryResultItemRespVO;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工艺变更通知重试管理服务实现
 */
@Service
public class RecipeChangeRetryServiceImpl implements RecipeChangeRetryService {

    private static final String JUMP = "JUMP";

    @Resource
    private RecipeChangeNoticeMapper recipeChangeNoticeMapper;
    @Resource
    private RecipeChangeNoticeDispatchService recipeChangeNoticeDispatchService;
    @Resource
    private RecipeChangeOperationLogMapper recipeChangeOperationLogMapper;
    @Resource
    private RecipeChangeStatusLogMapper recipeChangeStatusLogMapper;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 扫描发送失败通知并执行自动重试或转待人工处理
     */
    @Override
    public void executeScheduledRetry() {
        for (RecipeChangeNoticeDO recipeChangeNotice : recipeChangeNoticeMapper.selectSendFailedNotices()) {
            if (recipeChangeNotice.getRetryCount() >= recipeChangeNotice.getMaxRetry()) {
                markPendingManual(recipeChangeNotice);
                continue;
            }
            recipeChangeNoticeDispatchService.dispatchRecipeChangeNotice(recipeChangeNotice.getId(), RecipeChangeOperationTypeEnum.SCHEDULED_RETRY.getType(), RecipeChangeTriggerTypeEnum.SYSTEM.getType(), JUMP, true);
        }
    }

    /**
     * 从当前认证上下文获取人工处理操作人，并统一处理旧 Token 或异常上下文的空值。
     *
     * @return 当前登录管理员 username；未获取到时为 UNKNOWN
     */
    @Override
    public String getLoginUsername() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || loginUser.getInfo() == null) {
            return "UNKNOWN";
        }
        String username = loginUser.getInfo().get(LoginUser.INFO_KEY_USERNAME);
        return StringUtils.hasText(username) ? username : "UNKNOWN";
    }

    /**
     * 对发送失败、车间处理失败或待人工处理的通知执行人工重发
     */
    @Override
    public RecipeChangeNoticeRetryRespVO manualRetry(List<Long> noticeIds, String username) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            throw new IllegalArgumentException("noticeIds不能为空");
        }
        // Controller 已通过 getLoginUsername 获取并传入经过统一兜底的操作人。
        String operator = username;
        // 每条通知均独立统计结果，确保批量操作中部分失败时前端可准确提示
        RecipeChangeNoticeRetryRespVO retryRespVO = new RecipeChangeNoticeRetryRespVO();
        retryRespVO.setSuccessCount(0);
        retryRespVO.setFailureCount(0);
        List<RecipeChangeNoticeRetryResultItemRespVO> failedRetryResults = new ArrayList<>();
        List<RecipeChangeNoticeRetryResultItemRespVO> successfulRetryResults = new ArrayList<>();
        for (Long noticeId : noticeIds) {
            RecipeChangeNoticeDO recipeChangeNotice = recipeChangeNoticeMapper.selectById(noticeId);
            if (recipeChangeNotice == null || !isManualRetryAllowed(recipeChangeNotice.getStatus())) {
                // 处理期间记录可能被删除或已由其他流程改变状态，不能将其视为重发成功
                addRetryFailure(retryRespVO, failedRetryResults, noticeId, recipeChangeNotice, "通知不存在或当前状态不允许人工重发");
                continue;
            }
            recipeChangeNoticeDispatchService.dispatchRecipeChangeNotice(noticeId, RecipeChangeOperationTypeEnum.MANUAL_RETRY.getType(), RecipeChangeTriggerTypeEnum.MANUAL.getType(), operator, false);
            // 分发服务会同步等待 RabbitMQ 发布确认，再查询最终状态即可取得实际投递结果
            RecipeChangeNoticeDO latestRecipeChangeNotice = recipeChangeNoticeMapper.selectById(noticeId);
            if (latestRecipeChangeNotice != null && RecipeChangeNoticeStatusEnum.SENT_MQ.getStatus().equals(latestRecipeChangeNotice.getStatus())) {
                retryRespVO.setSuccessCount(retryRespVO.getSuccessCount() + 1);
                successfulRetryResults.add(buildRetryResultItem(latestRecipeChangeNotice.getNotifyId(), true, null));
                continue;
            }
            String errorMsg = latestRecipeChangeNotice != null ? latestRecipeChangeNotice.getErrorMsg() : null;
            addRetryFailure(retryRespVO, failedRetryResults, noticeId, recipeChangeNotice, errorMsg != null ? errorMsg : "RabbitMQ 投递未成功完成");
        }
        // 失败结果先返回，使前端表格无需额外排序即可将待处理问题放在最上方
        failedRetryResults.addAll(successfulRetryResults);
        retryRespVO.setRetryResults(failedRetryResults);
        return retryRespVO;
    }

    /**
     * 批量标记发送失败、车间处理失败或待人工处理的通知为已完成
     * 标记完成后通知不再满足自动重试的扫描条件，MES 消费端后续收到该消息时将直接确认并清除消息
     */
    @Override
    public RecipeChangeNoticeRetryRespVO batchMarkComplete(List<Long> noticeIds, String username) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            throw new IllegalArgumentException("noticeIds不能为空");
        }
        // 与人工重发使用同一操作人获取规则，保证两类人工操作的日志格式一致。
        String operator = username;
        RecipeChangeNoticeRetryRespVO markCompleteRespVO = new RecipeChangeNoticeRetryRespVO();
        markCompleteRespVO.setSuccessCount(0);
        markCompleteRespVO.setFailureCount(0);
        List<RecipeChangeNoticeRetryResultItemRespVO> failedResults = new ArrayList<>();
        List<RecipeChangeNoticeRetryResultItemRespVO> successfulResults = new ArrayList<>();
        for (Long noticeId : noticeIds) {
            RecipeChangeNoticeDO recipeChangeNotice = recipeChangeNoticeMapper.selectById(noticeId);
            if (recipeChangeNotice == null || !isManualRetryAllowed(recipeChangeNotice.getStatus())) {
                // 仅允许终止异常状态的通知，其他状态可能仍在正常处理，不能被人工结束
                addRetryFailure(markCompleteRespVO, failedResults, noticeId, recipeChangeNotice, "通知不存在或当前状态不允许标记完成");
                continue;
            }
            // 使用原状态作为更新条件，避免管理员操作覆盖并发完成、重发或回调产生的状态变化
            int updatedRows = recipeChangeNoticeMapper.updateMarkedComplete(noticeId, recipeChangeNotice.getStatus(), RecipeChangeNoticeStatusEnum.MARKED_COMPLETED.getStatus(), operator);
            if (updatedRows == 0) {
                addRetryFailure(markCompleteRespVO, failedResults, noticeId, recipeChangeNotice, "通知状态已变化，未能标记完成");
                continue;
            }
            LocalDateTime now = LocalDateTime.now();
            // 状态日志记录终止重试前的原状态和已标记完成状态，保留完整业务轨迹
            recipeChangeStatusLogMapper.insertStatusLog(buildMarkedCompleteStatusLog(recipeChangeNotice, now, operator));
            // 操作日志记录管理员、操作结果和请求响应快照，便于后续审计批量处理动作
            recipeChangeOperationLogMapper.insertOperationLog(buildMarkedCompleteOperationLog(recipeChangeNotice, now, operator));
            markCompleteRespVO.setSuccessCount(markCompleteRespVO.getSuccessCount() + 1);
            successfulResults.add(buildRetryResultItem(recipeChangeNotice.getNotifyId(), true, null));
        }
        // 失败结果置顶，管理员打开结果弹窗后可优先排查未处理成功的通知
        failedResults.addAll(successfulResults);
        markCompleteRespVO.setRetryResults(failedResults);
        return markCompleteRespVO;
    }

    /**
     * 将单条人工重发失败信息汇总到响应中
     *
     * @param retryRespVO        批量重发结果
     * @param failedRetryResults 失败重发结果列表
     * @param noticeId           本次处理的通知主键
     * @param recipeChangeNotice 本次处理的通知记录
     * @param errorMsg           重发失败原因
     */
    private void addRetryFailure(RecipeChangeNoticeRetryRespVO retryRespVO,
                                 List<RecipeChangeNoticeRetryResultItemRespVO> failedRetryResults, Long noticeId,
                                 RecipeChangeNoticeDO recipeChangeNotice, String errorMsg) {
        // 通知不存在时无从读取 notifyId，使用通知主键替代以便管理员定位问题
        String notificationIdentifier = recipeChangeNotice != null ? recipeChangeNotice.getNotifyId() : "通知主键" + noticeId;
        retryRespVO.setFailureCount(retryRespVO.getFailureCount() + 1);
        failedRetryResults.add(buildRetryResultItem(notificationIdentifier, false, errorMsg));
    }

    /**
     * 创建单条人工重发结果明细
     *
     * @param notifyId MPM 通知唯一标识
     * @param success  本次重发是否成功
     * @param errorMsg 本次重发失败原因
     * @return 可直接返回给前端表格的重发结果明细
     */
    private RecipeChangeNoticeRetryResultItemRespVO buildRetryResultItem(String notifyId, boolean success, String errorMsg) {
        RecipeChangeNoticeRetryResultItemRespVO retryResultItemRespVO = new RecipeChangeNoticeRetryResultItemRespVO();
        retryResultItemRespVO.setNotifyId(notifyId);
        retryResultItemRespVO.setSuccess(success);
        retryResultItemRespVO.setErrorMsg(errorMsg);
        return retryResultItemRespVO;
    }

    /**
     * 创建批量标记完成对应的状态流转日志
     *
     * @param recipeChangeNotice 被标记完成的通知
     * @param changeTime         状态变化时间
     * @param operator           执行操作的管理员
     * @return 待写入的状态日志
     */
    private RecipeChangeStatusLogDO buildMarkedCompleteStatusLog(RecipeChangeNoticeDO recipeChangeNotice,
                                                                 LocalDateTime changeTime, String operator) {
        RecipeChangeStatusLogDO statusLog = new RecipeChangeStatusLogDO();
        statusLog.setNoticeId(recipeChangeNotice.getId());
        statusLog.setNotifyId(recipeChangeNotice.getNotifyId());
        statusLog.setFromStatus(recipeChangeNotice.getStatus());
        statusLog.setToStatus(RecipeChangeNoticeStatusEnum.MARKED_COMPLETED.getStatus());
        statusLog.setChangeTime(changeTime);
        statusLog.setTriggerType(RecipeChangeTriggerTypeEnum.MANUAL.getType());
        statusLog.setCreator(operator);
        statusLog.setRemark("管理员批量标记完成");
        return statusLog;
    }

    /**
     * 创建批量标记完成对应的操作日志
     *
     * @param recipeChangeNotice 被标记完成的通知
     * @param operationTime      操作发生时间
     * @param operator           执行操作的管理员
     * @return 待写入的操作日志
     */
    private RecipeChangeOperationLogDO buildMarkedCompleteOperationLog(RecipeChangeNoticeDO recipeChangeNotice,
                                                                       LocalDateTime operationTime, String operator) {
        RecipeChangeOperationLogDO operationLog = new RecipeChangeOperationLogDO();
        operationLog.setNoticeId(recipeChangeNotice.getId());
        operationLog.setNotifyId(recipeChangeNotice.getNotifyId());
        operationLog.setWorkshopCode(recipeChangeNotice.getWorkshopCode());
        operationLog.setOperationType(RecipeChangeOperationTypeEnum.BATCH_PROCESS.getType());
        operationLog.setOperationTime(operationTime);
        operationLog.setOperator(operator);
        operationLog.setCreator(operator);
        operationLog.setOperationResult(RecipeChangeOperationResultEnum.SUCCESS.getResult());
        operationLog.setRequestParams(objectMapper.createObjectNode().put("operation", "批量标记完成"));
        operationLog.setResponseParams(objectMapper.createObjectNode()
                .put("status", RecipeChangeNoticeStatusEnum.MARKED_COMPLETED.getName()));
        return operationLog;
    }

    /**
     * 将自动重试次数耗尽的通知转入待人工处理，并留下完整追踪日志
     */
    private void markPendingManual(RecipeChangeNoticeDO recipeChangeNotice) {
        int updatedRows = recipeChangeNoticeMapper.updatePendingManual(recipeChangeNotice.getId(),
                RecipeChangeNoticeStatusEnum.SEND_FAILED.getStatus(), RecipeChangeNoticeStatusEnum.PENDING_MANUAL.getStatus());
        if (updatedRows == 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        RecipeChangeStatusLogDO statusLog = new RecipeChangeStatusLogDO();
        statusLog.setNoticeId(recipeChangeNotice.getId());
        statusLog.setNotifyId(recipeChangeNotice.getNotifyId());
        statusLog.setFromStatus(recipeChangeNotice.getStatus());
        statusLog.setToStatus(RecipeChangeNoticeStatusEnum.PENDING_MANUAL.getStatus());
        statusLog.setChangeTime(now);
        statusLog.setTriggerType(RecipeChangeTriggerTypeEnum.SYSTEM.getType());
        statusLog.setCreator(JUMP);
        recipeChangeStatusLogMapper.insertStatusLog(statusLog);
        RecipeChangeOperationLogDO operationLog = new RecipeChangeOperationLogDO();
        operationLog.setNoticeId(recipeChangeNotice.getId());
        operationLog.setNotifyId(recipeChangeNotice.getNotifyId());
        operationLog.setWorkshopCode(recipeChangeNotice.getWorkshopCode());
        operationLog.setOperationType(RecipeChangeOperationTypeEnum.SCHEDULED_RETRY.getType());
        operationLog.setOperationTime(now);
        operationLog.setOperator(JUMP);
        operationLog.setCreator(JUMP);
        operationLog.setOperationResult(RecipeChangeOperationResultEnum.FAILURE.getResult());
        operationLog.setErrorMsg("自动重试次数已达上限，等待人工处理");
        operationLog.setRequestParams(objectMapper.createObjectNode());
        operationLog.setResponseParams(objectMapper.createObjectNode());
        recipeChangeOperationLogMapper.insertOperationLog(operationLog);
    }

    /**
     * 判断当前状态是否允许人工重发
     */
    private boolean isManualRetryAllowed(Integer status) {
        return RecipeChangeNoticeStatusEnum.SEND_FAILED.getStatus().equals(status)
                || RecipeChangeNoticeStatusEnum.MES_PROCESS_FAILED.getStatus().equals(status)
                || RecipeChangeNoticeStatusEnum.PENDING_MANUAL.getStatus().equals(status);
    }

}
