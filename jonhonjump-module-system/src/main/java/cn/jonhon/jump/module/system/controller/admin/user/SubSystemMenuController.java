package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.excel.core.util.ExcelUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.service.user.SubSystemMenuService;
import cn.jonhon.jump.module.system.service.user.SubSystemMetaImportService;
import cn.jonhon.jump.module.system.service.user.SubSystemPermissionContextService;
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
import java.util.Comparator;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 外部系统菜单")
@RestController
@RequestMapping("/system/sub-system-menu")
@Validated
public class SubSystemMenuController {

    @Resource
    private SubSystemMenuService subSystemMenuService;
    @Resource
    private SubSystemMetaImportService subSystemMetaImportService;
    @Resource
    private SubSystemPermissionContextService subSystemPermissionContextService;

    @GetMapping("/list")
    @Operation(summary = "获得外部系统菜单列表")
    @PreAuthorize("@ss.hasPermission('sub-system:menu:list')")
    public CommonResult<List<SubSystemMenuRespVO>> getSubSystemMenuList(SubSystemMenuListReqVO reqVO) {
        List<SubSystemMenuRespVO> list = subSystemMenuService.getSubSystemMenuList(reqVO);
        list.sort(Comparator.comparing(SubSystemMenuRespVO::getSort));
        return success(list);
    }

    @GetMapping("/get")
    @Operation(summary = "获得外部系统菜单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:menu:list')")
    public CommonResult<SubSystemMenuRespVO> getSubSystemMenu(@RequestParam("id") Long id) {
        return success(subSystemMenuService.getSubSystemMenu(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建外部系统菜单")
    @PreAuthorize("@ss.hasPermission('sub-system:menu:create')")
    public CommonResult<Long> createSubSystemMenu(@Valid @RequestBody SubSystemMenuSaveReqVO createReqVO) {
        return success(subSystemMenuService.createSubSystemMenu(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新外部系统菜单")
    @PreAuthorize("@ss.hasPermission('sub-system:menu:update')")
    public CommonResult<Boolean> updateSubSystemMenu(@Valid @RequestBody SubSystemMenuSaveReqVO updateReqVO) {
        subSystemMenuService.updateSubSystemMenu(updateReqVO);
        return success(true);
    }

    @PostMapping("/clear-portal-cache")
    @Operation(summary = "清门户菜单 Redis 缓存（改菜单后仍见旧路由时手动调用）")
    @Parameter(name = "subSystemId", description = "外部系统编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:menu:update')")
    public CommonResult<Boolean> clearPortalMenuCache(@RequestParam("subSystemId") Long subSystemId) {
        subSystemPermissionContextService.evictBySubSystemId(subSystemId);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外部系统菜单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:menu:delete')")
    public CommonResult<Boolean> deleteSubSystemMenu(@RequestParam("id") Long id) {
        subSystemMenuService.deleteSubSystemMenu(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除外部系统菜单")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:menu:delete')")
    public CommonResult<Boolean> deleteSubSystemMenuList(@RequestParam("ids") List<Long> ids) {
        subSystemMenuService.deleteSubSystemMenuList(ids);
        return success(true);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "下载外部系统菜单导入模板")
    @PreAuthorize("@ss.hasPermission('sub-system:menu:create')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<SubSystemMenuImportExcelVO> list = Arrays.asList(
                SubSystemMenuImportExcelVO.builder()
                        .parentName("根")
                        .name("系统管理")
                        .type(1)
                        .sort(1)
                        .path("/system")
                        .status(0)
                        .visible(true)
                        .build()
        );
        ExcelUtils.write(response, "外部系统菜单导入模板.xls", "菜单", SubSystemMenuImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入外部系统菜单（须先选择已登记外部系统）")
    @Parameters({
            @Parameter(name = "subSystemId", description = "外部系统编号", required = true),
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否更新已存在", example = "false")
    })
    @PreAuthorize("@ss.hasPermission('sub-system:menu:create')")
    public CommonResult<SubSystemUserImportRespVO> importExcel(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport
    ) throws Exception {
        List<SubSystemMenuImportExcelVO> list = ExcelUtils.read(file, SubSystemMenuImportExcelVO.class);
        return success(subSystemMetaImportService.importMenuList(subSystemId, list, Boolean.TRUE.equals(updateSupport)));
    }

}
