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

package com.caucho.v5.config.type;

import java.lang.reflect.Constructor;
import java.util.Objects;

import org.w3c.dom.Node;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.attribute.FlowAttribute;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.util.L10N;

/**
 * Represents an introspected configuration type.
 */
abstract public class ConfigType<T>
{
  private static final L10N L = new L10N(ConfigType.class);
  
  private final TypeFactoryConfig _typeFactory;
  
  private boolean _isEnvBean;
  private boolean _isIntrospected;
  
  protected ConfigType()
  {
    this(TypeFactoryConfig.getFactory());
  }
    
  protected ConfigType(TypeFactoryConfig typeFactory)
  {
    Objects.requireNonNull(typeFactory);
    
    _typeFactory = typeFactory;
  }
  
  public TypeFactoryConfig getFactory()
  {
    return _typeFactory;
  }
  
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
  public Object create(Object parent, NameCfg name)
  {
    return null;
  }
  
  /**
   * Creates a top-level instance of the type.
   */
  public ConfigType<?> createType(NameCfg name)
  {
    return null;
  }
  
  /**
   * Returns the config type of the child bean.
   */
  public ConfigType<?> getType(Object childBean)
  {
    return TypeFactoryConfig.getType(childBean);
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
  /*
  public Object valueOf(ELContext env, Expr value)
  {
    return valueOf(CandiValueExpression.valueOf(value, env));
  }
  */

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

  /**
   * Returns true for a program type.
   */
  public boolean isProgramContainer()
  {
    return false;
  }

  public ConfigType<?> getComponentType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AttributeConfig getDefaultAttribute(NameCfg qName)
  {
    AttributeConfig attrStrategy = getProgramAttribute();

    if (attrStrategy != null) {
      return attrStrategy;
    }

    // ioc/2252 - flow attributes are not captured by ContentProgram

    attrStrategy = getProgramContentAttribute();

    TypeFactoryConfig factory = getFactory();

    AttributeConfig envStrategy = factory.getEnvironmentAttribute(qName);

    if (envStrategy instanceof FlowAttribute
        || envStrategy != null && attrStrategy == null) {
      return envStrategy;
    }
    else if (attrStrategy != null) {
      return attrStrategy;
    }

    if (qName.getNamespaceURI() != null
        && qName.getNamespaceURI().startsWith("urn:java:")) {
      if (getProgramBeanAttribute() != null) {
        return getProgramBeanAttribute();
      }
      else {
        return getAddBeanAttribute(qName);
      }
    }
    else {
      return null;
    }
  }

  public AttributeConfig getAddBeanAttribute(NameCfg qName)
  {
    return null;
  }

  /**
   * Returns the attribute with the given name.
   */
  public AttributeConfig getAttribute(NameCfg qName)
  {
    // ioc/0250
    return null;
  }

  /**
   * Returns the attribute with the given name.
   */
  public AttributeConfig getAttribute(String id)
  {
    return getAttribute(new NameCfg(id));
  }

  /**
   * Sets a property based on an attribute name, returning true if
   * successful.
   */
  public boolean setProperty(Object bean,
                             NameCfg name,
                             Object value)
  {
    AttributeConfig attr = getAttribute(name);

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
  public AttributeConfig getProgramAttribute()
  {
    return null;
  }

  /**
   * Returns the flow program attribute, i.e. attributes that also
   * save if/choose without interpreting.
   */
  public AttributeConfig getProgramContentAttribute()
  {
    return null;
  }

  /**
   * Returns bean attribute, saving beans but not fields. 
   */
  public AttributeConfig getProgramBeanAttribute()
  {
    return null;
  }

  /**
   * Returns any add attributes to add arbitrary content
   */
  public AttributeConfig getAddAttribute(Class<?> cl)
  {
    return null;
  }

  /**
   * Called before the children are configured.
   */
  public void beforeConfigureBean(ContextConfig builder,
                                  Object bean)
  {
  }
  
  public void setLocation(Object bean, String location)
  {
  }

  /**
   * Called before the children are configured.
   */
  public void beforeConfigureBean(ContextConfig builder,
                                  Object bean,
                                  Node node)
  {
    beforeConfigureBean(builder, bean);
  }

  /**
   * Called before the children are configured.  Also called for
   * attribute configuration, e.g. for macros and web-app-default.
   */
  public void beforeConfigure(ContextConfig builder, Object bean)
  {
  }

  /**
   * Called before the children are configured.  Also called for
   * attribute configuration, e.g. for macros and web-app-default.
   */
  public void beforeConfigure(ContextConfig builder, Object bean, Node node)
  {
    beforeConfigure(builder, bean);
  }

  /**
   * Called after the children are configured.
   */
  public void afterConfigure(ContextConfig builder, Object bean)
  {
  }

  /**
   * Called before the children are configured.  Also called for
   * attribute configuration, e.g. for macros and web-app-default.
   */
  public void beforeConfigure(Object bean)
  {
  }

  /**
   * Called after the children are configured.
   */
  public void afterConfigure(Object bean)
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

  public boolean isFlow()
  {
    return false;
  }

  /**
   * Config usage.
   */
  public String getAttributeUsage()
  {
    return "";
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getTypeName() + "]";
  }
}
