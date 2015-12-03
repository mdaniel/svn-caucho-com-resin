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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.sql;

import java.sql.SQLException;
import java.util.ArrayList;

import com.caucho.db.table.Table;
import com.caucho.db.table.TableIterator;
import com.caucho.db.table.Column.ColumnType;

class OidExpr extends Expr {
  private Table _table;
  private int _tableIndex;
  
  private FromItem _fromItem;

  OidExpr(FromItem fromItem, Table table, int tableIndex)
  {
    _fromItem = fromItem;
    _table = table;
    _tableIndex = tableIndex;
  }

  FromItem getFromItem()
  {
    return _fromItem;
  }
  
  @Override
  public Class<?> getType()
  {
    return long.class;
  }

  @Override
  public boolean isLong()
  {
    return true;
  }
  
  @Override
  public int getSQLType()
  {
    return ColumnType.IDENTITY.getSQLType();
  }

  /**
   * Returns any column name.
   */
  public String getName()
  {
    return "resin_oid";
  }

  /**
   * Returns the column's table.
   */
  public Table getTable()
  {
    return _table;
  }
  
  @Override
  public long indexSubCost(ArrayList<FromItem> costItems)
  {
    if (! costItems.contains(_fromItem))
      return INDEX_COST_INDEX;
    else
      return COST_INVALID - 1;
  }
  
  @Override
  public long subCost(ArrayList<FromItem> costItems)
  {
    if (costItems.contains(_fromItem))
      return 0;
    else
      return COST_INVALID;
  }
  
  @Override
  public Expr bind(Query query)
  {
    FromItem []fromItems = query.getFromItems();
    
    for (int i = 0; i < fromItems.length; i++) {
      if (fromItems[i] == _fromItem) {
        _tableIndex = i;
        return this;
      }
    }

    throw new IllegalStateException();
  }

  /**
   * Returns true if the expression is null.
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return false;
  }

  /**
   * Evaluates the expression as a string.
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return String.valueOf(row.getRowAddress());
  }

  public int evalInt(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return (int) row.getRowAddress();
  }

  @Override
  public long evalLong(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getRowAddress();
  }

  @Override
  public double evalDouble(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getRowAddress();
  }

  /**
   * Evaluates the expression, writing to the result stream.
   *
   * @param context the query context
   * @param result the output result
   */
  @Override
  public void evalToResult(QueryContext context, SelectResult result)
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    result.writeLong(row.getRowAddress());
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _fromItem + "," + _tableIndex + "]";
  }
}
