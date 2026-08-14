package cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 角色头像精简 Response VO")
@Data
public class RoleAvatarSimpleRespVO {

    @Schema(description = "角色标识")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "显示排序")
    private Integer sort;

}
