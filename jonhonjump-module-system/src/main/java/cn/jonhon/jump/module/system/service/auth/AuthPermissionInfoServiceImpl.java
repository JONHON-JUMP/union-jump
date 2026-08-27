package cn.jonhon.jump.module.system.service.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import cn.jonhon.jump.module.system.convert.auth.AuthConvert;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.mysql.permission.RoleMapper;
import cn.jonhon.jump.module.system.dal.redis.auth.PermissionInfoRedisDAO;
import cn.jonhon.jump.module.system.dal.redis.auth.PermissionRbacVersionRedisDAO;
import cn.jonhon.jump.module.system.enums.permission.RoleCodeEnum;
import cn.jonhon.jump.framework.tenant.core.context.TenantContextHolder;
import cn.jonhon.jump.framework.tenant.core.util.TenantUtils;
import cn.jonhon.jump.module.system.service.permission.MenuColorService;
import cn.jonhon.jump.module.system.service.permission.MenuService;
import cn.jonhon.jump.module.system.service.permission.PermissionService;
import cn.jonhon.jump.module.system.service.permission.RoleService;
import cn.jonhon.jump.module.system.service.user.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.filterList;
import static cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO.ID_ROOT;

@Service
@Slf4j
public class AuthPermissionInfoServiceImpl implements AuthPermissionInfoService {

    private static final String ALL_PERMISSION = "*:*:*";

    @Resource
    private PermissionInfoRedisDAO permissionInfoRedisDAO;
    @Resource
    private PermissionRbacVersionRedisDAO permissionRbacVersionRedisDAO;
    @Resource
    private AdminUserService userService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private MenuColorService menuColorService;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public AuthPermissionInfoRespVO getPermissionInfo(Long userId) {
        return getPermissionInfo(userId, true, false);
    }

    @Override
    public AuthPermissionInfoRespVO getPermissionInfo(Long userId, boolean includeMenus, boolean redisOnly) {
        AuthPermissionInfoRespVO cached = permissionInfoRedisDAO.get(userId);
        if (cached != null) {
            AuthPermissionInfoRespVO result = includeMenus ? cached : withoutMenus(cached);
            return attachRbacVersion(userId, result);
        }
        // 后台预热：只读 Redis，未命中不建库（避免默认进 SCADA 时后台又打出 982 菜单）
        if (redisOnly) {
            return null;
        }
        // 轻量登录：不返回菜单树，超管不查 system_menu；后台异步灌入完整权限包供进主系统时 Redis 命中
        if (!includeMenus) {
            AuthPermissionInfoRespVO light = buildLightPermissionInfo(userId);
            warmFullPermissionInfoAsync(userId);
            return attachRbacVersion(userId, light);
        }
        AuthPermissionInfoRespVO result = buildPermissionInfo(userId);
        if (result != null) {
            permissionInfoRedisDAO.set(userId, result);
        }
        return attachRbacVersion(userId, result);
    }

