package cn.jonhon.jump.framework.recipechange.consumer.client;

import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeMessageDTO;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeDingTalkAlarmLogDTO;

/**
 * MES Starter 调用 JUMP 工艺变更接口的客户端
 */
public interface RecipeChangeJUMPClient {

    /**
     * 向 JUMP 原子领取消息处理权。
     *
     * @param notifyId MPM 通知唯一标识
     * @param workshopCode 消费车间编码
     * @return 领取成功时返回令牌；终态表示直接确认；未领取且非终态表示其他消费者正在处理
     */
    RecipeChangeProcessingAcquireResult acquireProcessing(String notifyId, String workshopCode);

    /**
     * 上报 MES 对工艺变更消息的处理结果
     *
     * @param message 已处理的工艺变更消息
     * @param processingToken 领取处理权时 JUMP 返回的令牌，防止旧消费者覆盖新一轮结果
     * @param success MES 处理是否成功
     * @param errorMsg 处理失败原因，成功时为 null
     */
    void callbackProcessResult(RecipeChangeMessageDTO message, String processingToken, boolean success, String errorMsg);

    /**
     * 上报钉钉告警处理器的实际发送结果，供 JUMP 写入操作日志审计
     *
     * @param dingTalkAlarmLog MES 发送钉钉告警后的完整请求参数和发送结果
     */
    void recordDingTalkAlarmResult(RecipeChangeDingTalkAlarmLogDTO dingTalkAlarmLog);

}
