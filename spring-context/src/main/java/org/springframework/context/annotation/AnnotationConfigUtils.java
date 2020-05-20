/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.event.DefaultEventListenerFactory;
import org.springframework.context.event.EventListenerMethodProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Utility class that allows for convenient registration of common
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} and
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor}
 * definitions for annotation-based configuration. Also registers a common
 * {@link org.springframework.beans.factory.support.AutowireCandidateResolver}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.5
 * @see ContextAnnotationAutowireCandidateResolver
 * @see ConfigurationClassPostProcessor
 * @see CommonAnnotationBeanPostProcessor
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 * @see org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor
 */
public abstract class AnnotationConfigUtils {

	/**
	 * The bean name of the internally managed Configuration annotation processor.
	 */
	public static final String CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalConfigurationAnnotationProcessor";

	/**
	 * The bean name of the internally managed BeanNameGenerator for use when processing
	 * {@link Configuration} classes. Set by {@link AnnotationConfigApplicationContext}
	 * and {@code AnnotationConfigWebApplicationContext} during bootstrap in order to make
	 * any custom name generation strategy available to the underlying
	 * {@link ConfigurationClassPostProcessor}.
	 * @since 3.1.1
	 */
	public static final String CONFIGURATION_BEAN_NAME_GENERATOR =
			"org.springframework.context.annotation.internalConfigurationBeanNameGenerator";

	/**
	 * The bean name of the internally managed Autowired annotation processor.
	 */
	public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalAutowiredAnnotationProcessor";

	/**
	 * The bean name of the internally managed Required annotation processor.
	 * @deprecated as of 5.1, since no Required processor is registered by default anymore
	 */
	@Deprecated
	public static final String REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalRequiredAnnotationProcessor";

	/**
	 * The bean name of the internally managed JSR-250 annotation processor.
	 */
	public static final String COMMON_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalCommonAnnotationProcessor";

	/**
	 * The bean name of the internally managed JPA annotation processor.
	 */
	public static final String PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalPersistenceAnnotationProcessor";

	private static final String PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME =
			"org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor";

	/**
	 * The bean name of the internally managed @EventListener annotation processor.
	 */
	public static final String EVENT_LISTENER_PROCESSOR_BEAN_NAME =
			"org.springframework.context.event.internalEventListenerProcessor";

	/**
	 * The bean name of the internally managed EventListenerFactory.
	 */
	public static final String EVENT_LISTENER_FACTORY_BEAN_NAME =
			"org.springframework.context.event.internalEventListenerFactory";

	private static final boolean jsr250Present;

	private static final boolean jpaPresent;

	static {
		ClassLoader classLoader = AnnotationConfigUtils.class.getClassLoader();
		jsr250Present = ClassUtils.isPresent("javax.annotation.Resource", classLoader);
		jpaPresent = ClassUtils.isPresent("javax.persistence.EntityManagerFactory", classLoader) &&
				ClassUtils.isPresent(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, classLoader);
	}


