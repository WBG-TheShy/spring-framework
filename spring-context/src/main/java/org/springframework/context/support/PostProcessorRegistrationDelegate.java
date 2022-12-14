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
		//??????????????????bean?????????(????????????????????????,?????????????????????Set?????????)
		Set<String> processedBeans = new HashSet<>();

		//??????
		//???????????? beanFactoryPostProcessors ??????????????????"???????????? AnnotationConfigApplicationContext.addBeanFactoryPostProcessor() ????????????????????? BeanFactoryPostProcessor??????????????? spring ??????

		//???????????????????????????BeanFactoryPostProcessor,??????????????????????????????BeanFactoryPostProcessor
		//??????????????????,?????????spring????????????BeanFactoryPostProcessor

		//??????beanFactory?????????BeanDefinitionRegistry??????
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// ??????????????????????????????????????? BeanFactoryPostProcessor ??????
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// ?????????????????????????????????????????? BeanDefinitionRegistryPostProcessor ??????
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			// ????????????????????? beanFactoryPostProcessors???????????????????????????????????????
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				//?????????BeanDefinitionRegistryPostProcessor??????
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					//??????postProcessBeanDefinitionRegistry()??????(????????????bean???????????????)
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					//???????????????????????????????????????
					registryProcessors.add(registryProcessor);
				}
				else {
					//????????????BeanDefinitionRegistryPostProcessor??????,????????????????????????
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.

			//?????????????????????????????????BeanDefinitionRegistryPostProcessor,?????????????????????
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			//???Spring???????????????BeanDefinitionRegistryPostProcessor?????????(??????????????????ConfigurationClassPostProcessor,?????????????????????????????????Spring???????????????)
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//???????????????BeanDefinitionRegistryPostProcessor
			for (String ppName : postProcessorNames) {
				//???????????????@PriorityOrdered??????
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//??????????????????bean??????,???????????????????????????
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//????????????????????????????????????bean??????????????????,?????????????????????????????????
					processedBeans.add(ppName);
				}
			}
			//??????@PriorityOrdered??????value?????????
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//????????????????????????????????????
			registryProcessors.addAll(currentRegistryProcessors);
			//??????postProcessBeanDefinitionRegistry()??????(????????????bean???????????????)
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			//??????????????????
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// ??????????????????????????????????????? Ordered ????????? BeanDefinitionRegistryPostProcessors ???bean??????????????????????????????????????????
			// ?????????ConfigurationClassPostProcessor?????????????????? postProcessBeanDefinitionRegistry() ?????????????????????????????????????????????????????????????????????????????????
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//??????
			for (String ppName : postProcessorNames) {
				//?????????????????????????????????(????????????bean????????????) ??? ????????? @Ordered ??????
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					//??????bean??????,???????????????????????????
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//????????????????????????????????????bean??????????????????,?????????????????????????????????
					processedBeans.add(ppName);
				}
			}
			//??????@Ordered??????value?????????
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//????????????????????????????????????
			registryProcessors.addAll(currentRegistryProcessors);
			//??????postProcessBeanDefinitionRegistry()??????(????????????bean???????????????)
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			//??????????????????
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			//????????????Spring???????????????????????????????????? BeanDefinitionRegistryPostProcessors ??????
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				//???????????????BeanDefinitionRegistryPostProcessor
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				//??????
				for (String ppName : postProcessorNames) {
					//??????????????????
					if (!processedBeans.contains(ppName)) {
						//??????bean??????,???????????????????????????
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						//????????????????????????????????????bean??????????????????,?????????????????????????????????
						processedBeans.add(ppName);
						//?????????????????????,????????????????????? invokeBeanDefinitionRegistryPostProcessors() ?????????????????????????????????
						reiterate = true;
					}
				}
				//??????(???????????????currentRegistryProcessors?????????@PriorityOrdered,?????????@Ordered,???????????????????????????,???????????????????????????????????????)
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				//????????????????????????????????????
				registryProcessors.addAll(currentRegistryProcessors);
				//??????postProcessBeanDefinitionRegistry()??????(????????????bean???????????????)
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				//??????????????????
				currentRegistryProcessors.clear();
			}

			//??????
			//???????????????BeanDefinitionRegistryPostProcessor
			//?????????@PriorityOrdered?????????BeanDefinitionRegistryPostProcessor
			//?????????@Order?????????BeanDefinitionRegistryPostProcessor
			//?????????BeanDefinitionRegistryPostProcessor
			//??????????????????postProcessBeanDefinitionRegistry()??????

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			//??????BeanDefinitionRegistryPostProcessor ????????? BeanFactoryPostProcessor ??????,????????????????????????2??????????????????,?????????????????????,???????????????????????????
			//???????????? registryProcessors ??? postProcessBeanFactory() ????????????(??????registryProcessors???????????????????????????)
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			//?????? regularPostProcessors ??? postProcessBeanFactory() ????????????(??????regularPostProcessors??????????????????????????? addBeanFactoryPostProcessor() ??????????????? BeanFactoryPostProcessor)
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			//??????beanFactory????????????BeanDefinitionRegistry??????,????????????????????????????????? BeanFactoryPostProcessor
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		//??????
		//???????????? addBeanFactoryPostProcessor() ??????????????? BeanFactoryPostProcessor
		//Spring??????????????????BeanDefinitionRegistryPostProcessor?????????BeanFactoryPostProcessor
		//????????????????????????????????????

		//?????????,???????????????BeanFactoryPostProcessor????????? BeanFactoryPostProcessor???????????????

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		//???Spring???????????????????????? BeanFactoryPostProcessor ????????????
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// ??????????????? PriorityOrdered ????????? BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// ??????????????? Ordered ????????? BeanFactoryPostProcessor
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// ??????????????????????????? BeanFactoryPostProcessors
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		//??????
		for (String ppName : postProcessorNames) {
			//???????????????????????????????????? postProcessorNames ??????????????????????????????????????? BeanDefinitionRegistry ??????
			//???????????????,????????????,?????????????????????
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			//??????????????? PriorityOrdered ??????,??????bean????????????priorityOrderedPostProcessors?????????
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			//??????????????? Ordered ??????, ?????????orderedPostProcessorNames?????????
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			//?????????,?????????????????????
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		//??????@PriorityOrdered????????????
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		//?????? priorityOrderedPostProcessors ??? postProcessBeanFactory() ????????????(?????????bean????????????)
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		//
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		//??????@Ordered????????????
		sortPostProcessors(orderedPostProcessors, beanFactory);
		//?????? orderedPostProcessors ??? postProcessBeanFactory() ????????????(?????????bean????????????)
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		//????????? nonOrderedPostProcessorNames ?????? nonOrderedPostProcessors ??????????????????????????????myBeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// ?????? nonOrderedPostProcessors ??? postProcessBeanFactory() ????????????(?????????bean????????????)
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		//????????????????????????bean??????,??????????????????????????????????????????????????????,??????????????????????????????
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

		//??????Spring??????????????????BeanPostProcessor?????????
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		//???????????? BeanPostProcessorChecker ?????????,?????????????????????bean????????????????????????(??????bean??????????????????)
		//?????????,??????????????????info??????
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		//???????????????BeanPostProcessor, ???????????????@PriorityOrdered, @Ordered, ????????????BeanPostProcessor

		//?????????@PriorityOrdered?????????BeanPostProcessor??????
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		//?????????MergedBeanDefinitionPostProcessor?????????BeanPostProcessor??????
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		//?????????@Ordered?????????BeanPostProcessor???????????????
		List<String> orderedPostProcessorNames = new ArrayList<>();
		//???????????????????????? BeanPostProcessor ??????????????????
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		//???????????????BeanPostProcessor?????????
		for (String ppName : postProcessorNames) {
			//?????????PriorityOrdered???BeanPostProcessor,?????????bean??????,?????? priorityOrderedPostProcessors ?????????
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				//?????????????????????MergedBeanDefinitionPostProcessor??????,?????? internalPostProcessors ?????????
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			//?????????Ordered???BeanPostProcessor,???bean??????????????? orderedPostProcessorNames ?????????
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			//??????????????????BeanPostProcessor,???bean??????????????? nonOrderedPostProcessorNames ?????????
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		//???priorityOrderedPostProcessors????????????,??????@PriorityOrdered???order??????????????????
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		//??????????????????priorityOrderedPostProcessors??????BeanPostProcessor(?????????????????????,????????????????????????,???????????????)
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		//????????????@Order?????????BeanPostProcessor?????????????????????
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
		//????????????BeanPostProcessor(?????????????????????)?????????????????????
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
		//????????????MergedBeanDefinitionPostProcessor?????????BeanPostProcessor?????????????????????,??????????????????
		//???????????????????????????:??????????????????MergedBeanDefinitionPostProcessor??????,??????????????????@PriorityOrdered????????????@Ordered??????,????????????????????????.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		//???Spring??????????????????ApplicationListenerDetector??? beanPostProcessor
		//???????????????Bean???????????????????????????????????? bean ???????????????????????????????????????
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
