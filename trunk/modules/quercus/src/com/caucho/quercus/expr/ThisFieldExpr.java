/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

import java.io.IOException;
import java.util.ArrayList;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.ClassField;

/**
 * Represents a PHP field reference.
 */
public class ThisFieldExpr extends AbstractVarExpr {
  protected final ThisExpr _qThis;

  protected StringValue _name;

  protected boolean _isInit;

  public ThisFieldExpr(Location location,
                       ThisExpr qThis,
                       StringValue name)
  {
    super(location);

    _qThis = qThis;
    _name = name;
  }

  //
  // function call creation
  //

  /**
   * Creates a function call expression
   */
  @Override
  public Expr createCall(QuercusParser parser,
                         Location location,
                         ArrayList<Expr> args)
    throws IOException
  {
    ExprFactory factory = parser.getExprFactory();

    return factory.createThisMethod(location,
                                    _qThis, _name, args);
  }

  public void init()
  {
    /// XXX: have this called by QuercusParser after class parsing

    if (! _isInit) {
      _isInit = true;

      ClassField entry = _qThis.getClassDef().getField(_name);

      if (entry != null) {
        _name = entry.getCanonicalName();
      }
    }
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
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    return obj.getThisField(env, _name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    return obj.getThisField(env, _name).copy();
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
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      env.thisError(getLocation());

      return new Var();
    }

    return obj.getThisFieldVar(env, _name);
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
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    return obj.getThisFieldArg(env, _name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalAssignValue(Env env, Value value)
  {
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    obj.putThisField(env, _name, value);

    return value;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalAssignRef(Env env, Value value)
  {
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    obj.putThisField(env, _name, value);

    return value;
  }

  /**
   * Evaluates as an array index assign ($a[index] = value).
   */
  @Override
  public Value evalArrayAssign(Env env, Expr indexExpr, Expr valueExpr)
  {
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    // php/044i
    Value fieldVar = obj.getThisFieldArray(env, _name);
    Value index = indexExpr.eval(env);

    Value value = valueExpr.evalCopy(env);

    return fieldVar.putThisFieldArray(env, obj, _name, index, value);
  }

  /**
   * Evaluates as an array index assign ($a[index] = value).
   */
  @Override
  public Value evalArrayAssignRef(Env env, Expr indexExpr, Expr valueExpr)
  {
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    // php/044i
    Value fieldVar = obj.getThisFieldArray(env, _name);
    Value index = indexExpr.eval(env);

    Value value = valueExpr.evalRef(env);

    return fieldVar.putThisFieldArray(env, obj, _name, index, value);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    return obj.getThisFieldArray(env, _name);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      return env.thisError(getLocation());
    }

    return obj.getThisFieldObject(env, _name);
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
    init();

    Value obj = env.getThis();

    if (obj.isNull()) {
      env.thisError(getLocation());
    }

    obj.unsetThisField(_name);
  }

  public String toString()
  {
    return "$this->" + _name;
  }
}

