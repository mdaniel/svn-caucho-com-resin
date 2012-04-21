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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class BoundColumn
{
  private final String _columnAsName;
  private final Value _var;

  private int _column;
  private ColumnType _type;

  private boolean _isValid;

  /**
   * @param column 1-based column index
   * @param var reference that receives the value
   * @param type a PARM_* type, -1 for default
   */
  public BoundColumn(ResultSetMetaData metaData,
                     Value column, Value var, ColumnType type)
    throws SQLException
  {
    if (column.isNumberConvertible()) {
      _column = column.toInt();
      _columnAsName = null;
    }
    else {
      _columnAsName = column.toString();
    }

    _var = var;

    init(metaData);
  }

  private boolean init(ResultSetMetaData metaData)
    throws SQLException
  {
    int columnCount = metaData.getColumnCount();

    if (_columnAsName != null) {
      for (int i = 1; i <= columnCount; i++) {
        String name = metaData.getColumnName(i);
        if (name.equals(_columnAsName)) {
          _column = i;
          break;
        }
      }
    }

    _isValid = _column > 0 && _column <= columnCount;

    return true;
  }

  public boolean bind(Env env, JdbcResultResource rs)
    throws SQLException
  {
    if (!_isValid) {
      // this matches php behaviour
      _var.set(env.getEmptyString());
    }
    else {
      Value value;

      if (_type != null) {
        value = rs.getColumnValue(env, _column, _type.getType());
      }
      else {
        value = rs.getColumnValue(env, _column);
      }

      _var.set(value);
    }

    return true;
  }
}
