package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserImportExcelVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserImportRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUsersAssignRoleReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUsersSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_IMPORT_LIST_EMPTY;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_IMPORT_NOT_BOUND;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_NOT_EXISTS;

/**
 * 子系统用户导入：写入独立花名册，不强制匹配主系统用户。
 */
@Service
@Validated
public class SubSystemUserImportService {

    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private SubSystemUsersService subSystemUsersService;
    @Resource
    private SubSystemAccessService subSystemAccessService;

    @Transactional(rollbackFor = Exception.class)
    public SubSystemUserImportRespVO importUserList(Long subSystemId,
                                                    List<SubSystemUserImportExcelVO> importUsers,
                                                    boolean updateSupport) {
        if (subSystemId == null) {
            throw exception(SUB_SYSTEM_IMPORT_NOT_BOUND);
        }
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        // 可管系统范围校验：受限角色只能导入授权系统
        subSystemAccessService.checkAccessible(subSystemId);
        if (CollUtil.isEmpty(importUsers)) {
            throw exception(SUB_SYSTEM_IMPORT_LIST_EMPTY);
        }

        SubSystemUserImportRespVO resp = SubSystemUserImportRespVO.builder()
                .createKeys(new ArrayList<>())
                .updateKeys(new ArrayList<>())
                .failureKeys(new LinkedHashMap<>())
                .build();

        int rowIndex = 1;
        for (SubSystemUserImportExcelVO row : importUsers) {
            rowIndex++;
            String rowKey = buildRowKey(row, rowIndex);
            try {
                if (StrUtil.isBlank(row.getUsername())) {
                    resp.getFailureKeys().put(rowKey, "用户名不能为空");
                    continue;
                }
                String username = row.getUsername().trim();
                String workshopId = StrUtil.trim(row.getWorkshopId());
                // 角色入库唯一入口是「分配角色」：导入填了角色标识时，走统一入口写角色
                List<Long> roleIds = null;
                if (StrUtil.isNotBlank(row.getRoleCodes())) {
                    ResolvedRoles resolved = resolveRoleIds(subSystemId, row.getRoleCodes());
                    if (CollUtil.isNotEmpty(resolved.unknownCodes)) {
                        resp.getFailureKeys().put(rowKey, "角色标识未匹配：" + String.join("、", resolved.unknownCodes));
                        continue;
                    }
                    roleIds = resolved.roleIds;
                }
                SubSystemUsersDO exist = findImportRow(subSystemId, username, workshopId);
                if (exist == null) {
                    SubSystemUsersSaveReqVO createReq = new SubSystemUsersSaveReqVO();
                    createReq.setSubSystemId(subSystemId);
                    createReq.setUsername(username);
                    createReq.setNickname(row.getNickname());
                    createReq.setWorkshopId(row.getWorkshopId());
                    createReq.setTeamId(row.getTeamId());
                    createReq.setStatus(normalizeStatus(row.getStatus()));
                    createReq.setRemark(row.getRemark());
                    Long createdId = subSystemUsersService.createSubSystemUser(createReq);
                    if (CollUtil.isNotEmpty(roleIds)) {
                        assignImportedRoles(createdId, roleIds);
                    }
                    resp.getCreateKeys().add(rowKey);
                } else if (!updateSupport) {
                    resp.getFailureKeys().put(rowKey, "该用户名已存在（未勾选更新）");
                } else {
                    SubSystemUsersSaveReqVO updateReq = new SubSystemUsersSaveReqVO();
                    updateReq.setId(exist.getId());
                    updateReq.setSubSystemId(subSystemId);
                    updateReq.setMainUserId(exist.getMainUserId());
                    updateReq.setUsername(username);
                    updateReq.setNickname(StrUtil.blankToDefault(row.getNickname(), exist.getNickname()));
                    updateReq.setWorkshopId(StrUtil.blankToDefault(row.getWorkshopId(), exist.getWorkshopId()));
                    updateReq.setTeamId(StrUtil.blankToDefault(row.getTeamId(), exist.getTeamId()));
                    updateReq.setStatus(StrUtil.blankToDefault(normalizeStatus(row.getStatus()), exist.getStatus()));
                    updateReq.setRemark(StrUtil.blankToDefault(row.getRemark(), exist.getRemark()));
                    updateReq.setHomeMenuId(exist.getHomeMenuId());
                    updateReq.setPostIds(null);
                    subSystemUsersService.updateSubSystemUser(updateReq);
                    if (CollUtil.isNotEmpty(roleIds)) {
                        assignImportedRoles(exist.getId(), roleIds);
                    }
                    resp.getUpdateKeys().add(rowKey);
                }
            } catch (Exception ex) {
                resp.getFailureKeys().put(rowKey, StrUtil.blankToDefault(ex.getMessage(), "导入失败"));
            }
        }
        return resp;
    }

