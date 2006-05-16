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

package com.caucho.quercus.expr;

import java.io.IOException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Represents a PHP array reference expression.
 */
public class ArrayGetExpr extends AbstractVarExpr {
  private final Expr _expr;
  private final Expr _index;

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
  public Value evalCopy(Env env)
  {
    Value array = _expr.eval(env);

    Value index = _index.eval(env);

    return array.get(index).copy();
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
    Value array = _expr.evalArray(env);

    Value index = _index.eval(env);

    return array.getArray(index);
  }

  /**
   * Evaluates the expression, creating an object if the value is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
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
  public Value evalArg(Env env)
  {
    Value value = _expr.evalArg(env); // php/0d2t

    return value.getArg(_index.eval(env));
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
   * Analyze the statement
   */
  public void analyzeAssign(AnalyzeInfo info)
  {
    analyze(info);

    _expr.analyzeSetModified(info);
    // php/3a68
    _expr.analyzeSetReference(info);
  }

  /**
   * Analyze the statement as modified
   */
  public void analyzeSetModified(AnalyzeInfo info)
  {
    _expr.analyzeSetModified(info);
  }

  /**
   * Analyze the statement as a reference
   */
  public void analyzeSetReference(AnalyzeInfo info)
  {
    // php/3a69
    _expr.analyzeSetReference(info);
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
  public void generateRef(PhpWriter out)
    throws IOException
  {
    _expr.generateArray(out); // php/3d1c
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
    _expr.generateArg(out);
    out.print(".getArg(");
    _index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression with a copied result.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".copy()"); // php/3a5m
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
    value.generateCopy(out);  // php/3a5k
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

