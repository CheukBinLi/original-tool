package com.github.cheukbinli.original.common.annotation.reflect;

import java.lang.annotation.*;

/***
 *
 * @Title: original-common
 * @Description: 字段、方法别名
 * @Company:
 * @Email: 20796698@qq.com
 * @author cheuk.bin.li
 * @date 2017年8月4日 下午11:20:58
 *
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Alias {

    String value() default "";

}
