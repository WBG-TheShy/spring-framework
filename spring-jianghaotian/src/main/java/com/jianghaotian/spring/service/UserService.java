package com.jianghaotian.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
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
public class UserService {

	@Autowired
	private OrderService orderService;

	public void test() {
		System.out.println("test方法:" + orderService);
	}
}
