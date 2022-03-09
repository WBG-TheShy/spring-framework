package com.jianghaotian.service;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/3/7 14:28
 *
 * @author jianghaotian
 */
@Component
@Conditional({})
@DependsOn({"orderService","userService"})
public class UserService {

	public void test(){
		System.out.println("test");
	}
}
