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

package org.springframework.core.type;

/**
 * Interface that defines abstract metadata of a specific class,
 * in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see StandardClassMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
 * @see AnnotationMetadata
 */
public interface ClassMetadata {

	/**
	 * 返回class的名称
	 * Return the name of the underlying class.
	 */
	String getClassName();

	/**
	 * 判断class是否是interface
	 * Return whether the underlying class represents an interface.
	 */
	boolean isInterface();

	/**
	 * 判断class是否是Annotation
	 * Return whether the underlying class represents an annotation.
	 * @since 4.1
	 */
	boolean isAnnotation();

	/**
	 * 判断class是否是abstract类
	 * Return whether the underlying class is marked as abstract.
	 */
	boolean isAbstract();

	/**
	 * 判断class是否是具体的类（而不是抽象类或接口）
	 * Return whether the underlying class represents a concrete class,
	 * i.e. neither an interface nor an abstract class.
	 */
	boolean isConcrete();

	/**
	 * 判断class是否被final标记，final标记的类不能被继承
	 * Return whether the underlying class is marked as 'final'.
	 */
	boolean isFinal();

	/**
	 * 判断class是否是独立的类，意思就是说类是顶级类或者是静态内部类，说白了就是可以单独进行实例化的
	 * 顶级类的意思是：class不是内部类（包括方法内的匿名内部类）
	 * Determine whether the underlying class is independent, i.e. whether
	 * it is a top-level class or a nested class (static inner class) that
	 * can be constructed independently from an enclosing class.
	 */
	boolean isIndependent();

	/**
	 * 判断class是否是内部类或者方法内部的匿名内部类，如果返回false，表明该class是顶级类
	 * Return whether the underlying class is declared within an enclosing
	 * class (i.e. the underlying class is an inner/nested class or a
	 * local class within a method).
	 * <p>If this method returns {@code false}, then the underlying
	 * class is a top-level class.
	 */
	boolean hasEnclosingClass();

	/**
	 * 如果class是顶级类，那么返回null，如果class是内部类，则返回声明它的外部类的名称
	 * Return the name of the enclosing class of the underlying class,
	 * or {@code null} if the underlying class is a top-level class.
	 */
	String getEnclosingClassName();

	/**
	 * 判断class是否有父类
	 * Return whether the underlying class has a super class.
	 */
	boolean hasSuperClass();

	/**
	 * 返回class的父类，如果没有父类则返回null
	 * Return the name of the super class of the underlying class,
	 * or {@code null} if there is no super class defined.
	 */
	String getSuperClassName();

	/**
	 * 返回class实现的interface列表
	 * Return the names of all interfaces that the underlying class
	 * implements, or an empty array if there are none.
	 */
	String[] getInterfaceNames();

	/**
	 * 返回class内部声明的所有class（public/protected/private/default）和interface，但不包括继承的class和interface（也就是里面class和interface不能有extends）
	 * Return the names of all classes declared as members of the class represented by
	 * this ClassMetadata object. This includes public, protected, default (package)
	 * access, and private classes and interfaces declared by the class, but excludes
	 * inherited classes and interfaces. An empty array is returned if no member classes
	 * or interfaces exist.
	 * @since 3.1
	 */
	String[] getMemberClassNames();

}
