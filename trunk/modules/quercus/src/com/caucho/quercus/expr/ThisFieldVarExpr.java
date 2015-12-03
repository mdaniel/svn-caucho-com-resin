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

import java.io.IOException;
import java.util.ArrayList;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.ClassField;

/**
 * Represents a PHP field reference.
 */
public class ThisFieldVarExpr extends AbstractVarExpr {
  protected final ThisExpr _qThis;
  protected final Expr _nameExpr;

  public ThisFieldVarExpr(Location location, ThisExpr qThis, Expr nameExpr)
  {
    super(location);

    _qThis = qThis;
    _nameExpr = nameExpr;
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

    return factory.createThisMethod(location, _qThis, _nameExpr, args);
  }

  private StringValue evalName(Env env)
  {
    StringValue name = _nameExpr.evalStringValue(env);

    ClassField entry = _qThis.getClassDef().getField(name);

    if (entry != null) {
      name = entry.getCanonicalName();
    }

    return name;
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
    Value value = env.getThis();

    return value.getThisFieldArg(env, evalName(env));
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
    // quercus/0d1k
    Value value = env.getThis();

    return value.getThisFieldVar(env, evalName(env));
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
    Value obj = env.getThis();

    return obj.getThisField(env, evalName(env));
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
    Value obj = env.getThis();

    obj.putThisField(env, evalName(env), value);

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
    Value obj = env.getThis();

    obj.putThisField(env, evalName(env), value);

    return value;
  }

  /**
   * Evaluates as an array index assign ($a[index] = value).
   */
  @Override
  public Value evalArrayAssign(Env env, Expr indexExpr, Expr valueExpr)
  {
    Value obj = env.getThis();

    StringValue name = evalName(env);

    Value fieldVar = obj.getThisFieldArray(env, name);
    Value index = indexExpr.eval(env);

    Value value = valueExpr.evalCopy(env);

    // php/03mn
    return fieldVar.putThisFieldArray(env, obj, name, index, value);
  }

  /**
   * Evaluates as an array index assign ($a[index] = &value).
   */
  @Override
  public Value evalArrayAssignRef(Env env, Expr indexExpr, Expr valueExpr)
  {
    Value obj = env.getThis();

    StringValue name = evalName(env);

    Value fieldVar = obj.getThisFieldArray(env, name);
    Value index = indexExpr.eval(env);

    Value value = valueExpr.evalRef(env);

    // php/03mn
    return fieldVar.putThisFieldArray(env, obj, name, index, value);
  }

  /**
   * Evaluates the expression, creating an array if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    Value obj = env.getThis();

    return obj.getThisFieldArray(env, evalName(env));
  }

  /**
   * Evaluates the expression, creating an object if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    Value obj = env.getThis();

    return obj.getThisFieldObject(env, evalName(env));
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
    Value obj = env.getThis();

    obj.unsetThisField(evalName(env));
  }

  public String toString()
  {
    return "$this->{" + _nameExpr + "}";
  }
}

