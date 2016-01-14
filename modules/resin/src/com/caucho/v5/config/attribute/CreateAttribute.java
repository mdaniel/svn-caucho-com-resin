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
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;

public class CreateAttribute<T> extends AttributeConfig
{
  private static final Logger log
    = Logger.getLogger(CreateAttribute.class.getName());

  private final Method _create;
  private final Method _setter;
  
  private MethodHandle _createHandle;
  private MethodHandle _setterHandle;
  
  private Class<T> _type;
  
  private ConfigType<T> _configType;

  public CreateAttribute(Method create, Class<T> type)
  {
    _create = create;
    
    if (_create != null) {
      _create.setAccessible(true);
      try {
        _createHandle = MethodHandles.lookup().unreflect(_create);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    _type = type;

    _setter = null;
  }

  public CreateAttribute(Method create, Class<T> type, Method setter)
  {
    _create = create;
    
    if (_create != null) {
      _create.setAccessible(true);
      
      try {
        _createHandle = MethodHandles.lookup().unreflect(_create);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    _type = type;

    _setter = setter;
    
    if (_setter != null) { 
      _setter.setAccessible(true);
      
      try {
        _setterHandle = MethodHandles.lookup().unreflect(_setter);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  public ConfigType<?> getConfigType()
  {
    if (_configType == null)
      _configType = TypeFactoryConfig.getType(_type);
    
    return _configType;
  }

  /**
   * True if it allows text.
   */
  @Override
  public boolean isAllowText()
  {
    return false;
  }

  /**
   * True if it allows inline beans
   */
  @Override
  public boolean isAllowInline()
  {
    return _setter != null;
  }

  /**
   * True if it allows inline beans
   */
  @Override
  public boolean isInlineType(ConfigType<?> type)
  {
    // server/0219
    
    if (_setter == null)
      return false;
    else if (type == null)
      return false;
    else
      return _type.isAssignableFrom(type.getType());
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException
  {
    try {
      if (_setterHandle != null) {
        _setterHandle.invoke(bean, value);
      }
      else if (_setter != null) {
        _setter.invoke(bean, value);
      }
    } catch (Throwable e) {
      throw ConfigExceptionLocation.wrap(_setter, e);
    }
  }

  /**
   * Returns true for attributes which create objects.
   */
  public boolean isSetter()
  {
    return _setter != null;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg name)
    throws ConfigException
  {
    try {
      if (_createHandle != null) {
        return _createHandle.invoke(parent);
      }
      else {
        return _create.invoke(parent);
      }
    } catch (Throwable e) {
      throw ConfigExceptionLocation.wrap(_create, e);
    }
  }
  
  
  @Override
  public boolean isAssignableFrom(AttributeConfig attr)
  {
    if (! (attr instanceof CreateAttribute<?>))
      return false;
    
    CreateAttribute<?> createAttr = (CreateAttribute<?>) attr;
    Method create = createAttr._create;

    if (create == null || _create == null)
      return false;
    
    if (! _create.getName().equals(create.getName()))
      return false;
    
    if (! _create.getDeclaringClass().isAssignableFrom(create.getDeclaringClass()))
      return false;
    
    Method setter = createAttr._setter;

    if ((setter == null) != (_setter == null))
      return false;
    
    if (setter == null)
      return true;
    
    if (! _setter.getName().equals(setter.getName()))
      return false;
    
    if (! _setter.getDeclaringClass().isAssignableFrom(setter.getDeclaringClass()))
      return false;
    
    return true;
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
    
    CreateAttribute<?> attr = (CreateAttribute<?>) o;
    
    return (_type.equals(attr._type)
            && _setter == attr._setter
            && _create == attr._create);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[");
    
    if (_create != null) {
      sb.append(_create.getName()).append(",");
    }
    
    if (_setter != null) {
      sb.append(_setter.getName()).append(",");
    }
    
    if (_create != null) {
      sb.append(_create.getDeclaringClass().getSimpleName());
    }
    else if (_setter != null) {
      sb.append(_setter.getDeclaringClass().getSimpleName());
    }
    else {
      sb.append(getConfigType());
    }
    sb.append("]");
    
    return sb.toString();
  }
  
}
