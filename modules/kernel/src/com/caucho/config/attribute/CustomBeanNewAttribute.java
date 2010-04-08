/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.attribute;

import java.lang.reflect.*;

import javax.enterprise.context.spi.CreationalContext;

import com.caucho.config.*;
import com.caucho.config.type.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.PropertyValueProgram;
import com.caucho.config.types.CustomBeanConfig;
import com.caucho.config.types.ConfigProgramArray;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public class CustomBeanNewAttribute extends Attribute {
  public static final Attribute ATTRIBUTE = new CustomBeanNewAttribute();

  private ConfigType _configType;

  private CustomBeanNewAttribute()
  {
    _configType = TypeFactory.getType(ConfigProgramArray.class);
  }

  public ConfigType getConfigType()
  {
    return _configType;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, QName name)
    throws ConfigException
  {
    return _configType.create(parent, name);
  }
  
  /**
   * Sets the value of the attribute
   */
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    CustomBeanConfig customBean = (CustomBeanConfig) bean;

    if (value instanceof ConfigProgramArray) {
      ConfigProgramArray args = (ConfigProgramArray) value;

      customBean.addArgs(args.getArgs());
    }
    else
      customBean.addArg(new PropertyValueProgram("value", value));
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object bean, QName name, String text)
    throws ConfigException
  {
    try {
      CustomBeanConfig customBean = (CustomBeanConfig) bean;

      customBean.addArg(new TextArgProgram(text));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  static class TextArgProgram extends ConfigProgram {
    private String _arg;

    TextArgProgram(String arg)
    {
      _arg = arg;
    }
    
    @Override
    public <T> void inject(T bean, CreationalContext<T> env)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public <T> T create(ConfigType<T> type, CreationalContext<T> env)
      throws ConfigException
    {
      return (T) type.valueOf(_arg);
    }
  }
}
