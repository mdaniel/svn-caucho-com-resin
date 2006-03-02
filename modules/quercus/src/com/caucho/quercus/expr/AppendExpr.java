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
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;

import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP append ('.') expression.
 */
public class AppendExpr extends Expr {
  private final Expr _value;
  private AppendExpr _next;
  
  private AppendExpr(Expr value, AppendExpr next)
  {
    _value = value;
    _next = next;
  }

  /**
   * Returns the value expression.
   */
  public Expr getValue()
  {
    return _value;
  }

  /**
   * Returns the next value in the append chain.
   */
  public AppendExpr getNext()
  {
    return _next;
  }

  public static Expr create(Expr left, Expr right)
  {
    AppendExpr next;

    if (right instanceof AppendExpr)
      next = (AppendExpr) right;
    else
      next = new AppendExpr(right, null);

    AppendExpr leftAppend;
    
    if (left instanceof AppendExpr)
      leftAppend = (AppendExpr) left;
    else
      leftAppend = new AppendExpr(left, null);

    AppendExpr result = append(leftAppend, next);

    if (result.getNext() != null)
      return result;
    else
      return result.getValue();
  }

  /**
   * Appends the tail to the current expression, combining
   * constant literals.
   */
  private static AppendExpr append(AppendExpr left, AppendExpr tail)
  {
    if (left == null)
      return tail;

    tail = append(left._next, tail);

    if (left._value instanceof StringLiteralExpr &&
	tail._value instanceof StringLiteralExpr) {
      StringLiteralExpr leftString = (StringLiteralExpr) left._value;
      StringLiteralExpr rightString = (StringLiteralExpr) tail._value;

      Expr value = new StringLiteralExpr(leftString.evalConstant().toString() +
					 rightString.evalConstant().toString());

      return new AppendExpr(value, tail._next);
    }
    else {
      left._next = tail;

      return left;
    }
  }

  /**
   * Returns true for a string.
   */
  public boolean isString()
  {
    return true;
  }
  
  public Value eval(Env env)
    throws Throwable
  {
    return new StringValueImpl(evalString(env));
  }
  
  public String evalString(Env env)
    throws Throwable
  {
    StringBuilder sb = new StringBuilder();

    for (AppendExpr ptr = this; ptr != null; ptr = ptr._next) {
      sb.append(ptr._value.evalString(env));
    }

    return sb.toString();
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    _value.analyze(info);

    if (_next != null)
      _next.analyze(info);
  }

  /**
   * Generates code to evaluate the expression as a string.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("new StringBuilderValue()");
    
    for (AppendExpr ptr = this; ptr != null; ptr = ptr._next) {
      out.print(".append(");
      ptr._value.generateAppend(out);
      out.print(")");
    }
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

    _value.generateString(out);
    
    for (AppendExpr ptr = _next; ptr != null; ptr = ptr._next) {
      out.print(" + ");
      ptr._value.generateString(out);
    }
    
    out.print(")");
  }

  /**
   * Generates code to print the expression to the output
   *
   * @param out the writer to the Java source code.
   */
  public void generatePrint(PhpWriter out)
    throws IOException
  {
    for (AppendExpr ptr = this; ptr != null; ptr = ptr._next) {
      ptr._value.generatePrint(out);
    }
  }
  
  public String toString()
  {
    if (_next != null)
      return "(" + _value + " . " + _next + ")";
    else
      return String.valueOf(_value);
  }
}

