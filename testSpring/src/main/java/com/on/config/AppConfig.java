package com.on.config;

import com.on.dao.TestDao2;
import com.on.dao.TestDao3;
import org.springframework.context.annotation.*;

/**
 *
 */
@Configuration
@ComponentScan({"com.on"})
@Import(TestImportSelector.class)
//@ImportResource("applicationContext.xml") // 引入xml配置文件
@EnableAspectJAutoProxy
public class AppConfig {

	/**
	 * 调用getTes2()方法，在没有@Configuration注解时，TestDao2类的构造方法执行两次，有@Configuration注解时，执行一次
	 * 说明了，在有@Configuration注解时，getTestDao2方法已经被修改过了(cglib生产代理对象时修改了getTestDao2方法)
	 *
	 * 代理类实现了EnEnhancedConfiguration接口，EnEnhancedConfiguration接口继承了BeanFactoryAware接口，
	 * BeanFactoryAware接口提供了setBeanFactory()方法，可以将beanFactory当做一个属性放入代理类中，因而可以
	 * 在代理类中通过beanFactory获取对象，进而增强代理类。源代码见：ConfigurationClassEnhancer类newEnhancer()方法
	 * 原理：当通过配置类中的方法获取对象时，第一次时会创建对象，之后就会从Spring的beanFactory中获取对象，不会重复创建对象
	 */
	@Bean
	public TestDao3 testDao3() {
		testDao2();
		return new TestDao3();
	}

	@Bean
	public TestDao2 testDao2() {
		return new TestDao2();
	}
	
	
	public static void main(String[] args) {
		String s = "@";
		System.out.println(s.getBytes());
	}
	
}
