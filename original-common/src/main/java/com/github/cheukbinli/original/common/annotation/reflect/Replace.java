package com.github.cheukbinli.original.common.annotation.reflect;

import java.lang.annotation.*;

/***
 * 
 * @author Bin
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Replace {

	/***
	 * 唯一ID
	 * 
	 * @return
	 */
	String id() default "-1";

	String field() default "";

	/***
	 * 1:你好吗 2:一点都不好
	 * 
	 * @return
	 */
	String[] replacementRule() default "";

}
