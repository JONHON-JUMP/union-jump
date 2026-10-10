package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.collection.CollectionUtils;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.*;
import cn.jonhon.jump.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.*;
import cn.jonhon.jump.module.system.enums.permission.DataScopeEnum;
import cn.jonhon.jump.module.system.enums.permission.MenuTypeEnum;
import cn.jonhon.jump.module.system.enums.permission.RoleTypeEnum;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class SubSystemRoleServiceImpl implements SubSystemRoleService {

    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private OAuth2ClientMapper oauth2ClientMapper;
    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;
    @Resource
    private SubSystemRoleMenuMapper subSystemRoleMenuMapper;
    @Resource
    private SubSystemUserRoleMapper subSystemUserRoleMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemRoleQuickNavService subSystemRoleQuickNavService;
    @Resource
    private SubSystemPermissionContextService subSystemPermissionContextService;
    @Resource
    private SubSystemUserQuickNavService subSystemUserQuickNavService;
    @Resource
    private SubSystemApiConfigService subSystemApiConfigService;
    @Resource
    private SubSystemWorkshopService subSystemWorkshopService;
    @Resource
    private SubSystemAccessService subSystemAccessService;

    @Override
    public PageResult<SubSystemRoleRespVO> getSubSystemRolePage(SubSystemRolePageReqVO pageReqVO) {
        // 可管系统范围：受限时强制只查授权系统（null=不受限）
        java.util.Set<Long> allowedSubSystemIds = subSystemAccessService.getAllowedSubSystemIds(
                cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());
        if (pageReqVO.getSubSystemId() != null) {
            validateSubSystemExists(pageReqVO.getSubSystemId());
        } else if (allowedSubSystemIds != null && allowedSubSystemIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        PageResult<SubSystemRoleDO> pageResult = subSystemRoleMapper.selectPage(pageReqVO, allowedSubSystemIds);
        return new PageResult<>(buildRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public SubSystemRoleRespVO getSubSystemRole(Long id) {
        SubSystemRoleDO role = validateSubSystemRoleExists(id);
        return buildResp(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSubSystemRole(SubSystemRoleSaveReqVO createReqVO) {
        validateSubSystemExists(createReqVO.getSubSystemId());
        boolean syncToExternal = Boolean.TRUE.equals(createReqVO.getSyncToExternal());
        String roleName = StrUtil.trim(createReqVO.getName());
        String workshopCode = resolveWorkshopCode(createReqVO.getSubSystemId(), createReqVO.getWorkshopCode(), roleName);
        if (syncToExternal) {
            if (StrUtil.isBlank(workshopCode)) {
                throw exception(SUB_SYSTEM_ROLE_WORKSHOP_REQUIRED);
            }
            Long apiSubSystemId = createReqVO.getApiSubSystemId();
            if (apiSubSystemId == null || apiSubSystemId <= 0) {
                // 未显式指定时，兼容旧调用：用本地角色所属系统
                apiSubSystemId = createReqVO.getSubSystemId();
            }
            // 同步时：本地角色名统一为 车间编号_角色名称（输入已带此前缀则不再重复拼接）
            roleName = buildExternalRoleName(workshopCode, roleName);
            assertRoleNameLength(roleName);
            createReqVO.setName(roleName);
            validateRoleDuplicate(createReqVO.getSubSystemId(), roleName, createReqVO.getCode(), null);
            // 对方已有同名角色时不建本地角色，提示改走「关联已有角色」
            assertExternalRoleNameFree(apiSubSystemId, workshopCode, roleName, false);

            SubSystemRoleDO role = BeanUtils.toBean(createReqVO, SubSystemRoleDO.class);
            role.setName(roleName);
            role.setType(RoleTypeEnum.CUSTOM.getType());
            role.setDataScope(DataScopeEnum.ALL.getScope());
            role.setMenuCheckStrictly(1);
            role.setDeptCheckStrictly(1);
            role.setRoleRegistered("0");
            subSystemRoleMapper.insert(role);

            subSystemApiConfigService.pushExternalRoleCreate(apiSubSystemId, workshopCode, roleName);
            markRoleRegistered(role.getId(), "1");
            storeExternalRoleId(role.getId(), apiSubSystemId, workshopCode, roleName);
            return role.getId();
        }
        validateRoleDuplicate(createReqVO.getSubSystemId(), roleName, createReqVO.getCode(), null);

        SubSystemRoleDO role = BeanUtils.toBean(createReqVO, SubSystemRoleDO.class);
        role.setName(roleName);
        role.setType(RoleTypeEnum.CUSTOM.getType());
        role.setDataScope(DataScopeEnum.ALL.getScope());
        role.setMenuCheckStrictly(1);
        role.setDeptCheckStrictly(1);
        role.setRoleRegistered("0");
        subSystemRoleMapper.insert(role);
        return role.getId();
    }

    @Override
    public void updateSubSystemRole(SubSystemRoleSaveReqVO updateReqVO) {
        SubSystemRoleDO role = validateSubSystemRoleExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateRoleDuplicate(updateReqVO.getSubSystemId(), updateReqVO.getName(), updateReqVO.getCode(), updateReqVO.getId());

        SubSystemRoleDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemRoleDO.class);
        updateObj.setSubSystemId(role.getSubSystemId());
        if (StrUtil.isNotBlank(updateReqVO.getRoleRegistered())) {
            updateObj.setRoleRegistered("1".equals(updateReqVO.getRoleRegistered()) ? "1" : "0");
        } else {
            // 导入等未传字段时不覆盖原注册状态
            updateObj.setRoleRegistered(null);
        }
        subSystemRoleMapper.updateById(updateObj);
        subSystemPermissionContextService.evictByRoleId(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemRole(Long id) {
        validateSubSystemRoleExists(id);
        validateRoleNotAssigned(id);
        subSystemRoleMapper.deleteById(id);
        subSystemRoleMenuMapper.deleteListByRoleId(id);
        subSystemRoleQuickNavService.deleteByRoleId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemRoleList(List<Long> ids) {
        ids.forEach(id -> {
            validateSubSystemRoleExists(id);
            validateRoleNotAssigned(id);
        });
        subSystemRoleMapper.deleteByIds(ids);
        ids.forEach(id -> {
            subSystemRoleMenuMapper.deleteListByRoleId(id);
            subSystemRoleQuickNavService.deleteByRoleId(id);
        });
    }

    @Override
    public void updateSubSystemRoleStatus(Long id, Integer status) {
        validateSubSystemRoleExists(id);
        SubSystemRoleDO updateObj = new SubSystemRoleDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        subSystemRoleMapper.updateById(updateObj);
        subSystemPermissionContextService.evictByRoleId(id);
    }

    @Override
    public void updateSubSystemRoleRegisterStatus(Long id, String roleRegistered) {
        validateSubSystemRoleExists(id);
        markRoleRegistered(id, "1".equals(roleRegistered) ? "1" : "0");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerSubSystemRole(Long id, SubSystemRoleRegisterReqVO reqVO) {
        SubSystemRoleDO role = validateSubSystemRoleExists(id);
        if ("1".equals(role.getRoleRegistered())) {
            throw exception(SUB_SYSTEM_ROLE_ALREADY_REGISTERED);
        }
        String workshopCode = resolveWorkshopCode(role.getSubSystemId(),
                reqVO != null ? reqVO.getWorkshopCode() : null, role.getName());
        String roleName = StrUtil.trim(role.getName());
        if (StrUtil.isBlank(workshopCode)) {
            throw exception(SUB_SYSTEM_ROLE_NAME_WORKSHOP_INVALID);
        }
        Long apiSubSystemId = reqVO != null ? reqVO.getApiSubSystemId() : null;
        if (apiSubSystemId == null || apiSubSystemId <= 0) {
            apiSubSystemId = role.getSubSystemId();
        }
        // 名称尚未带车间前缀时，补全本地名再推送，与新建勾选同步规则一致
        String externalName = buildExternalRoleName(workshopCode, roleName);
        assertRoleNameLength(externalName);
        assertExternalRoleNameFree(apiSubSystemId, workshopCode, externalName, true);
        if (!Objects.equals(externalName, roleName)) {
            validateRoleDuplicate(role.getSubSystemId(), externalName, role.getCode(), role.getId());
            SubSystemRoleDO rename = new SubSystemRoleDO();
            rename.setId(role.getId());
            rename.setName(externalName);
            subSystemRoleMapper.updateById(rename);
            roleName = externalName;
        }
        subSystemApiConfigService.pushExternalRoleCreate(apiSubSystemId, workshopCode, roleName);
        markRoleRegistered(role.getId(), "1");
        storeExternalRoleId(role.getId(), apiSubSystemId, workshopCode, roleName);
    }

    /** 推送成功后回查外部 roleId 并落库。查不到就不算同步成功，列表才能显示「已关联外部」。 */
    private void storeExternalRoleId(Long roleId, Long apiSubSystemId, String workshopCode, String externalRoleName) {
        Map<String, String> nameToId;
        try {
            nameToId = subSystemApiConfigService.queryExternalRoleIds(apiSubSystemId, workshopCode);
        } catch (RuntimeException e) {
            log.warn("[storeExternalRoleId] roleId={} 回查外部 roleId 失败", roleId, e);
            throw exception0(BAD_REQUEST.getCode(),
                    "Camstar 角色【" + externalRoleName + "】已创建，但没有查回 roleId："
                            + e.getMessage() + "。请取消「同步注册」先保存本地角色，再用【关联外部】绑定");
        }
        String externalRoleId = findExternalRoleId(nameToId, externalRoleName);
        if (StrUtil.isBlank(externalRoleId)) {
            throw exception0(BAD_REQUEST.getCode(),
                    "Camstar 角色【" + externalRoleName + "】已创建，但按角色名没有查到 roleId。请取消「同步注册」先保存本地角色，再用【关联外部】绑定");
        }
        SubSystemRoleDO updateObj = new SubSystemRoleDO();
        updateObj.setId(roleId);
        updateObj.setExternalRoleId(externalRoleId.trim());
        updateObj.setRoleRegistered("1");
        subSystemRoleMapper.updateById(updateObj);
    }

    @Override
    public List<SubSystemExternalRoleRespVO> getExternalRoleList(Long apiSubSystemId, String workshopCode) {
        if (StrUtil.isBlank(workshopCode)) {
            throw exception0(BAD_REQUEST.getCode(), "请选择/填写车间编号");
        }
        Map<String, String> nameToId = subSystemApiConfigService.queryExternalRoleIds(apiSubSystemId, workshopCode.trim());
        return nameToId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SubSystemExternalRoleRespVO()
                        .setRoleName(entry.getKey())
                        .setRoleId(entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public void bindExternalRole(Long id, String externalRoleId) {
        validateSubSystemRoleExists(id);
        String roleId = StrUtil.trimToNull(externalRoleId);
        // updateById 默认跳过 null，解除关联必须显式把 external_role_id 写成空
        LambdaUpdateWrapper<SubSystemRoleDO> wrapper = new LambdaUpdateWrapper<SubSystemRoleDO>()
                .eq(SubSystemRoleDO::getId, id)
                .set(SubSystemRoleDO::getExternalRoleId, roleId);
        if (roleId != null) {
            // 已人工确认存在于对方系统，注册标记一并置为已注册
            wrapper.set(SubSystemRoleDO::getRoleRegistered, "1");
        }
        subSystemRoleMapper.update(null, wrapper);
    }

    /** 对方已有同名角色则中止。alreadyLocal=该 JUMP 角色是否已经存在，提示文案不同 */
    private void assertExternalRoleNameFree(Long apiSubSystemId, String workshopCode, String roleName,
                                            boolean alreadyLocal) {
        Map<String, String> nameToId;
        try {
            nameToId = subSystemApiConfigService.queryExternalRoleIds(apiSubSystemId, workshopCode);
        } catch (RuntimeException ex) {
            // 角色查询失败不拦创建：交给角色新增接口，由对方报同名后再转成提示
            log.warn("[assertExternalRoleNameFree] 回查外部角色失败，改为直接推送。roleName={}", roleName, ex);
            return;
        }
        if (StrUtil.isNotBlank(findExternalRoleId(nameToId, roleName))) {
            String tip = alreadyLocal
                    ? "Camstar 已存在同名角色【" + roleName + "】，请改用【关联外部】"
                    : "Camstar 已存在同名角色【" + roleName + "】。请取消「同步注册」先保存本地角色，再在列表中使用【关联外部】";
            throw exception0(BAD_REQUEST.getCode(), tip);
        }
    }

    private static void assertRoleNameLength(String roleName) {
        if (roleName != null && roleName.length() > 30) {
            throw exception0(BAD_REQUEST.getCode(),
                    "同步后的角色名【" + roleName + "】超过 30 个字符，请缩短角色名称");
        }
    }

    /** 角色名匹配忽略首尾空格和大小写（Camstar 回查字段名不完全稳定） */
    private static String findExternalRoleId(Map<String, String> nameToId, String roleName) {
        if (nameToId == null || nameToId.isEmpty() || StrUtil.isBlank(roleName)) {
            return null;
        }
        String direct = nameToId.get(roleName);
        if (StrUtil.isNotBlank(direct)) {
            return direct.trim();
        }
        String target = roleName.trim();
        for (Map.Entry<String, String> entry : nameToId.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(target)
                    && StrUtil.isNotBlank(entry.getValue())) {
                return entry.getValue().trim();
            }
        }
        return null;
    }

    private void markRoleRegistered(Long id, String roleRegistered) {
        SubSystemRoleDO updateObj = new SubSystemRoleDO();
        updateObj.setId(id);
        updateObj.setRoleRegistered(roleRegistered);
        subSystemRoleMapper.updateById(updateObj);
    }

    /** 车间优先：入参 > 角色名前缀 > 花名册系统推断（MES4200 → 4200） */
    private String resolveWorkshopCode(Long subSystemId, String explicit, String roleName) {
        if (StrUtil.isNotBlank(explicit)) {
            return explicit.trim();
        }
        String fromName = parseWorkshopPrefix(roleName);
        if (StrUtil.isNotBlank(fromName)) {
            return fromName;
        }
        return subSystemWorkshopService.inferWorkshopCode(subSystemId);
    }

    /** 车间编号_短名；若已带此前缀则原样返回 */
    private static String buildExternalRoleName(String workshopCode, String roleName) {
        String name = StrUtil.trim(roleName);
        String workshop = StrUtil.trim(workshopCode);
        if (StrUtil.isBlank(name) || StrUtil.isBlank(workshop)) {
            return name;
        }
        String prefix = workshop + "_";
        if (name.startsWith(prefix)) {
            return name;
        }
        return prefix + name;
    }

    /** 从「车间_角色」解析车间段；无下划线则返回 null */
    private static String parseWorkshopPrefix(String roleName) {
        if (StrUtil.isBlank(roleName)) {
            return null;
        }
        int idx = roleName.indexOf('_');
        if (idx <= 0) {
            return null;
        }
        return roleName.substring(0, idx);
    }

    @Override
    public List<SubSystemMenuSimpleRespVO> getMenuSimpleList(Long subSystemId) {
        validateSubSystemExists(subSystemId);
        return subSystemMenuMapper.selectListBySubSystemId(subSystemId).stream()
                .map(menu -> {
                    SubSystemMenuSimpleRespVO vo = new SubSystemMenuSimpleRespVO();
                    vo.setId(menu.getId());
                    vo.setName(menu.getMenuName());
                    vo.setParentId(menu.getParentId());
                    vo.setOrderNum(menu.getOrderNum());
                    vo.setType(convertMenuTypeFromDb(menu.getType()));
                    vo.setStatus(menu.getStatus());
                    vo.setVisible(menu.getVisible() != null && menu.getVisible() == 0);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private Integer convertMenuTypeFromDb(String type) {
        if ("M".equals(type)) {
            return MenuTypeEnum.DIR.getType();
        }
        if ("C".equals(type)) {
            return MenuTypeEnum.MENU.getType();
        }
        if ("F".equals(type)) {
            return MenuTypeEnum.BUTTON.getType();
        }
        return MenuTypeEnum.DIR.getType();
    }

    @Override
    public Set<Long> getRoleMenuIds(Long roleId) {
        validateSubSystemRoleExists(roleId);
        return convertSet(subSystemRoleMenuMapper.selectListByRoleId(roleId), SubSystemRoleMenuDO::getMenuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenu(SubSystemRoleAssignMenuReqVO reqVO) {
        SubSystemRoleDO role = validateSubSystemRoleExists(reqVO.getRoleId());
        List<SubSystemMenuDO> menus = subSystemMenuMapper.selectListBySubSystemId(role.getSubSystemId());
        // 门户只勾页面；保存时自动带上按钮（F），供 permissions 下发。数据范围仍由子系统本地管。
        Set<Long> menuIds = expandWithButtonChildren(menus, CollUtil.emptyIfNull(reqVO.getMenuIds()));
        if (CollUtil.isNotEmpty(menuIds)) {
            Set<Long> validMenuIds = convertSet(menus, SubSystemMenuDO::getId);
            for (Long menuId : menuIds) {
                if (!validMenuIds.contains(menuId)) {
                    throw exception(SUB_SYSTEM_ROLE_NOT_EXISTS);
                }
            }
        }

        Set<Long> dbMenuIds = convertSet(subSystemRoleMenuMapper.selectListByRoleId(reqVO.getRoleId()),
                SubSystemRoleMenuDO::getMenuId);
        Collection<Long> createMenuIds = CollUtil.subtract(menuIds, dbMenuIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbMenuIds, menuIds);
        if (CollUtil.isNotEmpty(deleteMenuIds)) {
            List<Long> quickNavMenuIds = subSystemRoleQuickNavService.getRoleQuickNav(reqVO.getRoleId()).getMenuIds();
            if (CollUtil.isNotEmpty(quickNavMenuIds) && CollUtil.containsAny(quickNavMenuIds, deleteMenuIds)) {
                throw exception(SUB_SYSTEM_ROLE_MENU_HAS_QUICK_NAV);
            }
        }
        if (CollUtil.isNotEmpty(createMenuIds)) {
            subSystemRoleMenuMapper.insertBatch(CollectionUtils.convertList(createMenuIds, menuId -> {
                SubSystemRoleMenuDO entity = new SubSystemRoleMenuDO();
                entity.setRoleId(reqVO.getRoleId());
                entity.setMenuId(menuId);
                return entity;
            }));
        }
        if (CollUtil.isNotEmpty(deleteMenuIds)) {
            subSystemRoleMenuMapper.deleteListByRoleIdAndMenuIds(reqVO.getRoleId(), deleteMenuIds);
            // 个人快捷导航不拦截；取消角色菜单后，去掉该角色用户个人快捷导航中的对应项
            List<SubSystemUserRoleDO> userRoles = subSystemUserRoleMapper.selectListByRoleId(reqVO.getRoleId());
            if (CollUtil.isNotEmpty(userRoles)) {
                Set<Long> subUserIds = convertSet(userRoles, SubSystemUserRoleDO::getUserId);
                Set<Long> mainUserIds = subSystemUsersMapper.selectBatchIds(subUserIds).stream()
                        .filter(user -> Objects.equals(user.getSubSystemId(), role.getSubSystemId()))
                        .map(SubSystemUsersDO::getMainUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                subSystemUserQuickNavService.removeMenusForUsers(role.getSubSystemId(), mainUserIds, deleteMenuIds);
            }
        }
        // 权限变更后失效权限包，子系统下一次请求重建
        subSystemPermissionContextService.evictByRoleId(reqVO.getRoleId());
    }

    @Override
    public void assignRoleDataScope(SubSystemRoleAssignDataScopeReqVO reqVO) {
        // 主系统不管子系统数据权限；请在各子系统本地角色中配置
        validateSubSystemRoleExists(reqVO.getRoleId());
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

    private List<SubSystemRoleRespVO> buildRespList(List<SubSystemRoleDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(list, SubSystemRoleDO::getSubSystemId)),
                SubSystemDO::getId);
        Map<Long, OAuth2ClientDO> clientMap = convertMap(
                oauth2ClientMapper.selectList(OAuth2ClientDO::getId,
                        convertSet(subSystemMap.values(), SubSystemDO::getOauth2ClientId)),
                OAuth2ClientDO::getId);
        return list.stream().map(role -> {
            SubSystemRoleRespVO vo = BeanUtils.toBean(role, SubSystemRoleRespVO.class);
            SubSystemDO subSystem = subSystemMap.get(role.getSubSystemId());
            if (subSystem != null) {
                OAuth2ClientDO client = clientMap.get(subSystem.getOauth2ClientId());
                vo.setClientId(subSystem.resolvePortalClientId(client != null ? client.getClientId() : null));
                vo.setClientName(subSystem.getSystemName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private SubSystemRoleRespVO buildResp(SubSystemRoleDO role) {
        List<SubSystemRoleRespVO> list = buildRespList(Collections.singletonList(role));
        return list.isEmpty() ? BeanUtils.toBean(role, SubSystemRoleRespVO.class) : list.get(0);
    }

    private SubSystemDO validateSubSystemExists(Long subSystemId) {
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        // 可管系统范围校验：受限角色只能操作授权系统
        subSystemAccessService.checkAccessible(subSystemId);
        return subSystem;
    }

    private SubSystemRoleDO validateSubSystemRoleExists(Long id) {
        SubSystemRoleDO role = subSystemRoleMapper.selectById(id);
        if (role == null) {
            throw exception(SUB_SYSTEM_ROLE_NOT_EXISTS);
        }
        // 可管系统范围校验：受限角色只能操作授权系统的角色
        subSystemAccessService.checkAccessible(role.getSubSystemId());
        return role;
    }

    private void validateRoleDuplicate(Long subSystemId, String name, String code, Long id) {
        SubSystemRoleDO role = subSystemRoleMapper.selectBySubSystemIdAndName(subSystemId, name);
        if (role != null && !ObjectUtil.equal(role.getId(), id)) {
            throw exception(SUB_SYSTEM_ROLE_NAME_DUPLICATE, name);
        }
        role = subSystemRoleMapper.selectBySubSystemIdAndCode(subSystemId, code);
        if (role != null && !ObjectUtil.equal(role.getId(), id)) {
            throw exception(SUB_SYSTEM_ROLE_CODE_DUPLICATE, code);
        }
    }

    private void validateRoleNotAssigned(Long roleId) {
        Long count = subSystemUserRoleMapper.selectCountByRoleId(roleId);
        if (count != null && count > 0) {
            throw exception(SUB_SYSTEM_ROLE_HAS_USERS);
        }
    }

}
