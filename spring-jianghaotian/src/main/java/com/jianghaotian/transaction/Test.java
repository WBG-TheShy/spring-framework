package com.jianghaotian.transaction;

import com.jianghaotian.transaction.service.UserService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/26 20:22
 *
 * @author jianghaotian
 */
public class Test {
	public static void main(String[] args) {
		//Spring开启事务再配置类上使用@EnableTransactionManagement注解
		//这个注解点进去可以看到注释


		/**
		 * test()方法中要执行 sql1,sql2,a()方法,sq3,并且test()方法有@Transactional注解
		 *
		 * 那么大概得执行逻辑是:
		 *
		 * 进入TransactionInterceptor的invoke()方法
		 * Spring事务管理器,创建数据库连接conn
		 * conn.autocommit = false
		 * conn.隔离级别 = @Transactional注解的isolation属性
		 * conn放入ThreadLocal<Map>中,Map的key是DataSource,value是conn链接
		 * target.test()
		 * sql1
		 * sql2
		 * a()
		 * 		当前事务挂起->生成挂起对象,将conn设置到挂起对象中
		 * 		Spring事务管理器,创建数据库连接conn1
		 * 		conn1.autocommit = false
		 * 		conn1.隔离级别 = @Transactional注解的isolation属性
		 * 		conn1放入ThreadLocal<Map>中,Map的key是DataSource,value是conn1链接
		 *
		 * 		a()方法执行
		 *
		 * 		conn1.提交
		 *
		 * 		恢复挂起事务->将挂起对象里的conn链接重新放入到ThreadLocal<Map>中
		 * sql3
		 * conn.提交
		 */
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		UserService userService = (UserService)context.getBean("userService");
		userService.test();
	}
}
