package com.on.config;

import org.springframework.web.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

// 实现WebApplicationInitializer接口的类，在Tomcat启动时会被自动执行
// Servlet3.0标准
public class TestWebApplicationInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		/**
		 * 使用外置的tomcat时，在这里初始化Spring和SpringMVC环境
		 * 使用内嵌的tomcat的jar包时，已经无法在这里初始化Spring和SpringMVC环境了
		 */

	}

}
