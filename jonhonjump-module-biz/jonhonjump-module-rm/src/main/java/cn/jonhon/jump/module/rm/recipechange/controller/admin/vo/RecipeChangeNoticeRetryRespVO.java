package cn.jonhon.jump.module.rm.recipechange.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工艺变更通知人工重发响应
 */
@Schema(description = "管理后台 - 工艺变更通知人工重发响应")
@Data
public class RecipeChangeNoticeRetryRespVO {

    /**
     * 成功完成 RabbitMQ 投递的通知数量
     */
    @Schema(description = "重发成功数量", example = "2")
    private Integer successCount;

    /**
     * 未完成 RabbitMQ 投递的通知数量
     */
    @Schema(description = "重发失败数量", example = "1")
    private Integer failureCount;

    /**
     * 每条通知本次人工重发的实际结果，失败记录排在列表前面
     */
    @Schema(description = "人工重发结果明细")
    private List<RecipeChangeNoticeRetryResultItemRespVO> retryResults;

}
