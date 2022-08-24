package com.jianghaotian.mybatis_spring.core;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Proxy;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/15 23:49
 * <p>
 * 因为每一个mapper都需要经过mybatis的代理逻辑生成一个代理对象,所以写成一个通用的bean工厂,专门用来生成代理对象
 *
 * @author jianghaotian
 */

public class MyMapperFactoryBean implements FactoryBean {

	private Class<?> mapperInterface;

	private SqlSession sqlSession;

	public MyMapperFactoryBean(Class<?> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	@Autowired
	public void setSqlSession(SqlSessionFactory sqlSessionFactory) {
		sqlSessionFactory.getConfiguration().addMapper(mapperInterface);
		this.sqlSession = sqlSessionFactory.openSession();
	}

	@Override
	public Object getObject() {
		//mybatis生成代理对象
		return sqlSession.getMapper(mapperInterface);
	}

	@Override
	public Class<?> getObjectType() {
		return mapperInterface;
	}

	@Override
	public boolean isSingleton() {
		return FactoryBean.super.isSingleton();
	}
}
