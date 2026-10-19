package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemHangableNodeRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemWorkshopDO;
import cn.jonhon.jump.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemWorkshopMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

/**
 *  sub_system_workshop
 */
@Service
@Validated
@Slf4j
public class SubSystemWorkshopServiceImpl implements SubSystemWorkshopService {

    @Resource
    private SubSystemWorkshopMapper workshopMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private OAuth2ClientMapper oauth2ClientMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private SubSystemAccessService subSystemAccessService;
    @Resource
    private SubSystemPermissionContextService subSystemPermissionContextService;

    @Override
    public List<SubSystemWorkshopRespVO> getEnabledWorkshopList() {
        Map<Long, SubSystemDO> byId = buildSubSystemByIdMap();
        Map<String, SubSystemDO> byClientId = buildClientIdSubSystemMap(byId.values());
        List<SubSystemWorkshopDO> rows = loadConfigRows();
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> toVoFromConfig(row, byId, byClientId, true))
                .collect(Collectors.toList());
    }

    @Override
    public List<SubSystemHangableNodeRespVO> getHangableTree() {
        Map<Long, SubSystemDO> byId = buildSubSystemByIdMap();
        List<SubSystemWorkshopDO> workshopRows = loadConfigRows();
        Map<Long, List<SubSystemWorkshopDO>> workshopsBySubSystem = new LinkedHashMap<>();
        Map<String, SubSystemDO> byClientId = buildClientIdSubSystemMap(byId.values());
        for (SubSystemWorkshopDO row : workshopRows) {
            Long ssId = resolveSubSystemId(row, byId, byClientId);
            if (ssId == null || StrUtil.isBlank(row.getWorkshopCode())) {
                continue;
            }
            workshopsBySubSystem.computeIfAbsent(ssId, k -> new ArrayList<>()).add(row);
        }

        List<SubSystemHangableNodeRespVO> tree = new ArrayList<>();
        // 可管系统范围：与其它管理页一致，受限角色只看到授权系统
        Set<Long> allowedSubSystemIds = subSystemAccessService.getAllowedSubSystemIds(
                SecurityFrameworkUtils.getLoginUserId());
        List<SubSystemDO> systems = byId.values().stream()
                .filter(ss -> ss != null && ss.portalBound())
                .filter(ss -> allowedSubSystemIds == null || allowedSubSystemIds.contains(ss.getId()))
                .sorted(Comparator.comparing(SubSystemDO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());

        for (SubSystemDO ss : systems) {
            String clientId = resolveClientId(ss);
            List<SubSystemWorkshopDO> leaves = workshopsBySubSystem.getOrDefault(ss.getId(), Collections.emptyList());
            // 未配置车间叶子：业务系统本身作为一个可挂接节点
            if (CollUtil.isEmpty(leaves)) {
                tree.add(toHangableSystemOnly(ss, clientId));
                continue;
            }
            List<SubSystemHangableNodeRespVO> children = leaves.stream()
                    .sorted(Comparator
                            .comparing((SubSystemWorkshopDO w) -> w.getSort() == null ? 0 : w.getSort())
                            .thenComparing(w -> StrUtil.blankToDefault(w.getWorkshopCode(), "")))
                    .map(w -> toHangableLeaf(ss, clientId, w, leaves.size() > 1))
                    .collect(Collectors.toList());
            if (children.size() == 1) {
                tree.add(children.get(0));
            } else {
                SubSystemHangableNodeRespVO group = new SubSystemHangableNodeRespVO();
                group.setNodeType("group");
                group.setSubSystemId(ss.getId());
                group.setClientId(clientId);
                group.setSubSystemName(ss.getSystemName());
                group.setWorkshopScopeEnabled(true);
                group.setLabel(formatSystemLabel(ss.getSystemName(), clientId));
                group.setChildren(children);
                tree.add(group);
            }
        }
        return tree;
    }

    @Override
    public PageResult<SubSystemWorkshopRespVO> getWorkshopPage(SubSystemWorkshopPageReqVO pageReqVO) {
        Set<Long> allowed = subSystemAccessService.getAllowedSubSystemIds(SecurityFrameworkUtils.getLoginUserId());
        if (pageReqVO.getSubSystemId() != null) {
            validateSubSystemExists(pageReqVO.getSubSystemId());
        } else if (allowed != null && allowed.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        PageResult<SubSystemWorkshopDO> page = workshopMapper.selectPage(pageReqVO, allowed);
        Map<Long, SubSystemDO> byId = buildSubSystemByIdMap();
        Map<String, SubSystemDO> byClientId = buildClientIdSubSystemMap(byId.values());
        List<SubSystemWorkshopRespVO> list = page.getList().stream()
                .map(row -> toVoFromConfig(row, byId, byClientId, true))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public SubSystemWorkshopRespVO getWorkshop(Long id) {
        SubSystemWorkshopDO row = validateWorkshopExists(id);
        Map<Long, SubSystemDO> byId = buildSubSystemByIdMap();
        Map<String, SubSystemDO> byClientId = buildClientIdSubSystemMap(byId.values());
        return toVoFromConfig(row, byId, byClientId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkshop(SubSystemWorkshopSaveReqVO createReqVO) {
        SubSystemDO ss = validateSubSystemExists(createReqVO.getSubSystemId());
        String code = normalizeCode(createReqVO.getWorkshopCode());
        validateCodeDuplicate(code, null);

        SubSystemWorkshopDO row = BeanUtils.toBean(createReqVO, SubSystemWorkshopDO.class);
        row.setWorkshopCode(code);
        row.setWorkshopName(StrUtil.trim(createReqVO.getWorkshopName()));
        row.setProductLine(StrUtil.trimToNull(createReqVO.getProductLine()));
        row.setRemark(StrUtil.trimToNull(createReqVO.getRemark()));
        row.setSort(createReqVO.getSort() == null ? 0 : createReqVO.getSort());
        row.setSubSystemId(ss.getId());
        row.setClientId(resolveClientId(ss));
        workshopMapper.insert(row);
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkshop(SubSystemWorkshopSaveReqVO updateReqVO) {
        SubSystemWorkshopDO exist = validateWorkshopExists(updateReqVO.getId());
        if (exist.getSubSystemId() == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        updateReqVO.setSubSystemId(exist.getSubSystemId());
        SubSystemDO ss = validateSubSystemExists(exist.getSubSystemId());
        String code = normalizeCode(updateReqVO.getWorkshopCode());
        validateCodeDuplicate(code, updateReqVO.getId());
        boolean leafIdentityChanged = false;
        if (!code.equals(StrUtil.trim(exist.getWorkshopCode()))) {
            assertNoUsersOrRoles(exist.getSubSystemId() != null ? exist.getSubSystemId() : ss.getId(),
                    exist.getWorkshopCode());
            leafIdentityChanged = true;
        }

        SubSystemWorkshopDO update = BeanUtils.toBean(updateReqVO, SubSystemWorkshopDO.class);
        update.setWorkshopCode(code);
        update.setWorkshopName(StrUtil.trim(updateReqVO.getWorkshopName()));
        update.setProductLine(StrUtil.trimToNull(updateReqVO.getProductLine()));
        update.setRemark(StrUtil.trimToNull(updateReqVO.getRemark()));
        update.setSort(updateReqVO.getSort() == null ? 0 : updateReqVO.getSort());
        update.setSubSystemId(ss.getId());
        update.setClientId(resolveClientId(ss));
        workshopMapper.updateById(update);
        if (leafIdentityChanged) {
            // 叶子编号变了：按车间缓存的 my-menus/快捷导航全部失效
            subSystemPermissionContextService.evictBySubSystemId(ss.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkshop(Long id) {
        SubSystemWorkshopDO exist = validateWorkshopExists(id);
        assertNoUsersOrRoles(exist.getSubSystemId(), exist.getWorkshopCode());
        workshopMapper.deleteById(id);
        if (exist.getSubSystemId() != null) {
            // 叶子下线：清掉按该车间缓存的 my-menus / 快捷导航，避免已删车间继续可用
            subSystemPermissionContextService.evictBySubSystemId(exist.getSubSystemId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkshopList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        ids.forEach(this::deleteWorkshop);
    }

    private SubSystemHangableNodeRespVO toHangableSystemOnly(SubSystemDO ss, String clientId) {
        SubSystemHangableNodeRespVO leaf = new SubSystemHangableNodeRespVO();
        leaf.setNodeType("leaf");
        leaf.setSubSystemId(ss.getId());
        leaf.setClientId(clientId);
        leaf.setSubSystemName(ss.getSystemName());
        leaf.setWorkshopCode("");
        leaf.setWorkshopName(ss.getSystemName());
        leaf.setWorkshopScopeEnabled(false);
        leaf.setLabel(formatSystemLabel(ss.getSystemName(), clientId));
        return leaf;
    }

    private SubSystemHangableNodeRespVO toHangableLeaf(SubSystemDO ss, String clientId,
                                                       SubSystemWorkshopDO w, boolean multiLeaf) {
        SubSystemHangableNodeRespVO leaf = new SubSystemHangableNodeRespVO();
        leaf.setNodeType("leaf");
        leaf.setSubSystemId(ss.getId());
        leaf.setClientId(clientId);
        leaf.setSubSystemName(ss.getSystemName());
        leaf.setWorkshopCode(w.getWorkshopCode().trim());
        leaf.setWorkshopName(w.getWorkshopName());
        leaf.setWorkshopScopeEnabled(multiLeaf || ss.workshopScopeOn());
        if (multiLeaf) {
            leaf.setLabel(formatWorkshopLabel(w.getWorkshopName(), w.getWorkshopCode()));
        } else {
            leaf.setLabel(formatSystemLabel(
                    StrUtil.blankToDefault(w.getWorkshopName(), ss.getSystemName()), clientId));
        }
        return leaf;
    }

    private List<SubSystemWorkshopDO> loadConfigRows() {
        try {
            return CollUtil.emptyIfNull(workshopMapper.selectEnabledList());
        } catch (Exception ex) {
            log.warn("[loadConfigRows] sub_system_workshop unavailable: {}", ex.toString());
            return Collections.emptyList();
        }
    }

    private SubSystemWorkshopRespVO toVoFromConfig(SubSystemWorkshopDO row,
                                                  Map<Long, SubSystemDO> byId,
                                                  Map<String, SubSystemDO> byClientId,
                                                  boolean withCounts) {
        SubSystemWorkshopRespVO vo = BeanUtils.toBean(row, SubSystemWorkshopRespVO.class);
        vo.setWorkshopCode(row.getWorkshopCode());
        vo.setWorkshopName(row.getWorkshopName());
        vo.setProductLine(row.getProductLine());
        vo.setClientId(row.getClientId());
        vo.setSort(row.getSort());
        vo.setStatus(row.getStatus());
        vo.setRemark(row.getRemark());
        vo.setCreateTime(row.getCreateTime());

        SubSystemDO ss = null;
        if (row.getSubSystemId() != null) {
            ss = byId.get(row.getSubSystemId());
        }
        if (ss == null && StrUtil.isNotBlank(row.getClientId())) {
            ss = byClientId.get(normalizeClientId(row.getClientId()));
        }
        if (ss != null) {
            vo.setSubSystemId(ss.getId());
            vo.setSubSystemName(ss.getSystemName());
            if (StrUtil.isBlank(vo.getClientId())) {
                vo.setClientId(resolveClientId(ss));
            }
            if (withCounts) {
                fillCounts(vo, ss.getId(), row.getWorkshopCode());
            }
        } else {
            vo.setSubSystemId(row.getSubSystemId());
            vo.setUserCount(0L);
            vo.setRoleCount(0L);
        }
        return vo;
    }

    private Long resolveSubSystemId(SubSystemWorkshopDO row,
                                    Map<Long, SubSystemDO> byId,
                                    Map<String, SubSystemDO> byClientId) {
        if (row.getSubSystemId() != null && byId.containsKey(row.getSubSystemId())) {
            return row.getSubSystemId();
        }
        if (StrUtil.isNotBlank(row.getClientId())) {
            SubSystemDO ss = byClientId.get(normalizeClientId(row.getClientId()));
            return ss != null ? ss.getId() : null;
        }
        return null;
    }

    private void fillCounts(SubSystemWorkshopRespVO vo, Long subSystemId, String workshopCode) {
        if (subSystemId == null || StrUtil.isBlank(workshopCode)) {
            vo.setUserCount(0L);
            vo.setRoleCount(0L);
            return;
        }
        vo.setUserCount(subSystemUsersMapper.selectCountBySubSystemIdAndWorkshopId(subSystemId, workshopCode));
        vo.setRoleCount(subSystemRoleMapper.selectCountBySubSystemIdAndWorkshopCode(subSystemId, workshopCode));
    }

    private void assertNoUsersOrRoles(Long subSystemId, String workshopCode) {
        if (subSystemId == null || StrUtil.isBlank(workshopCode)) {
            return;
        }
        Long users = subSystemUsersMapper.selectCountBySubSystemIdAndWorkshopId(subSystemId, workshopCode);
        if (users != null && users > 0) {
            throw exception(SUB_SYSTEM_WORKSHOP_HAS_USERS);
        }
        Long roles = subSystemRoleMapper.selectCountBySubSystemIdAndWorkshopCode(subSystemId, workshopCode);
        if (roles != null && roles > 0) {
            throw exception(SUB_SYSTEM_WORKSHOP_HAS_ROLES);
        }
    }

    private SubSystemWorkshopDO validateWorkshopExists(Long id) {
        SubSystemWorkshopDO row = workshopMapper.selectById(id);
        if (row == null) {
            throw exception(SUB_SYSTEM_WORKSHOP_NOT_EXISTS);
        }
        if (row.getSubSystemId() != null) {
            subSystemAccessService.checkAccessible(row.getSubSystemId());
        }
        return row;
    }

    private SubSystemDO validateSubSystemExists(Long subSystemId) {
        SubSystemDO ss = subSystemMapper.selectById(subSystemId);
        if (ss == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        subSystemAccessService.checkAccessible(subSystemId);
        return ss;
    }

    private void validateCodeDuplicate(String workshopCode, Long id) {
        SubSystemWorkshopDO exists = workshopMapper.selectByWorkshopCode(workshopCode);
        if (exists != null && !ObjectUtil.equal(exists.getId(), id)) {
            throw exception(SUB_SYSTEM_WORKSHOP_CODE_DUPLICATE, workshopCode);
        }
    }

    private static String normalizeCode(String raw) {
        return StrUtil.trim(raw);
    }

    private String resolveClientId(SubSystemDO ss) {
        if (StrUtil.isNotBlank(ss.getClientId())) {
            return ss.getClientId().trim();
        }
        if (ss.getOauth2ClientId() == null) {
            return null;
        }
        OAuth2ClientDO client = oauth2ClientMapper.selectById(ss.getOauth2ClientId());
        return client != null ? StrUtil.trim(client.getClientId()) : null;
    }

    private static String formatSystemLabel(String systemName, String clientId) {
        String name = StrUtil.trim(systemName);
        String cid = StrUtil.trim(clientId);
        if (StrUtil.isNotBlank(name) && StrUtil.isNotBlank(cid)) {
            return name.indexOf(cid) >= 0 ? name : (name + " (" + cid + ")");
        }
        return StrUtil.blankToDefault(name, cid);
    }

    private static String formatWorkshopLabel(String workshopName, String workshopCode) {
        String name = StrUtil.trim(workshopName);
        String code = StrUtil.trim(workshopCode);
        if (StrUtil.isBlank(name)) {
            return code;
        }
        if (StrUtil.isBlank(code)) {
            return name;
        }
        if (name.indexOf('(' + code + ')') >= 0 || name.endsWith(code)) {
            return name;
        }
        return name + "(" + code + ")";
    }

    private Map<Long, SubSystemDO> buildSubSystemByIdMap() {
        List<SubSystemDO> systems = subSystemMapper.selectListOrderByOauth2ClientId();
        if (CollUtil.isEmpty(systems)) {
            return Collections.emptyMap();
        }
        Map<Long, SubSystemDO> map = new HashMap<>();
        for (SubSystemDO ss : systems) {
            if (ss.getId() != null) {
                map.put(ss.getId(), ss);
            }
        }
        return map;
    }

    private Map<String, SubSystemDO> buildClientIdSubSystemMap(Iterable<SubSystemDO> systems) {
        Map<String, SubSystemDO> map = new HashMap<>();
        for (SubSystemDO ss : systems) {
            String clientId = resolveClientId(ss);
            if (StrUtil.isNotBlank(clientId)) {
                map.put(normalizeClientId(clientId), ss);
            }
        }
        return map;
    }

    private static String normalizeClientId(String clientId) {
        return clientId == null ? "" : clientId.trim().toLowerCase(Locale.ROOT);
    }

}
