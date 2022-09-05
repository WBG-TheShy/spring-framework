package com.jianghaotian.transaction;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/26 20:23
 *
 * @author jianghaotian
 */
@Configuration
@ComponentScan("com.jianghaotian.transaction")
@MapperScan("com.jianghaotian.transaction.mapper")
@EnableTransactionManagement
public class AppConfig {

	/**
	 * 事务管理器
	 *
	 * @return
	 */
	@Bean
	public PlatformTransactionManager transactionManager(){
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
		transactionManager.setDataSource(dataSource());
		//如果部分失败了 是否全部回滚 默认true-部分失败了,同一事务里都回滚 false-部分失败了,能执行成功的都提交
		transactionManager.setGlobalRollbackOnParticipationFailure(true);
		return transactionManager;
	}

	/**
	 * sqlSessionFactory
	 * @return
	 * @throws Exception
	 */
	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dataSource());
		return sqlSessionFactoryBean.getObject();
	}

	/**
	 * 数据源
	 * @return
	 */
	@Bean
	public DataSource dataSource(){
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://localhost:3306/test?useSSL=false");
		dataSource.setUsername("root");
		dataSource.setPassword("Jht19960726,.@");
		return dataSource;
	}
}
