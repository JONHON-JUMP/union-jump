package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.tenant.core.context.TenantContextHolder;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalPermContextRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemMenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleMenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserRoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMenuMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMenuMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUserRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.dal.redis.portal.PortalPermContextRedisDAO;
import cn.jonhon.jump.module.system.dal.redis.user.PortalMyMenusRedisDAO;
import cn.jonhon.jump.module.system.dal.redis.user.PortalRbacVersionRedisDAO;
import cn.jonhon.jump.module.system.dal.redis.user.SubSystemUserQuickNavRedisDAO;
import cn.jonhon.jump.module.system.service.user.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertList;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_NOT_EXISTS;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_USER_NOT_EXISTS;

/**
 * 子系统权限包服务实现。
 */
@Service
@Validated
@Slf4j
public class SubSystemPermissionContextServiceImpl implements SubSystemPermissionContextService {

    @Resource
    private PortalPermContextRedisDAO portalPermContextRedisDAO;
    @Resource
    private PortalMyMenusRedisDAO portalMyMenusRedisDAO;
    @Resource
    private PortalRbacVersionRedisDAO portalRbacVersionRedisDAO;
    @Resource
    private SubSystemUserQuickNavRedisDAO subSystemUserQuickNavRedisDAO;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemUserRoleMapper subSystemUserRoleMapper;
    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private SubSystemRoleMenuMapper subSystemRoleMenuMapper;
    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private OAuth2ClientMapper oauth2ClientMapper;
    @Resource
    private AdminUserService adminUserService;

    @Override
    public PortalPermContextRespVO getOrRebuild(Long tenantId, Long mainUserId, Long subSystemId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        // key 维度是 username:clientId，读前需解析；miss 时 rebuildAndCache 会从 DB 重建
        String username = resolveUsername(mainUserId);
        String clientId = resolveClientId(subSystemId);
        if (StrUtil.isNotBlank(username) && StrUtil.isNotBlank(clientId)) {
            PortalPermContextRespVO cached = portalPermContextRedisDAO.get(username, clientId);
            if (cached != null) {
                // 滑动续期：多机同用户登录时，以最近一次访问为准重置 TTL，
                // 避免 B 机还在用、权限包却按 A 登录时间先过期
                portalPermContextRedisDAO.refreshTtl(username, clientId);
                return cached;
            }
        }
        return rebuildAndCache(resolvedTenantId, mainUserId, subSystemId);
    }

    @Override
    public PortalPermContextRespVO rebuildAndCache(Long tenantId, Long mainUserId, Long subSystemId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        PortalPermContextRespVO context = buildFromDb(resolvedTenantId, mainUserId, subSystemId);
        // buildFromDb 已解析 username / clientId（写进 context），直接用它拼 key
        portalPermContextRedisDAO.set(context.getUsername(), context.getClientId(), context);
        // 权限包重建时同步刷新子系统 RBAC 版本号 TTL，保证版本号不会比权限包先过期。
        // 否则权限包到期删除后版本号仍在，子系统探测会误判为权限变更而提示重登。
        portalRbacVersionRedisDAO.refreshTtl(subSystemId);
        return context;
    }

    @Override
    public PortalPermContextRespVO getFromCache(Long tenantId, Long mainUserId, Long subSystemId) {
        String username = resolveUsername(mainUserId);
        String clientId = resolveClientId(subSystemId);
        if (StrUtil.isBlank(username) || StrUtil.isBlank(clientId)) {
            return null;
        }
        return portalPermContextRedisDAO.get(username, clientId);
    }

    @Override
    public void evict(Long tenantId, Long mainUserId, Long subSystemId) {
        // 精确删：按 username:clientId
        String username = resolveUsername(mainUserId);
        String clientId = resolveClientId(subSystemId);
        if (StrUtil.isNotBlank(username) && StrUtil.isNotBlank(clientId)) {
            portalPermContextRedisDAO.delete(username, clientId);
        }
        if (mainUserId != null && subSystemId != null) {
            portalMyMenusRedisDAO.delete(mainUserId, subSystemId);
        }
        bumpRbacVersion(subSystemId);
    }

