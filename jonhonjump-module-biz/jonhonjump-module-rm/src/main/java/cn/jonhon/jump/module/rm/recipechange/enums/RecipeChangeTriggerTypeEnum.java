package cn.jonhon.jump.module.rm.recipechange.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工艺变更通知状态流转触发方式定义
 */
@Getter
@AllArgsConstructor
public enum RecipeChangeTriggerTypeEnum {

    /** 由系统自动处理流程触发 */
    SYSTEM(10, "系统触发"),
    /** 由用户在管理端手动操作触发 */
    MANUAL(20, "人工触发");

    /** 触发方式编码，持久化至状态流水表 */
    private final Integer type;
    /** 触发方式中文名称，用于展示和日志说明 */
    private final String name;

}
