package com.github.cheukbinli.original.rmi.config.spring.schema;

import com.github.cheukbinli.original.rmi.config.AnnotationDrivenConfig;
import com.github.cheukbinli.original.rmi.config.ReferenceGroupConfig;
import com.github.cheukbinli.original.rmi.config.RmiConfig;
import com.github.cheukbinli.original.rmi.config.ServiceGroupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class RmiNamespaceHandler extends NamespaceHandlerSupport {

	private final static Logger LOG = LoggerFactory.getLogger(RmiNamespaceHandler.class);

	public void init() {
		try {
			registerBeanDefinitionParser("config", new RmiBeanDefinitionParser(RmiConfig.class));
			registerBeanDefinitionParser("service-group", new RmiBeanDefinitionParser(ServiceGroupConfig.class));
			registerBeanDefinitionParser("reference-group", new RmiBeanDefinitionParser(ReferenceGroupConfig.class));
			registerBeanDefinitionParser("annotation-driven", new RmiBeanDefinitionParser(AnnotationDrivenConfig.class));
		} catch (Exception e) {
			LOG.error(null, e);
		}
	}

}
