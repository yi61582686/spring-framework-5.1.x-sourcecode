package com.on.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class TestInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
	
	/**
	 * 在bean实例化前调用：
	 * 	 如果返回null，会继续执行spring流程，实例化bean，并完成初始化；
	 * 	 如果返回一个Object，那么会用这个Object替代目标 bean。
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName the name of the bean
	 * @return
	 * @throws BeansException
	 */
	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}
	
	/**
	 * 判断是否需要为 bean 进行属性注入。
	 * 	 如果返回true，需要进行属性注入；
	 * 	 如果返回false，不需要进行属性注入。
	 *
	 * @param bean the bean instance created, with properties not having been set yet
	 * @param beanName the name of the bean
	 * @return
	 * @throws BeansException
	 */
	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
//		if(beanName.equals("testDao4")) // 不为TestDao4注入属性
//			return false;
		return true;
	}
	
	/**
	 * 在bean注入属性前，可以修改属性的值。
	 * @param pvs the property values that the factory is about to apply (never {@code null})
	 * @param bean the bean instance created, but whose properties have not yet been set
	 * @param beanName the name of the bean
	 * @return
	 * @throws BeansException
	 */
	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		return null;
	}
}
