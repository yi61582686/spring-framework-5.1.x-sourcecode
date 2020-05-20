package com.on;

import com.on.dao.*;
import com.on.service.TestDaoService;
import com.on.config.AppConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestSpring {

	// 手动向spring中注册bean
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//		new ClassPathXmlApplicationContext("xx");
		context.register(AppConfig.class); // 自己再注册一个类或配置类
//		context.addBeanFactoryPostProcessor(new TestBeanFactoryPostProcessor()); // 添加自定义的BeanFactoryPostProcessor
//		context.scan("ss"); // 扫描其它包
		context.refresh(); // 调用register注册类时，需要调用refresh()方法
		TestDao dao = context.getBean(TestDao.class);
		System.out.println(dao.hashCode());
		dao = context.getBean(TestDao.class);
		System.out.println(dao.hashCode());
		context.getBean(TestDao1.class).query();
		context.getBean(TestDao2.class).query();
		context.getBean(TestDao3.class).query();
		System.out.println("-------------");
		TestDaoService service = context.getBean(TestDaoService.class);
		service.query("testDao");
		service.query("com.on.dao.TestDao1");
		service.query("testDao2");
		service.query("testDao3");
		
		TestDao4 dao4 = context.getBean(TestDao4.class);
		dao4.setName("on");
		dao4.query();

	}

	// 自动注册bean
//	public static void main(String[] args) {
//		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class); // 直接注册配置类
//		TestDao dao = context.getBean(TestDao.class);
//		System.out.println(dao.hashCode());
//		TestDao dao1 = context.getBean(TestDao.class);
//		System.out.println(dao1.hashCode());
//		dao.query();
//	}



}
