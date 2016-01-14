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
 * @author Scott Ferguson;
 */

package com.caucho.v5.config.program;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

/**
 * A saved program for configuring an object.
 */
public class PropertyValueProgram extends ConfigProgram {
  private static final L10N L = new L10N(PropertyValueProgram.class);
  
  private final String _name;
  private final NameCfg _qName;
  private final Object _value;

  private AttributeConfig _attr;

  public PropertyValueProgram(String name,
                              Object value)
  {
    this(ConfigContext.getCurrent(), name, value);
  }

  public PropertyValueProgram(ConfigContext config, 
                              String name, 
                              Object value)
  {
    this(config, null, name, value);
  }

  public PropertyValueProgram(ConfigContext config,
                              Class<?> type, 
                              String name, 
                              Object value)
  {
    super(config);
    
    _name = name;
    _qName = new NameCfg(name);
    _value = value;

    if (type != null) {
      ConfigType<?> configType = TypeFactoryConfig.getType(type);

      _attr = configType.getAttribute(_qName);
      
      if (_attr == null) {
        _attr = configType.getProgramAttribute();
      }
      
      if (_attr == null) {
        _attr = configType.getProgramContentAttribute();
      }
    }
  }
  
  /**
   * Returns the injection name.
   */
  public String getName()
  {
    return _name;
  }
  
  //
  // Inject API
  //
  
  /**
   * Injects the bean with the dependencies
   */
  @Override
  public <T> void inject(T bean, InjectContext env)
  {
    try {
      AttributeConfig attr = _attr;

      if (attr == null) {
        ConfigType<?> type = TypeFactoryConfig.getType(bean.getClass());

        attr = type.getAttribute(_qName);
        
        if (attr == null) {
          attr = type.getProgramAttribute();
        }
        
        if (attr == null) {
          attr = type.getProgramContentAttribute();
        }
      }

      if (attr == null) {
        throw new ConfigException(L.l("'{0}' is an unknown attribute of '{1}'",
                                      _qName.getName(), bean.getClass().getName()));
      }
      else if (attr.isProgram()) {
        attr.setValue(bean, _qName, this);
      }
      else {
        attr.setValue(bean, _qName, attr.getConfigType().valueOf(_value));
      }
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T create(ConfigType<T> type, InjectContext env)
    throws ConfigException
  {
    // ioc/04d7
    
    return (T) type.valueOf(_value);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "," + _value + "]";
  }
}
