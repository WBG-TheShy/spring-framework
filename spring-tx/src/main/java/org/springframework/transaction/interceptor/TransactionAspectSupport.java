/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import io.vavr.control.Try;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.reactive.AwaitKt;
import kotlinx.coroutines.reactive.ReactiveFlowKt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.CoroutinesUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.reactive.TransactionContextManager;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * Base class for transactional aspects, such as the {@link TransactionInterceptor}
 * or an AspectJ aspect.
 *
 * <p>This enables the underlying Spring transaction infrastructure to be used easily
 * to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling methods in this class in the correct order.
 *
 * <p>If no transaction name has been specified in the {@link TransactionAttribute},
 * the exposed name will be the {@code fully-qualified class name + "." + method name}
 * (by default).
 *
 * <p>Uses the <b>Strategy</b> design pattern. A {@link PlatformTransactionManager} or
 * {@link ReactiveTransactionManager} implementation will perform the actual transaction
 * management, and a {@link TransactionAttributeSource} (e.g. annotation-based) is used
 * for determining transaction definitions for a particular class or method.
 *
 * <p>A transaction aspect is serializable if its {@code TransactionManager} and
 * {@code TransactionAttributeSource} are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stéphane Nicoll
 * @author Sam Brannen
 * @author Mark Paluch
 * @author Sebastien Deleuze
 * @since 1.1
 * @see PlatformTransactionManager
 * @see ReactiveTransactionManager
 * @see #setTransactionManager
 * @see #setTransactionAttributes
 * @see #setTransactionAttributeSource
 */
public abstract class TransactionAspectSupport implements BeanFactoryAware, InitializingBean {

	// NOTE: This class must not implement Serializable because it serves as base
	// class for AspectJ aspects (which are not allowed to implement Serializable)!


	/**
	 * Key to use to store the default transaction manager.
	 */
	private static final Object DEFAULT_TRANSACTION_MANAGER_KEY = new Object();

	private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";

	/**
	 * Vavr library present on the classpath?
	 */
	private static final boolean vavrPresent = ClassUtils.isPresent(
			"io.vavr.control.Try", TransactionAspectSupport.class.getClassLoader());

	/**
	 * Reactive Streams API present on the classpath?
	 */
	private static final boolean reactiveStreamsPresent =
			ClassUtils.isPresent("org.reactivestreams.Publisher", TransactionAspectSupport.class.getClassLoader());

	/**
	 * Holder to support the {@code currentTransactionStatus()} method,
	 * and to support communication between different cooperating advices
	 * (e.g. before and after advice) if the aspect involves more than a
	 * single method (as will be the case for around advice).
	 */
	private static final ThreadLocal<TransactionInfo> transactionInfoHolder =
			new NamedThreadLocal<>("Current aspect-driven transaction");


	/**
	 * Subclasses can use this to return the current TransactionInfo.
	 * Only subclasses that cannot handle all operations in one method,
	 * such as an AspectJ aspect involving distinct before and after advice,
	 * need to use this mechanism to get at the current TransactionInfo.
	 * An around advice such as an AOP Alliance MethodInterceptor can hold a
	 * reference to the TransactionInfo throughout the aspect method.
	 * <p>A TransactionInfo will be returned even if no transaction was created.
	 * The {@code TransactionInfo.hasTransaction()} method can be used to query this.
	 * <p>To find out about specific transaction characteristics, consider using
	 * TransactionSynchronizationManager's {@code isSynchronizationActive()}
	 * and/or {@code isActualTransactionActive()} methods.
	 * @return the TransactionInfo bound to this thread, or {@code null} if none
	 * @see TransactionInfo#hasTransaction()
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isSynchronizationActive()
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
	 */
	@Nullable
	protected static TransactionInfo currentTransactionInfo() throws NoTransactionException {
		return transactionInfoHolder.get();
	}

