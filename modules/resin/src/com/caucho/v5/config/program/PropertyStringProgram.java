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
import com.caucho.v5.config.type.InlineBeanType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

/**
 * A saved program for configuring an object.
 */
public class PropertyStringProgram extends ConfigProgram {
  private static final L10N L = new L10N(PropertyStringProgram.class);
  
  private final String _name;
  private final NameCfg _qName;
  private final String _value;
  private final boolean _isOptional;

  public PropertyStringProgram(ConfigContext config,
                               String name, 
                               String value)
  {
    this(config, name, value, false);
  }

  public PropertyStringProgram(ConfigContext config,
                               String name, 
                               String value, 
                               boolean isOptional)
  {
    super(config);
    
    _name = name;
    _qName = new NameCfg(name);
    
    _value = value;
    _isOptional = isOptional;
    
    if (_name.startsWith("#")) {
      throw new IllegalArgumentException(L.l("name {0} with value {1}", _name, value));
    }
  }

  public PropertyStringProgram(ConfigContext config,
                               NameCfg qName, 
                               String value)
  {
    super(config);
    
    _name = qName.getLocalName();
    _qName = qName;
    _value = value;
    _isOptional = false;
    
    if (_name.startsWith("#")) {
      throw new IllegalArgumentException(L.l("name {0} with value {1}", qName, value));
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
      ConfigType<?> type = TypeFactoryConfig.getType(bean.getClass());

      AttributeConfig attr = type.getAttribute(_qName);

      if (attr != null)
        attr.setValue(bean, _qName, attr.getConfigType().valueOf(_value));
      else if (_qName.equals(InlineBeanType.TEXT) && "".equals(_value.trim())) {
        // server/3000
      }
      else if (! _isOptional)
        throw new ConfigException(L.l("'{0}' is an unknown property of '{1}' with value={2}",
                                      _qName, bean.getClass().getName(),
                                      _value));
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  @Override
  public <T> T create(ConfigType<T> type, InjectContext env)
    throws ConfigException
  {
    return (T) type.valueOf(_value);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _qName + "," + _value + "]";
  }
}
