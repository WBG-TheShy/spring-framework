package com.jianghaotian.aop.aspectj;

import com.jianghaotian.aop.service.UserInterface;
import com.jianghaotian.aop.service.UserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareAnnotation;
import org.aspectj.lang.annotation.DeclareParents;
import org.springframework.stereotype.Component;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/18 22:21
 *
 *
 * @author jianghaotian
 */
//@Aspect,@Before,@Around等等不是Spring的注解,而是另一个项目AspectJ的注解,所以要使用这个注解就得引入AspectJ的jar包
//由于这些注解都是在编译时期对类进行增强,所以要使用AspectJ提供的编译器,才能使用这些注解
//但是Spring为了更方便的让程序员使用AOP,Spring直接把这些注解拿过来,但是为了不额外使用AspectJ提供的编译器,,所以Spring
//将增强的时机从编译器变为运行期,这样就不需要使用AspectJ提供的编译器了
//如果Spring发现一个类是一个bean,同时bean上有@Aspect注解,那么Spring会认为你的类中定义了很多切面和代理逻辑
//Spring就把这些切面封装为一个PointCut,代理逻辑封装为一个Advice,组成一个Advisor,并将这个Advisor注册为一个bean,同时在每一个bean初始化后阶段,来生成代理对象

//最后,Spring不会自动的处理AspectJ注解,所以为了能让Spring来处理AspectJ注解,要额外的加一个注解@EnableAspectJAutoProxy
//这个注解的的原理就是@Import了一个可以处理AspectJ注解的一个类(AnnotationAwareAspectJAutoProxyCreator),这个类是一个beanPostProcessor
@Aspect
@Component
public class MyAspectJ {

	//AspectJ还提供了其他的功能(不过用的很少)
	//例如:代码方式给某个类实现某个接口

	//这个意思是,给value属性匹配的类(这里只能匹配到PersonService这个类)实现UserInterface接口,重写的方法逻辑在defaultImpl属性配置(这里是UserService)
	@DeclareParents(value = "com.jianghaotian.aop.service.PersonService", defaultImpl = UserService.class)
	private UserInterface userInterface;

	@Before("execution(* com.jianghaotian.aop.service.PersonService.test())")
	public void before(JoinPoint joinPoint) {
		System.out.println("aspectJ");
	}
}
