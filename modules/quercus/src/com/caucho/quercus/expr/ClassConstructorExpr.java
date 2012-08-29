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
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Represents a PHP A::A consturctor call
 */
public class ClassConstructorExpr extends Expr {
  private static final L10N L = new L10N(ClassConstructorExpr.class);

  protected final String _className;
  protected final StringValue _nameV;

  protected final Expr []_args;

  public ClassConstructorExpr(Location location,
                              String className,
                              StringValue nameV,
                              ArrayList<Expr> args)
  {
    super(location);

    _className = className.intern();
    _nameV = nameV;

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public ClassConstructorExpr(Location location,
                              String className,
                              StringValue nameV,
                              Expr []args)
  {
    super(location);

    _className = className.intern();

    _nameV = nameV;

    _args = args;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    QuercusClass cl = env.findClass(_className);

    if (cl == null)
      throw env.createErrorException(L.l("{0} is an unknown class",
                                         _className));

    StringValue nameV = _nameV;

    AbstractFunction fun = cl.getFunction(nameV);

    Value []values = evalArgs(env, _args);

    Value qThis = env.getThis();
    env.pushCall(this, qThis, values);

    try {
      env.checkTimeout();

      return cl.callMethod(env, qThis, nameV, nameV.hashCode(), values);
    } finally {
      env.popCall();
    }
  }

  public String toString()
  {
    return _className + "::" + _nameV + "()";
  }
}

