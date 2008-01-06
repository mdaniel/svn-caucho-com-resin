/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.el.EnvironmentELResolver;
import com.caucho.el.MapVariableResolver;
import com.caucho.el.StackELResolver;
import com.caucho.el.SystemPropertiesResolver;

import javax.el.*;
import java.util.Map;

/**
 * Creates a variable resolver based on the classloader.
 */
public class ConfigELContext extends ELContext {
  private final StackELResolver _stackResolver = new StackELResolver();
  private final ELResolver _varResolver;
  
  /**
   * Creates the resolver
   */
  public ConfigELContext()
  {
    this(new MapVariableResolver());
  }
  
  /**
   * Creates the resolver
   */
  public ConfigELContext(Map<String,Object> map)
  {
    this(new MapVariableResolver(map));
  }
  
  /**
   * Creates the resolver
   */
  public ConfigELContext(ELResolver varResolver)
  {
    _varResolver = varResolver;

    _stackResolver.push(new BeanELResolver());
    _stackResolver.push(new ArrayELResolver());
    _stackResolver.push(new MapELResolver());
    _stackResolver.push(new ListELResolver());
    
    _stackResolver.push(new SystemPropertiesResolver());
    _stackResolver.push(EnvironmentELResolver.create());

    if (varResolver != null)
      _stackResolver.push(varResolver);
  }

  public void push(ELResolver elResolver)
  {
    _stackResolver.push(elResolver);
  }

  public ELResolver pop()
  {
    return _stackResolver.pop();
  }

  public ELResolver getVariableResolver()
  {
    return _varResolver;
  }

  public Object getValue(String var)
  {
    if (_varResolver != null)
      return _varResolver.getValue(this, var, null);
    else
      return null;
  }

  public void setValue(String var, Object value)
  {
    if (_varResolver != null)
      _varResolver.setValue(this, var, null, value);
  }

  @Override
  public ELResolver getELResolver()
  {
    return _stackResolver;
  }

  @Override
  public FunctionMapper getFunctionMapper()
  {
    return null;
  }

  @Override
  public VariableMapper getVariableMapper()
  {
    return null;
  }

  public String toString()
  {
    return "ConfigELContext[]";
  }
}
