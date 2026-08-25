package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeMesCallbackReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeProcessingAcquireRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeDingTalkAlarmLogReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeDingTalkAlarmLogRespVO;
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

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MES 工艺变更消息处理结果回调服务实现
 */
@Service
public class RecipeChangeMesCallbackServiceImpl implements RecipeChangeMesCallbackService {

    /** MES 系统标识，用于主表更新人与回调流水创建人 */
    private static final String MES = "MES";

    @Resource
    private RecipeChangeNoticeMapper recipeChangeNoticeMapper;
    @Resource
    private RecipeChangeOperationLogMapper recipeChangeOperationLogMapper;
    @Resource
    private RecipeChangeStatusLogMapper recipeChangeStatusLogMapper;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 通过条件更新原子领取处理权；同一时刻只有一个 MES 消费者能够进入本地业务。
     */
    @Override
    public RecipeChangeProcessingAcquireRespVO acquireProcessing(String notifyId, String workshopCode) {
        if (isBlank(notifyId) || isBlank(workshopCode)) {
            throw new IllegalArgumentException("notifyId和workshopCode不能为空");
        }
        RecipeChangeNoticeDO recipeChangeNotice = recipeChangeNoticeMapper.selectByNotifyId(notifyId);
        if (recipeChangeNotice == null || !workshopCode.equals(recipeChangeNotice.getWorkshopCode())) {
            throw new IllegalArgumentException("工艺变更通知不存在或车间编码不匹配");
        }
        RecipeChangeProcessingAcquireRespVO response = new RecipeChangeProcessingAcquireRespVO();
        // 终态消息无需再次领取，Starter 收到该结果后直接 ACK。
        if (isProcessingNotRequired(recipeChangeNotice.getStatus())) {
            response.setProcessingNotRequired(true);
            response.setAcquired(false);
            return response;
        }
        // 每次领取生成新令牌；租约过期后重新领取会替换旧令牌，旧回调因此失效。
        String processingToken = UUID.randomUUID().toString();
        /*
         * 条件更新覆盖且仅覆盖以下可处理场景：
         * 1. SENT_MQ(10)：JUMP 已投递 MQ，当前消息首次被 MES 消费；
         * 2. MES_PROCESS_FAILED(25)：前一次 MES 业务失败后经延迟队列回流，需要再次尝试；
         * 3. MES_PROCESSING(18) 且租约到期：原消费者可能宕机或未确认，允许重投消息接管。
         *
         * MES_PROCESS_SUCCESS(20) 和 MARKED_COMPLETED(35) 是终态，前面已直接返回无需处理；
         * RECEIVED_SUCCESS(5)、SEND_FAILED(15)、PENDING_MANUAL(30) 不存在可安全处理的 MQ 消费场景，
         * 因此不在此处领取。SQL 返回 1 表示当前消费者唯一领取成功，返回 0 表示并发消费者已领取
         * 或状态已变化，Starter 将按重新读取后的状态 ACK 或转入延迟队列。
         */
        int updatedRows = recipeChangeNoticeMapper.tryAcquireProcessing(recipeChangeNotice.getId(),
                RecipeChangeNoticeStatusEnum.SENT_MQ.getStatus(), RecipeChangeNoticeStatusEnum.MES_PROCESS_FAILED.getStatus(),
                RecipeChangeNoticeStatusEnum.MES_PROCESSING.getStatus(), processingToken, MES);
        if (updatedRows == 0) {
            // 条件更新失败说明有并发消费者已领取，或状态刚刚进入终态；重新读取后准确返回给 Starter。
            RecipeChangeNoticeDO latestNotice = recipeChangeNoticeMapper.selectByNotifyId(notifyId);
            response.setProcessingNotRequired(latestNotice != null && isProcessingNotRequired(latestNotice.getStatus()));
            response.setAcquired(false);
            return response;
        }
        LocalDateTime now = LocalDateTime.now();
        // 领取成功后记录状态流水，使处理权占用和后续 MES 回调均可追溯。
        recipeChangeStatusLogMapper.insertStatusLog(buildStatusLog(recipeChangeNotice, RecipeChangeNoticeStatusEnum.MES_PROCESSING, now));
        response.setAcquired(true);
        response.setProcessingNotRequired(false);
        response.setProcessingToken(processingToken);
        return response;
    }

