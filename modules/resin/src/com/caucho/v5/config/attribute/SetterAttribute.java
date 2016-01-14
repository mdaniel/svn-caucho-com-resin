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

package com.caucho.v5.config.attribute;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.util.L10N;

public class SetterAttribute<T> extends AttributeConfig {
  private static final L10N L = new L10N(SetterAttribute.class);
  private static final Logger log
    = Logger.getLogger(SetterAttribute.class.getName());
  
  private final Method _setter;
  private MethodHandle _setterHandle;
  private final Class<T> _type;
  private ConfigType<T> _configType;

  public SetterAttribute(Method setter, Class<T> type)
  {
    setter.setAccessible(true);
    _setter = setter;
    
    try {
      MethodHandle setterHandle = MethodHandles.lookup().unreflect(_setter);
      setterHandle = setterHandle.asType(MethodType.methodType(void.class, 
                                                               Object.class,
                                                               Object.class));
      _setterHandle = setterHandle;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    _type = type;
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  @Override
  public ConfigType<T> getConfigType()
  {
    if (_configType == null)
      _configType = TypeFactoryConfig.getType(_type);
    
    return _configType;
  }

  @Override
  public boolean isAllowText()
  {
    return getConfigType().isConstructableFromString();
  }

  /**
   * True if it allows inline beans
   */
  @Override
  public boolean isAllowInline()
  {
    return true;
  }
  

  /**
   * True if it allows inline beans
   */
  @Override
  public boolean isInlineType(ConfigType<?> type)
  {
    if (type == null)
      return false;
    else if (type.isReplace())
      return true;
    else if (getConfigType().isInlineType(type))
      return true;
    else if (type.isQualifier())
      return true;
    else
      return _type.isAssignableFrom(type.getType());
  }
  
  /**
   * Returns true if the setter is marked with @Configurable
   */
  @Override
  public boolean isConfigurable()
  {
    return _setter.isAnnotationPresent(Configurable.class);
  }
  
  @Override
  public boolean isAssignableFrom(AttributeConfig attr)
  {
    if (! (attr instanceof SetterAttribute<?>)) {
      return false;
    }
    
    SetterAttribute<?> setterAttr = (SetterAttribute<?>) attr;
    Method setter = setterAttr._setter;
    
    if (! _setter.getName().equals(setter.getName())) {
      return false;
    }
    
    return _setter.getDeclaringClass().isAssignableFrom(setter.getDeclaringClass());
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object bean, NameCfg name, String value)
    throws ConfigException
  {
    try {
      ConfigType<?> configType = getConfigType();

      if (_setterHandle != null) {
        _setterHandle.invoke(bean, configType.valueOf(value));
      }
      else {
        _setter.invoke(bean, configType.valueOf(value));
      }
    } catch (Throwable e) {
      throw ConfigExceptionLocation.wrap(_setter, e);
    }
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException
  {
    try {
      Objects.requireNonNull(bean);
      
      if (_setterHandle != null) {
        _setterHandle.invoke(bean, value);
      }
      else {
        _setter.invoke(bean, value);
      }
    } catch (IllegalArgumentException e) {
      throw ConfigExceptionLocation.wrap(_setter,
                                   L.l("'{0}' is an illegal value.",
                                       value),
                                       e);
    } catch (Throwable e) {
//      System.out.println("SH-EXN: " + e + " " + _setterHandle + " " + _setter + " " + bean + " " + value);
      throw ConfigExceptionLocation.wrap(_setter, e);
    }
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg name, ConfigType<?> configType)
    throws ConfigException
  {
    try {
      if (configType != null && _type.isAssignableFrom(configType.getType())) {
        // ioc/2172
        return configType.create(parent, name);
      }
      else {
        return getConfigType().create(parent, name);
      }
    } catch (Exception e) {
      throw ConfigExceptionLocation.wrap(_setter, e);
    }
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg name)
    throws ConfigException
  {
    try {
      return getConfigType().create(parent, name);
    } catch (Exception e) {
      throw ConfigExceptionLocation.wrap(_setter, e);
    }
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o == null)
      return false;
    else if (getClass() != o.getClass())
      return false;
    
    SetterAttribute<?> attr = (SetterAttribute<?>) o;
    
    return _type.equals(attr._type) && _setter.equals(attr._setter);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _setter + "]";
  }
}
