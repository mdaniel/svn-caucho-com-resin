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
import com.caucho.php.env.StringValue;

import com.caucho.php.program.AnalyzeInfo;

import com.caucho.php.gen.PhpWriter;

/**
 * Represents a PHP array assignment expression.
 */
public class FieldSetExpr extends Expr {
  private final Expr _expr;
  private final Value _index;
  private final Expr _value;

  public FieldSetExpr(Expr expr, String index, Expr value)
  {
    _expr = expr;
    _index = new StringValue(index);
    _value = value;
  }

  public FieldSetExpr(Expr expr, Value index, Expr value)
  {
    _expr = expr;
    _index = index;
    _value = value;
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
    Value expr = _expr.eval(env);
    Value value = _value.eval(env);

    Value exprValue = expr.toValue();

    exprValue.put(_index, value);

    return value;
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression;
   */
  public void analyze(AnalyzeInfo info)
  {
    _expr.analyze(info);
    _value.analyze(info);
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
    out.print(".put(");
    out.print(out.addValue(_index));
    out.print(", ");
    if (_value.isRef())
      _value.generate(out);
    else
      _value.generateValue(out);
    out.print(")");
  }
  
  public String toString()
  {
    return _expr + "[" + _index + "] = " + _value;
  }
}

