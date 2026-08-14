package cn.jonhon.jump.module.bpm.controller.admin.definition.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 流程任务分配规则新增/修改 Request VO")
@Data
public class BpmTaskAssignRuleSaveReqVO {

    @Schema(description = "规则编号", example = "1024")
    private Long id;

    @Schema(description = "流程模型的编号", example = "1024")
    private String modelId;

    @Schema(description = "流程任务定义的 key", example = "Activity_leader_audit")
    private String taskDefinitionKey;

    @Schema(description = "规则类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "规则类型不能为空")
    private Integer type;

    @Schema(description = "规则范围")
    private List<Long> options;

}
