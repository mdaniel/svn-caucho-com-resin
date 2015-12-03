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
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;

/**
 * Represents a PHP array reference expression.
 */
public class ArrayGetExpr extends AbstractVarExpr {
  protected final Expr _expr;
  protected final Expr _index;

  public ArrayGetExpr(Location location, Expr expr, Expr index)
  {
    super(location);
    _expr = expr;
    _index = index;
  }

  public ArrayGetExpr(Expr expr, Expr index)
  {
    _expr = expr;
    _index = index;
  }

  /**
   * Returns the expr.
   */
  public Expr getExpr()
  {
    return _expr;
  }

  /**
   * Returns the index.
   */
  public Expr getIndex()
  {
    return _index;
  }

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
    Value array = _expr.eval(env);
    Value index = _index.eval(env);

    return array.get(index);
  }

  /**
   * Evaluates the expression as a copyable result.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalCopy(Env env)
  {
    Value array = _expr.eval(env);
    Value index = _index.eval(env);

    return array.get(index).copy();
  }

  /**
   * Evaluates the expression, creating an array if the value is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalArray(Env env)
  {
    Value array = _expr.evalArray(env);
    Value index = _index.eval(env);

    return array.getArray(index);
  }

  /**
   * Evaluates the expression, marking as dirty.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalDirty(Env env)
  {
    Value array = _expr.eval(env);
    Value index = _index.eval(env);

    return array.getDirty(index);
  }

  /**
   * Evaluates the expression, creating an object if the value is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalObject(Env env)
  {
    Value array = _expr.evalArray(env);
    Value index = _index.eval(env);

    return array.getObject(env, index);
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
    // php/0d2t
    // php/0d1c
    Value array = _expr.evalArg(env, false);
    Value index = _index.eval(env);

    Value result = array.getArg(index, isTop);

    return result;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Var evalVar(Env env)
  {
    Value array = _expr.evalArray(env);
    Value index = _index.eval(env);

    return array.getVar(index);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalAssignValue(Env env, Expr valueExpr)
  {
    // php/03mk, php/03mm, php/03mn, php/04b3
    // php/04ah
    Value array = _expr.evalArrayAssign(env, _index, valueExpr);

    return array;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalAssignRef(Env env, Expr valueExpr)
  {
    // php/03mk
    // php/04ai
    return _expr.evalArrayAssignRef(env, _index, valueExpr);
  }

  @Override
  public Value evalAssignRef(Env env, Value value)
  {
    return _expr.evalArrayAssignRef(env, _index, value);
  }

  /**
   * Evaluates the expression as an isset().
   */
  @Override
  public boolean evalIsset(Env env)
  {
    Value array = _expr.evalIssetValue(env);
    Value index = _index.evalIssetValue(env);

    return array.isset(index);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public void evalUnset(Env env)
  {
    _expr.evalUnsetArray(env, _index);
  }

  /**
   * Evaluates as an empty() expression.
   */
  @Override
  public boolean evalEmpty(Env env)
  {
    Value array = _expr.evalIssetValue(env);
    Value index = _index.evalIssetValue(env);

    return array.isEmpty(env, index);
  }

  @Override
  public String toString()
  {
    return _expr + "[" + _index + "]";
  }
}

