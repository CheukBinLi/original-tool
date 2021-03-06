package com.github.cheukbinli.original.rmi.model;

import com.github.cheukbinli.original.common.rmi.model.ClassBean;
import com.github.cheukbinli.original.rmi.unit.Reflection;

import java.io.Serializable;
import java.lang.reflect.Method;

public class MethodBean extends com.github.cheukbinli.original.common.rmi.model.MethodBean implements Serializable {

	private static final long serialVersionUID = 2697039684595021790L;

	private String md5Code;

	public static MethodBean builder(final ClassBean classBean, final Method currentMethod) throws Throwable {
		return new MethodBean(classBean, currentMethod, md5Code(classBean, currentMethod));
	}

	public static String md5Code(final ClassBean classBean, final Method currentMethod) throws Throwable {
		StringBuilder sb = new StringBuilder();
		sb.append(classBean.getRegistrationServiceName()).append(":");
		sb.append(classBean.getVersion()).append(":");
		sb.append(Reflection.newInstance().genericMethod(currentMethod));
		return Reflection.newInstance().genericRmiMethodMd5Code(classBean.getRegistrationServiceName(),classBean.getInterfaceClassFile().getName(), classBean.getVersion(), currentMethod);
	}

	public MethodBean(ClassBean classBean, Method currentMethod, String md5Code) {
		super(classBean, currentMethod);
		this.md5Code = md5Code;
	}

	public MethodBean(ClassBean classBean, Method currentMethod) {
		super(classBean, currentMethod);
	}

	public MethodBean(ClassBean classBean) {
		super(classBean);
	}

	public String getMd5Code() {
		return md5Code;
	}

	public MethodBean setMd5Code(String md5Code) {
		this.md5Code = md5Code;
		return this;
	}

}
