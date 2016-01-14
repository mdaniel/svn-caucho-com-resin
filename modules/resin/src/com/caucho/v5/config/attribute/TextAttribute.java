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

import java.lang.reflect.Method;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;

public class TextAttribute<T> extends AttributeConfig {
  private final Method _setter;
  private final Class<T> _type;
  private ConfigType<T> _configType;

  public TextAttribute(Method setter, Class<T> type)
  {
    _setter = setter;
    _type = type;
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  public ConfigType<T> getConfigType()
  {
    if (_configType == null)
      _configType = TypeFactoryConfig.getType(_type);
    
    return _configType;
  }
  
  @Override
  public boolean isAssignableFrom(AttributeConfig attr)
  {
    if (! (attr instanceof TextAttribute<?>))
      return false;
    
    TextAttribute<?> textAttr = (TextAttribute<?>) attr;
    Method setter = textAttr._setter;
    
    if (! _setter.getName().equals(setter.getName()))
      return false;
    
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
      _setter.invoke(bean, getConfigType().valueOf(value));
    } catch (Exception e) {
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
      _setter.invoke(bean, value);
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
    return null;
  }
}
