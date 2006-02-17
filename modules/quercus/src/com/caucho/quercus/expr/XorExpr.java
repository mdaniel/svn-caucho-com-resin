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
import com.caucho.quercus.env.BooleanValue;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a logical xor expression.
 */
public class XorExpr extends BinaryExpr {
  public XorExpr(Expr left, Expr right)
  {
    super(left, right);
  }

  /**
   * Returns true for a boolean.
   */
  public boolean isBoolean()
  {
    return true;
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
    if (_left.evalBoolean(env) != _right.evalBoolean(env))
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
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
    return _left.evalBoolean(env) != _right.evalBoolean(env);
  }

  //
  // Java code generation
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("env.toValue(");
    generateBoolean(out);
    out.print(")");
  }

  /**
   * Generates code to evaluate the expression as a long.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    out.print("(");
    _left.generateBoolean(out);
    out.print(" != ");
    _right.generateBoolean(out);
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
    out.print("new com.caucho.quercus.expr.XorExpr(");
    _left.generateExpr(out);
    out.print(", ");
    _right.generateExpr(out);
    out.print(")");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out)
    throws IOException
  {
    out.print("if (");
    generateBoolean(out);
    out.println(") {}");
  }
  
  public String toString()
  {
    return "(" + _left + " xor " + _right + ")";
  }
}

