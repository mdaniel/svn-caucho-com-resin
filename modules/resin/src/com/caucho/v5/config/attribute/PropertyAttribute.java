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
import com.caucho.v5.util.L10N;

public class PropertyAttribute extends AttributeConfig {
  private static final L10N L = new L10N(PropertyAttribute.class);
  
  private final Method _putMethod;
  private final ConfigType<?> _type;

  public PropertyAttribute(Method putMethod, ConfigType<?> type)
  {
    _putMethod = putMethod;
    _type = type;
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  @Override
  public ConfigType<?> getConfigType()
  {
    return _type;
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object bean, NameCfg name, String value)
    throws ConfigException
  {
    if ("#text".equals(name.getLocalName())) {
      if (value == null || value.trim().length() == 0)
        return;
      
      throw new ConfigException(L.l("text is not allowed for bean {0}\n  '{1}'",
                                    bean.getClass().getName(), value.trim()));
    }
    
    try {
      _putMethod.invoke(bean, name.getLocalName(), _type.valueOf(value));
    } catch (Exception e) {
      throw ConfigExceptionLocation.wrap(_putMethod, e);
    }
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException
  {
    if ("#text".equals(name.getLocalName()))
      throw new ConfigException(L.l("text is not allowed in this context\n  '{0}'",
                                    value));
    
    try {
      _putMethod.invoke(bean, name.getLocalName(), value);
    } catch (Exception e) {
      throw ConfigExceptionLocation.wrap(_putMethod, e);
    }
  }
}
