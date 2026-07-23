package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.excel.core.util.ExcelUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.service.user.SubSystemMetaImportService;
import cn.jonhon.jump.module.system.service.user.SubSystemTeamService;
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

@Tag(name = "管理后台 - 外部系统班组")
@RestController
@RequestMapping("/system/sub-system-team")
@Validated
public class SubSystemTeamController {

    @Resource
    private SubSystemTeamService subSystemTeamService;
    @Resource
    private SubSystemMetaImportService subSystemMetaImportService;

    @GetMapping("/page")
    @Operation(summary = "获得外部系统班组分页")
    @PreAuthorize("@ss.hasPermission('sub-system:team:list')")
    public CommonResult<PageResult<SubSystemTeamRespVO>> getSubSystemTeamPage(@Valid SubSystemTeamPageReqVO pageReqVO) {
        return success(subSystemTeamService.getSubSystemTeamPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得外部系统班组")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:team:list')")
    public CommonResult<SubSystemTeamRespVO> getSubSystemTeam(@RequestParam("id") Long id) {
        return success(subSystemTeamService.getSubSystemTeam(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建外部系统班组")
    @PreAuthorize("@ss.hasPermission('sub-system:team:create')")
    public CommonResult<Long> createSubSystemTeam(@Valid @RequestBody SubSystemTeamSaveReqVO createReqVO) {
        return success(subSystemTeamService.createSubSystemTeam(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新外部系统班组")
    @PreAuthorize("@ss.hasPermission('sub-system:team:update')")
    public CommonResult<Boolean> updateSubSystemTeam(@Valid @RequestBody SubSystemTeamSaveReqVO updateReqVO) {
        subSystemTeamService.updateSubSystemTeam(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外部系统班组")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:team:delete')")
    public CommonResult<Boolean> deleteSubSystemTeam(@RequestParam("id") Long id) {
        subSystemTeamService.deleteSubSystemTeam(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除外部系统班组")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:team:delete')")
    public CommonResult<Boolean> deleteSubSystemTeamList(@RequestParam("ids") List<Long> ids) {
        subSystemTeamService.deleteSubSystemTeamList(ids);
        return success(true);
    }

    @GetMapping("/user-simple-list")
    @Operation(summary = "获得外部系统用户精简列表（用于选择班组长）")
    @PreAuthorize("@ss.hasPermission('sub-system:team:list')")
    public CommonResult<List<SubSystemUsersSimpleRespVO>> getUserSimpleList(@RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemTeamService.getUserSimpleList(subSystemId));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "下载外部系统班组导入模板")
    @PreAuthorize("@ss.hasPermission('sub-system:team:create')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<SubSystemTeamImportExcelVO> list = Arrays.asList(
                SubSystemTeamImportExcelVO.builder()
                        .teamCode("T001")
                        .teamName("一班")
                        .description("示例班组")
                        .leaderUserUid("")
                        .leaderUsername("")
                        .build()
        );
        ExcelUtils.write(response, "外部系统班组导入模板.xls", "班组", SubSystemTeamImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入外部系统班组（须先选择已登记外部系统）")
    @Parameters({
            @Parameter(name = "subSystemId", description = "外部系统编号", required = true),
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否更新已存在", example = "false")
    })
    @PreAuthorize("@ss.hasPermission('sub-system:team:create')")
    public CommonResult<SubSystemUserImportRespVO> importExcel(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport
    ) throws Exception {
        List<SubSystemTeamImportExcelVO> list = ExcelUtils.read(file, SubSystemTeamImportExcelVO.class);
        return success(subSystemMetaImportService.importTeamList(subSystemId, list, Boolean.TRUE.equals(updateSupport)));
    }

}
