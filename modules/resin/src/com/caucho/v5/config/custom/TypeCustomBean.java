/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.custom;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AddAttribute;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.attribute.FlowAttribute;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.util.L10N;

/**
 * Represents a CanDI-style introspected bean type for configuration.
 */
public class TypeCustomBean<T> extends ConfigType<T>
{
  private static final L10N L = new L10N(TypeCustomBean.class);

  private static final NameCfg A_NEW = new NameCfg("new", null);

  private final Class<T> _beanClass;

  private final ConfigType<T> _beanType;

  private String _namespaceURI;

  private HashMap<NameCfg,AttributeConfig> _nsAttributeMap
    = new HashMap<>();

  public TypeCustomBean(Class<T> beanClass,
                        ConfigType<T> beanTypeConfig)
  {
    Objects.requireNonNull(beanClass);
    
    _beanClass = beanClass;

    _beanType = beanTypeConfig;

    int p = beanClass.getName().lastIndexOf('.');
    _namespaceURI = "urn:java:" + beanClass.getName().substring(0, p);

    addAttribute(A_NEW, AttributeCustomNew.ATTRIBUTE);
  }
  
  ConfigType<T> getBeanType()
  {
    return _beanType;
  }
  
  protected void addAttribute(NameCfg name, AttributeConfig attr)
  {
    _nsAttributeMap.put(name, attr);
  }

  /**
   * Returns the given type.
   */
  @Override
  public Class<T> getType()
  {
    return _beanClass;
  }

  /**
   * Creates a new instance
   */
  @Override
  public Object create(Object parent, NameCfg name)
  {
    return new CustomBean(name, _beanClass, null);
  }

  /**
   * Returns the attribute with the given name.
   */
  public AttributeConfig getAttribute(NameCfg qName)
  {
    AttributeConfig attr = _nsAttributeMap.get(qName);

    if (attr == null) {
      attr = getAttributeImpl(qName);

      if (attr != null) {
        _nsAttributeMap.put(qName, attr);
      }
    }
    
    return attr;
  }

  protected AttributeConfig getAttributeImpl(NameCfg qName)
  {
    AttributeConfig attr = _beanType.getAttribute(qName);

    if (attr != null) {
      return AttributeCustomProgram.ATTRIBUTE;
    }

    String uri = qName.getNamespaceURI();

    if ("new".equals(qName.getLocalName())) {
      return AttributeCustomNew.ATTRIBUTE;
    }

    if (uri == null) {
      return null;
    }
    else if (! uri.startsWith("urn:java:")) {
      return null;
    }

    Method method = null;
    if (uri.equals(_namespaceURI)
        && (method = findMethod(qName.getLocalName())) != null) {
      return new AttributeCustomMethod(_beanClass, method);
    }

    Field field = null;
    if (uri.equals(_namespaceURI)
        && (field = findField(qName.getLocalName())) != null) {
      return new AttributeCustomField(_beanClass, field);
    }

    if ("new".equals(qName.getLocalName())) {
      return AttributeCustomNew.ATTRIBUTE;
    }

    AttributeConfig envAttr
      = TypeFactoryConfig.getFactory().getEnvironmentAttribute(qName);

    if (envAttr instanceof FlowAttribute) {
      //        || envAttr instanceof EnvironmentAttribute) {
      // ioc/04c1
      return envAttr;
    }

    ConfigType<?> type = TypeFactoryConfig.getFactory().getEnvironmentType(qName);

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
      return new AttributeCustomAnnotation(cl);
    }

    AddAttribute addAttribute = (AddAttribute) _beanType.getAddAttribute(cl);

    if (addAttribute != null)
      return new AttributeCustomAdd(cl);

    throw new ConfigException(L.l("'{0}' is an unknown field or annotation",
                                  qName));
    // return new CustomBeanArgAttribute(cl);
  }

  @Override
  public AttributeConfig getProgramAttribute()
  {
    AttributeConfig attr = _beanType.getProgramAttribute();

    if (attr == null)
      return null;

    // server/1kl5
    return AttributeCustomProgram.ATTRIBUTE;
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
    CustomBean<?> customBean = (CustomBean<?>) bean;

    customBean.init();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _beanClass.getName() + "]";
  }
}
