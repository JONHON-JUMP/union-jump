package cn.jonhon.jump.module.system.controller.admin.user;



import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.excel.core.util.ExcelUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.service.user.SubSystemUserImportService;
import cn.jonhon.jump.module.system.service.user.SubSystemUsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
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

    @Resource
    private SubSystemUserImportService subSystemUserImportService;

    @Resource
    private cn.jonhon.jump.module.system.service.user.SubSystemPermissionContextService subSystemPermissionContextService;



    @GetMapping("/client-simple-list")
    @Operation(summary = "获得外部系统精简列表（portalOnly=true 仅 JUMP 门户业务系统）")
    @PreAuthorize("@ss.hasAnyPermissions('sub-system:user:list', 'sub-system:apiconfig:list', 'system:user:query', 'system:user:create')")
    public CommonResult<List<SubSystemClientSimpleRespVO>> getClientSimpleList(
            @RequestParam(value = "portalOnly", required = false) Boolean portalOnly) {
        return success(subSystemUsersService.getClientSimpleList(portalOnly));
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



    @GetMapping("/get-by-username")

    @Operation(summary = "按用户名获得外部系统用户")

    @Parameters({

            @Parameter(name = "subSystemId", description = "外部系统编号", required = true),

            @Parameter(name = "username", description = "子系统用户名", required = true)

    })

    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")

    public CommonResult<SubSystemUsersRespVO> getByUsername(@RequestParam("subSystemId") Long subSystemId,

                                                            @RequestParam("username") String username) {

        return success(subSystemUsersService.getBySubSystemIdAndUsername(subSystemId, username));

    }



    @PostMapping("/bind-main-user")

    @Operation(summary = "挂接主系统用户到外部系统同名用户")

    @Parameters({

            @Parameter(name = "subSystemId", description = "外部系统编号", required = true),

            @Parameter(name = "mainUserId", description = "主系统用户编号", required = true)

    })

    @PreAuthorize("@ss.hasPermission('sub-system:user:create')")

    public CommonResult<Long> bindMainUser(@RequestParam("subSystemId") Long subSystemId,

                                           @RequestParam("mainUserId") Long mainUserId) {

        return success(subSystemUsersService.bindMainUser(subSystemId, mainUserId));

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



    @PutMapping("/update-register-status")

    @Operation(summary = "修改人员接口注册状态（0未注册 1已注册；人工在对方系统建过人可标已注册，改回未注册可重推）")

    @PreAuthorize("@ss.hasPermission('sub-system:user:update')")

    public CommonResult<Boolean> updateSubSystemUserRegisterStatus(@RequestParam("id") Long id,

                                                                   @RequestParam("employeeRegistered") String employeeRegistered) {

        subSystemUsersService.updateSubSystemUserRegisterStatus(id, employeeRegistered);

        return success(true);

    }



    @GetMapping("/post-simple-list")
    @Operation(summary = "获得外部系统岗位精简列表")
    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")
    public CommonResult<List<SubSystemPostSimpleRespVO>> getPostSimpleList(@RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemUsersService.getPostSimpleList(subSystemId));
    }

    @GetMapping("/team-simple-list")
    @Operation(summary = "获得业务系统班组精简列表（可按部门过滤）")
    @PreAuthorize("@ss.hasPermission('sub-system:user:list')")
    public CommonResult<List<SubSystemTeamSimpleRespVO>> getTeamSimpleList(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam(value = "deptId", required = false) Long deptId) {
        return success(subSystemUsersService.getTeamSimpleList(subSystemId, deptId));
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

    @GetMapping("/my-menus-version")
    @Operation(summary = "获得指定外部系统 RBAC 版本（菜单/角色变更递增，门户轻量比对）")
    public CommonResult<Long> getMyPortalMenusVersion(@RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemPermissionContextService.getRbacVersion(subSystemId));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "下载子系统用户导入模板")
    @PreAuthorize("@ss.hasPermission('sub-system:user:create')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<SubSystemUserImportExcelVO> list = Arrays.asList(
                SubSystemUserImportExcelVO.builder()
                        .username("zhangsan")
                        .nickname("张三")
                        .workshopId("WS01")
                        .teamId("T01")
                        .roleCodes("common")
                        .status("0")
                        .remark("示例：按用户名导入子系统花名册，可不关联主系统用户")
                        .build()
        );
        ExcelUtils.write(response, "子系统用户导入模板.xls", "用户花名册", SubSystemUserImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入子系统用户（须先选择已登记外部系统）")
    @Parameters({
            @Parameter(name = "subSystemId", description = "已登记外部系统编号", required = true),
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否更新已绑定用户", example = "false")
    })
    @PreAuthorize("@ss.hasPermission('sub-system:user:create')")
    public CommonResult<SubSystemUserImportRespVO> importExcel(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport
    ) throws Exception {
        List<SubSystemUserImportExcelVO> list = ExcelUtils.read(file, SubSystemUserImportExcelVO.class);
        return success(subSystemUserImportService.importUserList(subSystemId, list, Boolean.TRUE.equals(updateSupport)));
    }

}
