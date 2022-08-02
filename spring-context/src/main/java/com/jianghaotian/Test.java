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
		//Bean的生命周期
		//1.解析配置类 parse()
			//A.扫描 doScan()
		//2.实例化 createBean()--->doCreateBean()---->createBeanInstance()
		//3.依赖注入 createBean()--->doCreateBean()---->populateBean()
		//4.初始化 createBean()--->doCreateBean()---->initializeBean()
		//5.注册销毁 createBean()--->doCreateBean()---->registerDisposableBeanIfNecessary()
		//6.使用
		//7.销毁 doClose()---->destroyBeans()


		//所有后处理器的执行顺序
		//-----------------------------------------parse()-----------------------------------------
		//0 [bean工厂回调]BeanFactoryPostProcessor.postProcessBeanFactory()----bean工厂后置处理器
			//A [可以利用bean定义注册器做额外的操作]BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry()--解析配置类(其中解析@ComponentScan注解的时候会去扫描[核心方法doScan()])

		//-----------------------------------------createBean()-----------------------------------------
		//1. [实例化前]InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()---基本不用

		//-----------------------------------------doCreateBean()-----------------------------------------
		//2. 实例化----推断构造方法[核心方法 createBeanInstance()]
		//3. [对bean定义进行额外的处理]MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition()---查找@Autowired的注入点并缓存

		//-----------------------------------------populateBean()-----------------------------------------
		//4. [实例化后]InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()---基本不用
		//5. 自动注入---设置对象的属性值(Spring自带的依赖注入,利用setXxx方法,再根据by_name或者by_type)
		//6. [处理属性回调]InstantiationAwareBeanPostProcessor.postProcessProperties()---对bean的属性进一步处理,对@Autowired,@Resource,@Value等标记的注入点进行赋值

		//-----------------------------------------initializeBean()-----------------------------------------
		//7. Aware对象----各种Aware接口的回调
		//8. [初始化前]BeanPostProcessor.postProcessBeforeInitialization()----执行@PostConstruct方法,另一些Aware接口的回调
		//9. 初始化-----如果实现了InitializingBean接口,就调用其afterPropertiesSet() 方法
		//10. [初始化后]BeanPostProcessor.postProcessAfterInitialization()----基于AOP的处理,返回的是代理bean对象
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		//创建bean
		UserService userService = (UserService) context.getBean("userService");
		userService.test();
	}
}
