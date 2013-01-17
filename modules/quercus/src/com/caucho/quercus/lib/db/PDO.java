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

package com.caucho.quercus.lib.db;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvCleanup;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PDO object oriented API facade.
 */
public class PDO implements EnvCleanup {
  private static final Logger log = Logger.getLogger(PDO.class.getName());
  private static final L10N L = new L10N(PDO.class);

  public static final int ATTR_AUTOCOMMIT = 0;
  public static final int ATTR_PREFETCH = 1;
  public static final int ATTR_TIMEOUT = 2;
  public static final int ATTR_ERRMODE = 3;
  public static final int ATTR_SERVER_VERSION = 4;
  public static final int ATTR_CLIENT_VERSION = 5;
  public static final int ATTR_SERVER_INFO = 6;
  public static final int ATTR_CONNECTION_STATUS = 7;
  public static final int ATTR_CASE = 8;
  public static final int ATTR_CURSOR_NAME = 9;
  public static final int ATTR_CURSOR = 10;
  public static final int ATTR_ORACLE_NULLS = 11;
  public static final int ATTR_PERSISTENT = 12;
  public static final int ATTR_STATEMENT_CLASS = 13;
  public static final int ATTR_FETCH_TABLE_NAMES = 14;
  public static final int ATTR_FETCH_CATALOG_NAMES = 15;
  public static final int ATTR_DRIVER_NAME = 16;
  public static final int ATTR_STRINGIFY_FETCHES = 17;
  public static final int ATTR_MAX_COLUMN_LEN = 18;
  public static final int ATTR_DEFAULT_FETCH_MODE = 19;
  public static final int ATTR_EMULATE_PREPARES = 20;

  public static final int CASE_NATURAL = 0;
  public static final int CASE_UPPER = 1;
  public static final int CASE_LOWER = 2;

  public static final int CURSOR_FWDONLY = 0;
  public static final int CURSOR_SCROLL = 1;

  public static final String ERR_NONE = "00000";

  public static final int ERRMODE_SILENT = 0;
  public static final int ERRMODE_WARNING = 1;
  public static final int ERRMODE_EXCEPTION = 2;

  public static final int FETCH_LAZY = 1;
  public static final int FETCH_ASSOC = 2;
  public static final int FETCH_NUM = 3;
  public static final int FETCH_BOTH = 4;
  public static final int FETCH_OBJ = 5;
  public static final int FETCH_BOUND = 6;
  public static final int FETCH_COLUMN = 7;
  public static final int FETCH_CLASS = 8;
  public static final int FETCH_INTO = 9;
  public static final int FETCH_FUNC = 10;
  public static final int FETCH_NAMED = 11;
  public static final int FETCH_KEY_PAIR = 12;

  public static final int FETCH_GROUP = 0x00010000;
  public static final int FETCH_UNIQUE = 0x00030000;
  public static final int FETCH_CLASSTYPE = 0x00040000;
  public static final int FETCH_SERIALIZE = 0x00080000;

  public static final int FETCH_ORI_NEXT = 0;
  public static final int FETCH_ORI_PRIOR = 1;
  public static final int FETCH_ORI_FIRST = 2;
  public static final int FETCH_ORI_LAST = 3;
  public static final int FETCH_ORI_ABS = 4;
  public static final int FETCH_ORI_REL = 5;

  public static final int FETCH_PROPS_LATE = 1048576;

  public static final int MYSQL_ATTR_USE_BUFFERED_QUERY = 1000;

  public static final int NULL_NATURAL = 0;
  public static final int NULL_EMPTY_STRING = 1;
  public static final int NULL_TO_STRING = 2;

  public static final int PARAM_NULL = 0;
  public static final int PARAM_INT = 1;
  public static final int PARAM_STR = 2;
  public static final int PARAM_LOB = 3;
  public static final int PARAM_STMT = 4;
  public static final int PARAM_BOOL = 5;

  public static final int PARAM_EVT_ALLOC = 0;
  public static final int PARAM_EVT_EXEC_POST = 3;
  public static final int PARAM_EVT_EXEC_PRE = 2;
  public static final int PARAM_EVT_FETCH_POST = 5;
  public static final int PARAM_EVT_FETCH_PRE = 4;
  public static final int PARAM_EVT_FREE = 1;
  public static final int PARAM_EVT_NORMALIZE = 6;

