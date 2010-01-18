/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

/**
 * Represents a variable class field reference $class::${"b"}.
 */
public class VarClassVarFieldExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(VarClassVarFieldExpr.class);

  protected final Expr _className;
  protected final Expr _varName;

  public VarClassVarFieldExpr(Expr className, Expr varName)
  {
    _className = className;

    _varName = varName;
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
    String className = _className.evalString(env);
    String varName = _varName.evalString(env);

    return env.getStaticClassFieldValue(className, varName);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalArg(Env env, boolean isTop)
  {
    String className = _className.evalString(env);
    String varName = _varName.evalString(env);

    return env.getStaticClassFieldVar(className, varName);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    String className = _className.evalString(env);
    String varName = _varName.evalString(env);

    return env.getStaticClassFieldVar(className, varName);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalAssign(Env env, Value value)
  {
    String className = _className.evalString(env);
    String varName = _varName.evalString(env);

    env.getStaticClassFieldVar(className, varName).set(value);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalUnset(Env env)
  {
    env.error(getLocation(),
              L.l("{0}::${1}: Cannot unset class variables.",
                  _className, _varName));
  }

  public String toString()
  {
    return _className + "::$" + _varName;
  }
}

