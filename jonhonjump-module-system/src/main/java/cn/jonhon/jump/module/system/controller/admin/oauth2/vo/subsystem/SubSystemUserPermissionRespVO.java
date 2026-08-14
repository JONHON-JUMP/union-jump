package cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 子系统用户权限查询 Response VO")
@Data
public class SubSystemUserPermissionRespVO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户姓名")
    private String nickname;

    @Schema(description = "状态（0正常 1停用）")
    private String status;

    @Schema(description = "车间编号")
    private String workshopId;

    @Schema(description = "班组编码")
    private String teamId;

    @Schema(description = "角色列表")
    private List<PortalPermContextRespVO.Role> roles = new ArrayList<>();

    @Schema(description = "权限标识列表（勾选页面后自动带上按钮 perms）")
    private List<String> permissions = new ArrayList<>();

    @Schema(description = "主系统用户编号（写入/校验 portal:perm:context 用；未绑定主用户时为空）")
    private Long mainUserId;

    @Schema(description = "子系统 RBAC 版本（登录灌权写入会话；菜单/角色变更后递增）")
    private Long rbacVersion;

    @Schema(description = "菜单树（目录/页面；按钮权限在 permissions 列表）")
    private List<MenuNode> menus = new ArrayList<>();

    @Data
    public static class MenuNode {
        private Long id;
        private Long parentId;
        private String name;
        /** M目录 C菜单 F按钮 */
        private String type;
        private String path;
        private String component;
        private String perms;
        private String icon;
        private Integer orderNum;
        private Integer visible;
        private Integer status;
        private List<MenuNode> children = new ArrayList<>();
    }

}
