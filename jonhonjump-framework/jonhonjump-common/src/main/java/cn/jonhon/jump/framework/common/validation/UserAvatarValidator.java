package cn.jonhon.jump.framework.common.validation;

import cn.hutool.core.util.StrUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 用户头像校验：空值 / system:角色标识 / http(s) URL
 */
public class UserAvatarValidator implements ConstraintValidator<UserAvatar, String> {

    private static final String SYSTEM_AVATAR_PREFIX = "system:";
    private static final Pattern SYSTEM_AVATAR_PATTERN = Pattern.compile("^system:[a-z][a-z0-9_]*$");
    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);

    @Override
    public void initialize(UserAvatar annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        if (value.startsWith(SYSTEM_AVATAR_PREFIX)) {
            return SYSTEM_AVATAR_PATTERN.matcher(value).matches();
        }
        return HTTP_URL_PATTERN.matcher(value).matches();
    }

}
