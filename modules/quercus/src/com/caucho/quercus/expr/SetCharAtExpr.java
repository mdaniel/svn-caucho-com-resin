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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.util.L10N;

/**
 * Dummy for code generation
 */
public class SetCharAtExpr extends Expr {
  private static final L10N L = new L10N(SetCharAtExpr.class);

  private final Expr _objExpr;
  private final Expr _indexExpr;
  private final Expr _valueExpr;

  public SetCharAtExpr(Expr objExpr, Expr indexExpr, Expr valueExpr)
  {
    _objExpr = objExpr;
    _indexExpr = indexExpr;
    _valueExpr = valueExpr;
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
    throw new UnsupportedOperationException();
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _objExpr.generate(out);
    out.print(".setCharAt(");
    _indexExpr.generateLong(out);
    out.print(",");
    _valueExpr.generateString(out);
    out.print(")");
  }
  
  public String toString()
  {
    return _objExpr + "{" + _indexExpr + "} = " + _valueExpr;
  }
}

