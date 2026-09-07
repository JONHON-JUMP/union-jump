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

    /**
     * 修改角色接口注册状态（0未注册 1已注册；人工在对方系统建过可标已注册，改回未注册可重推）
     */
    void updateSubSystemRoleRegisterStatus(Long id, String roleRegistered);

    /**
     * 未注册角色补调对方「角色新增」接口；成功后置已注册
     */
    void registerSubSystemRole(Long id, SubSystemRoleRegisterReqVO reqVO);

    List<SubSystemMenuSimpleRespVO> getMenuSimpleList(Long subSystemId);

    Set<Long> getRoleMenuIds(Long roleId);

    void assignRoleMenu(@Valid SubSystemRoleAssignMenuReqVO reqVO);

    void assignRoleDataScope(@Valid SubSystemRoleAssignDataScopeReqVO reqVO);

}
