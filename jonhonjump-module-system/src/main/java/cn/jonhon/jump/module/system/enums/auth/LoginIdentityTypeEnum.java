package cn.jonhon.jump.module.system.enums.auth;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 管理后台登录身份类型
 */
@Getter
@AllArgsConstructor
public enum LoginIdentityTypeEnum implements ArrayValuable<String> {

    USERNAME("username"),
    EMPLOYEE("employee"),
    DOMAIN("domain"),
    AUTO("auto"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LoginIdentityTypeEnum::getType).toArray(String[]::new);

    private final String type;

    public static LoginIdentityTypeEnum of(String type) {
        if (StrUtil.isBlank(type)) {
            return AUTO;
        }
        for (LoginIdentityTypeEnum item : values()) {
            if (item.type.equalsIgnoreCase(type)) {
                return item;
            }
        }
        return AUTO;
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
