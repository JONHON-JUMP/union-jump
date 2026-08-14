package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalPermContextRespVO;

/**
 * 子系统权限包：聚合 roles/permissions，写入 Redis，并提供失效入口。
 */
public interface SubSystemPermissionContextService {

    /**
     * 读 Redis；miss 时从 DB 重建并回写。
     */
    PortalPermContextRespVO getOrRebuild(Long tenantId, Long mainUserId, Long subSystemId);

    /**
     * 强制从 DB 重建并写入 Redis。
     */
    PortalPermContextRespVO rebuildAndCache(Long tenantId, Long mainUserId, Long subSystemId);

    /**
     * 仅读 Redis，不重建。
     */
    PortalPermContextRespVO getFromCache(Long tenantId, Long mainUserId, Long subSystemId);

    void evict(Long tenantId, Long mainUserId, Long subSystemId);

    /** 按子系统用户主键失效（改角色等） */
    void evictBySubSystemUserId(Long subSystemUserId);

    /** 按角色失效：该角色下所有关联用户 */
    void evictByRoleId(Long roleId);

    /** 按菜单失效：授权了该菜单的角色下所有用户 */
    void evictByMenuId(Long menuId);

    /** 主系统用户禁用/删除：清该用户所有子系统权限包 */
    void evictAllByMainUserId(Long mainUserId);

    /** 子系统停用/OAuth 变更：清该子系统全部用户权限包 */
    void evictBySubSystemId(Long subSystemId);

    /**
     * 当前子系统 RBAC 版本（菜单/角色变更递增）。门户用于判断是否需要重拉 my-menus。
     */
    long getRbacVersion(Long subSystemId);

}
