package cn.jonhon.jump.module.rm.recipechange.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工艺变更通知操作执行结果定义
 */
@Getter
@AllArgsConstructor
public enum RecipeChangeOperationResultEnum {

    /** 操作已成功完成 */
    SUCCESS(10, "成功"),
    /** 操作执行失败 */
    FAILURE(20, "失败");

    /** 操作结果编码，持久化至操作流水表 */
    private final Integer result;
    /** 操作结果中文名称，用于展示和日志说明 */
    private final String name;

}
