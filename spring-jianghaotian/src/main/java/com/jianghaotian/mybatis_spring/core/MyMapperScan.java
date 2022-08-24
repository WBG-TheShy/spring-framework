package com.jianghaotian.mybatis_spring.core;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/15 23:45
 *
 * 因为mapper要成为bean,注入到需要的位置,所以新增一个扫描器,用来扫描指定路径下的mapper接口,
 * 同时,使用@Import注解,解析MyMapperBeanDefinitonRegistar的同时会调用registerBeanDefinitions,这样就可以将mapper注册为bean定义
 *
 *
 * @author jianghaotian
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MyMapperBeanDefinitonRegistar.class)
public @interface MyMapperScan {

	/**
	 * 扫描路径
	 *
	 * @return
	 */
	String value();
}
