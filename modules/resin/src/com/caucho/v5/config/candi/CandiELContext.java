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

package com.caucho.v5.config.candi;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.VariableMapper;
import javax.enterprise.inject.spi.BeanManager;

import com.caucho.v5.el.StackELResolver;

/**
 * Creates a variable resolver based on the classloader.
 */
public class CandiELContext extends ELContext {
  private StackELResolver _resolver;
  
  /**
   * Creates the resolver
   */
  public CandiELContext(BeanManager manager)
  {
    _resolver = new StackELResolver();
    _resolver.push(new BeanELResolver());
    _resolver.push(new ArrayELResolver());
    _resolver.push(new MapELResolver());
    _resolver.push(new ListELResolver());
    
    _resolver.push(manager.getELResolver());
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
    return getClass().getSimpleName() + "[]";
  }
}
