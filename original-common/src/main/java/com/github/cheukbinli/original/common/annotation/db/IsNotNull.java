package com.github.cheukbinli.original.common.annotation.db;

import java.lang.annotation.*;

/***
 * 
 * @Title: original-common
 * @Description: is not null or ? <> ''
 * @Company: 
 * @Email: 20796698@qq.com
 * @author cheuk.bin.li
 * @date 2017年11月7日  上午11:05:09
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface IsNotNull {
}
