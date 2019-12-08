/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.ObjectUtils;

/**
 * Inner class that implements a Pointcut that matches if the underlying
 * {@link TransactionAttributeSource} has an attribute for a given method.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 */
@SuppressWarnings("serial")
abstract class TransactionAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

	/**
	 * 一般是{@link org.springframework.aop.support.AopUtils#canApply(Pointcut, Class, boolean)}这个方法转入这里来
	 * @param method the candidate method
	 * @param targetClass targetClass代表beanClass，当然有可能传入的是null
	 * the target class (may be {@code null}, in which case
	 * the candidate class must be taken to be the method's declaring class)
	 * @return
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		//targetClass如果实现了TransactionalProxy接口，则返回false
		if (targetClass != null && TransactionalProxy.class.isAssignableFrom(targetClass)) {
			return false;
		}
		//这里一般是返回AnnotationTransactionAttributeSource或者NameMatchTransactionAttributeSource
		TransactionAttributeSource tas = getTransactionAttributeSource();
		//一般会走到AbstractFallbackTransactionAttributeSource#getTransactionAttribute()方法里面
		//其实就是扫描class或者method上面的事务配置（比如@Transactional），只要扫描到了就返回true
		return (tas == null || tas.getTransactionAttribute(method, targetClass) != null);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TransactionAttributeSourcePointcut)) {
			return false;
		}
		TransactionAttributeSourcePointcut otherPc = (TransactionAttributeSourcePointcut) other;
		return ObjectUtils.nullSafeEquals(getTransactionAttributeSource(), otherPc.getTransactionAttributeSource());
	}

	@Override
	public int hashCode() {
		return TransactionAttributeSourcePointcut.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + getTransactionAttributeSource();
	}


	/**
	 * Obtain the underlying TransactionAttributeSource (may be {@code null}).
	 * To be implemented by subclasses.
	 */
	protected abstract TransactionAttributeSource getTransactionAttributeSource();

}
