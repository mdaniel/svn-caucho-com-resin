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

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.resources.JdbcConnectionResource;
import com.caucho.quercus.resources.JdbcResultResource;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.NullValue;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;

/**
 * mysqli object oriented API facade
 */
public class MysqliStatement {
  private static final Logger log
    = Logger.getLogger(MysqliStatement.class.getName());
  private static final L10N L = new L10N(MysqliStatement.class);

  private JdbcConnectionResource _conn;

  private String _query;
  
  private PreparedStatement _pstmt;
  private ResultSet _rs;
  private ResultSetMetaData _rsMetaData;
  
  private char[] _types;
  private Value[] _params;
  private Value[] _results;
  
  private String _errorMessage;
  private int _errorCode;
  
  MysqliStatement(JdbcConnectionResource conn)
  {
    _conn = conn;
  }

  /**
   * returns the number of affected rows.
   */
  public int affected_rows()
  {
    return validateConnection().getAffectedRows();
  }

  /**
   * creates _types and _params array for this prepared statement.
   *
   * @param types  = string of i,d,s,b (ie: "idds")
   * @param params = array of values (probably Vars)
   * @return true on success false on failure
   */
  public boolean bind_param(Env env,
			    String types,
			    @Reference Value[] params)
  {
    final int size = types.length();

    // Check to see that types and params have the same length
    if (size != params.length) {
      env.warning(L.l("number of types does not match number of parameters"));
      return false;
    }

    // Check to see that types only contains i,d,s,b
    for (int i = 0; i < size; i++) {
      if ("idsb".indexOf(types.charAt(i)) < 0) {
        env.warning(L.l("invalid type string {0}", types));
        return false;
      }
    }

    _types = new char[size];
    _params = new Value[size];

    for (int i = 0; i < size; i++) {
      _types[i] = types.charAt(i);
      _params[i] = params[i];
    }

    return true;
  }

  /**
   * binds outparams to result set
   */
  public boolean bind_result(Env env, @Reference Value[] outParams)
  {
    int size = outParams.length;
    int numColumns;

    try {
      ResultSetMetaData md = getMetaData();

      numColumns = md.getColumnCount();
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }

    if (size != numColumns) {
      env.warning(L.l("number of bound variables does not equal number of columns"));
      return false;
    }

    _results = new Value[size];

    for (int i = 0; i < size; i++) {
      _results[i] = outParams[i];
    }

    return true;
  }

  /**
   * Seeks to a given result
   */
  public void data_seek(int offset)
  {
    JdbcResultResource.setRowNumber(_rs, offset);
  }

  /**
   * Returns the mysql error number
   */
  public int errno()
  {
    return _errorCode;
  }

  /**
   * Returns the mysql error message
   */
  public String error()
  {
    return _errorMessage;
  }

  /**
   * executes statement stored in resultV. The statement has been prepared using mysqli_prepare.
   * <p/>
   * returns true on success or false on failure
   */
  public boolean execute(Env env)
  {
    try {
      if (_types != null) {
        int size = _types.length;
        for (int i = 0; i < size; i++) {
          switch (_types[i]) {
          case 'i':
            _pstmt.setInt(i + 1, _params[i].toInt());
            break;
          case 'd':
            _pstmt.setDouble(i + 1, _params[i].toDouble());
            break;
            // XXX: blob needs to be redone
            // Currently treated as a string
          case 'b':
            _pstmt.setString(i + 1, _params[i].toString());
            break;
          case 's':
            _pstmt.setString(i + 1, _params[i].toString());
            break;
          default:
            break;
          }
        }
      }

      if (_pstmt.execute()) {
        _conn.setAffectedRows(0);
        _rs = _pstmt.getResultSet();
      } else {
        _conn.setAffectedRows(_pstmt.getUpdateCount());
      }

    } catch (SQLException e) {
      env.warning(L.l(e.toString()));
      log.log(Level.FINE, e.toString(), e);
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      return false;
    }

    return true;
  }

  /**
   * Fetch results from a prepared statement into bound variables.
   * <p/>
   * returns true on success, false on error null if no more rows
   */
  public boolean fetch()
  {
    try {
      if (_rs.next()) {
	ResultSetMetaData md = getMetaData();

        int size = _results.length;
        for (int i = 0; i < size; i++) {
          _results[i].set(JdbcResultResource.getColumnValue(_rs, md, i + 1));
        }

        return true;
      } else
        return false;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * Frees the associated result
   */
  public void free_result()
  {
    try {
      ResultSet rs = _rs;
      _rs = null;
      _rsMetaData = null;

      if (rs != null)
	rs.close();
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Returns the number of rows in the result
   */
  public Value num_rows()
  {
    try {
      if (_rs != null)
	return JdbcResultResource.getNumRows(_rs);
      else
	return BooleanValue.FALSE;
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * counts the number of markers in the query string
   */
  public int param_count()
  {
    if (_query == null)
      return -1;

    int count = 0;
    int length = _query.length();
    boolean inQuotes = false;
    char c;

    for (int i = 0; i < length; i++) {
      c = _query.charAt(i);

      if (c == '\\') {
        if (i < length - 1)
          i++;
        continue;
      }

      if (inQuotes) {
        if (c == '\'')
          inQuotes = false;
        continue;
      }

      if (c == '\'') {
        inQuotes = true;
        continue;
      }

      if (c == '?') {
        count++;
      }
    }

    return count;
  }

  /**
   * prepares statement for query
   *
   * @param query SQL query
   *
   * @return true on success or false on failure
   */
  public boolean prepare(String query)
  {
    try {
      if (_pstmt != null)
        _pstmt.close();
      
      _query = query;
      
      _pstmt = _conn.getConnection().prepareStatement(query);
      
      return true;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      return false;
    }
  }

  /**
   * Resets the statement
   */
  public boolean reset()
  {
    return true;
  }

  /**
   * Returns result information for metadata
   */
  public Value result_metadata(Env env)
    throws SQLException
  {
    if (_rs != null) {
      JdbcResultResource result;

      result = new JdbcResultResource(_rs.getMetaData(), _conn);
      
      return env.wrapJava(new MysqliResult(result));
    }
    else
      return null;
  }

  /**
   * Returns the SQLSTATE error
   */
  public String sqlstate()
  {
    return "HY" + errno();
  }

  /**
   * Saves the result as buffered.
   */
  public boolean store_result()
  {
    return true;
  }

  public boolean close()
    throws SQLException
  {
    JdbcConnectionResource conn = _conn;
    _conn = null;

    Statement stmt = _pstmt;
    stmt = null;

    _rs = null;
    
    if (stmt != null)
      stmt.close();

    return conn != null;
  }

  private JdbcConnectionResource validateConnection()
  {
    return _conn;
  }
  
  private ResultSetMetaData getMetaData()
    throws SQLException
  {
    if (_rsMetaData == null)
      _rsMetaData = _rs.getMetaData();

    return _rsMetaData;
  }

  public String toString()
  {
    return "MysqliStatement[" + _conn + "]";
  }
}
