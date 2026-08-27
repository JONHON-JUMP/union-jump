package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.QuickNavAppItemVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemMenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleQuickNavDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserQuickNavDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserRoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMenuMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleQuickNavMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUserQuickNavMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUserRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.dal.redis.user.SubSystemUserQuickNavRedisDAO;
import cn.jonhon.jump.module.system.dal.redis.user.SubSystemUserQuickNavRedisDAO.SubSystemQuickNavCacheKey;
import cn.jonhon.jump.module.system.service.permission.MenuColorService;
import cn.jonhon.jump.module.system.util.MenuStyleHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * 用户外部子系统快捷导航 Service 实现
 */
@Service
@Validated
public class SubSystemUserQuickNavServiceImpl implements SubSystemUserQuickNavService {

    @Resource
    private SubSystemUserQuickNavMapper subSystemUserQuickNavMapper;
    @Resource
    private SubSystemRoleQuickNavMapper subSystemRoleQuickNavMapper;
    @Resource
    private SubSystemUserQuickNavRedisDAO subSystemUserQuickNavRedisDAO;
    @Resource
    private SubSystemUsersService subSystemUsersService;
    @Resource
    private SubSystemRoleQuickNavService subSystemRoleQuickNavService;
    @Resource
    private SubSystemUserRoleMapper subSystemUserRoleMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private OAuth2ClientMapper oauth2ClientMapper;
    @Resource
    private MenuColorService menuColorService;

    @Override
    public SubSystemUserQuickNavRespVO getUserQuickNav(Long userId, Long subSystemId) {
        // 跨界导航热路径（菜单在主库）：先 Redis；未命中再现场重建（含 apps）写回。不拉 my-menus 全树。
        SubSystemUserQuickNavRespVO cached = subSystemUserQuickNavRedisDAO.get(userId, subSystemId);
        if (isQuickNavCacheReady(cached)) {
            return cached;
        }
        return rebuildUserQuickNav(userId, subSystemId);
    }

    private static boolean isQuickNavCacheReady(SubSystemUserQuickNavRespVO cached) {
        return cached != null
                && cached.getMenuIds() != null
                && cached.getLockedMenuIds() != null
                && cached.getApps() != null;
    }

    private SubSystemUserQuickNavRespVO rebuildUserQuickNav(Long userId, Long subSystemId) {
        // 角色默认变更已在 saveRoleQuickNav → alignUsers 处理；此处不再 heal，
        // 否则会把「可配但非角色默认」误判为已取消，个人加星后立刻被清掉
        SubSystemUserQuickNavRespVO saved = loadUserQuickNavFromDb(userId, subSystemId);
        List<Long> roleDefaults = subSystemRoleQuickNavService.getUserDefaultMenuIds(userId, subSystemId);
        if (!Boolean.TRUE.equals(saved.getConfigured()) && CollUtil.isNotEmpty(roleDefaults)) {
            saved = new SubSystemUserQuickNavRespVO(roleDefaults, false, null);
        }
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(saved.getMenuIds())) {
            candidates.addAll(saved.getMenuIds());
        }
        if (CollUtil.isNotEmpty(roleDefaults)) {
            candidates.addAll(roleDefaults);
        }
        Set<Long> allowedMenuIds = subSystemUsersService.retainAllowedQuickNavMenuIds(
                userId, subSystemId, candidates);
        SubSystemUserQuickNavRespVO result = filterByAllowedMenuIds(saved, allowedMenuIds);
        List<Long> lockedMenuIds = CollUtil.isEmpty(roleDefaults) ? Collections.emptyList()
                : roleDefaults.stream().filter(allowedMenuIds::contains).collect(Collectors.toList());
        // 角色默认快捷导航为锁定项：展示/apps 必须并入，否则个人已 configured 时角色新配的入口不出现
        List<Long> displayMenuIds = mergeLockedMenuIds(result.getMenuIds(), lockedMenuIds);
        result.setMenuIds(displayMenuIds);
        result.setLockedMenuIds(lockedMenuIds);
        result.setApps(buildQuickNavApps(subSystemId, displayMenuIds));
        subSystemUserQuickNavRedisDAO.set(userId, subSystemId, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubSystemUserQuickNavRespVO saveUserQuickNav(Long userId, Long subSystemId, List<Long> menuIds) {
        List<Long> roleDefaults = subSystemRoleQuickNavService.getUserDefaultMenuIds(userId, subSystemId);
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(menuIds)) {
            candidates.addAll(menuIds);
        }
        if (CollUtil.isNotEmpty(roleDefaults)) {
            candidates.addAll(roleDefaults);
        }
        Set<Long> allowedMenuIds = subSystemUsersService.retainAllowedQuickNavMenuIds(
                userId, subSystemId, candidates);
        List<Long> lockedMenuIds = CollUtil.isEmpty(roleDefaults) ? Collections.emptyList()
                : roleDefaults.stream().filter(allowedMenuIds::contains).collect(Collectors.toList());
        List<Long> validMenuIds = CollUtil.isEmpty(menuIds) ? Collections.emptyList()
                : menuIds.stream().filter(allowedMenuIds::contains).distinct().collect(Collectors.toList());
        validMenuIds = mergeLockedMenuIds(validMenuIds, lockedMenuIds);

        subSystemUserQuickNavMapper.deleteByUserIdAndSubSystemId(userId, subSystemId);
        for (int i = 0; i < validMenuIds.size(); i++) {
            SubSystemUserQuickNavDO record = new SubSystemUserQuickNavDO();
            record.setUserId(userId);
            record.setSubSystemId(subSystemId);
            record.setMenuId(validMenuIds.get(i));
            record.setSort(i);
            subSystemUserQuickNavMapper.insert(record);
        }
        SubSystemUserQuickNavRespVO result = new SubSystemUserQuickNavRespVO(validMenuIds, true, lockedMenuIds);
        result.setApps(buildQuickNavApps(subSystemId, validMenuIds));
        subSystemUserQuickNavRedisDAO.set(userId, subSystemId, result);
        return result;
    }

