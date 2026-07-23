package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.UserQuickNavDO;
import cn.jonhon.jump.module.system.dal.mysql.user.UserQuickNavMapper;
import cn.jonhon.jump.module.system.dal.redis.user.UserQuickNavRedisDAO;
import cn.jonhon.jump.module.system.enums.permission.MenuTypeEnum;
import cn.jonhon.jump.module.system.service.permission.MenuService;
import cn.jonhon.jump.module.system.service.permission.PermissionService;
import cn.jonhon.jump.module.system.service.permission.RoleQuickNavService;
import cn.jonhon.jump.module.system.service.permission.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * 用户快捷导航 Service 实现（主系统）
 */
@Service
@Validated
public class UserQuickNavServiceImpl implements UserQuickNavService {

    @Resource
    private UserQuickNavMapper userQuickNavMapper;
    @Resource
    private UserQuickNavRedisDAO userQuickNavRedisDAO;
    @Resource
    private RoleQuickNavService roleQuickNavService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;

    @Override
    public UserQuickNavRespVO getUserQuickNav(Long userId) {
        // Redis 存「过滤后的完整结果」；lockedMenuIds != null 表示已计算，命中则不再扫库
        UserQuickNavRespVO cached = userQuickNavRedisDAO.get(userId);
        if (cached != null && cached.getLockedMenuIds() != null) {
            return cached;
        }

        UserQuickNavRespVO saved = cached != null ? cached : loadUserQuickNavFromDb(userId);
        if (!Boolean.TRUE.equals(saved.getConfigured())) {
            List<Long> roleDefaults = roleQuickNavService.getUserDefaultMenuIds(userId);
            if (CollUtil.isNotEmpty(roleDefaults)) {
                saved = new UserQuickNavRespVO(roleDefaults, false, null);
            }
        }
        Set<Long> allowedMenuIds = getAllowedMenuIds(userId);
        UserQuickNavRespVO result = filterByAllowedMenuIds(saved, allowedMenuIds);
        result.setLockedMenuIds(getLockedMenuIds(userId, allowedMenuIds));
        userQuickNavRedisDAO.set(userId, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserQuickNavRespVO saveUserQuickNav(Long userId, List<Long> menuIds) {
        Set<Long> allowedMenuIds = getAllowedMenuIds(userId);
        List<Long> lockedMenuIds = getLockedMenuIds(userId, allowedMenuIds);
        List<Long> validMenuIds = CollUtil.isEmpty(menuIds) ? Collections.emptyList()
                : menuIds.stream().filter(allowedMenuIds::contains).distinct().collect(Collectors.toList());
        validMenuIds = mergeLockedMenuIds(validMenuIds, lockedMenuIds);

        userQuickNavMapper.deleteByUserId(userId);
        for (int i = 0; i < validMenuIds.size(); i++) {
            UserQuickNavDO record = new UserQuickNavDO();
            record.setUserId(userId);
            record.setMenuId(validMenuIds.get(i));
            record.setSort(i);
            userQuickNavMapper.insert(record);
        }
        UserQuickNavRespVO result = new UserQuickNavRespVO(validMenuIds, true, lockedMenuIds);
        userQuickNavRedisDAO.set(userId, result);
        return result;
    }

    @Override
    public void deleteByMenuId(Long menuId) {
        List<UserQuickNavDO> affectedList = userQuickNavMapper.selectListByMenuId(menuId);
        if (CollUtil.isEmpty(affectedList)) {
            return;
        }
        userQuickNavMapper.deleteByMenuId(menuId);
        userQuickNavRedisDAO.deleteList(convertSet(affectedList, UserQuickNavDO::getUserId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUserQuickNavByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        syncUserQuickNavFromRoles(permissionService.getUserRoleIdListByRoleId(Collections.singleton(roleId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUserQuickNavFromRoles(Long userId) {
        if (userId == null) {
            return;
        }
        syncUserQuickNavFromRoles(Collections.singleton(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUserQuickNavFromRoles(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        for (Long userId : userIds) {
            writeUserQuickNavFromRoles(userId);
        }
    }

    @Override
    public void deleteByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        List<UserQuickNavDO> affectedList = userQuickNavMapper.selectListByMenuIds(menuIds);
        if (CollUtil.isEmpty(affectedList)) {
            return;
        }
        userQuickNavMapper.deleteByMenuIds(menuIds);
        userQuickNavRedisDAO.deleteList(convertSet(affectedList, UserQuickNavDO::getUserId));
    }

    private UserQuickNavRespVO loadUserQuickNavFromDb(Long userId) {
        List<UserQuickNavDO> savedList = userQuickNavMapper.selectListByUserId(userId);
        boolean configured = CollUtil.isNotEmpty(savedList);
        List<Long> menuIds = savedList.stream()
                .map(UserQuickNavDO::getMenuId)
                .collect(Collectors.toList());
        return new UserQuickNavRespVO(menuIds, configured, null);
    }

    private void writeUserQuickNavFromRoles(Long userId) {
        List<Long> existingMenuIds = loadUserQuickNavFromDb(userId).getMenuIds();
        List<Long> roleMenuIds = roleQuickNavService.getUserDefaultMenuIds(userId);
        Set<Long> allowedMenuIds = getAllowedMenuIds(userId);

        LinkedHashSet<Long> merged = new LinkedHashSet<>();
        existingMenuIds.stream().filter(allowedMenuIds::contains).forEach(merged::add);
        if (CollUtil.isNotEmpty(roleMenuIds)) {
            roleMenuIds.stream().filter(allowedMenuIds::contains).forEach(merged::add);
        }
        List<Long> validMenuIds = new ArrayList<>(merged);
        if (validMenuIds.equals(existingMenuIds)) {
            userQuickNavRedisDAO.delete(userId);
            return;
        }
        if (CollUtil.isEmpty(validMenuIds)) {
            userQuickNavRedisDAO.delete(userId);
            return;
        }

        userQuickNavMapper.deleteByUserId(userId);
        for (int i = 0; i < validMenuIds.size(); i++) {
            UserQuickNavDO record = new UserQuickNavDO();
            record.setUserId(userId);
            record.setMenuId(validMenuIds.get(i));
            record.setSort(i);
            userQuickNavMapper.insert(record);
        }
        userQuickNavRedisDAO.delete(userId);
    }

    private List<Long> getLockedMenuIds(Long userId, Set<Long> allowedMenuIds) {
        return roleQuickNavService.getUserDefaultMenuIds(userId).stream()
                .filter(allowedMenuIds::contains)
                .collect(Collectors.toList());
    }

    private List<Long> mergeLockedMenuIds(List<Long> menuIds, List<Long> lockedMenuIds) {
        LinkedHashSet<Long> merged = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(menuIds)) {
            menuIds.forEach(merged::add);
        }
        if (CollUtil.isNotEmpty(lockedMenuIds)) {
            lockedMenuIds.forEach(merged::add);
        }
        return new ArrayList<>(merged);
    }

    private UserQuickNavRespVO filterByAllowedMenuIds(UserQuickNavRespVO saved, Set<Long> allowedMenuIds) {
        List<Long> menuIds = saved.getMenuIds().stream()
                .filter(allowedMenuIds::contains)
                .collect(Collectors.toList());
        return new UserQuickNavRespVO(menuIds, saved.getConfigured(), null);
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

    private Set<Long> getAllowedMenuIds(Long userId) {
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        if (CollUtil.isEmpty(roles)) {
            return Collections.emptySet();
        }

        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
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

}
