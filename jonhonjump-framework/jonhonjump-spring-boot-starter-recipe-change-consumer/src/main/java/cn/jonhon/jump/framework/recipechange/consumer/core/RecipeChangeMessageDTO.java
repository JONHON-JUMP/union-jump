package cn.jonhon.jump.framework.recipechange.consumer.core;

import cn.jonhon.jump.framework.recipechange.consumer.dto.RecipeChangeContentDTO;

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
     * MPM 传入的工艺变更内容。消息转换器会按接口文档反序列化为强类型对象。
     */
    private RecipeChangeContentDTO changeContent;

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

    public RecipeChangeContentDTO getChangeContent() {
        return changeContent;
    }

    public void setChangeContent(RecipeChangeContentDTO changeContent) {
        this.changeContent = changeContent;
    }

}
