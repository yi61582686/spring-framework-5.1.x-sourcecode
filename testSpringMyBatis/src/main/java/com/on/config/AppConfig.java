package com.on.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("com.on")
@Import(TestMapperImportBeanDefinitionRegistrar.class)
public class AppConfig {

}
