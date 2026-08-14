package cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 角色头像分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleAvatarPageReqVO extends PageParam {

    @Schema(description = "角色标识", example = "super_admin")
    private String roleCode;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
