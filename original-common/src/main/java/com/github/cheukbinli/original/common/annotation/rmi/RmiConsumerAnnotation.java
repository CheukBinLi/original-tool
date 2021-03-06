package com.github.cheukbinli.original.common.annotation.rmi;

import java.lang.annotation.*;

/***
 * 
 * @Title: original-common
 * @Description: 消费者：接口注解
 * @Company:
 * @Email: 20796698@qq.com
 * @author cheuk.bin.li
 * @date 2017年9月19日 下午10:01:50
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RmiConsumerAnnotation {

	/***
	 * 注入名
	 * 
	 * @return
	 */
	String id() default "";
	/***
	 * 服务名
	 * 
	 * @return
	 */
	String serviceName() default "";

	/***
	 * 版本
	 * 
	 * @return
	 */
	String version() default "";

}
