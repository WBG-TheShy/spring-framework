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

package org.springframework.transaction.annotation;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.util.ClassUtils;

/**
 * Selects which implementation of {@link AbstractTransactionManagementConfiguration}
 * should be used based on the value of {@link EnableTransactionManagement#mode} on the
 * importing {@code @Configuration} class.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see EnableTransactionManagement
 * @see ProxyTransactionManagementConfiguration
 * @see TransactionManagementConfigUtils#TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME
 * @see TransactionManagementConfigUtils#JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME
 */
public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {

	/**
	 * Returns {@link ProxyTransactionManagementConfiguration} or
	 * {@code AspectJ(Jta)TransactionManagementConfiguration} for {@code PROXY}
	 * and {@code ASPECTJ} values of {@link EnableTransactionManagement#mode()},
	 * respectively.
	 */
	@Override
	protected String[] selectImports(AdviceMode adviceMode) {
		//Spring不会直接执行这个selectImports()方法,执行的是父类的selectImports(),因为父类的方法入参类型是AnnotationMetadata
		switch (adviceMode) {
			case PROXY:
				//默认是PROXY

				//所以这个时候Spring容器中就会有这两个bean
				//1.AutoProxyRegistrar
					//会注册一个InfrastructureAdvisorAutoProxyCreator(它的父类是AbstractAdvisorAutoProxyCreator)的bean,
					//注册这个bean为了支持AOP,因为Spring事务的实现要用AOP代理

					//这个和学习Spring AOP的时候开启的@EnableAspectJAutoProxy是不一样的
					//@EnableAspectJAutoProxy注册的是AnnotationAwareAspectJAutoProxyCreator
					//它的父类也是AbstractAdvisorAutoProxyCreator
					//但是它额外可以解析@Aspect,@Before等等AspectJ相关的注解

					//而InfrastructureAdvisorAutoProxyCreator只能解析Advisor对象,并不会解析切面bean
				//2.ProxyTransactionManagementConfiguration
					//往Spring容器扔了3个bean
					//1.BeanFactoryTransactionAttributeSourceAdvisor
					//2.TransactionAttributeSource
					//3.TransactionInterceptor
				return new String[] {AutoProxyRegistrar.class.getName(),
						ProxyTransactionManagementConfiguration.class.getName()};
			case ASPECTJ:
				//表示不用动态代理技术,用AspectJ技术,会比较麻烦
				return new String[] {determineTransactionAspectClass()};
			default:
				return null;
		}
	}

	private String determineTransactionAspectClass() {
		return (ClassUtils.isPresent("javax.transaction.Transactional", getClass().getClassLoader()) ?
				TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME :
				TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME);
	}

}
