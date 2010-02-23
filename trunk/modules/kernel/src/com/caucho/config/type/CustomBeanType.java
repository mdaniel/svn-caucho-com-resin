/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.PropertyStringProgram;
import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.xml.*;
import com.caucho.vfs.*;
import com.caucho.xml.QName;

import org.w3c.dom.*;

/**
 * Represents an webbeans-style introspected bean type for configuration.
 */
public class CustomBeanType<T> extends ConfigType
{
  private static final L10N L = new L10N(CustomBeanType.class);
  private static final Logger log
    = Logger.getLogger(CustomBeanType.class.getName());

  private static final String RESIN_NS
    = "http://caucho.com/ns/resin";
  private static final String JAVAEE_NS
    = "http://java.sun.com/xml/ns/javaee";

  private static final QName TEXT = new QName("#text");

  private static final QName W_VALUE = new QName("", "value", JAVAEE_NS);
  private static final QName R_VALUE = new QName("", "value", RESIN_NS);
  private static final QName A_VALUE = new QName("value", null);

  private static final QName W_NEW = new QName("", "new", JAVAEE_NS);
  private static final QName R_NEW = new QName("", "new", RESIN_NS);
  private static final QName A_NEW = new QName("new", null);

  private static final Object _introspectLock = new Object();

  private final Class<T> _beanClass;

  private final ConfigType _beanType;

  private String _namespaceURI;

  private boolean _hasZeroArg;

  private HashMap<QName,Attribute> _nsAttributeMap
    = new HashMap<QName,Attribute>();

  private HashMap<String,Attribute> _attributeMap
    = new HashMap<String,Attribute>();

  private Attribute _addAttribute;

  public CustomBeanType(Class<T> beanClass)
  {
    _beanClass = beanClass;

    _beanType = TypeFactory.getType(beanClass);

    int p = beanClass.getName().lastIndexOf('.');
    _namespaceURI = "urn:java:" + beanClass.getName().substring(0, p);

    _nsAttributeMap.put(W_NEW, CustomBeanNewAttribute.ATTRIBUTE);
    _nsAttributeMap.put(R_NEW, CustomBeanNewAttribute.ATTRIBUTE);
    _nsAttributeMap.put(A_NEW, CustomBeanNewAttribute.ATTRIBUTE);
  }

  /**
   * Returns the given type.
   */
  public Class<?> getType()
  {
    return _beanClass;
  }

  /**
   * Creates a new instance
   */
  @Override
  public Object create(Object parent, QName name)
  {
    return new CustomBeanConfig(name, _beanClass);
  }

  /**
   * Returns the attribute with the given name.
   */
  public Attribute getAttribute(QName qName)
  {
    Attribute attr = _nsAttributeMap.get(qName);

    if (attr == null) {
      attr = getAttributeImpl(qName);

      if (attr != null)
        _nsAttributeMap.put(qName, attr);
    }

    return attr;
  }

  protected Attribute getAttributeImpl(QName qName)
  {
    Attribute attr = _beanType.getAttribute(qName);

    if (attr != null) {
      return CustomBeanProgramAttribute.ATTRIBUTE;
    }

    String uri = qName.getNamespaceURI();

    if (uri == null)
      return null;
    else if (! uri.startsWith("urn:java:") && ! uri.equals(RESIN_NS))
      return null;

    Method method = null;
    if (uri.equals(_namespaceURI)
        && (method = findMethod(qName.getLocalName())) != null) {
      return new CustomBeanMethodAttribute(_beanClass, method);
    }

    Field field = null;
    if (uri.equals(_namespaceURI)
        && (field = findField(qName.getLocalName())) != null) {
      return new CustomBeanFieldAttribute(_beanClass, field);
    }

    /*
    if ("value".equals(qName.getLocalName())
        && (uri.equals("urn:java:ee") || (uri.equals(RESIN_NS)))) {
      // ioc/022k
      return CustomBeanValueArgAttribute.ATTRIBUTE;
    }
    */

    if ("new".equals(qName.getLocalName())) {
      return CustomBeanNewAttribute.ATTRIBUTE;
    }

    Attribute envAttr
      = TypeFactory.getFactory().getEnvironmentAttribute(qName);

    if (envAttr instanceof FlowAttribute) {
      //        || envAttr instanceof EnvironmentAttribute) {
      // ioc/04c1
      return envAttr;
    }

    ConfigType type = TypeFactory.getFactory().getEnvironmentType(qName);

    if (type == null) {
      if (Character.isLowerCase(qName.getLocalName().charAt(0))) {
        throw new ConfigException(L.l("'{0}' is an unknown field of {1}",
                                      qName.getLocalName(),
                                      _beanClass.getName()));
      }
      else {
        throw new ConfigException(L.l("'{0}' cannot be instantiated because it does not map to a known class",
                                      qName));
      }
    }

    Class<?> cl = type.getType();

    if (Annotation.class.isAssignableFrom(cl)) {
      return new CustomBeanAnnotationAttribute(cl);
    }

    AddAttribute addAttribute = (AddAttribute) _beanType.getAddAttribute(cl);

    if (addAttribute != null)
      return new CustomBeanAddAttribute(cl);

    throw new ConfigException(L.l("'{0}' is an unknown field or annotation",
                                  qName));
    // return new CustomBeanArgAttribute(cl);
  }

  @Override
  public Attribute getProgramAttribute()
  {
    Attribute attr = _beanType.getProgramAttribute();

    if (attr == null)
      return null;

    // server/1kl5
    return CustomBeanProgramAttribute.ATTRIBUTE;
  }

  private Method findMethod(String name)
  {
    return findMethod(_beanClass, name);
  }

  private Method findMethod(Class<?> cl, String name)
  {
    if (cl == null || cl.equals(Object.class))
      return null;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.getName().equals(name))
        return method;
    }

    return findMethod(cl.getSuperclass(), name);
  }

  private Field findField(String name)
  {
    return findField(_beanClass, name);
  }

  private Field findField(Class<?> cl, String name)
  {
    if (cl == null || cl.equals(Object.class))
      return null;

    for (Field field : cl.getDeclaredFields()) {
      if (field.getName().equals(name))
        return field;
    }

    return findField(cl.getSuperclass(), name);
  }

  private ConfigType createClass(QName name)
  {
    return TypeFactory.getFactory().getEnvironmentType(name);
    /*
    String uri = name.getNamespaceURI();

    if (uri.equals(RESIN_NS)) {
      return createResinClass(name.getLocalName());
    }

    if (! uri.startsWith("urn:java:"))
      return null;

    String pkg = uri.substring("urn:java:".length());

    return TypeFactory.loadClass(pkg, name.getLocalName());
    */
  }

  private Class<?> createResinClass(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    Class<?> cl = TypeFactory.loadClass("ee", name);

    return cl;
  }

  /**
   * Converts the string to a value of the type.
   */
  public Object valueOf(String text)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Initialize the type
   */
  @Override
  public void init(Object bean)
  {
    CustomBeanConfig customBean = (CustomBeanConfig) bean;

    customBean.init();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _beanClass.getName() + "]";
  }
}
