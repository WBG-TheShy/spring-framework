package com.jianghaotian.springmvc;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/9/10 14:46
 *
 * @author jianghaotian
 */
public class MyWebApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	//默认先去调用父类的父类的onStartup()方法

	/**
	 * 父容器配置类(Spring相关)
	 * @return
	 */
	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class[0];
	}

	/**
	 * 子容器配置类(springMVC相关)
	 * @return
	 */
	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class[0];
	}

	/**
	 * DispatcherServlet的拦截路径
	 *
	 * @return 返回/ 表示拦截所有请求
	 */
	@Override
	protected String[] getServletMappings() {
		return new String[]{"/"};
	}
}
