package cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 角色头像 Response VO")
@Data
public class RoleAvatarRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "角色标识")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
