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

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;

import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a conditional expression.
 */
public class ConditionalExpr extends Expr {
  private final Expr _test;
  private final Expr _trueExpr;
  private final Expr _falseExpr;

  public ConditionalExpr(Expr test, Expr trueExpr, Expr falseExpr)
  {
    _test = test;
    
    _trueExpr = trueExpr;
    _falseExpr = falseExpr;
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
    if (_test.evalBoolean(env))
      return _trueExpr.eval(env);
    else
      return _falseExpr.eval(env);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
    throws Throwable
  {
    if (_test.evalBoolean(env))
      return _trueExpr.evalBoolean(env);
    else
      return _falseExpr.evalBoolean(env);
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    _test.analyze(info);

    AnalyzeInfo falseExprInfo = info.copy();
    
    _trueExpr.analyze(info);
    
    _falseExpr.analyze(falseExprInfo);

    info.merge(falseExprInfo);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("(");
    _test.generateBoolean(out);
    out.print(" ? ");
    _trueExpr.generate(out);
    out.print(" : ");
    _falseExpr.generate(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression as a boolean.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    out.print("(");
    _test.generateBoolean(out);
    out.print(" ? ");
    _trueExpr.generateBoolean(out);
    out.print(" : ");
    _falseExpr.generateBoolean(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression as a long.
   *
   * @param out the writer to the Java source code.
   */
  public void generateLong(PhpWriter out)
    throws IOException
  {
    out.print("(");
    _test.generateBoolean(out);
    out.print(" ? ");
    _trueExpr.generateLong(out);
    out.print(" : ");
    _falseExpr.generateLong(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression as a double.
   *
   * @param out the writer to the Java source code.
   */
  public void generateDouble(PhpWriter out)
    throws IOException
  {
    out.print("(");
    _test.generateBoolean(out);
    out.print(" ? ");
    _trueExpr.generateDouble(out);
    out.print(" : ");
    _falseExpr.generateDouble(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression as a string.
   *
   * @param out the writer to the Java source code.
   */
  public void generateString(PhpWriter out)
    throws IOException
  {
    out.print("(");
    _test.generateBoolean(out);
    out.print(" ? ");
    _trueExpr.generateString(out);
    out.print(" : ");
    _falseExpr.generateString(out);
    out.print(")");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateStatement(PhpWriter out)
    throws IOException
  {
    out.print("if (");
    _test.generateBoolean(out);
    out.println(") {");
    out.pushDepth();
    _trueExpr.generateStatement(out);
    out.popDepth();
    out.println("} else {");
    out.pushDepth();
    _falseExpr.generateStatement(out);
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    out.print("new com.caucho.quercus.expr.ConditionalExpr(");
    _test.generateExpr(out);
    out.print(", ");
    _trueExpr.generateExpr(out);
    out.print(", ");
    _falseExpr.generateExpr(out);
    out.print(")");
  }
  
  public String toString()
  {
    return "(" + _test + " ? " + _trueExpr + " : " + _falseExpr + ")";
  }
}

