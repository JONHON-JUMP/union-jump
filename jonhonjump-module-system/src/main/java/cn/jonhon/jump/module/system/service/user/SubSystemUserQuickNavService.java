package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;

import java.util.Collection;
import java.util.List;

/**
 * 用户外部子系统快捷导航 Service
 */
public interface SubSystemUserQuickNavService {

    SubSystemUserQuickNavRespVO getUserQuickNav(Long userId, Long subSystemId);

    /**
     * 保存个人快捷导航，返回落库后的权威结果（含锁定项合并与 apps）
     */
    SubSystemUserQuickNavRespVO saveUserQuickNav(Long userId, Long subSystemId, List<Long> menuIds);

    void deleteByMenuId(Long menuId);

    void deleteByMenuIds(List<Long> menuIds);

    /** 是否被用户个人快捷导航引用 */
    boolean existsByMenuId(Long menuId);

    /** 是否被用户个人快捷导航引用（任一菜单） */
    boolean existsByMenuIds(List<Long> menuIds);

    /**
     * 角色取消菜单权限后：去掉指定用户在该子系统个人快捷导航中的对应菜单
     */
    void removeMenusForUsers(Long subSystemId, Collection<Long> userIds, Collection<Long> menuIds);

    /**
     * 角色默认快捷导航保存后：仅清理「本角色本次从默认里取消」的项，保留用户自己加星的入口，再并入全部角色默认
     */
    void alignUsersAfterRoleQuickNavSave(Long subSystemId, Collection<Long> userIds,
                                         Collection<Long> cancelledRoleDefaultMenuIds,
                                         Collection<Long> roleValidMenuIds);

    /**
     * 按角色合并默认快捷导航，写入关联用户的数据库记录，并清除 Redis 缓存
     */
    void syncUserQuickNavByRoleId(Long roleId, Long subSystemId);

    /**
     * 按当前用户在指定子系统下各角色默认快捷导航，写入数据库并清除 Redis 缓存
     */
    void syncUserQuickNavFromRoles(Long userId, Long subSystemId);

    void syncUserQuickNavFromRoles(Collection<Long> userIds, Long subSystemId);

    /**
     * 子系统菜单展示信息变更后失效快捷导航 Redis 缓存
     */
    void evictCacheByMenuIds(Collection<Long> menuIds);

}
