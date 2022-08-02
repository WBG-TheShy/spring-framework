package com.jianghaotian.postprocessor;

import com.jianghaotian.service.UserService;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/7/19 15:28
 *
 * @author jianghaotian
 */
@Component
public class MyBeanPostPrrocessor implements InstantiationAwareBeanPostProcessor {

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		//if("userService".equals(beanName)){
		//	try {
		//		Field field = bean.getClass().getDeclaredField("name");
		//		field.setAccessible(true);
		//		field.set(bean,"jianghaotian");
		//	} catch (IllegalAccessException | NoSuchFieldException e) {
		//		e.printStackTrace();
		//	}
		//}
		return pvs;
	}
}
