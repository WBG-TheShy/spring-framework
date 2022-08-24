package com.jianghaotian.mybatis_spring.core;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/16 00:15
 *
 * @author jianghaotian
 */
public class MyMapperBeanDefinitonRegistar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		//拿到导入类的MyMapperScan注解信息
		Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(MyMapperScan.class.getName());
		//获取扫描路径
		String path = (String)annotationAttributes.get("value");
		//扫描
		//这里不可以直接使用Spring提供的扫描方法ClassPathBeanDefinitionScanner
		//因为Spring的扫描器不会扫描接口,而我们需要扫描接口
		//所以要新写一个扫描器,继承ClassPathBeanDefinitionScanner
		MyMapperScanner scanner = new MyMapperScanner(registry);

		//设置包含过滤器都为true
		scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
		//扫描
		scanner.scan(path);
	}

}
