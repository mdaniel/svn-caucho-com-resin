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
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.Optional;

/**
 * mysqli object oriented API facade
 */
public class Mysqli {
  private static final Logger log = Logger.getLogger(Mysqli.class.getName());
  private static final L10N L = new L10N(Mysqli.class);

  private Env _env;
  private String _host;

  private JdbcConnectionResource _conn;

  public Mysqli(Env env,
		@Optional("localhost") String host,
		@Optional String user,
		@Optional String password,
		@Optional String db,
		@Optional("3306") int port,
		@Optional String socket)
  {
    _env = env;

    real_connect(env, host, user, password, db, port, socket, 0);
  }

  public Mysqli(Env env)
  {
    _env = env;
  }

  /**
   * returns the number of affected rows.
   */
  public int affected_rows()
  {
    return validateConnection().getAffectedRows();
  }

  /**
   * sets the autocommit mode
   */
  public boolean autocommit(boolean isAutoCommit)
  {
    return validateConnection().setAutoCommit(isAutoCommit);
  }

  /**
   * Changes the user and database
   *
   * @param user the new user
   * @param password the new password
   * @param db the new database
   */
  public boolean change_user(String user, String password, String db)
  {
    // XXX: these need to be saved
    String host = "localhost";
    int port = 3306;
    String socket = null;
    int flags = 0;

    close(_env);

    return real_connect(_env, host, user, password, db, port, socket, flags);
  }

  /**
   * Returns the client encoding.
   *
   * XXX: stubbed out. has to be revised once we
   * figure out what to do with character encoding
   */
  public String character_set_name()
  {
    return "latin1";
  }

  /**
   * Alias for character_set_name
   */
  public String client_encoding()
  {
    return character_set_name();
  }

  /**
   * Commits the transaction
   */
  public boolean commit()
  {
    return validateConnection().commit();
  }

  /**
   * Returns the error code for the most recent function call
   */
  public int errno()
  {
    return validateConnection().getErrorCode();
  }

  /**
   * Returns the error string for the most recent function call
   */
  public String error()
  {
    return validateConnection().getErrorMessage();
  }

  /**
   * Escapes the string
   */
  public String escape_string(String str)
  {
    return real_escape_string(str);
  }

  /**
   * Returns the host information.
   */
  public String get_client_info()
  {
    return validateConnection().getClientInfo();
  }

  /**
   * Returns the host information.
   */
  public String get_host_info()
  {
    return _host + " via TCP socket";
  }

  /**
   * Returns the protocol information.
   */
  public int get_proto_info()
  {
    return 10;
  }

  /**
   * Returns the server information.
   */
  public String get_server_info()
  {
    try {
      return validateConnection().getServerInfo();
    } catch (SQLException e) {
      return null;
    }
  }

  /**
   * Returns the server information.
   */
  public int get_server_version()
  {
    try {
      String info = validateConnection().getServerInfo();

      String[] result = info.split("[.]");

      if (result.length < 3)
	return 0;

      return (Integer.parseInt(result[0]) * 10000 +
	      Integer.parseInt(result[1]) * 100 +
	      Integer.parseInt(result[2]));
    } catch (SQLException e) {
      return 0;
    }
  }

  /**
   * Returns the number of columns in the last query.
   */
  public int field_count()
  {
    return validateConnection().getFieldCount();
  }

