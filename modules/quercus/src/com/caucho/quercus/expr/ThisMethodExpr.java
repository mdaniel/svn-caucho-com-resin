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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.InterpretedClassDef;

import java.util.ArrayList;

/**
 * Represents a PHP method call expression from $this.
 */
public class ThisMethodExpr extends ObjectMethodExpr {
  protected final InterpretedClassDef _classDef;

  protected final int _hashCodeInsensitive;
  protected boolean _isInit;

  protected AbstractFunction _fun;

  public ThisMethodExpr(Location location,
                        ThisExpr qThis,
                        StringValue methodName,
                        ArrayList<Expr> args)
  {
    super(location, qThis, methodName, args);

    _classDef = qThis.getClassDef();

    _hashCodeInsensitive = methodName.hashCodeCaseInsensitive();
  }

  //
  // java code generation
  //

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value eval(Env env)
  {
    if (! _isInit) {
      init();
    }

    Value qThis = _objExpr.eval(env);

    if (_fun != null) {
      return evalPrivate(env, _fun, qThis, _args);
    }
    else {
      return evalImpl(env, qThis, _methodName, _hashCodeInsensitive, _args);
    }
  }

  protected void init()
  {
    _isInit = true;

    AbstractFunction fun = _classDef.getFunction(_methodName);

    if (fun != null && fun.isPrivate()) {
      _fun = fun;
    }
  }

  private Value evalPrivate(Env env, AbstractFunction fun, Value qThis,
                            Expr []argExprs)
  {
    Value []args = evalArgs(env, argExprs);

    env.pushCall(this, qThis, args);

    try {
      env.checkTimeout();

      return fun.callMethod(env, qThis.getQuercusClass(), qThis, args);
    }
    finally {
      env.popCall();
    }
  }

  private Value evalImpl(Env env, Value qThis,
                         StringValue methodName, int hashCode,
                         Expr []argExprs)
  {
    Value []args = evalArgs(env, argExprs);

    env.pushCall(this, qThis, args);

    try {
      env.checkTimeout();

      return qThis.callMethod(env, methodName, hashCode, args);
    }
    finally {
      env.popCall();
    }
  }

  @Override
  public String toString()
  {
    return "$this->$" + _methodName + "()";
  }
}

