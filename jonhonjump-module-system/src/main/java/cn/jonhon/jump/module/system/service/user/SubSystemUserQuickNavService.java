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
     * @param workshopCode 可选；MES 车间入口时只返回该车间角色可见的快捷导航
     */
    SubSystemUserQuickNavRespVO getUserQuickNav(Long userId, Long subSystemId, String workshopCode);

    /**
     * 保存个人快捷导航，返回落库后的权威结果（含锁定项合并与 apps）
     */
    SubSystemUserQuickNavRespVO saveUserQuickNav(Long userId, Long subSystemId, List<Long> menuIds);

    /**
     * @param workshopCode 可选；多车间叶子各自保存，互不覆盖
     */
    SubSystemUserQuickNavRespVO saveUserQuickNav(Long userId, Long subSystemId, List<Long> menuIds,
                                                 String workshopCode);

    /**
     * 单项加入个人快捷导航（增量：只插一行，不动其它项），返回落库后的权威结果。
     * 星标开/关必须走增量接口，save 是全量覆盖语义，前端列表不完整时会把其它项全删。
     */
    SubSystemUserQuickNavRespVO addUserQuickNavMenu(Long userId, Long subSystemId, String workshopCode, Long menuId);

    /**
     * 单项移除个人快捷导航（增量：只删一行，不动其它项）；角色锁定默认项不移除。
     */
    SubSystemUserQuickNavRespVO removeUserQuickNavMenu(Long userId, Long subSystemId, String workshopCode, Long menuId);

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
