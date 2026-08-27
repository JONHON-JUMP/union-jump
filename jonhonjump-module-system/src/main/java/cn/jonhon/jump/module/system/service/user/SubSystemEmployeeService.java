package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeCreateFromUserReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeePageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEnabledSystemVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSimpleRespVO;
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
     * 从主系统用户同步到多个业务系统「新增人员」（用户管理页联动）：
     * 对每个选中系统：调对方 create 接口；仅已绑定 OAuth 的 JUMP 门户业务系统再写入 sub_system_users。
     */
    void createFromMainUser(SubSystemEmployeeCreateFromUserReqVO reqVO);

    /**
     * 已配置且启用人员接口的外部系统列表（用户创建联动下拉用）
     */
    List<SubSystemEnabledSystemVO> getEnabledSystems();

    /**
     * 联动下拉：某系统下按 JUMP 部门过滤的车间列表（未映射返回空）
     */
    List<SubSystemWorkshopSimpleRespVO> getWorkshopOptions(Long subSystemId, Long deptId);

}
