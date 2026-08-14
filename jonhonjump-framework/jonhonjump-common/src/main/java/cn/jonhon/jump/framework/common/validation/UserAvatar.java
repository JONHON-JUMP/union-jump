package cn.jonhon.jump.framework.common.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UserAvatarValidator.class)
public @interface UserAvatar {

    String message() default "头像地址格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
