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

package com.caucho.v5.config.cf;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;

/**
 * Program to assign parameters.
 */
public class ProgramPropertyString extends ProgramIdBase
{
  private final String _value;
  
  public ProgramPropertyString(ConfigContext config, String id, String value)
  {
    super(config, id);
    
    _value = value;
  }
  
  public ProgramPropertyString(ConfigContext config, NameCfg id, String value)
  {
    super(config, id);
    
    _value = value;
  }

  public String getValue()
  {
    return _value;
  }
  
  @Override
  public <T> void inject(T bean, InjectContext env)
  {
    inject(bean, TypeFactoryConfig.getType(bean), env);
  }
  
  @Override
  public <T> void inject(T bean, ConfigType<T> type, InjectContext env)
  {
    NameCfg id = getId();
    String value = _value;
    
    AttributeConfig attr = getAttribute(type);
    
    /*
    if (attr == null && _id.getLocalName().startsWith("_p")) {
      // config/1203 - rest values
      attr = type.getAttribute("_rest");
      
      if (attr != null) {
        id = new QName("_rest");
      }
    }
    */
    
    if (attr == null) {
      // config/1201 - boolean flags
      attr = type.getAttribute(_value);
      
      if (attr != null) {
        value = "true";
        id = new NameCfg(_value);
      }
    }
    
    if (attr == null) {
      /*
      throw error("'{0}' is an unknown property of {1}",
                  id.getLocalName(), bean.getClass().getName());
                  */
      throw error("'{0}' is an unknown property of {1}",
                  id.getLocalName(), type.getType().getName());
    }

    try {
      if (attr.isProgram()) {
        attr.setValue(bean, id, this);
      }
      else {
        //System.out.println("VS: " + attr.getConfigType() + " " + attr.getConfigType().valueOf(value));
        //attr.setValue(bean, id, attr.getConfigType().valueOf(value));
        attr.setText(bean, id,  value);
      }
    } catch (Exception e) {
      throw error(e);
    }
  }

  @Override
  public <T> T create(ConfigType<T> type, InjectContext env)
    throws ConfigException
  {
    return (T) type.valueOf(getValue());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "=" + _value + "]";
  }
}

