package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavSaveReqVO;
import cn.jonhon.jump.module.system.service.user.SubSystemUserQuickNavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 用户外部子系统快捷导航")
@RestController
@RequestMapping("/system/sub-system-user/quick-nav")
@Validated
public class SubSystemUserQuickNavController {

    @Resource
    private SubSystemUserQuickNavService subSystemUserQuickNavService;

    @GetMapping("/list")
    @Operation(summary = "获得当前用户在指定外部子系统的快捷导航配置")
    @Parameter(name = "subSystemId", description = "外部子系统编号", required = true)
    public CommonResult<SubSystemUserQuickNavRespVO> getUserQuickNavList(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam(value = "workshopCode", required = false) String workshopCode) {
        return success(subSystemUserQuickNavService.getUserQuickNav(getLoginUserId(), subSystemId, workshopCode));
    }

    @PutMapping("/save")
    @Operation(summary = "保存当前用户在指定外部子系统的快捷导航配置")
    public CommonResult<SubSystemUserQuickNavRespVO> saveUserQuickNav(@Valid @RequestBody SubSystemUserQuickNavSaveReqVO reqVO) {
        return success(subSystemUserQuickNavService.saveUserQuickNav(
                getLoginUserId(), reqVO.getSubSystemId(), reqVO.getMenuIds(), reqVO.getWorkshopCode()));
    }

    @PutMapping("/add-menu")
    @Operation(summary = "加入单个菜单到快捷导航（增量，只动这一项）")
    @Parameter(name = "subSystemId", description = "外部子系统编号", required = true)
    @Parameter(name = "menuId", description = "菜单编号", required = true)
    public CommonResult<SubSystemUserQuickNavRespVO> addUserQuickNavMenu(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam("menuId") Long menuId,
            @RequestParam(value = "workshopCode", required = false) String workshopCode) {
        return success(subSystemUserQuickNavService.addUserQuickNavMenu(
                getLoginUserId(), subSystemId, workshopCode, menuId));
    }

    @PutMapping("/remove-menu")
    @Operation(summary = "从快捷导航移除单个菜单（增量，只动这一项；角色锁定默认项不移除）")
    @Parameter(name = "subSystemId", description = "外部子系统编号", required = true)
    @Parameter(name = "menuId", description = "菜单编号", required = true)
    public CommonResult<SubSystemUserQuickNavRespVO> removeUserQuickNavMenu(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam("menuId") Long menuId,
            @RequestParam(value = "workshopCode", required = false) String workshopCode) {
        return success(subSystemUserQuickNavService.removeUserQuickNavMenu(
                getLoginUserId(), subSystemId, workshopCode, menuId));
    }

    @PostMapping("/sync-from-role")
    @Operation(summary = "按当前用户在指定子系统下的角色默认快捷导航，同步到数据库")
    @Parameter(name = "subSystemId", description = "外部子系统编号", required = true)
    public CommonResult<Boolean> syncUserQuickNavFromRole(@RequestParam("subSystemId") Long subSystemId) {
        subSystemUserQuickNavService.syncUserQuickNavFromRoles(getLoginUserId(), subSystemId);
        return success(true);
    }

}
