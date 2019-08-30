/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;

/**
 * Base class for {@link org.springframework.context.ApplicationContext}
 * implementations which are supposed to support multiple calls to {@link #refresh()},
 * creating a new internal bean factory instance every time.
 * Typically (but not necessarily), such a context will be driven by
 * a set of config locations to load bean definitions from.
 *
 * <p>The only method to be implemented by subclasses is {@link #loadBeanDefinitions},
 * which gets invoked on each refresh. A concrete implementation is supposed to load
 * bean definitions into the given
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory},
 * typically delegating to one or more specific bean definition readers.
 *
 * <p><b>Note that there is a similar base class for WebApplicationContexts.</b>
 * {@link org.springframework.web.context.support.AbstractRefreshableWebApplicationContext}
 * provides the same subclassing strategy, but additionally pre-implements
 * all context functionality for web environments. There is also a
 * pre-defined way to receive config locations for a web context.
 *
 * <p>Concrete standalone subclasses of this base class, reading in a
 * specific bean definition format, are {@link ClassPathXmlApplicationContext}
 * and {@link FileSystemXmlApplicationContext}, which both derive from the
 * common {@link AbstractXmlApplicationContext} base class;
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext}
 * supports {@code @Configuration}-annotated classes as a source of bean definitions.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.3
 * @see #loadBeanDefinitions
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
 * @see AbstractXmlApplicationContext
 * @see ClassPathXmlApplicationContext
 * @see FileSystemXmlApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {
	/**
	 * 是否允许beanDefinition被覆盖
	 */
	private Boolean allowBeanDefinitionOverriding;
	/**
	 * 是否允许循环依赖
	 */
	private Boolean allowCircularReferences;

	/** context创建完beanFactory之后，然后内部维持beanFactory强引用 */
	private DefaultListableBeanFactory beanFactory;

	/** 并发操作内部BeanFactory时使用到的锁 */
	private final Object beanFactoryMonitor = new Object();


	/**
	 * Create a new AbstractRefreshableApplicationContext with no parent.
	 */
	public AbstractRefreshableApplicationContext() {
	}

	/**
	 * Create a new AbstractRefreshableApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractRefreshableApplicationContext(ApplicationContext parent) {
		super(parent);
	}


	/**
	 * Set whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. Default is "true".
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Set whether to allow circular references between beans - and automatically
	 * try to resolve them.
	 * <p>Default is "true". Turn this off to throw an exception when encountering
	 * a circular reference, disallowing them completely.
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}


	/**
	 * 本方法才是context的refreshBeanFactory的实现，关闭之前已有的beanFactory（如果有的话），创建并初始化一个新的beanFactory
	 * <br>1.创建beanFactory，载入BeanDefinition并注册到beanFactory，别名也会注册进去
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		if (hasBeanFactory()) {//如果beanFactory不为null，也就是之前已有一个beanFactory
			destroyBeans();//销毁beanFactory所有的单例bean，子类可重写
			closeBeanFactory();//释放beanFactory，指向null让GC回收
		}
		try {
			//创建一个新的beanFactory
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			//id默认是类名+ @符号 + 16进制哈希码
			//设置serializationId，并且缓存到map，不过value是WeakReference
			beanFactory.setSerializationId(getId());
			//定制化beanFactory，子类可以自定义实现，默认是设置是否允许beanDefinition被覆盖、是否允许循环引用
			customizeBeanFactory(beanFactory);
			//抽象方法，一般是AbstractXmlApplicationContext#loadBeanDefinitions
			//解析xml里面的beanDefinition，并注册到beanFactory里面（别名也会注册进去）
			loadBeanDefinitions(beanFactory);
			synchronized (this.beanFactoryMonitor) {
				this.beanFactory = beanFactory;
			}
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		synchronized (this.beanFactoryMonitor) {
			if (this.beanFactory != null)
				this.beanFactory.setSerializationId(null);
		}
		super.cancelRefresh(ex);
	}

	/**
	 * 释放context内部的beanFactory，本方法在{@link #close()}方法中执行，必须等其他shutdown操作执行完之后才能接着执行closeBeanFactory()
	 * <p>如果本方法执行出了异常，则不应该抛出异常，应该直接log输出异常信息
	 */
	@Override
	protected final void closeBeanFactory() {
		synchronized (this.beanFactoryMonitor) {
			this.beanFactory.setSerializationId(null);
			this.beanFactory = null;
		}
	}

	/**
	 * 判断当前context是否内部持有了一个beanFactory对象
	 * i.e. has been refreshed at least once and not been closed yet.
	 */
	protected final boolean hasBeanFactory() {
		synchronized (this.beanFactoryMonitor) {
			return (this.beanFactory != null);
		}
	}

	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		synchronized (this.beanFactoryMonitor) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("BeanFactory not initialized or already closed - " +
						"call 'refresh' before accessing beans via the ApplicationContext");
			}
			return this.beanFactory;
		}
	}

	/**
	 * Overridden to turn it into a no-op: With AbstractRefreshableApplicationContext,
	 * {@link #getBeanFactory()} serves a strong assertion for an active context anyway.
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * 创建一个新的BeanFactory（refresh执行的时候），默认是{@link org.springframework.beans.factory.support.DefaultListableBeanFactory}，
	 * 并且创建DefaultListableBeanFactory时会把{@linkplain #getInternalParentBeanFactory() context自身原来的bean factory}作为入参传入进去，子类可重写该方法
	 * <b>需要注意这里的new DefaultListableBeanFactory的参数将会作为DefaultListableBeanFactory的parentFactory</b>
	 * @return bean factory，context会持有这个对象
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * 定制化context的bean factory（refresh执行的时候），本方法默认是给{@link DefaultListableBeanFactory}设置allowBeanDefinitionOverriding和allowCircularReferences，
	 * 前提条件是allowBeanDefinitionOverriding和allowCircularReferences不能为null，当然子类也可以扩展或覆盖父类实现
	 * @param beanFactory context新创建的beanFactory
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		if (this.allowBeanDefinitionOverriding != null) {
			beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		if (this.allowCircularReferences != null) {
			beanFactory.setAllowCircularReferences(this.allowCircularReferences);
		}
	}

	/**
	 * 抽象方法，由子类实现，载入所有beanDefinition到beanFactory，通过一个或多个beanDefinitionReader来执行载入动作
	 * @param beanFactory beanDefinition最终要载入到这个beanFactory
	 * @throws BeansException 如果beanDefinition解析失败则抛出该异常
	 * @throws IOException 如果beanDefinition文件读取失败则抛出该异常
	 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws BeansException, IOException;

}
