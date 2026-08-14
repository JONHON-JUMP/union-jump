package cn.jonhon.jump.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 权限有效性探测 Response VO")
@Data
public class AuthPermissionCheckRespVO {

    @Schema(description = "会话权限是否仍有效")
    private Boolean alive;

    @Schema(description = "是否需要重新登录")
    private Boolean needRelogin;

    @Schema(description = "当前服务端权限版本")
    private Long rbacVersion;

}
