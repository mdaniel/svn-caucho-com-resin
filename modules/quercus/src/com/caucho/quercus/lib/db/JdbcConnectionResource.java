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

import com.caucho.quercus.*;
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
  private String _userName;
  private String _password;
  private String _driver;
  private String _url;

  private boolean _connected;

  public JdbcConnectionResource(Env env)
  {
    _env = env;
  }

  /**
   * Returns the error string for the most recent function call
   */
  public String error()
  {
    if (isConnected())
      return getErrorMessage();
    else
      return null;
  }

  public boolean isConnected()
  {
    return _connected;
  }

  public Env getEnv()
  {
    return _env;
  }

  public String getHost()
  {
    return _host;
  }

  public String getUserName()
  {
    return _userName;
  }

  public String getPassword()
  {
    return _password;
  }

  public String getDbName()
  {
    return _dbname;
  }

  public int getPort()
  {
    return _port;
  }

  public String getDriver()
  {
    return _driver;
  }

  public String getUrl()
  {
    return _url;
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
  protected void setConnection(String host,
                               String userName,
                               String password,
                               String dbname,
                               int port,
                               Connection conn,
                               String driver,
                               String url)
  {
    _host = host;
    _userName = userName;
    _password = password;
    _dbname = dbname;
    _port = port;

    _conn = conn;

    _driver = driver;

    _url = url;

    if (conn != null) {
      _connected = true;

      _env.addClose(this);
    }
  }

  /**
   * Connects to the underlying database.
   */
  protected abstract boolean connectInternal(Env env,
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
   * Escape the given string for SQL statements.
   *
   * @param str a string
   * @return the string escaped for SQL statements
   */
  protected StringBuilderValue realEscapeString(StringValue str)
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
  protected JdbcResultResource getCatalogs()
  {
    clearErrors();

    try {
      if (_dmd == null)
        _dmd = _conn.getMetaData();

      ResultSet rs = _dmd.getCatalogs();

      if (rs != null)
        return createResult(_stmt, rs);
      else
        return null;
    } catch (SQLException e) {
      saveErrors(e);
      log.log(Level.WARNING, e.toString(), e);
      return null;
    }
  }

  /**
   * @return current catalog or false if error
   */
  protected Value getCatalog()
  {
    clearErrors();

    try {
      return new StringValueImpl(_conn.getCatalog());
    } catch (SQLException e) {
      saveErrors(e);
      log.log(Level.WARNING, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the client encoding.
   *
   * XXX: stubbed out. has to be revised once we
   * figure out what to do with character encoding
   */
  public String getCharacterSetName()
  {
    return "latin1";
  }

  /**
   * Alias for getCharacterSetName
   */
  public String getClientEncoding()
  {
    return getCharacterSetName();
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
  {
    if (_conn != null)
      return _conn;
    else if (_errorMessage != null)
      throw new QuercusModuleException(_errorMessage);
    else
      throw new QuercusModuleException(L.l("Connection is not available"));
  }

  /**
   * Returns the underlying SQL connection
   * associated to this statement.
   */
  protected Connection getJavaConnection()
    throws SQLException
  {
    return ((UserConnection) _conn).getConnection();
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
   * Closes the connection.
   */
  public boolean close(Env env)
  {
    if (_connected) {
      _connected = false;
      env.removeClose(this);
      close();
    }

    return true;
  }

  public JdbcConnectionResource validateConnection()
  {
    if (! _connected) {
      throw _env.errorException(L.l("Connection is not properly initialized {0}\nDriver {1}",
				    _url, _driver));
    }

    return this;
  }

  /**
   * Execute a single query.
   */
  protected JdbcResultResource realQuery(String sql)
  {
    clearErrors();

    _rs = null;

    Statement stmt = null;

    try {
      stmt = getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                   ResultSet.CONCUR_READ_ONLY);
      stmt.setEscapeProcessing(false); // php/1406
      
      if (stmt.execute(sql)) {
        ResultSet rs = stmt.getResultSet();
        _rs = createResult(stmt, rs);
        _affectedRows = 0;
        _warnings = stmt.getWarnings();
      } else {
        // php/430a should return a result set
        // for update statements. It is always
        // null though. So keep the stmt for
        // future reference (PostgresModule.pg_last_oid)

        // php/1f33

        // This is overriden in Postgres.java
        keepResourceValues(stmt);

        _affectedRows = 0;
        _affectedRows = stmt.getUpdateCount();
        if (_rs != null)
          _rs.setAffectedRows(_affectedRows);
        _warnings = stmt.getWarnings();

        // for php/430a
        if (!keepStatementOpen()) {
          stmt.close();
        }
      }
    } catch (DataTruncation truncationError) {
      try {
        _affectedRows = stmt.getUpdateCount();
        _warnings = stmt.getWarnings();
      } catch (SQLException e) {
        saveErrors(e);
        log.log(Level.WARNING, e.toString(), e);
        return null;
      }
    } catch (SQLException e) {

      saveErrors(e);

      // php/431h
      if (keepStatementOpen()) {
        keepResourceValues(stmt);
      } else {
        log.log(Level.WARNING, e.toString(), e);
        return null;
      }
    }

    return _rs;
  }

  /**
   * Creates a database-specific result.
   */
  protected JdbcResultResource createResult(Statement stmt,
                                            ResultSet rs)
  {
    return new JdbcResultResource(stmt, rs, this);
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
      saveErrors(e);
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
      saveErrors(e);
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
      saveErrors(e);
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
   * This function is overriden in Postgres to keep
   * result set references for php/430a (see also php/1f33)
   */
  protected void keepResourceValues(Statement stmt)
  {
    return;
  }

  /**
   * This function is overriden in Postgres to keep
   * statement references for php/430a
   */
  protected boolean keepStatementOpen()
  {
    return false;
  }

  /**
   * Get the current result resource
   */
  protected JdbcResultResource getResultResource()
  {
    return _rs;
  }

  /**
   * Set the current result resource
   */
  protected void setResultResource(JdbcResultResource rs)
  {
    _rs = rs;
  }

  /**
   * This function was added for PostgreSQL pg_last_notice
   *
   * @return warning messages
   */
  protected SQLWarning getWarnings()
  {
    return _warnings;
  }

  /**
   * Pings the database
   */
  public boolean ping()
  {
    try {

      return isConnected() && ! getConnection().isClosed();

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * Set the current SQL warnings.
   *
   * @param warnings the new SQL warnings
   */
  protected void setWarnings(SQLWarning warnings)
  {
    _warnings = warnings;
  }

  protected void clearErrors()
  {
    _errorMessage = null;
    _errorCode = 0;
    _warnings = null;
  }

  protected void saveErrors(SQLException e)
  {
    _errorMessage = e.getMessage();
    _errorCode = e.getErrorCode();
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

