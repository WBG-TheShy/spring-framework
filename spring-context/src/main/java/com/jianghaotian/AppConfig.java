package com.jianghaotian;

import com.jianghaotian.interfaces.AppConfigInterface;
import com.jianghaotian.service.OrderService;
import com.jianghaotian.service.UserService;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/3/7 14:29
 *
 * @author jianghaotian
 */
@ComponentScan("com.jianghaotian")
public class AppConfig {

}

//@ComponentScan("com.jianghaotian")
//public class AppConfig {
//
//}

//@Import(UserService.class)
//public class AppConfig {
//
//}
//public class AppConfig{
//
//
//}
