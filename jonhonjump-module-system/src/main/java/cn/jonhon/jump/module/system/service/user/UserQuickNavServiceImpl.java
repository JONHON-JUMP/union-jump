package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.QuickNavAppItemVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.UserQuickNavDO;
import cn.jonhon.jump.module.system.dal.mysql.user.UserQuickNavMapper;
import cn.jonhon.jump.module.system.dal.redis.user.UserQuickNavRedisDAO;
import cn.jonhon.jump.module.system.enums.permission.MenuTypeEnum;
import cn.jonhon.jump.module.system.service.permission.MenuColorService;
import cn.jonhon.jump.module.system.service.permission.MenuService;
import cn.jonhon.jump.module.system.service.permission.PermissionService;
import cn.jonhon.jump.module.system.service.permission.RoleQuickNavService;
import cn.jonhon.jump.module.system.service.permission.RoleService;
import cn.jonhon.jump.module.system.util.MenuStyleHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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
    @Resource
    private MenuColorService menuColorService;

    @Override
    public UserQuickNavRespVO getUserQuickNav(Long userId) {
        // 跨界导航热路径：先 Redis；未命中再现场重建（含 apps）写回。不拉全量菜单树。
        UserQuickNavRespVO cached = userQuickNavRedisDAO.get(userId);
        if (isQuickNavCacheReady(cached)) {
            return cached;
        }
        return rebuildUserQuickNav(userId);
    }

    private static boolean isQuickNavCacheReady(UserQuickNavRespVO cached) {
        return cached != null
                && cached.getMenuIds() != null
                && cached.getLockedMenuIds() != null
                && cached.getApps() != null;
    }

    private UserQuickNavRespVO rebuildUserQuickNav(Long userId) {
        UserQuickNavRespVO saved = loadUserQuickNavFromDb(userId);
        List<Long> roleDefaults = roleQuickNavService.getUserDefaultMenuIds(userId);
        if (!Boolean.TRUE.equals(saved.getConfigured()) && CollUtil.isNotEmpty(roleDefaults)) {
            saved = new UserQuickNavRespVO(roleDefaults, false, null);
        }
        // 冷路径只校验「个人勾选 ∪ 角色默认」这几个 id，禁止 getMenuList() 全表
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(saved.getMenuIds())) {
            candidates.addAll(saved.getMenuIds());
        }
        if (CollUtil.isNotEmpty(roleDefaults)) {
            candidates.addAll(roleDefaults);
        }
        Set<Long> allowedMenuIds = retainAllowedQuickNavMenuIds(userId, candidates);
        UserQuickNavRespVO result = filterByAllowedMenuIds(saved, allowedMenuIds);
        List<Long> lockedMenuIds = CollUtil.isEmpty(roleDefaults) ? Collections.emptyList()
                : roleDefaults.stream().filter(allowedMenuIds::contains).collect(Collectors.toList());
        List<Long> displayMenuIds = mergeLockedMenuIds(result.getMenuIds(), lockedMenuIds);
        result.setMenuIds(displayMenuIds);
        result.setLockedMenuIds(lockedMenuIds);
        result.setApps(buildQuickNavApps(displayMenuIds));
        userQuickNavRedisDAO.set(userId, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserQuickNavRespVO saveUserQuickNav(Long userId, List<Long> menuIds) {
        List<Long> roleDefaults = roleQuickNavService.getUserDefaultMenuIds(userId);
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(menuIds)) {
            candidates.addAll(menuIds);
        }
        if (CollUtil.isNotEmpty(roleDefaults)) {
            candidates.addAll(roleDefaults);
        }
        Set<Long> allowedMenuIds = retainAllowedQuickNavMenuIds(userId, candidates);
        List<Long> lockedMenuIds = CollUtil.isEmpty(roleDefaults) ? Collections.emptyList()
                : roleDefaults.stream().filter(allowedMenuIds::contains).collect(Collectors.toList());
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
        result.setApps(buildQuickNavApps(validMenuIds));
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
    public boolean existsByMenuId(Long menuId) {
        return menuId != null && CollUtil.isNotEmpty(userQuickNavMapper.selectListByMenuId(menuId));
    }

    @Override
    public boolean existsByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return false;
        }
        return CollUtil.isNotEmpty(userQuickNavMapper.selectListByMenuIds(menuIds));
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

    @Override
    public void removeMenusForUsers(Collection<Long> userIds, Collection<Long> menuIds) {
        if (CollUtil.isEmpty(userIds) || CollUtil.isEmpty(menuIds)) {
            return;
        }
        userQuickNavMapper.deleteByUserIdsAndMenuIds(userIds, menuIds);
        userQuickNavRedisDAO.deleteList(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void alignUsersAfterRoleQuickNavSave(Collection<Long> userIds,
                                                Collection<Long> cancelledRoleDefaultMenuIds,
                                                Collection<Long> roleValidMenuIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        Set<Long> cancelled = CollUtil.isEmpty(cancelledRoleDefaultMenuIds)
                ? Collections.emptySet() : new LinkedHashSet<>(cancelledRoleDefaultMenuIds);
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            List<Long> existingMenuIds = loadUserQuickNavFromDb(userId).getMenuIds();
            List<Long> roleDefaults = roleQuickNavService.getUserDefaultMenuIds(userId);
            LinkedHashSet<Long> candidates = new LinkedHashSet<>();
            CollUtil.emptyIfNull(existingMenuIds).forEach(candidates::add);
            CollUtil.emptyIfNull(roleDefaults).forEach(candidates::add);
            Set<Long> userAllowed = retainAllowedQuickNavMenuIds(userId, candidates);
            LinkedHashSet<Long> next = new LinkedHashSet<>();
            for (Long menuId : CollUtil.emptyIfNull(existingMenuIds)) {
                if (menuId == null || !userAllowed.contains(menuId)) {
                    continue;
                }
                if (cancelled.contains(menuId)) {
                    continue;
                }
                next.add(menuId);
            }
            if (CollUtil.isNotEmpty(roleDefaults)) {
                roleDefaults.stream().filter(userAllowed::contains).forEach(next::add);
            }
            List<Long> validMenuIds = new ArrayList<>(next);
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
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        CollUtil.emptyIfNull(existingMenuIds).forEach(candidates::add);
        CollUtil.emptyIfNull(roleMenuIds).forEach(candidates::add);
        Set<Long> allowedMenuIds = retainAllowedQuickNavMenuIds(userId, candidates);

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

    /**
     * 只校验候选 menuId 是否可作为快捷导航，不拉全表菜单。
     * 超管也不走 getRoleMenuListByRoleId（其内部会对超管 getMenuList 全表）。
     */
    private Set<Long> retainAllowedQuickNavMenuIds(Long userId, Collection<Long> candidateIds) {
        if (CollUtil.isEmpty(candidateIds)) {
            return Collections.emptySet();
        }
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        if (CollUtil.isEmpty(roles)) {
            return Collections.emptySet();
        }
        Set<Long> enabledRoleIds = convertSet(roles, RoleDO::getId);
        boolean superAdmin = roleService.hasAnySuperAdmin(enabledRoleIds);

        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        for (Long menuId : candidateIds) {
            if (menuId != null) {
                candidates.add(menuId);
            }
        }
        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }
        if (!superAdmin) {
            Set<Long> roleMenuIds = permissionService.getRoleMenuListByRoleId(enabledRoleIds);
            if (CollUtil.isEmpty(roleMenuIds)) {
                return Collections.emptySet();
            }
            candidates.removeIf(id -> !roleMenuIds.contains(id));
            if (candidates.isEmpty()) {
                return Collections.emptySet();
            }
        }

        Map<Long, MenuDO> menuMap = loadMenusWithAncestors(candidates);
        List<MenuDO> scopedMenus = new ArrayList<>(menuMap.values());
        Set<Long> enabledIds = convertSet(menuService.filterDisableMenus(scopedMenus), MenuDO::getId);
        return candidates.stream()
                .map(menuMap::get)
                .filter(Objects::nonNull)
                .filter(menu -> MenuTypeEnum.MENU.getType().equals(menu.getType()))
                .filter(menu -> enabledIds.contains(menu.getId()))
                .filter(menu -> isMenuShownInSidebar(menu, menuMap))
                .map(MenuDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 按候选 id 批量查菜单，并补齐祖先，供禁用/可见性判断 */
    private Map<Long, MenuDO> loadMenusWithAncestors(Collection<Long> ids) {
        Map<Long, MenuDO> menuMap = new HashMap<>();
        Set<Long> pending = new LinkedHashSet<>(ids);
        while (CollUtil.isNotEmpty(pending)) {
            List<MenuDO> batch = menuService.getMenuList(pending);
            pending.clear();
            if (CollUtil.isEmpty(batch)) {
                break;
            }
            for (MenuDO menu : batch) {
                if (menu == null || menu.getId() == null || menuMap.containsKey(menu.getId())) {
                    continue;
                }
                menuMap.put(menu.getId(), menu);
                Long parentId = menu.getParentId();
                if (parentId != null && !MenuDO.ID_ROOT.equals(parentId) && !menuMap.containsKey(parentId)) {
                    pending.add(parentId);
                }
            }
        }
        return menuMap;
    }

    /**
     * 仅按快捷导航勾选的菜单 id 组装可渲染卡片，不依赖前端等主菜单全树（后台故意延迟 8s 预热）。
     */
    private List<QuickNavAppItemVO> buildQuickNavApps(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptyList();
        }
        Map<Long, MenuDO> menuMap = loadMenusWithAncestors(menuIds);
        Set<Long> styleIds = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            MenuDO firstLevel = findFirstLevelMenu(menuMap.get(menuId), menuMap);
            if (firstLevel != null && firstLevel.getStyleId() != null) {
                styleIds.add(firstLevel.getStyleId());
            }
        }
        Map<Long, MenuColorDO> colorMap = menuColorService.getMenuColorMap(styleIds);

        List<QuickNavAppItemVO> apps = new ArrayList<>();
        for (Long menuId : menuIds) {
            MenuDO menu = menuMap.get(menuId);
            if (menu == null || !MenuTypeEnum.MENU.getType().equals(menu.getType())) {
                continue;
            }
            QuickNavAppItemVO app = new QuickNavAppItemVO();
            app.setMenuId(menuId);
            app.setName(menu.getName());
            app.setIcon(menu.getIcon());
            app.setManualUrl(menu.getManualUrl());
            app.setPath(buildMainMenuPath(menuId, menuMap));
            MenuDO firstLevel = findFirstLevelMenu(menu, menuMap);
            MenuColorDO style = MenuStyleHelper.resolveFirstLevelStyle(
                    firstLevel != null ? firstLevel.getStyleId() : menu.getStyleId(), colorMap);
            app.setColor(style.getColor());
            app.setShape(style.getShape());
            apps.add(app);
        }
        return apps;
    }

    private MenuDO findFirstLevelMenu(MenuDO menu, Map<Long, MenuDO> menuMap) {
        MenuDO current = menu;
        int guard = 0;
        while (current != null && !MenuStyleHelper.isFirstLevelMenu(current.getParentId()) && guard++ < 32) {
            current = menuMap.get(current.getParentId());
        }
        return current;
    }

    /**
     * 与前端 quickNavFromRoutes.resolveRoutePath 对齐：相对 path 拼接，绝对/外链覆盖。
     */
    private String buildMainMenuPath(Long menuId, Map<Long, MenuDO> menuMap) {
        List<MenuDO> chain = new ArrayList<>();
        Long current = menuId;
        int guard = 0;
        while (current != null && !MenuDO.ID_ROOT.equals(current) && guard++ < 32) {
            MenuDO item = menuMap.get(current);
            if (item == null) {
                break;
            }
            chain.add(0, item);
            current = item.getParentId();
        }
        String fullPath = "";
        for (MenuDO item : chain) {
            Integer type = item.getType();
            if (!MenuTypeEnum.DIR.getType().equals(type) && !MenuTypeEnum.MENU.getType().equals(type)) {
                continue;
            }
            String routePath = item.getPath();
            if (StrUtil.isBlank(routePath)) {
                continue;
            }
            if (isExternalLink(routePath) || routePath.charAt(0) == '/') {
                fullPath = routePath;
            } else if (StrUtil.isBlank(fullPath)) {
                fullPath = "/" + routePath;
            } else {
                fullPath = (fullPath + "/" + routePath).replaceAll("/+", "/");
            }
        }
        return StrUtil.isBlank(fullPath) ? "/" : fullPath;
    }

    private static boolean isExternalLink(String path) {
        return StrUtil.startWithIgnoreCase(path, "http://")
                || StrUtil.startWithIgnoreCase(path, "https://")
                || StrUtil.startWithIgnoreCase(path, "mailto:")
                || StrUtil.startWithIgnoreCase(path, "tel:");
    }

}