	/**
	 * Return the transaction status of the current method invocation.
	 * Mainly intended for code that wants to set the current transaction
	 * rollback-only but not throw an application exception.
	 * @throws NoTransactionException if the transaction info cannot be found,
	 * because the method was invoked outside an AOP invocation context
	 */
	public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
		TransactionInfo info = currentTransactionInfo();
		if (info == null || info.transactionStatus == null) {
			throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
		}
		return info.transactionStatus;
	}


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private final ReactiveAdapterRegistry reactiveAdapterRegistry;

	@Nullable
	private String transactionManagerBeanName;

	@Nullable
	private TransactionManager transactionManager;

	@Nullable
	private TransactionAttributeSource transactionAttributeSource;

	@Nullable
	private BeanFactory beanFactory;

	private final ConcurrentMap<Object, TransactionManager> transactionManagerCache =
			new ConcurrentReferenceHashMap<>(4);

	private final ConcurrentMap<Method, ReactiveTransactionSupport> transactionSupportCache =
			new ConcurrentReferenceHashMap<>(1024);


	protected TransactionAspectSupport() {
		if (reactiveStreamsPresent) {
			this.reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		}
		else {
			this.reactiveAdapterRegistry = null;
		}
	}


	/**
	 * Specify the name of the default transaction manager bean.
	 * <p>This can either point to a traditional {@link PlatformTransactionManager} or a
	 * {@link ReactiveTransactionManager} for reactive transaction management.
	 */
	public void setTransactionManagerBeanName(@Nullable String transactionManagerBeanName) {
		this.transactionManagerBeanName = transactionManagerBeanName;
	}

	/**
	 * Return the name of the default transaction manager bean.
	 */
	@Nullable
	protected final String getTransactionManagerBeanName() {
		return this.transactionManagerBeanName;
	}

	/**
	 * Specify the <em>default</em> transaction manager to use to drive transactions.
	 * <p>This can either be a traditional {@link PlatformTransactionManager} or a
	 * {@link ReactiveTransactionManager} for reactive transaction management.
	 * <p>The default transaction manager will be used if a <em>qualifier</em>
	 * has not been declared for a given transaction or if an explicit name for the
	 * default transaction manager bean has not been specified.
	 * @see #setTransactionManagerBeanName
	 */
	public void setTransactionManager(@Nullable TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Return the default transaction manager, or {@code null} if unknown.
	 * <p>This can either be a traditional {@link PlatformTransactionManager} or a
	 * {@link ReactiveTransactionManager} for reactive transaction management.
	 */
	@Nullable
	public TransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * Set properties with method names as keys and transaction attribute
	 * descriptors (parsed via TransactionAttributeEditor) as values:
	 * e.g. key = "myMethod", value = "PROPAGATION_REQUIRED,readOnly".
	 * <p>Note: Method names are always applied to the target class,
	 * no matter if defined in an interface or the class itself.
	 * <p>Internally, a NameMatchTransactionAttributeSource will be
	 * created from the given properties.
	 * @see #setTransactionAttributeSource
	 * @see TransactionAttributeEditor
	 * @see NameMatchTransactionAttributeSource
	 */
	public void setTransactionAttributes(Properties transactionAttributes) {
		NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
		tas.setProperties(transactionAttributes);
		this.transactionAttributeSource = tas;
	}

	/**
	 * Set multiple transaction attribute sources which are used to find transaction
	 * attributes. Will build a CompositeTransactionAttributeSource for the given sources.
	 * @see CompositeTransactionAttributeSource
	 * @see MethodMapTransactionAttributeSource
	 * @see NameMatchTransactionAttributeSource
	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
	 */
	public void setTransactionAttributeSources(TransactionAttributeSource... transactionAttributeSources) {
		this.transactionAttributeSource = new CompositeTransactionAttributeSource(transactionAttributeSources);
	}

	/**
	 * Set the transaction attribute source which is used to find transaction
	 * attributes. If specifying a String property value, a PropertyEditor
	 * will create a MethodMapTransactionAttributeSource from the value.
	 * @see TransactionAttributeSourceEditor
	 * @see MethodMapTransactionAttributeSource
	 * @see NameMatchTransactionAttributeSource
	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
	 */
	public void setTransactionAttributeSource(@Nullable TransactionAttributeSource transactionAttributeSource) {
		this.transactionAttributeSource = transactionAttributeSource;
	}

	/**
	 * Return the transaction attribute source.
	 */
	@Nullable
	public TransactionAttributeSource getTransactionAttributeSource() {
		return this.transactionAttributeSource;
	}

	/**
	 * Set the BeanFactory to use for retrieving {@code TransactionManager} beans.
	 */
	@Override
	public void setBeanFactory(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the BeanFactory to use for retrieving {@code TransactionManager} beans.
	 */
	@Nullable
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Check that required properties were set.
	 */
	@Override
	public void afterPropertiesSet() {
		if (getTransactionManager() == null && this.beanFactory == null) {
			throw new IllegalStateException(
					"Set the 'transactionManager' property or make sure to run within a BeanFactory " +
					"containing a TransactionManager bean!");
		}
		if (getTransactionAttributeSource() == null) {
			throw new IllegalStateException(
					"Either 'transactionAttributeSource' or 'transactionAttributes' is required: " +
					"If there are no transactional methods, then don't use a transaction aspect.");
		}
	}


	/**
	 * General delegate for around-advice-based subclasses, delegating to several other template
	 * methods on this class. Able to handle {@link CallbackPreferringPlatformTransactionManager}
	 * as well as regular {@link PlatformTransactionManager} implementations and
	 * {@link ReactiveTransactionManager} implementations for reactive return types.
	 * @param method the Method being invoked
	 * @param targetClass the target class that we're invoking the method on
	 * @param invocation the callback to use for proceeding with the target invocation
	 * @return the return value of the method, if any
	 * @throws Throwable propagated from the target invocation
	 */
	@Nullable
	protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
			final InvocationCallback invocation) throws Throwable {

		// If the transaction attribute is null, the method is non-transactional.
		//TransactionAttributeSource是事务注册的三个bean的其中一个,可以理解为工具类
		TransactionAttributeSource tas = getTransactionAttributeSource();
		//获取@Transactional注解的属性值封装的对象-TransactionAttribute
		final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);

		//返回Spring容器中类型为TransactionManager的Bean对象(这个需要程序员自己配置)
		//创建数据库连接,提交,回滚等操作都是TransactionManager帮我们做的
		final TransactionManager tm = determineTransactionManager(txAttr);

		//一般不会用到这个ReactiveTransactionManager,所以不用看这个if
		if (this.reactiveAdapterRegistry != null && tm instanceof ReactiveTransactionManager) {
			boolean isSuspendingFunction = KotlinDetector.isSuspendingFunction(method);
			boolean hasSuspendingFlowReturnType = isSuspendingFunction &&
					COROUTINES_FLOW_CLASS_NAME.equals(new MethodParameter(method, -1).getParameterType().getName());
			if (isSuspendingFunction && !(invocation instanceof CoroutinesInvocationCallback)) {
				throw new IllegalStateException("Coroutines invocation not supported: " + method);
			}
			CoroutinesInvocationCallback corInv = (isSuspendingFunction ? (CoroutinesInvocationCallback) invocation : null);

			ReactiveTransactionSupport txSupport = this.transactionSupportCache.computeIfAbsent(method, key -> {
				Class<?> reactiveType =
						(isSuspendingFunction ? (hasSuspendingFlowReturnType ? Flux.class : Mono.class) : method.getReturnType());
				ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(reactiveType);
				if (adapter == null) {
					throw new IllegalStateException("Cannot apply reactive transaction to non-reactive return type: " +
							method.getReturnType());
				}
				return new ReactiveTransactionSupport(adapter);
			});

			InvocationCallback callback = invocation;
			if (corInv != null) {
				callback = () -> CoroutinesUtils.invokeSuspendingFunction(method, corInv.getTarget(), corInv.getArguments());
			}
			Object result = txSupport.invokeWithinTransaction(method, targetClass, callback, txAttr, (ReactiveTransactionManager) tm);
			if (corInv != null) {
				Publisher<?> pr = (Publisher<?>) result;
				return (hasSuspendingFlowReturnType ? KotlinDelegate.asFlow(pr) :
						KotlinDelegate.awaitSingleOrNull(pr, corInv.getContinuation()));
			}
			return result;
		}

		//将TransactionManager强制转换成PlatformTransactionManager,所以我们的bean对象类型是PlatformTransactionManager
		//意思是事务管理器的类型要不是ReactiveTransactionManager,要不是PlatformTransactionManager,否则就报错
		PlatformTransactionManager ptm = asPlatformTransactionManager(tm);

		//拿到当前执行方法的名字作为事务的名字(但其实没啥用)
		//事务的名字可以通过API-TransactionSynchronizationManager.getCurrentTransactionName()获取
		//当然,前提是得有事务,另外,也可以通过TransactionSynchronizationManager可以获取当前事务的隔离级别
		final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

		//CallbackPreferringPlatformTransactionManager表示拥有回调功能的PlatformTransactionManager,也不常用
		//所以大多数情况都会进入这个if
		if (txAttr == null || !(ptm instanceof CallbackPreferringPlatformTransactionManager)) {
			// Standard transaction demarcation with getTransaction and commit/rollback calls.
			//如果有必要,创建事务,涉及到事务的传播机制
			//TransactionInfo表示一个逻辑事务,多个逻辑事务可以同属于一个物理事务
			//TransactionInfo里有当前的事务,也有挂起对象
			TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);

			Object retVal;
			try {
				// This is an around advice: Invoke the next interceptor in the chain.
				// This will normally result in a target object being invoked.

				//执行下一个Interceptor或者被代理对象的方法
				//理解为执行程序员写的业务方法了
				retVal = invocation.proceedWithInvocation();
			}
			catch (Throwable ex) {
				// target invocation exception
				//如果抛了异常,则去回滚事务
				completeTransactionAfterThrowing(txInfo, ex);
				//再抛异常出去
				throw ex;
			}
			finally {
				cleanupTransactionInfo(txInfo);
			}

			if (retVal != null && vavrPresent && VavrDelegate.isVavrTry(retVal)) {
				// Set rollback-only in case of Vavr failure matching our rollback rules...
				TransactionStatus status = txInfo.getTransactionStatus();
				if (status != null && txAttr != null) {
					retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
				}
			}

			//提交事务
			//提交事务的前提是当前的事务是新建的独立事务
			//如果是新建的事务(例如:REQUIRES_NEW),则要关闭数据库连接,然后提交事务,然后恢复被挂起的资源
			//如果不是新建的事务,仅仅只恢复被挂起的资源
			commitTransactionAfterReturning(txInfo);
			return retVal;
		}

		else {
			Object result;
			final ThrowableHolder throwableHolder = new ThrowableHolder();

			// It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
			try {
				result = ((CallbackPreferringPlatformTransactionManager) ptm).execute(txAttr, status -> {
					TransactionInfo txInfo = prepareTransactionInfo(ptm, txAttr, joinpointIdentification, status);
					try {
						Object retVal = invocation.proceedWithInvocation();
						if (retVal != null && vavrPresent && VavrDelegate.isVavrTry(retVal)) {
							// Set rollback-only in case of Vavr failure matching our rollback rules...
							retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
						}
						return retVal;
					}
					catch (Throwable ex) {
						if (txAttr.rollbackOn(ex)) {
							// A RuntimeException: will lead to a rollback.
							if (ex instanceof RuntimeException) {
								throw (RuntimeException) ex;
							}
							else {
								throw new ThrowableHolderException(ex);
							}
						}
						else {
							// A normal return value: will lead to a commit.
							throwableHolder.throwable = ex;
							return null;
						}
					}
					finally {
						cleanupTransactionInfo(txInfo);
					}
				});
			}
			catch (ThrowableHolderException ex) {
				throw ex.getCause();
			}
			catch (TransactionSystemException ex2) {
				if (throwableHolder.throwable != null) {
					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
					ex2.initApplicationException(throwableHolder.throwable);
				}
				throw ex2;
			}
			catch (Throwable ex2) {
				if (throwableHolder.throwable != null) {
					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
				}
				throw ex2;
			}

			// Check result state: It might indicate a Throwable to rethrow.
			if (throwableHolder.throwable != null) {
				throw throwableHolder.throwable;
			}
			return result;
		}
	}

	/**
	 * Clear the transaction manager cache.
	 */
	protected void clearTransactionManagerCache() {
		this.transactionManagerCache.clear();
		this.beanFactory = null;
	}

	/**
	 * Determine the specific transaction manager to use for the given transaction.
	 */
	@Nullable
	protected TransactionManager determineTransactionManager(@Nullable TransactionAttribute txAttr) {
		// Do not attempt to lookup tx manager if no tx attributes are set
		if (txAttr == null || this.beanFactory == null) {
			return getTransactionManager();
		}

		String qualifier = txAttr.getQualifier();
		if (StringUtils.hasText(qualifier)) {
			return determineQualifiedTransactionManager(this.beanFactory, qualifier);
		}
		else if (StringUtils.hasText(this.transactionManagerBeanName)) {
			return determineQualifiedTransactionManager(this.beanFactory, this.transactionManagerBeanName);
		}
		else {
			TransactionManager defaultTransactionManager = getTransactionManager();
			if (defaultTransactionManager == null) {
				defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
				if (defaultTransactionManager == null) {
					//从Spring容器中获取TransactionManager类型的bean
					defaultTransactionManager = this.beanFactory.getBean(TransactionManager.class);
					//放入缓存中
					this.transactionManagerCache.putIfAbsent(
							DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
				}
			}
			return defaultTransactionManager;
		}
	}

	private TransactionManager determineQualifiedTransactionManager(BeanFactory beanFactory, String qualifier) {
		TransactionManager txManager = this.transactionManagerCache.get(qualifier);
		if (txManager == null) {
			txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
					beanFactory, TransactionManager.class, qualifier);
			this.transactionManagerCache.putIfAbsent(qualifier, txManager);
		}
		return txManager;
	}


	@Nullable
	private PlatformTransactionManager asPlatformTransactionManager(@Nullable Object transactionManager) {
		if (transactionManager == null || transactionManager instanceof PlatformTransactionManager) {
			return (PlatformTransactionManager) transactionManager;
		}
		else {
			throw new IllegalStateException(
					"Specified transaction manager is not a PlatformTransactionManager: " + transactionManager);
		}
	}

	private String methodIdentification(Method method, @Nullable Class<?> targetClass,
			@Nullable TransactionAttribute txAttr) {

		String methodIdentification = methodIdentification(method, targetClass);
		if (methodIdentification == null) {
			if (txAttr instanceof DefaultTransactionAttribute) {
				methodIdentification = ((DefaultTransactionAttribute) txAttr).getDescriptor();
			}
			if (methodIdentification == null) {
				methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
			}
		}
		return methodIdentification;
	}

	/**
	 * Convenience method to return a String representation of this Method
	 * for use in logging. Can be overridden in subclasses to provide a
	 * different identifier for the given method.
	 * <p>The default implementation returns {@code null}, indicating the
	 * use of {@link DefaultTransactionAttribute#getDescriptor()} instead,
	 * ending up as {@link ClassUtils#getQualifiedMethodName(Method, Class)}.
	 * @param method the method we're interested in
	 * @param targetClass the class that the method is being invoked on
	 * @return a String representation identifying this method
	 * @see org.springframework.util.ClassUtils#getQualifiedMethodName
	 */
	@Nullable
	protected String methodIdentification(Method method, @Nullable Class<?> targetClass) {
		return null;
	}

	/**
	 * Create a transaction if necessary based on the given TransactionAttribute.
	 * <p>Allows callers to perform custom TransactionAttribute lookups through
	 * the TransactionAttributeSource.
	 * @param txAttr the TransactionAttribute (may be {@code null})
	 * @param joinpointIdentification the fully qualified method name
	 * (used for monitoring and logging purposes)
	 * @return a TransactionInfo object, whether a transaction was created.
	 * The {@code hasTransaction()} method on TransactionInfo can be used to
	 * tell if there was a transaction created.
	 * @see #getTransactionAttributeSource()
	 */
	@SuppressWarnings("serial")
	protected TransactionInfo createTransactionIfNecessary(@Nullable PlatformTransactionManager tm,
			@Nullable TransactionAttribute txAttr, final String joinpointIdentification) {

		// If no name specified, apply method identification as transaction name.
		//如果事务的名字获取为空,则将当前方法的名字设置为事务的名字
		if (txAttr != null && txAttr.getName() == null) {
			txAttr = new DelegatingTransactionAttribute(txAttr) {
				@Override
				public String getName() {
					return joinpointIdentification;
				}
			};
		}

		//每一个逻辑事务都会创建一个TransactionStatus,但是TransactionStatus中有一个属性代表当前逻辑事务底层的物理事务是不是新的
		TransactionStatus status = null;
		if (txAttr != null) {
			if (tm != null) {
				//开启事务
				status = tm.getTransaction(txAttr);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping transactional joinpoint [" + joinpointIdentification +
							"] because no transaction manager has been configured");
				}
			}
		}
		//将TransactionStatus对象(其中就有挂起对象)和其他对象封装成一个TransactionInfo对象,表示得到一个事务,可能是新创建的事务,也可能是拿到已创建好的事务
		//tm为事务管理器
		//txAttr为当前业务方法上的@Transactional注解的属性值
		//joinpointIdentification是当前业务方法的名字
		//status是逻辑事务(里面有挂起的事务,如果没雨可以挂起的,则为null)
		return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
	}

	/**
	 * Prepare a TransactionInfo for the given attribute and status object.
	 * @param txAttr the TransactionAttribute (may be {@code null})
	 * @param joinpointIdentification the fully qualified method name
	 * (used for monitoring and logging purposes)
	 * @param status the TransactionStatus for the current transaction
	 * @return the prepared TransactionInfo object
	 */
	protected TransactionInfo prepareTransactionInfo(@Nullable PlatformTransactionManager tm,
			@Nullable TransactionAttribute txAttr, String joinpointIdentification,
			@Nullable TransactionStatus status) {

		TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
		if (txAttr != null) {
			// We need a transaction for this method...
			if (logger.isTraceEnabled()) {
				logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
			}
			// The transaction manager will flag an error if an incompatible tx already exists.
			//将挂起对象设置到txInfo对象里
			txInfo.newTransactionStatus(status);
		}
		else {
			// The TransactionInfo.hasTransaction() method will return false. We created it only
			// to preserve the integrity of the ThreadLocal stack maintained in this class.
			if (logger.isTraceEnabled()) {
				logger.trace("No need to create transaction for [" + joinpointIdentification +
						"]: This method is not transactional.");
			}
		}

		// We always bind the TransactionInfo to the thread, even if we didn't create
		// a new transaction here. This guarantees that the TransactionInfo stack
		// will be managed correctly even if no transaction was created by this aspect.

		//先把当前线程里的TransactionInfo(ThreadLocal<TransactionInfo>),也就是上一个事务信息拿到,设置到txInfo里的oldTransactionInfo中
		//当本次事务结束,需要恢复上一次事务的时候,就从这个属性拿
		//将当前的txInfo设置到当前线程里的TransactionInfo(ThreadLocal<TransactionInfo>)里
		txInfo.bindToThread();
		return txInfo;
	}

	/**
	 * Execute after successful completion of call, but not after an exception was handled.
	 * Do nothing if we didn't create a transaction.
	 * @param txInfo information about the current transaction
	 */
	protected void commitTransactionAfterReturning(@Nullable TransactionInfo txInfo) {
		if (txInfo != null && txInfo.getTransactionStatus() != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
			}
			//执行事务管理器的提交,传入transactionStatus对象
			txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
		}
	}

	/**
	 * Handle a throwable, completing the transaction.
	 * We may commit or roll back, depending on the configuration.
	 * @param txInfo information about the current transaction
	 * @param ex throwable encountered
	 */
	protected void completeTransactionAfterThrowing(@Nullable TransactionInfo txInfo, Throwable ex) {
		if (txInfo != null && txInfo.getTransactionStatus() != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() +
						"] after exception: " + ex);
			}
			//如果当前的异常在回滚范围内
			//例如:@Transactional(rollbackFor = {A.class,A1.class},noRollbackFor = {B.class,B1.class})
			//当前的异常是A或A1的子类 且 不是B或B1的子类,才会进行回滚
			if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
				try {
					//进行回滚
					txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
				}
				catch (TransactionSystemException ex2) {
					logger.error("Application exception overridden by rollback exception", ex);
					ex2.initApplicationException(ex);
					throw ex2;
				}
				catch (RuntimeException | Error ex2) {
					logger.error("Application exception overridden by rollback exception", ex);
					throw ex2;
				}
			}

			//如果当前的异常不在回滚范围内
			else {
				// We don't roll back on this exception.
				// Will still roll back if TransactionStatus.isRollbackOnly() is true.
				try {
					//那就提交
					txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
				}
				catch (TransactionSystemException ex2) {
					logger.error("Application exception overridden by commit exception", ex);
					ex2.initApplicationException(ex);
					throw ex2;
				}
				catch (RuntimeException | Error ex2) {
					logger.error("Application exception overridden by commit exception", ex);
					throw ex2;
				}
			}
		}
	}

	/**
	 * Reset the TransactionInfo ThreadLocal.
	 * <p>Call this in all cases: exception or normal return!
	 * @param txInfo information about the current transaction (may be {@code null})
	 */
	protected void cleanupTransactionInfo(@Nullable TransactionInfo txInfo) {
		if (txInfo != null) {
			txInfo.restoreThreadLocalStatus();
		}
	}


	/**
	 * Opaque object used to hold transaction information. Subclasses
	 * must pass it back to methods on this class, but not see its internals.
	 */
	protected static final class TransactionInfo {

		@Nullable
		private final PlatformTransactionManager transactionManager;

		@Nullable
		private final TransactionAttribute transactionAttribute;

		private final String joinpointIdentification;

		@Nullable
		private TransactionStatus transactionStatus;

		@Nullable
		private TransactionInfo oldTransactionInfo;

		public TransactionInfo(@Nullable PlatformTransactionManager transactionManager,
				@Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {

			this.transactionManager = transactionManager;
			this.transactionAttribute = transactionAttribute;
			this.joinpointIdentification = joinpointIdentification;
		}

		public PlatformTransactionManager getTransactionManager() {
			Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
			return this.transactionManager;
		}

		@Nullable
		public TransactionAttribute getTransactionAttribute() {
			return this.transactionAttribute;
		}

		/**
		 * Return a String representation of this joinpoint (usually a Method call)
		 * for use in logging.
		 */
		public String getJoinpointIdentification() {
			return this.joinpointIdentification;
		}

		public void newTransactionStatus(@Nullable TransactionStatus status) {
			this.transactionStatus = status;
		}

		@Nullable
		public TransactionStatus getTransactionStatus() {
			return this.transactionStatus;
		}

		/**
		 * Return whether a transaction was created by this aspect,
		 * or whether we just have a placeholder to keep ThreadLocal stack integrity.
		 */
		public boolean hasTransaction() {
			return (this.transactionStatus != null);
		}

		private void bindToThread() {
			// Expose current TransactionStatus, preserving any existing TransactionStatus
			// for restoration after this transaction is complete.
			this.oldTransactionInfo = transactionInfoHolder.get();
			transactionInfoHolder.set(this);
		}

		private void restoreThreadLocalStatus() {
			// Use stack to restore old transaction TransactionInfo.
			// Will be null if none was set.
			transactionInfoHolder.set(this.oldTransactionInfo);
		}

		@Override
		public String toString() {
			return (this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction");
		}
	}


	/**
	 * Simple callback interface for proceeding with the target invocation.
	 * Concrete interceptors/aspects adapt this to their invocation mechanism.
	 */
	@FunctionalInterface
	protected interface InvocationCallback {

		@Nullable
		Object proceedWithInvocation() throws Throwable;
	}


	/**
	 * Coroutines-supporting extension of the callback interface.
	 */
	protected interface CoroutinesInvocationCallback extends InvocationCallback {

		Object getTarget();

		Object[] getArguments();

		default Object getContinuation() {
			Object[] args = getArguments();
			return args[args.length - 1];
		}
	}


	/**
	 * Internal holder class for a Throwable in a callback transaction model.
	 */
	private static class ThrowableHolder {

		@Nullable
		public Throwable throwable;
	}


	/**
	 * Internal holder class for a Throwable, used as a RuntimeException to be
	 * thrown from a TransactionCallback (and subsequently unwrapped again).
	 */
	@SuppressWarnings("serial")
	private static class ThrowableHolderException extends RuntimeException {

		public ThrowableHolderException(Throwable throwable) {
			super(throwable);
		}

		@Override
		public String toString() {
			return getCause().toString();
		}
	}


	/**
	 * Inner class to avoid a hard dependency on the Vavr library at runtime.
	 */
	private static class VavrDelegate {

		public static boolean isVavrTry(Object retVal) {
			return (retVal instanceof Try);
		}

		public static Object evaluateTryFailure(Object retVal, TransactionAttribute txAttr, TransactionStatus status) {
			return ((Try<?>) retVal).onFailure(ex -> {
				if (txAttr.rollbackOn(ex)) {
					status.setRollbackOnly();
				}
			});
		}
	}

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		private static Object asFlow(Publisher<?> publisher) {
			return ReactiveFlowKt.asFlow(publisher);
		}

		@SuppressWarnings({"unchecked", "deprecation"})
		@Nullable
		private static Object awaitSingleOrNull(Publisher<?> publisher, Object continuation) {
			return AwaitKt.awaitSingleOrNull(publisher, (Continuation<Object>) continuation);
		}
	}


	/**
	 * Delegate for Reactor-based management of transactional methods with a
	 * reactive return type.
	 */
	private class ReactiveTransactionSupport {

		private final ReactiveAdapter adapter;

		public ReactiveTransactionSupport(ReactiveAdapter adapter) {
			this.adapter = adapter;
		}

		public Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
				InvocationCallback invocation, @Nullable TransactionAttribute txAttr, ReactiveTransactionManager rtm) {

			String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

			// For Mono and suspending functions not returning kotlinx.coroutines.flow.Flow
			if (Mono.class.isAssignableFrom(method.getReturnType()) || (KotlinDetector.isSuspendingFunction(method) &&
					!COROUTINES_FLOW_CLASS_NAME.equals(new MethodParameter(method, -1).getParameterType().getName()))) {

				return TransactionContextManager.currentContext().flatMap(context ->
						createTransactionIfNecessary(rtm, txAttr, joinpointIdentification).flatMap(it -> {
							try {
								// Need re-wrapping until we get hold of the exception through usingWhen.
								return Mono.<Object, ReactiveTransactionInfo>usingWhen(
										Mono.just(it),
										txInfo -> {
											try {
												return (Mono<?>) invocation.proceedWithInvocation();
											}
											catch (Throwable ex) {
												return Mono.error(ex);
											}
										},
										this::commitTransactionAfterReturning,
										(txInfo, err) -> Mono.empty(),
										this::rollbackTransactionOnCancel)
										.onErrorResume(ex ->
												completeTransactionAfterThrowing(it, ex).then(Mono.error(ex)));
							}
							catch (Throwable ex) {
								// target invocation exception
								return completeTransactionAfterThrowing(it, ex).then(Mono.error(ex));
							}
						})).contextWrite(TransactionContextManager.getOrCreateContext())
						.contextWrite(TransactionContextManager.getOrCreateContextHolder());
			}

			// Any other reactive type, typically a Flux
			return this.adapter.fromPublisher(TransactionContextManager.currentContext().flatMapMany(context ->
					createTransactionIfNecessary(rtm, txAttr, joinpointIdentification).flatMapMany(it -> {
						try {
							// Need re-wrapping until we get hold of the exception through usingWhen.
							return Flux
									.usingWhen(
											Mono.just(it),
											txInfo -> {
												try {
													return this.adapter.toPublisher(invocation.proceedWithInvocation());
												}
												catch (Throwable ex) {
													return Mono.error(ex);
												}
											},
											this::commitTransactionAfterReturning,
											(txInfo, ex) -> Mono.empty(),
											this::rollbackTransactionOnCancel)
									.onErrorResume(ex ->
											completeTransactionAfterThrowing(it, ex).then(Mono.error(ex)));
						}
						catch (Throwable ex) {
							// target invocation exception
							return completeTransactionAfterThrowing(it, ex).then(Mono.error(ex));
						}
					})).contextWrite(TransactionContextManager.getOrCreateContext())
					.contextWrite(TransactionContextManager.getOrCreateContextHolder()));
		}

		@SuppressWarnings("serial")
		private Mono<ReactiveTransactionInfo> createTransactionIfNecessary(ReactiveTransactionManager tm,
				@Nullable TransactionAttribute txAttr, final String joinpointIdentification) {

			// If no name specified, apply method identification as transaction name.
			if (txAttr != null && txAttr.getName() == null) {
				txAttr = new DelegatingTransactionAttribute(txAttr) {
					@Override
					public String getName() {
						return joinpointIdentification;
					}
				};
			}

			final TransactionAttribute attrToUse = txAttr;
			Mono<ReactiveTransaction> tx = (attrToUse != null ? tm.getReactiveTransaction(attrToUse) : Mono.empty());
			return tx.map(it -> prepareTransactionInfo(tm, attrToUse, joinpointIdentification, it)).switchIfEmpty(
					Mono.defer(() -> Mono.just(prepareTransactionInfo(tm, attrToUse, joinpointIdentification, null))));
		}

		private ReactiveTransactionInfo prepareTransactionInfo(@Nullable ReactiveTransactionManager tm,
				@Nullable TransactionAttribute txAttr, String joinpointIdentification,
				@Nullable ReactiveTransaction transaction) {

			ReactiveTransactionInfo txInfo = new ReactiveTransactionInfo(tm, txAttr, joinpointIdentification);
			if (txAttr != null) {
				// We need a transaction for this method...
				if (logger.isTraceEnabled()) {
					logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
				}
				// The transaction manager will flag an error if an incompatible tx already exists.
				txInfo.newReactiveTransaction(transaction);
			}
			else {
				// The TransactionInfo.hasTransaction() method will return false. We created it only
				// to preserve the integrity of the ThreadLocal stack maintained in this class.
				if (logger.isTraceEnabled()) {
					logger.trace("Don't need to create transaction for [" + joinpointIdentification +
							"]: This method isn't transactional.");
				}
			}

			return txInfo;
		}

		private Mono<Void> commitTransactionAfterReturning(@Nullable ReactiveTransactionInfo txInfo) {
			if (txInfo != null && txInfo.getReactiveTransaction() != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
				}
				return txInfo.getTransactionManager().commit(txInfo.getReactiveTransaction());
			}
			return Mono.empty();
		}

		private Mono<Void> rollbackTransactionOnCancel(@Nullable ReactiveTransactionInfo txInfo) {
			if (txInfo != null && txInfo.getReactiveTransaction() != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Rolling back transaction for [" + txInfo.getJoinpointIdentification() + "] after cancellation");
				}
				return txInfo.getTransactionManager().rollback(txInfo.getReactiveTransaction());
			}
			return Mono.empty();
		}

		private Mono<Void> completeTransactionAfterThrowing(@Nullable ReactiveTransactionInfo txInfo, Throwable ex) {
			if (txInfo != null && txInfo.getReactiveTransaction() != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() +
							"] after exception: " + ex);
				}
				if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
					return txInfo.getTransactionManager().rollback(txInfo.getReactiveTransaction()).onErrorMap(ex2 -> {
								logger.error("Application exception overridden by rollback exception", ex);
								if (ex2 instanceof TransactionSystemException) {
									((TransactionSystemException) ex2).initApplicationException(ex);
								}
								return ex2;
							}
					);
				}
				else {
					// We don't roll back on this exception.
					// Will still roll back if TransactionStatus.isRollbackOnly() is true.
					return txInfo.getTransactionManager().commit(txInfo.getReactiveTransaction()).onErrorMap(ex2 -> {
								logger.error("Application exception overridden by commit exception", ex);
								if (ex2 instanceof TransactionSystemException) {
									((TransactionSystemException) ex2).initApplicationException(ex);
								}
								return ex2;
							}
					);
				}
			}
			return Mono.empty();
		}
	}


	/**
	 * Opaque object used to hold transaction information for reactive methods.
	 */
	private static final class ReactiveTransactionInfo {

		@Nullable
		private final ReactiveTransactionManager transactionManager;

		@Nullable
		private final TransactionAttribute transactionAttribute;

		private final String joinpointIdentification;

		@Nullable
		private ReactiveTransaction reactiveTransaction;

		public ReactiveTransactionInfo(@Nullable ReactiveTransactionManager transactionManager,
				@Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {

			this.transactionManager = transactionManager;
			this.transactionAttribute = transactionAttribute;
			this.joinpointIdentification = joinpointIdentification;
		}

		public ReactiveTransactionManager getTransactionManager() {
			Assert.state(this.transactionManager != null, "No ReactiveTransactionManager set");
			return this.transactionManager;
		}

		@Nullable
		public TransactionAttribute getTransactionAttribute() {
			return this.transactionAttribute;
		}

		/**
		 * Return a String representation of this joinpoint (usually a Method call)
		 * for use in logging.
		 */
		public String getJoinpointIdentification() {
			return this.joinpointIdentification;
		}

		public void newReactiveTransaction(@Nullable ReactiveTransaction transaction) {
			this.reactiveTransaction = transaction;
		}

		@Nullable
		public ReactiveTransaction getReactiveTransaction() {
			return this.reactiveTransaction;
		}

		@Override
		public String toString() {
			return (this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction");
		}
	}

}
