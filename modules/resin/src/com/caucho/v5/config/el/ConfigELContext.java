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

package com.caucho.v5.config.el;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.VariableMapper;

import com.caucho.v5.config.ConfigPropertiesResolver;
import com.caucho.v5.el.StackELResolver;
import com.caucho.v5.el.SystemPropertiesResolver;
import com.caucho.v5.jmx.MBeanAttributeELResolver;

/**
 * Creates a variable resolver based on the classloader.
 */
public class ConfigELContext extends ELContext
{
  public static final ConfigELContext EL_CONTEXT;
  
  private static final StackELResolver _stackResolver;

  private ELResolver _resolver = _stackResolver;
  
  /**
   * Creates the resolver
   */
  public ConfigELContext()
  {
  }
  
  /**
   * Creates the resolver
   */
  public ConfigELContext(ELResolver resolver)
  {
    _resolver = new StackELResolver(resolver, _resolver);
  }

  @Override
  public ELResolver getELResolver()
  {
    return _resolver;
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

  static {
    _stackResolver = new StackELResolver();
    _stackResolver.push(new BeanELResolver());
    _stackResolver.push(new ArrayELResolver());
    _stackResolver.push(new MapELResolver());
    _stackResolver.push(new ListELResolver());
    
    _stackResolver.push(new SystemPropertiesResolver());
    _stackResolver.push(new ConfigPropertiesResolver());
    _stackResolver.push(new CandiConfigResolver());
    _stackResolver.push(new ConfigContextResolver());
    _stackResolver.push(new MBeanAttributeELResolver());
    
    EL_CONTEXT = new ConfigELContext();
  }
}
