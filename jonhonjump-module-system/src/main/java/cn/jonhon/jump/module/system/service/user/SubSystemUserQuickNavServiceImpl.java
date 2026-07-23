package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.QuickNavAppItemVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemMenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserQuickNavDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserRoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMenuMapper;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
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
        SubSystemUserQuickNavRespVO cached = subSystemUserQuickNavRedisDAO.get(userId, subSystemId);
        // lockedMenuIds != null：已是过滤后的完整视图；apps 每次按一级菜单样式重算，避免缓存旧颜色
        if (cached != null && cached.getLockedMenuIds() != null) {
            cached.setApps(buildQuickNavApps(subSystemId, cached.getMenuIds()));
            return cached;
        }

        SubSystemUserQuickNavRespVO saved = cached != null ? cached : loadUserQuickNavFromDb(userId, subSystemId);
        if (!Boolean.TRUE.equals(saved.getConfigured())) {
            List<Long> roleDefaults = subSystemRoleQuickNavService.getUserDefaultMenuIds(userId, subSystemId);
            if (CollUtil.isNotEmpty(roleDefaults)) {
                saved = new SubSystemUserQuickNavRespVO(roleDefaults, false, null);
            }
        }
        Set<Long> allowedMenuIds = subSystemUsersService.getAllowedQuickNavMenuIds(userId, subSystemId);
        SubSystemUserQuickNavRespVO result = filterByAllowedMenuIds(saved, allowedMenuIds);
        result.setLockedMenuIds(getLockedMenuIds(userId, subSystemId, allowedMenuIds));
        result.setApps(buildQuickNavApps(subSystemId, result.getMenuIds()));
        subSystemUserQuickNavRedisDAO.set(userId, subSystemId, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubSystemUserQuickNavRespVO saveUserQuickNav(Long userId, Long subSystemId, List<Long> menuIds) {
        Set<Long> allowedMenuIds = subSystemUsersService.getAllowedQuickNavMenuIds(userId, subSystemId);
        List<Long> lockedMenuIds = getLockedMenuIds(userId, subSystemId, allowedMenuIds);
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
        refreshUserQuickNavCache(userId, subSystemId);
        // 返回与 list 接口一致的完整视图，避免前端再 GET 时被并发旧请求覆盖
        return getUserQuickNav(userId, subSystemId);
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

    private SubSystemUserQuickNavRespVO loadUserQuickNavFromDb(Long userId, Long subSystemId) {
        List<SubSystemUserQuickNavDO> savedList = subSystemUserQuickNavMapper
                .selectListByUserIdAndSubSystemId(userId, subSystemId);
        boolean configured = CollUtil.isNotEmpty(savedList);
        List<Long> menuIds = savedList.stream()
                .map(SubSystemUserQuickNavDO::getMenuId)
                .collect(Collectors.toList());
        return new SubSystemUserQuickNavRespVO(menuIds, configured, null);
    }

    private void refreshUserQuickNavCache(Long userId, Long subSystemId) {
        // 删掉让下次 get 重建完整视图（含 locked + allowed 过滤）
        subSystemUserQuickNavRedisDAO.delete(userId, subSystemId);
    }

    private void writeUserQuickNavFromRoles(Long userId, Long subSystemId) {
        List<Long> existingMenuIds = loadUserQuickNavFromDb(userId, subSystemId).getMenuIds();
        List<Long> roleMenuIds = subSystemRoleQuickNavService.getUserDefaultMenuIds(userId, subSystemId);
        Set<Long> allowedMenuIds = subSystemUsersService.getAllowedQuickNavMenuIds(userId, subSystemId);

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

    private List<Long> getLockedMenuIds(Long userId, Long subSystemId, Set<Long> allowedMenuIds) {
        return subSystemRoleQuickNavService.getUserDefaultMenuIds(userId, subSystemId).stream()
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
        // 祖先路径需要整表 path 段（量级通常几十～百，远小于主系统 982）
        List<SubSystemMenuDO> allMenus = subSystemMenuMapper.selectListBySubSystemId(subSystemId);
        Map<Long, SubSystemMenuDO> menuMap = convertMap(allMenus, SubSystemMenuDO::getId);
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
        List<String> segments = new ArrayList<>();
        Long current = menuId;
        while (current != null && current != 0L) {
            SubSystemMenuDO item = menuMap.get(current);
            if (item == null) {
                break;
            }
            if (("M".equals(item.getType()) || "C".equals(item.getType())) && StrUtil.isNotBlank(item.getPath())) {
                segments.add(0, StrUtil.removePrefix(item.getPath(), "/"));
            }
            current = item.getParentId();
        }
        if (segments.isEmpty()) {
            return "/portal/" + clientId;
        }
        return "/portal/" + clientId + "/" + String.join("/", segments);
    }

    private Set<SubSystemQuickNavCacheKey> convertCacheKeys(List<SubSystemUserQuickNavDO> affectedList) {
        return convertSet(affectedList, item -> SubSystemUserQuickNavRedisDAO.cacheKey(item.getUserId(), item.getSubSystemId()));
    }

}
