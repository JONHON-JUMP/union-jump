package cn.jonhon.jump.module.rm.recipechange.controller.mes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MES - 工艺变更通知无需处理状态查询响应
 */
@Schema(description = "MES - 工艺变更通知无需处理状态查询响应")
@Data
public class RecipeChangeProcessingNotRequiredRespVO {

    /** 是否无需 MES 继续处理 */
    @Schema(description = "是否无需 MES 继续处理", example = "false")
    private Boolean processingNotRequired;

}
