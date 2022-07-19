package com.jianghaotian;

import com.jianghaotian.interfaces.AppConfigInterface;
import com.jianghaotian.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/3/7 14:29
 *
 * @author jianghaotian
 */
//@ComponentScan("com.jianghaotian")
//public class AppConfig {
//
//	@Bean
//	public UserService userService() {
//		return new UserService();
//	}
//}

//@ComponentScan("com.jianghaotian")
//public class AppConfig {
//
//}

//@Import(UserService.class)
//public class AppConfig {
//
//}
@Configuration
public class AppConfig{

	@Configuration
	class AppConfig2{
		@Bean
		public UserService userService() {
			return new UserService();
		}
	}
}
