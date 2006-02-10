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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib;

import java.sql.*;

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
public class PDO {
  private static final Logger log = Logger.getLogger(PDO.class.getName());
  private static final L10N L = new L10N(PDO.class);

  public static final int ATTR_AUTOCOMMIT = 1;
  public static final int ATTR_CASE = 2;
  public static final int ATTR_CLIENT_VERSION = 3;
  public static final int ATTR_CONNECTION_STATUS = 4;
  public static final int ATTR_CURSOR = 5;
  public static final int ATTR_CURSOR_NAME = 6;
  public static final int ATTR_DRIVER_NAME = 7;
  public static final int ATTR_ERRMODE = 8;
  public static final int ATTR_FETCH_CATALOG_NAMES = 9;
  public static final int ATTR_FETCH_TABLE_NAMES = 10;
  public static final int ATTR_ORACLE_NULLS = 11;
  public static final int ATTR_PERSISTENT = 12;
  public static final int ATTR_PREFETCH = 13;
  public static final int ATTR_SERVER_INFO = 14;
  public static final int ATTR_SERVER_VERSION = 15;
  public static final int ATTR_TIMEOUT = 16;

  public static final int CASE_NATURAL = 1;
  public static final int CASE_LOWER = 2;
  public static final int CASE_UPPER = 2;

  public static final int CURSOR_FWDONLY = 1;
  public static final int CURSOR_SCROLL = 2;

  public static final int ERR_NONE = 0;

  public static final int ERRMODE_SILENT = 1;
  public static final int ERRMODE_WARNING = 2;
  public static final int ERRMODE_EXCEPTION = 3;

  public static final int FETCH_ASSOC = 0x1;
  public static final int FETCH_NUM = 0x2;
  public static final int FETCH_BOTH = FETCH_ASSOC|FETCH_NUM;
  public static final int FETCH_OBJ = 0x4;
  public static final int FETCH_LAZY = 0x5;
  public static final int FETCH_BOUND = 0x6;
  public static final int FETCH_COLUMN = 0x7;
  public static final int FETCH_CLASS = 0x8;
  public static final int FETCH_INTO = 0x9;
  public static final int FETCH_FUNC = 0xa;
  public static final int FETCH_GROUP= 0xb;
  public static final int FETCH_UNIQUE= 0xc;

  public static final int FETCH_ORI_NEXT = 0x100;
  public static final int FETCH_ORI_PRIOR = 0x101;
  public static final int FETCH_ORI_FIRST = 0x102;
  public static final int FETCH_ORI_ABS = 0x103;
  public static final int FETCH_ORI_REL = 0x104;

  public static final int PARAM_BOOL = 1;
  public static final int PARAM_NULL = 2;
  public static final int PARAM_INT = 3;
  public static final int PARAM_STR = 4;
  public static final int PARAM_LOB = 5;
  public static final int PARAM_STMT = 6;
  public static final int PARAM_INPUT_OUTPUT = 0x8000;

  private final Env _env;
  private final String _dsn;

  private JdbcConnectionResource _conn;

  private int _errorCode;
  private String _errorMessage;

  private boolean _inTransaction;

  public PDO(Env env,
	     String dsn,
	     @Optional String user,
	     @Optional String password,
	     @Optional ArrayValue options)
  {
    _env = env;
    _dsn = dsn;

    real_connect(env, user, password);
  }

  /**
   * Starts a transaction.
   */
  public boolean beginTransaction()
  {
    if (_inTransaction)
      return false;

    _inTransaction = true;

    return _conn.setAutoCommit(false);
  }

  /**
   * Commits a transaction.
   */
  public boolean commit()
  {
    if (! _inTransaction)
      return false;

    _inTransaction = false;

    boolean result = _conn.commit();

    _conn.setAutoCommit(true);

    return result;
  }

  /**
   * Executes a statement, returning the number of rows.
   */
  public int exec(String query)
    throws SQLException
  {
    Connection conn = _conn.getConnection();

    Statement stmt = null;
    try {
      stmt = conn.createStatement();
      stmt.setEscapeProcessing(false);

      return stmt.executeUpdate(query);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);

      _errorCode = e.getErrorCode();
      _errorMessage = e.getMessage();

      // XXX: depends on style
      // throw e;
      return -1;
    } finally {
      try {
	if (stmt != null)
	  stmt.close();
      } catch (SQLException e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  /**
   * Queries the database
   */
  public PDOStatement query(String query)
    throws SQLException
  {
    Connection conn = _conn.getConnection();
    Statement stmt = null;

    try {
      stmt = conn.createStatement();
      stmt.setEscapeProcessing(false);

      ResultSet rs = stmt.executeQuery(query);

      stmt = null;

      return new PDOStatement(this, new JdbcResultResource(stmt, rs, _conn));
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);

      _errorCode = e.getErrorCode();
      _errorMessage = e.getMessage();

      // XXX: depends on style
      // throw e;
      return null;
    } finally {
      try {
	if (stmt != null)
	  stmt.close();
      } catch (SQLException e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  /**
   * Quotes the string
   */
  public String quote(String query, @Optional int parameterType)
  {
    return "'" + real_escape_string(query) + "'";
  }

  /**
   * Rolls a transaction back.
   */
  public boolean rollBack()
  {
    if (! _inTransaction)
      return false;

    _inTransaction = false;

    boolean result = _conn.rollback();

    _conn.setAutoCommit(true);

    return result;
  }

  /**
   * Connects to the underlying database.
   */
  private boolean real_connect(Env env, String userName, String password)
  {
    if (_conn != null) {
      env.warning(L.l("Connection is already opened to '{0}'", _conn));
      return false;
    }

    try {
      String host = "localhost";
      int port = 3306;
      String dbname = "test";

      String driver = "com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource";

      String url = "jdbc:mysql://" + host + ":" + port + "/" + dbname;

      Connection jConn = env.getConnection(driver, url, userName, password);

      _conn = new JdbcConnectionResource(jConn);

      env.addResource(_conn);

      return true;
    } catch (SQLException e) {
      env.warning("A link to the server could not be established. " + e.toString());

      _errorCode = e.getErrorCode();
      _errorMessage = e.getMessage();

      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      env.warning("A link to the server could not be established. " + e.toString());

      // env.setSpecialValue("mysqli.connectError", new StringValue(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * Escapes the string
   */
  public String real_escape_string(String str)
  {
    StringBuilder buf = new StringBuilder();

    final int strLength = str.length();

    for (int i = 0; i < strLength; i++) {
      char c = str.charAt(i);

      switch (c) {
      case '\u0000':
        buf.append('\\');
        buf.append('\u0000');
        break;
      case '\n':
        buf.append('\\');
        buf.append('n');
        break;
      case '\r':
        buf.append('\\');
        buf.append('r');
        break;
      case '\\':
        buf.append('\\');
        buf.append('\\');
        break;
      case '\'':
        buf.append('\\');
        buf.append('\'');
        break;
      case '"':
        buf.append('\\');
        buf.append('\"');
        break;
      case '\032':
        buf.append('\\');
        buf.append('Z');
        break;
      default:
        buf.append(c);
        break;
      }
    }

    return buf.toString();
  }

  public String toString()
  {
    return "PDO[" + _dsn + "]";
  }
}
