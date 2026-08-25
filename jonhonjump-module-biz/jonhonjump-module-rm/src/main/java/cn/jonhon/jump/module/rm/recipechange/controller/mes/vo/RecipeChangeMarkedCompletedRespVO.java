package cn.jonhon.jump.module.rm.recipechange.controller.mes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MES 查询通知是否已标记完成的响应
 */
@Schema(description = "MES - 工艺变更通知已标记完成状态查询响应")
@Data
public class RecipeChangeMarkedCompletedRespVO {

    /** 是否已被管理员标记完成 */
    @Schema(description = "是否已标记完成", example = "false")
    private Boolean markedCompleted;

}
