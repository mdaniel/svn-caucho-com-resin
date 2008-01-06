/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.io.Closeable;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  private String _errorMessage = null;
  private int _errorCode;
  private boolean _fieldCount = false;
  private SQLWarning _warnings;

  private Env _env;
  protected String _host;
  private String _dbname;
  protected int _port;
  private String _userName;
  private String _password;
  protected String _driver;
  protected String _url;
  protected int _flags;
  protected String _socket;

  private boolean _isCatalogOptimEnabled = false;
  private boolean _isCloseOnClose = true;

  private boolean _isUsed;
  private boolean _isConnected;

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
    return _isConnected;
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
  final protected boolean connectInternal(Env env,
					  String host,
					  String userName,
					  String password,
					  String dbname,
					  int port,
					  String socket,
					  int flags,
					  String driver,
					  String url)
  {
    _host = host;
    _userName = userName;
    _password = password;
    _dbname = dbname;
    _port = port;
    _socket = socket;
    _flags = flags;
    _driver = driver;
    _url = url;

    Connection conn = connectImpl(env, host, userName, password,
				  dbname, port, socket, flags, driver, url);

    if (conn != null) {
      _conn = conn;
      
      _isConnected = true;

      _env.addClose(this);
    }

    return conn != null;
  }

  /**
   * Connects to the underlying database.
   */
  protected abstract Connection connectImpl(Env env,
					    String host,
					    String userName,
					    String password,
					    String dbname,
					    int port,
					    String socket,
					    int flags,
					    String driver,
					    String url);

  /**
   * Escape the given string for SQL statements.
   *
   * @param str a string
   * @return the string escaped for SQL statements
   */
  protected StringValue realEscapeString(StringValue str)
  {
    StringValue buf = _env.createUnicodeBuilder();

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
        return createResult(_env, _stmt, rs);
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
      return _env.createString(_conn.getCatalog());
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
   * Set encoding on the client side of the connection.
   * Return true if the encoding was set, otherwise false.
   */

  public boolean setClientEncoding(String encoding)
  {
    return true;
  }

  /**
   * Returns the client version
   * @deprecated
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
  public final Connection getConnection()
  {
    _isUsed = true;
    
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
    return _env.getQuercus().getConnection(_conn);
  }

  /**
   * Returns the data source.
   */
  public String getURL()
  {
    // return getJavaConnection().getURL();
    return _url;
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
    if (table == null || table.equals(""))
      return null;
    
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
    if (_isConnected) {
      _isConnected = false;

      // php/1418
      if (! _isUsed || _isCloseOnClose) {
	env.removeClose(this);
	close();
      }
    }

    return true;
  }

  public JdbcConnectionResource validateConnection()
  {
    if (! _isConnected) {
      throw _env.createErrorException(L.l("Connection is not properly initialized {0}\nDriver {1}",
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

    char ch;

    // clear table metadata cache if tables are deleted/altered
    if (sql != null) {
      int i = 0;
      int len = sql.length();
      
      while (i < len &&
          Character.isWhitespace(sql.charAt(i))) {
        i++;
      }
      
      if (i + 1 < len) {
        ch = sql.charAt(i);
        
        switch (ch) {
        case 'a': case 'A':
          // drop/alter clears metadata cache
          _tableMetadataMap.clear();
          break;
        case 'd': case 'D':
          if ((ch = sql.charAt(i + 1)) == 'r' || ch == 'R') {
            // drop/alter clears metadata cache
            _tableMetadataMap.clear();
          }
          break;
          /*
        case 'b': case 'B':
          // convert "begin" to begin
          // Test for mediawiki performance
          if (sql.equalsIgnoreCase("begin")) {
            setAutoCommit(false);
            return null;
          }
          break;
        case 'c': case 'C':
          // convert "commit" to begin
          if (sql.equalsIgnoreCase("commit")) {
            commit();
            setAutoCommit(true);
            return null;
          }
          break;
        case 'r': case 'R':
          // convert "rollback" to begin
          if (sql.equalsIgnoreCase("rollback")) {
            rollback();
            setAutoCommit(true);
            return null;
          }
          break;
          */
        }
      }
    }

    try {
      Connection conn = getConnection();

      // XXX: test for performance
      boolean canSeek = true;
      if (canSeek)
        stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                    ResultSet.CONCUR_READ_ONLY);
      else
        stmt = conn.createStatement();
        
      stmt.setEscapeProcessing(false); // php/1406

      if (stmt.execute(sql)) {
        ResultSet rs = stmt.getResultSet();
        _rs = createResult(_env, stmt, rs);
        _affectedRows = 0;

	// XXX: if these are needed, get them lazily for performance
        // _warnings = stmt.getWarnings();
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
	
	// XXX: if these are neede, get them lazily for performance
        // _warnings = stmt.getWarnings();

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
    } catch (IllegalStateException e) {
      // #2184, some drivers return this on closed connection
      saveErrors(new SQLExceptionWrapper(e));

      return null;
    }

    return _rs;
  }

  /**
   * Creates a database-specific result.
   */
  protected JdbcResultResource createResult(Env env,
					    Statement stmt,
                                            ResultSet rs)
  {
    return new JdbcResultResource(env, stmt, rs, this);
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
    if (name != null && name.equals(_dbname))
      return;
    
    clearErrors();

    if (! _isUsed && _isCatalogOptimEnabled) {
      // The database is only connected, but not used, reopen with
      // a real catalog
      
      Connection conn = _conn;
      _conn = null;
      _isConnected = false;
      
      if (conn != null)
	conn.close();

      _dbname = name;
      
      connectInternal(_env, 
		      _host,
		      _userName,
		      _password,
		      _dbname,
		      _port,
		      _socket,
		      _flags,
		      _driver,
		      _url);
    }
    else
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
    if (_errorMessage == null || "".equals(_errorMessage))
      _errorMessage = e.toString();
    
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

      if (_url != null)
        hash = 65537 * hash + _url.hashCode();

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

      if ((_url == null) != (key._url == null))
        return false;
      else if (_url != null && ! _url.equals(key._url))
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

