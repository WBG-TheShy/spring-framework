package com.jianghaotian.aop.service;

import org.springframework.stereotype.Component;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/18 21:26
 *
 * @author jianghaotian
 */
@Component
public class PersonService {
	public void test() {
		System.out.println("test");
	}

	public void test1() {
		System.out.println("test1");
	}
}
