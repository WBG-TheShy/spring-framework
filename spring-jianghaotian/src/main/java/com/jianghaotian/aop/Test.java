package com.jianghaotian.aop;

import com.jianghaotian.aop.service.PersonService;
import com.jianghaotian.aop.service.UserInterface;
import com.jianghaotian.aop.service.UserService;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.cglib.proxy.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/16 17:33
 *
 * @author jianghaotian
 */
public class Test {
	public static void main(String[] args) {
		//cglibTest();
		//jdkTest();
		//springAopTest();
		//aopFromBeanNameAutoProxyCreator();
		defaultAdvisorAutoProxyCreator();
	}


	/**
	 * jdk动态代理
	 */
	public static void jdkTest(){


		//创建代理对象
		UserInterface UserInterface = (UserInterface) Proxy.newProxyInstance(UserService.class.getClassLoader(), new Class[]{UserInterface.class}, new InvocationHandler() {

			UserInterface target = new UserService();

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				System.out.println("before");
				Object result = method.invoke(target, args);
				System.out.println("after");
				return result;
			}
		});
		UserInterface.test();
	}

	/**
	 * cglib代理
	 */
	public static void cglibTest() {

		//cglib动态代理
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(UserService.class);
		enhancer.setCallbacks(new Callback[]{new MethodInterceptor() {

			UserService target = new UserService();

			/**
			 * 代理逻辑
			 *
			 * @param o            代理对象
			 * @param method       代理对象执行的方法本身
			 * @param objects      方法入参
			 * @param methodProxy  cglib提供的
			 * @return
			 * @throws Throwable
			 */
			@Override
			public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
				System.out.println("前置");
				//这个其实是在调用test方法
				//以下两种写法都可以
				Object result = method.invoke(target, objects);
				//result = methodProxy.invokeSuper(o, objects);
				System.out.println("后置");
				return result;
			}
		}, NoOp.INSTANCE});

		//如果没有其他配置的话,默认UserService的所有方法都会走代理逻辑
		//如果想自定义代理的方法,可以继续配置一个代理过滤器
		enhancer.setCallbackFilter(new CallbackFilter() {

			/**
			 * 返回的是代理的方法索引
			 *
			 * @param method
			 * @return
			 */
			@Override
			public int accept(Method method) {
				//如果执行的是test方法,采用下标为0的代理逻辑,也就是打印前置后置字符串的逻辑
				//其他方法,采用下标为1的代理逻辑,也就是NoOp.INSTANCE,这个是spring自己定义的一个代理逻辑:什么也不做
				if (method.getName().equals("test")) {
					return 0;
				} else {
					return 1;
				}
			}
		});


		//代理对象
		UserService userService = (UserService) enhancer.create();
		userService.test();
		userService.test2();
	}

	/**
	 * springAOP代理
	 */
	public static void springAopTest(){
		//对JDK动态代理和CGLIB进行了封装

		UserService target = new UserService();

		//Spring封装的aop工厂
		ProxyFactory proxyFactory = new ProxyFactory();

		//设置被代理的对象,内部会根据target生成一个TargetSource,然后调用proxyFactory.setTargetSource(),设置进去
		//@Lazy注解就是用的TargetSource,TargetSource可以理解为被代理对象的来源,或者说生成被代理对象的逻辑
		//buildLazyResolutionProxy()就会在使用bean的时候,才根据TargetSource里的逻辑实时的生成被代理对象
		proxyFactory.setTarget(target);

		////设置代理逻辑(根据add的顺序来执行)
		////前置通知
		//proxyFactory.addAdvice(new MethodBeforeAdvice() {
		//	@Override
		//	public void before(Method method, Object[] objects, Object o) throws Throwable {
		//		System.out.println("前置通知1");
		//	}
		//});
		//
		////后置通知
		//proxyFactory.addAdvice(new AfterReturningAdvice() {
		//	@Override
		//	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		//		System.out.println("后置通知2");
		//	}
		//});
		//
		////环绕通知
		//proxyFactory.addAdvice(new org.aopalliance.intercept.MethodInterceptor() {
		//	@Nullable
		//	@Override
		//	public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
		//		System.out.println("环绕前");
		//		Object proceed = invocation.proceed();
		//		System.out.println("环绕后");
		//		return proceed;
		//	}
		//});
		//
		////前置通知
		//proxyFactory.addAdvice(new MethodBeforeAdvice() {
		//	@Override
		//	public void before(Method method, Object[] objects, Object o) throws Throwable {
		//		System.out.println("前置通知2");
		//	}
		//});
		//
		////后置通知
		//proxyFactory.addAdvice(new AfterReturningAdvice() {
		//	@Override
		//	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		//		System.out.println("后置通知");
		//	}
		//});
		//
		////异常通知
		//proxyFactory.addAdvice(new ThrowsAdvice() {
		//	public void afterThrowing(Method method, Object[] args, Object target, Exception ex) throws Throwable {
		//		System.out.println("异常通知");
		//	}
		//});

		//addAdvice会代理所有方法
		//而addAdvisor可以指定你想代理哪一个或哪些方法上
		//addAdvisor可以理解为addAdvice+pointcut
		proxyFactory.addAdvisors(new PointcutAdvisor() {

			/**
			 * 代理的切点
			 *
			 * @return
			 */
			@Override
			public Pointcut getPointcut() {
				return new StaticMethodMatcherPointcut() {
					@Override
					public boolean matches(Method method, Class<?> targetClass) {
						return method.getName().equals("test");
					}
				};
			}

			/**
			 * 代理逻辑
			 *
			 * @return
			 */
			@Override
			public Advice getAdvice() {
				return new MethodBeforeAdvice() {
					@Override
					public void before(Method method, Object[] args, Object target) throws Throwable {
						System.out.println("前置通知3");
					}
				};
			}

			/**
			 * 这个方法不管
			 * @return
			 */
			@Override
			public boolean isPerInstance() {
				return false;
			}
		});

		//得到代理对象
		UserService proxy = (UserService) proxyFactory.getProxy();
		proxy.test();
		proxy.test2();
	}

	/**
	 * beanNameAutoProxyCreator的用法
	 */
	public static void aopFromBeanNameAutoProxyCreator(){
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		PersonService personService = (PersonService)context.getBean("personService");
		personService.test();
	}

	/**
	 * defaultAdvisorAutoProxyCreator用法
	 */
	public static void defaultAdvisorAutoProxyCreator(){
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		PersonService personService = (PersonService)context.getBean("personService");
		personService.test();
	}
}
