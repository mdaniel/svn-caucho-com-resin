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

package com.caucho.quercus.marshal;

import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

public class DoubleMarshal extends Marshal
{
  public static final DoubleMarshal MARSHAL = new DoubleMarshal();

  public boolean isDouble()
  {
    return true;
  }

  public boolean isReadOnly()
  {
    return true;
  }

  public Object marshal(Env env, Expr expr, Class expectedClass)
  {
    return new Double(expr.evalDouble(env));
  }

  public Object marshal(Env env, Value value, Class expectedClass)
  {
    return new Double(value.toDouble());
  }

  public Value unmarshal(Env env, Object value)
  {
    if (value == null)
      return DoubleValue.ZERO;
    else
      return new DoubleValue(((Number) value).doubleValue());
  }

  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    return argValue.toDoubleMarshalCost();

    /*
    if (argValue instanceof DoubleValue)
      return COST_EQUAL;
    else if (argValue.isLongConvertible())
      return COST_LOSSY_NUMERIC;
    else if (argValue.isDoubleConvertible())
      return COST_LOSSLESS_NUMERIC;
    else
      return Marshal.FOUR;
    */
  }

  public Class getExpectedClass()
  {
    return double.class;
  }
}
