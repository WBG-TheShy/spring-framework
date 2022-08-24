package com.jianghaotian.mybatis_spring.core;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

import java.util.Set;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/16 10:20
 *
 * @author jianghaotian
 */
public class MyMapperScanner extends ClassPathBeanDefinitionScanner {

	public MyMapperScanner(BeanDefinitionRegistry registry) {
		super(registry);
	}

	@Override
	protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
		//mapper都是接口,所以只有接口才可以当做候选者
		return beanDefinition.getMetadata().isInterface();
	}

	@Override
	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		//扫描并注册完成的bean定义
		Set<BeanDefinitionHolder> beanDefinitionHolders = super.doScan(basePackages);
		beanDefinitionHolders.forEach(x -> {
			GenericBeanDefinition beanDefinition = (GenericBeanDefinition) x.getBeanDefinition();
			//设置构造方法入参值是mapper的类名称
			beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition.getBeanClassName());
			//设置bean的类型本身为FactoryBean
			beanDefinition.setBeanClassName(MyMapperFactoryBean.class.getName());
		});
		return beanDefinitionHolders;
	}
}
