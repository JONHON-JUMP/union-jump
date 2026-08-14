package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserImportExcelVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserImportRespVO;
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
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
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
                List<Long> roleIds = StrUtil.isBlank(row.getRoleCodes())
                        ? null
                        : resolveRoleIds(subSystemId, row.getRoleCodes());
                SubSystemUsersDO exist = subSystemUsersMapper.selectBySubSystemIdAndUsername(subSystemId, username);
                if (exist == null) {
                    SubSystemUsersSaveReqVO createReq = new SubSystemUsersSaveReqVO();
                    createReq.setSubSystemId(subSystemId);
                    createReq.setUsername(username);
                    createReq.setNickname(row.getNickname());
                    createReq.setWorkshopId(row.getWorkshopId());
                    createReq.setTeamId(row.getTeamId());
                    createReq.setRoleIds(roleIds != null ? roleIds : new ArrayList<>());
                    createReq.setStatus(normalizeStatus(row.getStatus()));
                    createReq.setRemark(row.getRemark());
                    subSystemUsersService.createSubSystemUser(createReq);
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
                    updateReq.setRoleIds(roleIds);
                    updateReq.setStatus(StrUtil.blankToDefault(normalizeStatus(row.getStatus()), exist.getStatus()));
                    updateReq.setRemark(StrUtil.blankToDefault(row.getRemark(), exist.getRemark()));
                    updateReq.setHomeMenuId(exist.getHomeMenuId());
                    updateReq.setPostIds(null);
                    subSystemUsersService.updateSubSystemUser(updateReq);
                    resp.getUpdateKeys().add(rowKey);
                }
            } catch (Exception ex) {
                resp.getFailureKeys().put(rowKey, StrUtil.blankToDefault(ex.getMessage(), "导入失败"));
            }
        }
        return resp;
    }

    private List<Long> resolveRoleIds(Long subSystemId, String roleCodes) {
        if (StrUtil.isBlank(roleCodes)) {
            return new ArrayList<>();
        }
        List<String> codes = Arrays.stream(roleCodes.split("[,，;；\\s]+"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        if (codes.isEmpty()) {
            return new ArrayList<>();
        }
        List<SubSystemRoleDO> roles = subSystemRoleMapper.selectListBySubSystemId(subSystemId);
        if (CollUtil.isEmpty(roles)) {
            return new ArrayList<>();
        }
        return roles.stream()
                .filter(r -> codes.stream().anyMatch(c -> Objects.equals(c, r.getCode())))
                .map(SubSystemRoleDO::getId)
                .collect(Collectors.toList());
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
        if (StrUtil.isNotBlank(row.getUsername())) {
            return row.getUsername().trim();
        }
        return "第" + rowIndex + "行";
    }

}
