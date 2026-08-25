package cn.jonhon.jump.module.rm.recipechange.controller.mpm.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MPM 推送工艺变更通知的请求参数
 */
@Schema(description = "MPM - 工艺变更通知请求")
@Data
public class RecipeChangeNoticeReqVO {

    /** MPM 为本次通知分配的唯一标识，用于接口幂等处理 */
    @Schema(description = "MPM 通知唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String notifyId;

    /** 需要接收该工艺变更通知的目标车间编码 */
    @Schema(description = "目标车间编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workshopCode;

    /** 工艺变更的业务内容，按原始 JSON 结构保存，不限制是否为空 */
    @Schema(description = "工艺变更内容")
    private JsonNode changeContent;

}
