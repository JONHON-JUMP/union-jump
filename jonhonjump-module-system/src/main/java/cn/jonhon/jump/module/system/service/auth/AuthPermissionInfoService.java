package cn.jonhon.jump.module.system.service.auth;

import cn.jonhon.jump.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;

/**
 * 登录用户权限信息 Service
 */
public interface AuthPermissionInfoService {

    /**
     * 获取完整权限信息（含菜单树，写/读 Redis）
     */
    AuthPermissionInfoRespVO getPermissionInfo(Long userId);

    /**
     * @param includeMenus true=含菜单树；false=仅用户/角色/权限（登录首屏）
     * @param redisOnly    true=仅 Redis，未命中返回 null，绝不重建库（后台预热用）
     */
    AuthPermissionInfoRespVO getPermissionInfo(Long userId, boolean includeMenus, boolean redisOnly);

    void evictUser(Long userId);

    void evictUsersByRoleId(Long roleId);

    void evictUsersAffectedByMenu(Long menuId);

    /** 当前用户权限 RBAC 版本 */
    long getRbacVersion(Long userId);

    /**
     * 会话侧版本与 Redis 是否一致；不一致则需重新登录。
     * sessionVersion 为空时不判定（视为 alive）。
     */
    boolean isPermissionAlive(Long userId, Long sessionVersion);

}
