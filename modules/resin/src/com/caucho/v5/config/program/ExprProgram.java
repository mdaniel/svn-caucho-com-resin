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

import java.util.Objects;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.inject.impl.InjectContext;

/**
 * A saved program for configuring an object.
 */
public class ExprProgram extends ConfigProgram
{
  //private final Expr _expr;
  private final Object _expr;

  public ExprProgram(ConfigContext config, 
                     Object expr)
  {
    super(config);
    
    Objects.requireNonNull(expr);
    
    _expr = expr;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T create(ConfigType<T> type, InjectContext env)
    throws ConfigException
  {
    // return (T) type.valueOf(_expr.evalObject(ContextConfig.getELContext()));
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public <T> void inject(T bean, InjectContext env)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _expr + "]";
  }
}
