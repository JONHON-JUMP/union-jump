package cn.jonhon.jump.module.bpm.controller.admin.definition;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.bpm.controller.admin.definition.vo.task.BpmTaskAssignRuleRespVO;
import cn.jonhon.jump.module.bpm.controller.admin.definition.vo.task.BpmTaskAssignRuleSaveReqVO;
import cn.jonhon.jump.module.bpm.service.definition.BpmTaskAssignRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 流程任务分配规则")
@RestController
@RequestMapping("/bpm/task-assign-rule")
@Validated
public class BpmTaskAssignRuleController {

    @Resource
    private BpmTaskAssignRuleService taskAssignRuleService;

    @GetMapping("/list")
    @Operation(summary = "获得流程任务分配规则列表")
    @PreAuthorize("@ss.hasPermission('bpm:task-assign-rule:query')")
    public CommonResult<List<BpmTaskAssignRuleRespVO>> getTaskAssignRuleList(
            @Parameter(name = "modelId", description = "流程模型的编号")
            @RequestParam(value = "modelId", required = false) String modelId,
            @Parameter(name = "processDefinitionId", description = "流程定义的编号")
            @RequestParam(value = "processDefinitionId", required = false) String processDefinitionId) {
        return success(taskAssignRuleService.getTaskAssignRuleList(modelId, processDefinitionId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建流程任务分配规则")
    @PreAuthorize("@ss.hasPermission('bpm:task-assign-rule:create')")
    public CommonResult<Long> createTaskAssignRule(@Valid @RequestBody BpmTaskAssignRuleSaveReqVO createReqVO) {
        return success(taskAssignRuleService.createTaskAssignRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新流程任务分配规则")
    @PreAuthorize("@ss.hasPermission('bpm:task-assign-rule:update')")
    public CommonResult<Boolean> updateTaskAssignRule(@Valid @RequestBody BpmTaskAssignRuleSaveReqVO updateReqVO) {
        taskAssignRuleService.updateTaskAssignRule(updateReqVO);
        return success(true);
    }

}
