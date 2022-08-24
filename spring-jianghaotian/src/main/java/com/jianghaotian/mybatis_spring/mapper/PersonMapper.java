package com.jianghaotian.mybatis_spring.mapper;

import org.apache.ibatis.annotations.Select;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/15 23:23
 *
 * @author jianghaotian
 */
public interface PersonMapper {

	@Select("select '蒋浩天'")
	String getName();
}