  public static final int PARAM_INPUT_OUTPUT = 0x80000000;

  private final String _dsn;

  private JdbcConnectionResource _conn;

  private final PDOError _error;

  private PDOStatement _lastPDOStatement;
  private PDOStatement _lastExecutedStatement;

  private boolean _inTransaction;

  private String _statementClassName;
  private Value[] _statementClassArgs;

  private int _columnCase = JdbcResultResource.COLUMN_CASE_NATURAL;

  public PDO(Env env,
             String dsn,
             @Optional String user,
             @Optional String pass,
             @Optional @ReadOnly ArrayValue options)
  {
    _dsn = dsn;
    _error = new PDOError();

    if (options != null) {
      for (Map.Entry<Value,Value> entry : options.entrySet()) {
        setAttribute(env, entry.getKey().toInt(), entry.getValue());
      }
    }

    // XXX: following would be better as annotation on destroy() method
    env.addCleanup(this);

    try {
      JdbcConnectionResource conn = getConnection(env, dsn, user, pass);
      _conn = conn;

      if (conn == null) {
        env.warning(L.l("'{0}' is an unknown PDO data source.", dsn));
      }
    }
    catch (SQLException e) {
      env.warning(e.getMessage(), e);
      _error.error(env, e);
    }
  }

  protected JdbcConnectionResource getConnection()
  {
    return _conn;
  }

  protected boolean isConnected()
  {
    return _conn != null && _conn.isConnected();
  }

  protected void setLastExecutedStatement(PDOStatement stmt)
  {
    _lastExecutedStatement = stmt;
  }

  /**
   * Starts a transaction.
   */
  public boolean beginTransaction()
  {
    JdbcConnectionResource conn = getConnection();

    if (! isConnected())
      return false;

    if (_inTransaction) {
      return false;
    }

    _inTransaction = true;

    return conn.setAutoCommit(false);
  }

  private void closeStatements()
  {
    PDOStatement stmt = _lastPDOStatement;
    _lastPDOStatement = null;

    if (stmt != null) {
      stmt.close();
    }
  }

  /**
   * Commits a transaction.
   */
  public boolean commit()
  {
    JdbcConnectionResource conn = getConnection();

    if (conn == null) {
      return false;
    }

    if (! _inTransaction) {
      return false;
    }

    _inTransaction = false;

    conn.commit();
    return conn.setAutoCommit(true);
  }

  public void close()
  {
    cleanup();
  }

  /**
   * Implements the EnvCleanup interface.
   */
  public void cleanup()
  {
    JdbcConnectionResource conn = _conn;
    _conn = null;

    closeStatements();

    if (conn != null) {
      conn.close();
    }
  }

  public String errorCode()
  {
    return _error.getErrorCode();
  }

  public ArrayValue errorInfo()
  {
    return _error.getErrorInfo();
  }

  protected int getColumnCase()
  {
    return _columnCase;
  }

  /**
   * Executes a statement, returning the number of rows.
   */
  public Value exec(Env env, String query)
  {
    _error.clear();

    JdbcConnectionResource conn = getConnection();

    if (! conn.isConnected()) {
      return BooleanValue.FALSE;
    }

    try {
      PDOStatement stmt = new PDOStatement(env, this, _error, query, false, null, true);
      _lastExecutedStatement = stmt;

      return LongValue.create(conn.getAffectedRows());
    }
    catch (SQLException e) {
      _error.error(env, e);

      return BooleanValue.FALSE;
    }
  }

