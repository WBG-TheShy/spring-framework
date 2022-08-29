package com.jianghaotian.transaction.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/26 20:25
 *
 * @author jianghaotian
 */
public interface UserMapper {

	@Select("select 'user'")
	String select();

	@Insert("insert into test values (1)")
	void insert1();

	@Insert("insert into test values (2)")
	void insert2();
}
