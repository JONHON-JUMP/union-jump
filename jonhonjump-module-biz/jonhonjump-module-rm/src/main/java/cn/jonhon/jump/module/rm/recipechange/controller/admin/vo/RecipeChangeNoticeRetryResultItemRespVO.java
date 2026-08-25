package cn.jonhon.jump.module.rm.recipechange.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单条工艺变更通知人工重发结果
 */
@Schema(description = "管理后台 - 单条工艺变更通知人工重发结果")
@Data
public class RecipeChangeNoticeRetryResultItemRespVO {

    /**
     * MPM 通知唯一标识
     */
    @Schema(description = "MPM 通知唯一标识", example = "MPM-20260821-001")
    private String notifyId;

    /**
     * 本次是否成功完成 RabbitMQ 投递
     */
    @Schema(description = "本次重发是否成功", example = "true")
    private Boolean success;

    /**
     * 本次重发失败原因，成功时为空
     */
    @Schema(description = "本次重发失败原因", example = "RabbitMQ 发布确认失败")
    private String errorMsg;

}