    @Override
    public void deleteByMenuId(Long menuId) {
        List<SubSystemUserQuickNavDO> affectedList = subSystemUserQuickNavMapper.selectListByMenuId(menuId);
        if (CollUtil.isEmpty(affectedList)) {
            return;
        }
        subSystemUserQuickNavMapper.deleteByMenuId(menuId);
        subSystemUserQuickNavRedisDAO.deleteList(convertCacheKeys(affectedList));
    }

    @Override
    public boolean existsByMenuId(Long menuId) {
        return menuId != null && CollUtil.isNotEmpty(subSystemUserQuickNavMapper.selectListByMenuId(menuId));
    }

    @Override
    public boolean existsByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return false;
        }
        return CollUtil.isNotEmpty(subSystemUserQuickNavMapper.selectListByMenuIds(menuIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUserQuickNavByRoleId(Long roleId, Long subSystemId) {
        if (roleId == null || subSystemId == null) {
            return;
        }
        List<SubSystemUserRoleDO> userRoles = subSystemUserRoleMapper.selectListByRoleId(roleId);
        if (CollUtil.isEmpty(userRoles)) {
            return;
        }
        Set<Long> subUserIds = convertSet(userRoles, SubSystemUserRoleDO::getUserId);
        List<SubSystemUsersDO> subUsers = subSystemUsersMapper.selectBatchIds(subUserIds);
        Set<Long> mainUserIds = subUsers.stream()
                .filter(user -> Objects.equals(user.getSubSystemId(), subSystemId))
                .map(SubSystemUsersDO::getMainUserId)
                .collect(Collectors.toSet());
        syncUserQuickNavFromRoles(mainUserIds, subSystemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUserQuickNavFromRoles(Long userId, Long subSystemId) {
        if (userId == null || subSystemId == null) {
            return;
        }
        syncUserQuickNavFromRoles(Collections.singleton(userId), subSystemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUserQuickNavFromRoles(Collection<Long> userIds, Long subSystemId) {
        if (CollUtil.isEmpty(userIds) || subSystemId == null) {
            return;
        }
        for (Long userId : userIds) {
            writeUserQuickNavFromRoles(userId, subSystemId);
        }
    }

    @Override
    public void deleteByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        List<SubSystemUserQuickNavDO> affectedList = subSystemUserQuickNavMapper.selectListByMenuIds(menuIds);
        if (CollUtil.isEmpty(affectedList)) {
            return;
        }
        subSystemUserQuickNavMapper.deleteByMenuIds(menuIds);
        subSystemUserQuickNavRedisDAO.deleteList(convertCacheKeys(affectedList));
    }

    @Override
    public void removeMenusForUsers(Long subSystemId, Collection<Long> userIds, Collection<Long> menuIds) {
        if (subSystemId == null || CollUtil.isEmpty(userIds) || CollUtil.isEmpty(menuIds)) {
            return;
        }
        subSystemUserQuickNavMapper.deleteByUserIdsAndSubSystemIdAndMenuIds(userIds, subSystemId, menuIds);
        Set<SubSystemQuickNavCacheKey> keys = userIds.stream()
                .filter(Objects::nonNull)
                .map(uid -> SubSystemUserQuickNavRedisDAO.cacheKey(uid, subSystemId))
                .collect(Collectors.toSet());
        subSystemUserQuickNavRedisDAO.deleteList(keys);
    }

    @Override
    public void evictCacheByMenuIds(Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        Set<SubSystemQuickNavCacheKey> keys = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            if (menuId == null) {
                continue;
            }
            List<SubSystemUserQuickNavDO> personal = subSystemUserQuickNavMapper.selectListByMenuId(menuId);
            if (CollUtil.isNotEmpty(personal)) {
                keys.addAll(convertCacheKeys(personal));
            }
            List<SubSystemRoleQuickNavDO> roleRefs = subSystemRoleQuickNavMapper.selectListByMenuId(menuId);
            if (CollUtil.isEmpty(roleRefs)) {
                continue;
            }
            for (SubSystemRoleQuickNavDO roleRef : roleRefs) {
                Long roleId = roleRef.getRoleId();
                if (roleId == null) {
                    continue;
                }
                List<SubSystemUserRoleDO> userRoles = subSystemUserRoleMapper.selectListByRoleId(roleId);
                if (CollUtil.isEmpty(userRoles)) {
                    continue;
                }
                Set<Long> subUserIds = convertSet(userRoles, SubSystemUserRoleDO::getUserId);
                List<SubSystemUsersDO> subUsers = subSystemUsersMapper.selectBatchIds(subUserIds);
                for (SubSystemUsersDO subUser : subUsers) {
                    if (subUser == null || subUser.getMainUserId() == null || subUser.getSubSystemId() == null) {
                        continue;
                    }
                    keys.add(SubSystemUserQuickNavRedisDAO.cacheKey(subUser.getMainUserId(), subUser.getSubSystemId()));
                }
            }
        }
        if (CollUtil.isNotEmpty(keys)) {
            subSystemUserQuickNavRedisDAO.deleteList(keys);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void alignUsersAfterRoleQuickNavSave(Long subSystemId, Collection<Long> userIds,
                                                Collection<Long> cancelledRoleDefaultMenuIds,
                                                Collection<Long> roleValidMenuIds) {
        if (subSystemId == null || CollUtil.isEmpty(userIds)) {
            return;
        }
        Set<Long> cancelled = CollUtil.isEmpty(cancelledRoleDefaultMenuIds)
                ? Collections.emptySet() : new LinkedHashSet<>(cancelledRoleDefaultMenuIds);
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            List<Long> existingMenuIds = loadUserQuickNavFromDb(userId, subSystemId).getMenuIds();
            List<Long> roleDefaults = subSystemRoleQuickNavService.getUserDefaultMenuIds(userId, subSystemId);
            LinkedHashSet<Long> candidates = new LinkedHashSet<>();
            CollUtil.emptyIfNull(existingMenuIds).forEach(candidates::add);
            CollUtil.emptyIfNull(roleDefaults).forEach(candidates::add);
            Set<Long> userAllowed = subSystemUsersService.retainAllowedQuickNavMenuIds(
                    userId, subSystemId, candidates);
            LinkedHashSet<Long> next = new LinkedHashSet<>();
            for (Long menuId : CollUtil.emptyIfNull(existingMenuIds)) {
                if (menuId == null || !userAllowed.contains(menuId)) {
                    continue;
                }
                // 仅去掉本角色本次从默认里取消的项；用户自己加星的入口保留
                if (cancelled.contains(menuId)) {
                    continue;
                }
                next.add(menuId);
            }
            if (CollUtil.isNotEmpty(roleDefaults)) {
                roleDefaults.stream().filter(userAllowed::contains).forEach(next::add);
            }
            List<Long> validMenuIds = new ArrayList<>(next);
            subSystemUserQuickNavMapper.deleteByUserIdAndSubSystemId(userId, subSystemId);
            for (int i = 0; i < validMenuIds.size(); i++) {
                SubSystemUserQuickNavDO record = new SubSystemUserQuickNavDO();
                record.setUserId(userId);
                record.setSubSystemId(subSystemId);
                record.setMenuId(validMenuIds.get(i));
                record.setSort(i);
                subSystemUserQuickNavMapper.insert(record);
            }
            subSystemUserQuickNavRedisDAO.delete(userId, subSystemId);
        }
    }

    private SubSystemUserQuickNavRespVO loadUserQuickNavFromDb(Long userId, Long subSystemId) {
        List<SubSystemUserQuickNavDO> savedList = subSystemUserQuickNavMapper
                .selectListByUserIdAndSubSystemId(userId, subSystemId);
        boolean configured = CollUtil.isNotEmpty(savedList);
        List<Long> menuIds = savedList.stream()
                .map(SubSystemUserQuickNavDO::getMenuId)
                .collect(Collectors.toList());
        return new SubSystemUserQuickNavRespVO(menuIds, configured, null);
    }

    private void writeUserQuickNavFromRoles(Long userId, Long subSystemId) {
        List<Long> existingMenuIds = loadUserQuickNavFromDb(userId, subSystemId).getMenuIds();
        List<Long> roleMenuIds = subSystemRoleQuickNavService.getUserDefaultMenuIds(userId, subSystemId);
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        CollUtil.emptyIfNull(existingMenuIds).forEach(candidates::add);
        CollUtil.emptyIfNull(roleMenuIds).forEach(candidates::add);
        Set<Long> allowedMenuIds = subSystemUsersService.retainAllowedQuickNavMenuIds(
                userId, subSystemId, candidates);

        LinkedHashSet<Long> merged = new LinkedHashSet<>();
        existingMenuIds.stream().filter(allowedMenuIds::contains).forEach(merged::add);
        if (CollUtil.isNotEmpty(roleMenuIds)) {
            roleMenuIds.stream().filter(allowedMenuIds::contains).forEach(merged::add);
        }
        List<Long> validMenuIds = new ArrayList<>(merged);
        if (validMenuIds.equals(existingMenuIds)) {
            return;
        }
        if (CollUtil.isEmpty(validMenuIds)) {
            return;
        }

        subSystemUserQuickNavMapper.deleteByUserIdAndSubSystemId(userId, subSystemId);
        for (int i = 0; i < validMenuIds.size(); i++) {
            SubSystemUserQuickNavDO record = new SubSystemUserQuickNavDO();
            record.setUserId(userId);
            record.setSubSystemId(subSystemId);
            record.setMenuId(validMenuIds.get(i));
            record.setSort(i);
            subSystemUserQuickNavMapper.insert(record);
        }
        subSystemUserQuickNavRedisDAO.delete(userId, subSystemId);
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

    private SubSystemUserQuickNavRespVO filterByAllowedMenuIds(SubSystemUserQuickNavRespVO saved,
                                                               Set<Long> allowedMenuIds) {
        List<Long> menuIds = saved.getMenuIds().stream()
                .filter(allowedMenuIds::contains)
                .collect(Collectors.toList());
        return new SubSystemUserQuickNavRespVO(menuIds, saved.getConfigured(), null);
    }

    /**
     * 仅按快捷导航勾选的菜单 id 组装可渲染卡片，不拉整棵 my-menus。
     */
    private List<QuickNavAppItemVO> buildQuickNavApps(Long subSystemId, List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds) || subSystemId == null) {
            return Collections.emptyList();
        }
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null || subSystem.getOauth2ClientId() == null) {
            return Collections.emptyList();
        }
        OAuth2ClientDO client = oauth2ClientMapper.selectById(subSystem.getOauth2ClientId());
        if (client == null || StrUtil.isBlank(client.getClientId())) {
            return Collections.emptyList();
        }
        String clientId = client.getClientId();
        // 只加载勾选菜单及其祖先 path，不拉该子系统全量菜单
        Map<Long, SubSystemMenuDO> menuMap = loadMenusWithAncestors(menuIds);
        // 颜色只配在一级菜单，快捷导航页菜单需沿父链继承，否则全落默认蓝
        Set<Long> styleIds = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            SubSystemMenuDO firstLevel = findFirstLevelMenu(menuMap.get(menuId), menuMap);
            if (firstLevel != null && firstLevel.getStyleId() != null) {
                styleIds.add(firstLevel.getStyleId());
            }
        }
        Map<Long, MenuColorDO> colorMap = menuColorService.getMenuColorMap(styleIds);

        List<QuickNavAppItemVO> apps = new ArrayList<>();
        for (Long menuId : menuIds) {
            SubSystemMenuDO menu = menuMap.get(menuId);
            if (menu == null || !"C".equals(menu.getType())) {
                continue;
            }
            QuickNavAppItemVO app = new QuickNavAppItemVO();
            app.setMenuId(menuId);
            app.setName(menu.getMenuName());
            app.setIcon(menu.getIcon());
            app.setManualUrl(menu.getManualUrl());
            app.setPath(buildPortalMenuPath(clientId, menuId, menuMap));
            SubSystemMenuDO firstLevel = findFirstLevelMenu(menu, menuMap);
            MenuColorDO style = MenuStyleHelper.resolveFirstLevelStyle(
                    firstLevel != null ? firstLevel.getStyleId() : menu.getStyleId(), colorMap);
            app.setColor(style.getColor());
            app.setShape(style.getShape());
            apps.add(app);
        }
        return apps;
    }

    private Map<Long, SubSystemMenuDO> loadMenusWithAncestors(Collection<Long> ids) {
        Map<Long, SubSystemMenuDO> menuMap = new HashMap<>();
        Set<Long> pending = new LinkedHashSet<>(ids);
        while (CollUtil.isNotEmpty(pending)) {
            List<SubSystemMenuDO> batch = subSystemMenuMapper.selectListByIds(pending);
            pending.clear();
            if (CollUtil.isEmpty(batch)) {
                break;
            }
            for (SubSystemMenuDO menu : batch) {
                if (menu == null || menu.getId() == null || menuMap.containsKey(menu.getId())) {
                    continue;
                }
                menuMap.put(menu.getId(), menu);
                Long parentId = menu.getParentId();
                if (parentId != null && parentId != 0L && !menuMap.containsKey(parentId)) {
                    pending.add(parentId);
                }
            }
        }
        return menuMap;
    }

    /**
     * 沿 parentId 找到一级菜单（parentId=0），用于继承图标颜色。
     */
    private SubSystemMenuDO findFirstLevelMenu(SubSystemMenuDO menu, Map<Long, SubSystemMenuDO> menuMap) {
        SubSystemMenuDO current = menu;
        int guard = 0;
        while (current != null && !MenuStyleHelper.isFirstLevelMenu(current.getParentId()) && guard++ < 32) {
            current = menuMap.get(current.getParentId());
        }
        return current;
    }

    private String buildPortalMenuPath(String clientId, Long menuId, Map<Long, SubSystemMenuDO> menuMap) {
        SubSystemMenuDO leaf = menuMap.get(menuId);
        // Camstar/http：壳 path 与 4200 地址栏一致，含上级目录（如 15/192/168/...）
        if (leaf != null && StrUtil.isNotBlank(leaf.getPath())) {
            String leafPath = StrUtil.removePrefix(leaf.getPath(), "/");
            if (StrUtil.startWithIgnoreCase(leafPath, "http://")
                    || StrUtil.startWithIgnoreCase(leafPath, "https://")
                    || leafPath.contains(":")
                    || leafPath.matches(".*\\d+/\\d+/\\d+/\\d+.*")) {
                List<String> segments = new ArrayList<>();
                Long current = leaf.getParentId();
                int guard = 0;
                while (current != null && current != 0L && guard++ < 32) {
                    SubSystemMenuDO item = menuMap.get(current);
                    if (item == null) {
                        break;
                    }
                    if (("M".equals(item.getType()) || "C".equals(item.getType())) && StrUtil.isNotBlank(item.getPath())) {
                        String p = StrUtil.removePrefix(item.getPath(), "/");
                        if (!StrUtil.startWithIgnoreCase(p, "http://") && !StrUtil.startWithIgnoreCase(p, "https://")
                                && !p.contains(":") && !p.matches(".*\\d+/\\d+/\\d+/\\d+.*")) {
                            segments.add(0, p);
                        }
                    }
                    current = item.getParentId();
                }
                segments.add(toMesRoutePathSegment(leaf));
                return "/portal/" + clientId + "/" + String.join("/", segments);
            }
        }
        List<String> segments = new ArrayList<>();
        Long current = menuId;
        while (current != null && current != 0L) {
            SubSystemMenuDO item = menuMap.get(current);
            if (item == null) {
                break;
            }
            if (("M".equals(item.getType()) || "C".equals(item.getType())) && StrUtil.isNotBlank(item.getPath())) {
                segments.add(0, toMesRoutePathSegment(item));
            }
            current = item.getParentId();
        }
        if (segments.isEmpty()) {
            return "/portal/" + clientId;
        }
        return "/portal/" + clientId + "/" + String.join("/", segments);
    }

    /**
     * 门户壳 path 段：与前端 encodeHttpToMesPath 对齐。
     * 点分 IP + 端口 → 192.168.240.12794200/...（勿再用点改斜杠，否则快捷导航与侧栏 path 不一致打不开）
     */
    private String toMesRoutePathSegment(SubSystemMenuDO item) {
        String raw = StrUtil.removePrefix(StrUtil.blankToDefault(item.getPath(), ""), "/");
        if (StrUtil.isBlank(raw)) {
            return "menu" + item.getId();
        }
        String shell = encodeHttpOrIpPortToShell(raw);
        if (StrUtil.isNotBlank(shell)) {
            return shell;
        }
        if (raw.contains(":")) {
            java.util.regex.Matcher dotted = java.util.regex.Pattern
                    .compile("^(\\d{1,3}(?:\\.\\d{1,3}){3}):(\\d{2,5})(/.*)?$")
                    .matcher(raw);
            if (dotted.matches()) {
                String after = dotted.group(3);
                return dotted.group(1) + "9" + dotted.group(2) + (after != null ? after : "");
            }
            return raw.replace(":", "/");
        }
        return raw.replace('.', '_');
    }

    /** http://192.168.240.127:4200/a → 192.168.240.12794200/a；与前端 portalRoute.encodeHttpToMesPath 一致 */
    private static String encodeHttpOrIpPortToShell(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            String withProto = raw;
            if (!StrUtil.startWithIgnoreCase(raw, "http://") && !StrUtil.startWithIgnoreCase(raw, "https://")) {
                if (!raw.matches("^\\d{1,3}(\\.\\d{1,3}){3}:\\d{2,5}(/.*)?$")) {
                    return null;
                }
                withProto = "http://" + raw;
            }
            java.net.URI u = java.net.URI.create(withProto);
            String host = u.getHost();
            int port = u.getPort();
            if (host != null && host.matches("\\d{1,3}(\\.\\d{1,3}){3}") && port > 0) {
                String path = u.getPath();
                if (path == null || "/".equals(path)) {
                    path = "";
                } else {
                    path = path.replaceAll("^/+", "").replaceAll("/+$", "");
                }
                String q = u.getRawQuery();
                return host + "9" + port
                        + (StrUtil.isNotBlank(path) ? "/" + path : "")
                        + (StrUtil.isNotBlank(q) ? "?" + q : "");
            }
            // 非点分 IP 主机：保留旧斜杠编码
            return raw.replace("https://", "")
                    .replace("http://", "")
                    .replace("www.", "")
                    .replace(".", "/")
                    .replace(":", "/");
        } catch (Exception ignored) {
            return null;
        }
    }

    private Set<SubSystemQuickNavCacheKey> convertCacheKeys(List<SubSystemUserQuickNavDO> affectedList) {
        return convertSet(affectedList, item -> SubSystemUserQuickNavRedisDAO.cacheKey(item.getUserId(), item.getSubSystemId()));
    }

}
