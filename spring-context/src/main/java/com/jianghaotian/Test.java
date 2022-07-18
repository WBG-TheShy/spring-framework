package com.jianghaotian;

import com.jianghaotian.service.UserService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/3/7 13:38
 *
 * @author jianghaotian
 */
public class Test {
	public static void main(String[] args) {
		//Bean的生成过程

		//所有后处理器的执行顺序
		//0. BeanFactoryPostProcessor.postProcessBeanFactory()----扫描包并解析为元数据(ASM技术),符合要求的,封装为bean定义进行注册[核心方法 doScan()]
		//1. InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()---基本不用[核心方法 createBean()]
		//2. 实例化----推断构造方法[核心方法 doCreateBean()]
		//3. MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition()---查找@Autowired的注入点并缓存[核心方法 doCreateBean()]
		//4. InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()---基本不用[核心方法 populateBean()]
		//5. 自动注入---设置对象的属性值[核心方法 populateBean()]
		//6. InstantiationAwareBeanPostProcessor.postProcessProperties()---处理@Autowired,@Resource,@Value等注解[核心方法 populateBean()]
		//7. Aware对象----各种Aware接口的回调[核心方法 initializeBean()]
		//8. BeanPostProcessor.postProcessBeforeInitialization()----执行@PostConstruct方法,另一些Aware接口的回调[核心方法 initializeBean()]
		//9. 初始化-----如果实现了InitializingBean接口,就调用其afterPropertiesSet() 方法[核心方法 initializeBean()]
		//10. BeanPostProcessor.postProcessAfterInitialization()----基于AOP的处理,返回的是代理bean对象[核心方法 initializeBean()]
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		//创建bean
		UserService userService = (UserService) context.getBean("userService");
		userService.test();
	}
}
