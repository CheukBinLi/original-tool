package com.github.cheukbinli.original.spring.plugin.util.design.factory;

import com.github.cheukbinli.original.common.util.conver.StringUtil;
import com.github.cheukbinli.original.common.util.design.factory.AbstractHandlerManager;
import com.github.cheukbinli.original.common.util.design.factory.Handler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("unchecked")
public abstract class DefaultHandlerManager<T extends Handler<?>> extends AbstractHandlerManager<T> implements ApplicationContextAware, InitializingBean {

    protected DefaultListableBeanFactory defaultListableBeanFactory;

    @Override
    public T instance(Class<T> clazz) {
        if (concat(clazz)) {
            return getHandler(clazz.getName());
        }
        return registerBean(clazz, StringUtil.toLowerCaseFirstOne(clazz.getName()), null, "init");
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        this.defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public T registerBean(Class<T> clazz, String registerName, Map<String, Object> initPropertyValue, String initMethodName) {

        T instance;
        try {
            instance = defaultListableBeanFactory.getBean(clazz);
            if (null != instance) {
                return instance;
            }
        } catch (NoSuchBeanDefinitionException e) {
        }

        BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        if (null != initMethodName) {
            bean.setInitMethodName(initMethodName);
        }
        if (null != initPropertyValue) {
            for (Entry<String, Object> en : initPropertyValue.entrySet()) {
                bean.addPropertyValue(en.getKey(), defaultListableBeanFactory.getBeanDefinition(en.getKey()));
            }
        }
        String beanName = null == registerName ? StringUtil.toLowerCaseFirstOne(clazz.getSimpleName()) : registerName;
        defaultListableBeanFactory.registerBeanDefinition(beanName, bean.getRawBeanDefinition());
        instance = (T) defaultListableBeanFactory.getBean(beanName);
        return instance;
    }
}
