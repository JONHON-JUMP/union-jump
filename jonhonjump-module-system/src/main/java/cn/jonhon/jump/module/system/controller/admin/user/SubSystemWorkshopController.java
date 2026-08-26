package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSimpleRespVO;
import cn.jonhon.jump.module.system.service.user.SubSystemWorkshopService;
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

@Tag(name = "管理后台 - 外部系统车间")
@RestController
@RequestMapping("/system/sub-system-workshop")
@Validated
public class SubSystemWorkshopController {

    @Resource
    private SubSystemWorkshopService subSystemWorkshopService;

    @GetMapping("/page")
    @Operation(summary = "获得外部系统车间分页")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:list')")
    public CommonResult<PageResult<SubSystemWorkshopRespVO>> getSubSystemWorkshopPage(@Valid SubSystemWorkshopPageReqVO pageReqVO) {
        return success(subSystemWorkshopService.getSubSystemWorkshopPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得外部系统车间")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:list')")
    public CommonResult<SubSystemWorkshopRespVO> getSubSystemWorkshop(@RequestParam("id") Long id) {
        return success(subSystemWorkshopService.getSubSystemWorkshop(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建外部系统车间")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:create')")
    public CommonResult<Long> createSubSystemWorkshop(@Valid @RequestBody SubSystemWorkshopSaveReqVO createReqVO) {
        return success(subSystemWorkshopService.createSubSystemWorkshop(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新外部系统车间")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:update')")
    public CommonResult<Boolean> updateSubSystemWorkshop(@Valid @RequestBody SubSystemWorkshopSaveReqVO updateReqVO) {
        subSystemWorkshopService.updateSubSystemWorkshop(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外部系统车间")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:delete')")
    public CommonResult<Boolean> deleteSubSystemWorkshop(@RequestParam("id") Long id) {
        subSystemWorkshopService.deleteSubSystemWorkshop(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除外部系统车间")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:delete')")
    public CommonResult<Boolean> deleteSubSystemWorkshopList(@RequestParam("ids") List<Long> ids) {
        subSystemWorkshopService.deleteSubSystemWorkshopList(ids);
        return success(true);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得外部系统车间精简列表（人员/用户表单下拉用，可按部门过滤）")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:list')")
    public CommonResult<List<SubSystemWorkshopSimpleRespVO>> getWorkshopSimpleList(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam(value = "deptId", required = false) Long deptId) {
        return success(subSystemWorkshopService.getWorkshopSimpleList(subSystemId, deptId));
    }

    @GetMapping("/by-dept")
    @Operation(summary = "按 JUMP 部门查询映射车间（用户创建联动用）")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:list')")
    public CommonResult<SubSystemWorkshopSimpleRespVO> getWorkshopByDept(
            @RequestParam("subSystemId") Long subSystemId,
            @RequestParam("deptId") Long deptId) {
        return success(subSystemWorkshopService.getWorkshopByDept(subSystemId, deptId));
    }

}
