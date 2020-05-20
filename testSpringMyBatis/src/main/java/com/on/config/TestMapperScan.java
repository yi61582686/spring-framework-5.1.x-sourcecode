package com.on.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(TestMapperImportBeanDefinitionRegistrar.class)
public @interface TestMapperScan {

	String[] value() default {};

}
