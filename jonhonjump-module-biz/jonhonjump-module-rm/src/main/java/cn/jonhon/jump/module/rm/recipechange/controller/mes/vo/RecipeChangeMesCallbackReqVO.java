package cn.jonhon.jump.module.rm.recipechange.controller.mes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MES 工艺变更消息处理结果回调请求
 */
@Schema(description = "MES - 工艺变更消息处理结果回调请求")
@Data
public class RecipeChangeMesCallbackReqVO {

    /** MPM 通知唯一标识 */
    @Schema(description = "MPM 通知唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String notifyId;
    /** MES 所属车间编码 */
    @Schema(description = "MES 所属车间编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workshopCode;
    /** JUMP 原子领取处理权后返回的令牌 */
    @Schema(description = "JUMP 返回的处理令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processingToken;
    /** MES 是否成功完成处理 */
    @Schema(description = "MES 是否成功完成处理", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean success;
    /** MES 处理失败原因，成功时为空 */
    @Schema(description = "MES 处理失败原因")
    private String errorMsg;

}
