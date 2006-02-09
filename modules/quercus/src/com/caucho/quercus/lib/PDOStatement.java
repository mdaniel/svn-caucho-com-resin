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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import java.sql.*;

import java.util.ArrayList;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.resources.JdbcConnectionResource;
import com.caucho.quercus.resources.JdbcResultResource;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.Optional;

/**
 * pdo object oriented API facade
 */
public class PDOStatement {
  private static final Logger log = Logger.getLogger(PDOStatement.class.getName());
  private static final L10N L = new L10N(PDOStatement.class);

  private final PDO _pdo;

  private JdbcResultResource _result;
  private ResultSet _rs;

  private int _fetchStyle = PDO.FETCH_BOTH;

  PDOStatement(PDO pdo, JdbcResultResource result)
  {
    _pdo = pdo;

    _result = result;
  }

  /**
   * Closes the current cursor.
   */
  public boolean closeCursor()
  {
    JdbcResultResource result = _result;
    _result = null;

    if (result == null)
      return false;

    result.close();

    return true;
  }

  /**
   * Fetch the next row.
   */
  public Value fetch(Env env,
		     @Optional int style,
		     @Optional int cursorOrientation,
		     @Optional int cursorOffset)
    throws SQLException
  {
    if (style == 0)
      style = _fetchStyle;

    switch (style) {
    case PDO.FETCH_ASSOC:
      return _result.fetchArray(PDO.FETCH_ASSOC);

    case PDO.FETCH_NUM:
      return _result.fetchArray(PDO.FETCH_NUM);

    case PDO.FETCH_BOTH:
      return _result.fetchArray(PDO.FETCH_BOTH);

    case PDO.FETCH_OBJ:
    case PDO.FETCH_LAZY:
      return _result.fetchObject(env);

    default:
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the number of columns
   */
  public int columnCount()
  {
    JdbcResultResource result = _result;

    if (result == null)
      return 0;

    return result.getFieldCount();
  }

  /**
   * Returns an iterator of the values.
   */
  public Iterator iterator()
  {
    ArrayList<Value> rows = new ArrayList<Value>();

    JdbcResultResource result = _result;
    _result = null;

    if (result == null)
      return rows.iterator();

    Value value;

    while ((value = result.fetchArray(_fetchStyle)).isset()) {
      rows.add(value);
    }

    result.close();

    return rows.iterator();
  }

  /**
   * Sets the fetch mode.
   */
  public boolean setFetchMode(int mode)
  {
    _fetchStyle = mode;

    return true;
  }

  public String toString()
  {
    return "PDOStatement[" + _pdo + "]";
  }
}
