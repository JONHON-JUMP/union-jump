package cn.jonhon.jump.module.rm.recipechange.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工艺变更通知人工重发请求
 */
@Schema(description = "管理后台 - 工艺变更通知人工重发请求")
@Data
public class RecipeChangeNoticeRetryReqVO {

    /**
     * 需要人工重发的通知主键列表
     */
    @Schema(description = "通知主键列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> noticeIds;

}
