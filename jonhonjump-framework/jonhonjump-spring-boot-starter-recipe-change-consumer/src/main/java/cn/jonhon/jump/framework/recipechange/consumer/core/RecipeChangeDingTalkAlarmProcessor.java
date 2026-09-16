package cn.jonhon.jump.framework.recipechange.consumer.core;

/**
 * 工艺变更消息消费失败钉钉告警扩展接口
 * MES 项目实现本接口后，Starter 会在告警未被抑制时调用具体钉钉发送逻辑
 */
public interface RecipeChangeDingTalkAlarmProcessor {

    /**
     * 发送工艺变更消息消费失败告警
     *
     * @param alarm 已组装完成的告警内容
     */
    void sendConsumptionFailureAlarm(RecipeChangeConsumptionAlarmDTO alarm);

}
