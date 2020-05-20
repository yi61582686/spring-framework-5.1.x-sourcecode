package com.on.cglib;

import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Enhancer;

import java.io.IOException;

public class TestCglib {

	public static void main(String[] args) {
		// 在build中生成代理类的class文件，对应的com/on/cglib目录下
		String path = "/Users/hh.wang/mySpaces/Fusion/gitHub/spring-framework-5.1.x/testSpring/build";
		System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, path);

		// 模拟生成cglib的代理类
		Enhancer enhancer =  new Enhancer();
		enhancer.setSuperclass(TestService.class);
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		enhancer.setCallback(new TestMethodInterceptor());
		TestService service = (TestService) enhancer.create();
		service.query2();
	}

}
