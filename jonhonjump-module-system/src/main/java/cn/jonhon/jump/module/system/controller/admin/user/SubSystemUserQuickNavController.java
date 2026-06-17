package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavCandidateRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavSaveReqVO;
import cn.jonhon.jump.module.system.service.user.SubSystemUserQuickNavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 用户外部子系统快捷导航")
@RestController
@RequestMapping("/system/user/sub-system-quick-nav")
@Validated
public class SubSystemUserQuickNavController {

    @Resource
    private SubSystemUserQuickNavService subSystemUserQuickNavService;

    @GetMapping("/list")
    @Operation(summary = "获得当前用户在指定外部子系统的快捷导航配置")
    @Parameter(name = "subSystemId", description = "外部子系统编号", required = true)
    public CommonResult<SubSystemUserQuickNavRespVO> getUserQuickNavList(@RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemUserQuickNavService.getUserQuickNav(getLoginUserId(), subSystemId));
    }

    @GetMapping("/candidate-list")
    @Operation(summary = "获得当前用户在指定外部子系统可选的快捷导航候选菜单")
    @Parameter(name = "subSystemId", description = "外部子系统编号", required = true)
    public CommonResult<List<SubSystemUserQuickNavCandidateRespVO>> getCandidateList(
            @RequestParam("subSystemId") Long subSystemId) {
        return success(subSystemUserQuickNavService.getCandidateList(getLoginUserId(), subSystemId));
    }

    @PutMapping("/save")
    @Operation(summary = "保存当前用户在指定外部子系统的快捷导航配置")
    public CommonResult<Boolean> saveUserQuickNav(@Valid @RequestBody SubSystemUserQuickNavSaveReqVO reqVO) {
        subSystemUserQuickNavService.saveUserQuickNav(getLoginUserId(), reqVO.getSubSystemId(), reqVO.getMenuIds());
        return success(true);
    }

}
