package com.github.cheukbinli.original.common.annotation.weixin;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MessageEventHandleAnnotation {
    String value() default "";
}
