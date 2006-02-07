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

package com.caucho.quercus.expr;

import java.io.IOException;

import java.util.HashSet;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.Statement;
import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.program.ExprStatement;

/**
 * Represents a PHP variable expression.
 */
public class VarVarExpr extends AbstractVarExpr {
  private static final NullValue NULL = NullValue.create();
  
  private final Expr _var;

  public VarVarExpr(Expr var)
  {
    _var = var;
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
    String varName = _var.evalString(env);

    Value value = env.getValue(varName);

    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Evaluates the expression, returning a copy as necessary.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
    throws Throwable
  {
    String varName = _var.evalString(env);

    Value value = env.getValue(varName);

    if (value != null)
      return value.copy();
    else
      return NULL;
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
    String varName = _var.evalString(env);

    env.setVar(varName, value);
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
    String varName = _var.evalString(env);

    env.unsetVar(varName);
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
    String varName = _var.evalString(env);

    Value value = env.getVar(varName);

    if (value != null)
      return value;
    else
      return NULL;
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
    String varName = _var.evalString(env);

    Value value = env.getVar(varName);

    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Evaluates the expression, converting to an array if necessary.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
    throws Throwable
  {
    String varName = _var.evalString(env);

    Value value = env.getVar(varName);

    if (value != null)
      return value.getArray();
    else {
      ArrayValue array = new ArrayValueImpl();

      env.setVar(varName, array);
      
      return array;
    }
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    info.getFunction().setVariableVar(true);
    
    _var.analyze(info);
  }
  /**
   * Analyze the expression
   */
  public void analyzeAssign(AnalyzeInfo info)
  {
    info.getFunction().setVariableVar(true);
    
    _var.analyzeAssign(info);

    // php/3253
    info.clear();
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("env.getValue(");
    _var.generateString(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    out.print("env.getVar(");
    _var.generateString(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression, copying the result
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".copy()");  // php/3a5t
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    out.print("env.getVar(");
    _var.generateString(out);
    out.print(").set(");
    value.generateCopy(out); // php/3a84
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    out.print("env.setVar(");
    _var.generateString(out);
    out.print(", ");
    value.generateRef(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateUnset(PhpWriter out)
    throws IOException
  {
    out.print("env.unsetVar(");
    _var.generateString(out);
    out.print(")");
  }
  
  public String toString()
  {
    return "$" + _var;
  }
}

