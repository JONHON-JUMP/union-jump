package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemOAuth2ClientSimpleRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemSaveReqVO;
import cn.jonhon.jump.module.system.service.user.SubSystemService;
import cn.jonhon.jump.framework.common.pojo.PageResult;
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

@Tag(name = "管理后台 - 外部系统")
@RestController
@RequestMapping("/system/sub-system")
@Validated
public class SubSystemController {

    @Resource
    private SubSystemService subSystemService;

    @GetMapping("/page")
    @Operation(summary = "获得外部系统分页")
    @PreAuthorize("@ss.hasPermission('sub-system:system:list')")
    public CommonResult<PageResult<SubSystemRespVO>> getSubSystemPage(@Valid SubSystemPageReqVO pageReqVO) {
        return success(subSystemService.getSubSystemPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得外部系统")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:system:list')")
    public CommonResult<SubSystemRespVO> getSubSystem(@RequestParam("id") Long id) {
        return success(subSystemService.getSubSystem(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建外部系统")
    @PreAuthorize("@ss.hasPermission('sub-system:system:create')")
    public CommonResult<Long> createSubSystem(@Valid @RequestBody SubSystemSaveReqVO createReqVO) {
        return success(subSystemService.createSubSystem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新外部系统")
    @PreAuthorize("@ss.hasPermission('sub-system:system:update')")
    public CommonResult<Boolean> updateSubSystem(@Valid @RequestBody SubSystemSaveReqVO updateReqVO) {
        subSystemService.updateSubSystem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外部系统")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:system:delete')")
    public CommonResult<Boolean> deleteSubSystem(@RequestParam("id") Long id) {
        subSystemService.deleteSubSystem(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除外部系统")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:system:delete')")
    public CommonResult<Boolean> deleteSubSystemList(@RequestParam("ids") List<Long> ids) {
        subSystemService.deleteSubSystemList(ids);
        return success(true);
    }

    @GetMapping("/oauth2-client-simple-list")
    @Operation(summary = "获得 OAuth2 客户端精简列表（用于关联外部系统）")
    @PreAuthorize("@ss.hasPermission('sub-system:system:list')")
    public CommonResult<List<SubSystemOAuth2ClientSimpleRespVO>> getOAuth2ClientSimpleList(
            @RequestParam(value = "excludeSubSystemId", required = false) Long excludeSubSystemId) {
        return success(subSystemService.getOAuth2ClientSimpleList(excludeSubSystemId));
    }

}
