package cn.jonhon.jump.module.rm.recipechange.controller.mes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MES - 工艺变更消息处理权领取响应。
 */
@Schema(description = "MES - 工艺变更消息处理权领取响应")
@Data
public class RecipeChangeProcessingAcquireRespVO {

    /** 当前消息是否已获得 MES 处理权 */
    @Schema(description = "是否获得处理权", example = "true")
    private Boolean acquired;
    /** 通知是否已进入无需 MES 继续处理的终态 */
    @Schema(description = "是否无需处理", example = "false")
    private Boolean processingNotRequired;
    /** 获得处理权时返回的处理令牌，回调时必须原样携带 */
    @Schema(description = "处理令牌")
    private String processingToken;

}
