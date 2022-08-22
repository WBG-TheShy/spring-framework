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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {
		//入参的config就是ProxyFactory,method是代理的方法,targetClass是代理的类的Class对象

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		//获取这个ProxyFactory里所有的Advisors(即使你是用addAdvice()方法添加的,Spring也会给你封装成一个Advisor)
		//所以,最终所有的代理逻辑都以Advisor形式存在
		Advisor[] advisors = config.getAdvisors();
		//代理逻辑链
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		Boolean hasIntroductions = null;

		//遍历所有的Advisor
		for (Advisor advisor : advisors) {
			if (advisor instanceof PointcutAdvisor) {
				//如果是一个切点类型的通知器
				//每一个PointCut,会有两个方法需要实现
				//一个是getClassFilter(),表示这个切点是否匹配当前的类
				//一个是getMethodMatcher(),表示这个切点是否匹配当前的方法
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				//调用getPointcut()方法获取切点,看当前的类是否符合切点的条件
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					//如果符合当前类,再判断方法(拿出来方法匹配器)
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					if (mm instanceof IntroductionAwareMethodMatcher) {
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
					}
					else {
						//执行匹配器的匹配方法,获得匹配结果
						match = mm.matches(method, actualClass);
					}
					//如果匹配
					if (match) {
						//将advisor封装成MethodInterceptor,这里是数组的原因是:有可能这个advisor既是before通知器,也是afterRunning通知器,所以生成两个Interceptor
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						//如果方法匹配器的isRuntime=true
						//这里要理解一个东西:MethodMatcher有两个matchs方法,一个是带方法入参的,一个是不带的
						//不带方法入参的,属于静态筛选,静态的意思是编译期就已经确定了的东西:所以只比较method的被代理的类
						//带方法入参的,属于动态筛选,因为方法的的入参只有在运行时才会确定,所以要在静态筛选的基础上,额外进行一次动态筛选(这个时候,方法入参就作为比较条件了)
						//而isRuntime属性=true的意思是,当代理对象调用方法时,先静态筛选代理逻辑链,筛选完后开始执行,再执行代理逻辑的途中,再进行动态筛选
						//举个例子:静态筛选出来的代理逻辑链里,有3个(前两个isRuntime=false的,最后一个isRuntime=true),那么正常执行第1个,第2个,执行第3个之前,进行了动态筛选,发现matchs方法返回false,所以第3个代理逻辑不会执行
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							//Spring会将刚刚得到的MethodInterceptor遍历一遍,将代理逻辑和比较器重新封装成一个InterceptorAndDynamicMethodMatcher
							//再放入到代理逻辑链中
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
					//如果不匹配,说明当前方法不进行代理逻辑,直接返回空的链
				}
			}
			//如果这个通知器
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			//其余情况,直接
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}

		return interceptorList;
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
