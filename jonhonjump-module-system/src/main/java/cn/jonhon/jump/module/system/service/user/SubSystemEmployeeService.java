package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeePageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemRegisterableApiRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserRegisterReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserRegisterRespVO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemTeamComboDTO;

import java.util.List;

/**
 * 子系统人员 Service（经适配器分发到各外部系统）
 */
public interface SubSystemEmployeeService {

    PageResult<SubSystemEmployeeRespVO> getEmployeePage(SubSystemEmployeePageReqVO pageReqVO);

    void createEmployee(SubSystemEmployeeSaveReqVO createReqVO);

    void updateEmployee(SubSystemEmployeeSaveReqVO updateReqVO);

    void deleteEmployee(Long subSystemId, String userCode);

    List<SubSystemTeamComboDTO> getTeamCombo(Long subSystemId, String workshopCode);

    /**
     * 该系统的删除二次确认提示语（前端删除弹窗用）
     */
    String getDeleteTip(Long subSystemId);

    /**
     * 可选「新增人员」接口目标列表（接口管理中 create 用途已启用的系统；与花名册系统解耦）
     */
    List<SubSystemRegisterableApiRespVO> getRegisterableApis();

    /**
     * 按车间查询外部系统已有人员（关联外部用，最多 200 条）
     */
    List<SubSystemEmployeeRespVO> listExternalEmployees(Long apiSubSystemId, String workshopCode, String userCode);

    /**
     * 花名册人员手动调「新增人员」接口注册：逐项调所选接口目标的新增接口，
     * 成功后把花名册行的人员接口注册状态置为已注册；已注册项跳过；单项失败不影响其余
     */
    List<SubSystemUserRegisterRespVO> registerEmployees(SubSystemUserRegisterReqVO reqVO);

    /**
     * 把 JUMP 分配的业务角色同步到外部系统（查-合并-回写）：
     * 角色按 external_role_id 主键映射上挂/解除，不依赖角色名匹配；
     * 外部系统人工挂的角色不在 JUMP 管辖范围，不会被增删。
     *
     * @param apiSubSystemId   接口目标系统 ID
     * @param rosterSubSystemId 花名册所属系统 ID（角色与管辖范围按它取）
     * @param userCode         对方系统用户名（拼接车间的为 车间编号_工号）
     * @param jumpRoleIds      本次分配生效的 JUMP 角色编号集合（须已关联外部角色）
     */
    void syncUserRoles(Long apiSubSystemId, Long rosterSubSystemId, String userCode, List<Long> jumpRoleIds);

}
