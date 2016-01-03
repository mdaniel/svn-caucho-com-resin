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

package com.caucho.v5.config;

import javax.el.ELContext;
import javax.el.ELResolver;

import com.caucho.v5.el.ELUtil;
import com.caucho.v5.el.VariableResolverBase;

/**
 * Variable resolver for config properties.
 *
 */
public class ConfigPropertiesResolver extends VariableResolverBase {
  public static final String []PROPERTIES = new String[] {
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
    
    /*
    ConfigProperties properties = Config.getConfigProperties();
    
    if (properties == null)
      return null;
      */
    
    Object value = null;
    
    for (String prop : PROPERTIES) {
      String key = (String) ConfigContext.getProperty(prop);

      if (key == null) {
        break;
      }
      
      value = ConfigContext.getProperty(key + '.' + var);
      
      if (value != null) {
        break;
      }
    }
    
    if (value == null) {
      value = ConfigContext.getProperty(var);
    }

    if (value != null) {
      if (ELUtil.isJavaee7()) {
        setPropertyResolved(env, base, property);
      }
      else {
        env.setPropertyResolved(true);
      }
      
      return value;
    }
    else
      return null;
  }
  
  private void setPropertyResolved(ELContext env, Object base, Object property)
  {
    env.setPropertyResolved(base, property);
  }

  /**
   * Returns the system property resolver.
   */
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
