/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.sql;

import com.caucho.db.index.BTree;
import com.caucho.db.index.IndexCache;
import com.caucho.db.table.Column;
import com.caucho.db.table.TableIterator;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.Hex;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

class IndexExpr extends RowIterateExpr {
  private IdExpr _columnExpr;
  private Column _column;
  private BTree _index;

  private Expr _expr;

  IndexExpr(IdExpr index, Expr expr)
  {
    _expr = expr;
    _columnExpr = index;

    if (expr == null || index == null)
      throw new NullPointerException();

    _column = index.getColumn();
    _index = _column.getIndex();

    if (_index == null)
      throw new IllegalArgumentException();
  }

  /**
   * Binds the expression.
   */
  public Expr bind(Query query)
    throws SQLException
  {
    _expr = _expr.bind(query);

    return this;
  }

  /**
   * Returns true if shifing the child rows will make a difference.
   */
  boolean allowChildRowShift(QueryContext context, TableIterator rowIter)
  {
    return false;
  }

  /**
   * Sets the initial row.
   */
  boolean init(QueryContext context, TableIterator rowIter)
    throws SQLException, IOException
  {
    rowIter.init(context);

    // the Query will call initRow immediately after, so the following
    // call isn't necessary
    //return initRow(context, rowIter);

    return true;
  }

  /**
   * Sets the initial row.
   */
  boolean initRow(QueryContext context, TableIterator tableIter)
    throws SQLException, IOException
  {
    long rowAddr = evalIndex(context);

    if (rowAddr == 0)
      return false;

    context.unlock();

    tableIter.setRow(rowAddr);

    context.lock();

    return true;
  }

  /**
   * Returns the next row.
   */
  boolean nextRow(QueryContext context, TableIterator table)
  {
    return false;
  }

  long evalIndex(QueryContext context)
    throws SQLException
  {
    byte []buffer = context.getBuffer();

    int length = _expr.evalToBuffer(context, buffer, 0, _column.getTypeCode());

    if (length <= 0) {
      //System.out.println("EVAL: " + length);
      return 0;
    }

    /*
    long index = _cache.lookup(_index, buffer, 0, length,
                               context.getTransaction());
    */

    long index;
    try {
      index = _index.lookup(buffer, 0, length);
    } catch (IOException e) {
      throw new SQLException(e);
    }
    // System.out.println("ROW: " + index + " " + _index + " " + Hex.toHex(buffer, 0, length));

    // System.out.println("LOOKUP: " + index + " " + _expr.evalString(context));
    return index;
  }

  /**
   * Returns the next row.
   */
  boolean nextBlock(QueryContext context, TableIterator rowIter)
    throws IOException, SQLException
  {
    context.unlock();

    return false;
  }

  public String toString()
  {
    return "(" + _columnExpr + " = " + _expr + ")";
  }
}
