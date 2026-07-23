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

    void deleteByMenuId(Long menuId);

    void deleteByMenuIds(List<Long> menuIds);

    /**
     * 按角色合并默认快捷导航，写入关联用户的数据库记录，并清除 Redis 缓存
     */
    void syncUserQuickNavByRoleId(Long roleId);

    /**
     * 按当前用户各角色默认快捷导航，写入数据库并清除 Redis 缓存
     */
    void syncUserQuickNavFromRoles(Long userId);

    void syncUserQuickNavFromRoles(Collection<Long> userIds);

}
