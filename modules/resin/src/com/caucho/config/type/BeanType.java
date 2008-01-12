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
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.xml.QName;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

/**
 * Represents an introspected bean type for configuration.
 */
public class BeanType extends ConfigType
{
  private static final L10N L = new L10N(BeanType.class);
  private static final Logger log
    = Logger.getLogger(BeanType.class.getName());

  private static final QName TEXT = new QName("#text");

  private final Class _beanClass;
  
  private HashMap<QName,Attribute> _nsAttributeMap
    = new HashMap<QName,Attribute>();
  
  private HashMap<String,Attribute> _attributeMap
    = new HashMap<String,Attribute>();

  private Method _valueOf;
  private Method _setParent;
  private Method _replaceObject;
  
  private Attribute _addText;
  private Attribute _addProgram;
  private Attribute _setProperty;
  

  private ComponentImpl _component;

  private ArrayList<ConfigProgram> _initList = new ArrayList<ConfigProgram>();

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

  /**
   * Creates a new instance
   */
  @Override
  public Object create(Object parent)
  {
    try {
      if (_component == null) {
	if (_beanClass.isInterface())
	  throw new ConfigException(L.l("{0} cannot be instantiated because it is an interface",
					_beanClass.getName()));

	WebBeansContainer webBeans
	  = WebBeansContainer.create(_beanClass.getClassLoader());

	_component = (ComponentImpl) webBeans.createTransient(_beanClass);
      }

      Object bean = _component.createNoInit();

      if (_setParent != null) {
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
   * Returns the attribute based on the given name.
   */
  @Override
  public Attribute getAttribute(QName name)
  {
    synchronized (_nsAttributeMap) {
      Attribute attr = _nsAttributeMap.get(name);

      if (attr == null) {
	attr = _attributeMap.get(name.getLocalName());

	if (attr != null)
	  _nsAttributeMap.put(name, attr);
      }

      if (attr != null)
	return attr;
    }
    
    return null;
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
   * Initialize the type
   */
  @Override
  public void init(Object bean)
  {
    for (int i = 0; i < _initList.size(); i++)
      _initList.get(i).inject(bean, null);
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
	return _valueOf.invoke(text);
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }
    else if (_addText != null) {
      Object bean = create(null);
      _addText.setText(bean, TEXT, text);

      // XXX: init
      
      return bean;
    }
    
    throw new ConfigException(L.l("Can't convert to '{0}' from '{1}'.",
				  _beanClass.getName(), text));
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
    introspectMethods();

    InjectIntrospector.introspectInit(_initList, _beanClass);
  }

  /**
   * Introspect the beans methods for setters
   */
  public void introspectMethods()
  {
    try {
      Method valueOf = _beanClass.getMethod("valueOf",
					    new Class[] { String.class } );

      if (Modifier.isStatic(valueOf.getModifiers()))
	_valueOf = valueOf;
    } catch (NoSuchMethodException e) {
    }
    
    try {
      Method replaceObject = _beanClass.getMethod("replaceObject",
						  new Class[] { } );

      _replaceObject = replaceObject;
    } catch (NoSuchMethodException e) {
    }
    
    for (Method method : _beanClass.getMethods()) {
      if (Modifier.isStatic(method.getModifiers()))
	continue;
      
      if (! Modifier.isPublic(method.getModifiers()))
	continue;

      Class []paramTypes = method.getParameterTypes();

      String name = method.getName();

      if (name.equals("addBuilderProgram")
	  && paramTypes.length == 1
	  && paramTypes[0].equals(BuilderProgram.class)) {
	ConfigType type = TypeFactory.getType(paramTypes[0]);
	
	_addProgram = new ProgramAttribute(method, type);
      }
      else if (name.equals("setProperty")
	       && paramTypes.length == 2
	       && paramTypes[0].equals(String.class)) {
	ConfigType type = TypeFactory.getType(paramTypes[1]);

	PropertyAttribute attr = new PropertyAttribute(method, type);

	_setProperty = attr;
      }
      else if (name.equals("setParent")
	       && paramTypes.length == 1
	       && paramTypes[0].equals(Object.class)) {
	// XXX: use annotation
	_setParent = method;
      }
      else if ((name.startsWith("set") || name.startsWith("add"))
	       && paramTypes.length == 1
	       && findCreate(name.substring(3)) == null) {
	ConfigType type = TypeFactory.getType(paramTypes[0]);

	String propName = toXmlName(name.substring(3));
	
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

	if (propName.equals("value"))
	  _attributeMap.put("#text", attr);

	propName = toCamelName(name.substring(3));
	_attributeMap.put(propName, attr);
      }
      else if ((name.startsWith("create")
		&& paramTypes.length == 0
		&& ! void.class.equals(method.getReturnType()))) {
	ConfigType type = TypeFactory.getType(method.getReturnType());

	Method setter = findSetter(name.substring(6));

	CreateAttribute attr = new CreateAttribute(method, type, setter);

	String propName = toXmlName(name.substring(6));

	_attributeMap.put(propName, attr);
      }
    }
  }

  private Method findCreate(String name)
  {
    String createName = "create" + name;

    for (Method method : _beanClass.getMethods()) {
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
