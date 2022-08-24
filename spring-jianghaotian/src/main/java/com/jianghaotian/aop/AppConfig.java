package com.jianghaotian.aop;

import com.jianghaotian.aop.advise.MyAdvice;
import com.jianghaotian.aop.service.UserService;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.annotation.*;

import java.lang.reflect.Method;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/18 20:49
 *
 * @author jianghaotian
 */
@ComponentScan("com.jianghaotian.aop")
@EnableAspectJAutoProxy
public class AppConfig {

	/**
	 * 将代理对象直接成为一个bean,可以更方便的使用
	 *
	 * 不过用的很少
	 *
	 * @return
	 */
	//@Bean
	//public ProxyFactoryBean proxyFactoryBean(){
	//	ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
	//	proxyFactoryBean.addAdvice(new MethodBeforeAdvice() {
	//		@Override
	//		public void before(Method method, Object[] args, Object target) throws Throwable {
	//			System.out.println("before");
	//		}
	//	});
	//	proxyFactoryBean.setTarget(new UserService());
	//
	//
	//	return proxyFactoryBean;
	//}

	/**
	 * 如果代理逻辑和被代理的对象都是bean,则可以用BeanNameAutoProxyCreator
	 * 将两者一起
	 * @return
	 */
	//@Bean
	//public BeanNameAutoProxyCreator beanNameAutoProxyCreator(){
	//	//BeanNameAutoProxyCreator是一个BeanPostProcessor,所以实现了postProcessAfterInitialization方法,
	//	//也就是在bean初始化后的阶段中,判断beanName是否在你所想要的(用setBeanNames方法指定),如果是,则在生成代理对象,代理逻辑(用setInterceptorNames方法)也是你指定的并返回
	//	BeanNameAutoProxyCreator beanNameAutoProxyCreator = new BeanNameAutoProxyCreator();
	//
	//	//被代理的bean对象的名字
	//	//setBeanNames的时候甚至可以设置前缀,那么只要bean的名字符合前缀就会生成对应的代理对象(适用于要代理多个bean的情况)
	//	//beanNameAutoProxyCreator.setBeanNames("personSer*");
	//	beanNameAutoProxyCreator.setBeanNames("personService");
	//
	//	//代理的逻辑的bean名字
	//	beanNameAutoProxyCreator.setInterceptorNames("myAdvice");
	//	return beanNameAutoProxyCreator;
	//}


	/**
	 * 下面两个bean要一起使用,更牛逼一点
	 *
	 * @return
	 */
	@Bean
	public DefaultPointcutAdvisor defaultPointcutAdvisor(){
		//只代理bean中的test方法
		NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
		pointcut.addMethodName("test");

		//生成代理器
		DefaultPointcutAdvisor defaultPointcutAdvisor = new DefaultPointcutAdvisor();
		defaultPointcutAdvisor.setPointcut(pointcut);
		defaultPointcutAdvisor.setAdvice(new MyAdvice());

		return defaultPointcutAdvisor;
	}
	@Bean
	public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator(){
		//DefaultAdvisorAutoProxyCreator这个也是一个BeanPostProcessor,所以实现了postProcessAfterInitialization方法
		//在当前的bean初始化后阶段,它会去找所有当前容器中类型是DefaultPointcutAdvisor类型的bean
		//找到后,判断当前的bean是否和有你指定的切点(比如:用addMethodName("test")方法指定的话,也就去判断当前bean是否有一个test方法),如果有,则证明要生成代理对象
		//代理逻辑用setAdvice()方法去指定
		return new DefaultAdvisorAutoProxyCreator();
	}
}
