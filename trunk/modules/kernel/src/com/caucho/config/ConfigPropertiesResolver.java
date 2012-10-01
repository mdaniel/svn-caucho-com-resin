/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.config;

import javax.el.ELContext;
import javax.el.ELResolver;

import com.caucho.config.Config.ConfigProperties;
import com.caucho.el.AbstractVariableResolver;

/**
 * Variable resolver for Resin config properties.
 *
 */
public class ConfigPropertiesResolver extends AbstractVariableResolver {
  public static final String []RESIN_PROPERTIES = new String[] {
    "rvar0", "rvar1", "rvar2", "rvar3", "rvar4"
  };
  
  /**
   * Creates the resolver
   */
  public ConfigPropertiesResolver()
  {
  }
  
  /**
   * Creates the resolver
   */
  public ConfigPropertiesResolver(ELResolver next)
  {
    super(next);
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public Object getValue(ELContext env,
                         Object base,
                         Object property)
  {
    String var;
    
    if (base == null && property instanceof String)
      var = (String) property;
    else if (base == this && property instanceof String)
      var = (String) property;
    else
      return null;
    
    ConfigProperties properties = Config.getConfigProperties();
    
    if (properties == null)
      return null;
    
    Object value = null;
    
    for (String resinProp: RESIN_PROPERTIES) {
      String resinKey = (String) properties.get(resinProp);
      
      if (resinKey == null)
        break;
      
      value = properties.get(resinKey + '.' + var);
      
      if (value != null)
        break;
    }
    
    if (value == null)
      value = Config.getProperty(var);

    if (value != null) {
      env.setPropertyResolved(true);
      return value;
    }
    else
      return null;
  }

  /**
   * Returns the system property resolver.
   */
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
