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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib.db;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.Closeable;

import java.sql.*;

import com.caucho.quercus.env.*;
import com.caucho.sql.UserConnection;
import com.caucho.util.LruCache;
import com.caucho.util.L10N;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;

/**
 * Represents a JDBC Connection value.
 */
public abstract class JdbcConnectionResource implements Closeable {
  private static final L10N L = new L10N(JdbcConnectionResource.class);
  private static final Logger log
    = Logger.getLogger(JdbcConnectionResource.class.getName());

  private static LruCache<TableKey,JdbcTableMetaData> _tableMetadataMap
    = new LruCache<TableKey,JdbcTableMetaData>(256);

  private Connection _conn;
  // cached statement
  private Statement _stmt;

  private DatabaseMetaData _dmd;

  private JdbcResultResource _rs;
  private int _affectedRows;

  private String _errorMessage = "";
  private int _errorCode;
  private boolean _fieldCount = false;
  private SQLWarning _warnings;

  private Env _env;
  private String _host;
  private String _dbname;
  private int _port;

  private boolean _connected;

  // postgres asynchronous queries
  MysqliResult _asyncResult;
  // named prepared statements for postgres
  private HashMap<String,MysqliStatement> _stmtTable
    = new HashMap<String,MysqliStatement>();

  /**
   * mysqli_multi_query populates _resultValues
   * NB: any updates (ie: INSERT, UPDATE, DELETE) will
   * have the update counts ignored.
   *
   * Has been stored tells moreResults whether the
   * _nextResultValue has been stored already.
   * If so, more results will return true only if
   * there is another result.
   *
   * _hasBeenStored is set to true by default.
   * if _hasBeenUsed == false, then
   * _resultValues.get(_nextResultValue)
   * is ready to be used by the next call to
   * mysqli_store_result or mysqli_use_result.
   */
  private ArrayList<JdbcResultResource> _resultValues = new ArrayList<JdbcResultResource>();
  private int _nextResultValue = 0;
  private boolean _hasBeenUsed = true;

  // This is a workaround flag for postgres features (should be replaced with PostgresModule)
  private boolean _pgconn = false;

  public JdbcConnectionResource(Env env)
  {
    _env = env;
  }

  public boolean isConnected()
  {
    return _connected;
  }

  /**
   * Set the current underlying connection and
   * corresponding information: host, port and
   * database name.
   *
   * @param host server host
   * @param port server port
   * @param dbname database name
   */
  public void setConnection(String host,
                            int port,
                            String dbname,
                            Connection conn,
                            boolean pgconn)
  {
    _host = host;
    _port = port;
    _dbname = dbname;

    _conn = conn;

    if (conn != null) {
      _connected = true;

      _env.addClose(this);
    }

    _pgconn = pgconn;
  }

  /**
   * Connects to the underlying database.
   */
  public abstract boolean real_connect(Env env,
                                       @Optional("localhost") String host,
                                       @Optional String userName,
                                       @Optional String password,
                                       @Optional String dbname,
                                       @Optional int port,
                                       @Optional String socket,
                                       @Optional int flags,
                                       @Optional String driver,
                                       @Optional String url);

  /**
   * Returns the affected rows from the last query.
   */
  public int getAffectedRows()
  {
    return _affectedRows;
  }

  public void setAffectedRows(int i)
  {
    _affectedRows = i;
  }

  /**
   * @return _fieldCount
   */
  public int getFieldCount()
  {
    if (_rs == null) {
      return 0;
    } else {
      return _rs.getFieldCount();
    }
  }

