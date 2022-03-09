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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		//创建bean
		UserService userService = (UserService) context.getBean("userService");
		userService.test();
	}
}
