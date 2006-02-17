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
package com.caucho.db.jdbc;

import java.io.*;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.math.BigDecimal;

import java.sql.*;
import javax.sql.*;

import com.caucho.util.L10N;

import com.caucho.vfs.Path;

import com.caucho.log.Log;

import com.caucho.db.Database;
import com.caucho.db.table.Column;

import com.caucho.db.sql.Data;

/**
 * The JDBC statement implementation.
 */
public class GeneratedKeysResultSet extends AbstractResultSet {
  private ArrayList<Data> _keys = new ArrayList<Data>();

  private Statement _stmt;
  private int _row;

  /**
   * Initialize the keys result set at the beginning of the query.
   */
  public void init(Statement stmt)
  {
    _stmt = stmt;
    _row = 0;
  }

  /**
   * Initialize the keys result set at the beginning of the query.
   */
  public void init()
  {
    _row = 0;
  }

  /**
   * Returns the statement associated with the keys.
   */
  public java.sql.Statement getStatement()
    throws SQLException
  {
    return _stmt;
  }

  public java.sql.ResultSetMetaData getMetaData()
    throws SQLException
  {
    return null;
  }

  public boolean next()
    throws SQLException
  {
    return _row++ == 0;
  }

  public boolean wasNull()
    throws SQLException
  {
    return false;
  }

  /**
   * Returns the index for the given column name.
   */
  public int findColumn(String columnName)
    throws SQLException
  {
    for (int i = 0; i < _keys.size(); i++) {
      Column column = _keys.get(i).getColumn();

      if (column.getName().equals(columnName))
	return i + 1;
    }

    throw new SQLException(L.l("`{0}' is an unknown column.", columnName));
  }

  /**
   * Sets the specified column.
   */
  public void setColumn(int index, Column column)
  {
    Data data = addData(index);

    data.setColumn(column);
  }

  /**
   * Returns the generated string key.
   */
  public String getString(int columnIndex)
    throws SQLException
  {
    Data data = _keys.get(columnIndex - 1);

    return data.getString();
  }

  /**
   * Sets the generated string key.
   */
  public void setString(int columnIndex, String value)
    throws SQLException
  {
    Data data = addData(columnIndex);

    data.setString(value);
  }

  /**
   * Returns the generated integer key.
   */
  public int getInt(int columnIndex)
    throws SQLException
  {
    Data data = _keys.get(columnIndex - 1);

    return data.getInt();
  }

  /**
   * Sets the generated int key.
   */
  public void setInt(int columnIndex, int value)
    throws SQLException
  {
    Data data = addData(columnIndex);

    data.setInt(value);
  }

  /**
   * Returns the generated long key.
   */
  public long getLong(int columnIndex)
    throws SQLException
  {
    Data data = _keys.get(columnIndex - 1);

    return data.getLong();
  }

  /**
   * Sets the generated long key.
   */
  public void setLong(int columnIndex, long value)
    throws SQLException
  {
    Data data = addData(columnIndex);

    data.setLong(value);
  }

  /**
   * Extends the capacity for the data.
   */
  private Data addData(int columnIndex)
  {
    for (int i = _keys.size(); i < columnIndex; i++)
      _keys.add(new Data());

    return _keys.get(columnIndex - 1);
  }

  public void close()
  {
    _stmt = null;
  }
}
