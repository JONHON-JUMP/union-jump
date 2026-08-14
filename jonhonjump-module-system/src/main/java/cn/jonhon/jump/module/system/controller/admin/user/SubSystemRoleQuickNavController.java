package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.quicknav.RoleQuickNavRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemRoleQuickNavSaveReqVO;
import cn.jonhon.jump.module.system.service.user.SubSystemRoleQuickNavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 外部子系统角色快捷导航")
@RestController
@RequestMapping("/system/sub-system-role/quick-nav")
@Validated
public class SubSystemRoleQuickNavController {

    @Resource
    private SubSystemRoleQuickNavService subSystemRoleQuickNavService;

    @GetMapping("/list")
    @Operation(summary = "获得外部子系统角色的默认快捷导航配置")
    @Parameter(name = "roleId", description = "角色编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:role:update')")
    public CommonResult<RoleQuickNavRespVO> getRoleQuickNavList(@RequestParam("roleId") Long roleId) {
        return success(subSystemRoleQuickNavService.getRoleQuickNav(roleId));
    }

    @PutMapping("/save")
    @Operation(summary = "保存外部子系统角色的默认快捷导航配置")
    @PreAuthorize("@ss.hasPermission('sub-system:role:update')")
    public CommonResult<Boolean> saveRoleQuickNav(@Valid @RequestBody SubSystemRoleQuickNavSaveReqVO reqVO) {
        subSystemRoleQuickNavService.saveRoleQuickNav(reqVO.getSubSystemId(), reqVO.getRoleId(), reqVO.getMenuIds());
        return success(true);
    }

}
