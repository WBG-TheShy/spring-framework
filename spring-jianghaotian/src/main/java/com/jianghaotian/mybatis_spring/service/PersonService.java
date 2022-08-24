package com.jianghaotian.mybatis_spring.service;

import com.jianghaotian.mybatis_spring.mapper.PersonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/15 23:24
 *
 * @author jianghaotian
 */
@Component
public class PersonService {

	//直接注入是不会有值的(Spring扫描的时候会直接过滤接口),所以会报错
	//按照mybatis的逻辑,这里的personMapper都是由mybatis生成的代理对象
	//所以整合的目的就是在这里注入代理对象
	@Autowired
	private PersonMapper personMapper;

	public void test(){
		//此处会打印org.apache.ibatis.binding.MapperProxy@2d928643,很明显这个是mybatis生成的代理对象
		System.out.println(personMapper);
		//正常执行sql
		System.out.println(personMapper.getName());
	}
}
