package com.on.config;

import com.on.dao.TestMapper;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 通过实现ImportBeanDefinitionRegistrar接口，将接口转换为对象，然后注册到Spring容器中
 */
public class TestMapperImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		// 通过BeanDefinitionBuilder，获取到接口beanDefinition信息
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(TestMapper.class);
		GenericBeanDefinition beanDefinition = (GenericBeanDefinition) builder.getBeanDefinition();

		// 修改接口的beanDefinition中的信息
		// 修改bean的class类型为FactoryBean，这样就能通过工厂bean获取到对象
		beanDefinition.setBeanClass(TestMapperFactoryBean.class);

		// 增加构造方法，即我们实现的TestMapperFactoryBean中提供的构造方法，需要传入接口的类型
		// 这里传入字符串即可，Spring会将字符串替换为接口的class类型
//		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testMapper");

		// 向容器中注册bean
		 registry.registerBeanDefinition("testMapper", beanDefinition);
//		registry.registerBeanDefinition(beanDefinition.getBeanClassName(), beanDefinition);

	}

}
