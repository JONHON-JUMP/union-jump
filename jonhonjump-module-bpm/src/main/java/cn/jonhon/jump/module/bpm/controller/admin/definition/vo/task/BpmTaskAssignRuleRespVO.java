package cn.jonhon.jump.module.bpm.controller.admin.definition.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 流程任务分配规则 Response VO")
@Data
public class BpmTaskAssignRuleRespVO {

    @Schema(description = "规则编号", example = "1024")
    private Long id;

    @Schema(description = "流程模型的编号", example = "1024")
    private String modelId;

    @Schema(description = "流程定义的编号", example = "2048")
    private String processDefinitionId;

    @Schema(description = "流程任务定义的 key", example = "Activity_leader_audit")
    private String taskDefinitionKey;

    @Schema(description = "流程任务定义的名字", example = "领导审批")
    private String taskDefinitionName;

    @Schema(description = "规则类型", example = "30")
    private Integer type;

    @Schema(description = "规则范围")
    private List<Long> options;

}
