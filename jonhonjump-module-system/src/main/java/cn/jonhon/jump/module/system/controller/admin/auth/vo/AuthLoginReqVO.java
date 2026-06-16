package cn.jonhon.jump.module.system.controller.admin.auth.vo;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.validation.InEnum;
import cn.jonhon.jump.module.system.enums.auth.LoginIdentityTypeEnum;
import cn.jonhon.jump.module.system.enums.social.SocialTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 账号密码登录 Request VO，如果登录并绑定社交用户，需要传递 social 开头的参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthLoginReqVO extends CaptchaVerificationReqVO {

    @Schema(description = "登录方式：auto-自动识别 username-用户名 employee-工号 domain-域账号", example = "auto")
    @InEnum(LoginIdentityTypeEnum.class)
    private String loginType;

    @Schema(description = "登录账号（用户名 / 工号 / 域账号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotEmpty(message = "登录账号不能为空")
    @Length(max = 64, message = "登录账号长度不能超过 64 位")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "buzhidao")
    @NotEmpty(message = "密码不能为空")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    // ========== 绑定社交登录时，需要传递如下参数 ==========

    @Schema(description = "社交平台的类型，参见 SocialTypeEnum 枚举值", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @InEnum(SocialTypeEnum.class)
    private Integer socialType;

    @Schema(description = "授权码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private String socialCode;

    @Schema(description = "state", requiredMode = Schema.RequiredMode.REQUIRED, example = "9b2ffbc1-7425-4155-9894-9d5c08541d62")
    private String socialState;

    @AssertTrue(message = "用户名格式为 4-30 位字母或数字")
    public boolean isUsernameFormatValid() {
        if (LoginIdentityTypeEnum.of(loginType) != LoginIdentityTypeEnum.USERNAME) {
            return true;
        }
        return StrUtil.isNotEmpty(username)
                && username.length() >= 4 && username.length() <= 30
                && username.matches("^[a-zA-Z0-9]+$");
    }

    @AssertTrue(message = "工号长度不能超过 20 位")
    public boolean isEmployeeNoFormatValid() {
        if (LoginIdentityTypeEnum.of(loginType) != LoginIdentityTypeEnum.EMPLOYEE) {
            return true;
        }
        return StrUtil.isNotEmpty(username) && username.length() <= 20;
    }

    @AssertTrue(message = "域账号长度为 2-32 位")
    public boolean isDomainNoFormatValid() {
        LoginIdentityTypeEnum type = LoginIdentityTypeEnum.of(loginType);
        if (type != LoginIdentityTypeEnum.DOMAIN && type != LoginIdentityTypeEnum.AUTO) {
            return true;
        }
        if (type == LoginIdentityTypeEnum.AUTO) {
            return true;
        }
        return StrUtil.isNotEmpty(username) && username.length() >= 2 && username.length() <= 32;
    }

    @AssertTrue(message = "授权码不能为空")
    public boolean isSocialCodeValid() {
        return socialType == null || StrUtil.isNotEmpty(socialCode);
    }

    @AssertTrue(message = "授权 state 不能为空")
    public boolean isSocialState() {
        return socialType == null || StrUtil.isNotEmpty(socialState);
    }

}
