package com.on.config;

import com.on.dao.TestMapper;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// 通过FactoryBean，根据class类型，来生成不同接口的对象
// jdk生成代理对象时，需要传入InvocationHandler对象
//@Component
public class TestMapperFactoryBean implements FactoryBean<TestMapper>, InvocationHandler {

//	Class<?> clazz;
//
//	public TestMapperFactoryBean(Class<?> clazz) {
//		this.clazz = clazz;
//	}

	@Override
	public TestMapper getObject() throws Exception {

		// JDK生成代理对象
		Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{TestMapper.class}, this);
		return  (TestMapper) proxy;
	}

	@Override
	public Class<TestMapper> getObjectType() {
		return TestMapper.class;
	}

	// 生成代理对象
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//		Method method1 = proxy.getClass().getInterfaces()[0].getMethod(method.getName(), null);
//		Select annotation = method1.getDeclaredAnnotation(Select.class);
//		System.out.println(annotation.value());
		System.out.println("proxy");
		return null;

	}


}