    /**
     * 将 MES 处理结果写入通知主表、状态流水和操作流水
     * 仅持有当前有效处理令牌的回调可以完成状态流转；旧令牌回调不覆盖状态或重复写入流水。
     */
    @Override
    public void callbackProcessResult(RecipeChangeMesCallbackReqVO callbackReqVO) {
        // 显式校验 MES 回调关键字段，便于从接口日志和异常信息中快速定位调用问题
        validateRequiredFields(callbackReqVO);
        RecipeChangeNoticeDO recipeChangeNotice = recipeChangeNoticeMapper.selectByNotifyId(callbackReqVO.getNotifyId());
        if (recipeChangeNotice == null || !callbackReqVO.getWorkshopCode().equals(recipeChangeNotice.getWorkshopCode())) {
            throw new IllegalArgumentException("工艺变更通知不存在或车间编码不匹配");
        }
        RecipeChangeNoticeStatusEnum targetStatus = Boolean.TRUE.equals(callbackReqVO.getSuccess()) ? RecipeChangeNoticeStatusEnum.MES_PROCESS_SUCCESS : RecipeChangeNoticeStatusEnum.MES_PROCESS_FAILED;
        String errorMsg = Boolean.TRUE.equals(callbackReqVO.getSuccess()) ? null : callbackReqVO.getErrorMsg();
        // SQL 同时校验处理中状态和处理令牌，租约到期后的旧消费者无法覆盖新一轮处理结果。
        int updatedRows = recipeChangeNoticeMapper.updateMesProcessStatus(recipeChangeNotice.getId(),
                RecipeChangeNoticeStatusEnum.MES_PROCESSING.getStatus(), targetStatus.getStatus(), MES,
                errorMsg, callbackReqVO.getProcessingToken());
        if (updatedRows == 0) {
            // 重复、过期或已被人工终止的回调均保持幂等，不追加状态和操作流水。
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        recipeChangeStatusLogMapper.insertStatusLog(buildStatusLog(recipeChangeNotice, targetStatus, now));
        recipeChangeOperationLogMapper.insertOperationLog(buildOperationLog(recipeChangeNotice, callbackReqVO, targetStatus, errorMsg, now));
    }

    /**
     * 将 MES 上报的钉钉告警发送请求完整落入工艺变更操作日志
     */
    @Override
    public RecipeChangeDingTalkAlarmLogRespVO recordDingTalkAlarmLog(RecipeChangeDingTalkAlarmLogReqVO dingTalkAlarmLogReqVO) {
        // 显式校验关联通知和发送结果，避免无法定位通知或无法判断钉钉发送结果的无效审计记录
        validateDingTalkAlarmLogRequiredFields(dingTalkAlarmLogReqVO);
        RecipeChangeNoticeDO recipeChangeNotice = recipeChangeNoticeMapper.selectByNotifyId(dingTalkAlarmLogReqVO.getNotifyId());
        // 通知不存在或车间不匹配时拒绝记录，防止 MES 跨车间写入不属于自身的告警流水
        if (recipeChangeNotice == null || !dingTalkAlarmLogReqVO.getWorkshopCode().equals(recipeChangeNotice.getWorkshopCode())) {
            throw new IllegalArgumentException("工艺变更通知不存在或车间编码不匹配");
        }
        LocalDateTime processedTime = LocalDateTime.now();
        // 操作日志 requestParams 保留 MES 的完整原始请求体，responseParams 仅保存 JUMP 对本请求的实际处理结果
        recipeChangeOperationLogMapper.insertOperationLog(buildDingTalkAlarmOperationLog(recipeChangeNotice, dingTalkAlarmLogReqVO, processedTime));
        RecipeChangeDingTalkAlarmLogRespVO response = new RecipeChangeDingTalkAlarmLogRespVO();
        response.setRecorded(true);
        response.setProcessedTime(processedTime);
        return response;
    }

    /**
     * 校验 MES 回调必须提供的通知标识、车间编码和处理结果
     */
    private void validateRequiredFields(RecipeChangeMesCallbackReqVO callbackReqVO) {
        if (callbackReqVO == null || isBlank(callbackReqVO.getNotifyId()) || isBlank(callbackReqVO.getWorkshopCode())
                || isBlank(callbackReqVO.getProcessingToken()) || callbackReqVO.getSuccess() == null) {
            throw new IllegalArgumentException("notifyId、workshopCode、processingToken和success不能为空");
        }
    }

    /**
     * 校验钉钉告警日志请求中用于关联通知和判定发送结果的关键字段
     */
    private void validateDingTalkAlarmLogRequiredFields(RecipeChangeDingTalkAlarmLogReqVO dingTalkAlarmLogReqVO) {
        if (dingTalkAlarmLogReqVO == null || isBlank(dingTalkAlarmLogReqVO.getNotifyId())
                || isBlank(dingTalkAlarmLogReqVO.getWorkshopCode()) || dingTalkAlarmLogReqVO.getSendSuccess() == null) {
            throw new IllegalArgumentException("notifyId、workshopCode和sendSuccess不能为空");
        }
    }

    /**
     * 判断字符串是否为空或只包含空白字符
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 判断通知是否已进入成功或人工终止的终态。
     */
    private boolean isProcessingNotRequired(Integer status) {
        return RecipeChangeNoticeStatusEnum.MES_PROCESS_SUCCESS.getStatus().equals(status)
                || RecipeChangeNoticeStatusEnum.MARKED_COMPLETED.getStatus().equals(status);
    }

    /**
     * 创建 MES 回调导致的状态变更流水
     */
    private RecipeChangeStatusLogDO buildStatusLog(RecipeChangeNoticeDO recipeChangeNotice, RecipeChangeNoticeStatusEnum targetStatus, LocalDateTime changeTime) {
        RecipeChangeStatusLogDO statusLog = new RecipeChangeStatusLogDO();
        statusLog.setNoticeId(recipeChangeNotice.getId());
        statusLog.setNotifyId(recipeChangeNotice.getNotifyId());
        statusLog.setFromStatus(recipeChangeNotice.getStatus());
        statusLog.setToStatus(targetStatus.getStatus());
        statusLog.setChangeTime(changeTime);
        statusLog.setTriggerType(RecipeChangeTriggerTypeEnum.SYSTEM.getType());
        statusLog.setCreator(MES);
        return statusLog;
    }

    /**
     * 创建 MES 回调操作流水，记录回调参数、成功失败结果和错误原因
     */
    private RecipeChangeOperationLogDO buildOperationLog(RecipeChangeNoticeDO recipeChangeNotice,
                                                         RecipeChangeMesCallbackReqVO callbackReqVO,
                                                         RecipeChangeNoticeStatusEnum targetStatus,
                                                         String errorMsg, LocalDateTime operationTime) {
        RecipeChangeOperationLogDO operationLog = new RecipeChangeOperationLogDO();
        operationLog.setNoticeId(recipeChangeNotice.getId());
        operationLog.setNotifyId(recipeChangeNotice.getNotifyId());
        operationLog.setWorkshopCode(recipeChangeNotice.getWorkshopCode());
        operationLog.setOperationType(RecipeChangeOperationTypeEnum.MES_CALLBACK.getType());
        operationLog.setOperationTime(operationTime);
        operationLog.setOperator(MES);
        operationLog.setCreator(MES);
        operationLog.setOperationResult(Boolean.TRUE.equals(callbackReqVO.getSuccess())
                ? RecipeChangeOperationResultEnum.SUCCESS.getResult() : RecipeChangeOperationResultEnum.FAILURE.getResult());
        operationLog.setErrorMsg(errorMsg);
        operationLog.setRequestParams(objectMapper.valueToTree(callbackReqVO));
        operationLog.setResponseParams(objectMapper.createObjectNode().put("status", targetStatus.getName()));
        return operationLog;
    }

    /**
     * 创建钉钉告警发送操作流水，完整保留 MES 请求体并记录 JUMP 写入结果
     */
    private RecipeChangeOperationLogDO buildDingTalkAlarmOperationLog(RecipeChangeNoticeDO recipeChangeNotice,
                                                                       RecipeChangeDingTalkAlarmLogReqVO dingTalkAlarmLogReqVO,
                                                                       LocalDateTime processedTime) {
        RecipeChangeOperationLogDO operationLog = new RecipeChangeOperationLogDO();
        operationLog.setNoticeId(recipeChangeNotice.getId());
        operationLog.setNotifyId(recipeChangeNotice.getNotifyId());
        operationLog.setWorkshopCode(recipeChangeNotice.getWorkshopCode());
        operationLog.setOperationType(RecipeChangeOperationTypeEnum.DINGTALK_ALARM.getType());
        operationLog.setOperationTime(processedTime);
        operationLog.setOperator(MES);
        operationLog.setCreator(MES);
        operationLog.setOperationResult(Boolean.TRUE.equals(dingTalkAlarmLogReqVO.getSendSuccess()) ? RecipeChangeOperationResultEnum.SUCCESS.getResult() : RecipeChangeOperationResultEnum.FAILURE.getResult());
        operationLog.setErrorMsg(dingTalkAlarmLogReqVO.getSendErrorMsg());
        operationLog.setRequestParams(objectMapper.valueToTree(dingTalkAlarmLogReqVO));
        operationLog.setResponseParams(objectMapper.createObjectNode()
                .put("recorded", true)
                .put("processedTime", processedTime.toString()));
        return operationLog;
    }

}
