package cn.jonhon.jump.module.system.service.permission;

import cn.jonhon.jump.module.system.controller.admin.permission.vo.quicknav.RoleQuickNavRespVO;

import java.util.List;

/**
 * 角色默认快捷导航 Service（主系统）
 */
public interface RoleQuickNavService {

    RoleQuickNavRespVO getRoleQuickNav(Long roleId);

    void saveRoleQuickNav(Long roleId, List<Long> menuIds);

    /**
     * 合并当前用户各角色的默认快捷导航（按角色 sort 顺序去重）
     */
    List<Long> getUserDefaultMenuIds(Long userId);

    void deleteByRoleId(Long roleId);

    void deleteByRoleIds(List<Long> roleIds);

    void deleteByMenuId(Long menuId);

    void deleteByMenuIds(List<Long> menuIds);

    /** 是否被角色默认快捷导航引用 */
    boolean existsByMenuId(Long menuId);

    /** 是否被角色默认快捷导航引用（任一菜单） */
    boolean existsByMenuIds(List<Long> menuIds);

}
