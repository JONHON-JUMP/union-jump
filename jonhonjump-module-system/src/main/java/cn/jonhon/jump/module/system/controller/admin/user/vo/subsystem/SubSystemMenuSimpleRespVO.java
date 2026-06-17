package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统菜单精简 Response VO")
@Data
public class SubSystemMenuSimpleRespVO {

    @Schema(description = "菜单编号")
    private Long id;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "父菜单编号")
    private Long parentId;

    @Schema(description = "显示顺序")
    private Integer orderNum;

}
