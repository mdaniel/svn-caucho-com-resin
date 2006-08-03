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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.query;

import java.util.ArrayList;

import com.caucho.util.CharBuffer;

/**
 * Function expression
 */
class FunExpr extends AbstractAmberExpr {
  private String _id;
  private ArrayList<AmberExpr> _args;

  /**
   * Creates a new cmp expression
   */
  protected FunExpr(String id, ArrayList<AmberExpr> args)
  {
    _id = id;
    _args = args;
  }

  static FunExpr create(String id, ArrayList<AmberExpr> args)
  {
    return new FunExpr(id, args);
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    for (int i = 0; i < _args.size(); i++) {
      AmberExpr arg = _args.get(i);

      arg = arg.bindSelect(parser);

      _args.set(i, arg);
    }

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    for (int i = 0; i < _args.size(); i++) {
      AmberExpr arg = _args.get(i);

      if (arg.usesFrom(from, type))
        return true;
    }

    return false;
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    cb.append(_id);
    cb.append('(');

    for (int i = 0; i < _args.size(); i++) {
      if (i != 0)
        cb.append(',');

      _args.get(i).generateWhere(cb);
    }

    cb.append(')');
  }

  /**
   * Generates the having expression.
   */
  public void generateHaving(CharBuffer cb)
  {
    generateWhere(cb);
  }

  public String toString()
  {
    String str = _id + "(";

    for (int i = 0; i < _args.size(); i++) {
      if (i != 0)
        str += ',';

      str += _args.get(i);
    }

    return str + ")";
  }
}
