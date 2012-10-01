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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import com.caucho.db.blob.BlobInputStream;
import com.caucho.db.block.BlockStore;
import com.caucho.db.table.TableIterator;
import com.caucho.db.table.Column.ColumnType;
import com.caucho.util.CharBuffer;
import com.caucho.util.FreeList;
import com.caucho.util.IntArray;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.util.SQLExceptionWrapper;
import com.caucho.vfs.TempBuffer;

public class SelectCursor {
  private static final L10N L = new L10N(SelectCursor.class);

  private static final FreeList<SelectCursor> _freeList
    = new FreeList<SelectCursor>(32);

  private static final int SIZE = TempBuffer.SIZE;

  private CharBuffer _cb = new CharBuffer();
  private byte []_blob = new byte[128];

  private Expr []_exprs;
  private BlockStore []_stores = new BlockStore[32];

  private TableIterator []_rows = new TableIterator[16];

  private int _row;

  private int _offset;
  private int _rowOffset;
  private int _columnOffset;
  private int _column;

  private boolean _wasNull;
  private boolean _isStart;
  private SelectQuery _query;
  private QueryContext _context;

  public SelectCursor(Expr []exprs,
                      SelectQuery query,
                      QueryContext context)
  {
    init(exprs);
    
    _query = query;
    _context = context;
  }

  /**
   * Initialize the iterator.
   */
  TableIterator []initRows(FromItem []fromItems)
  {
    if (_rows.length < fromItems.length)
      _rows = new TableIterator[fromItems.length];

    for (int i = 0; i < fromItems.length; i++) {
      if (_rows[i] == null)
        _rows[i] = new TableIterator();
      _rows[i].init(fromItems[i].getTable());
    }

    return _rows;
  }
  
  TableIterator []getRows()
  {
    return _rows;
  }

  /**
   * Initialize based on the exprs.
   */
  private void init(Expr []exprs)
  {
    _exprs = exprs;

    if (_stores.length < _exprs.length) {
      _stores = new BlockStore[exprs.length];
    }

    for (int i = 0; i < exprs.length; i++)
      _stores[i] = exprs[i].getTable();
  }

  void initRead()
    throws SQLException
  {
    _row = -1;
    _offset = 0;
    _column = 0;
    _rowOffset = 0;
    _columnOffset = 0;
  }

  /**
   * Returns the expressions.
   */
  public Expr []getExprs()
  {
    return _exprs;
  }

  /**
   * Returns the column index with the given name.
   */
  public int findColumnIndex(String name)
    throws SQLException
  {
    for (int i = 0; i < _exprs.length; i++) {
      if (_exprs[i].getName().equals(name))
        return i + 1;
    }

    throw new SQLException(L.l("column `{0}' does not exist.", name));
  }

  public boolean next()
    throws SQLException
  {
    if (! _isStart) {
      _isStart = true;
      
      return true;
    }
    
    return _query.nextCursor(_rows, _context, _context.getTransaction());
  }

  public String getString(int i)
    throws SQLException
  {
    return _exprs[i].evalString(_context);
  }

  public void updateString(int i, String value)
    throws SQLException
  {
    _exprs[i].updateString(_context, value);
  }

  public int getInt(int i)
  throws SQLException
  {
    return (int) _exprs[i].evalLong(_context);
  }
  
  public void updateInt(int i, int value)
    throws SQLException
  {
    _exprs[i].updateLong(_context, value);
  }

  public long getLong(int i)
    throws SQLException
  {
    return _exprs[i].evalLong(_context);
  }
  
  public void updateLong(int i, long value)
    throws SQLException
  {
    _exprs[i].updateLong(_context, value);
  }

  public double getDouble(int i)
    throws SQLException
  {
    return _exprs[i].evalDouble(_context);
  }
  
  public void updateDouble(int i, double value)
    throws SQLException
  {
    _exprs[i].updateDouble(_context, value);
  }

  public byte []getBytes(int i)
    throws SQLException
  {
    return _exprs[i].evalBytes(_context);
  }

  public void updateRow()
  {
    
  }

  public void close()
  {
  }
}
