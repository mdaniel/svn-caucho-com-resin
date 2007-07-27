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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * mysqli object oriented API facade
 */
public class Mysqli extends JdbcConnectionResource {
  private static final Logger log = Logger.getLogger(Mysqli.class.getName());
  private static final L10N L = new L10N(Mysqli.class);

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
  private ArrayList<JdbcResultResource> _resultValues
    = new ArrayList<JdbcResultResource>();
  private int _nextResultValue = 0;
  private boolean _hasBeenUsed = true;

  public Mysqli(Env env,
                @Optional("localhost") String host,
                @Optional String user,
                @Optional String password,
                @Optional String db,
                @Optional("3306") int port,
                @Optional String socket,
                @Optional int flags,
                @Optional String driver,
                @Optional String url)
  {
    super(env);

    connectInternal(env, host, user, password, db, port, socket, flags, driver, url);
  }

  protected Mysqli(Env env)
  {
    super(env);
  }

  public String getResourceType()
  {
    return "mysql link";
  }

  /**
   * Connects to the underlying database.
   */
  protected boolean connectInternal(Env env,
                                    @Optional("localhost") String host,
                                    @Optional String userName,
                                    @Optional String password,
                                    @Optional String dbname,
                                    @Optional("3306") int port,
                                    @Optional String socket,
                                    @Optional int flags,
                                    @Optional String driver,
                                    @Optional String url)
  {
    if (isConnected()) {
      env.warning(L.l("Connection is already opened to '{0}'", this));
      return false;
    }

    if (port <= 0)
      port = 3306;

    try {
      if (host == null || host.equals(""))
        host = "localhost";

      if (driver == null || driver.equals("")) {
        driver = "com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource";
      }

      if (url == null || url.equals("")) {
        url = "jdbc:mysql://" + host + ":" + port + "/" + dbname;
      }

      Connection jConn = env.getConnection(driver, url, userName, password);

      setConnection(host, userName, password, dbname, port, jConn, driver, url);

      return true;

    } catch (SQLException e) {
      env.warning(L.l("A link to the server could not be established.\n  url={0}\n  driver={1}\n  {2}", url, driver, e.toString()));

      env.setSpecialValue("mysqli.connectErrno",new LongValue(e.getErrorCode()));
      env.setSpecialValue("mysqli.connectError", new UnicodeValueImpl(e.getMessage()));

      return false;
    } catch (Exception e) {
      env.warning(L.l("A link to the server could not be established.\n  url={0}\n  driver={1}\n  {2}", url, driver, e.toString()));
      env.setSpecialValue("mysqli.connectError", new UnicodeValueImpl(e.toString()));

      return false;
    }
  }

  public int getaffected_rows()
  {
    return affected_rows();
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
    close(getEnv());

    if ((user == null) || user.equals("")) {
      user = getUserName();
    }

    if ((password == null) || password.equals("")) {
      password = getPassword();
    }

    if ((db == null) || db.equals("")) {
      db = getDbName();
    }

    return connectInternal(getEnv(), getHost(), user, password, db, getPort(),
                           "", 0, getDriver(), getUrl());
  }

  /**
   * Returns the client encoding.
   *
   * XXX: stubbed out. has to be revised once we
   * figure out what to do with character encoding
   */
  public String character_set_name()
  {
    return getCharacterSetName();
  }

  /**
   * Alias for character_set_name
   */
  public String client_encoding()
  {
    return character_set_name();
  }

  /**
   * Quercus function to get the field 'errno'.
   */
  public int geterrno()
  {
    return errno();
  }
  
  /**
   * Returns the error code for the most recent function call
   */
  public int errno()
  {
    if (isConnected())
      return getErrorCode();
    else
      return 0;
  }

  /**
   * Quercus function to get the field 'error'.
   */
  public String geterror()
  {
    return error();
  }
  
  /**
   * Returns the error string for the most recent function call
   */
  public String error()
  {
    String errorString = super.error();

    if (errorString == null) {
      return "";
    }

    return errorString;
  }

  /**
   * Escapes the string
   */
  public Value escape_string(StringValue str)
  {
    return real_escape_string(str);
  }

  /**
   * Quercus function to get the field 'client_info'.
   */
  public String getclient_info(Env env)
  {
    return get_client_info(env);
  }
  
  /**
   * Returns the client information.
   */
  public String get_client_info(Env env)
  {
    return MysqlModule.mysql_get_client_info(env);
  }

  /**
   * Quercus function to get the field 'client_version'.
   */
  public int getclient_version(Env env)
  {
    return MysqliModule.mysqli_get_client_version(env);
  }
  
