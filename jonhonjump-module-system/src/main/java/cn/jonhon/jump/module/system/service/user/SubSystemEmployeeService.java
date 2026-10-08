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
     * 花名册人员手动调「新增人员」接口注册：逐项调所选接口目标的新增接口，
     * 成功后把花名册行的人员接口注册状态置为已注册；已注册项跳过；单项失败不影响其余
     */
    List<SubSystemUserRegisterRespVO> registerEmployees(SubSystemUserRegisterReqVO reqVO);

}
