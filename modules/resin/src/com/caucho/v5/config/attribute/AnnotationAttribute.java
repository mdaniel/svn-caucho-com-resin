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

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.RawStringArrayType;
import com.caucho.v5.config.type.RawStringType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.config.types.AnnotationConfig;

public class AnnotationAttribute<T> extends AttributeConfig {
  private String _name;
  private ConfigType<T> _type;

  public AnnotationAttribute(String name, Class<T> type, boolean isEL)
  {
    _name = name;

    if (isEL)
      _type = TypeFactoryConfig.getType(type);
    else if (String.class.equals(type))
      _type = (ConfigType) RawStringType.TYPE;
    else if (String[].class.equals(type))
      _type = RawStringArrayType.TYPE;
    else
      _type = TypeFactoryConfig.getType(type);
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  public ConfigType<T> getConfigType()
  {
    return _type;
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
    else if (_type.isInlineType(type))
      return true;
    else
      return _type.getType().isAssignableFrom(type.getType());
  }

  /**
   * Creates the child bean.
   */
  public Object create(Object parent, NameCfg name)
    throws ConfigException
  {
    // ioc/04f7
    // ejb/1332 - need to refactor to remove isArray test
    /*
    if (_type.isArray())
      return _type.getComponentType().create(parent, name);
    else
      return _type.create(parent, name);
      */
    return _type.create(parent, name);
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object bean, NameCfg name, String value)
    throws ConfigException
  {
    try {
      AnnotationConfig ann = (AnnotationConfig) bean;

      ann.setAttribute(name.getLocalName(), _type.valueOf(value));
    } catch (Exception e) {
      throw ConfigException.wrap(e);
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
      AnnotationConfig ann = (AnnotationConfig) bean;

      ann.setAttribute(name.getLocalName(), value);
      //_putMethod.invoke(bean, name.getLocalName(), value);
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
}
