package cn.jonhon.jump.framework.recipechange.consumer.core;

/**
 * JUMP 投递至 MES 的工艺变更消息体
 */
public class RecipeChangeMessageDTO {

    /**
     * MPM 通知唯一标识，也是幂等处理和回调关联标识
     */
    private String notifyId;
    /**
     * 目标 MES 所属车间编码
     */
    private String workshopCode;
    /**
     * MPM 传入的工艺变更 JSON 内容，由消息转换器反序列化为通用对象
     */
    private Object changeContent;

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

    public Object getChangeContent() {
        return changeContent;
    }

    public void setChangeContent(Object changeContent) {
        this.changeContent = changeContent;
    }

}
