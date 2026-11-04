package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.quicknav.RoleQuickNavRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemMenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleQuickNavDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserRoleDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMenuMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleQuickNavMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUserRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMenuMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertList;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_ROLE_NOT_EXISTS;

@Service
@Validated
public class SubSystemRoleQuickNavServiceImpl implements SubSystemRoleQuickNavService {

    @Resource
    private SubSystemRoleQuickNavMapper subSystemRoleQuickNavMapper;
    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private SubSystemRoleMenuMapper subSystemRoleMenuMapper;
    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemUserRoleMapper subSystemUserRoleMapper;
    @Resource
    @Lazy
    private SubSystemUserQuickNavService subSystemUserQuickNavService;
    @Resource
    @Lazy
    private SubSystemPermissionContextService subSystemPermissionContextService;

    @Override
    public RoleQuickNavRespVO getRoleQuickNav(Long roleId) {
        validateRoleExists(roleId);
        return new RoleQuickNavRespVO(selectMenuIdsByRoleId(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleQuickNav(Long subSystemId, Long roleId, List<Long> menuIds) {
        SubSystemRoleDO role = validateRoleBelongsToSubSystem(roleId, subSystemId);
        Set<Long> allowedMenuIds = getRoleAllowedQuickNavMenuIds(role.getSubSystemId(), role.getId());
        List<Long> oldDefaults = selectMenuIdsByRoleId(roleId);
        List<Long> validMenuIds = CollUtil.isEmpty(menuIds) ? Collections.emptyList()
                : menuIds.stream().filter(allowedMenuIds::contains).distinct().collect(Collectors.toList());
        Set<Long> cancelledDefaults = new LinkedHashSet<>(CollUtil.emptyIfNull(oldDefaults));
        cancelledDefaults.removeAll(validMenuIds);

        subSystemRoleQuickNavMapper.deleteByRoleId(roleId);
        for (int i = 0; i < validMenuIds.size(); i++) {
            SubSystemRoleQuickNavDO record = new SubSystemRoleQuickNavDO();
            record.setRoleId(roleId);
            record.setSubSystemId(subSystemId);
            record.setMenuId(validMenuIds.get(i));
            record.setSort(i);
            subSystemRoleQuickNavMapper.insert(record);
        }
        // 保存后立刻对齐该角色下用户个人快捷导航，避免只改角色默认、员工端仍残留旧项
        List<SubSystemUserRoleDO> userRoles = subSystemUserRoleMapper.selectListByRoleId(roleId);
        if (CollUtil.isNotEmpty(userRoles)) {
            Set<Long> subUserIds = convertSet(userRoles, SubSystemUserRoleDO::getUserId);
            Set<Long> mainUserIds = new LinkedHashSet<>();
            if (CollUtil.isNotEmpty(subUserIds)) {
                subSystemUsersMapper.selectBatchIds(subUserIds).forEach(user -> {
                    if (user != null
                            && Objects.equals(user.getSubSystemId(), subSystemId)
                            && user.getMainUserId() != null) {
                        mainUserIds.add(user.getMainUserId());
                    }
                });
            }
            // 兼容：角色用户关联异常时，仍尽量用子系统用户表反查
            if (CollUtil.isEmpty(mainUserIds) && CollUtil.isNotEmpty(subUserIds)) {
                subUserIds.forEach(subUserId -> {
                    SubSystemUsersDO user = subSystemUsersMapper.selectById(subUserId);
                    if (user != null
                            && Objects.equals(user.getSubSystemId(), subSystemId)
                            && user.getMainUserId() != null) {
                        mainUserIds.add(user.getMainUserId());
                    }
                });
            }
            if (CollUtil.isNotEmpty(mainUserIds)) {
                subSystemUserQuickNavService.alignUsersAfterRoleQuickNavSave(
                        subSystemId, mainUserIds, cancelledDefaults, validMenuIds);
            }
        }
        // 清子系统权限包/快捷导航 Redis + bump rbac，在线用户及时重拉
        subSystemPermissionContextService.evictByRoleId(roleId);
    }

    @Override
    public List<Long> getUserDefaultMenuIds(Long userId, Long subSystemId) {
        return getUserDefaultMenuIds(userId, subSystemId, null);
    }

    @Override
    public List<Long> getUserDefaultMenuIds(Long userId, Long subSystemId, String workshopCode) {
        // 指定车间：只取该叶子挂接行上的角色；未指定：系统级（车间空）挂接行
        LinkedHashSet<Long> roleIdSet = new LinkedHashSet<>();
        String workshop = StrUtil.trim(workshopCode);
        for (SubSystemUsersDO row : CollUtil.emptyIfNull(subSystemUsersMapper.selectListByMainUserId(userId))) {
            if (!Objects.equals(subSystemId, row.getSubSystemId()) || "1".equals(row.getStatus())) {
                continue;
            }
            String rowWorkshop = StrUtil.trim(row.getWorkshopId());
            if (StrUtil.isNotBlank(workshop)) {
                if (!workshop.equals(rowWorkshop)) {
                    continue;
                }
            } else if (StrUtil.isNotBlank(rowWorkshop)) {
                // 未带车间时不合并各叶子角色，避免 5500 默认渗到「系统级」视图
                continue;
            }
            roleIdSet.addAll(convertList(subSystemUserRoleMapper.selectListByUserId(row.getId()),
                    SubSystemUserRoleDO::getRoleId));
        }
        // 兼容：未带车间且无系统级挂接时，仍合并该系统下全部挂接角色（旧数据）
        if (roleIdSet.isEmpty() && StrUtil.isBlank(workshop)) {
            for (SubSystemUsersDO row : CollUtil.emptyIfNull(subSystemUsersMapper.selectListByMainUserId(userId))) {
                if (!Objects.equals(subSystemId, row.getSubSystemId()) || "1".equals(row.getStatus())) {
                    continue;
                }
                roleIdSet.addAll(convertList(subSystemUserRoleMapper.selectListByUserId(row.getId()),
                        SubSystemUserRoleDO::getRoleId));
            }
        }
        List<Long> roleIds = new ArrayList<>(roleIdSet);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<SubSystemRoleDO> roles = subSystemRoleMapper.selectListByIds(roleIds).stream()
                .filter(role -> Objects.equals(role.getSubSystemId(), subSystemId))
                .filter(role -> CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()))
                .sorted(Comparator.comparing(SubSystemRoleDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SubSystemRoleDO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(roles)) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> merged = new LinkedHashSet<>();
        List<SubSystemRoleQuickNavDO> savedList = subSystemRoleQuickNavMapper
                .selectListByRoleIds(convertSet(roles, SubSystemRoleDO::getId));
        Map<Long, List<SubSystemRoleQuickNavDO>> grouped = savedList.stream()
                .collect(Collectors.groupingBy(SubSystemRoleQuickNavDO::getRoleId));
        for (SubSystemRoleDO role : roles) {
            grouped.getOrDefault(role.getId(), Collections.emptyList()).stream()
                    .map(SubSystemRoleQuickNavDO::getMenuId)
                    .forEach(merged::add);
        }
        return new ArrayList<>(merged);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        subSystemRoleQuickNavMapper.deleteByRoleId(roleId);
    }

    @Override
    public void deleteByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        subSystemRoleQuickNavMapper.deleteByRoleIds(roleIds);
    }

    @Override
    public void deleteByMenuId(Long menuId) {
        subSystemRoleQuickNavMapper.deleteByMenuId(menuId);
    }

    @Override
    public void deleteByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        subSystemRoleQuickNavMapper.deleteByMenuIds(menuIds);
    }

    @Override
    public boolean existsByMenuId(Long menuId) {
        return menuId != null && CollUtil.isNotEmpty(subSystemRoleQuickNavMapper.selectListByMenuId(menuId));
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
        return subSystemRoleQuickNavMapper.selectListByRoleId(roleId).stream()
                .map(SubSystemRoleQuickNavDO::getMenuId)
                .collect(Collectors.toList());
    }

    private SubSystemRoleDO validateRoleExists(Long roleId) {
        SubSystemRoleDO role = subSystemRoleMapper.selectById(roleId);
        if (role == null) {
            throw exception(SUB_SYSTEM_ROLE_NOT_EXISTS);
        }
        return role;
    }

    private SubSystemRoleDO validateRoleBelongsToSubSystem(Long roleId, Long subSystemId) {
        SubSystemRoleDO role = validateRoleExists(roleId);
        if (!Objects.equals(role.getSubSystemId(), subSystemId)) {
            throw exception(SUB_SYSTEM_ROLE_NOT_EXISTS);
        }
        return role;
    }

    private Set<Long> getRoleAllowedQuickNavMenuIds(Long subSystemId, Long roleId) {
        Set<Long> roleMenuIds = convertSet(subSystemRoleMenuMapper.selectListByRoleId(roleId),
                item -> item.getMenuId());
        if (CollUtil.isEmpty(roleMenuIds)) {
            return Collections.emptySet();
        }
        List<SubSystemMenuDO> allMenus = subSystemMenuMapper.selectListBySubSystemId(subSystemId).stream()
                .filter(menu -> menu.getStatus() == null || menu.getStatus() == 0)
                .collect(Collectors.toList());
        Map<Long, SubSystemMenuDO> menuMap = convertMap(allMenus, SubSystemMenuDO::getId);
        Set<Long> displayMenuIds = new HashSet<>();
        for (Long menuId : roleMenuIds) {
            Long current = menuId;
            while (current != null && current != 0L) {
                if (!displayMenuIds.add(current)) {
                    break;
                }
                SubSystemMenuDO menu = menuMap.get(current);
                if (menu == null) {
                    break;
                }
                current = menu.getParentId();
            }
        }
        return allMenus.stream()
                .filter(menu -> "C".equals(menu.getType()) && displayMenuIds.contains(menu.getId()))
                .filter(menu -> menu.getVisible() == null || menu.getVisible() == 0)
                .map(SubSystemMenuDO::getId)
                .collect(Collectors.toSet());
    }

}
