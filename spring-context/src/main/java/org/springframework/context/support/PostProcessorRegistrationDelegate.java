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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}

	/**
	 * 回调Spring中实现了BeanFactoryPostProcessor或BeanRegistryPostProcessor接口的类，用来完成beanFactory的初始化
	 * 这样做可以解耦，方便扩展
	 * 这些类可以是Spring内部自己实现的，也可以是程序员自己实现并注册到Spring容器中的
	 * Spring会通过不同的List集合去存放这些类，并在合适的地方进行回调
	 * 例如，实现了BeanRegistryPostProcessor接口的ConfigurationClassPostProcessor类，就是Spring内部的，它使beanFactory实现了解析和扫描注解的功能
	 * @param beanFactory
	 * @param beanFactoryPostProcessors
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		// 如果有BeanDefinitionRegistryPostProcessor的话，先处理它内部方法的回调
		// 因为实现了BeanDefinitionRegistryPostProcessor接口的类，可以注册新的类到容器中，而
		// 实现了BeanFactoryPostProcessor接口的类则不能注册新的类到容器中
		Set<String> processedBeans = new HashSet<>();

		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			/**
			 * 1. BeanDefinitionRegistryPostProcessor接口继承了了BeanFactoryPostProcessor接口，所以本质上和BeanFactoryPostProcessor是一样
			 * 	  但BeanDefinitionRegistryPostProcessor是BeanFactoryPostProcessor的子类，它扩展了BeanFactoryPostProcessor，
			 * 	  增加了postProcessBeanDefinitionRegistry()方法，所以需要进行不同的处理
			 * 2. List<BeanFactoryPostProcessor> 用来存放程序员自定义的实现了BeanFactoryPostProcessor接口的类
			 *    List<BeanDefinitionRegistryPostProcessor> 用来存放程序员自己定义的实现了BeanDefinitionRegistryPostProcessor接口的类
			 */
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>(); // 存放自定义的实现了BeanFactoryPostProcessor接口的类
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>(); // 存放自定义的实现了BeanDefinitionRegistryPostProcessor接口的类

			// 处理自定义的BeanFactoryPostProcessor和BeanDefinitionRegistryPostProcessor
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) { // 实现了BeanDefinitionRegistryPostProcessor接口的类
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry); // 注册BeanDefinition
					registryProcessors.add(registryProcessor);
				}
				else { // 实现了BeanFactoryPostProcessor接口的类
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			/**
			 * List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors，存放Spring自己内部实现了BeanDefinitionRegistryPostProcessor接口的类
			 */
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			/**
			 * getBeanNamesForType()方法，顾名思义，就是在beanFactory中的BeanDefinitionMap中通过bean类型去获取到beanName
			 * BeanDefinitionRegistryPostProcessor继承了BeanFactoryPostProcessor
			 */
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			/**
			 * 1. 这里可以得到一个BeanDefinitionRegistryPostProcessor的实现类，是Spring在最开始在beanFactory初始化之前默认自己注册的，
			 * 2. 因为Spring的beanFactory需要实现解析、扫描等等功能。例如Spring一开始就注册了BeanDefinitionRegistryPostProcessor(即BeanFactoryPostProcessor)，
			 * 		用来插手beanFactory的实例化过程，这个BeanDefinitionRegistryPostProcessor的实现类是ConfigurationClassPostProcessor
			 * 3. 在当前invokeBeanFactoryPostProcessors()方法中，Spring将其它工作委托给实现了BeanDefinitionRegistryPostProcessor接口的类去做，再回调这些类，就实现了解耦
			 */
			// 首先，优先处理实现了PriorityOrdered接口的BeanDefinitionRegistryPostProcessor
			for (String ppName : postProcessorNames) {
				// Spring内部提供ConfigurationClassPostProcessor，就同时实现了BeanDefinitionRegistryPostProcessor接口和PriorityOrdered接口
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 下面会处理这个BeanDefinitionRegistryPostProcessor，放入已处理过的容器中，
					// 在处理BeanFactoryPostProcessor时会用来判断是否是BeanDefinitionRegistryPostProcessor类型
					processedBeans.add(ppName);
				}
			}

			// 排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 将程序员自定义的和Spring内部定义的BeanDefinitionRegistryPostProcessor实现类，放到一起
			// 根据Spring的要求，先处理BeanDefinitionRegistryPostProcessor
			registryProcessors.addAll(currentRegistryProcessors); // registryProcessors在此之前存放的是程序员自定义的BeanDefinitionRegistryPostProcessor
			/**
			 * 进入该方法内部，发现这里只是方法调用，循环所有的BeanDefinitionRegistryPostProcessor，并调用内部的扩展
			 * 方法postProcessBeanDefinitionRegistry()。
			 * 一定会执行Spring自己提供的ConfigurationClassPostProcessor类的postProcessBeanDefinitionRegistry()方法，该方法会
			 * 完成包扫描的工作并注册beanDefinition。
			 * 完成包扫描后，会引入所有加了@Component注解的类，也就会引入新的@Component注解标注的实现了BeanFactoryPostProcessor接口
			 * 或BeanDefinitionRegistryPostProcessor接口的类。
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 清空currentRegistryProcessors，准备用来存放新加入的BeanDefinitionRegistryPostProcessor
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			/**
			 * 判断完成包扫描后，是否有新的BeanDefinitionRegistryPostProcessor增加进来
			 * 然后先执行实现了Ordered的BeanDefinitionRegistryPostProcessor类
 			 */
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					// 将新的实现了Ordered接口的BeanDefinitionRegistryPostProcessor类，放入currentRegistryProcessors集合中
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 合并list
			registryProcessors.addAll(currentRegistryProcessors); // 将新的BeanPostProcessor放入registryProcessors集合中

			// 执行新增加进来的BeanDefinitionRegistryPostProcessor的内部方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 清空，再次存放新的BeanDefinitionRegistryPostProcessor
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 最后处理其他所有的BeanDefinitionRegistryPostProcessor类
			// 再次判断，循环判断是否新增了BeanDefinitionRegistryPostProcessor，如果有就执行所有的BeanDefinitionRegistryPostProcessor
			// 内部的方法，并接着判断是否还有新的BeanDefinitionRegistryPostProcessor引入，有的话再次执行其内部方法，直到没有新的
			// BeanDefinitionRegistryPostProcessor引入。
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						// 有新的BeanDefinitionRegistryPostProcessor引入，设置为true，继续循环
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				// 执行BeanDefinitionRegistryPostProcessor的回调
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 前面执行的是BeanFactoryPostProcessor的子类BeanDefinitionRegistryPostProcessor的回调
			// 到这里所有的BeanDefinitionRegistryPostProcessor都已经被执行了
			/**
			 * 执行所有的已处理的BeanDefinitionRegistryPostProcessor的回调
			 * 注意，这里传入的是工厂，而不是registry，虽然工厂实现了BeanDefinitionRegistry接口，本质也是registry，
			 * 但是这里执行的不是同一个方法(重载)，所以不会重复执行方法
			 */
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// 执行所有已经引入的自定义的BeanPostProcessor的回调
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}
		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		// 获取所有的BeanFactoryPostProcessor
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		// 循环BeanFactoryPostProcessor集合，跳过已经执行的BeanDefinitionRegistryPostProcessor，
		// 并判断类型，放入不同的容器中按顺序处理
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) { // 如果BeanFactoryPostProcessor已经被处理过了，包括BeanDefinitionRegistryPostProcessor
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		// 首先执行实现了PriorityOrdered接口的BeanFactoryPostProcessor
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		// 然后执行实现了Ordered接口的BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		// 最后执行其它类型的BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	// 注册后置处理器
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// 从beanDefinitionMap中得到所有的BeanPostProcessor
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		// BeanPostProcessorChecker：检查bean实例化时是否执行它的beanPostProcessor后置处理器
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * 回调BeanDefinitionRegistryPostProcessor中的postProcessorBeanDefinitionRegistry()方法
	 * BeanDefinitionRegistryPostProcessor的实现类是ConfigurationClassPostProcessor
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {
		// BeanDefinitionRegistryPostProcessor类只有一个，实现类是ConfigurationClassPostProcessor
		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			// 调用方法，内部会进行包扫描
			// 实现类是ConfigurationClassPostProcessor
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * 检查bean创建过程中有没有执行它的BeanPostProcessor后置处理器，如果没有执行，就会打印信息。
	 * 当Spring的配置中的后置处理器还没有被注册就已经开始了bean的初始化，便会
	 * 打印出BeanPostProcessorChecker中设定的信息
	 *
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
