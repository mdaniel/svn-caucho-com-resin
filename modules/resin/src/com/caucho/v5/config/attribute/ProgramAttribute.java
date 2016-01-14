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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;

public class ProgramAttribute extends AttributeConfig {
  private final Method _setter;
  private final ConfigType _type;

  public ProgramAttribute(Method setter, ConfigType type)
  {
    _setter = setter;
    _setter.setAccessible(true);
    _type = type;
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  public ConfigType getConfigType()
  {
    return _type;
  }

  /**
   * Returns true for a program-style attribute.
   */
  @Override
  public boolean isProgram()
  {
    return true;
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
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      
      if (cause instanceof ConfigException) {
        // throw ConfigException.create(getMethodName() + ": [" + bean + "] " + cause.getMessage(), e);
        throw (ConfigException) cause;
      }
      else {
        throw ConfigExceptionLocation.wrap(_setter, "[" + bean + "] " + cause.toString(), e);
      }
    } catch (Exception e) {
      throw ConfigExceptionLocation.wrap(_setter, "[" + bean + "] " + e, e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _setter + "]";
  }
}
