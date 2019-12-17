/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} allowing for the configuration of
 * declarative transaction management using either XML or using annotations.
 *
 * <p>This namespace handler is the central piece of functionality in the
 * Spring transaction management facilities and offers two approaches
 * to declaratively manage transactions.
 *
 * <p>One approach uses transaction semantics defined in XML using the
 * {@code <tx:advice>} elements, the other uses annotations
 * in combination with the {@code <tx:annotation-driven>} element.
 * Both approached are detailed to great extent in the Spring reference manual.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class TxNamespaceHandler extends NamespaceHandlerSupport {

	static final String TRANSACTION_MANAGER_ATTRIBUTE = "transaction-manager";

	static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";


	static String getTransactionManagerName(Element element) {
		return (element.hasAttribute(TRANSACTION_MANAGER_ATTRIBUTE) ?
				element.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE) : DEFAULT_TRANSACTION_MANAGER_BEAN_NAME);
	}


	@Override
	public void init() {
		/**
		 * <tx:advice>是用来手工配置事务切面的，用法如下：
		 * <bean id="transactionManager" class="org.springframework.orm.hibernate3.HibernateTransactionManager">
		 *     <property name="sessionFactory" ref="mySessionFactory" />
		 * </bean>
		 *
		 * <tx:advice id="myAdvice" transaction-manager="transactionManager">
		 *     <tx:attributes>
		 *         <tx:method name="select*" propagation="REQUIRED"/>
		 *         <tx:method name="delete*" propagation="REQUIRED"/>
		 *         <tx:method name="update*" propagation="REQUIRED"/>
		 *         <tx:method name="insert*" propagation="REQUIRED"/>
		 *     </tx:attributes>
		 * </tx:advice>
		 *
		 * <aop:config>
		 *     <aop:pointcut id="myPointcut" expression="execution(* com.example.demo.service.*.*(..))"/>
		 *     <aop:advisor pointcut-ref="myPointcut" advice-ref="myAdvice" />
		 * </aop:config>
		 */
		registerBeanDefinitionParser("advice", new TxAdviceBeanDefinitionParser());

		/**
		 * <tx:annotation-driven>是用来配置事务，这个是能够自动扫描@Transactional注解，并且对@Transactional注解标注的class自动生成代理对象
		 */
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenBeanDefinitionParser());
		registerBeanDefinitionParser("jta-transaction-manager", new JtaTransactionManagerBeanDefinitionParser());
	}

}
