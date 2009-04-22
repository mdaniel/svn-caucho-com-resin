/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.type;

import java.beans.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.PropertyStringProgram;
import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.xml.*;
import com.caucho.vfs.*;
import com.caucho.xml.QName;

import org.w3c.dom.*;

/**
 * Represents an introspected bean type for configuration.
 */
public class BeanType extends ConfigType
{
  private static final L10N L = new L10N(BeanType.class);
  private static final Logger log
    = Logger.getLogger(BeanType.class.getName());
  
  private static final String RESIN_NS
    = "http://caucho.com/ns/resin";

  public static final QName TEXT = new QName("#text");
  public static final QName VALUE = new QName("value");

  private static final Object _introspectLock = new Object();

  private final Class _beanClass;
  
  private ConcurrentHashMap<QName,Attribute> _nsAttributeMap
    = new ConcurrentHashMap<QName,Attribute>();
  
  private HashMap<String,Attribute> _attributeMap
    = new HashMap<String,Attribute>();

  /*
  private HashMap<String,Method> _createMap
    = new HashMap<String,Method>();
  */

  private Constructor _stringConstructor;
  
  private Method _valueOf;
  private Method _setParent;
  private Method _replaceObject;
  private Method _setConfigLocation;
  private Method _setConfigNode;
  
  private Attribute _addText;
  private Attribute _addProgram;
  private Attribute _addContentProgram;
  private Attribute _addBean; // add(Object)
  private Attribute _setProperty;

  private HashMap<Class,Attribute> _addMethodMap
    = new HashMap<Class,Attribute>();

  private Attribute _addCustomBean;
  private Attribute _addAnnotation;
  
  private ComponentImpl _component;

  private ArrayList<ConfigProgram> _injectList;
  private ArrayList<ConfigProgram> _initList;

  private boolean _isIntrospecting;
  private boolean _isIntrospected;
  private boolean _isIntrospectComplete;
  private ArrayList<BeanType> _pendingChildList = new ArrayList<BeanType>();

  public BeanType(Class beanClass)
  {
    _beanClass = beanClass;
  }

  /**
   * Returns the given type.
   */
  public Class getType()
  {
    return _beanClass;
  }

  protected void setAddCustomBean(Attribute addCustomBean)
  {
    _addCustomBean = addCustomBean;
  }

  protected void setAddAnnotation(Attribute addAnnotation)
  {
    _addAnnotation = addAnnotation;
  }

