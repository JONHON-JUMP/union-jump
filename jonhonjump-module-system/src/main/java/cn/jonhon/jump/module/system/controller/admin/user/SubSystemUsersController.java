package cn.jonhon.jump.module.system.controller.admin.user;



import cn.jonhon.jump.framework.common.pojo.CommonResult;

import cn.jonhon.jump.framework.common.pojo.PageResult;

import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;

import cn.jonhon.jump.module.system.service.user.SubSystemUsersService;

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

import static cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;



@Tag(name = "管理后台 - 人员子系统关系")

@RestController

@RequestMapping("/system/sub-system-users")

@Validated

public class SubSystemUsersController {



    @Resource

    private SubSystemUsersService subSystemUsersService;



    @GetMapping("/client-simple-list")

    @Operation(summary = "获得外部系统精简列表")

    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")

    public CommonResult<List<SubSystemClientSimpleRespVO>> getClientSimpleList() {

        return success(subSystemUsersService.getClientSimpleList());

    }



    @GetMapping("/page")

    @Operation(summary = "获得外部系统用户分页")

    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")

    public CommonResult<PageResult<SubSystemUsersRespVO>> getSubSystemUserPage(@Valid SubSystemUsersPageReqVO pageReqVO) {

        return success(subSystemUsersService.getSubSystemUserPage(pageReqVO));

    }



    @GetMapping("/get")

    @Operation(summary = "获得外部系统用户")

    @Parameter(name = "id", description = "编号", required = true)

    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")

    public CommonResult<SubSystemUsersRespVO> getSubSystemUser(@RequestParam("id") Long id) {

        return success(subSystemUsersService.getSubSystemUser(id));

    }



    @PostMapping("/create")

    @Operation(summary = "创建外部系统用户")

    @PreAuthorize("@ss.hasPermission('sub-system:user:create')")

    public CommonResult<Long> createSubSystemUser(@Valid @RequestBody SubSystemUsersSaveReqVO createReqVO) {

        return success(subSystemUsersService.createSubSystemUser(createReqVO));

    }



    @PutMapping("/update")

    @Operation(summary = "更新外部系统用户")

    @PreAuthorize("@ss.hasPermission('sub-system:user:update')")

    public CommonResult<Boolean> updateSubSystemUser(@Valid @RequestBody SubSystemUsersSaveReqVO updateReqVO) {

        subSystemUsersService.updateSubSystemUser(updateReqVO);

        return success(true);

    }



    @DeleteMapping("/delete")

    @Operation(summary = "删除外部系统用户")

    @Parameter(name = "id", description = "编号", required = true)

    @PreAuthorize("@ss.hasPermission('sub-system:user:delete')")

    public CommonResult<Boolean> deleteSubSystemUser(@RequestParam("id") Long id) {

        subSystemUsersService.deleteSubSystemUser(id);

        return success(true);

    }



    @DeleteMapping("/delete-list")

    @Operation(summary = "批量删除外部系统用户")

    @Parameter(name = "ids", description = "编号列表", required = true)

    @PreAuthorize("@ss.hasPermission('sub-system:user:delete')")

    public CommonResult<Boolean> deleteSubSystemUserList(@RequestParam("ids") List<Long> ids) {

        subSystemUsersService.deleteSubSystemUserList(ids);

        return success(true);

    }



    @PutMapping("/update-status")

    @Operation(summary = "修改外部系统用户状态")

    @PreAuthorize("@ss.hasPermission('sub-system:user:update')")

    public CommonResult<Boolean> updateSubSystemUserStatus(@RequestParam("id") Long id,

                                                           @RequestParam("status") String status) {

        subSystemUsersService.updateSubSystemUserStatus(id, status);

        return success(true);

    }



    @GetMapping("/post-simple-list")
    @Operation(summary = "获得外部系统岗位精简列表")
    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")
    public CommonResult<List<SubSystemPostSimpleRespVO>> getPostSimpleList(@RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemUsersService.getPostSimpleList(subSystemId));
    }

    @GetMapping("/team-simple-list")
    @Operation(summary = "获得外部系统班组精简列表")
    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")
    public CommonResult<List<SubSystemTeamSimpleRespVO>> getTeamSimpleList(@RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemUsersService.getTeamSimpleList(subSystemId));
    }

    @GetMapping("/home-menu-tree-list")
    @Operation(summary = "获得用户角色可见的主页面菜单树")
    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")
    public CommonResult<List<SubSystemMenuTreeRespVO>> getUserHomeMenuTree(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam(value = "roleIds", required = false) List<Long> roleIds) {
        return success(subSystemUsersService.getUserHomeMenuTree(subSystemId, roleIds));
    }

    @GetMapping("/role-simple-list")

    @Operation(summary = "获得外部系统角色精简列表")

    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")

    public CommonResult<List<SubSystemRoleSimpleRespVO>> getRoleSimpleList(@RequestParam("subSystemId") Long subSystemId) {

        return success(subSystemUsersService.getRoleSimpleList(subSystemId));

    }



    @GetMapping("/list-role-ids")

    @Operation(summary = "获得外部系统用户角色编号列表")

    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")

    public CommonResult<List<Long>> getSubSystemUserRoleIds(@RequestParam("id") Long id) {

        return success(subSystemUsersService.getSubSystemUserRoleIds(id));

    }



    @PutMapping("/assign-role")

    @Operation(summary = "分配外部系统用户角色")

    @PreAuthorize("@ss.hasPermission('sub-system:user:update')")

    public CommonResult<Boolean> assignSubSystemUserRole(@Valid @RequestBody SubSystemUsersAssignRoleReqVO reqVO) {

        subSystemUsersService.assignSubSystemUserRole(reqVO);

        return success(true);

    }



    @GetMapping("/list-by-main-user-id")

    @Operation(summary = "获得指定用户的子系统关系列表")

    @Parameter(name = "mainUserId", description = "主数据用户编号", required = true)

    @PreAuthorize("@ss.hasPermission('system:user:query')")

    public CommonResult<List<SubSystemUsersRespVO>> getListByMainUserId(@RequestParam("mainUserId") Long mainUserId) {

        return success(subSystemUsersService.getListByMainUserId(mainUserId));

    }



    @GetMapping("/my-list")

    @Operation(summary = "获得当前用户可访问的外部系统列表")

    public CommonResult<List<UserExternalSystemRespVO>> getMyExternalSystemList() {

        return success(subSystemUsersService.getMyExternalSystemList(getLoginUserId()));

    }

    @GetMapping("/my-menus")
    @Operation(summary = "获得当前用户在指定外部系统下的门户菜单")
    public CommonResult<List<SubSystemPortalMenuRespVO>> getMyPortalMenus(
            @RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemUsersService.getMyPortalMenus(getLoginUserId(), subSystemId));
    }

}
