package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeMesCallbackReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeProcessingAcquireRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeDingTalkAlarmLogReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeDingTalkAlarmLogRespVO;

/**
 * MES 工艺变更消息处理结果回调服务
 */
public interface RecipeChangeMesCallbackService {

    /**
     * 原子领取通知的 MES 处理权，防止同一通知的重复消息并发进入 MES 业务。
     */
    RecipeChangeProcessingAcquireRespVO acquireProcessing(String notifyId, String workshopCode);

    /**
     * 处理 MES 对工艺变更消息的处理结果回调
     *
     * @param callbackReqVO MES 回调参数
     */
    void callbackProcessResult(RecipeChangeMesCallbackReqVO callbackReqVO);

    /**
     * 记录 MES 调用钉钉告警处理器后的完整请求参数和发送结果
     *
     * @param dingTalkAlarmLogReqVO MES 上报的钉钉告警发送结果
     * @return JUMP 对本次日志记录请求的处理结果
     */
    RecipeChangeDingTalkAlarmLogRespVO recordDingTalkAlarmLog(RecipeChangeDingTalkAlarmLogReqVO dingTalkAlarmLogReqVO);

}
