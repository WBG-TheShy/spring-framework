package com.jianghaotian.springmvc;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/9/5 15:33
 *
 * @author jianghaotian
 */
public class Test {
	public static void main(String[] args) {
		//Java提供了Servlet的功能
		//使用纯Servlet就要在web.xml文件里配置很多servlet映射,并且每一个Servlet实现类还要继承HttpServlet,侵入性强,使用起来非常不方便
		//web.xml文件格式定义在Servlet规范中，因此所有符合Servlet规范的Java Servlet Container都需要web.xml,Tomcat就是一个Java Servlet Container

		//那Spring为了方便程序员使用Servlet,诞生了SpringMVC
		//SpringMVC封装了Servlet
		//SpringMVC核心在于org.springframework.web.servlet.DispatcherServlet.doDispatch()方法
		//里面会根据客户端传入的url连接,找到各种组件来完成映射(组件默认是通过DispatcherServlet.properties文件配置的),方法调用,视图解析,渲染等等

		//一般情况下,我们都是使用@RequestMapping注解作为url和方法的映射工具

		//配置类上添加@EnableWebMvc注解,import了一个DelegatingWebMvcConfiguration的配置类,这个类里有许多@Bean方法
		// 	1.org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport.requestMappingHandlerMapping():添加一个RequestMappingHandlerMapping的bean对象
		//	2.org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport.requestMappingHandlerAdapter():添加一个RequestMappingHandlerAdapter的bean对象
		//	3.org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport.mvcViewResolver():添加一个ViewResolver的bean对象
		//  等等还有很多,但先看这3个
		//	这3个bean都实现了InitializingBean接口,所以重写了afterPropertiesSet()方法
		//子容器创建并执行refresh()方法
		//refresh过程中实例化非懒加载的bean,而执行到初始化步骤时,会调用到上述3个bean的afterPropertiesSet()方法中去
		//
		//1.RequestMappingHandlerMapping的afterPropertiesSet()逻辑:
		//	a.拿到子容器中所有的beanName
		//	b.获取beanName对应的的bean的Class对象
		//	c.判断类上是否有@Controller或者@RequestMapping注解,如果有,才会进行下一步
		//	d.拿到类中所有的方法,解析方法上的@RequestMapping注解,并对应生成一个RequestMappingInfo对象(含有注解的所有属性以及各种condition)
		//	e.根据当前的bean生成一个HandlerMethod
		//	f.将解析后的结果放入缓存中
		//		f1.pathLookup  key:方法的映射路径(例如:/user/info) value:RequestMappingInfo对象
		//		f2.registry    key:RequestMappingInfo对象 value:MappingRegistration对象
		//
		//2.RequestMappingHandlerAdapter的afterPropertiesSet()逻辑:
		//	a.初始化@ControllerAdvice标注的Bean，并解析此Bean内部各部分（@ModelAttribute、@InitBinder、RequestBodyAdvice和ResponseBodyAdvice接口）然后缓存起来。
		//	b.初始化参数解析器,也就是@RequestMapping的handler上能写哪些注解自动封装参数(比如@RequestParam、@PathVariable、@RequestBody)
		//	c.初始化@InitBinder的参数解析器
		//	d.初始化返回值解析器,它是提供对HandlerMethod返回值的支持（比如@ResponseBody、DeferredResult等返回值类型）。

		//至此@RequestMapping解析完毕,等待客户端发送请求.....

		//1.发起请求
		//2.DispatcherServlet收到请求,具体方法签名为:org.springframework.web.servlet.DispatcherServlet.doDispatch()方法-进入此方法看详细注释
		//	a.解析请求映射(委托给HandleMapping,这是一个接口,具体实现类自由配置,默认是RequestMappingHandleMapping)
		//    返回一个HandleChain给DispatcherServlet,这是一个链,里面有程序中定义的拦截器
		//	b.调用处理器适配器(HandleAdapter,这也是一个接口),解析url请求的参数,然后执行程序员写的业务方法
		//	c.业务方法返回ModelAndView给HandleAdapter,再由HandleAdapter返回给DispatcherServlet
		//	d.解析视图名称(ViewReslover视图解析器),解析后返回给DispatcherServlet一个View
		//	e.DispatcherServlet解析视图,渲染视图,随后响应到客户端
	}
}
