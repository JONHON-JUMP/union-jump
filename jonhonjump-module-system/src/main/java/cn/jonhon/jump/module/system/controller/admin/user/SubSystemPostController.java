package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.excel.core.util.ExcelUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.service.user.SubSystemMetaImportService;
import cn.jonhon.jump.module.system.service.user.SubSystemPostService;
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

@Tag(name = "管理后台 - 外部系统岗位")
@RestController
@RequestMapping("/system/sub-system-post")
@Validated
public class SubSystemPostController {

    @Resource
    private SubSystemPostService subSystemPostService;
    @Resource
    private SubSystemMetaImportService subSystemMetaImportService;

    @GetMapping("/page")
    @Operation(summary = "获得外部系统岗位分页")
    @PreAuthorize("@ss.hasPermission('sub-system:post:list')")
    public CommonResult<PageResult<SubSystemPostRespVO>> getSubSystemPostPage(@Valid SubSystemPostPageReqVO pageReqVO) {
        return success(subSystemPostService.getSubSystemPostPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得外部系统岗位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:post:list')")
    public CommonResult<SubSystemPostRespVO> getSubSystemPost(@RequestParam("id") Long id) {
        return success(subSystemPostService.getSubSystemPost(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建外部系统岗位")
    @PreAuthorize("@ss.hasPermission('sub-system:post:create')")
    public CommonResult<Long> createSubSystemPost(@Valid @RequestBody SubSystemPostSaveReqVO createReqVO) {
        return success(subSystemPostService.createSubSystemPost(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新外部系统岗位")
    @PreAuthorize("@ss.hasPermission('sub-system:post:update')")
    public CommonResult<Boolean> updateSubSystemPost(@Valid @RequestBody SubSystemPostSaveReqVO updateReqVO) {
        subSystemPostService.updateSubSystemPost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外部系统岗位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:post:delete')")
    public CommonResult<Boolean> deleteSubSystemPost(@RequestParam("id") Long id) {
        subSystemPostService.deleteSubSystemPost(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除外部系统岗位")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:post:delete')")
    public CommonResult<Boolean> deleteSubSystemPostList(@RequestParam("ids") List<Long> ids) {
        subSystemPostService.deleteSubSystemPostList(ids);
        return success(true);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "下载外部系统岗位导入模板")
    @PreAuthorize("@ss.hasPermission('sub-system:post:create')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<SubSystemPostImportExcelVO> list = Arrays.asList(
                SubSystemPostImportExcelVO.builder().name("普通岗位").code("common").sort(1).status(0).build()
        );
        ExcelUtils.write(response, "外部系统岗位导入模板.xls", "岗位", SubSystemPostImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入外部系统岗位（须先选择已登记外部系统）")
    @Parameters({
            @Parameter(name = "subSystemId", description = "外部系统编号", required = true),
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否更新已存在", example = "false")
    })
    @PreAuthorize("@ss.hasPermission('sub-system:post:create')")
    public CommonResult<SubSystemUserImportRespVO> importExcel(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport
    ) throws Exception {
        List<SubSystemPostImportExcelVO> list = ExcelUtils.read(file, SubSystemPostImportExcelVO.class);
        return success(subSystemMetaImportService.importPostList(subSystemId, list, Boolean.TRUE.equals(updateSupport)));
    }

}
