package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

public interface SubSystemRoleService {

    PageResult<SubSystemRoleRespVO> getSubSystemRolePage(SubSystemRolePageReqVO pageReqVO);

    SubSystemRoleRespVO getSubSystemRole(Long id);

    Long createSubSystemRole(@Valid SubSystemRoleSaveReqVO createReqVO);

    void updateSubSystemRole(@Valid SubSystemRoleSaveReqVO updateReqVO);

    void deleteSubSystemRole(Long id);

    void deleteSubSystemRoleList(List<Long> ids);

    void updateSubSystemRoleStatus(Long id, Integer status);

    List<SubSystemMenuSimpleRespVO> getMenuSimpleList(Long subSystemId);

    Set<Long> getRoleMenuIds(Long roleId);

    void assignRoleMenu(@Valid SubSystemRoleAssignMenuReqVO reqVO);

    void assignRoleDataScope(@Valid SubSystemRoleAssignDataScopeReqVO reqVO);

}
