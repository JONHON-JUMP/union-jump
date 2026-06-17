package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统角色精简 Response VO")
@Data
public class SubSystemRoleSimpleRespVO {

    @Schema(description = "角色编号")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色标识")
    private String code;

}
