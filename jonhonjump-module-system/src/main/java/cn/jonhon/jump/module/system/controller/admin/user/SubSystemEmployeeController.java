package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeePageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemRegisterableApiRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserRegisterReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserRegisterRespVO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemTeamComboDTO;
import cn.jonhon.jump.module.system.service.user.SubSystemEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 子系统人员")
@RestController
@RequestMapping("/system/sub-system-employee")
@Validated
public class SubSystemEmployeeController {

    @Resource
    private SubSystemEmployeeService subSystemEmployeeService;

    @GetMapping("/page")
    @Operation(summary = "获得子系统人员分页（经适配器调用目标系统）")
    @PreAuthorize("@ss.hasPermission('sub-system:employee:list')")
    public CommonResult<PageResult<SubSystemEmployeeRespVO>> getEmployeePage(@Valid SubSystemEmployeePageReqVO pageReqVO) {
        return success(subSystemEmployeeService.getEmployeePage(pageReqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建子系统人员")
    @PreAuthorize("@ss.hasPermission('sub-system:employee:create')")
    public CommonResult<Boolean> createEmployee(@Valid @RequestBody SubSystemEmployeeSaveReqVO createReqVO) {
        subSystemEmployeeService.createEmployee(createReqVO);
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "更新子系统人员")
    @PreAuthorize("@ss.hasPermission('sub-system:employee:update')")
    public CommonResult<Boolean> updateEmployee(@Valid @RequestBody SubSystemEmployeeSaveReqVO updateReqVO) {
        subSystemEmployeeService.updateEmployee(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除子系统人员（注意目标系统可能的连带副作用）")
    @Parameter(name = "subSystemId", description = "外部系统编号", required = true)
    @Parameter(name = "userCode", description = "工号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:employee:delete')")
    public CommonResult<Boolean> deleteEmployee(@RequestParam("subSystemId") Long subSystemId,
                                                @RequestParam("userCode") String userCode) {
        subSystemEmployeeService.deleteEmployee(subSystemId, userCode);
        return success(true);
    }

    @GetMapping("/team-combo")
    @Operation(summary = "获得目标系统班组下拉（按车间）")
    @Parameter(name = "subSystemId", description = "外部系统编号", required = true)
    @Parameter(name = "workshopCode", description = "车间编码", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:employee:list')")
    public CommonResult<List<SubSystemTeamComboDTO>> getTeamCombo(@RequestParam("subSystemId") Long subSystemId,
                                                                  @RequestParam("workshopCode") String workshopCode) {
        return success(subSystemEmployeeService.getTeamCombo(subSystemId, workshopCode));
    }

    @GetMapping("/delete-tip")
    @Operation(summary = "获得删除二次确认提示语")
    @Parameter(name = "subSystemId", description = "外部系统编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:employee:delete')")
    public CommonResult<String> getDeleteTip(@RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemEmployeeService.getDeleteTip(subSystemId));
    }

    @GetMapping("/registerable-apis")
    @Operation(summary = "可选「新增人员」接口目标列表（接口管理中 create 已启用；与花名册系统解耦）")
    @PreAuthorize("@ss.hasAnyPermissions('sub-system:user:list', 'sub-system:employee:list')")
    public CommonResult<List<SubSystemRegisterableApiRespVO>> getRegisterableApis() {
        return success(subSystemEmployeeService.getRegisterableApis());
    }

    @PostMapping("/register-employee")
    @Operation(summary = "花名册人员手动调「新增人员」接口注册（成功自动置已注册；逐项返回结果）")
    @PreAuthorize("@ss.hasAnyPermissions('sub-system:employee:create', 'sub-system:user:update')")
    public CommonResult<List<SubSystemUserRegisterRespVO>> registerEmployee(@Valid @RequestBody SubSystemUserRegisterReqVO reqVO) {
        return success(subSystemEmployeeService.registerEmployees(reqVO));
    }

}
