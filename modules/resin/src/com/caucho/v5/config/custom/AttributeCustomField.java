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

import java.lang.reflect.Field;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.program.PropertyStringProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.util.L10N;

/**
 * Attribute for configuring an XML CanDI bean's field. The equivalent of
 * mybean.setFoo("stuff") is
 * 
 * <code><pre>
 * &lt;mypkg:MyBean>
 *   &lt;foo>stuff&lt;/foo>
 * &lt;/mypkg:MyBean>
 * </pre></code>
 *
 */
public class AttributeCustomField extends AttributeConfig {
  private static final L10N L = new L10N(AttributeCustomField.class);

  private static final NameCfg VALUE = new NameCfg("value");

  private final Field _field;
  private final ConfigType<ConfigCustomField> _configType;

  public AttributeCustomField(Class cl, Field field)
  {
    _field = field;
    _configType = TypeFactoryConfig.getType(ConfigCustomField.class);
  }

  public ConfigType getConfigType()
  {
    return _configType;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg qName)
    throws ConfigException
  {
    return new ConfigCustomField(_field);
  }
  
  /**
   * Sets the value of the attribute
   */
  public void setValue(Object bean, NameCfg name, Object value)
    throws ConfigException
  {
    CustomBean customBean = (CustomBean) bean;

    customBean.addField((ConfigCustomField) value);
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object parent, NameCfg name, String text)
    throws ConfigException
  {
    CustomBean customBean = (CustomBean) parent;
    
    ConfigContext config = ConfigContext.getCurrent();

    customBean.addBuilderProgram(new PropertyStringProgram(config, name, text));
  }
}