  public Value getAttribute(Env env, int attribute)
  {
    switch (attribute) {
      case ATTR_AUTOCOMMIT:
      {
        if (getAutocommit()) {
          return LongValue.ONE;
        }
        else {
          return LongValue.ZERO;
        }
      }
      case ATTR_CASE:
      {
        return LongValue.create(getCase());
      }
      case ATTR_CLIENT_VERSION:
      {
        String clientVersion = getConnection().getClientInfo(env);

        return env.createString(clientVersion);
      }
      case ATTR_CONNECTION_STATUS:
      {
        try {
          String hostInfo = getConnection().getHostInfo();

          return env.createString(hostInfo);
        }
        catch (SQLException e) {
          env.warning(e);

          return BooleanValue.FALSE;
        }
      }
      case ATTR_DRIVER_NAME:
      {
        String driverName = getConnection().getDriverName();

        return env.createString(driverName);
      }
      case ATTR_ERRMODE:
      {
        return LongValue.create(_error.getErrmode());
      }
      case ATTR_ORACLE_NULLS:
      {
        return LongValue.create(getOracleNulls());
      }
      case ATTR_PERSISTENT:
      {
        return BooleanValue.create(getPersistent());
      }
      case ATTR_PREFETCH:
      {
        return getPrefetch(env);
      }
      case ATTR_SERVER_INFO:
      {
        return getConnection().getServerStat(env);
      }
      case ATTR_SERVER_VERSION:
      {
        return getServerVersion(env);
      }
      case ATTR_TIMEOUT:
      {
        return getTimeout(env);
      }
      default:
        _error.unsupportedAttribute(env, attribute);
        // XXX: check what php does
        return BooleanValue.FALSE;
    }
  }

  public static ArrayValue getAvailableDrivers()
  {
    ArrayValue array = new ArrayValueImpl();

    array.put("mysql");
    array.put("pgsql");
    array.put("java");
    array.put("jdbc");
    array.put("sqlite");

    return array;
  }

  /**
   * Returns the auto commit value for the connection.
   */
  private boolean getAutocommit()
  {
    JdbcConnectionResource conn = getConnection();

    if (conn == null) {
      return true;
    }

    return conn.getAutoCommit();
  }

  public int getCase()
  {
    return _columnCase;
  }

  public int getOracleNulls()
  {
    return 0;
  }

  private boolean getPersistent()
  {
    return true;
  }

  private Value getPrefetch(Env env)
  {
    env.warning(L.l("driver does not support prefetch"));

    return BooleanValue.FALSE;
  }

  // XXX: might be int return
  private Value getServerVersion(Env env)
  {
    if (_conn == null) {
      return BooleanValue.FALSE;
    }

    try {
      String info = _conn.getServerInfo();

      return env.createString(info);
    }
    catch (SQLException e) {
      _error.error(env, e);

      return BooleanValue.FALSE;
    }
  }

  private Value getTimeout(Env env)
  {
    env.warning(L.l("Driver does not support timeouts"));

    return BooleanValue.FALSE;
  }

  public String lastInsertId(Env env, @Optional Value nameV)
  {
    if (! nameV.isDefault()) {
      throw new UnimplementedException("lastInsertId with name");
    }

    if (_lastExecutedStatement == null) {
      return "0";
    }

    try {
      String lastInsertId = _lastExecutedStatement.lastInsertId(env);

      if (lastInsertId == null) {
        return "0";
      }

      return lastInsertId;
    }
    catch (SQLException e) {
      _error.error(env, e);
      return "0";
    }
  }

