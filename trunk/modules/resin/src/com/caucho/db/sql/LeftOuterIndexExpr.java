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

import java.io.IOException;
import java.sql.SQLException;

import com.caucho.db.table.TableIterator;

/**
 * resin_oid index.
 *
 */
class LeftOuterIndexExpr extends RowIterateExpr {
  private RowIterateExpr _indexExpr;

  LeftOuterIndexExpr(RowIterateExpr indexExpr)
  {
    _indexExpr = indexExpr;

    if (indexExpr == null)
      throw new NullPointerException();

    // index.getColumn();
  }

  /**
   * Binds the expression.
   */
  @Override
  public Expr bind(Query query)
    throws SQLException
  {
    _indexExpr.bind(query);

    return this;
  }

  /**
   * Returns true if shifting the child rows will make a difference.
   */
  @Override
  boolean allowChildRowShift(QueryContext context, TableIterator rowIter)
  {
    return false;
  }

  /**
   * Sets the initial row.
   */
  @Override
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
  @Override
  boolean initRow(QueryContext context, TableIterator tableIter)
    throws SQLException, IOException
  {
    boolean isInitRow = _indexExpr.initRow(context, tableIter);
    
    if (isInitRow)
      return true;
    
    tableIter.setBlockId(0);
    tableIter.setRowOffset(0);
    
    return true;
  }

  /**
   * Returns the next row.
   */
  @Override
  boolean nextRow(QueryContext context, TableIterator table)
  {
    return false;
  }

  /**
   * Returns the next row.
   */
  @Override
  boolean nextBlock(QueryContext context, TableIterator rowIter)
    throws IOException, SQLException
  {
    context.unlock();

    return false;
  }

  @Override
  public String toString()
  {
    return "LeftOuter(" + _indexExpr + ")";
  }
}
