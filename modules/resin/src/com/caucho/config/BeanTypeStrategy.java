/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.jsp.el.VariableResolver;
import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;
import com.caucho.util.NotImplementedException;
import com.caucho.xml.QName;
import org.w3c.dom.Node;

public class BeanTypeStrategy extends TypeStrategy {
  protected static final L10N L = new L10N(BeanTypeStrategy.class);

  private SoftReference<Method []> _methodsRef;
  private HashMap<QName,AttributeStrategy> _nsAttributeMap =
          new HashMap<QName,AttributeStrategy>();
  private final Class _type;

  private final Method _setParent;
  private final Method _init;
  private final Method _replaceObject;

  BeanTypeStrategy(Class type)
  {
    _type = type;

    Method setParent = null;

    setParent = findMethod("setParent", new Class[] { null });

    _setParent = setParent;

    Method init = null;

    try {
      init = type.getMethod("init", new Class[0]);
    } catch (NoSuchMethodException e) {
    }

    _init = init;

    Method replaceObject = null;

    replaceObject = findMethod("replaceObject", new Class[0]);

    _replaceObject = replaceObject;
  }

  /**
   * Returns the type class.
   */
  public Class getType()
  {
    return _type;
  }

  /**
   * Creates an instance of the type.
   *caucho.com/bugtrack
   */
  public Object create()
    throws Exception
  {
    Object bean = _type.newInstance();

    return bean;
  }

