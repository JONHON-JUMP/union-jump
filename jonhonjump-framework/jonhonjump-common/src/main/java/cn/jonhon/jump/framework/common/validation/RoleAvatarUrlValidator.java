package cn.jonhon.jump.framework.common.validation;

import cn.hutool.core.util.StrUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 角色头像校验：static:文件名 / http(s) URL
 */
public class RoleAvatarUrlValidator implements ConstraintValidator<RoleAvatarUrl, String> {

    private static final String STATIC_AVATAR_PREFIX = "static:";
    private static final Pattern STATIC_AVATAR_PATTERN = Pattern.compile("^static:[a-z][a-z0-9_-]*$");
    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);

    @Override
    public void initialize(RoleAvatarUrl annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        if (value.startsWith(STATIC_AVATAR_PREFIX)) {
            return STATIC_AVATAR_PATTERN.matcher(value).matches();
        }
        return HTTP_URL_PATTERN.matcher(value).matches();
    }

}
