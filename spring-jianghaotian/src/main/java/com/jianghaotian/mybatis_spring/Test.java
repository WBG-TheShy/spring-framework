package com.jianghaotian.mybatis_spring;

import com.jianghaotian.mybatis_spring.core.AppConfig;
import com.jianghaotian.mybatis_spring.service.PersonService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/15 23:19
 *
 * @author jianghaotian
 */
public class Test {
	public static void main(String[] args) {
		//由很多框架都需要和Spring进行整合，而整合的核心思想就是把其他框架所产生的对象放到Spring容 器中，让其成为Bean。
		//比如Mybatis，Mybatis框架可以单独使用，而单独使用Mybatis框架就需要用到Mybatis所提供的一 些类构造出对应的对象，然后使用该对象，就能使用到Mybatis框架给我们提供的功能，和Mybatis 整合Spring就是为了将这些对象放入Spring容器中成为Bean，只要成为了Bean，在我们的Spring项 目中就能很方便的使用这些对象了，也就能很方便的使用Mybatis框架所提供的功能了。
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		PersonService personService = (PersonService) context.getBean("personService");
		personService.test();
	}
}
