package cn.jonhon.jump.module.system.controller.admin.oauth2;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.util.http.HttpUtils;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.SubSystemCardLoginReqVO;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.SubSystemCardLoginRespVO;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.SubSystemUserPermissionRespVO;
import cn.jonhon.jump.module.system.service.user.SubSystemUsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_CARD_LOGIN_CLIENT_MISMATCH;

/**
 * 子系统刷卡等机端登录：无用户 Token，凭 OAuth 客户端密钥校验花名册 / 拉取权限。
 */
@Tag(name = "管理后台 - OAuth2 子系统刷卡认证")
@RestController
@RequestMapping("/system/oauth2/subsystem/v1")
@Validated
public class OAuth2SubSystemAuthController {

    @Resource
    private SubSystemUsersService subSystemUsersService;

    @PostMapping("/card-login")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "子系统刷卡校验", description = "按 clientId + username 校验外部系统花名册；支持 HTTP Basic 或 body 传递 clientSecret；无需用户 Token / tenant-id")
    public CommonResult<SubSystemCardLoginRespVO> cardLogin(HttpServletRequest request,
                                                            @Valid @RequestBody SubSystemCardLoginReqVO reqVO) {
        String clientId = reqVO.getClientId().trim();
        String clientSecret = resolveClientSecret(request, clientId, reqVO.getClientSecret());
        return success(subSystemUsersService.cardLogin(clientId, clientSecret, reqVO.getUsername()));
    }

    @PostMapping("/permission-info")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "查询子系统用户权限", description = "按 clientId + username 返回角色、菜单树、permission 标识；鉴权方式同刷卡校验；无需用户 Token / tenant-id")
    public CommonResult<SubSystemUserPermissionRespVO> permissionInfo(HttpServletRequest request,
                                                                      @Valid @RequestBody SubSystemCardLoginReqVO reqVO) {
        String clientId = reqVO.getClientId().trim();
        String clientSecret = resolveClientSecret(request, clientId, reqVO.getClientSecret());
        return success(subSystemUsersService.getPermissionInfo(clientId, clientSecret, reqVO.getUsername()));
    }

    private String resolveClientSecret(HttpServletRequest request, String clientId, String bodySecret) {
        String[] basic = HttpUtils.obtainBasicAuthorization(request);
        if (ArrayUtil.isNotEmpty(basic) && basic.length == 2) {
            if (!StrUtil.equals(clientId, basic[0])) {
                throw exception(SUB_SYSTEM_CARD_LOGIN_CLIENT_MISMATCH);
            }
            return basic[1];
        }
        if (StrUtil.isNotBlank(bodySecret)) {
            return bodySecret;
        }
        throw exception0(BAD_REQUEST.getCode(), "client_id 或 client_secret 未正确传递");
    }

}
