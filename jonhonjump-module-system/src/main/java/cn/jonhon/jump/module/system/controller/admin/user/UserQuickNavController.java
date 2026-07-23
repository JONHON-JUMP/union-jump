package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavSaveReqVO;
import cn.jonhon.jump.module.system.service.user.UserQuickNavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 用户快捷导航")
@RestController
@RequestMapping("/system/user/quick-nav")
@Validated
public class UserQuickNavController {

    @Resource
    private UserQuickNavService userQuickNavService;

    @GetMapping("/list")
    @Operation(summary = "获得当前用户的主系统快捷导航配置")
    public CommonResult<UserQuickNavRespVO> getUserQuickNavList() {
        return success(userQuickNavService.getUserQuickNav(getLoginUserId()));
    }

    @PutMapping("/save")
    @Operation(summary = "保存当前用户的主系统快捷导航配置")
    public CommonResult<UserQuickNavRespVO> saveUserQuickNav(@Valid @RequestBody UserQuickNavSaveReqVO reqVO) {
        return success(userQuickNavService.saveUserQuickNav(getLoginUserId(), reqVO.getMenuIds()));
    }

    @PostMapping("/sync-from-role")
    @Operation(summary = "按当前用户角色默认快捷导航，同步到数据库")
    public CommonResult<Boolean> syncUserQuickNavFromRole() {
        userQuickNavService.syncUserQuickNavFromRoles(getLoginUserId());
        return success(true);
    }

}
