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
		//1.扫描 doScan()
		//2.实例化 createBean()--->doCreateBean()---->createBeanInstance()
		//3.依赖注入 createBean()--->doCreateBean()---->populateBean()
		//4.初始化 createBean()--->doCreateBean()---->initializeBean()
		//5.注册销毁 createBean()--->doCreateBean()---->registerDisposableBeanIfNecessary()
		//6.使用
		//7.销毁 doClose()---->destroyBeans()


		//所有后处理器的执行顺序
		//-----------------------------------------doScan()-----------------------------------------
		//0. BeanFactoryPostProcessor.postProcessBeanFactory()----扫描包并解析为元数据(ASM技术),符合要求的,封装为bean定义进行注册

		//-----------------------------------------createBean()-----------------------------------------
		//1. InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()---基本不用

		//-----------------------------------------doCreateBean()-----------------------------------------
		//2. 实例化----推断构造方法[核心方法 doCreateBean()]
		//3. MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition()---查找@Autowired的注入点并缓存

		//-----------------------------------------populateBean()-----------------------------------------
		//4. InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()---基本不用
		//5. 自动注入---设置对象的属性值
		//6. InstantiationAwareBeanPostProcessor.postProcessProperties()---处理@Autowired,@Resource,@Value等注解

		//-----------------------------------------initializeBean()-----------------------------------------
		//7. Aware对象----各种Aware接口的回调
		//8. BeanPostProcessor.postProcessBeforeInitialization()----执行@PostConstruct方法,另一些Aware接口的回调
		//9. 初始化-----如果实现了InitializingBean接口,就调用其afterPropertiesSet() 方法
		//10. BeanPostProcessor.postProcessAfterInitialization()----基于AOP的处理,返回的是代理bean对象
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		//创建bean
		UserService userService = (UserService) context.getBean("userService");
		userService.test();
	}
}
