package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.excel.core.util.ExcelUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.service.user.SubSystemMetaImportService;
import cn.jonhon.jump.module.system.service.user.SubSystemRoleService;
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
import java.util.Set;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 外部系统角色")
@RestController
@RequestMapping("/system/sub-system-role")
@Validated
public class SubSystemRoleController {

    @Resource
    private SubSystemRoleService subSystemRoleService;
    @Resource
    private SubSystemMetaImportService subSystemMetaImportService;

    @GetMapping("/page")
    @Operation(summary = "获得外部系统角色分页")
    @PreAuthorize("@ss.hasPermission('sub-system:role:list')")
    public CommonResult<PageResult<SubSystemRoleRespVO>> getSubSystemRolePage(@Valid SubSystemRolePageReqVO pageReqVO) {
        return success(subSystemRoleService.getSubSystemRolePage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得外部系统角色")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:role:list')")
    public CommonResult<SubSystemRoleRespVO> getSubSystemRole(@RequestParam("id") Long id) {
        return success(subSystemRoleService.getSubSystemRole(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建外部系统角色")
    @PreAuthorize("@ss.hasPermission('sub-system:role:create')")
    public CommonResult<Long> createSubSystemRole(@Valid @RequestBody SubSystemRoleSaveReqVO createReqVO) {
        return success(subSystemRoleService.createSubSystemRole(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新外部系统角色")
    @PreAuthorize("@ss.hasPermission('sub-system:role:update')")
    public CommonResult<Boolean> updateSubSystemRole(@Valid @RequestBody SubSystemRoleSaveReqVO updateReqVO) {
        subSystemRoleService.updateSubSystemRole(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外部系统角色")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:role:delete')")
    public CommonResult<Boolean> deleteSubSystemRole(@RequestParam("id") Long id) {
        subSystemRoleService.deleteSubSystemRole(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除外部系统角色")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:role:delete')")
    public CommonResult<Boolean> deleteSubSystemRoleList(@RequestParam("ids") List<Long> ids) {
        subSystemRoleService.deleteSubSystemRoleList(ids);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改外部系统角色状态")
    @PreAuthorize("@ss.hasPermission('sub-system:role:update')")
    public CommonResult<Boolean> updateSubSystemRoleStatus(@RequestParam("id") Long id,
                                                           @RequestParam("status") Integer status) {
        subSystemRoleService.updateSubSystemRoleStatus(id, status);
        return success(true);
    }

    @GetMapping("/menu-simple-list")
    @Operation(summary = "获得外部系统菜单精简列表")
    @PreAuthorize("@ss.hasPermission('sub-system:role:list')")
    public CommonResult<List<SubSystemMenuSimpleRespVO>> getMenuSimpleList(@RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemRoleService.getMenuSimpleList(subSystemId));
    }

    @GetMapping("/list-role-menu-ids")
    @Operation(summary = "获得外部系统角色菜单编号列表")
    @PreAuthorize("@ss.hasPermission('sub-system:role:list')")
    public CommonResult<Set<Long>> getRoleMenuIds(@RequestParam("roleId") Long roleId) {
        return success(subSystemRoleService.getRoleMenuIds(roleId));
    }

    @PutMapping("/assign-role-menu")
    @Operation(summary = "分配外部系统角色菜单")
    @PreAuthorize("@ss.hasPermission('sub-system:role:update')")
    public CommonResult<Boolean> assignRoleMenu(@Valid @RequestBody SubSystemRoleAssignMenuReqVO reqVO) {
        subSystemRoleService.assignRoleMenu(reqVO);
        return success(true);
    }

    @PutMapping("/assign-role-data-scope")
    @Operation(summary = "分配外部系统角色数据权限")
    @PreAuthorize("@ss.hasPermission('sub-system:role:update')")
    public CommonResult<Boolean> assignRoleDataScope(@Valid @RequestBody SubSystemRoleAssignDataScopeReqVO reqVO) {
        subSystemRoleService.assignRoleDataScope(reqVO);
        return success(true);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "下载外部系统角色导入模板")
    @PreAuthorize("@ss.hasPermission('sub-system:role:create')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<SubSystemRoleImportExcelVO> list = Arrays.asList(
                SubSystemRoleImportExcelVO.builder().name("普通角色").code("common").sort(1).status(0).build()
        );
        ExcelUtils.write(response, "外部系统角色导入模板.xls", "角色", SubSystemRoleImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入外部系统角色（须先选择已登记外部系统）")
    @Parameters({
            @Parameter(name = "subSystemId", description = "外部系统编号", required = true),
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否更新已存在", example = "false")
    })
    @PreAuthorize("@ss.hasPermission('sub-system:role:create')")
    public CommonResult<SubSystemUserImportRespVO> importExcel(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport
    ) throws Exception {
        List<SubSystemRoleImportExcelVO> list = ExcelUtils.read(file, SubSystemRoleImportExcelVO.class);
        return success(subSystemMetaImportService.importRoleList(subSystemId, list, Boolean.TRUE.equals(updateSupport)));
    }

}
