package cn.jonhon.jump.framework.common.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色头像地址：static:文件名 或 http(s) URL
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RoleAvatarUrlValidator.class)
public @interface RoleAvatarUrl {

    String message() default "头像地址格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
