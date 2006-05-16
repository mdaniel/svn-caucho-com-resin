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
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Converts to a boolean
 */
public class ToBooleanExpr extends UnaryExpr {
  public ToBooleanExpr(Location location, Expr expr)
  {
    super(location, expr);
  }

  public ToBooleanExpr(Expr expr)
  {
    super(expr);
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
    return _expr.evalBoolean(env) ? BooleanValue.TRUE : BooleanValue.FALSE;
  }

  //
  // Java code generation
  //

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print('(');

    _expr.generateBoolean(out);

    out.print(" ? com.caucho.quercus.env.BooleanValue.TRUE : com.caucho.quercus.env.BooleanValue.FALSE)");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    _expr.generateBoolean(out);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    out.print("new com.caucho.quercus.expr.ToBooleanExpr(");
    _expr.generateExpr(out);
    out.print(")");
  }

  public String toString()
  {
    return "((boolean) " + _expr + ")";
  }
}

