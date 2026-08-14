package cn.jonhon.jump.module.system.controller.admin.permission;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.quicknav.RoleQuickNavRespVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.quicknav.RoleQuickNavSaveReqVO;
import cn.jonhon.jump.module.system.service.permission.RoleQuickNavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 角色快捷导航")
@RestController
@RequestMapping("/system/role/quick-nav")
@Validated
public class RoleQuickNavController {

    @Resource
    private RoleQuickNavService roleQuickNavService;

    @GetMapping("/list")
    @Operation(summary = "获得角色的默认快捷导航配置")
    @Parameter(name = "roleId", description = "角色编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<RoleQuickNavRespVO> getRoleQuickNavList(@RequestParam("roleId") Long roleId) {
        return success(roleQuickNavService.getRoleQuickNav(roleId));
    }

    @PutMapping("/save")
    @Operation(summary = "保存角色的默认快捷导航配置")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<Boolean> saveRoleQuickNav(@Valid @RequestBody RoleQuickNavSaveReqVO reqVO) {
        roleQuickNavService.saveRoleQuickNav(reqVO.getRoleId(), reqVO.getMenuIds());
        return success(true);
    }

}
