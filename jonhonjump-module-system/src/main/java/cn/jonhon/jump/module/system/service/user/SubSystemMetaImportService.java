package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.dal.dataobject.user.*;
import cn.jonhon.jump.module.system.dal.mysql.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_IMPORT_LIST_EMPTY;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_IMPORT_NOT_BOUND;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_NOT_EXISTS;

/**
 * 子系统角色/菜单/岗位/班组导入（须先绑定已登记外部系统）。
 */
@Service
@Validated
public class SubSystemMetaImportService {

    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private SubSystemPostMapper subSystemPostMapper;
    @Resource
    private SubSystemTeamMapper subSystemTeamMapper;
    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private SubSystemRoleService subSystemRoleService;
    @Resource
    private SubSystemPostService subSystemPostService;
    @Resource
    private SubSystemTeamService subSystemTeamService;
    @Resource
    private SubSystemMenuService subSystemMenuService;

    @Transactional(rollbackFor = Exception.class)
    public SubSystemUserImportRespVO importRoleList(Long subSystemId, List<SubSystemRoleImportExcelVO> rows, boolean updateSupport) {
        validateBound(subSystemId, rows);
        SubSystemUserImportRespVO resp = emptyResp();
        int i = 1;
        for (SubSystemRoleImportExcelVO row : rows) {
            i++;
            String key = StrUtil.blankToDefault(row.getCode(), "第" + i + "行");
            try {
                if (StrUtil.isBlank(row.getName()) || StrUtil.isBlank(row.getCode())) {
                    resp.getFailureKeys().put(key, "角色名称/标识不能为空");
                    continue;
                }
                SubSystemRoleDO exist = subSystemRoleMapper.selectBySubSystemIdAndCode(subSystemId, row.getCode().trim());
                SubSystemRoleSaveReqVO req = new SubSystemRoleSaveReqVO();
                req.setSubSystemId(subSystemId);
                req.setName(row.getName().trim());
                req.setCode(row.getCode().trim());
                req.setSort(row.getSort() != null ? row.getSort() : 0);
                req.setStatus(normalizeStatusInt(row.getStatus()));
                if (exist == null) {
                    subSystemRoleService.createSubSystemRole(req);
                    resp.getCreateKeys().add(key);
                } else if (!updateSupport) {
                    resp.getFailureKeys().put(key, "角色标识已存在（未勾选更新）");
                } else {
                    req.setId(exist.getId());
                    subSystemRoleService.updateSubSystemRole(req);
                    resp.getUpdateKeys().add(key);
                }
            } catch (Exception ex) {
                resp.getFailureKeys().put(key, StrUtil.blankToDefault(ex.getMessage(), "导入失败"));
            }
        }
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public SubSystemUserImportRespVO importPostList(Long subSystemId, List<SubSystemPostImportExcelVO> rows, boolean updateSupport) {
        validateBound(subSystemId, rows);
        SubSystemUserImportRespVO resp = emptyResp();
        int i = 1;
        for (SubSystemPostImportExcelVO row : rows) {
            i++;
            String key = StrUtil.blankToDefault(row.getCode(), "第" + i + "行");
            try {
                if (StrUtil.isBlank(row.getName()) || StrUtil.isBlank(row.getCode())) {
                    resp.getFailureKeys().put(key, "岗位名称/编码不能为空");
                    continue;
                }
                SubSystemPostDO exist = subSystemPostMapper.selectBySubSystemIdAndCode(subSystemId, row.getCode().trim());
                SubSystemPostSaveReqVO req = new SubSystemPostSaveReqVO();
                req.setSubSystemId(subSystemId);
                req.setName(row.getName().trim());
                req.setCode(row.getCode().trim());
                req.setSort(row.getSort() != null ? row.getSort() : 0);
                req.setStatus(normalizeStatusInt(row.getStatus()));
                if (exist == null) {
                    subSystemPostService.createSubSystemPost(req);
                    resp.getCreateKeys().add(key);
                } else if (!updateSupport) {
                    resp.getFailureKeys().put(key, "岗位编码已存在（未勾选更新）");
                } else {
                    req.setId(exist.getId());
                    subSystemPostService.updateSubSystemPost(req);
                    resp.getUpdateKeys().add(key);
                }
            } catch (Exception ex) {
                resp.getFailureKeys().put(key, StrUtil.blankToDefault(ex.getMessage(), "导入失败"));
            }
        }
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public SubSystemUserImportRespVO importTeamList(Long subSystemId, List<SubSystemTeamImportExcelVO> rows, boolean updateSupport) {
        validateBound(subSystemId, rows);
        SubSystemUserImportRespVO resp = emptyResp();
        int i = 1;
        for (SubSystemTeamImportExcelVO row : rows) {
            i++;
            String key = StrUtil.blankToDefault(row.getTeamCode(), "第" + i + "行");
            try {
                if (StrUtil.isBlank(row.getTeamCode()) || StrUtil.isBlank(row.getTeamName())) {
                    resp.getFailureKeys().put(key, "班组编码/名称不能为空");
                    continue;
                }
                Long leaderId = resolveTeamLeaderId(subSystemId, row);
                SubSystemTeamDO exist = subSystemTeamMapper.selectBySubSystemIdAndTeamCode(subSystemId, row.getTeamCode().trim());
                SubSystemTeamSaveReqVO req = new SubSystemTeamSaveReqVO();
                req.setSubSystemId(subSystemId);
                req.setTeamCode(row.getTeamCode().trim());
                req.setTeamName(row.getTeamName().trim());
                req.setDescription(row.getDescription());
                req.setTeamLeaderId(leaderId);
                if (exist == null) {
                    subSystemTeamService.createSubSystemTeam(req);
                    resp.getCreateKeys().add(key);
                } else if (!updateSupport) {
                    resp.getFailureKeys().put(key, "班组编码已存在（未勾选更新）");
                } else {
                    req.setId(exist.getId());
                    subSystemTeamService.updateSubSystemTeam(req);
                    resp.getUpdateKeys().add(key);
                }
            } catch (Exception ex) {
                resp.getFailureKeys().put(key, StrUtil.blankToDefault(ex.getMessage(), "导入失败"));
            }
        }
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public SubSystemUserImportRespVO importMenuList(Long subSystemId, List<SubSystemMenuImportExcelVO> rows, boolean updateSupport) {
        validateBound(subSystemId, rows);
        SubSystemUserImportRespVO resp = emptyResp();
        int i = 1;
        for (SubSystemMenuImportExcelVO row : rows) {
            i++;
            String key = StrUtil.blankToDefault(row.getName(), "第" + i + "行");
            try {
                if (StrUtil.isBlank(row.getName()) || row.getType() == null) {
                    resp.getFailureKeys().put(key, "菜单名称/类型不能为空");
                    continue;
                }
                Long parentId = resolveParentMenuId(subSystemId, row.getParentName());
                SubSystemMenuDO exist = subSystemMenuMapper.selectBySubSystemIdAndParentIdAndName(
                        subSystemId, parentId, row.getName().trim());
                SubSystemMenuSaveReqVO req = new SubSystemMenuSaveReqVO();
                req.setSubSystemId(subSystemId);
                req.setParentId(parentId);
                req.setName(row.getName().trim());
                req.setType(row.getType());
                req.setSort(row.getSort() != null ? row.getSort() : 0);
                req.setPath(row.getPath());
                req.setComponent(row.getComponent());
                req.setComponentName(row.getComponentName());
                req.setPermission(row.getPermission());
                req.setIcon(row.getIcon());
                req.setStatus(normalizeStatusInt(row.getStatus()));
                req.setVisible(row.getVisible() != null ? row.getVisible() : Boolean.TRUE);
                if (exist == null) {
                    subSystemMenuService.createSubSystemMenu(req);
                    resp.getCreateKeys().add(key);
                } else if (!updateSupport) {
                    resp.getFailureKeys().put(key, "同级菜单名称已存在（未勾选更新）");
                } else {
                    req.setId(exist.getId());
                    // 保留未在 Excel 中的字段：从已有 DO 尽量不丢
                    subSystemMenuService.updateSubSystemMenu(req);
                    resp.getUpdateKeys().add(key);
                }
            } catch (Exception ex) {
                resp.getFailureKeys().put(key, StrUtil.blankToDefault(ex.getMessage(), "导入失败"));
            }
        }
        return resp;
    }

    private Long resolveParentMenuId(Long subSystemId, String parentName) {
        if (StrUtil.isBlank(parentName) || "根".equals(parentName.trim()) || "0".equals(parentName.trim())) {
            return 0L;
        }
        List<SubSystemMenuDO> menus = subSystemMenuMapper.selectListBySubSystemId(subSystemId);
        if (CollUtil.isEmpty(menus)) {
            throw new IllegalArgumentException("父菜单不存在：" + parentName);
        }
        String target = parentName.trim();
        for (SubSystemMenuDO menu : menus) {
            if (Objects.equals(menu.getMenuName(), target)) {
                return menu.getId();
            }
        }
        throw new IllegalArgumentException("父菜单不存在：" + parentName);
    }

    private Long resolveTeamLeaderId(Long subSystemId, SubSystemTeamImportExcelVO row) {
        if (StrUtil.isBlank(row.getLeaderUserUid()) && StrUtil.isBlank(row.getLeaderUsername())) {
            return null;
        }
        AdminUserDO main = null;
        if (StrUtil.isNotBlank(row.getLeaderUserUid())) {
            main = adminUserMapper.selectByUserUid(row.getLeaderUserUid().trim());
        }
        if (main == null && StrUtil.isNotBlank(row.getLeaderUsername())) {
            main = adminUserMapper.selectByUsername(row.getLeaderUsername().trim());
        }
        if (main == null) {
            throw new IllegalArgumentException("未找到班组长对应的主系统用户");
        }
        SubSystemUsersDO subUser = subSystemUsersMapper.selectBySubSystemIdAndMainUserId(subSystemId, main.getId());
        if (subUser == null) {
            throw new IllegalArgumentException("班组长尚未绑定到该外部系统，请先导入/关联子系统用户");
        }
        return subUser.getId();
    }

    private void validateBound(Long subSystemId, List<?> rows) {
        if (subSystemId == null) {
            throw exception(SUB_SYSTEM_IMPORT_NOT_BOUND);
        }
        if (subSystemMapper.selectById(subSystemId) == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        if (CollUtil.isEmpty(rows)) {
            throw exception(SUB_SYSTEM_IMPORT_LIST_EMPTY);
        }
    }

    private static SubSystemUserImportRespVO emptyResp() {
        return SubSystemUserImportRespVO.builder()
                .createKeys(new ArrayList<>())
                .updateKeys(new ArrayList<>())
                .failureKeys(new LinkedHashMap<>())
                .build();
    }

    private static Integer normalizeStatusInt(Integer status) {
        if (status == null) {
            return 0;
        }
        return status == 1 ? 1 : 0;
    }

}
