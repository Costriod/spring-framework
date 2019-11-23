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

package org.springframework.transaction.config;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} for the {@code <tx:advice/>} tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @author Chris Beams
 * @since 2.0
 */
class TxAdviceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String METHOD_ELEMENT = "method";

	private static final String METHOD_NAME_ATTRIBUTE = "name";

	private static final String ATTRIBUTES_ELEMENT = "attributes";

	private static final String TIMEOUT_ATTRIBUTE = "timeout";

	private static final String READ_ONLY_ATTRIBUTE = "read-only";

	private static final String PROPAGATION_ATTRIBUTE = "propagation";

	private static final String ISOLATION_ATTRIBUTE = "isolation";

	private static final String ROLLBACK_FOR_ATTRIBUTE = "rollback-for";

	private static final String NO_ROLLBACK_FOR_ATTRIBUTE = "no-rollback-for";

	/**
	 * 父类执行parseInternal的时候，会执行这个getBeanClass方法，也就是最终会生成一个beanDefinition，class就是本方法返回的
	 * @param element the {@code Element} that is being parsed
	 * @return
	 */
	@Override
	protected Class<?> getBeanClass(Element element) {
		return TransactionInterceptor.class;
	}

	/**
	 * 父类执行parseInternal的最后一段代码会执行doParse方法，最终会转入这里来
	 * @param element the XML element being parsed
	 * @param parserContext the object encapsulating the current state of the parsing process
	 * @param builder used to define the {@code BeanDefinition}
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		//这里就是解析xml的transaction-manager属性，如果没设置这个属性，则使用默认值transactionManager
		builder.addPropertyReference("transactionManager", TxNamespaceHandler.getTransactionManagerName(element));
		//读取<tx:advice>标签内嵌的<tx:attributes>标签，最多允许一个内嵌的<tx:attributes>标签
		List<Element> txAttributes = DomUtils.getChildElementsByTagName(element, ATTRIBUTES_ELEMENT);
		if (txAttributes.size() > 1) {
			parserContext.getReaderContext().error(
					"Element <attributes> is allowed at most once inside element <advice>", element);
		}
		else if (txAttributes.size() == 1) {//如果解析了一个<tx:attributes>标签
			// Using attributes source.
			Element attributeSourceElement = txAttributes.get(0);
			//解析<tx:attributes>标签内部的参数，生成一个NameMatchTransactionAttributeSource的BeanDefinition
			RootBeanDefinition attributeSourceDefinition = parseAttributeSource(attributeSourceElement, parserContext);
			builder.addPropertyValue("transactionAttributeSource", attributeSourceDefinition);
		}
		else {//如果没有配置<tx:attributes>标签，则使用默认的AnnotationTransactionAttributeSource
			// Assume annotations source.
			builder.addPropertyValue("transactionAttributeSource",
					new RootBeanDefinition("org.springframework.transaction.annotation.AnnotationTransactionAttributeSource"));
		}
	}

	/**
	 * 解析<tx:attributes>标签内部的参数信息，生成一个NameMatchTransactionAttributeSource的BeanDefinition
	 * @param attrEle
	 * @param parserContext
	 * @return
	 */
	private RootBeanDefinition parseAttributeSource(Element attrEle, ParserContext parserContext) {
		//获取里面所有的<tx:method>标签
		List<Element> methods = DomUtils.getChildElementsByTagName(attrEle, METHOD_ELEMENT);
		//存储<tx:method>标签的属性信息，key是<tx:method>标签的name，value是<tx:method>标签的其他属性值
		ManagedMap<TypedStringValue, RuleBasedTransactionAttribute> transactionAttributeMap =
			new ManagedMap<TypedStringValue, RuleBasedTransactionAttribute>(methods.size());
		transactionAttributeMap.setSource(parserContext.extractSource(attrEle));

		for (Element methodEle : methods) {
			String name = methodEle.getAttribute(METHOD_NAME_ATTRIBUTE);//解析<tx:method>标签的name属性
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(methodEle));

			RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
			String propagation = methodEle.getAttribute(PROPAGATION_ATTRIBUTE);//解析<tx:method>标签的propagation属性，这个对应事务传播级别
			String isolation = methodEle.getAttribute(ISOLATION_ATTRIBUTE);//解析<tx:method>标签的isolation属性，这个对应事务隔离级别
			String timeout = methodEle.getAttribute(TIMEOUT_ATTRIBUTE);//解析<tx:method>标签的timeout属性，这个对应事务超时时间
			String readOnly = methodEle.getAttribute(READ_ONLY_ATTRIBUTE);//解析<tx:method>标签的read-only属性
			if (StringUtils.hasText(propagation)) {//如果有propagation属性，设置propagation的值
				attribute.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
			}
			if (StringUtils.hasText(isolation)) {
				attribute.setIsolationLevelName(RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
			}
			if (StringUtils.hasText(timeout)) {
				try {
					attribute.setTimeout(Integer.parseInt(timeout));
				}
				catch (NumberFormatException ex) {
					parserContext.getReaderContext().error("Timeout must be an integer value: [" + timeout + "]", methodEle);
				}
			}
			if (StringUtils.hasText(readOnly)) {
				attribute.setReadOnly(Boolean.valueOf(methodEle.getAttribute(READ_ONLY_ATTRIBUTE)));
			}

			List<RollbackRuleAttribute> rollbackRules = new LinkedList<RollbackRuleAttribute>();
			//解析<tx:method>标签的rollback-for属性，属性值是Exception，多个Exception用“,”隔开，表示事务执行期间捕获到这些异常则回滚事务
			if (methodEle.hasAttribute(ROLLBACK_FOR_ATTRIBUTE)) {
				String rollbackForValue = methodEle.getAttribute(ROLLBACK_FOR_ATTRIBUTE);
				addRollbackRuleAttributesTo(rollbackRules,rollbackForValue);
			}
			//解析<tx:method>标签的no-rollback-for属性，属性值是Exception，多个Exception用“,”隔开，表示事务执行期间捕获到这些异常则不用回滚事务
			if (methodEle.hasAttribute(NO_ROLLBACK_FOR_ATTRIBUTE)) {
				String noRollbackForValue = methodEle.getAttribute(NO_ROLLBACK_FOR_ATTRIBUTE);
				addNoRollbackRuleAttributesTo(rollbackRules,noRollbackForValue);
			}
			attribute.setRollbackRules(rollbackRules);

			transactionAttributeMap.put(nameHolder, attribute);
		}

		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchTransactionAttributeSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(attrEle));
		attributeSourceDefinition.getPropertyValues().add("nameMap", transactionAttributeMap);
		return attributeSourceDefinition;
	}

	private void addRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String rollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue);
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new RollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

	private void addNoRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String noRollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue);
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new NoRollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

}
