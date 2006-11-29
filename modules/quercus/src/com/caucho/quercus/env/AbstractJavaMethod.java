/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.env;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.L10N;

/**
 * Represents the introspected static function information.
 */
abstract public class AbstractJavaMethod extends AbstractFunction
{
  private static final L10N L = new L10N(AbstractJavaMethod.class);

  private static final Object [] NULL_ARGS = new Object[0];
  private static final Value [] NULL_VALUES = new Value[0];

  public Value call(Env env, Value []value)
  {
    return call(env, env.getThis(), value);
  }

  /**
   * Evaluates the function, returning a copy.  Java methods don't
   * need the copy.
   */
  public Value callCopy(Env env, Value []args)
  {
    return call(env, args);
  }

  public Value call(Env env, Value obj, Value []value)
  {
    return call(env, obj.toJavaObject(), value);
  }

  abstract public Value call(Env env, Object obj, Expr []args);

  abstract public Value call(Env env, Object obj, Value []args);

  public Value call(Env env, Object obj)
  {
    return call(env, obj, new Value[0]);
  }

  public Value call(Env env, Object obj, Value a1)
  {
    return call(env, obj, new Value[]{a1});
  }

  public Value call(Env env, Object obj, Value a1, Value a2)
  {
    return call(env, obj, new Value[]{a1, a2});
  }

  public Value call(Env env, Object obj, Value a1, Value a2, Value a3)
  {
    return call(env, obj, new Value[]{a1, a2, a3});
  }

  public Value call(Env env, Object obj,
                    Value a1, Value a2, Value a3, Value a4)
  {
    return call(env, obj, new Value[]{a1, a2, a3, a4});
  }

  public Value call(Env env, Object obj,
                    Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return call(env, obj, new Value[]{a1, a2, a3, a4, a5});
  }
}
