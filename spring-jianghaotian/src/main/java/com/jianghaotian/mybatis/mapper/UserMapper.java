package com.jianghaotian.mybatis.mapper;

import org.apache.ibatis.annotations.Select;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/15 16:45
 *
 * @author jianghaotian
 */
public interface UserMapper {

	@Select("select 'user'")
	String select();
}
