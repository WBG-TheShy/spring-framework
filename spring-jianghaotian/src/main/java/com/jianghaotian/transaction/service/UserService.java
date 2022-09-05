package com.jianghaotian.transaction.service;

import com.jianghaotian.transaction.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 描述:
 * 公司: 纽睿科技
 * 项目: spring
 * 创建时间: 2022/8/26 20:24
 *
 * @author jianghaotian
 */
@Service
public class UserService {

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private UserService userService;

	@Transactional(rollbackFor = {Exception.class,ClassCastException.class},noRollbackFor = {NullPointerException.class})
	public void test(){

		//当test()方法对应的事务在不同阶段可以执行程序员自己定义的逻辑
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void suspend() {
				//挂起
				TransactionSynchronization.super.suspend();
			}

			@Override
			public void resume() {
				//恢复
				TransactionSynchronization.super.resume();
			}

			@Override
			public void beforeCommit(boolean readOnly) {
				//提交事务之前
				TransactionSynchronization.super.beforeCommit(readOnly);
			}

			@Override
			public void afterCommit() {
				//提交事务之后
				TransactionSynchronization.super.afterCommit();
			}

			@Override
			public void beforeCompletion() {
				//提交事务或回滚之前
				TransactionSynchronization.super.beforeCompletion();
			}

			@Override
			public void afterCompletion(int status) {
				//提交事务或回滚之后
				TransactionSynchronization.super.afterCompletion(status);
			}
		});


		userMapper.insert1();

		try {
			userService.a();
		} catch (Exception e){

		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void a(){
		userMapper.insert2();
		throw new NullPointerException();
	}

}
