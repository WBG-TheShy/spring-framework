package com.jianghaotian.mybatis;

import com.jianghaotian.mybatis.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/15 16:43
 *
 * @author jianghaotian
 */
public class Test {
	public static void main(String[] args) throws IOException {
		InputStream inputStream = Resources.getResourceAsStream("mybatis.xml");
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
		SqlSession sqlSession = sqlSessionFactory.openSession();

		//此处的UserMapper是mybatis生成的UserMapper代理对象
		UserMapper mapper = sqlSession.getMapper(UserMapper.class);
		String result = mapper.select();

		System.out.println(result);

		sqlSession.commit();
		sqlSession.flushStatements();
		sqlSession.close();
	}
}
