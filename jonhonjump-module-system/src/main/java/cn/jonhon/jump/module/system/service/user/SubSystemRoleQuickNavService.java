package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.permission.vo.quicknav.RoleQuickNavRespVO;

import java.util.List;

/**
 * 角色默认快捷导航 Service（外部子系统）
 */
public interface SubSystemRoleQuickNavService {

    RoleQuickNavRespVO getRoleQuickNav(Long roleId);

    void saveRoleQuickNav(Long subSystemId, Long roleId, List<Long> menuIds);

    /**
     * 合并当前用户在指定子系统下各角色的默认快捷导航
     */
    List<Long> getUserDefaultMenuIds(Long userId, Long subSystemId);

    void deleteByRoleId(Long roleId);

    void deleteByRoleIds(List<Long> roleIds);

    void deleteByMenuId(Long menuId);

    void deleteByMenuIds(List<Long> menuIds);

    /** 是否被角色默认快捷导航引用 */
    boolean existsByMenuId(Long menuId);

    /** 是否被角色默认快捷导航引用（任一菜单） */
    boolean existsByMenuIds(List<Long> menuIds);

}
