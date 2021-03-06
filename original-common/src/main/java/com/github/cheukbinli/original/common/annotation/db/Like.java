package com.github.cheukbinli.original.common.annotation.db;

import java.lang.annotation.*;

/***
 * 
 * @Title: original-common
 * @Description: ? like %xxx%
 * @Company: 
 * @Email: 20796698@qq.com
 * @author cheuk.bin.li
 * @date 2017年11月7日  上午11:10:06
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Like {

	/***
	 * 匹配左边:    %xxxxx
	 * @return
	 */
	boolean MatchLeftSide() default true;

	/***
	 * 匹配右边:    xxxxx%
	 * @return
	 */
	boolean MatchRightSide() default true;

}
