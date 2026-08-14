package cn.jonhon.jump.module.bpm.controller.admin.definition.vo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 流程模型导入 Request VO")
@Data
public class BpmModeImportReqVO {

    @Schema(description = "流程标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "oa_leave")
    @NotEmpty(message = "流程标识不能为空")
    private String key;

    @Schema(description = "流程名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "OA请假")
    @NotEmpty(message = "流程名称不能为空")
    private String name;

    @Schema(description = "流程描述", example = "OA 请假示例流程")
    private String description;

}
