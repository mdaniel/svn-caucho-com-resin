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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.sql;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import com.caucho.vfs.WriteStream;

import com.caucho.log.Log;

import com.caucho.db.table.Table;
import com.caucho.db.table.Column;
import com.caucho.db.table.TableIterator;

class ColumnExpr extends Expr {
  private static final Logger log = Log.open(ColumnExpr.class);

  private Table _table;
  
  private int _tableIndex;
  private Column _column;
  private int _columnIndex;

  private String _name;
  private Class _type;

  ColumnExpr(String name, Table table, int tableIndex,
	     int columnIndex, Class type)
  {
    _name = name;
    _table = table;
    _tableIndex = tableIndex;
    _columnIndex = columnIndex;
    _column = table.getColumns()[_columnIndex];
    _type = type;
  }

  public Class getType()
  {
    return _type;
  }

  /**
   * Returns any column name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the column.
   */
  public Column getColumn()
  {
    return _column;
  }

  /**
   * Returns the column's table.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Returns true if the expression is null.
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.isNull(_column);
  }

  /**
   * Evaluates the expression as a string.
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getString(_column);
  }

  public int evalInt(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getInteger(_column);
  }

  public long evalLong(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getLong(_column);
  }

  public double evalDouble(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getDouble(_column);
  }

  /**
   * Evaluates the expression, writing to the result stream.
   *
   * @param context the query context
   * @param result the output result
   */
  public void evalToResult(QueryContext context, SelectResult result)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    row.evalToResult(_column, result);
  }

  public boolean evalEqual(QueryContext context, byte []matchBuffer)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.isEqual(_column, matchBuffer);
  }

  public boolean evalEqual(QueryContext context, String string)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.isEqual(_column, string);
  }

  public boolean equals(Object o)
  {
    if (o == null || ! ColumnExpr.class.equals(o.getClass()))
      return false;

    ColumnExpr expr = (ColumnExpr) o;

    return (_tableIndex == expr._tableIndex &&
	    _column == expr._column);
  }

  public String toString()
  {
    return "ColumnExpr[" + _tableIndex + "," + _columnIndex + "]";
  }
}
