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
import cn.jonhon.jump.module.system.framework.subsystemapi.ApiEndpointSpecs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    private SubSystemEmployeeService subSystemEmployeeService;
    @Resource
    private SubSystemApiConfigMapper subSystemApiConfigMapper;
    @Resource
    private SubSystemApiEndpointMapper subSystemApiEndpointMapper;

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
    public SubSystemRoleCreateRespVO createSubSystemRole(SubSystemRoleSaveReqVO createReqVO) {
        validateSubSystemExists(createReqVO.getSubSystemId());
        boolean syncToExternal = Boolean.TRUE.equals(createReqVO.getSyncToExternal());
        String roleName = StrUtil.trim(createReqVO.getName());
        String workshopCode = resolveWorkshopCode(createReqVO.getSubSystemId(), createReqVO.getWorkshopCode(), roleName);
        if (!syncToExternal) {
            String localName = roleName;
            return new TransactionTemplate(transactionManager).execute(status ->
                    insertLocalRole(createReqVO, localName));
        }
        if (StrUtil.isBlank(workshopCode)) {
            throw exception(SUB_SYSTEM_ROLE_WORKSHOP_REQUIRED);
        }
        Long apiSubSystemId = createReqVO.getApiSubSystemId();
        if (apiSubSystemId == null || apiSubSystemId <= 0) {
            apiSubSystemId = createReqVO.getSubSystemId();
        }
        roleName = buildExternalRoleName(workshopCode, roleName);
        assertRoleNameLength(roleName);
        createReqVO.setName(roleName);
        String externalName = roleName;
        Long resolvedApiId = apiSubSystemId;
        // 先提交本地角色，再调 Camstar，避免查询和推送占着数据库连接
        Long roleId = new TransactionTemplate(transactionManager).execute(status -> {
            validateRoleDuplicate(createReqVO.getSubSystemId(), externalName, createReqVO.getCode(), null);
            return insertLocalRole(createReqVO, externalName).getId();
        });
        assertExternalRoleNameFree(resolvedApiId, workshopCode, externalName, true);
        try {
            subSystemApiConfigService.pushExternalRoleCreate(resolvedApiId, workshopCode, externalName);
        } catch (RuntimeException e) {
            throw exception0(BAD_REQUEST.getCode(),
                    "本地角色已保存，但推送到 Camstar 失败：" + e.getMessage()
                            + "。请到列表中重新注册，不要再次新建。");
        }
        markRoleRegistered(roleId, "1");
        String warning = storeExternalRoleId(roleId, resolvedApiId, workshopCode, externalName);
        return new SubSystemRoleCreateRespVO().setId(roleId).setWarning(warning);
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
    public String registerSubSystemRole(Long id, SubSystemRoleRegisterReqVO reqVO) {
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
        String externalName = buildExternalRoleName(workshopCode, roleName);
        assertRoleNameLength(externalName);
        if (!Objects.equals(externalName, roleName)) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                validateRoleDuplicate(role.getSubSystemId(), externalName, role.getCode(), role.getId());
                SubSystemRoleDO rename = new SubSystemRoleDO();
                rename.setId(role.getId());
                rename.setName(externalName);
                subSystemRoleMapper.updateById(rename);
            });
        }
        // 先确认对方没有同名角色，查询失败则不推送
        assertExternalRoleNameFree(apiSubSystemId, workshopCode, externalName, true);
        subSystemApiConfigService.pushExternalRoleCreate(apiSubSystemId, workshopCode, externalName);
        markRoleRegistered(role.getId(), "1");
        return storeExternalRoleId(role.getId(), apiSubSystemId, workshopCode, externalName);
    }

    /**
     * 推送成功后回查外部 roleId 并落库。
     * 查不到不回滚：对方角色已经建出，本地保留并标记已注册，外部编号留空，由管理员用【关联外部】补绑。
     *
     * @return 需要提示管理员补绑时的文案；已写入 roleId 时返回 null
     */
    private String storeExternalRoleId(Long roleId, Long apiSubSystemId, String workshopCode, String externalRoleName) {
        Map<String, String> nameToId;
        try {
            nameToId = lookupExternalRoles(apiSubSystemId, workshopCode, externalRoleName);
        } catch (RuntimeException e) {
            log.warn("[storeExternalRoleId] roleId={} 回查外部 roleId 失败", roleId, e);
            return "Camstar 角色【" + externalRoleName + "】已创建，但没有查回 roleId："
                    + e.getMessage() + "。本地角色已保留并标记为已注册，请用【关联外部】按角色名补绑";
        }
        String externalRoleId = findExternalRoleId(nameToId, externalRoleName);
        if (StrUtil.isBlank(externalRoleId)) {
            return "Camstar 角色【" + externalRoleName + "】已创建，但按角色名没有查到 roleId。本地角色已保留并标记为已注册，请用【关联外部】按角色名补绑";
        }
        SubSystemRoleDO updateObj = new SubSystemRoleDO();
        updateObj.setId(roleId);
        updateObj.setExternalRoleId(externalRoleId.trim());
        updateObj.setRoleRegistered("1");
        subSystemRoleMapper.updateById(updateObj);
        return null;
    }

    @Override
    public List<SubSystemExternalRoleRespVO> getExternalRoleList(Long apiSubSystemId, String workshopCode, String roleName) {
        Map<String, String> nameToId = lookupExternalRoles(apiSubSystemId,
                StrUtil.trimToNull(workshopCode), StrUtil.trimToNull(roleName));
        return nameToId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SubSystemExternalRoleRespVO()
                        .setRoleName(entry.getKey())
                        .setRoleId(entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 车间列表 + 按角色名全系统补查。只按车间拉列表时，对方若截断分页，新建角色会落在页外；
     * 按角色名再查一次（不带车间），与接口管理按条件查询同一路，保证目标角色在结果里。
     */
    private Map<String, String> lookupExternalRoles(Long apiSubSystemId, String workshopCode, String roleName) {
        Map<String, String> nameToId = new LinkedHashMap<>();
        RuntimeException workshopError = null;
        if (StrUtil.isNotBlank(workshopCode)) {
            try {
                nameToId.putAll(subSystemApiConfigService.queryExternalRoleIds(apiSubSystemId, workshopCode, null));
            } catch (RuntimeException e) {
                workshopError = e;
                log.warn("[lookupExternalRoles] 按车间查询外部角色失败，继续按角色名补查。workshop={}", workshopCode, e);
            }
        }
        if (StrUtil.isNotBlank(roleName)) {
            RuntimeException nameError = null;
            try {
                mergeNamedRoles(nameToId,
                        subSystemApiConfigService.queryExternalRoleIds(apiSubSystemId, null, roleName), roleName);
            } catch (RuntimeException e) {
                nameError = e;
                log.warn("[lookupExternalRoles] 按角色名查询外部角色失败。roleName={}", roleName, e);
            }
            // 对方角色名是 车间_角色名，本地若还是短名，再按完整名查一次（精确条件，不靠车间列表）
            if (StrUtil.isNotBlank(workshopCode) && !roleName.startsWith(workshopCode.trim() + "_")) {
                String prefixed = workshopCode.trim() + "_" + roleName.trim();
                try {
                    mergeNamedRoles(nameToId,
                            subSystemApiConfigService.queryExternalRoleIds(apiSubSystemId, null, prefixed), prefixed);
                } catch (RuntimeException e) {
                    if (nameError == null) {
                        nameError = e;
                    }
                    log.warn("[lookupExternalRoles] 按完整角色名查询外部角色失败。roleName={}", prefixed, e);
                }
            }
            if (nameToId.isEmpty() && nameError != null) {
                throw workshopError != null ? workshopError : nameError;
            }
        }
        if (nameToId.isEmpty() && workshopError != null) {
            throw workshopError;
        }
        return nameToId;
    }

    /** 角色名补查只并入同名角色，避免对方忽略 roleName、把全系统角色都灌进下拉。 */
    private static void mergeNamedRoles(Map<String, String> target, Map<String, String> queried, String roleName) {
        if (queried == null || queried.isEmpty() || StrUtil.isBlank(roleName)) {
            return;
        }
        String wanted = roleName.trim();
        for (Map.Entry<String, String> entry : queried.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(wanted)
                    && StrUtil.isNotBlank(entry.getValue())) {
                target.putIfAbsent(entry.getKey().trim(), entry.getValue().trim());
            }
        }
    }

    @Override
    public void bindExternalRole(Long id, String externalRoleId) {
        SubSystemRoleDO role = validateSubSystemRoleExists(id);
        String roleId = StrUtil.trimToNull(externalRoleId);
        if (roleId == null) {
            // 先按仍在管辖范围内的角色，把对方人员上的该角色卸下，再清本地编号
            detachExternalRoleFromUsers(role);
        }
        LambdaUpdateWrapper<SubSystemRoleDO> wrapper = new LambdaUpdateWrapper<SubSystemRoleDO>()
                .eq(SubSystemRoleDO::getId, id)
                .set(SubSystemRoleDO::getExternalRoleId, roleId)
                .set(SubSystemRoleDO::getRoleRegistered, roleId != null ? "1" : "0");
        subSystemRoleMapper.update(null, wrapper);
    }

    /**
     * 解除关联前，把已注册人员在 Camstar 上的该角色卸下。
     * 此时外部编号还在，同步仍把它算作 JUMP 管辖，不会被当成对方手工角色留下来。
     */
    private void detachExternalRoleFromUsers(SubSystemRoleDO role) {
        if (role == null || StrUtil.isBlank(role.getExternalRoleId())) {
            return;
        }
        List<SubSystemUserRoleDO> links = subSystemUserRoleMapper.selectListByRoleId(role.getId());
        if (CollUtil.isEmpty(links)) {
            return;
        }
        List<Long> userIds = links.stream().map(SubSystemUserRoleDO::getUserId).collect(Collectors.toList());
        List<SubSystemUsersDO> users = subSystemUsersMapper.selectBatchIds(userIds).stream()
                .filter(user -> "1".equals(user.getEmployeeRegistered()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(users)) {
            return;
        }
        Long apiSubSystemId = resolveCamstarApiSubSystemId(role.getSubSystemId());
        if (apiSubSystemId == null) {
            throw exception0(BAD_REQUEST.getCode(),
                    "已有人员使用该角色，但没有可用的 Camstar 人员接口，无法从对方系统卸下。"
                            + "请先配置人员接口后再解除关联。");
        }
        for (SubSystemUsersDO user : users) {
            String userCode = externalUserCode(user);
            if (StrUtil.isBlank(userCode)) {
                continue;
            }
            List<Long> keepRoleIds = subSystemUserRoleMapper.selectListByUserId(user.getId()).stream()
                    .map(SubSystemUserRoleDO::getRoleId)
                    .filter(roleId -> !Objects.equals(roleId, role.getId()))
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(keepRoleIds)) {
                keepRoleIds = subSystemRoleMapper.selectListByIds(keepRoleIds).stream()
                        .filter(item -> StrUtil.isNotBlank(item.getExternalRoleId()))
                        .map(SubSystemRoleDO::getId)
                        .collect(Collectors.toList());
            }
            subSystemEmployeeService.syncUserRoles(apiSubSystemId, role.getSubSystemId(), userCode, keepRoleIds);
        }
    }

    private Long resolveCamstarApiSubSystemId(Long rosterSubSystemId) {
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        if (rosterSubSystemId != null) {
            candidates.add(rosterSubSystemId);
        }
        List<Long> roleCreateIds = subSystemApiEndpointMapper
                .selectEnabledSubSystemIdsByPurpose(ApiEndpointSpecs.ROLE_CREATE);
        if (CollUtil.isNotEmpty(roleCreateIds)) {
            candidates.addAll(roleCreateIds);
        }
        List<Long> createIds = subSystemApiEndpointMapper
                .selectEnabledSubSystemIdsByPurpose(ApiEndpointSpecs.CREATE);
        if (CollUtil.isNotEmpty(createIds)) {
            candidates.addAll(createIds);
        }
        for (Long candidate : candidates) {
            SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(candidate);
            if (config != null && "camstar".equalsIgnoreCase(StrUtil.blankToDefault(config.getApiType(), ""))) {
                return candidate;
            }
        }
        return null;
    }

    private static String externalUserCode(SubSystemUsersDO user) {
        String workshop = StrUtil.trim(user.getWorkshopId());
        String bare = StrUtil.blankToDefault(user.getUsername(), "").trim();
        if ("1".equals(user.getUsernameWithWorkshop()) && StrUtil.isNotBlank(workshop)
                && !bare.startsWith(workshop + "_")) {
            return workshop + "_" + bare;
        }
        return bare;
    }

    /** 对方已有同名角色，或暂时查不到对方角色时，中止推送。 */
    private void assertExternalRoleNameFree(Long apiSubSystemId, String workshopCode, String roleName,
                                            boolean alreadyLocal) {
        Map<String, String> nameToId;
        try {
            // 车间列表 + 按角色名全系统补查：只看车间列表时，截断会把已有同名角色漏掉
            nameToId = lookupExternalRoles(apiSubSystemId, workshopCode, roleName);
        } catch (RuntimeException ex) {
            log.warn("[assertExternalRoleNameFree] 回查外部角色失败，已中止推送。roleName={}", roleName, ex);
            String saved = alreadyLocal ? "本地角色已保留。" : "";
            throw exception0(BAD_REQUEST.getCode(),
                    saved + "无法确认 Camstar 是否已有同名角色【" + roleName + "】，已中止推送。请稍后重试。"
                            + ex.getMessage());
        }
        if (StrUtil.isNotBlank(findExternalRoleId(nameToId, roleName))) {
            String tip = alreadyLocal
                    ? "Camstar 已存在同名角色【" + roleName + "】。本地角色已保留，请使用【关联外部】绑定，不要再次新建。"
                    : "Camstar 已存在同名角色【" + roleName + "】。请先保存本地角色，再使用【关联外部】。";
            throw exception0(BAD_REQUEST.getCode(), tip);
        }
    }

    private SubSystemRoleCreateRespVO insertLocalRole(SubSystemRoleSaveReqVO createReqVO, String roleName) {
        validateRoleDuplicate(createReqVO.getSubSystemId(), roleName, createReqVO.getCode(), null);
        SubSystemRoleDO role = BeanUtils.toBean(createReqVO, SubSystemRoleDO.class);
        role.setName(roleName);
        role.setType(RoleTypeEnum.CUSTOM.getType());
        role.setDataScope(DataScopeEnum.ALL.getScope());
        role.setMenuCheckStrictly(1);
        role.setDeptCheckStrictly(1);
        role.setRoleRegistered("0");
        subSystemRoleMapper.insert(role);
        return new SubSystemRoleCreateRespVO().setId(role.getId());
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
