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
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Represents a PHP static method expression ${class}:${foo}(...).
 */
public class ClassVarMethodVarExpr extends Expr
{
  private static final L10N L = new L10N(ClassVarMethodVarExpr.class);

  protected final Expr _className;
  protected final Expr _methodName;
  protected final Expr []_args;

  protected Expr []_fullArgs;

  protected AbstractFunction _fun;
  protected boolean _isMethod;

  public ClassVarMethodVarExpr(Location location,
                               Expr className,
                               Expr methodName,
                               ArrayList<Expr> args)
  {
    super(location);

    _className = className;
    _methodName = methodName;

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  //
  // expr creation
  //

  /**
   * Returns the reference of the value.
   * @param location
   */
  @Override
  public Expr createRef(QuercusParser parser)
  {
    return parser.getFactory().createRef(this);
  }

  /**
   * Returns the copy of the value.
   * @param location
   */
  @Override
  public Expr createCopy(ExprFactory factory)
  {
    return factory.createCopy(this);
  }

  //
  // evaluation
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
    String className = _className.evalString(env);

    QuercusClass cl = env.findClass(className);

    if (cl == null) {
      env.error(L.l("no matching class {0}", className), getLocation());
    }

    StringValue methodName = _methodName.evalStringValue(env);
    int hash = methodName.hashCodeCaseInsensitive();
    Value []args = evalArgs(env, _args);

    return cl.callMethod(env, env.getThis(),
                         methodName, hash,
                         args);
  }

  public String toString()
  {
    return _className + "::" + _methodName + "()";
  }
}

