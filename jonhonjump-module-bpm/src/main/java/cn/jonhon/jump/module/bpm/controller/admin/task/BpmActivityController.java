package cn.jonhon.jump.module.bpm.controller.admin.task;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.bpm.controller.admin.task.vo.activity.BpmActivityRespVO;
import cn.jonhon.jump.module.bpm.service.task.BpmTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flowable.engine.history.HistoricActivityInstance;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - 流程活动实例")
@RestController
@RequestMapping("/bpm/activity")
@Validated
public class BpmActivityController {

    @Resource
    private BpmTaskService taskService;

    @GetMapping("/list")
    @Operation(summary = "生成指定流程实例的高亮流程图")
    @Parameter(name = "processInstanceId", description = "流程实例的编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:process-instance:query')")
    public CommonResult<List<BpmActivityRespVO>> getActivityList(@RequestParam("processInstanceId") String processInstanceId) {
        List<HistoricActivityInstance> activityList = taskService.getActivityListByProcessInstanceId(processInstanceId);
        return success(convertList(activityList, activity -> {
            BpmActivityRespVO respVO = new BpmActivityRespVO();
            respVO.setKey(activity.getActivityId());
            respVO.setType(activity.getActivityType());
            respVO.setStartTime(activity.getStartTime());
            respVO.setEndTime(activity.getEndTime());
            respVO.setTaskId(activity.getTaskId());
            return respVO;
        }));
    }

}
