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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

/**
 * Code for marshalling arguments.
 */
public class EnumMarshal extends Marshal {
  private Class _enumClass;

  public EnumMarshal(Class enumClass)
  {
    _enumClass = enumClass;
  }

  public Object marshal(Env env, Expr expr, Class argClass)
  {
    String name = expr.evalString(env);

    if (name == null) {
      return null;
    }

    try {
      return Enum.valueOf(_enumClass, name);
    }
    catch (IllegalArgumentException e) {
      env.warning(e);

      return null;
    }
  }

  public Object marshal(Env env, Value value, Class argClass)
  {
    return value.toJavaEnum(env, argClass);
  }

  public Value unmarshal(Env env, Object value)
  {
    if (value == null)
      return NullValue.NULL;
    else
      return env.wrapJava(value);
  }

  @Override
  public Class getExpectedClass()
  {
    return _enumClass;
  }

  @Override
  public int getMarshalingCostImpl(Value value)
  {
    // php/0cs1
    // returning a cost greater than all the other marshallings
    // to make sure they are tried before the enum
    // TODO re-check if FOUR is the correct cost or another would be better.
    return FOUR;
  }
}

