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

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.util.L10N;

public class AttributeCustomArg extends AttributeConfig {
  private static final L10N L = new L10N(AttributeCustomArg.class);

  private final ConfigType _configType;

  public AttributeCustomArg(Class cl)
  {
    _configType = TypeFactoryConfig.getType(cl);
  }

  public ConfigType getConfigType()
  {
    return _configType;
  }

  /**
   * Returns true for a program-style attribute.
   */
  public boolean isProgram()
  {
    return true;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg qName)
    throws ConfigException
  {
    return _configType.create(parent, qName);
  }
  
  /**
   * Sets the value of the attribute
   */
  public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException
  {
    try {
      CustomBean customBean = (CustomBean) bean;

      customBean.addArg((ConfigProgram) value);
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object bean, NameCfg name, String text)
    throws ConfigException
  {
    try {
      CustomBean customBean = (CustomBean) bean;

      customBean.addArg(new TextArgProgram(ConfigContext.getCurrent(), text));
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
}