	/**
	 * 注册了一系列的处理注解配置的BeanFactoryPostProcessor
	 *
	 * Register all relevant annotation post processors in the given registry.
	 * @param registry the registry to operate on
	 */
	public static void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
		registerAnnotationConfigProcessors(registry, null);
	}

	/**
	 * Register all relevant annotation post processors in the given registry.
	 * @param registry the registry to operate on
	 * @param source the configuration source element (already extracted)
	 * that this registration was triggered from. May be {@code null}.
	 * @return a Set of BeanDefinitionHolders, containing all bean definitions
	 * that have actually been registered by this call
	 */
	public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		DefaultListableBeanFactory beanFactory = unwrapDefaultListableBeanFactory(registry);
		if (beanFactory != null) {
			// AnnotationAwareOrderComparator主要能解析@Order注解和@Priority注解
			if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
				beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
			}
			if (!(beanFactory.getAutowireCandidateResolver() instanceof ContextAnnotationAutowireCandidateResolver)) {
				// ContextAnnotationAutowiredCandidateResolver提供处理延迟加载的功能
				beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
			}
		}

		Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);
		// BeanDefinition的注册，需要理解注册每个bean的类型
		// 1.注册ConfigurationClassPostProcessor
		// ConfigurationClassPostProcessor类的作用是：帮助beanFactory需要实现解析、扫描等等功，
		// 		是Spring在最开始在beanFactory初始化之前默认自己注册的，用来插手beanFactory的实例化过程
		// 在org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors方法中使用
		if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			// ConfigurationClassPostProcessor的类型是BeanDefinitionRegistryPostProcessor
			// 而BeanDefinitionRegistryPostProcessor继承了BeanFactoryPostProcessor这个接口
			// BeanDefinition接口有很多实现类，比如AnnotatedBeanDefinition用来描述被加了注解的bean(用户的)
			// RootBeanDefinition用来描述Spring内部提供的bean
			// 向BeanDefinitionMap中注册Bean：
			// 方式1.先通过BeanDefinitionReader读取类，再通过BeanDefinition的构造方法将类转换为BeanDefinition对象，然后注册到BeanDefinitionMap中；
			// 方式2.通过BeanDefinition的构造方法将bean转换为BeanDefinition对象，然后注册到BeanDefinitionMap中。
			RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class); // 方式2
			def.setSource(source);
			// 通过registerPostProcessor方法向BeanDefinitionMap中注册BeanDefinition,返回BeanDefinitionHolder
			// 将BeanDefinitionHolder放入Set集合中
			beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
		}
		 // 2.注册AutowiredAnnotationBeanPostProcessor
		/**
		 * BeanPostProcessor
		 * 处理@Autowired注解的BeanPostProcessor，为属性注入值
		 */
		if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			// AutowiredAnnotationBeanPostProcessor 实现了MergedBeanDefinitionPostProcessor接口
			// MergedBeanDefinitionPostProcessor 继承了 BeanPostProcessor接口
			RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		// 3.注册CommonAnnotationBeanPostProcessor
		/**
		 * BeanPostProcessor
		 * CommonAnnotationBeanPostProcessor：处理@PostConstruct和@PreDestroy
		 */
		// Check for JSR-250 support, and if present add the CommonAnnotationBeanPostProcessor.
		if (jsr250Present && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		// 4.注册PersistenceAnnotationBeanPostProcessor
		// Check for JPA support, and if present add the PersistenceAnnotationBeanPostProcessor.
		if (jpaPresent && !registry.containsBeanDefinition(PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition();
			try {
				def.setBeanClass(ClassUtils.forName(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME,
						AnnotationConfigUtils.class.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(
						"Cannot load optional framework class: " + PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, ex);
			}
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		// 5.注册EventListenerMethodProcessor
		if (!registry.containsBeanDefinition(EVENT_LISTENER_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(EventListenerMethodProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		}

		// 6.注册DefaultEventListenerFactory
		if (!registry.containsBeanDefinition(EVENT_LISTENER_FACTORY_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(DefaultEventListenerFactory.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_FACTORY_BEAN_NAME));
		}

		return beanDefs;
	}

	// 向BeanDefinitionMap中注册BeanDefinition
	private static BeanDefinitionHolder registerPostProcessor(
			BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName) {

		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 向BeanDefinitionMap中注册BeanDefinition
		registry.registerBeanDefinition(beanName, definition);
		// 返回BeanDefinitionHolder
		return new BeanDefinitionHolder(definition, beanName);
	}

	@Nullable
	private static DefaultListableBeanFactory unwrapDefaultListableBeanFactory(BeanDefinitionRegistry registry) {
		if (registry instanceof DefaultListableBeanFactory) {
			return (DefaultListableBeanFactory) registry;
		}
		else if (registry instanceof GenericApplicationContext) {
			return ((GenericApplicationContext) registry).getDefaultListableBeanFactory();
		}
		else {
			return null;
		}
	}

	// 检查常用的注解
	public static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd) {
		processCommonDefinitionAnnotations(abd, abd.getMetadata());
	}

	static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd, AnnotatedTypeMetadata metadata) {
		// 如果bean单独还有@Lazy注解，修改其BeanDefinition中对应的属性值
		AnnotationAttributes lazy = attributesFor(metadata, Lazy.class);
		if (lazy != null) {
			abd.setLazyInit(lazy.getBoolean("value"));
		}
		else if (abd.getMetadata() != metadata) {
			lazy = attributesFor(abd.getMetadata(), Lazy.class);
			if (lazy != null) {
				abd.setLazyInit(lazy.getBoolean("value"));
			}
		}

		// 处理@Primary注解
		if (metadata.isAnnotated(Primary.class.getName())) {
			abd.setPrimary(true);
		}

		// 如果bean单独还有@DependsOn注解，修改其BeanDefinition中对应的属性值
		AnnotationAttributes dependsOn = attributesFor(metadata, DependsOn.class);
		if (dependsOn != null) {
			abd.setDependsOn(dependsOn.getStringArray("value"));
		}

		// 处理@Role注解
		AnnotationAttributes role = attributesFor(metadata, Role.class);
		if (role != null) {
			abd.setRole(role.getNumber("value").intValue());
		}
		AnnotationAttributes description = attributesFor(metadata, Description.class);
		if (description != null) {
			abd.setDescription(description.getString("value"));
		}
	}

	static BeanDefinitionHolder applyScopedProxyMode(
			ScopeMetadata metadata, BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {

		ScopedProxyMode scopedProxyMode = metadata.getScopedProxyMode();
		if (scopedProxyMode.equals(ScopedProxyMode.NO)) {
			return definition;
		}
		boolean proxyTargetClass = scopedProxyMode.equals(ScopedProxyMode.TARGET_CLASS);
		return ScopedProxyCreator.createScopedProxy(definition, registry, proxyTargetClass);
	}

	@Nullable
	static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, Class<?> annotationClass) {
		return attributesFor(metadata, annotationClass.getName());
	}

	@Nullable
	static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, String annotationClassName) {
		return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annotationClassName, false));
	}

	static Set<AnnotationAttributes> attributesForRepeatable(AnnotationMetadata metadata,
			Class<?> containerClass, Class<?> annotationClass) {

		return attributesForRepeatable(metadata, containerClass.getName(), annotationClass.getName());
	}

	@SuppressWarnings("unchecked")
	static Set<AnnotationAttributes> attributesForRepeatable(
			AnnotationMetadata metadata, String containerClassName, String annotationClassName) {

		Set<AnnotationAttributes> result = new LinkedHashSet<>();

		// Direct annotation present?
		addAttributesIfNotNull(result, metadata.getAnnotationAttributes(annotationClassName, false));

		// Container annotation present?
		Map<String, Object> container = metadata.getAnnotationAttributes(containerClassName, false);
		if (container != null && container.containsKey("value")) {
			for (Map<String, Object> containedAttributes : (Map<String, Object>[]) container.get("value")) {
				addAttributesIfNotNull(result, containedAttributes);
			}
		}

		// Return merged result
		return Collections.unmodifiableSet(result);
	}

	private static void addAttributesIfNotNull(
			Set<AnnotationAttributes> result, @Nullable Map<String, Object> attributes) {

		if (attributes != null) {
			result.add(AnnotationAttributes.fromMap(attributes));
		}
	}

}
