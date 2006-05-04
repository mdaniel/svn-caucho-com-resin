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
import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.parser.QuercusParser;

import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.Location;

/**
 * Represents a PHP list assignment expression.
 */
public class ListExpr extends Expr {
  private final ListHeadExpr _listHead;
  private final Expr _value;

  private ListExpr(Location location, ListHeadExpr head, Expr value)
    throws IOException
  {
    super(location);
    _listHead = head;

    _value = value;
  }

  public static Expr create(QuercusParser parser,
                            ListHeadExpr head, Expr value)
    throws IOException
  {
    boolean isSuppress = value instanceof SuppressErrorExpr;

    if (isSuppress) {
      SuppressErrorExpr suppressExpr = (SuppressErrorExpr) value;

      value = suppressExpr.getExpr();
    }

    Expr expr;

    if (value instanceof EachExpr) {
      expr = new ListEachExpr(parser.getLocation(), head.getVarList(), (EachExpr) value);
    }
    else
      expr = new ListExpr(parser.getLocation(), head, value);

    if (isSuppress)
      return new SuppressErrorExpr(expr.getLocation(), expr);
    else
      return expr;
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
    Value value = _value.eval(env);

    _listHead.evalAssign(env, value);

    return value;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
    throws Throwable
  {
    return eval(env).copy();
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    // XXX: should be unique (?)
    info.getFunction().addTempVar("_quercus_list");

    _value.analyze(info);

    _listHead.analyze(info);
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _listHead.generateAssign(out, _value);
  }
}