    @Override
    public void evictBySubSystemUserId(Long subSystemUserId) {
        if (subSystemUserId == null) {
            return;
        }
        SubSystemUsersDO user = subSystemUsersMapper.selectById(subSystemUserId);
        if (user == null || user.getMainUserId() == null || user.getSubSystemId() == null) {
            return;
        }
        evict(resolveTenantId(null), user.getMainUserId(), user.getSubSystemId());
    }

    @Override
    public void evictByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        SubSystemRoleDO role = subSystemRoleMapper.selectById(roleId);
        if (role == null) {
            return;
        }
        List<SubSystemUserRoleDO> userRoles = subSystemUserRoleMapper.selectListByRoleId(roleId);
        Set<Long> mainUserIds = new HashSet<>();
        if (CollUtil.isNotEmpty(userRoles)) {
            Set<Long> subSystemUserIds = convertSet(userRoles, SubSystemUserRoleDO::getUserId);
            List<SubSystemUsersDO> users = subSystemUsersMapper.selectBatchIds(subSystemUserIds);
            mainUserIds = users.stream()
                    .filter(u -> u.getMainUserId() != null && u.getSubSystemId() != null)
                    .map(SubSystemUsersDO::getMainUserId)
                    .collect(Collectors.toSet());
        }
        // 权限包：只清绑了该角色的用户（不管在不在线，在线的删后重建，不在线的无害）
        String clientId = resolveClientId(role.getSubSystemId());
        if (StrUtil.isNotBlank(clientId) && CollUtil.isNotEmpty(mainUserIds)) {
            Set<String> usernames = resolveUsernames(mainUserIds);
            portalPermContextRedisDAO.deleteBatch(clientId, usernames);
        }
        // 角色菜单变更会影响该子系统所有用户的 my-menus（含超管全量树）
        portalMyMenusRedisDAO.deleteBySubSystemId(role.getSubSystemId(), mainUserIds);
        subSystemUserQuickNavRedisDAO.deleteBySubSystemId(role.getSubSystemId(), mainUserIds);
        bumpRbacVersion(role.getSubSystemId());
    }

    @Override
    public void evictByMenuId(Long menuId) {
        if (menuId == null) {
            return;
        }
        SubSystemMenuDO menu = subSystemMenuMapper.selectById(menuId);
        if (menu == null || menu.getSubSystemId() == null) {
            return;
        }
        Long subSystemId = menu.getSubSystemId();
        String clientId = resolveClientId(subSystemId);
        // 查出授权了该菜单的角色 → 这些角色绑了哪些用户 → 精确清这些用户的权限包
        List<SubSystemRoleMenuDO> roleMenus = subSystemRoleMenuMapper.selectListByMenuId(menuId);
        Set<Long> mainUserIds = new HashSet<>();
        if (CollUtil.isNotEmpty(roleMenus)) {
            Set<Long> roleIds = convertSet(roleMenus, SubSystemRoleMenuDO::getRoleId);
            // 收集这些角色下的所有用户
            for (Long roleId : roleIds) {
                List<SubSystemUserRoleDO> userRoles = subSystemUserRoleMapper.selectListByRoleId(roleId);
                if (CollUtil.isNotEmpty(userRoles)) {
                    Set<Long> subSystemUserIds = convertSet(userRoles, SubSystemUserRoleDO::getUserId);
                    List<SubSystemUsersDO> users = subSystemUsersMapper.selectBatchIds(subSystemUserIds);
                    users.stream()
                            .filter(u -> u.getMainUserId() != null && u.getSubSystemId() != null)
                            .map(SubSystemUsersDO::getMainUserId)
                            .forEach(mainUserIds::add);
                }
            }
        }
        // 权限包：只清授权了该菜单的角色下的用户（不管在不在线）
        if (StrUtil.isNotBlank(clientId) && CollUtil.isNotEmpty(mainUserIds)) {
            Set<String> usernames = resolveUsernames(mainUserIds);
            portalPermContextRedisDAO.deleteBatch(clientId, usernames);
        }
        // my-menus 只清受影响用户
        portalMyMenusRedisDAO.deleteBySubSystemId(subSystemId, mainUserIds);
        subSystemUserQuickNavRedisDAO.deleteBySubSystemId(subSystemId, mainUserIds);
        bumpRbacVersion(subSystemId);
    }

    @Override
    public void evictAllByMainUserId(Long mainUserId) {
        if (mainUserId == null) {
            return;
        }
        List<SubSystemUsersDO> users = subSystemUsersMapper.selectListByMainUserId(mainUserId);
        // 权限包按用户 pattern 扫描清除（无需逐个解析 clientId）
        String username = resolveUsername(mainUserId);
        if (StrUtil.isNotBlank(username)) {
            portalPermContextRedisDAO.deleteByUsername(username);
        }
        // my-menus 用 pattern 扫描兜底，清掉该用户在所有子系统下的缓存（含已解绑的孤儿缓存）
        portalMyMenusRedisDAO.deleteByMainUserId(mainUserId);
        if (CollUtil.isNotEmpty(users)) {
            users.stream()
                    .filter(u -> u.getSubSystemId() != null)
                    .forEach(u -> bumpRbacVersion(u.getSubSystemId()));
        }
    }

    @Override
    public void evictBySubSystemId(Long subSystemId) {
        if (subSystemId == null) {
            return;
        }
        List<SubSystemUsersDO> users = subSystemUsersMapper.selectListBySubSystemId(subSystemId);
        Set<Long> mainUserIds = new HashSet<>();
        if (CollUtil.isNotEmpty(users)) {
            mainUserIds = users.stream()
                    .filter(u -> u.getMainUserId() != null)
                    .map(SubSystemUsersDO::getMainUserId)
                    .collect(Collectors.toSet());
        }
        // 权限包按子系统 pattern 扫描清除（无需逐个解析 username）
        String clientId = resolveClientId(subSystemId);
        if (StrUtil.isNotBlank(clientId)) {
            portalPermContextRedisDAO.deleteByClientId(clientId);
        }
        // 精确删每个用户的 my-menus + SCAN 兜底（勿只靠 KEYS）
        portalMyMenusRedisDAO.deleteBySubSystemId(subSystemId, mainUserIds);
        // 快捷导航与全量菜单同一套失效：下次进系统 Redis 空 → DB → 写回
        subSystemUserQuickNavRedisDAO.deleteBySubSystemId(subSystemId, mainUserIds);
        bumpRbacVersion(subSystemId);
    }

    /**
     * mainUserId → username（轻量单条查询，读缓存/精确失效时用）。
     */
    private String resolveUsername(Long mainUserId) {
        if (mainUserId == null) {
            return null;
        }
        AdminUserDO adminUser = adminUserService.getUser(mainUserId);
        return adminUser != null ? adminUser.getUsername() : null;
    }

    /**
     * 批量 mainUserId → username（一条 IN 查询，避免逐个查库）。
     */
    private Set<String> resolveUsernames(Collection<Long> mainUserIds) {
        if (CollUtil.isEmpty(mainUserIds)) {
            return new LinkedHashSet<>();
        }
        List<AdminUserDO> users = adminUserService.getUserList(mainUserIds);
        return users.stream()
                .map(AdminUserDO::getUsername)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * subSystemId → clientId（subSystem.oauth2ClientId → OAuth2Client.clientId）。
     */
    private String resolveClientId(Long subSystemId) {
        if (subSystemId == null) {
            return null;
        }
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null || subSystem.getOauth2ClientId() == null) {
            return null;
        }
        OAuth2ClientDO client = oauth2ClientMapper.selectById(subSystem.getOauth2ClientId());
        return client != null ? client.getClientId() : null;
    }

    @Override
    public long getRbacVersion(Long subSystemId) {
        return portalRbacVersionRedisDAO.get(subSystemId);
    }

    private void bumpRbacVersion(Long subSystemId) {
        if (subSystemId != null) {
            portalRbacVersionRedisDAO.bump(subSystemId);
        }
    }

    private PortalPermContextRespVO buildFromDb(Long tenantId, Long mainUserId, Long subSystemId) {
        SubSystemUsersDO subSystemUser = subSystemUsersMapper.selectBySubSystemIdAndMainUserId(subSystemId, mainUserId);
        if (subSystemUser == null || "1".equals(subSystemUser.getStatus())) {
            throw exception(SUB_SYSTEM_USER_NOT_EXISTS);
        }
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus())) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }

        AdminUserDO adminUser = adminUserService.getUser(mainUserId);
        if (adminUser == null || CommonStatusEnum.isDisable(adminUser.getStatus())) {
            throw exception(SUB_SYSTEM_USER_NOT_EXISTS);
        }
        String username = adminUser.getUsername();
        String clientId = null;
        if (subSystem.getOauth2ClientId() != null) {
            OAuth2ClientDO client = oauth2ClientMapper.selectById(subSystem.getOauth2ClientId());
            if (client != null) {
                clientId = client.getClientId();
            }
        }

        List<Long> roleIds = convertList(
                subSystemUserRoleMapper.selectListByUserId(subSystemUser.getId()),
                SubSystemUserRoleDO::getRoleId);

        // 权限包缓存角色 + 按钮 permissions；数据范围不下发
        List<PortalPermContextRespVO.Role> roles = new ArrayList<>();
        Set<String> permissions = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(roleIds)) {
            List<SubSystemRoleDO> roleList = subSystemRoleMapper.selectBatchIds(roleIds).stream()
                    .filter(role -> role.getStatus() == null || Objects.equals(role.getStatus(), 0))
                    .collect(Collectors.toList());
            for (SubSystemRoleDO role : roleList) {
                PortalPermContextRespVO.Role r = new PortalPermContextRespVO.Role();
                r.setId(role.getId());
                r.setCode(role.getCode());
                r.setName(role.getName());
                roles.add(r);
                if ("super_admin".equals(role.getCode()) || "admin".equals(role.getCode())) {
                    permissions.add("*:*:*");
                }
            }
            if (!permissions.contains("*:*:*")) {
                Set<Long> menuIds = convertSet(
                        subSystemRoleMenuMapper.selectListByRoleIds(
                                convertList(roleList, SubSystemRoleDO::getId)),
                        SubSystemRoleMenuDO::getMenuId);
                if (CollUtil.isNotEmpty(menuIds)) {
                    List<SubSystemMenuDO> allMenus = subSystemMenuMapper.selectListBySubSystemId(subSystemId).stream()
                            .filter(menu -> menu.getStatus() == null || Objects.equals(menu.getStatus(), 0))
                            .collect(Collectors.toList());
                    menuIds = expandWithButtonChildren(allMenus, menuIds);
                    Map<Long, SubSystemMenuDO> menuMap = convertMap(allMenus, SubSystemMenuDO::getId);
                    for (Long menuId : menuIds) {
                        SubSystemMenuDO menu = menuMap.get(menuId);
                        if (menu == null || StrUtil.isBlank(menu.getPerms())) {
                            continue;
                        }
                        for (String perm : menu.getPerms().split(",")) {
                            if (StrUtil.isNotBlank(perm)) {
                                permissions.add(perm.trim());
                            }
                        }
                    }
                }
            }
        }

        PortalPermContextRespVO context = new PortalPermContextRespVO();
        context.setUsername(username);
        context.setClientId(clientId);
        context.setUserId(mainUserId);
        context.setTenantId(tenantId);
        context.setSubSystemId(subSystemId);
        context.setRoles(roles);
        context.setPermissions(new ArrayList<>(permissions));
        return context;
    }

    /**
     * 勾选目录/菜单时，自动带上其下按钮（F）。
     */
    private Set<Long> expandWithButtonChildren(List<SubSystemMenuDO> menus, Set<Long> selectedIds) {
        if (CollUtil.isEmpty(selectedIds) || CollUtil.isEmpty(menus)) {
            return new LinkedHashSet<>(CollUtil.emptyIfNull(selectedIds));
        }
        Map<Long, List<SubSystemMenuDO>> childrenMap = menus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(SubSystemMenuDO::getParentId));
        LinkedHashSet<Long> result = new LinkedHashSet<>(selectedIds);
        ArrayDeque<Long> queue = new ArrayDeque<>(selectedIds);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            List<SubSystemMenuDO> children = childrenMap.get(parentId);
            if (CollUtil.isEmpty(children)) {
                continue;
            }
            for (SubSystemMenuDO child : children) {
                if (!"F".equals(child.getType())) {
                    continue;
                }
                if (result.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }
        return result;
    }

    private static Long resolveTenantId(Long tenantId) {
        if (tenantId != null) {
            return tenantId;
        }
        Long fromContext = TenantContextHolder.getTenantId();
        return fromContext != null ? fromContext : 1L;
    }

}