    /** 导入的角色标识走「分配角色」统一入口写角色（不勾选外部同步） */
    private void assignImportedRoles(Long userId, List<Long> roleIds) {
        SubSystemUsersAssignRoleReqVO assignReq = new SubSystemUsersAssignRoleReqVO();
        assignReq.setId(userId);
        assignReq.setRoleIds(roleIds);
        subSystemUsersService.assignSubSystemUserRole(assignReq);
    }

    /**
     * Excel 填了车间就按 (系统, 用户名, 车间) 精确命中，没有则新建该叶子。
     * 车间为空时只更新唯一的空车间行；已经挂在叶子上则要求补车间编号，避免改到别的叶子或再插一条空车间。
     */
    private SubSystemUsersDO findImportRow(Long subSystemId, String username, String workshopId) {
        List<SubSystemUsersDO> rows = subSystemUsersMapper.selectListBySubSystemIdAndUsername(subSystemId, username);
        if (CollUtil.isEmpty(rows)) {
            return null;
        }
        if (StrUtil.isNotBlank(workshopId)) {
            List<SubSystemUsersDO> exact = rows.stream()
                    .filter(row -> workshopId.equals(StrUtil.trim(row.getWorkshopId())))
                    .collect(Collectors.toList());
            if (exact.size() > 1) {
                throw exception0(BAD_REQUEST.getCode(), "该工号在车间 " + workshopId + " 存在多条记录，请先清理重复数据");
            }
            return exact.isEmpty() ? null : exact.get(0);
        }
        List<SubSystemUsersDO> blank = rows.stream()
                .filter(row -> StrUtil.isBlank(row.getWorkshopId()))
                .collect(Collectors.toList());
        if (rows.size() == 1 && blank.size() == 1) {
            return blank.get(0);
        }
        if (rows.size() == 1) {
            throw exception0(BAD_REQUEST.getCode(),
                    "该工号已挂在车间 " + StrUtil.trim(rows.get(0).getWorkshopId()) + "，请在 Excel 填车间编号");
        }
        throw exception0(BAD_REQUEST.getCode(), "该工号在多个车间存在，请在 Excel 填车间编号");
    }

    private ResolvedRoles resolveRoleIds(Long subSystemId, String roleCodes) {
        ResolvedRoles resolved = new ResolvedRoles();
        List<String> codes = Arrays.stream(roleCodes.split("[,，;；\\s]+"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (codes.isEmpty()) {
            return resolved;
        }
        Map<String, Long> roleIdByCode = new LinkedHashMap<>();
        for (SubSystemRoleDO role : CollUtil.emptyIfNull(subSystemRoleMapper.selectListBySubSystemId(subSystemId))) {
            if (StrUtil.isNotBlank(role.getCode())) {
                roleIdByCode.putIfAbsent(role.getCode(), role.getId());
            }
        }
        for (String code : codes) {
            Long roleId = roleIdByCode.get(code);
            if (roleId == null) {
                resolved.unknownCodes.add(code);
            } else {
                resolved.roleIds.add(roleId);
            }
        }
        return resolved;
    }

    private static final class ResolvedRoles {
        private final List<Long> roleIds = new ArrayList<>();
        private final List<String> unknownCodes = new ArrayList<>();
    }

    private static String normalizeStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return "0";
        }
        String s = status.trim();
        if ("1".equals(s) || "停用".equals(s) || "禁用".equals(s)) {
            return "1";
        }
        return "0";
    }

    private static String buildRowKey(SubSystemUserImportExcelVO row, int rowIndex) {
        String username = StrUtil.trim(row.getUsername());
        String workshopId = StrUtil.trim(row.getWorkshopId());
        if (StrUtil.isNotBlank(username) && StrUtil.isNotBlank(workshopId)) {
            return username + " / " + workshopId;
        }
        if (StrUtil.isNotBlank(username)) {
            return username;
        }
        return "第" + rowIndex + "行";
    }

}
