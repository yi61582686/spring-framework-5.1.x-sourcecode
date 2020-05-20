package com.on;

import com.on.config.AppConfig;
import com.on.service.TestService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestApplication {

	public static void main(String[] args) {
	ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		TestService testService = context.getBean(TestService.class);
		testService.findAll();
	}

}
