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

package com.caucho.amber.expr.fun;

import com.caucho.amber.manager.AmberConnection;

import com.caucho.amber.expr.*;

import com.caucho.amber.query.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;


/**
 * Function expression
 */
public class FunExpr extends AbstractAmberExpr {
  private static final L10N L = new L10N(FunExpr.class);

  private String _id;
  private ArrayList<AmberExpr> _args;
  private boolean _distinct;

  /**
   * Creates a new cmp expression
   */
  protected FunExpr(String id,
                    ArrayList<AmberExpr> args,
                    boolean distinct)
  {
    _id = id;
    _args = args;
    _distinct = distinct;
  }

  public static FunExpr create(String id,
                               ArrayList<AmberExpr> args,
                               boolean distinct)
  {
    return new FunExpr(id, args, distinct);
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
   * Generates the having expression.
   */
  public void generateHaving(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Returns the args.
   */
  protected ArrayList<AmberExpr> getArgs()
  {
    return _args;
  }

  /**
   * Returns the object for the expr.
   */
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    // XXX: needs to be factored into a CountFunExpr
    if (_id.equalsIgnoreCase("count"))
      return rs.getLong(index);
    else
      return super.getObject(aConn, rs, index);
  }

  public String toString()
  {
    String str = _id + "(";

    if (_distinct)
      str += "distinct ";

    for (int i = 0; i < _args.size(); i++) {
      if (i != 0)
        str += ',';

      str += _args.get(i);
    }

    return str + ")";
  }

  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    cb.append(_id);
    cb.append('(');

    if (_distinct)
      cb.append("distinct ");

    for (int i = 0; i < _args.size(); i++) {
      if (i != 0)
        cb.append(',');

      if (select)
        _args.get(i).generateWhere(cb);
      else
        _args.get(i).generateUpdateWhere(cb);
    }

    cb.append(')');
  }
}