  /**
   * Returns JdbcResultResource of available databases
   */
  public Value getCatalogs()
  {
    clearErrors();

    try {
      if (_dmd == null)
        _dmd = _conn.getMetaData();

      ResultSet rs = _dmd.getCatalogs();

      if (rs != null)
        return new JdbcResultResource(_stmt, rs, this);
      else
        return BooleanValue.FALSE;
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * @return current catalog or false if error
   */
  public Value getCatalog()
  {
    clearErrors();

    try {
      return new StringValueImpl(_conn.getCatalog());
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the client version
   */
  public String getClientInfo()
  {
    try {
      if (_dmd == null)
        _dmd = _conn.getMetaData();

      return _dmd.getDatabaseProductVersion();
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return null;
    }
  }

  /**
   * Returns the connection
   */
  public Connection getConnection()
    throws SQLException
  {
    return _conn;
  }

  /**
   * Returns the data source.
   */
  public String getURL()
  {
    return ((UserConnection) _conn).getURL();
  }

  /**
   * Returns the last error code.
   */
  public int getErrorCode()
  {
    return _errorCode;
  }

  /**
   * Returns the last error message.
   */
  public String getErrorMessage()
  {
    return _errorMessage;
  }

  /**
   *
   * returns the URL string for the given connection
   * IE: jdbc:mysql://localhost:3306/test
   * XXX: PHP returns Localhost via UNIX socket
   */
  public String getHostInfo()
    throws SQLException
  {
    if (_dmd == null)
      _dmd = _conn.getMetaData();

    return _dmd.getURL();
  }

  /**
   * returns the server version
   * XXX: PHP seems to return the same value
   * for client_info and server_info
   */
  public String getServerInfo()
    throws SQLException
  {
    return getMetaData().getDatabaseProductVersion();
  }

  /**
   * Returns the table metadata.
   */
  public JdbcTableMetaData getTableMetaData(String catalog,
                                            String schema,
                                            String table)
    throws SQLException
  {
    TableKey key = new TableKey(getURL(), catalog, schema, table);

    // XXX: needs invalidation on DROP or ALTER
    JdbcTableMetaData tableMd = _tableMetadataMap.get(key);

    if (tableMd != null && tableMd.isValid())
      return tableMd;

    tableMd = new JdbcTableMetaData(catalog, schema, table, getMetaData());

    _tableMetadataMap.put(key, tableMd);

    return tableMd;
  }

  private DatabaseMetaData getMetaData()
    throws SQLException
  {
    if (_dmd == null)
      _dmd = _conn.getMetaData();

    return _dmd;
  }

  /**
   * indicates if one or more result sets are
   * available from a multi query
   *
   * _hasBeenStored tells moreResults whether the
   * _nextResultValue has been stored already.
   * If so, more results will return true only if
   * there is another result.
   */
  public boolean moreResults()
  {
    return !_hasBeenUsed || _nextResultValue < _resultValues.size() - 1;
  }

  /**
   * prepares the next resultset from
   * a multi_query
   */
  public boolean nextResult()
  {
    if (_nextResultValue + 1 < _resultValues.size()) {
      _hasBeenUsed = false;
      _nextResultValue++;
      return true;
    } else
      return false;
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

    //@todo driver and url for postgres (see null below)

    return real_connect(_env, host, user, password, db, port, socket, flags, null, null);
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
   * Returns the error code for the most recent function call
   */
  public int errno()
  {
    if (_connected)
      return getErrorCode();
    else
      return 0;
  }

  /**
   * Returns the error string for the most recent function call
   */
  public String error()
  {
    if (_connected)
      return getErrorMessage();
    else
      return null;
  }

  /**
   * Escapes the string
   */
  public Value escape_string(StringValue str)
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
   * Returns the database name.
   */
  public String get_dbname()
  {
    return _dbname;
  }

  /**
   * Returns the host information.
   */
  public String get_host_info()
  {
    return _host + " via TCP socket";
  }

  /**
   * Returns the host name.
   */
  public String get_host_name()
  {
    return _host;
  }

  /**
   * Returns the port number.
   */
  public int get_port_number()
  {
    return _port;
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

      return infoToVersion(info);
    } catch (SQLException e) {
      return 0;
    }
  }

  static int infoToVersion(String info)
  {
    String[] result = info.split("[.a-z-]");

    if (result.length < 3)
      return 0;

    return (Integer.parseInt(result[0]) * 10000 +
            Integer.parseInt(result[1]) * 100 +
            Integer.parseInt(result[2]));
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
        if (stmt != null)
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

      return _connected && ! getConnection().isClosed();

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
   * @return a {@link JdbcResultResource}, or null for failure
   */
  public JdbcResultResource query(String sql,
				  @Optional("MYSQLI_STORE_RESULT") int resultMode)
  {
    try {
      Value value = real_query(sql);

      if (value instanceof JdbcResultResource)
        return (JdbcResultResource) value;

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return null;
  }

  /*
  public JdbcResultResource query(String sql)
  {
    return query(sql, MysqliModule.MYSQLI_STORE_RESULT);
  }
  */

  /**
   * Escapes the string
   */
  public Value real_escape_string(StringValue str)
  {
    StringBuilderValue buf = new StringBuilderValue(str.length());

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

    return buf;
  }

  /**
   * Selects the underlying database/catalog to use.
   *
   * @param dbname the name of the database to select.
   */
  public boolean select_db(String dbname)
  {
    try {
      if (_connected) {
        validateConnection().setCatalog(dbname);

        return true;
      }
      else
        return false;
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

        return new StringValueImpl(str.toString());
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
  {
    return validateConnection().getWarningCount();
  }

  /**
   * Closes the connection.
   */
  public boolean close(Env env)
  {
    if (_connected) {

      _connected = false;
      env.removeClose(this);
      this.close();
      return true;

    } else {
      return false;
    }
  }

  public JdbcConnectionResource validateConnection()
  {
    if (!_connected) {
      throw _env.errorException(L.l("Connection is not properly initialized"));
    }

    return this;
  }

  public void putStatement(String name,
                           MysqliStatement stmt)
  {
    _stmtTable.put(name, stmt);
  }

  public MysqliStatement getStatement(String name)
  {
    return (MysqliStatement) _stmtTable.get(name);
  }

  public MysqliStatement removeStatement(String name)
  {
    return (MysqliStatement) _stmtTable.remove(name);
  }

  public void setAsynchronousResult(MysqliResult asyncResult)
  {
    _asyncResult = asyncResult;
  }

  public MysqliResult getAsynchronousResult()
  {
    return _asyncResult;
  }

  /**
   * returns the next jdbcResultValue
   */
  public Value storeResult()
  {
    if (!_hasBeenUsed) {
      _hasBeenUsed = true;

      return _resultValues.get(_nextResultValue);
    } else
      return BooleanValue.FALSE;
  }

  /**
   * splits a string of multiple queries separated
   * by ";" into an arraylist of strings
   */
  private ArrayList<String> splitMultiQuery(String sql)
  {
    ArrayList<String> result = new ArrayList<String>();
    String query = "";
    int length = sql.length();
    boolean inQuotes = false;
    char c;

    for (int i = 0; i < length; i++) {
      c = sql.charAt(i);

      if (c == '\\') {
        query += c;
        if (i < length - 1) {
          query += sql.charAt(i+1);
          i++;
        }
        continue;
      }

      if (inQuotes) {
        query += c;
        if (c == '\'') {
          inQuotes = false;
        }
        continue;
      }

      if (c == '\'') {
        query += c;
        inQuotes = true;
        continue;
      }

      if (c == ';') {
        result.add(query.trim());
        query = "";
      } else
        query += c;
    }

    if (query != null)
      result.add(query.trim());

    return result;
  }

  /**
   * Execute a single query.
   */
  private Value real_query(String sql)
  {
    clearErrors();

    // Empty _resultValues on new call to query
    // But DO NOT close the individual result sets.
    // They may still be in use.
    _resultValues.clear();
    _rs = null;

    Statement stmt = null;

    try {
      stmt = _conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                   ResultSet.CONCUR_READ_ONLY);
      stmt.setEscapeProcessing(false); // php/1406

      if (stmt.execute(sql)) {
        ResultSet rs = stmt.getResultSet();

        _rs = new JdbcResultResource(stmt, rs, this);
        _affectedRows = 0;
        _resultValues.add(_rs);
        _warnings = stmt.getWarnings();
      } else {
        // php/4310 should return a result set
        // for update statements. It is always
        // null though. So keep the stmt for
        // future reference (PostgresModule.pg_last_oid)

        // php/1f33
        if (_pgconn) {
          _rs = new JdbcResultResource(stmt, null, this);
          _resultValues.add(_rs);
        }
        _affectedRows = 0;
        _affectedRows = stmt.getUpdateCount();
        _rs.setAffectedRows(_affectedRows);
        _warnings = stmt.getWarnings();
        // for php/4310
        if (!_pgconn) {
          stmt.close();
        }
      }
    } catch (DataTruncation truncationError) {
      try {
        _affectedRows = stmt.getUpdateCount();
        _warnings = stmt.getWarnings();
      } catch (SQLException e) {
        _errorMessage = e.getMessage();
        _errorCode = e.getErrorCode();
        log.log(Level.WARNING, e.toString(), e);
        return BooleanValue.FALSE;
      }
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      log.log(Level.WARNING, e.toString(), e);
      return BooleanValue.FALSE;
    }

    if (_resultValues.size() > 0) {
      _nextResultValue = 0;
      _hasBeenUsed = false;
      return _resultValues.get(0);
    } else
      return BooleanValue.TRUE;
  }

  /**
   * Used by the
   * various mysqli functions to query the database
   * for metadata about the resultset which is
   * not in ResultSetMetaData.
   *
   * This function DOES NOT clear existing resultsets.
   */
  public Value metaQuery(String sql,
                         String catalog)
  {
    clearErrors();

    Value currentCatalog = getCatalog();

    try {
      _conn.setCatalog(catalog);

      // need to create statement after setting catalog or
      // else statement will have wrong catalog
      Statement stmt = _conn.createStatement();
      stmt.setEscapeProcessing(false);

      if (stmt.execute(sql)) {
        Value result = new JdbcResultResource(stmt, stmt.getResultSet(), this);
        _conn.setCatalog(currentCatalog.toString());
        return result;
      } else {
        _conn.setCatalog(currentCatalog.toString());
        return BooleanValue.FALSE;
      }
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      log.log(Level.WARNING, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Used for multiple queries. the
   * JdbcConnectionResource now stores the
   * result sets so that mysqli_store_result
   * and mysqli_use_result can return result values.
   *
   * XXX: this may not function correctly in the
   * context of a transaction.  Unclear wether
   * mysqli_multi_query was designed with transactions
   * in mind.
   *
   * XXX: multiQuery sets fieldCount to true or false
   * depending on the last query entered.  Not sure what
   * actual PHP intention is.
   */
  public boolean multiQuery(String sql)
  {
    clearErrors();

    // Empty _resultValues on new call to query
    // But DO NOT close the individual result sets.
    // They may still be in use.
    _resultValues.clear();

    ArrayList<String> splitQuery = splitMultiQuery(sql);

    Statement stmt = null;

    try {
      _rs = null;

      for (String s : splitQuery) {
        stmt = _conn.createStatement();
        stmt.setEscapeProcessing(false);
        if (stmt.execute(s)) {
          _affectedRows = 0;
          _rs = new JdbcResultResource(stmt, stmt.getResultSet(), this);
          _resultValues.add(_rs);
          _warnings = stmt.getWarnings();
        } else {
          _affectedRows = stmt.getUpdateCount();
          _warnings = stmt.getWarnings();
        }
      }
    } catch (DataTruncation truncationError) {
      try {
        _affectedRows = stmt.getUpdateCount();
        _warnings = stmt.getWarnings();
      } catch (SQLException e) {
        _errorMessage = e.getMessage();
        _errorCode = e.getErrorCode();
        log.log(Level.WARNING, e.toString(), e);
        return false;
      }
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    if (_resultValues.size() > 0) {
      _nextResultValue = 0;
      _hasBeenUsed = false;
    }

    return true;
  }

  /**
   * sets auto-commmit to true or false
   */
  public boolean setAutoCommit(boolean mode)
  {
    clearErrors();

    try {
      _conn.setAutoCommit(mode);
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    return true;
  }

  /**
   * commits the transaction of the current connection
   */
  public boolean commit()
  {
    clearErrors();

    try {
      _conn.commit();
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    return true;
  }

  /**
   * rolls the current transaction back
   *
   * NOTE: quercus doesn't seem to support the idea
   * of savepoints
   */
  public boolean rollback()
  {
    clearErrors();

    try {
      _conn.rollback();
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    return true;
  }
  /**
   * Sets the catalog
   */
  public void setCatalog(String name)
    throws SQLException
  {
    clearErrors();

    _conn.setCatalog(name);
  }

  /**
   * returns a string with the status of the connection
   */
  public Value stat()
  {
    clearErrors();

    StringBuilder str = new StringBuilder();

    try {
      Statement stmt = _conn.createStatement();
      stmt.execute("SHOW STATUS");

      ResultSet rs = stmt.getResultSet();

      while (rs.next()) {
        if (str.length() > 0)
          str.append(' ');
        str.append(rs.getString(1));
        str.append(": ");
        str.append(rs.getString(2));
      }

      return new StringValueImpl(str.toString());
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();

      log.log(Level.WARNING, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return _conn.toString();
  }

  /**
   * Closes the connection.
   */
  public void close()
  {
    try {
      Statement stmt = _stmt;
      _stmt = null;

      if (stmt != null)
        stmt.close();
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      Connection conn = _conn;
      // XXX: since the above code doesn't check for _conn == null can't null
      // _conn = null;

      if (conn != null)
        conn.close();
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * This functions queries the connection with "SHOW WARNING"
   *
   * @return # of warnings
   */
  public int getWarningCount()
  {
    if (_warnings != null) {
      Value warningResult = metaQuery("SHOW WARNINGS",getCatalog().toString());
      Value warningCount = null;

      if (warningResult instanceof JdbcResultResource) {
        warningCount =
          JdbcResultResource.getNumRows(((JdbcResultResource) warningResult).getResultSet());
      }

      if ((warningCount != null) && (warningCount != BooleanValue.FALSE))
        return warningCount.toInt();
      else
        return 0;
    } else
      return 0;
  }

  /**
   * This function was added for PostgreSQL pg_last_notice
   *
   * @return warning messages
   */
  public SQLWarning getWarnings()
  {
    return _warnings;
  }

  private void clearErrors()
  {
    _errorMessage = null;
    _errorCode = 0;
    _warnings = null;
  }

  static class TableKey {
    private final String _url;
    private final String _catalog;
    private final String _schema;
    private final String _table;

    TableKey(String url, String catalog, String schema, String table)
    {
      _url = url;
      _catalog = catalog;
      _schema = schema;
      _table = table;
    }

    public int hashCode()
    {
      int hash = 37;

      if (_catalog != null)
        hash = 65537 * hash + _catalog.hashCode();

      if (_schema != null)
        hash = 65537 * hash + _schema.hashCode();

      if (_table != null)
        hash = 65537 * hash + _table.hashCode();

      return hash;
    }

    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      else if (! (o instanceof TableKey))
        return false;

      TableKey key = (TableKey) o;

      if (_url != key._url)
        return false;

      if ((_catalog == null) != (key._catalog == null))
        return false;
      else if (_catalog != null && ! _catalog.equals(key._catalog))
        return false;

      if ((_schema == null) != (key._schema == null))
        return false;
      else if (_schema != null && ! _schema.equals(key._schema))
        return false;

      if ((_table == null) != (key._table == null))
        return false;
      else if (_table != null && ! _table.equals(key._table))
        return false;

      return true;
    }
  }
}

