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
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvCleanup;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JDBC Connection value.
 */
public abstract class JdbcConnectionResource
    implements EnvCleanup
{
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

  protected boolean _connectionLog = false;

  protected SqlParseToken _sqlParseToken = new SqlParseToken();

  public JdbcConnectionResource(Env env)
  {
    _env = env;
  }

  /**
   * Returns the error string for the most recent function call.
   * This method is not invoked from PHP code.
   */
  public StringValue error(Env env)
  {
    if (isConnected())
      return env.createString(getErrorMessage());
    else
      return env.createEmptyString();
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
    return getCatalog().toString();
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
   * Invoke this method to indicate that connection
   * lifetime logging should be enabled. This method
   * is used for regression testing connection lifetime.
   */  

  public void enableConnectionLog()
  {
    _connectionLog = true;
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

      _env.addCleanup(this);
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
      log.log(Level.FINEST, e.toString(), e);
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
      log.log(Level.FINEST, e.toString(), e);
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
  public final Connection getConnection(Env env)
  {
    _isUsed = true;
    
    if (_conn != null)
      return _conn;
    else if (_errorMessage != null) {
      env.warning(_errorMessage);
      return null;
    }
    else {
      env.warning(L.l("Connection is not available"));
      return null;
    }
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
    try {
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
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
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
    if (_connectionLog)
      log.log(Level.FINER, "close()");

    if (_isConnected) {
      _isConnected = false;

      // php/1418
      if (! _isUsed || _isCloseOnClose) {
        env.removeCleanup(this);
        cleanup();
      }
    }

    return true;
  }

  /**
   * Implements the EnvCleanup interface. This method
   * will deallocate resources associated with this
   * connection. This method can be invoked via a
   * call to close(), or it can be invoked when the
   * environment is being cleaned up after a quercus
   * request has been processed.
   */
  public void cleanup()
  {
    if (_connectionLog)
      log.log(Level.FINER, "cleanup()");

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

      if (conn != null) {
        conn.close();
      }
      
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);
    }
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
  protected Value realQuery(Env env, String sql)
  {
    clearErrors();

    _rs = null;

    Statement stmt = null;

    try {
      Connection conn = getConnection(env);
      
      if (conn == null)
        return BooleanValue.FALSE;

      checkSql(conn, sql);
      
      // XXX: test for performance
      boolean canSeek = true;
      if (canSeek)
        stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                    ResultSet.CONCUR_READ_ONLY);
      else
        stmt = conn.createStatement();

      stmt.setEscapeProcessing(false); // php/1406

      if (stmt.execute(sql)) {
        // Statement.execute(String) returns true when SQL statement is a
        // SELECT statement that returns a result set.

        ResultSet rs = stmt.getResultSet();
        _rs = createResult(_env, stmt, rs);
        _affectedRows = 0;

	// XXX: if these are needed, get them lazily for performance
        // _warnings = stmt.getWarnings();
      } else {
        // Statement.execute(String) returns false when SQL statement does
        // not returns a result set (UPDATE, INSERT, DELETE, or REPLACE).

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
        if (! keepStatementOpen()) {
          stmt.close();
        }
      }
    } catch (DataTruncation truncationError) {
      try {
        _affectedRows = stmt.getUpdateCount();
        _warnings = stmt.getWarnings();
      } catch (SQLException e) {
        saveErrors(e);
        log.log(Level.FINEST, e.toString(), e);
        return BooleanValue.FALSE;
      }
    } catch (SQLException e) {
      saveErrors(e);

      // php/431h
      if (keepStatementOpen()) {
        keepResourceValues(stmt);
      } else {
        log.log(Level.FINEST, e.toString(), e);
        return BooleanValue.FALSE;
      }
    } catch (IllegalStateException e) {
      // #2184, some drivers return this on closed connection
      saveErrors(new SQLExceptionWrapper(e));

      return BooleanValue.FALSE;
    }

    if (_rs == null)
      return BooleanValue.TRUE;
    
    return env.wrapJava(_rs);
  }

  private void checkSql(Connection conn, String sql)
  {
    SqlParseToken tok = parseSqlToken(sql, null);

    if (tok == null)
      return;

    switch (tok.getFirstChar()) {
      case 'a': case 'A': {
        // drop/alter clears metadata cache
        _tableMetadataMap.clear();
        break;
      }
      case 'd': case 'D': {
        if (tok.matchesToken("DROP")) {
          // drop/alter clears metadata cache
          _tableMetadataMap.clear();

          // If DROP is dropping the current database, then clear
          // the cached database name in the driver.
          //
          // php/144a

          tok = parseSqlToken(sql, tok);

          if ((tok != null) && tok.matchesToken("DATABASE")) {
            tok = parseSqlToken(sql, tok);

            if (tok != null) {
              String dbname = tok.toUnquotedString();

              if (getCatalog().toString().equals(dbname)) {
                try {
                  setCatalog(null);
                } catch (SQLException e) {
                  log.log(Level.FINEST, e.toString(), e);
                }
              }
            }
          }
        }
        break;
      }
      case 'c': case 'C': {
        if (tok.matchesToken("CREATE")) {
          // don't pool connections that create tables
          _env.getQuercus().markForPoolRemoval(conn);
        }
        break;
      }
/*
      case 'b': case 'B': {
        if (tok.matchesToken("BEGIN")) {
          // Test for mediawiki performance
          setAutoCommit(false);
        }
        break;
      }
      case 'c': case 'C': {
        if (tok.matchesToken("COMMIT")) {
          commit();
          setAutoCommit(true);
        }
        break;
      }
      case 'r': case 'R': {
        if (tok.matchesToken("ROLLBACK")) {
          rollback();
          setAutoCommit(true);
        }
        break;
      }
*/
    }
  }

  /**
   * Parse a token from a string containing a SQL statement.
   * If the prevToken is null, then the first token in parsed.
   * If a SQL token can't be found in the string, then null
   * is returned. If a SQL token is found, data is captured in
   * the returned SqlParseToken result.
   */
  protected SqlParseToken parseSqlToken(String sql, SqlParseToken prevToken)
  {
    if (sql == null) {
      _sqlParseToken.init();
      return null;
    }

    final int len = sql.length();
    int i, start;

    // Start at index 0, or where we left off last time

    if (prevToken == null)
      i = 0;
    else
      i = prevToken._end;

    while (i < len &&
        Character.isWhitespace(sql.charAt(i))) {
      i++;
    }

    // Must be at least 1 non-whitespace character

    if ((i + 1) >= len) {
      _sqlParseToken.init();
      return null;
    }

    start = i;

    while (i < len && !Character.isWhitespace(sql.charAt(i))) {
      i++;
    }

    _sqlParseToken.assign(sql, start, i);

    return _sqlParseToken;
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
      log.log(Level.FINEST, e.toString(), e);
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
      log.log(Level.FINEST, e.toString(), e);
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
      log.log(Level.FINEST, e.toString(), e);
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

    if (! _isUsed && _isCatalogOptimEnabled) {
      // The database is only connected, but not used, reopen with
      // a real catalog

      Connection conn = _conn;
      _conn = null;
      _isConnected = false;

      if (conn != null)
        conn.close();

      connectInternal(_env,
		      _host,
		      _userName,
		      _password,
		      name,
		      _port,
		      _socket,
		      _flags,
		      _driver,
		      _url);
    }
    else {
      _conn.setCatalog(name);
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
  public boolean ping(Env env)
  {
    try {

      return isConnected() && ! getConnection(env).isClosed();

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      env.warning(e.toString(), e);
      
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

  /*
   * This class enables efficient parsing of a SQL token from
   * a String. An SQL statement can be parsed one token at a
   * time. One can efficiently check that first letter of
   * the parse token via matchesFirstChar() without creating
   * a substring from the original.
   */

  protected static class SqlParseToken {
    private String _query;
    private String _token;
    private int _start;
    private int _end;
    private char _firstChar;

    public void init()
    {
      _query = null;
      _token = null;
      _start = -1;
      _end = -1;
      _firstChar = '\0';
    }

    public void assign(String query, int start, int end)
    {
      _query = query;
      _token = null;
      _start = start;
      _end = end;
      _firstChar = query.charAt(start);
    }

    public boolean matchesFirstChar(char upper, char lower)
    {
      return (_firstChar == upper) || (_firstChar == lower);
    }

    public char getFirstChar()
    {
      return _firstChar;
    }

    // Case insensitive compare of token string

    public boolean matchesToken(String token)
    {
      if (_token == null)
        _token = _query.substring(_start, _end);

      return _token.equalsIgnoreCase(token);
    }

    public String toString()
    {
      if (_token == null)
        _token = _query.substring(_start, _end);
      
      return _token;
    }

    /*
     * Return the SQL token as a string. If the token
     * is back quoted, then remove the back quotes and
     * return the string inside.
     */

    public String toUnquotedString()
    {
      String tok = toString();

      // Extract database name if back quoted : "DROP DATABASE `DBNAME`"

      if (tok.length() >= 2 &&
        tok.charAt(0) == '`' &&
        tok.charAt(tok.length() - 1) == '`') {
        tok = tok.substring(1, tok.length() - 1);
      }

      return tok;
    }
  }
}

