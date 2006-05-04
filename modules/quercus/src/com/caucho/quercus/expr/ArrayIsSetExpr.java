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
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.UnsetValue;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Represents a PHP array is set expression.
 */
public class ArrayIsSetExpr extends Expr {
  private final Expr _expr;
  private final Expr _index;

  public ArrayIsSetExpr(Location location, Expr expr, Expr index)
  {
    super(location);
    _expr = expr;
    _index = index;
  }

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
    return evalBoolean(env) ? BooleanValue.TRUE : BooleanValue.FALSE;
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
    Value array = _expr.evalArg(env);
    Value index = _index.eval(env);

    return array.get(index) != UnsetValue.UNSET;
  }

  //
  // java code generation
  //

  /**
   * Analyze the expression
   */
  /*
  public void analyze(AnalyzeInfo info)
  {
    _expr.analyze(info);
    _index.analyze(info);
  }
  */

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    out.print("(");
    _expr.generate(out);
    out.print(".get(");
    _index.generate(out);
    out.print(") != UnsetValue.UNSET)");
  }

  public String toString()
  {
    return "isset(" + _expr + "[" + _index + "])";
  }
}

