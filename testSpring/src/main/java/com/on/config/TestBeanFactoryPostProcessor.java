package com.on.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class TestBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinition definition = beanFactory.getBeanDefinition("testDao");
		definition.getConstructorArgumentValues().addGenericArgumentValue("com.on.dao.Dao");
//		definition.setScope("prototype");// 修改为多例
	}
}
