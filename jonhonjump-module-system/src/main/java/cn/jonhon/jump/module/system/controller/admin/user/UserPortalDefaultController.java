package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.portal.UserPortalDefaultRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.portal.UserPortalDefaultSaveReqVO;
import cn.jonhon.jump.module.system.service.user.UserPortalDefaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 用户门户默认打开系统")
@RestController
@RequestMapping("/system/user/portal-default")
@Validated
public class UserPortalDefaultController {

    @Resource
    private UserPortalDefaultService userPortalDefaultService;

    @GetMapping("/get")
    @Operation(summary = "获得当前用户的门户默认打开系统配置")
    public CommonResult<UserPortalDefaultRespVO> getUserPortalDefault() {
        return success(userPortalDefaultService.getUserPortalDefault(getLoginUserId()));
    }

    @PutMapping("/save")
    @Operation(summary = "保存当前用户的门户默认打开系统配置")
    public CommonResult<Boolean> saveUserPortalDefault(@Valid @RequestBody UserPortalDefaultSaveReqVO reqVO) {
        userPortalDefaultService.saveUserPortalDefault(getLoginUserId(), reqVO.getSubSystemId());
        return success(true);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清除当前用户的门户默认打开系统配置")
    public CommonResult<Boolean> clearUserPortalDefault() {
        userPortalDefaultService.clearUserPortalDefault(getLoginUserId());
        return success(true);
    }

}
