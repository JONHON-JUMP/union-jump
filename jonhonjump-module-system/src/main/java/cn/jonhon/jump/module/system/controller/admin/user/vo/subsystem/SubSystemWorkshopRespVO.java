package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 外部系统车间 Response VO")
@Data
public class SubSystemWorkshopRespVO {

    @Schema(description = "主键编号")
    private Long id;

    @Schema(description = "外部系统 ID")
    private Long subSystemId;

    @Schema(description = "外部系统名称")
    private String clientName;

    @Schema(description = "JUMP 部门 ID")
    private Long deptId;

    @Schema(description = "JUMP 部门名称")
    private String deptName;

    @Schema(description = "车间编码")
    private String workshopCode;

    @Schema(description = "车间名称")
    private String workshopName;

    @Schema(description = "车间描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
