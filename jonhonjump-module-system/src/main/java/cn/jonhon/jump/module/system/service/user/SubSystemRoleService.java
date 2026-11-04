package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

public interface SubSystemRoleService {

    PageResult<SubSystemRoleRespVO> getSubSystemRolePage(SubSystemRolePageReqVO pageReqVO);

    SubSystemRoleRespVO getSubSystemRole(Long id);

    /**
     * 创建角色。勾选同步外部时，若对方已建角色但未查回 roleId，本地角色仍保留，warning 说明补绑方式。
     */
    SubSystemRoleCreateRespVO createSubSystemRole(@Valid SubSystemRoleSaveReqVO createReqVO);

    void updateSubSystemRole(@Valid SubSystemRoleSaveReqVO updateReqVO);

    void deleteSubSystemRole(Long id);

    void deleteSubSystemRoleList(List<Long> ids);

    void updateSubSystemRoleStatus(Long id, Integer status);

    /**
     * 修改角色接口注册状态（0未注册 1已注册；人工在对方系统建过可标已注册，改回未注册可重推）
     */
    void updateSubSystemRoleRegisterStatus(Long id, String roleRegistered);

    /**
     * 未注册角色补调对方「角色新增」接口；成功后置已注册。
     *
     * @return 外部角色已创建但未查回 roleId 时的提示；成功关联时返回 null
     */
    String registerSubSystemRole(Long id, SubSystemRoleRegisterReqVO reqVO);

    /**
     * 列出外部系统已有角色（角色名 + roleId），供「关联外部角色」选择。
     * 传 roleName 时再按角色名全系统补查，避免车间列表截断漏掉新建角色。
     */
    List<SubSystemExternalRoleRespVO> getExternalRoleList(Long apiSubSystemId, String workshopCode, String roleName);

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