  /**
   * If the bean implements a setParent method, call it with the actual parent.
   *
   * @param bean the bean to be configured
   * @param parent the parent bean
   * @throws Exception
   */
  public void setParent(Object bean, Object parent)
    throws Exception
  {
    if (_setParent != null) {
      try {
        // XXX: should this be an error?
        if (parent != null &&
            _setParent.getParameterTypes()[0].isAssignableFrom(parent.getClass()))
          _setParent.invoke(bean, parent);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof Exception)
          throw (Exception) e.getCause();
        else
          throw e;
      }
    }
  }

  /**
   * Returns the appropriate strategy for the bean.
   *
   * @param attrName
   * @return the strategy
   * @throws ConfigException
   */
  public AttributeStrategy getAttributeStrategy(QName attrName)
          throws Exception
  {
    AttributeStrategy strategy = _nsAttributeMap.get(attrName);

    if (strategy == null) {
      strategy = getAttributeStrategyImpl(attrName);

      if (strategy == null && attrName.getName().equals("#text")) {
	strategy = getAttributeStrategyImpl(new QName("text"));

	if (strategy == null)
	  strategy = getAttributeStrategyImpl(new QName("value"));
      }

      if (strategy == null)
	throw new ConfigException(L.l("'{0}' is an unknown property of '{1}'.",
				      attrName.getName(), _type.getName()));

      _nsAttributeMap.put(attrName, strategy);
    }

    return strategy;
  }

  /**
   * Returns the type's configured value
   */
  public Object configure(NodeBuilder builder, Node node, Object parent)
    throws Exception
  {

    /* XXX: line #
    builder.setLocation(bean, qelt.getBaseURI(),
                        qelt.getFilename(), qelt.getLine());
    builder.setNode(bean, qelt);
    */
    // XXX: DependencyBean

    return builder.configureChildImpl(this, node, parent);
  }

  /**
   * Returns the attribute builder.
   */
  protected AttributeStrategy getAttributeStrategyImpl(QName qName)
    throws Exception
  {
    String name = qName.getName();

    AttributeStrategy strategy = null;

    Method builderMethod = findProgramBuilderMethod();

    AttributeStrategy flow = getFlowAttribute(qName);
    if (flow != null) {
      if (builderMethod != null)
	return new ProgramAttributeStrategy(builderMethod);

      return flow;
    }

    // hack for addText
    if (name.equals("#text"))
      name = "text";

    Method setterMethod = findOneArgSetter(name);
    Method createMethod = findCreate(name);
    if (createMethod != null) {
      return new CreateAttributeStrategy(createMethod, setterMethod);
    }

    if (setterMethod != null) {
      strategy = new SetterAttributeStrategy(setterMethod);

      return strategy;
    }

    if (builderMethod != null)
      return new ProgramAttributeStrategy(builderMethod);

    strategy = getEnvironmentAttribute(qName);

    if (strategy != null)
      return strategy;

    Method method = findSetPropertyMethod();

    if (method != null) {
      if (QName.class.isAssignableFrom(method.getParameterTypes()[0]))
        return new QNameMapAttributeStrategy(method, qName);
      else
        return new PrimitivePropertyStrategy(method, name);
    }

    return null;
  }

  /**
   * Returns the attribute builder.
   */
  public AttributeStrategy getFlowAttribute(QName name)
    throws Exception
  {
    return TypeStrategyFactory.getFlowAttribute(getType(),name);
  }

  /**
   * Returns the attribute builder.
   */
  public AttributeStrategy getEnvironmentAttribute(QName name)
    throws Exception
  {
    return null;
  }

  /**
   * Initializes the bean.
   */
  public void init(Object bean)
    throws Exception
  {
    if (_init != null)
      _init.invoke(bean);
  }

  /**
   * Replaces the bean object.
   *
   * @param bean the bean or factory to replace
   */
  public Object replaceObject(Object bean)
    throws Exception
  {
    if (_replaceObject != null)
      return _replaceObject.invoke(bean);
    else
      return bean;
  }

  protected Method findOneArgSetter(String attributeName)
  {
    String methodSuffix = attributeNameToBeanName(attributeName);

    Method method = findSetter("set" + methodSuffix);

    if (method == null)
      method = findSetter("add" + methodSuffix);

    return method;
  }

  protected Method findCreate(String attributeName)
  {
    String methodSuffix = attributeNameToBeanName(attributeName);

    return findMethod("create" + methodSuffix, new Class[0]);
  }

  protected Method findSetter(String methodName)
  {
    Method method = findSetter(methodName, false);

    if (method != null)
      return method;

    return findSetter(methodName, true);
  }

  /**
   * returns a one-arg public method or NULL
   */
  protected Method findSetter(String methodName,
                              boolean ignoreCase)
  {
    Method bestMethod = null;

    Method[] methods = getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (!ignoreCase && !method.getName().equals(methodName))
        continue;
      if (ignoreCase && !method.getName().equalsIgnoreCase(methodName))
        continue;
      if (method.getParameterTypes().length != 1)
        continue;

      if (method.getParameterTypes()[0].equals(String.class))
        return method;

      bestMethod = method;
    }

    return bestMethod;
  }

  /**
   * Translates a configuration name to a bean name.
   *
   * <pre>
   * foo-bar maps to fooBar
   * </pre>
   */
  protected static String attributeNameToBeanName(String name)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (ch == '-')
        cb.append(Character.toUpperCase(name.charAt(++i)));
      else if (ch == ':')
	cb.clear();
      else
        cb.append(ch);
    }

    if (Character.isLowerCase(cb.charAt(0)))
      cb.setCharAt(0, Character.toUpperCase(cb.charAt(0)));

    return cb.close();
  }

  protected Method findSetPropertyMethod()
  {
    Method method;

    method = findSetPropertyMethod("setProperty");
    if (method != null)
      return method;

    method = findSetPropertyMethod("setAttribute");
    if (method != null)
      return method;

    method = findSetPropertyMethod("put");
    if (method != null)
      return method;

    method = findSetPropertyMethod("set");
    if (method != null)
      return method;

    return method;
  }

  protected Method findSetPropertyMethod(String methodName)
  {
    Method method;

    method = findSetPropertyMethod(methodName,
				   String.class, BuilderProgram.class);
    if (method != null)
      return method;

    method = findSetPropertyMethod(methodName,
				   Object.class, BuilderProgram.class);
    if (method != null)
      return method;

    method = findSetPropertyMethod(methodName, QName.class, null);
    if (method != null)
      return method;

    method = findSetPropertyMethod(methodName, String.class, String.class);
    if (method != null)
      return method;

    method = findSetPropertyMethod(methodName, String.class, Object.class);
    if (method != null)
      return method;
    method = findSetPropertyMethod(methodName, String.class, null);
    if (method != null)
      return method;

    method = findSetPropertyMethod(methodName, Object.class, Object.class);
    if (method != null)
      return method;
    method = findSetPropertyMethod(methodName, Object.class, null);
    if (method != null)
      return method;

    return null;
  }

  protected Method findSetPropertyMethod(String name,
					 Class keyClass,
					 Class valueClass)
  {
    Method []methods = getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (! method.getName().equals(name))
        continue;
      else if (! Modifier.isPublic(method.getModifiers()))
        continue;

      Class []methodTypes = method.getParameterTypes();

      if (methodTypes.length != 2)
        continue;

      if (! methodTypes[0].equals(keyClass))
	continue;

      if (valueClass != null && ! methodTypes[1].equals(valueClass))
	continue;

      return method;
    }

    return null;
  }

  protected Method findProgramBuilderMethod()
  {
    Method method = findMethod("addBuilderProgram",
                               new Class[] { BuilderProgram.class });

    if (method != null)
      return method;

    return method;
  }

  protected Method findMethod(String methodName, Class[] parameterTypes)
  {
    Method []methods = getMethods();

    loop:
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (! method.getName().equals(methodName))
        continue;
      else if (! Modifier.isPublic(method.getModifiers()))
        continue;

      Class []methodTypes = method.getParameterTypes();

      if (methodTypes.length != parameterTypes.length)
        continue;

      for (int j = 0; j < methodTypes.length; j++) {
        if (parameterTypes[j] != null && ! methodTypes[j].equals(parameterTypes[j]))
          continue loop;
      }

      return method;
    }

    return null;
  }

  private Method []getMethods()
  {
    SoftReference<Method []> methodsRef = _methodsRef;
    Method [] methods;

    if (methodsRef != null && (methods = methodsRef.get()) != null)
      return methods;

    ArrayList<Method> methodList = new ArrayList<Method>();

    getMethods(methodList, _type);

    methods = methodList.toArray(new Method[methodList.size()]);

    _methodsRef = new SoftReference<Method []>(methods);

    return methods;
  }

  public static void getMethods(ArrayList<Method> list, Class type)
  {
    if (type == null)
      return;

    if (Modifier.isPublic(type.getModifiers())) {
      Method []methods = type.getDeclaredMethods();

      for (int i = 0; i < methods.length; i++) {
	if (! contains(list, methods[i]))
	  list.add(methods[i]);
      }
    }

    Class []interfaces = type.getInterfaces();
    for (int i = 0; i < interfaces.length; i++)
      getMethods(list, interfaces[i]);

    getMethods(list, type.getSuperclass());
  }

  public static boolean contains(ArrayList<Method> list, Method method)
  {
    for (int i = 0; i < list.size(); i++) {
      if (isMatch(list.get(i), method))
	return true;
    }

    return false;
  }

  public static boolean isMatch(Method methodA, Method methodB)
  {
    if (! methodA.getName().equals(methodB.getName()))
      return false;

    Class []paramA = methodA.getParameterTypes();
    Class []paramB = methodB.getParameterTypes();

    if (paramA.length != paramB.length)
      return false;

    for (int i = 0; i < paramA.length; i++) {
      if (! paramA[i].equals(paramB[i]))
	return false;
    }

    return true;
  }

  protected static void invokeBeanMethod(Object bean, Method method, Object[] args)
    throws ConfigException
  {
    try {
      method.invoke(bean, args);
    }
    catch (IllegalArgumentException e) {
      //e.printStackTrace();
      throw new ConfigException(L.l("{0}: parameter type mismatch invoking method `{1}'.", bean.getClass().getName(), method.toString()), e);
    }
    catch (IllegalAccessException e) {
      throw new ConfigException(L.l("Bean method `{0}' isn't accessible.", method.toString()), e);
    }
    catch (InvocationTargetException e) {
      throw new ConfigException(e.getCause());
    }
  }

  protected VariableResolver getEnvironment()
  {
    return Config.getEnvironment();
  }

  protected static NotImplementedException notImplemented()
  {
    return new NotImplementedException(L.l("Not implemented."));
  }
}
