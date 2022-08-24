package com.jianghaotian.mybatis_spring.core;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.io.InputStream;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/16 10:48
 *
 * @author jianghaotian
 */
@ComponentScan("com.jianghaotian.mybatis_spring")
@MyMapperScan("com.jianghaotian.mybatis_spring.mapper")
public class AppConfig {

	@Bean
	public SqlSessionFactory sqlSessionFactory() throws IOException {
		InputStream resourceAsStream = Resources.getResourceAsStream("mybatis.xml");
		return new SqlSessionFactoryBuilder().build(resourceAsStream);
	}
}
