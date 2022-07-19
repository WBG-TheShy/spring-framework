package com.jianghaotian.interfaces;

import com.jianghaotian.service.UserService;
import org.springframework.context.annotation.Bean;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/7/19 22:46
 *
 * @author jianghaotian
 */
public interface AppConfigInterface {

	@Bean
	default UserService userService() {
		return new UserService();
	}
}
