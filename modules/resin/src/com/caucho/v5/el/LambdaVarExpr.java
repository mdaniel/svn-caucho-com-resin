/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 * @author Paul Cowan
 */

package com.caucho.v5.el;

import javax.el.ELContext;
import javax.el.ELException;

/**
 * Identifier expression.
 */
public class LambdaVarExpr extends IdExpr {
  /**
   * Creates the identifier
   */
  public LambdaVarExpr(String id)
  {
    super(id);
  }

  /**
   * Evaluate the expr as an object.
   *
   * @param env the variable environment
   *
   * @return the value as an object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    Object value = env.getLambdaArgument(getId());
        
    return value;
  }

  /**
   * Evaluates the expression, setting an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  @Override
  public void setValue(ELContext env, Object value)
    throws ELException
  {
    // throw exception
  }
}
