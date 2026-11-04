package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavRespVO;

import java.util.Collection;
import java.util.List;

/**
 * 用户快捷导航 Service（主系统）
 */
public interface UserQuickNavService {

    UserQuickNavRespVO getUserQuickNav(Long userId);

    /**
     * 保存个人快捷导航，返回落库后的权威结果（含锁定项合并）
     */
    UserQuickNavRespVO saveUserQuickNav(Long userId, List<Long> menuIds);

    /**
     * 单项加入个人快捷导航（增量：只插一行，不动其它项），返回落库后的权威结果。
     * 星标开/关必须走增量接口，save 是全量覆盖语义，前端列表不完整时会把其它项全删。
     */
    UserQuickNavRespVO addUserQuickNavMenu(Long userId, Long menuId);

    /**
     * 单项移除个人快捷导航（增量：只删一行，不动其它项）；角色锁定默认项不移除。
     */
    UserQuickNavRespVO removeUserQuickNavMenu(Long userId, Long menuId);

    void deleteByMenuId(Long menuId);

    void deleteByMenuIds(List<Long> menuIds);

    /** 是否被用户个人快捷导航引用 */
    boolean existsByMenuId(Long menuId);

    /** 是否被用户个人快捷导航引用（任一菜单） */
    boolean existsByMenuIds(List<Long> menuIds);

    /**
     * 角色取消菜单权限后：去掉指定用户个人快捷导航中的对应菜单
     */
    void removeMenusForUsers(Collection<Long> userIds, Collection<Long> menuIds);

    /**
     * 角色默认快捷导航保存后：仅清理「本角色本次从默认里取消」的项，保留用户自己加星的入口，再并入全部角色默认
     */
    void alignUsersAfterRoleQuickNavSave(Collection<Long> userIds,
                                         Collection<Long> cancelledRoleDefaultMenuIds,
                                         Collection<Long> roleValidMenuIds);

    /**
     * 按角色合并默认快捷导航，写入关联用户的数据库记录，并清除 Redis 缓存
     */
    void syncUserQuickNavByRoleId(Long roleId);

    /**
     * 按当前用户各角色默认快捷导航，写入数据库并清除 Redis 缓存
     */
    void syncUserQuickNavFromRoles(Long userId);

    void syncUserQuickNavFromRoles(Collection<Long> userIds);

    /**
     * 菜单展示信息变更后失效快捷导航 Redis 缓存（库内仍只存 menuId，apps 名称等下次读取时重建）
     */
    void evictCacheByMenuIds(Collection<Long> menuIds);

}
