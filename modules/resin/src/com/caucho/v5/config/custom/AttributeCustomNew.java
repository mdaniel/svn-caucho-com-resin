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
import com.caucho.v5.config.program.ExprProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;

public class AttributeCustomNew extends AttributeConfig
{
  public static final AttributeConfig ATTRIBUTE = new AttributeCustomNew();

  private ConfigType<?> _configType;

  private AttributeCustomNew()
  {
    _configType = TypeFactoryConfig.getType(ConfigCustomNew.class);
  }

  @Override
  public ConfigType<?> getConfigType()
  {
    return _configType;
  }
  
  @Override
  public boolean isEL()
  {
    return false;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, NameCfg name)
    throws ConfigException
  {
    return new ConfigCustomNew((CustomBean<?>) parent);
  }
  
  /**
   * Sets the value of the attribute as text
   */
  public void setText(Object object, NameCfg name, String value)
    throws ConfigException
  {
    CustomBean bean = (CustomBean) object;
    
    //Expr expr = (Expr) ExprType.TYPE.valueOf(value);
    Object expr = null;
    
    bean.addArg(new ExprProgram(ConfigContext.getCurrent(), expr));
  }

  @Override
  public void setValue(Object bean, NameCfg name, Object value)
      throws ConfigException
  {
  }
}
