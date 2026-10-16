package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemHangableNodeRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSaveReqVO;
import cn.jonhon.jump.module.system.service.user.SubSystemWorkshopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 业务系统子表叶子")
@RestController
@RequestMapping("/system/sub-system-workshop")
@Validated
public class SubSystemWorkshopController {

    @Resource
    private SubSystemWorkshopService workshopService;

    @GetMapping("/list")
    @Operation(summary = "子表叶子列表（启用态字典/辅助筛选）")
    @PreAuthorize("@ss.hasPermission('sub-system:user:list') or @ss.hasPermission('sub-system:role:list') or @ss.hasPermission('system:user:query') or @ss.hasPermission('sub-system:workshop:list')")
    public CommonResult<List<SubSystemWorkshopRespVO>> getEnabledList() {
        return success(workshopService.getEnabledWorkshopList());
    }

    @GetMapping("/hangable-tree")
    @Operation(summary = "人员可挂外部树（仅子表叶子可选）")
    @PreAuthorize("@ss.hasPermission('sub-system:user:create') or @ss.hasPermission('system:user:update') or @ss.hasPermission('system:user:query') or @ss.hasPermission('sub-system:workshop:list')")
    public CommonResult<List<SubSystemHangableNodeRespVO>> getHangableTree() {
        return success(workshopService.getHangableTree());
    }

    @GetMapping("/page")
    @Operation(summary = "业务系统子表叶子分页")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:list')")
    public CommonResult<PageResult<SubSystemWorkshopRespVO>> getPage(@Valid SubSystemWorkshopPageReqVO pageReqVO) {
        return success(workshopService.getWorkshopPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得业务系统子表叶子")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:list')")
    public CommonResult<SubSystemWorkshopRespVO> get(@RequestParam("id") Long id) {
        return success(workshopService.getWorkshop(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建业务系统子表叶子")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:create')")
    public CommonResult<Long> create(@Valid @RequestBody SubSystemWorkshopSaveReqVO createReqVO) {
        return success(workshopService.createWorkshop(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新业务系统子表叶子")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody SubSystemWorkshopSaveReqVO updateReqVO) {
        workshopService.updateWorkshop(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除业务系统子表叶子")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        workshopService.deleteWorkshop(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除业务系统子表叶子")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:delete')")
    public CommonResult<Boolean> deleteList(@RequestParam("ids") List<Long> ids) {
        workshopService.deleteWorkshopList(ids);
        return success(true);
    }

}
