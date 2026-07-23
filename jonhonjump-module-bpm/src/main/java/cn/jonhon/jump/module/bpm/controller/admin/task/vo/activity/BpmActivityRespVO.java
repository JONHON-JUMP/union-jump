package cn.jonhon.jump.module.bpm.controller.admin.task.vo.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "管理后台 - 流程活动 Response VO")
@Data
public class BpmActivityRespVO {

    @Schema(description = "活动节点编号", example = "Activity_leader_audit")
    private String key;

    @Schema(description = "活动类型", example = "userTask")
    private String type;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "关联的任务编号", example = "1024")
    private String taskId;

}
