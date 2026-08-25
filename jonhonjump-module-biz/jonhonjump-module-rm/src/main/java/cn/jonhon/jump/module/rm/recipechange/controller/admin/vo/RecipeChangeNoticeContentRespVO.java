package cn.jonhon.jump.module.rm.recipechange.controller.admin.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工艺变更通知内容查询响应
 */
@Schema(description = "管理后台 - 工艺变更通知内容查询响应")
@Data
public class RecipeChangeNoticeContentRespVO {

    /**
     * 通知主键
     */
    @Schema(description = "通知主键", example = "1")
    private Long id;

    /**
     * MPM 通知唯一标识
     */
    @Schema(description = "MPM 通知唯一标识", example = "MPM-20260821-001")
    private String notifyId;

    /**
     * 目标车间编码
     */
    @Schema(description = "目标车间编码", example = "5600")
    private String workshopCode;

    /**
     * MPM 发送的工艺变更 JSON 内容
     */
    @Schema(description = "MPM 发送的工艺变更 JSON 内容")
    private JsonNode changeContent;

}
