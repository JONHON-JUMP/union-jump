package cn.jonhon.jump.framework.recipechange.consumer.core;

/**
 * 工艺变更消息消费失败告警内容
 */
public class RecipeChangeConsumptionAlarmDTO {

    /**
     * 告警标题
     */
    private String title;
    /**
     * MPM 通知唯一标识
     */
    private String notifyId;
    /**
     * 消费消息所属车间编码
     */
    private String workshopCode;
    /**
     * RabbitMQ 队列名称
     */
    private String queueName;
    /**
     * 消费失败发生时间，使用 ISO-8601 字符串避免钉钉处理器 JSON 解析异常
     */
    private String failureTime;
    /**
     * 消费失败原因
     */
    private String errorMsg;
    /**
     * 实际发送至钉钉的完整告警文本
     */
    private String alarmContent;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

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

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getAlarmContent() {
        return alarmContent;
    }

    public void setAlarmContent(String alarmContent) {
        this.alarmContent = alarmContent;
    }

}
