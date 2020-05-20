package com.on;


import com.on.config.AppConfig;
import com.on.service.AService;
import com.on.service.BService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Spring 循环引用Demo
 */
public class TestCircularReference {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		context.getBean(AService.class).run();
		context.getBean(BService.class).run();

	}

}
