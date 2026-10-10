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

    /**
     * 按车间列出外部系统（Camstar）已有角色（角色名 + roleId），供「关联外部角色」选择
     */
    List<SubSystemExternalRoleRespVO> getExternalRoleList(Long apiSubSystemId, String workshopCode);

    /**
     * 关联/解除外部系统角色：把外部 roleId 绑到本地角色上（external_role_id），
     * 之后分配角色同步按该 ID 上挂；传空解除关联
     */
    void bindExternalRole(Long id, String externalRoleId);

    List<SubSystemMenuSimpleRespVO> getMenuSimpleList(Long subSystemId);

    Set<Long> getRoleMenuIds(Long roleId);

    void assignRoleMenu(@Valid SubSystemRoleAssignMenuReqVO reqVO);

    void assignRoleDataScope(@Valid SubSystemRoleAssignDataScopeReqVO reqVO);

}
