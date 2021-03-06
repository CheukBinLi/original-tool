package com.github.cheukbinli.original.common.rmi.model;

import com.github.cheukbinli.original.common.rmi.RmiBeanFactory;

import java.io.Serializable;

public class ClassBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String id;// 注入ID
	private Class<?> interfaceClassFile;// 接口
	private Class<?> originalClassFile;// 原实现
	private Class<?> proxyClassFile;// 代理
	private String registrationServiceName;// 服务名
	private String version;// 版本
	private Object instance;
	private boolean multiInstance;

	public String getId() {
		return id;
	}

	public ClassBean setId(String id) {
		this.id = id;
		return this;
	}

	public Class<?> getInterfaceClassFile() {
		return interfaceClassFile;
	}

	public ClassBean setInterfaceClassFile(Class<?> interfaceClassFile) {
		this.interfaceClassFile = interfaceClassFile;
		return this;
	}

	public Class<?> getOriginalClassFile() {
		return originalClassFile;
	}

	public ClassBean setOriginalClassFile(Class<?> originalClassFile) {
		this.originalClassFile = originalClassFile;
		return this;
	}

	public Class<?> getProxyClassFile() {
		return proxyClassFile;
	}

	public ClassBean setProxyClassFile(Class<?> proxyClassFile) {
		this.proxyClassFile = proxyClassFile;
		return this;
	}

	public String getRegistrationServiceName() {
		return registrationServiceName;
	}

	public ClassBean setRegistrationServiceName(String registrationServiceName) {
		this.registrationServiceName = registrationServiceName;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public ClassBean setVersion(String version) {
		this.version = version;
		return this;
	}

	public Object getInstance(@SuppressWarnings("rawtypes") RmiBeanFactory rmiBeanFactory) throws InstantiationException, IllegalAccessException {
		if (null == this.instance) {
			synchronized (this) {
				if (null == this.instance)
					setInstance(rmiBeanFactory.getBean(this.id));
			}
		}
		return instance;
	}

	public ClassBean setInstance(Object instance) {
		this.instance = instance;
		return this;
	}

	public boolean isMultiInstance() {
		return multiInstance;
	}

	public ClassBean setMultiInstance(boolean multiInstance) {
		this.multiInstance = multiInstance;
		return this;
	}

	public ClassBean(Class<?> originalClassFile, String registrationServiceName, String version, boolean multiInstance) {
		super();
		this.originalClassFile = originalClassFile;
		this.registrationServiceName = registrationServiceName;
		this.version = version;
		this.multiInstance = multiInstance;
	}

	public ClassBean(Class<?> originalClassFile, String registrationServiceName, Object instance, String version) {
		super();
		this.originalClassFile = originalClassFile;
		this.registrationServiceName = registrationServiceName;
		this.version = version;
		this.instance = instance;
	}

	public ClassBean() {
		super();
	}

}
