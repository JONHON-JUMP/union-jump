package cn.jonhon.jump.framework.portalsubsystem.auth;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 主系统 permission-info 响应：角色 + 权限 + 菜单树。
 */
@Data
public class PortalPermissionInfoResult {

    private String username;
    private String nickname;
    private String status;
    private String workshopId;
    private String teamId;
    private List<Role> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    /** 主系统用户 ID；用于读/校验 portal:perm:context */
    private Long mainUserId;
    /**
     * 子系统 RBAC 版本：登录时写入；主系统改菜单/角色/数据权限会递增。
     * 子系统比对会话版本与 Redis 当前版本，不一致则提示重登（不受 my-menus warm 影响）。
     */
    private Long rbacVersion;
    private List<MenuNode> menus = new ArrayList<>();

    @Data
    public static class Role {
        private Long id;
        private String code;
        private String name;
        /** 数据范围（与若依一致：1全部 2自定义 3本部门 4本部门及以下 5仅本人） */
        private Integer dataScope;
        /** 自定义数据权限部门 */
        private java.util.Set<Long> dataScopeDeptIds;
    }

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
