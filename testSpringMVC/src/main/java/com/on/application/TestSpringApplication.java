package com.on.application;

import com.on.config.AppConfig;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;

public class TestSpringApplication {

	public static void run() throws LifecycleException, InterruptedException {

		// 初始化Spring环境，为了支持SpringMVC，需要使用WebApplicationContext
		AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		applicationContext.register(AppConfig.class);
		applicationContext.refresh();

		File base = new File(System.getProperty("java.io.tmpdir"));

		// 实例化Tomcat容器
		Tomcat tomcat = new Tomcat();
		// 设置端口号
		tomcat.setPort(8080);
		/**
		 * addWebapp()与addContext()方法，都可以为tomcat环境添加一个Context对象，
		 * 区别是addWebapp表示这是一个web项目。
		 * contextPath：tomcat的访问路径；
		 * docBase：项目的Webapp目录。
		 */
		//tomcat.addWebapp("/", base.getAbsolutePath());
		Context context = tomcat.addContext("/", base.getAbsolutePath());

		// 初始化SpringMVC环境
		// 1. 初始化DispatcherServlet
		DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);

		// 2. 将DispatcherServlet将入到Tomcat容器中
		tomcat.addServlet(context, "dispatcherServlet", dispatcherServlet).setLoadOnStartup(0);
		// 3. 设置DispatcherServlet的映射，拦截的请求路径，拦截所有请求
		context.addServletMapping("/", "dispatcherServlet");

		// 启动tomcat
		tomcat.start();
		tomcat.getServer().await();

	}

}
