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
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.NullValue;

import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.program.Statement;
import com.caucho.quercus.program.ExprStatement;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP array[] reference expression.
 */
public class ArrayTailExpr extends AbstractVarExpr {
  private final Expr _expr;

  public ArrayTailExpr(Expr expr)
  {
    _expr = expr;
  }

  /**
   * Returns true for an expression that can be read (only $a[] uses this)
   */
  public boolean canRead()
  {
    return false;
  }

  /**
   * Returns the expr.
   */
  public Expr getExpr()
  {
    return _expr;
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
    return env.error("Cannot use [] as a read-value.");
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
    return evalRef(env);
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
    Value obj = _expr.evalArray(env);

    if (obj instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) obj;
      Value key = array.createTailKey();
      
      return array.getRef(key);
    }
    else
      return NullValue.NULL;
  }

  /**
   * Evaluates the expression, setting an array if unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
    throws Throwable
  {
    Value obj = _expr.evalArray(env);

    ArrayValue array = new ArrayValueImpl();

    obj.put(array);

    return array;
  }

  /**
   * Evaluates the expression, assigning an object if unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
    throws Throwable
  {
    Value array = _expr.evalArray(env);

    Value value = env.createObject();

    array.put(value);

    return value;
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

    array.put(value);
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
    throw new UnsupportedOperationException();
  }

  /**
   * Analyze the statement
   */
  public void analyze(AnalyzeInfo info)
  {
    _expr.analyze(info);
    _expr.analyzeSetReference(info);
    _expr.analyzeSetModified(info);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(toString() + " cannot be read");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArg(PhpWriter out)
    throws IOException
  {
    _expr.generateArray(out);
    out.print(".putRef()");
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
    out.print(".putRef()");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArray(PhpWriter out)
    throws IOException
  {
    // quercus/3d1i
    _expr.generateArray(out);
    out.print(".putArray()");
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
    out.print(".putObject(env)");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    // php/3a55
    _expr.generateArray(out);
    out.print(".put(");
    value.generateCopy(out); // php/3a80
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
    // php/3a56
    _expr.generateArray(out);
    out.print(".put(");
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
    throw new UnsupportedOperationException();
  }
  
  public String toString()
  {
    return _expr + "[]";
  }
}

