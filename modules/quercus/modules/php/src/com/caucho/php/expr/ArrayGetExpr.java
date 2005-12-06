/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.php.expr;

import java.io.IOException;

import com.caucho.php.env.Env;
import com.caucho.php.env.Value;
import com.caucho.php.env.ArrayValue;
import com.caucho.php.env.NullValue;

import com.caucho.php.program.AnalyzeInfo;
import com.caucho.php.program.Statement;
import com.caucho.php.program.ExprStatement;

import com.caucho.php.gen.PhpWriter;

/**
 * Represents a PHP array reference expression.
 */
public class ArrayGetExpr extends AbstractVarExpr {
  private final Expr _expr;
  private final Expr _index;

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
  public Value eval(Env env)
    throws Throwable
  {
    Value array = _expr.eval(env);

    Value index = _index.eval(env);

    return array.get(index);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
    throws Throwable
  {
    Value array = _expr.evalArray(env);

    Value index = _index.eval(env);

    return array.getArray(index);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArgArray(Env env)
    throws Throwable
  {
    Value array = _expr.evalArray(env);

    Value index = _index.eval(env);

    return array.getArgArray(index);
  }

  /**
   * Evaluates the expression, creating an object if the value is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
    throws Throwable
  {
    Value array = _expr.evalArray(env);

    Value index = _index.eval(env);

    return array.getObject(env, index);
  }

  /**
   * Evaluates the expression, creating an object if the value is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArgObject(Env env)
    throws Throwable
  {
    Value array = _expr.evalArray(env);

    Value index = _index.eval(env);

    return array.getArgObject(env, index);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArg(Env env)
    throws Throwable
  {
    Value value = _expr.evalArray(env);

    return value.getRef(_index.eval(env));
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
    throws Throwable
  {
    Value value = _expr.evalArray(env);
    
    return value.getRef(_index.eval(env));
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalAssign(Env env, Value value)
    throws Throwable
  {
    Value array = _expr.evalArray(env);

    Value index = _index.eval(env);

    array.put(index, value);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalUnset(Env env)
    throws Throwable
  {
    Value array = _expr.eval(env);

    Value index = _index.eval(env);

    array.remove(index);
  }

  /**
   * Analyze the statement
   */
  public void analyze(AnalyzeInfo info)
  {
    _expr.analyze(info);
    _index.analyze(info);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _expr.generate(out);
    out.print(".get(");
    _index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArray(PhpWriter out)
    throws IOException
  {
    _expr.generateArray(out);
    out.print(".getArray(");
    _index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArgArray(PhpWriter out)
    throws IOException
  {
    _expr.generateArgArray(out);
    out.print(".getArgArray(");
    _index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateObject(PhpWriter out)
    throws IOException
  {
    _expr.generateArray(out);
    out.print(".getObject(env, ");
    _index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArgObject(PhpWriter out)
    throws IOException
  {
    _expr.generateArgArray(out);
    out.print(".getArgObject(env, ");
    _index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    _expr.generateArray(out);
    out.print(".getRef(");
    _index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArg(PhpWriter out)
    throws IOException
  {
    _expr.generateArgArray(out);
    out.print(".getArg(");
    _index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    _expr.generateArray(out);
    out.print(".put(");
    _index.generate(out);
    out.print(", ");
    value.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    _expr.generateArray(out);
    out.print(".put(");
    _index.generate(out);
    out.print(", ");
    value.generateRef(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateUnset(PhpWriter out)
    throws IOException
  {
    _expr.generate(out);
    out.print(".remove(");
    _index.generate(out);
    out.print(")");
  }
  
  public String toString()
  {
    return _expr + "[" + _index + "]";
  }
}

