/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		//已经处理过的bean的名字(处理指的不是调用,而是加入到这个Set集合里)
		Set<String> processedBeans = new HashSet<>();

		//注意
		//方法上的 beanFactoryPostProcessors 这个参数是指"用户通过 AnnotationConfigApplicationContext.addBeanFactoryPostProcessor() 方法手动传入的 BeanFactoryPostProcessor，没有交给 spring 管理

		//先处理方法参数中的BeanFactoryPostProcessor,也就是用户手动添加的BeanFactoryPostProcessor
		//处理完了这些,再处理spring容器里的BeanFactoryPostProcessor

		//如果beanFactory实现了BeanDefinitionRegistry接口
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// 常规后置处理器集合，仅实现 BeanFactoryPostProcessor 接口
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 注册后置处理器集合，即实现了 BeanDefinitionRegistryPostProcessor 接口
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			// 处理手动添加的 beanFactoryPostProcessors，一般情况下都不会手动添加
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				//实现了BeanDefinitionRegistryPostProcessor接口
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					//调用postProcessBeanDefinitionRegistry()方法(将当前的bean注册器传入)
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					//加入到注册后置处理器集合中
					registryProcessors.add(registryProcessor);
				}
				else {
					//没有实现BeanDefinitionRegistryPostProcessor接口,加入到常规集合里
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.

			//临时存储当前正在处理的BeanDefinitionRegistryPostProcessor,下面会反复用到
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			//从Spring容器中获取BeanDefinitionRegistryPostProcessor的名字(目前只能找到ConfigurationClassPostProcessor,在实例化上下文的时候由Spring手动注册的)
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//遍历所有的BeanDefinitionRegistryPostProcessor
			for (String ppName : postProcessorNames) {
				//如果实现了@PriorityOrdered接口
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//根据名字创建bean对象,添加到临时的集合里
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//把名字添加到已经处理过的bean的名字集合里,表示已经处理过这个类了
					processedBeans.add(ppName);
				}
			}
			//根据@PriorityOrdered上的value值排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//添加到注册后置处理器集合
			registryProcessors.addAll(currentRegistryProcessors);
			//调用postProcessBeanDefinitionRegistry()方法(将当前的bean注册器传入)
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			//清空临时集合
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 接下来，从容器中查找实现了 Ordered 接口的 BeanDefinitionRegistryPostProcessors 的bean的名字，这里可能会查找出多个
			// 因为【ConfigurationClassPostProcessor】已经完成了 postProcessBeanDefinitionRegistry() 方法，已经向容器中完成扫描工作，所以容器会有很多个组件
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//遍历
			for (String ppName : postProcessorNames) {
				//如果不在已处理的集合里(表示这个bean还未处理) 且 实现了 @Ordered 接口
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					//创建bean对象,并加入到临时集合里
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//把名字添加到已经处理过的bean的名字集合里,表示已经处理过这个类了
					processedBeans.add(ppName);
				}
			}
			//根据@Ordered上的value值排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//添加到注册后置处理器集合
			registryProcessors.addAll(currentRegistryProcessors);
			//调用postProcessBeanDefinitionRegistry()方法(将当前的bean注册器传入)
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			//清空临时集合
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			//最后，从Spring容器中查找剩余所有常规的 BeanDefinitionRegistryPostProcessors 类型
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				//查找所有的BeanDefinitionRegistryPostProcessor
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				//遍历
				for (String ppName : postProcessorNames) {
					//如果没处理过
					if (!processedBeans.contains(ppName)) {
						//创建bean对象,并加入到临时集合里
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						//把名字添加到已经处理过的bean的名字集合里,表示已经处理过这个类了
						processedBeans.add(ppName);
						//标记为重新遍历,防止下面调用了 invokeBeanDefinitionRegistryPostProcessors() 方法引入新的后置处理器
						reiterate = true;
					}
				}
				//排序(其实这里的currentRegistryProcessors既没有@PriorityOrdered,也没有@Ordered,所以都其实没啥意义,都会设置为最低优先级来排序)
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				//添加到注册后置处理器集合
				registryProcessors.addAll(currentRegistryProcessors);
				//调用postProcessBeanDefinitionRegistry()方法(将当前的bean注册器传入)
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				//清空临时集合
				currentRegistryProcessors.clear();
			}

			//目前
			//手动传入的BeanDefinitionRegistryPostProcessor
			//实现了@PriorityOrdered接口的BeanDefinitionRegistryPostProcessor
			//实现了@Order接口的BeanDefinitionRegistryPostProcessor
			//剩下的BeanDefinitionRegistryPostProcessor
			//都调用了调用postProcessBeanDefinitionRegistry()方法

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			//因为BeanDefinitionRegistryPostProcessor 继承了 BeanFactoryPostProcessor 接口,所以同时重写了这2个接口的方法,上面调用了一个,这里统一调用另一个
			//现在执行 registryProcessors 的 postProcessBeanFactory() 回调方法(这里registryProcessors里已经是排好序的了)
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			//执行 regularPostProcessors 的 postProcessBeanFactory() 回调方法(这里regularPostProcessors里只有用户手动调用 addBeanFactoryPostProcessor() 方法添加的 BeanFactoryPostProcessor)
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			//如果beanFactory没实现了BeanDefinitionRegistry接口,那就直接处理手动入参的 BeanFactoryPostProcessor
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		//目前
		//手动调用 addBeanFactoryPostProcessor() 方法添加的 BeanFactoryPostProcessor
		//Spring容器里实现了BeanDefinitionRegistryPostProcessor接口的BeanFactoryPostProcessor
		//相关的回调方法都已经执行

		//接下来,执行仅实现BeanFactoryPostProcessor接口的 BeanFactoryPostProcessor的回调方法

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		//从Spring容器中查找实现了 BeanFactoryPostProcessor 接口的类
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 表示实现了 PriorityOrdered 接口的 BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 表示实现了 Ordered 接口的 BeanFactoryPostProcessor
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 表示剩下来的常规的 BeanFactoryPostProcessors
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		//遍历
		for (String ppName : postProcessorNames) {
			//判断是否已经处理过，因为 postProcessorNames 其实包含了上面步骤处理过的 BeanDefinitionRegistry 类型
			//如果处理过,直接跳过,进入下一个循环
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			//如果实现了 PriorityOrdered 接口,创建bean并加入到priorityOrderedPostProcessors集合中
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			//如果实现了 Ordered 接口, 加入到orderedPostProcessorNames集合中
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			//剩下的,也就是未排序的
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		//根据@PriorityOrdered进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		//执行 priorityOrderedPostProcessors 的 postProcessBeanFactory() 回调方法(将当前bean工厂传入)
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		//
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		//根据@Ordered进行排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		//执行 orderedPostProcessors 的 postProcessBeanFactory() 回调方法(将当前bean工厂传入)
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		//最后把 nonOrderedPostProcessorNames 转成 nonOrderedPostProcessors 集合，这里只有一个，myBeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 执行 nonOrderedPostProcessors 的 postProcessBeanFactory() 回调方法(将当前bean工厂传入)
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		//清除缓存的合并的bean定义,因为后置处理器可能修改了原始的元数据,比如在值中替换占位符
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		//获取Spring容器中所有的BeanPostProcessor的名字
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		//注册一个 BeanPostProcessorChecker 的实例,用于记录是否有bean已经初始化完成了(根据bean的数量来判断)
		//如果有,就会记录一个info日志
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		//分离出来的BeanPostProcessor, 其中包含了@PriorityOrdered, @Ordered, 和其他的BeanPostProcessor

		//实现了@PriorityOrdered接口的BeanPostProcessor集合
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		//实现了MergedBeanDefinitionPostProcessor接口的BeanPostProcessor集合
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		//实现了@Ordered接口的BeanPostProcessor名称的集合
		List<String> orderedPostProcessorNames = new ArrayList<>();
		//存放剩下来普通的 BeanPostProcessor 的名称的集合
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		//遍历所有的BeanPostProcessor的名字
		for (String ppName : postProcessorNames) {
			//如果是PriorityOrdered的BeanPostProcessor,先创建bean对象,放入 priorityOrderedPostProcessors 集合中
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				//同时如果实现了MergedBeanDefinitionPostProcessor接口,放入 internalPostProcessors 集合中
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			//如果是Ordered的BeanPostProcessor,把bean的名字放入 orderedPostProcessorNames 集合中
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			//剩下的普通的BeanPostProcessor,把bean的名字放入 nonOrderedPostProcessorNames 集合中
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		//对priorityOrderedPostProcessors进行排序,按照@PriorityOrdered的order属性进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		//按照顺序注册priorityOrderedPostProcessors中的BeanPostProcessor(这里仅仅是注册,也就是放入缓存里,并没有调用)
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		//对实现了@Order接口的BeanPostProcessor也是一样的逻辑
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
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
		//对普通的BeanPostProcessor(也就是未排序的)也是一样的逻辑
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		//对实现了MergedBeanDefinitionPostProcessor接口的BeanPostProcessor也是一样的逻辑,排序后再注册
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		//向Spring容器中添加【ApplicationListenerDetector】 beanPostProcessor
		//作用是判断Bean是不是监听器，如果是就把 bean 添加的上下文的监听器集合中
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
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
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanFactory(beanFactory);
			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		}
		else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}


	/**
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
