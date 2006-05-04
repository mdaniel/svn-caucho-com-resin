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
import com.caucho.quercus.env.Var;
import com.caucho.quercus.env.RefVar;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.Location;

/**
 * Represents a PHP reference argument.
 */
public class RefExpr extends UnaryExpr {
  public RefExpr(Location location, Expr expr)
  {
    super(location, expr);
  }

  /**
   * Returns true for a reference.
   */
  public boolean isRef()
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
    // quercus/0d28
    Value value = getExpr().evalRef(env);

    if (value instanceof Var)
      return new RefVar((Var) value);
    else
      return value;
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
    Value value = getExpr().evalRef(env);

    // php/112d
    return value;
    /*
    if (value instanceof Var)
      return new RefVar((Var) value);
    else
      return value;
    */
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyzeAssign(AnalyzeInfo info)
  {
    _expr.analyzeAssign(info);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _expr.generateRef(out);
    out.print(".toRef()");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    // php/3c1q
    _expr.generateRef(out);
  }

  public String toString()
  {
    return _expr.toString();
  }
}

