/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;

/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>Registered by default when using {@code <context:annotation-config/>} or
 * {@code <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other BeanFactoryPostProcessor.
 *
 * <p>This post processor is priority-ordered as it is important that any
 * {@link Bean} methods declared in {@code @Configuration} classes have
 * their corresponding bean definitions registered before any other
 * {@link BeanFactoryPostProcessor} executes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
		PriorityOrdered, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

	private static final String IMPORT_REGISTRY_BEAN_NAME =
			ConfigurationClassPostProcessor.class.getName() + ".importRegistry";


	private final Log logger = LogFactory.getLog(getClass());

	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	@Nullable
	private Environment environment;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	private boolean setMetadataReaderFactoryCalled = false;

	private final Set<Integer> registriesPostProcessed = new HashSet<>();

	private final Set<Integer> factoriesPostProcessed = new HashSet<>();

	@Nullable
	private ConfigurationClassBeanDefinitionReader reader;

	private boolean localBeanNameGeneratorSet = false;

	/* Using short class names as default bean names */
	private BeanNameGenerator componentScanBeanNameGenerator = new AnnotationBeanNameGenerator();

	/* Using fully qualified class names as default bean names */
	private BeanNameGenerator importBeanNameGenerator = new AnnotationBeanNameGenerator() {
		@Override
		protected String buildDefaultBeanName(BeanDefinition definition) {
			String beanClassName = definition.getBeanClassName();
			Assert.state(beanClassName != null, "No bean class name set");
			return beanClassName;
		}
	};


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
	}

	/**
	 * Set the {@link SourceExtractor} to use for generated bean definitions
	 * that correspond to {@link Bean} factory methods.
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
	}

	/**
	 * Set the {@link ProblemReporter} to use.
	 * <p>Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, an @Bean method marked as {@code final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * Set the {@link MetadataReaderFactory} to use.
	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
	 * {@linkplain #setBeanClassLoader bean class loader}.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.setMetadataReaderFactoryCalled = true;
	}

	/**
	 * Set the {@link BeanNameGenerator} to be used when triggering component scanning
	 * from {@link Configuration} classes and when registering {@link Import}'ed
	 * configuration classes. The default is a standard {@link AnnotationBeanNameGenerator}
	 * for scanned components (compatible with the default in {@link ClassPathBeanDefinitionScanner})
	 * and a variant thereof for imported configuration classes (using unique fully-qualified
	 * class names instead of standard component overriding).
	 * <p>Note that this strategy does <em>not</em> apply to {@link Bean} methods.
	 * <p>This setter is typically only appropriate when configuring the post-processor as
	 * a standalone bean definition in XML, e.g. not using the dedicated
	 * {@code AnnotationConfig*} application contexts or the {@code
	 * <context:annotation-config>} element. Any bean name generator specified against
	 * the application context will take precedence over any value set here.
	 * @since 3.1.1
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
		this.localBeanNameGeneratorSet = true;
		this.componentScanBeanNameGenerator = beanNameGenerator;
		this.importBeanNameGenerator = beanNameGenerator;
	}

	@Override
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
		}
	}


	/**
	 * Derive further bean definitions from the configuration classes in the registry.
	 */
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		int registryId = System.identityHashCode(registry);
		if (this.registriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
		}
		if (this.factoriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + registry);
		}
		this.registriesPostProcessed.add(registryId);

		// postProcessBeanDefinitionRegistry()是空壳方法，实际调用了processConfigBeanDefinitions()方法
		processConfigBeanDefinitions(registry);
	}

	/**
	 * 1. 扫描包，向beanDefinitionMap中注册beanDefinition
	 * 2. 生成带有@Configuration配置类的的cglib代理增强类
	 * Prepare the Configuration classes for servicing bean requests at runtime
	 * by replacing them with CGLIB-enhanced subclasses.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		int factoryId = System.identityHashCode(beanFactory);
		if (this.factoriesPostProcessed.contains(factoryId)) { // 判断是否已经执行过了
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + beanFactory);
		}
		this.factoriesPostProcessed.add(factoryId);
		/**
		 * 处理配置类，完成bean的扫描，并注册beanDefinition
		 */
		if (!this.registriesPostProcessed.contains(factoryId)) {
			// BeanDefinitionRegistryPostProcessor hook apparently not supported...
			// Simply call processConfigurationClasses lazily at this point then.
			// 获取方法内部完成包扫描
			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		}

		/**
		 * 增强配置类，通过cglib生成代理对象
		 * 当配置类的configurationClass属性值为full时，会增强该配置类，在Spring中实例化的是生成的cglib代理对象
		 * 		  当configurationClass属性值为lite时，不会增强，在Spring中实例化的是原生对象
		 * jdk动态代理是基于接口，需要被代理对象实现接口，而我们的配置类不一定会实现接口
		 * cglib动态代理，是基于继承被代理对象实现的，不需要接口
		 */
		enhanceConfigurationClasses(beanFactory);

		/**
		 * 加入一个ImportAwareBeanPostProcessor类型的beanPostProcessor后置处理器
		 * 处理实现了ImportAware接口的bean，获取到某个类上的某些注解里的参数值，给属性赋值
		 */
		beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
	}

	/**
	 * Build and validate a configuration model based on the registry of
	 * {@link Configuration} classes.
	 */
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		// 存放BeanDefinitionHolder的容器
		List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
		// 获取容器中所有Bean定义(beanDefinition)的名字
		String[] candidateNames = registry.getBeanDefinitionNames();

		/**
		 * 构建Spring项目，需要配置文件或配置类
		 * 因为是通过AnnotationApplicationContext类构建Spring项目，所以至少有一个配置类
		 * 循环所有的bean，找出配置类并解析
		 * 需要解析的类是程序员自定义的类，即添加了注解的类，
		 * 配置类一定需要@Configuration注解，或者@Component注解，或者@ComponentScan注解，或者@Import注解，或者@ImportResorce注解之一(引入xml配置文件)
		 * 所以需要遍历所有的beanDefinition，来找到配置类，并解析配置类
		 */
		for (String beanName : candidateNames) { // 循环bean
			BeanDefinition beanDef = registry.getBeanDefinition(beanName); // 获取beanDefinition信息
			// 判断配置类是否已经被处理过
			// 需要结合下面代码才能理解
			// 如果BeanDefinition中的configurationClass属性值为full或lite，则意味着已经处理过了，会直接跳过
			if (ConfigurationClassUtils.isFullConfigurationClass(beanDef) ||
					ConfigurationClassUtils.isLiteConfigurationClass(beanDef)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
				}
			}
			/**
			 * 判断类的BeanDefinition中是否包含是否添加了注解，分为两种
			 * 	1. 添加了@Configuration的配置类，设置configurationClass属性值为full
			 * 	2. 添加了@Component、@ComponentScan、@Import、@ImportResource四个注解的配置类，设置configurationClass属性值为lite
			 * 配置类为full和lite的区别：
			 * 	full：spring在实例化这个类时会对这个类进行cglib增强，在Spring容器中放入一个代理对象
			 * 	lite：spring在实例化这个类时不会对这个类进行cglib增强，在Spring容器中放入一个原生对象
			 */
			else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				// 放入BeanDefinitionHolder容器中
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		if (configCandidates.isEmpty()) {
			return;
		}

		// Sort by previously determined @Order value, if applicable
		// 根据@Order注解，进行排序
		configCandidates.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});

		// Detect any custom bean name generation strategy supplied through the enclosing application context
		SingletonBeanRegistry sbr = null;
		/**
		 * 如果BeanDefinitionRegistry是SingletonBeanRegistry的子类的话，会将其转换为SingletonBeanRegistry
		 */
		if (registry instanceof SingletonBeanRegistry) {
			sbr = (SingletonBeanRegistry) registry;
			if (!this.localBeanNameGeneratorSet) { // 是否有自定义的
				BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
				if (generator != null) {
					// SingletonBeanRegistry中有id为org.springframework.context.annotation.internalConfigurationAnnotationProcessor
					// 有提供自定义的就使用它，没有就用spring默认的
					this.componentScanBeanNameGenerator = generator;
					this.importBeanNameGenerator = generator;
				}
			}
		}

		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

		// Parse each @Configuration class
		// 实例化ConfigurationClassParser：为了解析各个配置类
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);

		/**
		 * 为了给配置类去重，准备两个Set集合
		 * candidates用于将之前加入的configCandidates进行去重，多个配置类可能重复
		 * 去重的原因是，我们可以通过ApplicationContext的register()方法向Spring容器中多次注册类，可能会重复注册类
		 * alreadyParsed用于判断是否已经处理过
		 */
		Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates); // 将list放入set中，即可去重
		Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size()); // 容量为指定值的集合，已经解析了多少个配置类
		do {
			/**
			 * 解析所有配置类，完成包扫描
			 */
			parser.parse(candidates);
			// 验证扫描出来的类是否可用
			parser.validate();

			// 获取到configurationClasses中存放的所有的配置类(@Configuration、@Component、@ComponentScan、@Import、@ImportResource)
			Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
			// 去掉已经解析过的数量
			configClasses.removeAll(alreadyParsed);

			// Read the model and create bean definitions based on its content
			if (this.reader == null) {
				this.reader = new ConfigurationClassBeanDefinitionReader(
						registry, this.sourceExtractor, this.resourceLoader, this.environment,
						this.importBeanNameGenerator, parser.getImportRegistry());
			}

			// 把扫描出来的bean对应的beanDefinition添加到beanFactory当中的beanDefinitionMap中
			// 配置类就是所有需要扫描并交给Spring管理的类，即有@Configuration注解、@Component注解、@Import注解、@ComponentScan注解、@ImportResource注解的类，还有被@Import引入的没有注解的类
			// 在解析过程中，有些配置类可能已经被注册过了，所以在loadBeanDefinitions()方法内部会进行判断
			/**
			 * 	初始化Spring时@Configuration注解标注的类，会先被注册进来。而包扫描时，可能还会将它扫描进来，出现重复
			 * 	有些类被扫描进来后，可能还会通过register()方法再次注册进来，也需要去重
			 * 	通过@Import注解引入的类，也可能会被扫描进来，也需要去重
			 */
			this.reader.loadBeanDefinitions(configClasses);
			alreadyParsed.addAll(configClasses); // 将已经解析完毕的所有类，放入集合中

			candidates.clear();
			// 把扫描出来的beanDefinition注册给beanFactory
			if (registry.getBeanDefinitionCount() > candidateNames.length) {
				String[] newCandidateNames = registry.getBeanDefinitionNames();
				Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
				Set<String> alreadyParsedClasses = new HashSet<>();
				for (ConfigurationClass configurationClass : alreadyParsed) {
					alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
				}
				for (String candidateName : newCandidateNames) {
					if (!oldCandidateNames.contains(candidateName)) {
						BeanDefinition bd = registry.getBeanDefinition(candidateName);
						/**
						 * 通过checkConfigurationClassCandidate()方法，检查候选类是否是配置类
						 */
						if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
								!alreadyParsedClasses.contains(bd.getBeanClassName())) {
							candidates.add(new BeanDefinitionHolder(bd, candidateName));
						}
					}
				}
				candidateNames = newCandidateNames;
			}
		}
		while (!candidates.isEmpty());

		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
		if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
			sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
		}

		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
			// for a shared cache since it'll be cleared by the ApplicationContext.
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}

	/**
	 * cglib增强beanFactory
	 *
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
	 * Candidate status is determined by BeanDefinition attribute metadata.
	 * @see ConfigurationClassEnhancer
	 */
	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			/**
			 * 判断是否是一个全注解类，即配置类的configurationClass属性值是full还是lite
			 * full是全配置类，lite不是全配置类
			 * full就生成cglib代理对象，lite就生成原对象
			 */
			if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {
				if (!(beanDef instanceof AbstractBeanDefinition)) {
					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
				}
				else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
					logger.info("Cannot enhance @Configuration bean definition '" + beanName +
							"' since its singleton instance has been created too early. The typical cause " +
							"is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
							"return type: Consider declaring such methods as 'static'.");
				}
				// 是full全配置类，放入map中
				configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
			}
		}
		if (configBeanDefs.isEmpty()) {
			// nothing to enhance -> return immediately
			return;
		}

		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			// 获取full配置类的beanDefinition
			AbstractBeanDefinition beanDef = entry.getValue();
			// If a @Configuration class gets proxied, always proxy the target class
			beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			try {
				// Set enhanced subclass of the user-specified bean class
				Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
				if (configClass != null) {
					/**
					 * enhance()方法完成对full配置类的cglib代理，得到class对象
					 */
					Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
					if (configClass != enhancedClass) {
						if (logger.isTraceEnabled()) {
							logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
									"enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
						}
						// 设置full配置类的Class类型为代理类的Class
						beanDef.setBeanClass(enhancedClass);
					}
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
			}
		}
	}


	private static class ImportAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

		private final BeanFactory beanFactory;

		public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public PropertyValues postProcessProperties(@Nullable PropertyValues pvs, Object bean, String beanName) {
			// Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
			// postProcessProperties method attempts to autowire other configuration beans.
			if (bean instanceof EnhancedConfiguration) {
				((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);
			}
			return pvs;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			if (bean instanceof ImportAware) {
				ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
				AnnotationMetadata importingClass = ir.getImportingClassFor(bean.getClass().getSuperclass().getName());
				if (importingClass != null) {
					((ImportAware) bean).setImportMetadata(importingClass);
				}
			}
			return bean;
		}
	}

}
