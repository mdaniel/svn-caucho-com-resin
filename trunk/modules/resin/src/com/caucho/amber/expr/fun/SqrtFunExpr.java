/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.expr.fun;

import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.type.DoubleType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.ArrayList;


/**
 * SQRT function expression
 */
public class SqrtFunExpr extends FunExpr {
  private static final L10N L = new L10N(SqrtFunExpr.class);

  /**
   * Creates a new expression
   */
  protected SqrtFunExpr(QueryParser parser,
                        ArrayList<AmberExpr> args)
  {
    super(parser, "sqrt", args, false);
  }

  public static FunExpr create(QueryParser parser,
                               ArrayList<AmberExpr> args)
  {
    return new SqrtFunExpr(parser, args);
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    generateInternalWhere(cb, true);
  }

  /**
   * Generates the (update) where expression.
   */
  public void generateUpdateWhere(CharBuffer cb)
  {
    generateInternalWhere(cb, false);
  }

  /**
   * Returns the expr type.
   */
  public AmberType getType()
  {
    // jpa/1197
    return DoubleType.create();
  }

  //
  // protected

  void generateInternalWhere(CharBuffer cb,
                             boolean select)
  {
    ArrayList<AmberExpr> args = getArgs();

    int n = args.size();

    // XXX: this validation should be moved to QueryParser
    // if (n != 1)
    //   throw new QueryParseException(L.l("expected 1 argument for SQRT"));

    cb.append("sqrt(");

    if (select)
      args.get(0).generateWhere(cb);
    else
      args.get(0).generateUpdateWhere(cb);

    cb.append(")");
  }
}
