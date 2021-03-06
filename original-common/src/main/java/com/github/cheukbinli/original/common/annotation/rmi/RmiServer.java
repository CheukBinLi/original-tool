package com.github.cheukbinli.original.common.annotation.rmi;

import java.lang.annotation.*;

/***
 * 远程调用服务端注解(实现)
 * 
 * @author ben
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Deprecated
public @interface RmiServer {
	/***
	 * 注解实例名字
	 * 
	 * @return
	 */
	String serviceName() default "";

	/***
	 * 版本
	 * 
	 * @return
	 */
	String version() default "1.0";

	/**
	 * 多例
	 * 
	 * @return
	 */
	boolean multiInstance() default false;

}
