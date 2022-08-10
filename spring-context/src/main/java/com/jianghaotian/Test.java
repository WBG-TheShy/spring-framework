package com.jianghaotian;

import com.jianghaotian.service.UserService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/3/7 13:38
 *
 * @author jianghaotian
 */
@EnableAspectJAutoProxy
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
			//[可以利用bean定义注册器做额外的操作]BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry()--解析配置类(其中解析@ComponentScan注解的时候会去扫描[核心方法doScan()])
				//解析配置类(配置类:类上有@Component,@PropertySource,@ComponentScan,@Import,@ImportResource至少一个,方法上有@Bean)
				//a.@Component:检查内部类是不是配置类,如果是,则解析它
				//b.@ComponentScan:扫描并注册bean定义(扫描的过程中,如果发现扫描出来的也是配置类,则解析它)
					//核心方法doScan()
					//b1. 首先，通过ResourcePatternResolver获得指定包路径下的所有.class文件(Spring源码中将 此文件包装成了Resource对象)
					//b2. 遍历每个Resource对象
					//b3. 利用MetadataReaderFactory解析Resource对象得到MetadataReader(在Spring源码中 MetadataReaderFactory具体的实现类为CachingMetadataReaderFactory， MetadataReader的具体实现类为SimpleMetadataReader)
						//值得注意的是，CachingMetadataReaderFactory解析某个.class文件得到MetadataReader对象是利用的ASM技术，并没有加载这个类到JVM。
					//b4. 利用MetadataReader进行excludeFilters和includeFilters，以及条件注解@Conditional的筛选 (条件注解并不能理解:某个类上是否存在@Conditional注解，如果存在则调用注解中所指定 的类的match方法进行匹配，匹配成功则通过筛选，匹配失败则pass掉。)
					//b5. 筛选通过后，基于metadataReader生成ScannedGenericBeanDefinition
						//最终得到的ScannedGenericBeanDefinition对象，beanClass属性存储的是当前类的名字，而不是class对象。(beanClass属性的类型是Object，它即可以存储类的名字，也可以存储class对象)
					//b6. 再基于metadataReader判断是不是对应的类是不是接口或抽象类
					//b7. 为每个bean定义生成beanName
					//b8. 给bean定义的一些属性赋默认值(包括解析@Lazy,@Primary,@DependsOn,@Order,@Scope)
					//b7. 注册beanName+bean定义到Spring容器中
				//c.@Import:调用processImports()方法
					//c1.导入的是ImportSelector类型:那么调用执行selectImports方法得到类名，然后在把这个类当做配置类进行解析
					//c2.导入的是ImportBeanDefinitionRegis类型:那么则生成一个ImportBeanDefinitionRegistrar实例对象，并添加到配置类对象中(ConfigurationClass)的importBeanDefinitionRegistrars属性中
					//c3.导入的是普通的类:将这个类当做新配置类解析
				//d.@ImportResource:把导入进来的资源路径存在配置类对象中的importedResources属性中
				//e.@PropertySource:解析该注解，并得到PropertySource对象，并添加到environment中去
				//f.@Bean方法:把这些方法封装为BeanMethod对象，并添加到配置类对象中的beanMethods属性中
				//g.配置类实现了某些接口，则看这些接口内是否定义了@Bean的默认方法
				//h.配置类有父类，则把父类当做配置类进行解析

				//解析配置类过程中,极有可能会有新的配置类生成
				//a.如果新的配置类是通过@Import注解导入进来的，则把这个类生成一个BeanDefinition，同时解析这个类上@Scope,@Lazy等注解信息，并注册BeanDefinition
				//b.如果新的配置类中存在一些BeanMethod，也就是定义了一些@Bean，那么则解析这些@Bean，并生成对应的BeanDefinition，并注册
					//假如一个配置类叫AppConfig.class,里面有一个@Bean注解修饰的方法
					//b1. 如果方法是static的，那么解析出来的BeanDefinition中:
						//b11. factoryBeanName为AppConfig所对应的beanName，比如"appConfig"
						//b12. factoryMethodName为对应的方法名，比如"aService"
						//b13. factoryClass为AppConfig.class
					//b2. 如果方法不是static的，那么解析出来的BeanDefinition中:
						//b21. factoryBeanName为null
						//b22. factoryMethodName为对应的方法名，比如"aService"
						//b23. factoryClass也为AppConfig.class
				//c.如果新的配置类中导入了一些资源文件，比如xx.xml，那么则解析这些xx.xml文件，得到并注册BeanDefinition
				//d.如果新的配置类中导入了一些ImportBeanDefinitionRegistrar，那么则执行对应的registerBeanDefinitions进行BeanDefinition的注册

		//-----------------------------------------createBean()-----------------------------------------
		//1. [实例化前]InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()---基本不用

		//-----------------------------------------doCreateBean()-----------------------------------------
		//2. 实例化----推断构造方法[核心方法 createBeanInstance()]
			//a.根据BeanDefinition加载类得到Class对象
			//b.如果BeanDefinition绑定了一个Supplier，那就调用Supplier的get方法得到一个对象并直接返回
			//c.如果BeanDefinition中存在factoryMethodName，那么就调用该工厂方法得到一个bean对象并返回(处理@Bean就用到了工厂方法)
			//d.如果BeanDefinition已经自动构造过了，那就调用autowireConstructor()自动构造一个对象
			//e.调用SmartInstantiationAwareBeanPostProcessor的determineCandidateConstructors()方法得到哪些构造方法是可以用的
				//我们熟知的AutowiredAnnotationBeanPostProcessor就是实现了SmartInstantiationAwareBeanPostProcessor,
				//它可以让程序员通过@Autowired注解去指定用哪一个构造方法实例化
				//e1.如果所有的构造方法没有一个加了@Autowired注解
					//e11.有多个构造方法,返回null
					//e12.只有一个有参构造,返回此构造方法
					//e13.只有一个无参构造,返回null
				//e2.存在加了@Autowired注解的构造方法
					//e21.只有一个required=true的构造方法,返回此构造方法
					//e22.有多个required=true的构造方法,抛异常
					//e23.有一个required=true和其他required=false的构造方法,抛异常
					//e24.没有required=true的构造方法,返回所有required=false的构造方法+无参构造方法
			//f.如果第5步的返回了可用的构造方法，
			// 	或者当前BeanDefinition的autowired是AUTOWIRE_CONSTRUCTOR(xml才会有这个)，
			// 	或者BeanDefinition中指定了构造方法参数值(AbstractBeanDefinition.setConstructorArgumentValues())，
			// 	或者创建Bean的时候指定了构造方法参数值(getBean()方法可以传入构造方法入参值)，
			// 	那么就调用autowireConstructor()方法自动构造一个对象
			//g.最后，如果不是上述情况，就根据无参的构造方法实例化一个对象

			//实例化要使用构造方法,所以Spring会有一套策略来推断出最适合的一个构造方法,而推断的逻辑,就在autowireConstructor()里面实现
			//具体逻辑:
			//a. 先检查是否指定了具体的构造方法和构造方法参数值，或者在BeanDefinition中缓存了具体的构造方法和构造方法参数值，如果存在那么则直接使用该构造方法进行实例化
			//b. 如果没有确定的构造方法或构造方法参数值，那么
				//b1. 如果没有传入构造方法，那么则找出类中所有的构造方法作为候选者,如果传入了构造方法,则直接把该构造方法当做候选者
				//b2. 如果只有一个无参的构造方法，那么直接使用无参的构造方法进行实例化,流程结束
				//b3. 如果有多个可用的构造方法或者当前Bean需要自动通过构造方法注入
				//b4. 根据所指定的构造方法参数值，确定所需要的最少的构造方法参数值的个数
				//b5. 对所有的构造方法进行筛选(入参数量小于上一步的值,则排除掉)+排序(参数个数多的在前面)
				//b6. 遍历每个构造方法
				//b7. 如果不是调用getBean方法时所指定的构造方法参数值，那么则根据构造方法参数类型找值
				//b8. 如果是调用getBean方法时所指定的构造方法参数值，就直接利用这些值
				//b9. 如果根据当前构造方法找到了对应的构造方法参数值，那么这个构造方法就是可用的，但是不一定这个构造方法就是最佳的，所以这里会涉及到是否有多个构造方法匹配了同样的值，
				// 	  这个时候就会用值和构造方法类型进行匹配程度的打分，找到一个最匹配的

			//如果实例化时出现循环依赖,则无法解决
		//3. [对bean定义进行额外的处理]MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition()---查找@Autowired的注入点并缓存
			//只要属性或方法上有@Autowired,@Value,@Inject注解,Spring会将其封装为一个注入点(忽略static修饰的),并缓存到bean定义中

		//-----------------------------------------populateBean()-----------------------------------------
		//4. [实例化后]InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()---基本不用
		//5. 自动注入---设置对象的属性值
			//a.xml配置的的依赖注入
				//a1. byType(根据setXxx的xxx类型注入)
				//a2. byName(根据setXxx的xxx名字注入)
				//a3. constructor(spring利用构造方法的参数信息从Spring容器中去找bean，找到bean之后作为参数传给构造方法，从而实例化 得到一个bean对象，并完成属性赋值(属性赋值的代码得程序员来写))
				//a4. default
				//a5. no(不使用自动注入,默认就是no)
			//b.@Bean注解的依赖注入
				//b1. byType(根据setXxx的xxx类型注入)
				//b2. byName(根据setXxx的xxx名字注入)
				//b3. no(不使用自动注入,默认就是no)
		//6. [处理属性回调]InstantiationAwareBeanPostProcessor.postProcessProperties()---对bean的属性进一步处理,对@Autowired,@Resource,@Value等标记的注入点进行赋值
			//我们熟知的AutowiredAnnotationBeanPostProcessor就是实现了InstantiationAwareBeanPostProcessor
			//它可以让Spring通过@Autowired注解来对属性进行赋值

			//a.遍历注入点(注入点有可能是属性,有可能是构造方法,有可能是set方法)
			//b.先从缓存里拿(当前注入点的cachedFieldValue属性中)
			//b.把注入点封装成一个依赖描述符(DependencyDescriptor)
			//c.处理@Value注解(占位符,SpringEL表达式)
			//d.处理集合,数组,map等类型的注入点
			//e.根据依赖描述符类型(方法入参的类型或属性的类型)找bean对象,并构造一个Map(key:找到的bean对象的名字,value:找到的bean对象)
			//	根据类型找bean对象的逻辑(也就是by_Type):
				//e1. 找出BeanFactory中类型为type的所有的Bean的名字，注意是名字，而不是Bean对象，因为我们可以根据BeanDefinition就能判断和当前type是不是匹配，不用生成Bean对象
				//e2. 把resolvableDependencies中key为type的对象找出来并添加到map中
				//e3. 遍历根据type找出的beanName，判断当前beanName对应的Bean是不是能够被自动注入
					//e31. 先判断beanName对应的BeanDefinition中的autowireCandidate属性，如果为false，表示不能用来进行自动注入，如果为true则继续进行判断
					//e32. 判断当前type是不是泛型，如果是泛型是会把容器中所有的beanName找出来的，如果是这种情况，那么在这一步中就要获取到泛型的真正类型，然后进行匹配，如果当前beanName和当前泛型对应的真实类型匹配，那么则继续判断
					//e33. 如果当前DependencyDescriptor上存在@Qualifier注解，那么则要判断当前beanName上是否定义了Qualifier，并且是否和当前DependencyDescriptor上的Qualifier相等，相等则匹配
					//e34. 经过上述验证之后，当前beanName对应的bean对象才能被Spring认定为可注入的，随后添加到Map中

		    //f.如果Map中有多个bean对象,进行筛选
				//f1.从这些bean对象里获取被@Primary注解修饰的bean对象(只会有一个,如果有多个,就报错)
				//f2.如果没有找到被@Primary注解修饰的bean对象,那么再从这些bean对象中找优先级最高的(如果bean对象被@Priority注解修饰,那么@Priority注解的value值越小优先级越高)
				//f3.如果既没有@Primary也没有@Priority,那么再根据bean的名字进行匹配唯一的bean对象
			//g.如果Map中只有1个bean对象,直接使用此对象
			//h.将注入点和找到的bean对象的名字封装成一个依赖描述符(ShortcutDependencyDescriptor)缓存到注入点的属性cachedFieldValue中
			//	为什么缓存的是bean名字而不直接缓存bean对象呢?
			//	因为找到的bean对象也有可能是原型的,要保证每次赋的值都是不一样的,所以要通过bean名字重新getBean获取对象,而不能缓存获取对象(缓存的话每次都是一样的bean对象,不符合原型的要求)
			//i.利用反射进行属性赋值或方法调用

			//属性注入的时候出现循环依赖,则使用到三级缓存来解决
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
