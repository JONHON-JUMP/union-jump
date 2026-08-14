package cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 子系统权限包（与 portal-subsystem-client 的 PortalPermissionContext 字段对齐）
 */
@Schema(description = "子系统权限上下文")
@Data
public class PortalPermContextRespVO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "OAuth client_id")
    private String clientId;

    @Schema(description = "主系统用户编号")
    private Long userId;

    @Schema(description = "租户编号")
    private Long tenantId;

    @Schema(description = "子系统编号")
    private Long subSystemId;

    @Schema(description = "子系统角色列表")
    private List<Role> roles = new ArrayList<>();

    @Schema(description = "按钮权限标识列表")
    private List<String> permissions = new ArrayList<>();

    @Data
    public static class Role {
        private Long id;
        private String code;
        private String name;
        /** @deprecated 主系统不下发数据范围，由子系统本地管理 */
        @Deprecated
        private Integer dataScope;
        /** @deprecated 主系统不下发数据范围，由子系统本地管理 */
        @Deprecated
        private java.util.Set<Long> dataScopeDeptIds;
    }

}
