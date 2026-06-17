package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuListReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuSaveReqVO;
import cn.jonhon.jump.module.system.service.user.SubSystemMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
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

}
