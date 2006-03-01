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

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.LongValue;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.AnalyzeInfo;

/**
 * Represents a PHP post increment expression.
 */
public class PostIncrementExpr extends UnaryExpr {
  private final int _incr;

  public PostIncrementExpr(Expr expr, int incr)
    throws IOException
  {
    // super(expr.createRef());
    super(expr);

    _incr = incr;
  }
  
  public Value eval(Env env)
    throws Throwable
  {
    Value var = _expr.evalRef(env);

    return var.postincr(_incr);
  }

  /**
   * Return true for a double value
   */
  public boolean isDouble()
  {
    return _expr.isDouble();
  }

  /**
   * Return true for a long value
   */
  public boolean isLong()
  {
    return _expr.isLong();
  }

  /**
   * Return true for a number
   */
  public boolean isNumber()
  {
    return true;
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    _expr.analyze(info);
    _expr.analyzeAssign(info);

    _expr.analyzeSetReference(info);
    _expr.analyzeSetModified(info);
  }

  /**
   * Generates code to evaluate the expression as a long.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _expr.generateRef(out);
    out.print(".postincr(");
    out.print(_incr);
    out.print(")");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    out.print("new com.caucho.quercus.expr.PostIncrementExpr(");
    _expr.generateExpr(out);
    out.print(", ");
    out.print(_incr);
    out.print(")");
  }
  
  public String toString()
  {
    if (_incr > 0)
      return _expr + "++";
    else
      return _expr + "--";
  }
}

