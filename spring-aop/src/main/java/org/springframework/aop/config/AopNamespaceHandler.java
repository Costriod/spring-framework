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

package org.springframework.aop.config;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} for the {@code aop} namespace.
 *
 * <p>Provides a {@link org.springframework.beans.factory.xml.BeanDefinitionParser} for the
 * {@code <aop:config>} tag. A {@code config} tag can include nested
 * {@code pointcut}, {@code advisor} and {@code aspect} tags.
 *
 * <p>The {@code pointcut} tag allows for creation of named
 * {@link AspectJExpressionPointcut} beans using a simple syntax:
 * <pre class="code">
 * &lt;aop:pointcut id=&quot;getNameCalls&quot; expression=&quot;execution(* *..ITestBean.getName(..))&quot;/&gt;
 * </pre>
 *
 * <p>Using the {@code advisor} tag you can configure an {@link org.springframework.aop.Advisor}
 * and have it applied to all relevant beans in you {@link org.springframework.beans.factory.BeanFactory}
 * automatically. The {@code advisor} tag supports both in-line and referenced
 * {@link org.springframework.aop.Pointcut Pointcuts}:
 *
 * <pre class="code">
 * &lt;aop:advisor id=&quot;getAgeAdvisor&quot;
 *     pointcut=&quot;execution(* *..ITestBean.getAge(..))&quot;
 *     advice-ref=&quot;getAgeCounter&quot;/&gt;
 *
 * &lt;aop:advisor id=&quot;getNameAdvisor&quot;
 *     pointcut-ref=&quot;getNameCalls&quot;
 *     advice-ref=&quot;getNameCounter&quot;/&gt;</pre>
 *
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AopNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * Register the {@link BeanDefinitionParser BeanDefinitionParsers} for the
	 * '{@code config}', '{@code spring-configured}', '{@code aspectj-autoproxy}'
	 * and '{@code scoped-proxy}' tags.
	 */
	@Override
	public void init() {
		// In 2.0 XSD as well as in 2.1 XSD.
		/**
		 * 一般我们配置aop使用的是下面的xml
		 * <aop:config>
		 *     <!-- 配置切入点 expression填写切入点表达式 -->
		 *     <!-- 双引号里面的内容必须写上execution前缀 -->
		 *     <aop:pointcut expression="execution(* com.*.*(..))" id="pointcut"/>
		 *     <!-- 配置切面 切面是切入点和通知的结合 -->
		 *     <aop:aspect ref="自定义增强Advise（无需实现Advice接口）">
		 *         <!-- <aop:aspect>标签内也可以配置pointcut -->
		 *         <aop:pointcut expression="execution(* com.*.*(..))" id="pointcut2"/>
		 *         <!-- <aop:aspect>标签内可以配置declare-parents标签，declare-parents是用来给bean增加额外方法的，不过需要注意一点delegate-ref和default-impl只能配置一个 -->
		 *         <aop:declare-parents types-matching="com.example.demo.*" implement-interface="com.example.demo.IBase" delegate-ref="base" default-impl="com.example.demo.BaseImpl" />
		 *
		 *         <aop:before method="before" pointcut-ref="pointcut"/>
		 *         <aop:around method="around" pointcut-ref="pointcut"/>
		 *         <aop:after-returning method="afterReturn" pointcut-ref="pointcut"/>
		 *         <aop:after-throwing method="afterException" pointcut-ref="pointcut"/>
		 *         <aop:around method="around" pointcut-ref="pointcut"/>
		 *     </aop:aspect>
		 *     <!-- 注意<aop:advisor> 不允许同时配置pointcut-ref或pointcut属性，只允许配置其中的一个 -->
		 *     <aop:advisor advice-ref="自定义增强Advisor（必须实现Advice接口）" pointcut-ref="pointcut">
		 *     </aop:advisor>
		 *     <aop:advisor advice-ref="自定义增强Advisor（必须实现Advice接口）" pointcut="execution(* com.*.*(..))">
		 *     </aop:advisor>
		 * </aop:config>
		 */
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());

		/**
		 * <aop:aspectj-autoproxy />声明自动为spring容器中那些配置@AspectJ切面的bean创建代理
		 */
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());

		/**
		 * <aop:scoped-proxy />是包裹在bean标签内部的，一般用法如下所示：
		 * <bean id="mybean" class="com.xxx.xxx" scope="prototype">
		 *     <aop:scoped-proxy />
		 * </bean>
		 */
		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// Only in 2.0 XSD: moved to context namespace as of 2.1
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}

}
