package cn.jonhon.jump.framework.recipechange.consumer.core;

/**
 * MES 向 JUMP 上报钉钉告警发送结果的完整请求参数
 */
public class RecipeChangeDingTalkAlarmLogDTO {

    /**
     * MPM 通知唯一标识
     */
    private String notifyId;
    /**
     * MES 所属车间编码
     */
    private String workshopCode;
    /**
     * 钉钉告警标题
     */
    private String title;
    /**
     * 实际发送给钉钉的完整告警文本
     */
    private String alarmContent;
    /**
     * 本次消费的 RabbitMQ 队列名称
     */
    private String queueName;
    /**
     * MES 工艺变更处理失败时间，使用 ISO-8601 字符串避免客户端 JSON 转换器缺少 Java Time 模块
     */
    private String failureTime;
    /**
     * MES 工艺变更处理失败原因
     */
    private String processErrorMsg;
    /**
     * 钉钉告警处理器是否成功完成发送
     */
    private Boolean sendSuccess;
    /**
     * 钉钉发送异常原因，发送成功时为空
     */
    private String sendErrorMsg;

    public String getNotifyId() {
        return notifyId;
    }

    public void setNotifyId(String notifyId) {
        this.notifyId = notifyId;
    }

    public String getWorkshopCode() {
        return workshopCode;
    }

    public void setWorkshopCode(String workshopCode) {
        this.workshopCode = workshopCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlarmContent() {
        return alarmContent;
    }

    public void setAlarmContent(String alarmContent) {
        this.alarmContent = alarmContent;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getFailureTime() {
        return failureTime;
    }

    public void setFailureTime(String failureTime) {
        this.failureTime = failureTime;
    }

    public String getProcessErrorMsg() {
        return processErrorMsg;
    }

    public void setProcessErrorMsg(String processErrorMsg) {
        this.processErrorMsg = processErrorMsg;
    }

    public Boolean getSendSuccess() {
        return sendSuccess;
    }

    public void setSendSuccess(Boolean sendSuccess) {
        this.sendSuccess = sendSuccess;
    }

    public String getSendErrorMsg() {
        return sendErrorMsg;
    }

    public void setSendErrorMsg(String sendErrorMsg) {
        this.sendErrorMsg = sendErrorMsg;
    }

}