    /**
     * 登录轻量路径不写 Redis；后台尽快补全含菜单树的权限包，供门户全量菜单预热命中。
     * 仅短延迟，避免与快捷导航首屏抢连接，又不过度拖慢「全部应用」。
     */
    private void warmFullPermissionInfoAsync(Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(300L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                Runnable task = () -> {
                    if (permissionInfoRedisDAO.get(userId) != null) {
                        return;
                    }
                    AuthPermissionInfoRespVO full = buildPermissionInfo(userId);
                    if (full != null) {
                        permissionInfoRedisDAO.set(userId, full);
                    }
                };
                if (tenantId != null) {
                    TenantUtils.execute(tenantId, task);
                } else {
                    TenantUtils.executeIgnore(task);
                }
            } catch (Exception ex) {
                log.warn("[warmFullPermissionInfoAsync] failed userId={}, cause={}", userId, ex.toString());
            }
        });
    }

    @Override
    public void evictUser(Long userId) {
        if (userId != null) {
            permissionInfoRedisDAO.delete(userId);
            permissionRbacVersionRedisDAO.bump(userId);
        }
    }

    @Override
    public void evictUsersByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        Set<Long> userIds = permissionService.getUserRoleIdListByRoleId(Collections.singleton(roleId));
        if (CollUtil.isNotEmpty(userIds)) {
            permissionInfoRedisDAO.deleteList(userIds);
            permissionRbacVersionRedisDAO.bumpList(userIds);
        }
    }

    @Override
    public void evictUsersAffectedByMenu(Long menuId) {
        if (menuId == null) {
            return;
        }
        Set<Long> userIds = new HashSet<>(getSuperAdminUserIds());
        Set<Long> menuIds = menuService.getMenuSelfAndChildIds(menuId);
        for (Long id : menuIds) {
            Set<Long> roleIds = permissionService.getMenuRoleIdListByMenuIdFromCache(id);
            if (CollUtil.isNotEmpty(roleIds)) {
                userIds.addAll(permissionService.getUserRoleIdListByRoleId(roleIds));
            }
        }
        if (CollUtil.isNotEmpty(userIds)) {
            permissionInfoRedisDAO.deleteList(userIds);
            permissionRbacVersionRedisDAO.bumpList(userIds);
        }
    }

    @Override
    public long getRbacVersion(Long userId) {
        return permissionRbacVersionRedisDAO.get(userId);
    }

    @Override
    public boolean isPermissionAlive(Long userId, Long sessionVersion) {
        if (userId == null || sessionVersion == null) {
            return true;
        }
        long current = permissionRbacVersionRedisDAO.get(userId);
        return current == sessionVersion;
    }

    private AuthPermissionInfoRespVO attachRbacVersion(Long userId, AuthPermissionInfoRespVO vo) {
        if (vo != null) {
            vo.setRbacVersion(permissionRbacVersionRedisDAO.get(userId));
        }
        return vo;
    }

    private Set<Long> getSuperAdminUserIds() {
        RoleDO superAdminRole = roleMapper.selectByCode(RoleCodeEnum.SUPER_ADMIN.getCode());
        if (superAdminRole == null) {
            return Collections.emptySet();
        }
        return permissionService.getUserRoleIdListByRoleId(Collections.singleton(superAdminRole.getId()));
    }

    /**
     * 登录首屏：用户 + 角色 + 权限标识，不含菜单树。
     * 超级管理员直接 *:*:*，不查 900+ 菜单。
     */
    private AuthPermissionInfoRespVO buildLightPermissionInfo(Long userId) {
        AdminUserDO user = userService.getUser(userId);
        if (user == null) {
            return null;
        }
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return AuthPermissionInfoRespVO.builder()
                    .user(BeanUtils.toBean(user, AuthPermissionInfoRespVO.UserVO.class))
                    .roles(Collections.emptySet())
                    .permissions(Collections.emptySet())
                    .menus(Collections.emptyList())
                    .build();
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        Set<String> roleCodes = convertSet(roles, RoleDO::getCode);
        Set<String> permissions;
        // 直接看角色 code，避免 cache 抖动导致超管仍走全量菜单
        boolean isSuperAdmin = roles.stream().anyMatch(role -> RoleCodeEnum.isSuperAdmin(role.getCode()))
                || roleService.hasAnySuperAdmin(convertSet(roles, RoleDO::getId));
        if (isSuperAdmin) {
            permissions = Collections.singleton(ALL_PERMISSION);
        } else {
            Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
            List<MenuDO> menuList = menuService.filterDisableMenus(menuService.getMenuList(menuIds));
            permissions = convertSet(menuList, MenuDO::getPermission);
            permissions.removeIf(StrUtil::isEmpty);
        }
        return AuthPermissionInfoRespVO.builder()
                .user(BeanUtils.toBean(user, AuthPermissionInfoRespVO.UserVO.class))
                .roles(roleCodes)
                .permissions(permissions)
                .menus(Collections.emptyList())
                .build();
    }

    private AuthPermissionInfoRespVO buildPermissionInfo(Long userId) {
        AdminUserDO user = userService.getUser(userId);
        if (user == null) {
            return null;
        }
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return AuthConvert.INSTANCE.convert(user, Collections.emptyList(), Collections.emptyList());
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));

        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
        List<MenuDO> menuList = menuService.getMenuList(menuIds);
        menuList = menuService.filterDisableMenus(menuList);

        Set<Long> styleIds = convertSet(
                filterList(menuList, menu -> ID_ROOT.equals(menu.getParentId())),
                MenuDO::getStyleId);
        styleIds.remove(null);
        Map<Long, MenuColorDO> colorMap = menuColorService.getMenuColorMap(styleIds);

        return AuthConvert.INSTANCE.convert(user, roles, menuList, colorMap);
    }

    private AuthPermissionInfoRespVO withoutMenus(AuthPermissionInfoRespVO cached) {
        return AuthPermissionInfoRespVO.builder()
                .user(cached.getUser())
                .roles(cached.getRoles())
                .permissions(cached.getPermissions())
                .rbacVersion(cached.getRbacVersion())
                .menus(Collections.emptyList())
                .build();
    }

}
