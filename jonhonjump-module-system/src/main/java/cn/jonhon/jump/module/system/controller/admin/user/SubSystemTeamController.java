package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUsersSimpleRespVO;
import cn.jonhon.jump.module.system.service.user.SubSystemTeamService;
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

@Tag(name = "管理后台 - 外部系统班组")
@RestController
@RequestMapping("/system/sub-system-team")
@Validated
public class SubSystemTeamController {

    @Resource
    private SubSystemTeamService subSystemTeamService;

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

}
