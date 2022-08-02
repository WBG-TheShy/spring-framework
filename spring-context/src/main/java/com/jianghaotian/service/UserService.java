package com.jianghaotian.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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
public class UserService {

	private OrderService orderService;

	public void test(){
		System.out.println("test方法:"+orderService);
	}

	@Autowired
	public void kkk(OrderService orderService) {
		this.orderService = orderService;
		System.out.println(orderService);
	}
}