  /**
   * Creates a new instance
   */
  @Override
  public Object create(Object parent, QName name)
  {
    try {
      if (_component == null) {
	if (_beanClass.isInterface())
	  throw new ConfigException(L.l("{0} cannot be instantiated because it is an interface",
					_beanClass.getName()));

	InjectManager webBeans
	  = InjectManager.create(_beanClass.getClassLoader());

	_component = (ComponentImpl) webBeans.createTransient(_beanClass);
      }

      Object bean = _component.createNoInit();

      if (_setParent != null
	  && parent != null
	  && _setParent.getParameterTypes()[0].isAssignableFrom(parent.getClass())) {
	try {
	  _setParent.invoke(bean, parent);
	} catch (IllegalArgumentException e) {
	  throw ConfigException.create(_setParent,
				       L.l("{0}: setParent value of '{1}' is not valid",
					   bean, parent),
				       e);
	} catch (Exception e) {
	  throw ConfigException.create(_setParent, e);
	}
      }

      return bean;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Called before the children are configured.
   */
  @Override
  public void beforeConfigure(ConfigContext env, Object bean, Node node)
  {
    super.beforeConfigure(env, bean, node);

    if (_setConfigNode != null) {
      try {
	_setConfigNode.invoke(bean, node);
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }

    if (_setConfigLocation != null && node instanceof QNode) {
      String filename = ((QNode) node).getFilename();
      int line = ((QNode) node).getLine();

      try {
	_setConfigLocation.invoke(bean, filename, line);
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }

    if (bean instanceof DependencyBean) {
      DependencyBean dependencyBean = (DependencyBean) bean;
      
      ArrayList<Dependency> dependencyList = env.getDependencyList();
      if (dependencyList != null) {
	for (Dependency depend : dependencyList) {
	  dependencyBean.addDependency((PersistentDependency) depend);
	}
      }
    }
  }

  /**
   * Returns the attribute based on the given name.
   */
  @Override
  public Attribute getAttribute(QName name)
  {
    Attribute attr = _nsAttributeMap.get(name);

    if (attr == null) {
      attr = getAttributeImpl(name);

      if (attr != null)
	_nsAttributeMap.put(name, attr);
    }

    return attr;
  }

  protected Attribute getAttributeImpl(QName name)
  {
    // server/2r10 vs jms/2193
    // attr = _attributeMap.get(name.getLocalName().toLowerCase());

    Attribute attr = _attributeMap.get(name.getLocalName());

    if (attr != null)
      return attr;

    String uri = name.getNamespaceURI();

    if (uri == null || ! uri.startsWith("urn:java"))
      return null;

    Class cl = createClass(name);

    if (cl != null) {
      attr = getAddAttribute(cl);

      if (attr != null)
	return attr;
    }

    if (_addCustomBean != null) {
      return _addCustomBean;
    }
    else if (_addBean != null) {
      return _addBean;
    }

    return null;
  }

  @Override
  public Attribute getAddBeanAttribute(QName qName)
  {
    return _addBean;
  }

  /**
   * Returns any add attributes to add arbitrary content
   */
  public Attribute getAddAttribute(Class cl)
  {
    if (cl == null)
      return null;

    Attribute attr = _addMethodMap.get(cl);

    if (attr != null) {
      return attr;
    }
    
    for (Class iface : cl.getInterfaces()) {
      attr = getAddAttribute(iface);

      if (attr != null)
	return attr;
    }

    return getAddAttribute(cl.getSuperclass());
  }

  private Class createClass(QName name)
  {
    String uri = name.getNamespaceURI();

    if (uri.equals(RESIN_NS)) {
      return createResinClass(name.getLocalName());
    }

    if (! uri.startsWith("urn:java:"))
      return null;

    String pkg = uri.substring("urn:java:".length());

    return TypeFactory.loadClass(pkg, name.getLocalName());
  }

  private Class createResinClass(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    Class cl = TypeFactory.loadClass("ee", name);

    return cl;
  }

  /**
   * Returns the program attribute.
   */
  @Override
  public Attribute getProgramAttribute()
  {
    if (_setProperty != null)
      return _setProperty;
    else
      return _addProgram;
  }

  /**
   * Returns the content program attribute (program excluding if, choose).
   */
  @Override
  public Attribute getContentProgramAttribute()
  {
    return _addContentProgram;
  }

  /**
   * Initialize the type
   */
  @Override
  public void inject(Object bean)
  {
    introspectInject();
    
    for (int i = 0; i < _injectList.size(); i++)
      _injectList.get(i).inject(bean, null);
  }

  /**
   * Initialize the type
   */
  @Override
  public void init(Object bean)
  {
    introspectInject();
    
    for (int i = 0; i < _initList.size(); i++)
      _initList.get(i).inject(bean, null);
  }

  /**
   * Return true if the object is replaced
   */
  public boolean isReplace()
  {
    return _replaceObject != null;
  }
  
  /**
   * Replace the type with the generated object
   */
  @Override
  public Object replaceObject(Object bean)
  {
    if (_replaceObject != null) {
      try {
	return _replaceObject.invoke(bean);
      } catch (Exception e) {
	throw ConfigException.create(_replaceObject, e);
      }
    }
    else
      return bean;
  }
  
  /**
   * Converts the string to the given value.
   */
  public Object valueOf(String text)
  {
    if (_valueOf != null) {
      try {
	return _valueOf.invoke(null, text);
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }
    else if (_stringConstructor != null) {
      try {
	return _stringConstructor.newInstance(text);
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }
    else if (_addText != null) {
      Object bean = create(null, TEXT);
      _addText.setText(bean, TEXT, text);

      inject(bean);
      init(bean);
      
      return bean;
    }
    else if (_addProgram != null || _addContentProgram != null) {
      Object bean = create(null, TEXT);

      inject(bean);
      
      try {
	ConfigProgram program = new PropertyStringProgram(TEXT, text);

	if (_addProgram != null)
	  _addProgram.setValue(bean, TEXT, program);
	else
	  _addContentProgram.setValue(bean, TEXT, program);
      } catch (Exception e) {
	throw ConfigException.create(e);
      }

      init(bean);

      return bean;
    }
    else if ("".equals(text.trim())) {
      Object bean = create(null, TEXT);

      inject(bean);
      init(bean);
      
      return bean;
    }

    throw new ConfigException(L.l("Can't convert to '{0}' from '{1}'.",
				  _beanClass.getName(), text));
  }

  public boolean isConstructableFromString()
  {
    return _valueOf != null ||
           _stringConstructor != null ||
           _addText != null ||
           _addProgram != null ||
           _addContentProgram != null;
  }

  /**
   * Converts the string to the given value.
   */
  @Override
  public Object valueOf(Object value)
  {
    if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else if (_beanClass.isAssignableFrom(value.getClass()))
      return value;
    else if (value.getClass().getName().startsWith("java.lang."))
      return valueOf(String.valueOf(value));
    else
      return value;
  }

  //
  // Introspection
  //

  /**
   * Introspect the bean for configuration
   */
  @Override
  public void introspect()
  {
   synchronized (_introspectLock) {
      if (_isIntrospecting)
	return;
    
      _isIntrospecting = true;
    
      try {
	// ioc/20h4 - after to deal with recursion
	introspectParent();

	//Method []methods = _beanClass.getMethods();
	if (! _isIntrospected) {
	  _isIntrospected = true;

	  try {
	    Method []methods = _beanClass.getDeclaredMethods();
    
	    introspectMethods(methods);
	  } catch (NoClassDefFoundError e) {
	    log.fine(_beanClass + " " + e);
	  }

	  /*
	  InjectIntrospector.introspectInject(_injectList, _beanClass);

	  InjectIntrospector.introspectInit(_initList, _beanClass, null);
	  */
	}
      } finally {
	_isIntrospecting = false;
      }
    }

    introspectComplete();
  }

  private void introspectComplete()
  {
    ArrayList<BeanType> childList = new ArrayList<BeanType>(_pendingChildList);

    // ioc/20h4
    for (BeanType child : childList) {
      child.introspectParent();
      child.introspectComplete();
    }
  }
  
  private boolean isIntrospecting()
  {
    if (_isIntrospecting)
      return true;

    Class parentClass = _beanClass.getSuperclass();
    
    if (parentClass != null) {
      ConfigType parentType = TypeFactory.getType(parentClass);

      if (parentType instanceof BeanType) {
	BeanType parentBean = (BeanType) parentType;

	return parentBean.isIntrospecting();
      }
    }

    return false;
  }

  private void introspectParent()
  {
    Class parentClass = _beanClass.getSuperclass();
    
    if (parentClass != null) {
      ConfigType parentType = TypeFactory.getType(parentClass);

      if (parentType instanceof BeanType) {
	BeanType parentBean = (BeanType) parentType;

	if (! parentBean._isIntrospected)
	  parentBean.introspect();

	// ioc/20h4
	if (parentBean.isIntrospecting()) {
	  if (! parentBean._pendingChildList.contains(this))
	    parentBean._pendingChildList.add(this);
	  return;
	}

	if (_setParent == null)
	  _setParent = parentBean._setParent;
	
	if (_replaceObject == null)
	  _replaceObject = parentBean._replaceObject;
	
	if (_setConfigLocation == null)
	  _setConfigLocation = parentBean._setConfigLocation;
	
	if (_setConfigNode == null)
	  _setConfigNode = parentBean._setConfigNode;

	if (_addText == null)
	  _addText = parentBean._addText;

	if (_addProgram == null)
	  _addProgram = parentBean._addProgram;
	
	if (_addContentProgram == null)
	  _addContentProgram = parentBean._addContentProgram;

	if (_setProperty == null)
	  _setProperty = parentBean._setProperty;

	if (_addCustomBean == null)
	  _addCustomBean = parentBean._addCustomBean;

	for (Map.Entry<QName,Attribute> entry : parentBean._nsAttributeMap.entrySet()) {
	  if (_nsAttributeMap.get(entry.getKey()) == null)
	    _nsAttributeMap.put(entry.getKey(), entry.getValue());
	}

	for (Map.Entry<String,Attribute> entry : parentBean._attributeMap.entrySet()) {
	  if (_attributeMap.get(entry.getKey()) == null)
	    _attributeMap.put(entry.getKey(), entry.getValue());
	}

	_addMethodMap.putAll(parentBean._addMethodMap);
      }
    }
  }

  /**
   * Introspect the beans methods for setters
   */
  public void introspectMethods(Method []methods)
  {
    Constructor []constructors = _beanClass.getConstructors();

    _stringConstructor = findConstructor(constructors, String.class);

    HashMap<String,Method> createMap = new HashMap<String,Method>(8);
    fillCreateMap(createMap, methods);

    HashMap<String,Method> setterMap = new HashMap<String,Method>(8);
    fillSetterMap(setterMap, methods);

    for (Method method : methods) {
      Class []paramTypes = method.getParameterTypes();

      String name = method.getName();

      if ("replaceObject".equals(name) && paramTypes.length == 0) {
	_replaceObject = method;
	continue;
      }

      if ("valueOf".equals(name)
	  && paramTypes.length == 1
	  && String.class.equals(paramTypes[0])
	  && Modifier.isStatic(method.getModifiers())) {
	_valueOf = method;
	continue;
      }
      
      if (Modifier.isStatic(method.getModifiers()))
	continue;
      
      if (! Modifier.isPublic(method.getModifiers()))
	continue;

      if ((name.equals("addBuilderProgram") || name.equals("addProgram"))
	  && paramTypes.length == 1
	  && paramTypes[0].equals(ConfigProgram.class)) {
	ConfigType type = TypeFactory.getType(paramTypes[0]);
	
	_addProgram = new ProgramAttribute(method, type);
      }
      else if (name.equals("addContentProgram")
	       && paramTypes.length == 1
	       && paramTypes[0].equals(ConfigProgram.class)) {
	ConfigType type = TypeFactory.getType(paramTypes[0]);
	
	_addContentProgram = new ProgramAttribute(method, type);
      }
      else if ((name.equals("setConfigLocation")
		&& paramTypes.length == 2
		&& paramTypes[0].equals(String.class)
		&& paramTypes[1].equals(int.class))) {
	_setConfigLocation = method;
      }
      else if ((name.equals("setConfigNode")
		&& paramTypes.length == 1
		&& paramTypes[0].equals(Node.class))) {
	_setConfigNode = method;
      }
      else if ((name.equals("addCustomBean")
		&& paramTypes.length == 1
		&& paramTypes[0].equals(CustomBeanConfig.class))) {
	ConfigType customBeanType
	  = TypeFactory.getType(CustomBeanConfig.class);

	_addCustomBean = new CustomBeanAttribute(method, customBeanType);
      }
      else if ((name.equals("addAnnotation")
		&& paramTypes.length == 1
		&& paramTypes[0].equals(Annotation.class))) {
	ConfigType customBeanType
	  = TypeFactory.getType(CustomBeanConfig.class);

	_addCustomBean = new CustomBeanAttribute(method, customBeanType);
      }
      else if (name.equals("setProperty")
	       && paramTypes.length == 2
	       && paramTypes[0].equals(String.class)) {
	ConfigType type = TypeFactory.getType(paramTypes[1]);

	PropertyAttribute attr = new PropertyAttribute(method, type);

	_setProperty = attr;
      }
      else if (name.equals("setParent")
	       && paramTypes.length == 1) {
	// XXX: use annotation
	_setParent = method;
      }
      else if (name.equals("add")
	       && paramTypes.length == 1) {
	ConfigType type = TypeFactory.getType(paramTypes[0]);

	Attribute addAttr = new AddAttribute(method, type);

	_addMethodMap.put(paramTypes[0], addAttr);

	// _addBean = addAttr;
      }
      else if ((name.startsWith("set") || name.startsWith("add"))
	       && paramTypes.length == 1
	       && createMap.get(name.substring(3)) == null) {
	Class type = paramTypes[0];

	String className = name.substring(3);
	String propName = toXmlName(name.substring(3));

        TagName tagName = method.getAnnotation(TagName.class);

        if (tagName != null)
          propName = tagName.value();

        Attribute attr;
	
	if (propName.equals("text")
	    && (paramTypes[0].equals(String.class)
		|| paramTypes[0].equals(RawString.class))) {
	  attr = new TextAttribute(method, type);
	  _addText = attr;
	  _attributeMap.put("#text", attr);
	}
	else
	  attr = new SetterAttribute(method, type);

	_attributeMap.put(propName, attr);
	// server/2e28 vs jms/2193
	// _attributeMap.put(className, attr);

	if (propName.equals("value")) {
	  _attributeMap.put("#text", attr);

	  // server/12aa
	  if (_addText == null)
	    _addText = attr;
	}

	propName = toCamelName(className);
	
	_attributeMap.put(propName, attr);
      }
      else if ((name.startsWith("create")
		&& paramTypes.length == 0
		&& ! void.class.equals(method.getReturnType()))) {
	Class type = method.getReturnType();

	Method setter = setterMap.get(name.substring(6));

	CreateAttribute attr = new CreateAttribute(method, type, setter);

	String propName = toXmlName(name.substring(6));

        TagName tagName = method.getAnnotation(TagName.class);

        if (tagName != null)
          propName = tagName.value();

        _attributeMap.put(propName, attr);
      }
    }
  }

  /**
   * Introspect the bean for configuration
   */
  protected void introspectInject()
  {
    synchronized (_introspectLock) {
      if (_injectList != null)
	return;

      _injectList = new ArrayList<ConfigProgram>();
      _initList = new ArrayList<ConfigProgram>();
    
      InjectIntrospector.introspectInject(_injectList, _beanClass);

      InjectIntrospector.introspectInit(_initList, _beanClass, null);
    }
  }

  private static Constructor findConstructor(Constructor []constructors,
					     Class ...types)
  {
    for (Constructor ctor : constructors) {
      Class []paramTypes = ctor.getParameterTypes();

      if (isMatch(paramTypes, types))
	return ctor;
    }

    return null;
  }

  private static boolean isMatch(Class []aTypes, Class []bTypes)
  {
    if (aTypes.length != bTypes.length)
      return false;

    for (int i = aTypes.length - 1; i >= 0; i--) {
      if (! aTypes[i].equals(bTypes[i]))
	return false;
    }

    return true;
  }


  private void fillCreateMap(HashMap<String,Method> createMap,
			     Method []methods)
  {
    for (Method method : methods) {
      String name = method.getName();

      if (name.startsWith("create")
	  && ! name.equals("create")
	  && method.getParameterTypes().length == 0) {
	createMap.put(name.substring("create".length()), method);
      }
    }
  }

  private void fillSetterMap(HashMap<String,Method> setterMap,
			     Method []methods)
  {
    for (Method method : methods) {
      String name = method.getName();

      if (name.length() > 3
	  && (name.startsWith("add") || name.startsWith("set"))
	  && method.getParameterTypes().length == 1) {
	setterMap.put(name.substring("set".length()), method);
      }
    }
  }

  private Method findCreate(Method []methods, String name)
  {
    String createName = "create" + name;

    for (Method method : methods) {
      if (method.getParameterTypes().length != 0)
	continue;

      if (method.getName().equals(createName))
	return method;
    }

    return null;
  }

  private Method findSetter(String name)
  {
    String addName = "add" + name;
    String setName = "set" + name;

    for (Method method : _beanClass.getMethods()) {
      if (method.getParameterTypes().length != 1)
	continue;

      if (method.getName().equals(addName)
	  || method.getName().equals(setName))
	return method;
    }

    return null;
  }

  private String toXmlName(String name)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (Character.isUpperCase(ch)
	  && i > 0
	  && (Character.isLowerCase(name.charAt(i - 1))
	      || (i + 1 < name.length()
		  && Character.isLowerCase(name.charAt(i + 1))))) {
	sb.append('-');
      }

      sb.append(Character.toLowerCase(ch));
    }

    return sb.toString();
  }

  private String toCamelName(String name)
  {
    return Introspector.decapitalize(name);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _beanClass.getName() + "]";
  }
}