  /**
   * returns ID generated for an AUTO_INCREMENT column by the previous
   * INSERT query on success, 0 if the previous query does not generate
   * an AUTO_INCREMENT value, or FALSE if no MySQL connection was established
   *
   */
  public Value insert_id()
  {
    try {
      JdbcConnectionResource connV = validateConnection();
      Connection conn = connV.getConnection();

      Statement stmt = null;

      try {
        stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT @@identity");

        if (rs.next())
          return new LongValue(rs.getLong(1));
        else
          return BooleanValue.FALSE;
      } finally {
        stmt.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Stub: kills the given mysql thread.
   */
  public boolean kill(int processId)
  {
    return false;
  }

  public Value list_dbs()
  {
    Value result = validateConnection().getCatalogs();

    if (result instanceof JdbcResultResource)
      return _env.wrapJava(new MysqliResult((JdbcResultResource) result));
    else
      return result;
  }

  /**
   * Check for more results in a multi-query
   */
  public boolean more_results()
  {
    return validateConnection().moreResults();
  }

  /**
   * executes one or multiple queries which are
   * concatenated by a semicolon.
   */
  public boolean multi_query(String query)
  {
    return validateConnection().multiQuery(query);
  }

  /**
   * prepares next result set from a previous call to
   * mysqli_multi_query
   */
  public boolean next_result()
  {
    return validateConnection().nextResult();
  }

  /**
   * Sets a mysqli options
   */
  public boolean options(int option, Value value)
  {
    return false;
  }

  /**
   * Pings the database
   */
  public boolean ping()
  {
    try {
      return _conn != null && ! _conn.getConnection().isClosed();
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * Executes a query.
   *
   * @param sql the escaped query string (can contain escape sequences like `\n' and `\Z')
   * @param resultMode ignored
   *
   * @return a {@link JdbcResultResource}, or BooleanValue.FALSE for failure
   */
  public Value query(String sql,
		     @Optional("MYSQLI_STORE_RESULT") int resultMode)
  {
    // XXX: resultMode = MYSQLI_USE_RESULT is an unbuffered query, not supported.
    JdbcConnectionResource conn = validateConnection();

    try {
      Value result = conn.query(sql);

      if (result instanceof JdbcResultResource)
	return _env.wrapJava(new MysqliResult((JdbcResultResource) result));
      else
	return result;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Connects to the underlying database.
   */
  public boolean real_connect(Env env,
			      @Optional("localhost") String host,
			      @Optional String userName,
			      @Optional String password,
			      @Optional String dbname,
			      @Optional("3306") int port,
			      @Optional String socket,
			      @Optional int flags)
  {
    if (_conn != null) {
      env.warning(L.l("Connection is already opened to '{0}'", _conn));
      return false;
    }

    _host = host;

    try {
      if (host == null || host.equals(""))
	host = "localhost";

      String driver = "com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource";

      String url = "jdbc:mysql://" + host + ":" + port + "/" + dbname;

      Connection jConn = env.getConnection(driver, url, userName, password);

      _conn = new JdbcConnectionResource(jConn);

      env.addResource(_conn);

      return true;
    } catch (SQLException e) {
      env.warning("A link to the server could not be established. " + e.toString());
      env.setSpecialValue("mysqli.connectErrno",new LongValue(e.getErrorCode()));
      env.setSpecialValue("mysqli.connectError", new StringValue(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      env.warning("A link to the server could not be established. " + e.toString());
      env.setSpecialValue("mysqli.connectError", new StringValue(e.getMessage()));

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

  /**
   * Unescape the string.
   */
  private static String real_unescape_string(String str)
  {
    StringBuilder result = new StringBuilder(str.length());

    int length = str.length();

    for (int i = 0; i < length; i++) {
      int ch = str.charAt(i);

      if (ch == '\\') {
        i++;

        if (i == length)
          ch = '\\';
        else {
          ch = str.charAt(i);

          switch (ch) {
            case '\\':
            case '\'':
            case '\"':
              break;
            case 'n':
              ch = '\n';
              break;
            case 'r':
              ch = '\r';
              break;
            case 'Z':
              ch = ' ';
              break;
            case 'u':
              // XXX: s/b proper unicode handling?
              if (str.regionMatches(i, "u0000", 0, 5)) {
                ch = (char) 0;
                i += 5;
              }
              else {
                i--;
                ch = '\'';
              }

          }
        }
      } // if ch == '/'

      result.append((char) ch);
    }

    return result.toString();
  }

  /**
   * Rolls the transaction back
   */
  public boolean rollback()
  {
    return validateConnection().rollback();
  }

  /**
   * Selects the underlying database/catalog to use.
   *
   * @param dbname the name of the database to select.
   */
  public boolean select_db(String dbname)
  {
    try {
      validateConnection().setCatalog(dbname);

      return true;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      _env.warning(e.getMessage());
      return false;
    }
  }

  /**
   * Sets the character set
   */
  public boolean set_charset(String charset)
  {
    return false;
  }

  /**
   * Sets a mysqli option
   */
  public boolean set_opt(int option, Value value)
  {
    return options(option, value);
  }

  /**
   * Returns the SQLSTATE error
   */
  public String sqlstate()
  {
    return "HY" + validateConnection().getErrorCode();
  }

  /**
   * returns a string with the status of the connection
   * or FALSE if error
   */
  public Value stat(Env env)
  {
    try {
      JdbcConnectionResource connV = validateConnection();

      Connection conn = connV.getConnection();
      Statement stmt = null;

      StringBuilder str = new StringBuilder();

      try {
        stmt = conn.createStatement();
        stmt.execute("SHOW STATUS");

        ResultSet rs = stmt.getResultSet();

        while (rs.next()) {
          if (str.length() > 0)
            str.append(' ');
          str.append(rs.getString(1));
          str.append(": ");
          str.append(rs.getString(2));
        }

        return new StringValue(str.toString());
      } finally {
        if (stmt != null)
          stmt.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * returns a statement for use with
   * mysqli_stmt_prepare
   */
  public MysqliStatement stmt_init(Env env)
  {
    return new MysqliStatement(validateConnection());
  }

  /**
   * returns a prepared statement
   */
  public MysqliStatement prepare(Env env, String query)
  {
    MysqliStatement stmt = new MysqliStatement(validateConnection());

    stmt.prepare(query);

    return stmt;
  }

  /**
   * Transfers the result set from the last query on the
   * database connection represented by conn.
   *
   * Used in conjunction with mysqli_multi_query
   */
  public Value store_result(Env env)
  {
    Value value = validateConnection().storeResult();

    if (value instanceof JdbcResultResource)
      return env.wrapJava(new MysqliResult((JdbcResultResource) value));
    else
      return value;
  }

  /**
   * Transfers the result set from the last query on the
   * database connection represented by conn.
   *
   * Used in conjunction with mysqli_multi_query
   */
  public Value use_result(Env env)
  {
    Value value = validateConnection().storeResult();

    if (value instanceof JdbcResultResource)
      return env.wrapJava(new MysqliResult((JdbcResultResource) value));
    else
      return value;
  }

  /**
   * Returns a bogus thread id
   */
  public int thread_id()
  {
    return 1;
  }

  /**
   * Returns true for thread_safe
   */
  public boolean thread_safe()
  {
    return true;
  }

  /**
   * returns the number of warnings from the last query
   * in the connection object.
   *
   * @return number of warnings
   */
  public int warning_count()
    throws SQLException
  {
    return validateConnection().getWarningCount();
  }

  /**
   * Closes the connection.
   */
  public boolean close(Env env)
  {
    JdbcConnectionResource conn = _conn;
    _conn = null;

    if (conn != null) {
      env.removeResource(conn);
      conn.close();

      return true;
    }
    else
      return false;
  }

  JdbcConnectionResource validateConnection()
  {
    JdbcConnectionResource conn = _conn;

    if (conn == null)
      _env.error(L.l("Connection is not properly initialized"));

    return conn;
  }

  public String toString()
  {
    if (_conn != null)
      return "Mysqli[" + _host + "]";
    else
      return "Mysqli[]";
  }
}
