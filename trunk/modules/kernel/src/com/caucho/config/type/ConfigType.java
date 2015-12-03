/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.lang.reflect.Constructor;

import javax.el.ELContext;

import org.w3c.dom.Node;

import com.caucho.config.ConfigException;
import com.caucho.config.attribute.Attribute;
import com.caucho.config.attribute.FlowAttribute;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.el.Expr;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

/**
 * Represents an introspected configuration type.
 */
abstract public class ConfigType<T>
{
  private static final L10N L = new L10N(ConfigType.class);
  
  private boolean _isEnvBean;
  private boolean _isIntrospected;
  
  /**
   * Returns the Java type.
   */
  abstract public Class<T> getType();

  public void carefulIntrospect()
  {
    if (_isIntrospected)
      return;
    
    synchronized (this) {
      if (! _isIntrospected)
        introspect();
      
      _isIntrospected = true;
    }
  }
  /**
   * Introspect the type.
   */
  public void introspect()
  {
  }

  /**
   * Returns a printable name of the type.
   */
  public String getTypeName()
  {
    return getType().getName();
  }
  
  /**
   * Creates a new instance of the type.
   */
  public Object create(Object parent, QName name)
  {
    return null;
  }
  
  /**
   * Creates a top-level instance of the type.
   */
  public ConfigType<?> createType(QName name)
  {
    return null;
  }
  
  /**
   * Returns the config type of the child bean.
   */
  public ConfigType<?> getType(Object childBean)
  {
    return TypeFactory.getType(childBean);
  }
  
  /**
   * Inject and initialize the type
   */
  public void inject(Object bean)
  {
  }
  
  /**
   * Initialize the type
   */
  public void init(Object bean)
  {
  }
  
  /**
   * Replace the type with the generated object
   */
  public Object replaceObject(Object bean)
  {
    return bean;
  }

  /**
   * Returns the constructor with the given number of arguments
   */
  public Constructor<?> getConstructor(int count)
  {
    throw new ConfigException(L.l("'{0}' does not support <new> constructors",
                                  this));
  }
  
  /**
   * Converts the string to a value of the type.
   */
  abstract public Object valueOf(String text);
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value instanceof String)
      return valueOf((String) value);
    else
      return value;
  }
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(ELContext env, Expr value)
  {
    return valueOf(value.getValue(env));
  }

  /**
   * Returns true for a bean-style type.
   */
  public boolean isBean()
  {
    return false;
  }

  /**
   * Returns true for an XML node type.
   */
  public boolean isNode()
  {
    return false;
  }

  /**
   * Return true for non-trim.
   */
  public boolean isNoTrim()
  {
    return false;
  }

  /**
   * Return true for EL evaluation
   */
  public boolean isEL()
  {
    return true;
  }

  /**
   * Returns true for an array type
   */
  public boolean isArray()
  {
    return false;
  }

  /**
   * Return true if the object is replaced
   */
  public boolean isReplace()
  {
    return false;
  }
  
  /**
   * Returns true for a qualifier annotation
   */
  public boolean isQualifier()
  {
    return false;
  }

  /**
   * Returns true for a program type.
   */
  public boolean isProgram()
  {
    return false;
  }

  public ConfigType<?> getComponentType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Attribute getDefaultAttribute(QName qName)
  {
    Attribute attrStrategy = getProgramAttribute();

    if (attrStrategy != null) {
      return attrStrategy;
    }

    // ioc/2252 - flow attributes are not captured by ContentProgram

    attrStrategy = getContentProgramAttribute();

    TypeFactory factory = TypeFactory.getFactory();

    Attribute envStrategy = factory.getEnvironmentAttribute(qName);

    if (envStrategy instanceof FlowAttribute
        || envStrategy != null && attrStrategy == null) {
      return envStrategy;
    }
    else if (attrStrategy != null) {
      return attrStrategy;
    }

    if (qName.getNamespaceURI() != null
        && qName.getNamespaceURI().startsWith("urn:java:")) {
      return getAddBeanAttribute(qName);
    }
    else {
      return null;
    }
  }

  public Attribute getAddBeanAttribute(QName qName)
  {
    return null;
  }

  /**
   * Returns the attribute with the given name.
   */
  public Attribute getAttribute(QName qName)
  {
    // ioc/0250
    return null;
  }

  /**
   * Sets a property based on an attribute name, returning true if
   * successful.
   */
  public boolean setProperty(Object bean,
                             QName name,
                             Object value)
  {
    Attribute attr = getAttribute(name);

    if (attr != null) {
      attr.setValue(bean, name, attr.getConfigType().valueOf(value));
      
      return true;
    }
    else
      return false;
  }

  /**
   * Returns the program attribute.
   */
  public Attribute getProgramAttribute()
  {
    return null;
  }

  /**
   * Returns the flow program attribute, i.e. attributes that also
   * save if/choose without interpreting.
   */
  public Attribute getContentProgramAttribute()
  {
    return null;
  }

  /**
   * Returns any add attributes to add arbitrary content
   */
  public Attribute getAddAttribute(Class<?> cl)
  {
    return null;
  }

  /**
   * Called before the children are configured.
   */
  public void beforeConfigureBean(XmlConfigContext builder,
                                  Object bean,
                                  Node node)
  {
  }

  /**
   * Called before the children are configured.  Also called for
   * attribute configuration, e.g. for macros and web-app-default.
   */
  public void beforeConfigure(XmlConfigContext builder, Object bean, Node node)
  {
  }

  /**
   * Called after the children are configured.
   */
  public void afterConfigure(XmlConfigContext builder, Object bean)
  {
  }

  public boolean isConstructableFromString()
  {
    return true;
  }

  public boolean isInlineType(ConfigType<?> type)
  {
    return false;
  }

  /**
   * Returns true for an environment bean.
   */
  public boolean isEnvBean()
  {
    return _isEnvBean;
  }

  /**
   * Returns true for an environment bean.
   */
  public void setEnvBean(boolean isEnvBean)
  {
    _isEnvBean = isEnvBean;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getTypeName() + "]";
  }
}
