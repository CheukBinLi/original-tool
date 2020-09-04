package com.github.cheukbinli.original.common.annotation.rmi;

import java.lang.annotation.*;

/***
 * 远程调用客户端注解(接口/实现)
 * 
 * @author ben
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Deprecated
public @interface RmiClient {

	// String host() default "127.0.0.1:10086";

	String id() default "";// 注册名

	/***
	 * 远程实例名字
	 * 
	 * @return
	 */
	String serviceImplementation();

	/***
	 * 远程实例版本
	 * 
	 * @return
	 */
	String version() default "1.0";

	/**
	 * 本地多例
	 * 
	 * @return
	 */
	boolean multiInstance() default false;
}