  /**
   * Returns the database name.
   */
  public String get_dbname()
  {
    return getDbName();
  }
  
  /**
   * Quercus function to get the field 'host_info'.
   */
  public String gethost_info()
  {
    return get_host_info();
  }
  
  /**
   * Returns the host information.
   */
  public String get_host_info()
  {
    return getHost() + " via TCP socket";
  }

  /**
   * Returns the host name.
   */
  public String get_host_name()
  {
    return getHost();
  }

  /**
   * Quercus function to get the field 'info'.
   */
  public String getinfo()
  {
    throw new UnimplementedException("mysqli info field");
    
    //return MysqliModule.mysqli_info(this);
  }
  
  /**
   * Returns the port number.
   */
  public int get_port_number()
  {
    return getPort();
  }

  /**
   * Quercus function to get the field 'protocol_version'.
   */
  public int getprotocol_version()
  {
    return get_proto_info();
  }
  
  /**
   * Returns the protocol information.
   */
  public int get_proto_info()
  {
    return 10;
  }

  /**
   * Quercus function to get the field 'server_info'.
   */
  public String getserver_info()
  {
    return get_server_info();
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
   * Quercus function to get the field 'server_version'.
   */
  public int getserver_version()
  {
    return get_server_version();
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

  /**
   * Quercus function to get the field 'field_count'.
   */
  public int getfield_count()
  {
    return field_count();
  }
  
  /**
   * Returns the number of columns in the last query.
   */
  public int field_count()
  {
    return validateConnection().getFieldCount();
  }

  /**
   * Quercus function to get the field 'insert_id'.
   */
  public Value getinsert_id()
  {
    return insert_id();
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

  @ReturnNullAsFalse
  public JdbcResultResource list_dbs()
  {
    return validateConnection().getCatalogs();
  }

  /**
   * Check for more results in a multi-query
   */
  public boolean more_results()
  {
    return ((Mysqli) validateConnection()).moreResults();
  }

  /**
   * executes one or multiple queries which are
   * concatenated by a semicolon.
   */
  public boolean multi_query(String query)
  {
    return ((Mysqli) validateConnection()).multiQuery(query);
  }

  /**
   * prepares next result set from a previous call to
   * mysqli_multi_query
   */
  public boolean next_result()
  {
    return ((Mysqli) validateConnection()).nextResult();
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
    return super.ping();
  }

  /**
   * Executes a query.
   *
   * @param env the PHP executing environment
   * @param sql the escaped query string (can contain escape sequences like `\n' and `\Z')
   * @param resultMode ignored
   *
   * @return a {@link JdbcResultResource}, or null for failure
   */
  public Value query(Env env,
                     String sql,
                     @Optional("MYSQLI_STORE_RESULT") int resultMode)
  {
    MysqliResult result = (MysqliResult) realQuery(sql);

    if (result == null) {
      if (getErrorCode() == 0) {
        // No error, this was an UPDATE, DELETE, etc.
        return BooleanValue.TRUE;
      }
      return BooleanValue.FALSE;
    }

    return env.wrapJava(result);
  }

  /**
   * returns a prepared statement
   */
  public MysqliStatement prepare(Env env, String query)
  {
    MysqliStatement stmt = new MysqliStatement((Mysqli) validateConnection());

    stmt.prepare(query);

    return stmt;
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
    return connectInternal(env, host, userName, password, dbname, port, socket,
                           flags, "", "");
  }

  /**
   * Escapes the string
   */
  public Value real_escape_string(StringValue str)
  {
    return realEscapeString(str);
  }

  /**
   * Rolls the current transaction back.
   */
  public boolean rollback()
  {
    return super.rollback();
  }

  /**
   * Selects the underlying database/catalog to use.
   *
   * @param dbname the name of the database to select.
   */
  public boolean select_db(String dbname)
  {
    try {
      if (isConnected()) {
        validateConnection().setCatalog(dbname);

        return true;
      }
      else
        return false;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      getEnv().warning(e.getMessage());
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
   * Quercus function to get the field 'sqlstate'.
   */
  public String getsqlstate()
  {
    return sqlstate();
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

        return new UnicodeValueImpl(str.toString());
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
   * returns a string with the status of the connection
   *
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
      saveErrors(e);
      log.log(Level.WARNING, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }
  */

  /**
   * returns a statement for use with
   * mysqli_stmt_prepare
   */
  public MysqliStatement stmt_init(Env env)
  {
    return new MysqliStatement((Mysqli) validateConnection());
  }

  /**
   * Transfers the result set from the last query on the
   * database connection represented by conn.
   *
   * Used in conjunction with mysqli_multi_query
   */
  @ReturnNullAsFalse
  public JdbcResultResource store_result(Env env)
  {
    return ((Mysqli) validateConnection()).storeResult();
  }


  /**
   * Quercus function to get the field 'thread_id'.
   */
  public int getthread_id()
  {
    return thread_id();
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
   * Transfers the result set from the last query on the
   * database connection represented by conn.
   *
   * Used in conjunction with mysqli_multi_query
   */
  @ReturnNullAsFalse
  public JdbcResultResource use_result(Env env)
  {
    return ((Mysqli) validateConnection()).storeResult();
  }

  /**
   * Quercus function to get the field 'warning_count'.
   */
  public int getwarning_count()
  {
    return warning_count();
  }
  
  /**
   * returns the number of warnings from the last query
   * in the connection object.
   *
   * @return number of warnings
   */
  public int warning_count()
  {
    return ((Mysqli) validateConnection()).getWarningCount();
  }

  /**
   * Closes the connection
   */
  public boolean close(Env env)
  {
    return super.close(env);
  }

  /**
   * Creates a database-specific result.
   */
  protected JdbcResultResource createResult(Statement stmt,
                                            ResultSet rs)
  {
    return new MysqliResult(stmt, rs, this);
  }

  /**
   * This functions queries the connection with "SHOW WARNING"
   *
   * @return # of warnings
   */
  private int getWarningCount()
  {
    if (getWarnings() != null) {
      JdbcResultResource warningResult;
      warningResult = metaQuery("SHOW WARNINGS",getCatalog().toString());
      int warningCount = 0;

      if (warningResult != null) {
        warningCount =
          JdbcResultResource.getNumRows(warningResult.getResultSet());
      }

      if (warningCount >= 0)
        return warningCount;
      else
        return 0;
    } else
      return 0;
  }

  /**
   * Used by the
   * various mysqli functions to query the database
   * for metadata about the resultset which is
   * not in ResultSetMetaData.
   *
   * This function DOES NOT clear existing resultsets.
   */
  protected MysqliResult metaQuery(String sql,
                                   String catalog)
  {
    clearErrors();

    Value currentCatalog = getCatalog();

    try {
      getConnection().setCatalog(catalog);

      // need to create statement after setting catalog or
      // else statement will have wrong catalog
      Statement stmt = getConnection().createStatement();
      stmt.setEscapeProcessing(false);

      if (stmt.execute(sql)) {
        MysqliResult result = (MysqliResult) createResult(stmt, stmt.getResultSet());
        getConnection().setCatalog(currentCatalog.toString());
        return result;
      } else {
        getConnection().setCatalog(currentCatalog.toString());
        return null;
      }
    } catch (SQLException e) {
      saveErrors(e);
      log.log(Level.WARNING, e.toString(), e);
      return null;
    }
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
  private boolean moreResults()
  {
    return !_hasBeenUsed || _nextResultValue < _resultValues.size() - 1;
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
  private boolean multiQuery(String sql)
  {
    clearErrors();

    // Empty _resultValues on new call to query
    // But DO NOT close the individual result sets.
    // They may still be in use.
    _resultValues.clear();

    ArrayList<String> splitQuery = splitMultiQuery(sql);

    Statement stmt = null;

    try {
      setResultResource(null);

      for (String s : splitQuery) {
        stmt = getConnection().createStatement();
        stmt.setEscapeProcessing(false);
        if (stmt.execute(s)) {
          setAffectedRows(0);
          setResultResource(createResult(stmt, stmt.getResultSet()));
          _resultValues.add(getResultResource());
          setWarnings(stmt.getWarnings());
        } else {
          setAffectedRows(stmt.getUpdateCount());
          setWarnings(stmt.getWarnings());
        }
      }
    } catch (DataTruncation truncationError) {
      try {
        setAffectedRows(stmt.getUpdateCount());
        setWarnings(stmt.getWarnings());
      } catch (SQLException e) {
        saveErrors(e);
        log.log(Level.WARNING, e.toString(), e);
        return false;
      }
    } catch (SQLException e) {
      saveErrors(e);
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
   * prepares the next resultset from
   * a multi_query
   */
  private boolean nextResult()
  {
    if (_nextResultValue + 1 < _resultValues.size()) {
      _hasBeenUsed = false;
      _nextResultValue++;
      return true;
    } else
      return false;
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
   * returns the next jdbcResultValue
   */
  private JdbcResultResource storeResult()
  {
    if (!_hasBeenUsed) {
      _hasBeenUsed = true;

      return _resultValues.get(_nextResultValue);
    } else
      return null;
  }

  public String toString()
  {
    if (isConnected())
      return "Mysqli[" + get_host_name() + "]";
    else
      return "Mysqli[]";
  }
}
