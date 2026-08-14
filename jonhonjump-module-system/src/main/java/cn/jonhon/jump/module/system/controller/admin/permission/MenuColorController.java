package cn.jonhon.jump.module.system.controller.admin.permission;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorRespVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorSimpleRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import cn.jonhon.jump.module.system.service.permission.MenuColorService;
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

@Tag(name = "管理后台 - 菜单样式")
@RestController
@RequestMapping("/system/menu-style")
@Validated
public class MenuColorController {

    @Resource
    private MenuColorService menuColorService;

    @PostMapping("/create")
    @Operation(summary = "创建菜单颜色")
    @PreAuthorize("@ss.hasPermission('system:menu-style:create')")
    public CommonResult<Long> createMenuColor(@Valid @RequestBody MenuColorSaveReqVO createReqVO) {
        return success(menuColorService.createMenuColor(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改菜单颜色")
    @PreAuthorize("@ss.hasPermission('system:menu-style:update')")
    public CommonResult<Boolean> updateMenuColor(@Valid @RequestBody MenuColorSaveReqVO updateReqVO) {
        menuColorService.updateMenuColor(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除菜单颜色")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:menu-style:delete')")
    public CommonResult<Boolean> deleteMenuColor(@RequestParam("id") Long id) {
        menuColorService.deleteMenuColor(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除菜单颜色")
    @PreAuthorize("@ss.hasPermission('system:menu-style:delete')")
    public CommonResult<Boolean> deleteMenuColorList(@RequestParam("ids") List<Long> ids) {
        menuColorService.deleteMenuColorList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得菜单颜色")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:menu-style:query')")
    public CommonResult<MenuColorRespVO> getMenuColor(@RequestParam("id") Long id) {
        MenuColorDO color = menuColorService.getMenuColor(id);
        return success(BeanUtils.toBean(color, MenuColorRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得菜单颜色分页")
    @PreAuthorize("@ss.hasPermission('system:menu-style:query')")
    public CommonResult<PageResult<MenuColorRespVO>> getMenuColorPage(@Valid MenuColorPageReqVO pageReqVO) {
        PageResult<MenuColorDO> pageResult = menuColorService.getMenuColorPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MenuColorRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用的菜单颜色精简列表（选色器用）")
    public CommonResult<List<MenuColorSimpleRespVO>> getMenuColorSimpleList() {
        List<MenuColorDO> list = menuColorService.getMenuColorSimpleList();
        return success(BeanUtils.toBean(list, MenuColorSimpleRespVO.class));
    }

    @GetMapping("/default")
    @Operation(summary = "获得通用默认菜单样式")
    public CommonResult<MenuColorSimpleRespVO> getDefaultMenuStyle() {
        MenuColorDO style = menuColorService.getDefaultMenuStyle();
        return success(BeanUtils.toBean(style, MenuColorSimpleRespVO.class));
    }

}