  /**
   * Prepares a statement for execution.
   */
  public Value prepare(Env env,
                       String query,
                       @Optional ArrayValue driverOptions)
  {
    if (! isConnected()) {
      return BooleanValue.FALSE;
    }

    try {
      closeStatements();

      PDOStatement pdoStatement
        = new PDOStatement(env, this, _error, query, true, driverOptions, true);

      _lastPDOStatement = pdoStatement;

      if (_statementClassName != null) {
        QuercusClass cls = env.getClass(_statementClassName);

        Value phpObject = cls.callNew(env, pdoStatement, _statementClassArgs);

        return phpObject;
      }
      else {
        return env.wrapJava(pdoStatement);
      }
    }
    catch (SQLException e) {
      _error.error(env, e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Queries the database
   */
  public Value query(Env env,
                     String query,
                     @Optional int mode,
                     @Optional @ReadOnly Value[] args)
  {
    _error.clear();

    JdbcConnectionResource conn = getConnection();

    if (! conn.isConnected()) {
      return BooleanValue.FALSE;
    }

    try {
      closeStatements();

      PDOStatement pdoStatement
         = new PDOStatement(env, this, _error, query, false, null, true);

      if (mode != 0) {
        pdoStatement.setFetchMode(env, mode, args);
      }

      _lastPDOStatement = pdoStatement;

      if (_statementClassName != null) {
        QuercusClass cls = env.getClass(_statementClassName);

        return cls.callNew(env, pdoStatement, _statementClassArgs);
      }
      else {
        return env.wrapJava(pdoStatement);
      }
    }
    catch (SQLException e) {
      _error.error(env, e);

      return BooleanValue.FALSE;
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
   * Escapes the string.
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
   * Rolls a transaction back.
   */
  public boolean rollBack(Env env)
  {
    JdbcConnectionResource conn = getConnection();

    if (conn == null) {
      return false;
    }

    if (! _inTransaction) {
      return false;
    }

    _inTransaction = false;

    conn.rollback();
    return conn.setAutoCommit(true);
  }

  public boolean setAttribute(Env env, int attribute, Value value)
  {
    return setAttribute(env, attribute, value, false);
  }

  private boolean setAttribute(Env env,
                               int attribute, Value value, boolean isInit)
  {
    switch (attribute) {
      case ATTR_AUTOCOMMIT:
        return setAutocommit(env, value.toBoolean());

      case ATTR_ERRMODE:
        return _error.setErrmode(env, value.toInt());

      case ATTR_CASE:
        return setCase(env, value.toInt());

      case ATTR_ORACLE_NULLS:
        return setOracleNulls(env, value.toInt());

      case ATTR_STRINGIFY_FETCHES:
        return setStringifyFetches(value.toBoolean());

      case ATTR_STATEMENT_CLASS:
      {
        if (! value.isArray()) {
          env.warning(L.l("ATTR_STATEMENT_CLASS attribute must be an array"));

          return false;
        }

        return setStatementClass(env, value.toArrayValue(env));
      }
      case ATTR_EMULATE_PREPARES:
      {
        return true;
      }
    }

    if (isInit) {
      switch (attribute) {
        // XXX: there may be more of these
        case ATTR_TIMEOUT:
          return setTimeout(value.toInt());

        case ATTR_PERSISTENT:
          return setPersistent(value.toBoolean());
      }
    }

    // XXX: check what PHP does
    _error.unsupportedAttribute(env, attribute);
    return false;
  }

  /**
   * Sets the auto commit, if true commit every statement.
   * @return true on success, false on error.
   */
  private boolean setAutocommit(Env env, boolean autoCommit)
  {
    JdbcConnectionResource conn = getConnection();

    if (conn == null) {
      return false;
    }

    return conn.setAutoCommit(autoCommit);
  }

  /**
   * Force column names to a specific case.
   *
   * <dl>
   * <dt>{@link CASE_LOWER}
   * <dt>{@link CASE_NATURAL}
   * <dt>{@link CASE_UPPER}
   * </dl>
   */
  private boolean setCase(Env env, int value)
  {
    switch (value) {
      case CASE_LOWER:
        _columnCase = JdbcResultResource.COLUMN_CASE_LOWER;
        return true;

      case CASE_NATURAL:
        _columnCase = JdbcResultResource.COLUMN_CASE_NATURAL;
        return true;

      case CASE_UPPER:
        _columnCase = JdbcResultResource.COLUMN_CASE_UPPER;
        return true;

      default:
        _error.unsupportedAttributeValue(env, env);
        return false;
    }
  }

  /**
   * Sets whether or not the convert nulls and empty strings, works for
   * all drivers.
   *
   * <dl>
   * <dt> {@link NULL_NATURAL}
   * <dd> no conversion
   * <dt> {@link NULL_EMPTY_STRING}
   * <dd> empty string is converted to NULL
   * <dt> {@link NULL_TO_STRING} NULL
   * <dd> is converted to an empty string.
   * </dl>
   *
   * @return true on success, false on error.
   */
  private boolean setOracleNulls(Env env, int value)
  {
    switch (value) {
      case NULL_NATURAL:
      case NULL_EMPTY_STRING:
      case NULL_TO_STRING:
        throw new UnimplementedException();
      default:
        _error.warning(env, L.l("unknown value `{0}'", value));
        _error.unsupportedAttributeValue(env, value);
        return false;
    }
  }

  private boolean setPersistent(boolean isPersistent)
  {
    return true;
  }

  /**
   * Sets a custom statement  class derived from PDOStatement.
   *
   * @param value an array(classname, array(constructor args)).
   *
   * @return true on success, false on error.
   */
  private boolean setStatementClass(Env env, ArrayValue value)
  {
    Value className = value.get(LongValue.ZERO);

    if (! className.isString()) {
      return false;
    }

    _statementClassName = className.toStringValue(env).toString();

    Value argV = value.get(LongValue.ONE);

    if (argV.isArray()) {
      ArrayValue array = argV.toArrayValue(env);
      _statementClassArgs = array.valuesToArray();;
    }
    else {
      _statementClassArgs = Value.NULL_ARGS;
    }

    return true;
  }

  /**
   * Convert numeric values to strings when fetching.
   *
   * @return true on success, false on error.
   */
  private boolean setStringifyFetches(boolean stringifyFetches)
  {
    throw new UnimplementedException();
  }

  private boolean setTimeout(int timeoutSeconds)
  {
    throw new UnimplementedException();
  }

  /**
   * Opens a connection based on the dsn.
   */
  private JdbcConnectionResource getConnection(Env env,
                                               String dsn,
                                               String user,
                                               String pass)
    throws SQLException
  {
    if (dsn.startsWith("mysql:")) {
      return getMysqlConnection(env, dsn, user, pass);
    }
    else if (dsn.startsWith("pgsql:")) {
      return getPgsqlDataSource(env, dsn, user, pass);
    }
    else if (dsn.startsWith("java")) {
      return getJndiDataSource(env, dsn, user, pass);
    }
    else if (dsn.startsWith("jdbc:")) {
      return getJdbcDataSource(env, dsn, user, pass);
    }
    else if (dsn.startsWith("resin:")) {
      return getResinDataSource(env, dsn);
    }
    else if (dsn.startsWith("sqlite:")) {
      return getSqliteDataSource(env, dsn);
    }
    else {
      env.error(L.l("'{0}' is an unknown PDO data source.",
                    dsn));

      return null;
    }
  }

  /**
   * Opens a mysql connection based on the dsn.
   */
  private JdbcConnectionResource getMysqlConnection(Env env,
                                                    String dsn,
                                                    String user,
                                                    String pass)
    throws SQLException
  {
    HashMap<String,String> attrMap = parseAttr(dsn, dsn.indexOf(':'));

    String host = "localhost";
    int port = -1;
    String dbName = null;

    for (Map.Entry<String,String> entry : attrMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if ("host".equals(key)) {
        host = value;
      }
      else if ("port".equals(key)) {
        try {
          port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
          env.warning(e);
        }
      }
      else if ("dbname".equals(key)) {
        dbName = value;
      }
      else if ("user".equals(key)) {
        user = value;
      }
      else if ("password".equals(key)) {
        pass = value;
      }
      else {
        env.warning(L.l("pdo dsn attribute not supported: {0}={1}", key, value));
      }
    }

    // PHP doc does not sure user and password as attributes for mysql,
    // but in the pgsql doc  the dsn specified user and
    // password override arguments
    // passed to constructor

    String socket = null;
    int flags = 0;
    String driver = null;
    String url = null;
    boolean isNewLink = false;

    return new Mysqli(env, host, user, pass, dbName, port,
                      socket, flags, driver, url, isNewLink);
  }

  /**
   * Opens a postgres connection based on the dsn.
   */
  private JdbcConnectionResource getPgsqlDataSource(Env env,
                                                    String dsn,
                                                    String user,
                                                    String pass)
  {
    HashMap<String,String> attrMap = parseAttr(dsn, dsn.indexOf(':'));

    String host = "localhost";
    int port = -1;
    String dbName = null;

    for (Map.Entry<String,String> entry : attrMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if ("host".equals(key)) {
        host = value;
      }
      else if ("port".equals(key)) {
        try {
          port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
          env.warning(e);
        }
      }
      else if ("dbname".equals(key)) {
        dbName = value;
      }
      else if ("user".equals(key)) {
        user = value;
      }
      else if ("password".equals(key)) {
        pass = value;
      }
      else {
        env.warning(L.l("pdo dsn attribute not supported: {0}={1}", key, value));
      }
    }

    String driver = null;
    String url = null;

    Postgres postgres
      = new Postgres(env, host, user, pass, dbName, port, driver, url);

    return postgres;
  }

  /**
   * Opens a resin connection based on the dsn.
   */
  private JdbcConnectionResource getResinDataSource(Env env, String dsn)
  {
    try {
      String driver = "com.caucho.db.jdbc.ConnectionPoolDataSourceImpl";
      String url = "jdbc:" + dsn;

      DataSource ds = env.getDataSource(driver, url);

      return new DataSourceConnection(env, ds, null, null);
    }
    catch (Exception e) {
      env.warning(e);

      return null;
    }
  }

  /**
   * Opens a connection based on the dsn.
   */
  private JdbcConnectionResource getJndiDataSource(Env env,
                                                   String dsn,
                                                   String user,
                                                   String pass)
  {
    DataSource ds = null;

    try {
      Context ic = new InitialContext();

      ds = (DataSource) ic.lookup(dsn);
    } catch (NamingException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (ds == null) {
      env.error(L.l("'{0}' is an unknown PDO JNDI data source.", dsn));

      return null;
    }

    return new DataSourceConnection(env, ds, user, pass);
  }

  /**
   * Opens a connection based on the dsn.
   */
  private JdbcConnectionResource getJdbcDataSource(Env env,
                                                   String dsn,
                                                   String user,
                                                   String pass)
  {
    JdbcDriverContext context = env.getQuercus().getJdbcDriverContext();

    int i = dsn.indexOf("jdbc:");
    int j = dsn.indexOf("://", i + 5);

    if (j < 0) {
      j = dsn.indexOf(":", i + 5);
    }

    if (j < 0) {
      return null;
    }

    String protocol = dsn.substring(i + 5, j);

    if ("sqlite".equals(protocol)) {
      return new SQLite3(env, dsn);
    }

    String driver = context.getDriver(protocol);

    if (driver == null) {
      return null;
    }

    try {
      DataSource ds = env.getDataSource(driver, dsn.toString());

      return new DataSourceConnection(env, ds, user, pass);
    }
    catch (Exception e) {
      env.warning(e);

      return null;
    }
  }

  /**
   * Opens a resin connection based on the dsn.
   */
  private JdbcConnectionResource getSqliteDataSource(Env env, String dsn)
  {
    String jdbcUrl = "jdbc:" + dsn;

    return new SQLite3(env, jdbcUrl);
  }

  private HashMap<String,String> parseAttr(String dsn, int i)
  {
    HashMap<String,String> attr = new LinkedHashMap<String,String>();

    int length = dsn.length();

    StringBuilder sb = new StringBuilder();

    for (; i < length; i++) {
      char ch = dsn.charAt(i);

      if (! Character.isJavaIdentifierStart(ch))
        continue;

      for (;
           i < length && Character.isJavaIdentifierPart((ch = dsn.charAt(i)));
           i++) {
        sb.append(ch);
      }

      String name = sb.toString();
      sb.setLength(0);

      for (; i < length && ((ch = dsn.charAt(i)) == ' ' || ch == '='); i++) {
      }

      for (; i < length && (ch = dsn.charAt(i)) != ' ' && ch != ';'; i++) {
        sb.append(ch);
      }

      String value = sb.toString();
      sb.setLength(0);

      attr.put(name, value);
    }

    return attr;
  }

  public String toString()
  {
    // do not show password!
    if (_dsn == null)
      return "PDO[]";

    if (_dsn.indexOf("pass") < 0)
      return "PDO[" + _dsn + "]";

    int i = _dsn.lastIndexOf(':');

    if (i < 0)
      return "PDO[]";

    if (_dsn.startsWith("java"))
      return "PDO[" + _dsn + "]";

    StringBuilder str = new StringBuilder();
    str.append("PDO[");

    str.append(_dsn, 0, i + 1);

    HashMap<String,String> attr = parseAttr(_dsn, i);

    boolean first = true;

    for (Map.Entry<String,String> entry : attr.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if ("password".equalsIgnoreCase(key))
        value = "XXXXX";
      else if ("passwd".equalsIgnoreCase(key))
        value = "XXXXX";
      else if ("pass".equalsIgnoreCase(key))
        value = "XXXXX";

      if (! first)
        str.append(' ');

      first = false;

      str.append(key);
      str.append("=");
      str.append(value);
    }

    str.append("]");

    return str.toString();
  }
}
