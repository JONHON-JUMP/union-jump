package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 子系统车间精简 Response VO")
@Data
public class SubSystemWorkshopSimpleRespVO {

    @Schema(description = "主键编号")
    private Long id;

    @Schema(description = "JUMP 部门 ID")
    private Long deptId;

    @Schema(description = "JUMP 部门名称")
    private String deptName;

    @Schema(description = "车间编码")
    private String workshopCode;

    @Schema(description = "车间名称")
    private String workshopName;

}
