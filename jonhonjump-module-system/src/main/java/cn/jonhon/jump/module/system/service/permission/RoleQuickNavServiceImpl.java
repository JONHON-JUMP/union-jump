package cn.jonhon.jump.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.quicknav.RoleQuickNavRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleQuickNavDO;
import cn.jonhon.jump.module.system.dal.mysql.permission.RoleQuickNavMapper;
import cn.jonhon.jump.module.system.enums.permission.MenuTypeEnum;
import cn.jonhon.jump.module.system.service.user.UserQuickNavService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.ROLE_NOT_EXISTS;

@Service
@Validated
public class RoleQuickNavServiceImpl implements RoleQuickNavService {

    @Resource
    private RoleQuickNavMapper roleQuickNavMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private MenuService menuService;
    @Resource
    @Lazy
    private UserQuickNavService userQuickNavService;

    @Override
    public RoleQuickNavRespVO getRoleQuickNav(Long roleId) {
        validateRoleExists(roleId);
        return new RoleQuickNavRespVO(selectMenuIdsByRoleId(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleQuickNav(Long roleId, List<Long> menuIds) {
        validateRoleExists(roleId);
        Set<Long> allowedMenuIds = getRoleAllowedQuickNavMenuIds(roleId);
        List<Long> oldDefaults = selectMenuIdsByRoleId(roleId);
        List<Long> validMenuIds = CollUtil.isEmpty(menuIds) ? Collections.emptyList()
                : menuIds.stream().filter(allowedMenuIds::contains).distinct().collect(Collectors.toList());
        Set<Long> cancelledDefaults = new LinkedHashSet<>(CollUtil.emptyIfNull(oldDefaults));
        cancelledDefaults.removeAll(validMenuIds);

        roleQuickNavMapper.deleteByRoleId(roleId);
        for (int i = 0; i < validMenuIds.size(); i++) {
            RoleQuickNavDO record = new RoleQuickNavDO();
            record.setRoleId(roleId);
            record.setMenuId(validMenuIds.get(i));
            record.setSort(i);
            roleQuickNavMapper.insert(record);
        }
        Set<Long> userIds = permissionService.getUserRoleIdListByRoleId(Collections.singleton(roleId));
        userQuickNavService.alignUsersAfterRoleQuickNavSave(userIds, cancelledDefaults, validMenuIds);
    }

    @Override
    public List<Long> getUserDefaultMenuIds(Long userId) {
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds).stream()
                .filter(role -> CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()))
                .sorted(Comparator.comparing(RoleDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RoleDO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(roles)) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> merged = new LinkedHashSet<>();
        List<RoleQuickNavDO> savedList = roleQuickNavMapper.selectListByRoleIds(convertSet(roles, RoleDO::getId));
        Map<Long, List<RoleQuickNavDO>> grouped = savedList.stream()
                .collect(Collectors.groupingBy(RoleQuickNavDO::getRoleId));
        for (RoleDO role : roles) {
            List<RoleQuickNavDO> items = grouped.getOrDefault(role.getId(), Collections.emptyList());
            items.stream().map(RoleQuickNavDO::getMenuId).forEach(merged::add);
        }
        return new ArrayList<>(merged);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        roleQuickNavMapper.deleteByRoleId(roleId);
    }

    @Override
    public void deleteByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        roleQuickNavMapper.deleteByRoleIds(roleIds);
    }

    @Override
    public void deleteByMenuId(Long menuId) {
        roleQuickNavMapper.deleteByMenuId(menuId);
    }

    @Override
    public void deleteByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        roleQuickNavMapper.deleteByMenuIds(menuIds);
    }

    @Override
    public boolean existsByMenuId(Long menuId) {
        return menuId != null && CollUtil.isNotEmpty(roleQuickNavMapper.selectListByMenuId(menuId));
    }

    @Override
    public boolean existsByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return false;
        }
        for (Long menuId : menuIds) {
            if (existsByMenuId(menuId)) {
                return true;
            }
        }
        return false;
    }

    private List<Long> selectMenuIdsByRoleId(Long roleId) {
        return roleQuickNavMapper.selectListByRoleId(roleId).stream()
                .map(RoleQuickNavDO::getMenuId)
                .collect(Collectors.toList());
    }

    private RoleDO validateRoleExists(Long roleId) {
        RoleDO role = roleService.getRole(roleId);
        if (role == null) {
            throw exception(ROLE_NOT_EXISTS);
        }
        return role;
    }

    private Set<Long> getRoleAllowedQuickNavMenuIds(Long roleId) {
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(roleId);
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptySet();
        }
        Map<Long, MenuDO> menuMap = convertMap(menuService.getMenuList(), MenuDO::getId);
        List<MenuDO> menus = menuService.filterDisableMenus(menuService.getMenuList(menuIds));
        return menus.stream()
                .filter(menu -> MenuTypeEnum.MENU.getType().equals(menu.getType()))
                .filter(menu -> isMenuShownInSidebar(menu, menuMap))
                .map(MenuDO::getId)
                .collect(Collectors.toSet());
    }

    private boolean isMenuShownInSidebar(MenuDO menu, Map<Long, MenuDO> menuMap) {
        if (Boolean.FALSE.equals(menu.getVisible())) {
            return false;
        }
        Long parentId = menu.getParentId();
        if (parentId == null || MenuDO.ID_ROOT.equals(parentId)) {
            return true;
        }
        MenuDO parent = menuMap.get(parentId);
        if (parent == null) {
            return true;
        }
        return isMenuShownInSidebar(parent, menuMap);
    }

}
