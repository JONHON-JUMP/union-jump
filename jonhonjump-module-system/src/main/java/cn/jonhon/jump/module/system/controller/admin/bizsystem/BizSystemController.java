package cn.jonhon.jump.module.system.controller.admin.bizsystem;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.bizsystem.vo.BizSystemPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.bizsystem.vo.BizSystemRespVO;
import cn.jonhon.jump.module.system.controller.admin.bizsystem.vo.BizSystemSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.bizsystem.BizSystemDO;
import cn.jonhon.jump.module.system.service.bizsystem.BizSystemService;
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

@Tag(name = "管理后台 - 系统明细")
@RestController
@RequestMapping("/system/biz-system")
@Validated
public class BizSystemController {

    @Resource
    private BizSystemService bizSystemService;

    @PostMapping("/create")
    @Operation(summary = "新增系统明细")
    @PreAuthorize("@ss.hasPermission('system:biz-system:create')")
    public CommonResult<Long> createBizSystem(@Valid @RequestBody BizSystemSaveReqVO createReqVO) {
        return success(bizSystemService.createBizSystem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改系统明细")
    @PreAuthorize("@ss.hasPermission('system:biz-system:update')")
    public CommonResult<Boolean> updateBizSystem(@Valid @RequestBody BizSystemSaveReqVO updateReqVO) {
        bizSystemService.updateBizSystem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除系统明细")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:biz-system:delete')")
    public CommonResult<Boolean> deleteBizSystem(@RequestParam("id") Long id) {
        bizSystemService.deleteBizSystem(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除系统明细")
    @PreAuthorize("@ss.hasPermission('system:biz-system:delete')")
    public CommonResult<Boolean> deleteBizSystemList(@RequestParam("ids") List<Long> ids) {
        bizSystemService.deleteBizSystemList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得系统明细")
    @PreAuthorize("@ss.hasPermission('system:biz-system:query')")
    public CommonResult<BizSystemRespVO> getBizSystem(@RequestParam("id") Long id) {
        BizSystemDO row = bizSystemService.getBizSystem(id);
        return success(BeanUtils.toBean(row, BizSystemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "系统明细分页")
    @PreAuthorize("@ss.hasPermission('system:biz-system:query')")
    public CommonResult<PageResult<BizSystemRespVO>> getBizSystemPage(@Valid BizSystemPageReqVO pageReqVO) {
        PageResult<BizSystemDO> page = bizSystemService.getBizSystemPage(pageReqVO);
        return success(BeanUtils.toBean(page, BizSystemRespVO.class));
    }

}
