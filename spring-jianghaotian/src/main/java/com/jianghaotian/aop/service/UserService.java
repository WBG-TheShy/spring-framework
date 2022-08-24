package com.jianghaotian.aop.service;

import org.springframework.stereotype.Component;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/16 17:33
 *
 * @author jianghaotian
 */
@Component
public class UserService implements UserInterface {

	@Override
	public void test() {
		System.out.println("test");
	}

	public void test2() {
		System.out.println("test22222");
	}
}
