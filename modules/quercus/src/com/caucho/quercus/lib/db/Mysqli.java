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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ConnectionEntry;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;
import com.caucho.util.SQLExceptionWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * mysqli object oriented API facade
 */
public class Mysqli extends JdbcConnectionResource
{
  private static final Logger log = Logger.getLogger(Mysqli.class.getName());
  private static final L10N L = new L10N(Mysqli.class);

  private static MysqlMetaDataMethod _lastMetaDataMethod;

  private StringValue _charset;
  private StringValue _collation;

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

  private boolean _isPersistent;

  private LastSqlType _lastSql;

  private MysqlMetaDataMethod _metaDataMethod;

  /**
    * This is the constructor for the mysqli class.
    * It can be invoked by PHP or and by Java code.
    */

  public Mysqli(Env env,
                @Optional("localhost") StringValue host,
                @Optional StringValue user,
                @Optional StringValue password,
                @Optional String db,
                @Optional("-1") int port,
                @Optional StringValue socket)
  {
    super(env);

    connectInternal(env, host.toString(), user.toString(), password.toString(),
                    db, port, socket.toString(),
                    0, null, null, true);
  }

  /**
   * This constructor can only be invoked by other method
   * implementations in the mysql and mysqli modules. It
   * accepts String arguments and supports additional
   * arguments not available in the mysqli constructor.
   */

  Mysqli(Env env, String host, String user, String pass, String db, int port,
         String socket, int flags, String driver, String url, boolean isNewLink)
  {
    super(env);

    if (host == null || host.length() == 0)
      host = "localhost";

    connectInternal(env, host, user, pass, db, port, socket,
                    flags, driver, url, isNewLink);
  }

  protected Mysqli(Env env)
  {
    super(env);
  }

  public boolean isLastSqlDescribe()
  {
    return _lastSql == LastSqlType.DESCRIBE;
  }

  /**
   * Connects to the underlying database.
   */
  @Override
  protected ConnectionEntry connectImpl(Env env,
                                        String host,
                                        String user,
                                        String pass,
                                        String dbname,
                                        int port,
                                        String socket,
                                        int flags,
                                        String driver,
                                        String url,
                                        boolean isNewLink)
  {
    if (isConnected()) {
      env.warning(L.l("Connection is already opened to '{0}'", this));
      return null;
    }

    JdbcDriverContext driverContext = env.getQuercus().getJdbcDriverContext();

    try {
      if (host == null || host.equals("")) {
        host = "localhost";
      }
      else if (host.startsWith("jdbc:")) {
        int slashPos = host.indexOf("://");

        if (slashPos > 5) {
          String protocol = host.substring(5, slashPos);

          driver = driverContext.getDriver(protocol);

          if (driver != null) {
            url = host;
          }
        }
      }

      if (driver == null || driver.equals("")) {
        if (env.getIniBoolean("quercus-mysql-driver")) {
          driver = "com.caucho.quercus.mysql.QuercusMysqlDriver";
        }
        else {
          driver = driverContext.getDefaultDriver();
        }
      }

      if (url == null || url.length() == 0) {
        url = getUrl(env, host, port, dbname, driverContext.getDefaultEncoding(),
                     (flags & MysqliModule.MYSQL_CLIENT_INTERACTIVE) != 0,
                     (flags & MysqliModule.MYSQL_CLIENT_COMPRESS) != 0,
                     (flags & MysqliModule.MYSQL_CLIENT_SSL) != 0);
      }

      ConnectionEntry jConn
        = env.getConnection(driver, url, user, pass, ! isNewLink);

      Connection conn = jConn.getConnection();

      _driver = driver;

      return jConn;
    } catch (SQLException e) {
      env.warning(L.l("A link to the server could not be established.\n  "
                      + "url={0}\n  driver={1}\n  {2}", url, driver, e.toString()), e);

      env.setSpecialValue("mysqli.connectErrno", LongValue.create(e.getErrorCode()));
      env.setSpecialValue("mysqli.connectError", env.createString(e.getMessage()));

      return null;
    } catch (Exception e) {
      env.warning(L.l("A link to the server could not be established.\n  url={0}\n  "
                      + "driver={1}\n  {2}", url, driver, e.toString()), e);
      env.setSpecialValue("mysqli.connectError", env.createString(e.toString()));

      return null;
    }
  }

  protected static String getUrl(Env env,
                                 String host,
                                 int port,
                                 String dbname,
                                 String encoding,
                                 boolean useInteractive,
                                 boolean useCompression,
                                 boolean useSsl)
  {
    StringBuilder urlBuilder = new StringBuilder();

    JdbcDriverContext driverContext = env.getQuercus().getJdbcDriverContext();
    String jdbcUrlPrefix = driverContext.getDefaultUrlPrefix();

    urlBuilder.append(jdbcUrlPrefix);
    urlBuilder.append(host);

    if (port > 0) {
      urlBuilder.append(":");
      urlBuilder.append(port);
    }

    urlBuilder.append("/");

    if (dbname.length() > 0) {
      urlBuilder.append(dbname);
    }

    // Ignore MYSQL_CLIENT_LOCAL_FILES and MYSQL_CLIENT_IGNORE_SPACE flags.

    if (useInteractive) {
      char sep = (urlBuilder.indexOf("?") < 0) ? '?' : '&';

      urlBuilder.append(sep);
      urlBuilder.append("interactiveClient=true");
    }

    if (useCompression) {
      char sep = (urlBuilder.indexOf("?") < 0) ? '?' : '&';

      urlBuilder.append(sep);
      urlBuilder.append("useCompression=true");
    }

    if (useSsl) {
      char sep = (urlBuilder.indexOf("?") < 0) ? '?' : '&';

      urlBuilder.append(sep);
      urlBuilder.append("useSSL=true");
    }

    {
      char sep = (urlBuilder.indexOf("?") < 0) ? '?' : '&';
      urlBuilder.append(sep);
      urlBuilder.append("jdbcCompliantTruncation=false");
    }

    {
      char sep = (urlBuilder.indexOf("?") < 0) ? '?' : '&';

      // this sends a "SET character_set_results = latin1" query, thereby
      // telling the server to do all the result set encoding on the server-side
      // (e.g. the raw wire bytes will be in the character_set_client encoding)
      urlBuilder.append(sep);
      urlBuilder.append("characterSetResults=ISO8859_1");

      // this sets the encoding that the jdbc driver uses to encode the queries,
      // ISO8859_1 matches PHP's behavior, whereas the JDBC driver maps latin1
      // to cp1252 and cp1252 doesn't translate to utf-8 properly in Java
      urlBuilder.append('&');
      urlBuilder.append("characterEncoding=ISO8859_1");
    }

    //urlBuilder.append("&useInformationSchema=true");

    // required to get the result table name alias,
    // doesn't work in mysql JDBC 5.1.6, but set it anyways in case
    // the mysql guys fix it
    //
    // php/141p
    //urlBuilder.append("&useOldAliasMetadataBehavior=true");

    return urlBuilder.toString();
  }

  /**
   * Quercus function to get the field 'affected_rows'.
   */

  public int getaffected_rows(Env env)
  {
    return affected_rows(env);
  }

  /**
   * returns the number of affected rows.
   */
  public int affected_rows(Env env)
  {
    return validateConnection(env).getAffectedRows();
  }

  /**
   * sets the autocommit mode
   */
  public boolean autocommit(Env env, boolean isAutoCommit)
  {
    return validateConnection(env).setAutoCommit(isAutoCommit);
  }

  /**
   * Changes the user and database
   *
   * @param user the new user
   * @param password the new password
   * @param db the new database
   */
  public boolean change_user(Env env,
                             String user,
                             String password,
                             String db)
  {
    try {
      if (isConnected()) {
        Connection conn = getJavaConnection(env);

        Class<?> cls = conn.getClass();

        Method method = cls.getMethod("changeUser", String.class, String.class);

        if (method != null) {
          method.invoke(conn, user, password);

          select_db(env, db);

          return true;
        }
      }
    } catch (NoSuchMethodException e) {
      throw new QuercusException(e);
    } catch (InvocationTargetException e) {
      throw new QuercusException(e);
    } catch (IllegalAccessException e) {
      throw new QuercusException(e);
    } catch (SQLException e) {
      env.warning(L.l("unable to change user to '{0}'", user));

      return false;
    }

    // XXX: Docs for mysqli_change_user indicate that
    // if new user authorization fails,
    // then the existing user perms are retained.

    close();

    return connectInternal(env, _host, user, password,
                           db, _port, _socket, _flags, _driver, _url, false);
  }

  /**
   * Returns the client encoding.
   *
   * XXX: stubbed out. has to be revised once we
   * figure out what to do with character encoding
   */
  public StringValue character_set_name(Env env)
  {
    return env.createString(getCharacterSetName());
  }

  /**
   * Alias for character_set_name
   */
  public StringValue client_encoding(Env env)
  {
    return character_set_name(env);
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
  public StringValue geterror(Env env)
  {
    return error(env);
  }

  /**
   * Escapes the string
   */
  public StringValue escape_string(Env env, StringValue str)
  {
    return real_escape_string(env, str);
  }

  /**
   * Quercus function to get the field 'client_info'.
   */
  public String getclient_info(Env env)
  {
    String version = getClientInfo(env);

    return version;
  }

  protected static String getClientInfoStatic(Env env)
  {
    QuercusContext quercus = env.getQuercus();

    String version = quercus.getMysqlVersion();

    if (version != null) {
      // php/1f2h

      // Initialized to a specific version via:
      // <init mysql-version="X.X.X">
    } else {
      try {
        JdbcDriverContext jdbcDriverContext = quercus.getJdbcDriverContext();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> cls = loader.loadClass(jdbcDriverContext.getDefaultDriver());

        Driver driver = (Driver) cls.newInstance();

        version = driver.getMajorVersion() + "."
                  + driver.getMinorVersion() + ".00";
      }
      catch (Exception e) {
        log.log(Level.FINE, e.getMessage(), e);
      }
    }

    if (version == null) {
      version = "0.00.00";
    }

    return version;
  }

  @Override
  protected String getClientInfo(Env env)
  {
    return getClientInfoStatic(env);
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
  public Value get_dbname(Env env)
  {
    return env.createString(getCatalog());
  }

  /**
   * Quercus function to get the field 'host_info'.
   */
  public StringValue gethost_info(Env env)
  {
    return get_host_info(env);
  }

  /**
   * Returns the host information.
   */
  public StringValue get_host_info(Env env)
  {
    return env.createString(getHost() + " via TCP socket");
  }

  /**
   * Returns the host name.
   */
  public StringValue get_host_name(Env env)
  {
    return env.createString(getHost());
  }

  /**
   * Quercus function to get the field 'info'.
   */
  public Value getinfo(Env env)
  {
    return info(env);
  }

  /**
   * Return info string about the most recently executed
   * query. Documentation for mysql_info() indicates that
   * only some kinds of INSERT, UPDATE, LOAD, and ALTER
   * statements return results. A SELECT statement always
   * returns FALSE. The ConnectorJ module should provide a
   * way to get this result string since it is read from
   * the server, but that is not supported. This function
   * errors on the side of returning more results than
   * it should since it is an acceptable compromise.
   */

  Value info(Env env)
  {
    if (getResultResource() != null) {
      // Last SQL statement was a SELECT

      return BooleanValue.FALSE;
    }

    // INSERT result:      "Records: 23 Duplicates: 0 Warnings: 0"
    // LOAD result:        "Records: 42 Deleted: 0 Skipped: 0 Warnings: 0"
    // ALTER TABLE result: "Records: 60 Duplicates: 0 Warnings: 0"
    // UPDATE result:      "Rows matched: 1  Changed: 1  Warnings: 0"

    StringValue sb = env.createStringBuilder();

    int matched = affected_rows(env);
    int changed = matched;
    int duplicates = 0;
    int warnings = 0;

    SQLWarning warning = getWarnings();
    while (warning != null) {
      warning = warning.getNextWarning();
      warnings++;
    }

    if (_lastSql == LastSqlType.UPDATE)
      sb.append("Rows matched: ");
    else
      sb.append("Records: ");

    sb.append(matched);

    if (_lastSql == LastSqlType.UPDATE) {
      sb.append("  Changed: "); // PHP adds 2 spaces before Changed:
      sb.append(changed);
    } else {
      sb.append(" Duplicates: ");
      sb.append(duplicates);
    }

    if (_lastSql == LastSqlType.UPDATE)
      sb.append("  Warnings: "); // Only update has 2 spaces here
    else
      sb.append(" Warnings: ");

    sb.append(warnings);

    return sb;
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
  public StringValue getserver_info(Env env)
  {
    return get_server_info(env);
  }

  /**
   * Returns the server information.
   */
  public StringValue get_server_info(Env env)
  {
    String version = env.getQuercus().getMysqlVersion();

    if (version != null)
      return env.createString(version);

    try {
      return env.createString(validateConnection(env).getServerInfo());
    } catch (SQLException e) {
      return env.getEmptyString();
    }
  }

  /**
   * Quercus function to get the field 'server_version'.
   */
  public int getserver_version(Env env)
  {
    return get_server_version(env);
  }

  /**
   * Returns the server information.
   */
  public int get_server_version(Env env)
  {
    try {
      String info = validateConnection(env).getServerInfo();

      return infoToVersion(info);
    } catch (SQLException e) {
      env.warning(e);

      return 0;
    }
  }

  /**
   * Quercus function to get the field 'field_count'.
   */
  public int getfield_count(Env env)
  {
    return field_count(env);
  }

  /**
   * Returns the number of columns in the last query.
   */
  public int field_count(Env env)
  {
    return validateConnection(env).getFieldCount();
  }

  /**
   * Quercus function to get the field 'insert_id'.
   */
  public Value getinsert_id(Env env)
  {
    return insert_id(env);
  }

  /**
   * returns ID generated for an AUTO_INCREMENT column by the previous
   * INSERT query on success, 0 if the previous query does not generate
   * an AUTO_INCREMENT value, or FALSE if no MySQL connection was established
   *
   */
  public Value insert_id(Env env)
  {
    try {
      JdbcConnectionResource connV = validateConnection(env);
      Connection conn = connV.getConnection(env);

      if (conn == null)
        return BooleanValue.FALSE;

      Statement stmt = null;

      try {
        stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT @@identity");

        if (rs.next())
          return LongValue.create(rs.getLong(1));
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

  @ReturnNullAsFalse
  public JdbcResultResource list_dbs(Env env)
  {
    return validateConnection(env).getCatalogs(env);
  }

  /**
   * Check for more results in a multi-query
   */
  public boolean more_results(Env env)
  {
    return ((Mysqli) validateConnection(env)).moreResults();
  }

  /**
   * executes one or multiple queries which are
   * concatenated by a semicolon.
   */
  public boolean multi_query(Env env, StringValue query)
  {
    return ((Mysqli) validateConnection(env)).multiQuery(env, query);
  }

  /**
   * prepares next result set from a previous call to
   * mysqli_multi_query
   */
  public boolean next_result(Env env)
  {
    return ((Mysqli) validateConnection(env)).nextResult();
  }

  /**
   * Sets a mysqli option.
   */
  public boolean options(int option, Value value)
  {
    return false;
  }

  /**
   * Executes a query.
   *
   * @param env the PHP executing environment
   * @param sql the escaped query string (can contain
   * escape sequences like `\n' and `\Z')
   * @param resultMode ignored
   *
   * @return a {@link JdbcResultResource}, or null for failure
   */
  public Value query(Env env,
                     StringValue sqlV,
                     @Optional("MYSQLI_STORE_RESULT") int resultMode)
  {
    String sql = toBinarySafeString(sqlV);

    return realQuery(env, sql);
  }

  private static final String toBinarySafeString(StringValue str)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);

      if (ch > 0x7f) {
        // hack to prevent the JDBC driver from doing any decoding/encoding on this char
        sb.append('\\');
      }

      sb.append(ch);
    }

    return sb.toString();
  }

  /*
  private static final String toBinarySafeString2(StringValue str)
  {
    int len = str.length();
    boolean isBinary = false;

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if (ch > 0x7f) {
        isBinary = true;
        break;
      }
    }

    if (! isBinary) {
      return str.toString();
    }

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if (ch == '\'' || ch == '"' || ch == '`') {
        i = appendString(sb, str, len, i + 1, ch);
      }
      else {
        sb.append(ch);
      }
    }

    return sb.toString();
  }

  private static final int appendString(StringBuilder sb, StringValue str,
                                        int length, int index, char quoteChar)
  {
    int start = index;
    boolean isBinary = false;

    for (; index < length; index++) {
      char ch = str.charAt(index);

      if (ch > 0x7f) {
        isBinary = true;
      }
      else if (ch == '\\' && index + 1 < length) {
        char ch2 = str.charAt(index + 1);
        index++;

        if (ch2 > 0x7f) {
          isBinary = true;
        }
      }
      else if (ch == quoteChar) {
        break;
      }
    }

    int end = index;

    if (! isBinary) {
      sb.append(quoteChar);
      sb.append(str, start, end);
      sb.append(quoteChar);
    }
    else {
      sb.append("0x");

      for (int i = start; i < end; i++) {
        char ch = str.charAt(i);

        if (ch == '\\' && i + 1 < end) {
          char ch2 = str.charAt(i + 1);
          i++;

          switch (ch2) {
            case '0':
              sb.append('0');
              sb.append('0');
              break;
            case 'r':
              Hex.appendHex(sb, (byte) '\r');
              break;
            case 'n':
              Hex.appendHex(sb, (byte) '\n');
              break;
            case '\\':
              Hex.appendHex(sb, (byte) '\\');
              break;
            case 'a':
              sb.append('1');
              sb.append('a');
              break;
            case '"':
              Hex.appendHex(sb, (byte) '"');
              break;
            case '\'':
              Hex.appendHex(sb, (byte) '\'');
              break;
            default:
              Hex.appendHex(sb, (byte) '\\');
              Hex.appendHex(sb, (byte) ch2);
          }
        }
        else {
          Hex.appendHex(sb, (byte) ch);
        }
      }
    }

    return end;
  }
  */

  /**
   * Intercept Mysql specific query before sending to JDBC driver
   * to handle any special cases.
   */

  @Override
  protected Value realQuery(Env env, String sql)
  {
    clearErrors();

    _lastSql = null;

    setResultResource(null);

    if (log.isLoggable(Level.FINE))
      log.fine("mysql_query(" + sql + ")");

    try {
      // Check for valid conneciton

      Connection conn = getConnection(env);

      if (conn == null)
        return BooleanValue.FALSE;

      SqlParseToken tok = parseSqlToken(sql, null);

      if (tok != null) {
        switch (tok.getFirstChar()) {
        case 'u': case 'U':
          if (tok.matchesToken("USE")) {
            // Mysql "USE DBNAME" statement.
            //
            // The Mysql JDBC driver does not properly implement getCatalog()
            // when calls to setCatalog() are mixed with SQL USE statements.
            // Work around this problem by translating a SQL USE statement
            // into a JDBC setCatalog() invocation. The setCatalog() API in
            // the ConnectorJ implementation just creates a USE statement
            // anyway. This also makes sure the database pool logic knows
            // which database is currently selected. If a second call to
            // select the current database is found, it is a no-op.

            tok = parseSqlToken(sql, tok);

            if (tok != null) {
              String dbname = tok.toUnquotedString();

              setCatalog(env, dbname);

              return BooleanValue.TRUE;
            }
          }
          else if (tok != null && tok.matchesToken("UPDATE")) {
            // SQL UPDATE statement

            _lastSql = LastSqlType.UPDATE;
          }
          break;

        case 'd': case 'D':
          if (tok.matchesToken("DESCRIBE")) {
            // SQL DESCRIBE statement

            _lastSql = LastSqlType.DESCRIBE;
          }
          break;

        case 's': case 'S':
          if (false && tok.matchesToken("SET")) {
            // SQL SET statement

            String lower = sql.toLowerCase(Locale.ENGLISH);
            if (lower.indexOf(" names ") >= 0) {
              // php/1469 - need to control i18n 'names'
              return LongValue.ONE;
            }
          }
          break;
        }
      }

      return super.realQuery(env, sql);
    } catch (SQLException e) {
      saveErrors(e);

      log.log(Level.FINER, e.toString(), e);

      return BooleanValue.FALSE;
    } catch (IllegalStateException e) {
      log.log(Level.FINEST, e.toString(), e);

      // #2184, some drivers return this on closed connection
      saveErrors(new SQLExceptionWrapper(e));

      return BooleanValue.FALSE;
    }
  }

  /**
   * Execute an single query against the database whose result
   * can then be retrieved or stored using the mysqli_store_result()
   * or mysqli_use_result() functions.
   *
   * @param env the PHP executing environment
   * @param query the escaped query string (can contain
   * escape sequences like `\n' and `\Z')
   */
  public boolean real_query(Env env,
                            StringValue query)
  {
    // Assume that the query argument contains just one query. Reuse the
    // result management logic in multiQuery(), so that a future call to
    // mysqli_store_result() will work as expected.

    return multiQuery(env, query);
  }

  /**
   * returns a prepared statement or null on error.
   */
  public MysqliStatement prepare(Env env, String query)
  {
    MysqliStatement stmt = new MysqliStatement((Mysqli) validateConnection(env));

    boolean result = stmt.prepare(env, query);

    if (! result) {
      stmt.close();
      return null;
    }

    return stmt;
  }

  /**
   * Connects to the underlying database.
   */
  public boolean real_connect(Env env,
                              @Optional("localhost") StringValue host,
                              @Optional StringValue userName,
                              @Optional StringValue password,
                              @Optional StringValue dbname,
                              @Optional("3306") int port,
                              @Optional StringValue socket,
                              @Optional int flags)
  {
    return connectInternal(env,
                           host.toString(),
                           userName.toString(),
                           password.toString(),
                           dbname.toString(),
                           port,
                           socket.toString(),
                           flags,
                           null,
                           null,
                           false);
  }

  /**
   * Escapes the string
   */
  public StringValue real_escape_string(Env env, StringValue str)
  {
    return realEscapeString(env, str);
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
  public boolean select_db(Env env, String db)
  {
    try {
      if (isConnected()) {
        validateConnection(env).setCatalog(env, db);

        return true;
      }
      else
        return false;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      // php/142d - php doesn't issue a warning if the database is
      // unselectable.  modx depends on this behavior.
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
  public StringValue getsqlstate(Env env)
  {
    return sqlstate(env);
  }

  /**
   * Returns the SQLSTATE error
   */
  public StringValue sqlstate(Env env)
  {
    int code = validateConnection(env).getErrorCode();
    return env.createString(lookupSqlstate(code));
  }

  /**
   * Given an error number, returns a SQLSTATE error string.
   */
  static String lookupSqlstate(int errno)
  {
    if (errno == 0)
      return "00000";
    else
      return "HY" + errno;
  }

  @Override
  protected String getDriverName()
  {
    return "mysql";
  }

  public Value get_charset(Env env)
  {
    String collation;
    String charset;

    if (_charset != null) {
      charset = _charset.toString();
    }
    else {
      charset = "latin1";
      _charset = env.createString(charset);
    }

    if (_collation != null) {
      collation = _collation.toString();
    }
    else {
      collation = MysqlCharset.getDefaultCollation(charset);
      _collation = env.createString(collation);
    }

    int charsetIndex = MysqlCharset.getCollationIndex(collation);
    int maxBytes = MysqlCharset.getMaxBytes(charset);

    String description = MysqlCharset.getDescription(charset);

    ObjectValue obj = env.createObject();

    obj.putField(env, env.createString("charset"), _charset);
    obj.putField(env, env.createString("collation"), _collation);
    obj.putField(env, env.createString("dir"), env.getEmptyString());

    obj.putField(env, env.createString("min_length"), LongValue.ONE);
    obj.putField(env, env.createString("max_length"), LongValue.create(maxBytes));
    obj.putField(env, env.createString("number"), LongValue.create(charsetIndex));

    obj.putField(env, env.createString("state"), LongValue.ONE);
    obj.putField(env, env.createString("comment"), env.createString(description));

    return obj;
  }

  public boolean set_charset(Env env, StringValue charset)
  {
    Connection conn = getConnection(env);

    if (conn == null) {
      return false;
    }

    Statement stmt = null;

    try {
      stmt = conn.createStatement();

      StringBuilder sb = new StringBuilder();
      sb.append("SET NAMES ");
      sb.append(charset);

      stmt.executeUpdate(sb.toString());
      stmt.close();

      _charset = charset;

      stmt = conn.createStatement();
      stmt.execute("SELECT * FROM information_schema.SESSION_VARIABLES "
                   + "WHERE VARIABLE_NAME = 'collation_connection'");

      ResultSet rs = stmt.getResultSet();

      if (rs.next()) {
        String str = rs.getString(2);

        _collation = env.createString(str);
      }
      else {
        _collation = env.getEmptyString();

        env.warning(L.l("unable to retrieve collation_connection variable"));
      }

      return true;
    }
    catch (SQLException e) {
      env.warning(e);

      return false;
    }
    finally {
      if (stmt != null) {
        try {
          stmt.close();
        }
        catch (Exception e) {
        }
      }
    }
  }

  @Override
  protected Value getServerStat(Env env)
  {
    return stat(env);
  }

  /**
   * returns a string with the status of the connection
   * or FALSE if error
   */
  public Value stat(Env env)
  {
    try {
      JdbcConnectionResource connV = validateConnection(env);

      Connection conn = connV.getConnection(env);

      if (conn == null)
        return BooleanValue.FALSE;

      Statement stmt = null;

      try {
        stmt = conn.createStatement();
        stmt.execute("SHOW STATUS");

        HashMap<String,String> statusMap = new HashMap<String,String>();

        ResultSet rs = stmt.getResultSet();

        while (rs.next()) {
          statusMap.put(rs.getString(1), rs.getString(2));
        }

        StringValue sb = env.createStringBuilder();
        sb.append("Uptime: ");
        sb.append(statusMap.get("Uptime"));

        sb.append("  ");
        sb.append("Threads: ");
        sb.append(statusMap.get("Threads_connected"));

        sb.append("  ");
        sb.append("Questions: ");
        sb.append(statusMap.get("Queries"));

        sb.append("  ");
        sb.append("Slow queries: ");
        sb.append(statusMap.get("Slow_queries"));

        sb.append("  ");
        sb.append("Opens: ");
        sb.append(statusMap.get("Opened_tables")); // XXX: right open?

        sb.append("  ");
        sb.append("Flush tables: ");
        sb.append(statusMap.get("Flush_commands"));

        sb.append("  ");
        sb.append("Open tables: ");
        sb.append(statusMap.get("Open_tables"));

        sb.append("  ");
        sb.append("Queries per second avg: ");

        String totalQueriesStr = statusMap.get("Queries");
        String uptimeStr = statusMap.get("Uptime");

        long totalQueries = Long.valueOf(totalQueriesStr);
        long uptime = Long.valueOf(uptimeStr);

        double average = ((double) totalQueries) / uptime;

        sb.append(String.format("%.2f", average));

        return sb;
      }
      finally {
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
    return new MysqliStatement((Mysqli) validateConnection(env));
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
    return ((Mysqli) validateConnection(env)).storeResult();
  }

  /**
   * Quercus function to get the field 'thread_id'.
   */
  public Value getthread_id(Env env)
  {
    return thread_id(env);
  }

  /**
   * Query an identifier that corresponds to this specific
   * connection. Mysql calls this integer identifier a
   * thread id, but it is really a connection id.
   * Return an integer on success, FALSE on failure.
   */

  Value thread_id(Env env)
  {
    try {
      JdbcConnectionResource connV = validateConnection(env);
      Connection conn = connV.getConnection(env);

      if (conn == null)
        return BooleanValue.FALSE;

      Statement stmt = null;

      try {
        stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT CONNECTION_ID()");

        if (rs.next())
          return LongValue.create(rs.getLong(1));
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
   * Kills the given mysql thread id. Killing the connection
   * is not the same as simply closing the connection. For
   * example, table locks are released by a KILL.
   */
  public boolean kill(Env env, int threadId)
  {
    try {
      JdbcConnectionResource connV = validateConnection(env);
      Connection conn = connV.getConnection(env);

      if (conn == null)
        return false;

      Statement stmt = null;
      boolean result = false;

      try {
        stmt = conn.createStatement();

        _conn.markForPoolRemoval();

        stmt.executeQuery("KILL CONNECTION " + threadId);

        result = true;

        // close the underlying java.sql connection, not Mysqli itself
        conn.close();

      }
      catch (SQLException e) {
        // exception thrown if cannot find specified thread id
      }
      finally {
        if (stmt != null) {
          stmt.close();
        }
      }

      return result;
    }
    catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
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
    return ((Mysqli) validateConnection(env)).storeResult();
  }

  /**
   * Quercus function to get the field 'warning_count'.
   */
  public int getwarning_count(Env env)
  {
    return warning_count(env);
  }

  /**
   * returns the number of warnings from the last query
   * in the connection object.
   *
   * @return number of warnings
   */
  public int warning_count(Env env)
  {
    return ((Mysqli) validateConnection(env)).getWarningCount(env);
  }

  /**
   * Creates a database-specific result.
   */
  @Override
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
  private int getWarningCount(Env env)
  {
    JdbcResultResource rs = metaQuery(env, "SHOW WARNINGS", getCatalog());
    int warningCount = 0;

    if (rs != null) {
      warningCount = rs.getNumRows();
    }

    if (warningCount >= 0)
      return warningCount;
    else
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
  protected MysqliResult metaQuery(Env env,
                                   String sql,
                                   String catalog)
  {
    clearErrors();

    String currentCatalog = getCatalog();

    try {
      Connection conn = getConnection(env);

      if (conn == null)
        return null;

      conn.setCatalog(catalog);

      // need to create statement after setting catalog or
      // else statement will have wrong catalog
      Statement stmt = conn.createStatement();
      stmt.setEscapeProcessing(false);

      if (stmt.execute(sql)) {
        MysqliResult result
          = (MysqliResult) createResult(stmt, stmt.getResultSet());
        conn.setCatalog(currentCatalog);
        return result;
      } else {
        conn.setCatalog(currentCatalog);
        return null;
      }
    } catch (SQLException e) {
      saveErrors(e);
      log.log(Level.FINE, e.toString(), e);
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
  private boolean multiQuery(Env env, StringValue sql)
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
        Connection conn = getConnection(env);

        if (conn == null)
          return false;

        stmt = conn.createStatement();
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
        log.log(Level.FINE, e.toString(), e);
        return false;
      }
    } catch (SQLException e) {
      saveErrors(e);
      log.log(Level.FINE, e.toString(), e);
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
  private ArrayList<String> splitMultiQuery(StringValue sqlStr)
  {
    ArrayList<String> result = new ArrayList<String>();
    StringBuilder queryBuffer = new StringBuilder(64);
    final String sql = sqlStr.toString();
    final int length = sql.length();
    boolean inQuotes = false;
    char c;

    for (int i = 0; i < length; i++) {
      c = sql.charAt(i);

      if (c == '\\') {
        queryBuffer.append(c);
        if (i < length - 1) {
          queryBuffer.append(sql.charAt(i + 1));
          i++;
        }
        continue;
      }

      if (inQuotes) {
        queryBuffer.append(c);
        if (c == '\'') {
          inQuotes = false;
        }
        continue;
      }

      if (c == '\'') {
        queryBuffer.append(c);
        inQuotes = true;
        continue;
      }

      if (c == ';') {
        result.add(queryBuffer.toString().trim());
        queryBuffer = new StringBuilder(64);
      } else
        queryBuffer.append(c);
    }

    if (queryBuffer.length() > 0)
      result.add(queryBuffer.toString().trim());

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

  // Indicate that this connection is a "persistent" connections,
  // meaning it can't be closed.

  public void setPersistent()
  {
    _isPersistent = true;
  }

  /**
   * Returns the mysql getColumnCharacterSet method
   */
  Method getColumnCharacterSetMethod(Class<?> metaDataClass)
  {
    if (_metaDataMethod == null) {
      MysqlMetaDataMethod metaDataMethod = _lastMetaDataMethod;

      if (metaDataMethod == null
          || metaDataMethod.getMetaDataClass() != metaDataClass) {
        metaDataMethod = new MysqlMetaDataMethod(metaDataClass);
        _lastMetaDataMethod = metaDataMethod;
      }

      _metaDataMethod = metaDataMethod;
    }

    return _metaDataMethod.getColumnCharacterSetMethod();
  }

  /*
  private static String checkDriverVersionImpl(Env env, Connection conn)
    throws SQLException
  {
    DatabaseMetaData databaseMetaData = null;

    try {
      databaseMetaData = conn.getMetaData();
    } catch (SQLException e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    // If getMetaData() returns null or raises a SQLException,
    // then we can't verify the driver version.

    if (databaseMetaData == null)
      return "";

    String fullVersion = null;

    try {
      fullVersion = databaseMetaData.getDriverVersion();
    } catch (SQLException e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    // If getDriverVersion() returns null or raises a SQLException,
    // then we can't verify the driver version.

    if (fullVersion == null) {
      return "";
    }

    String version = fullVersion;

    // Extract full version number.

    int start;
    int end = version.indexOf(' ');

    String checkedDriverVersion = "";

    if (end != -1) {
      version = version.substring(0, end);

      start = version.lastIndexOf('-');

      if (start != -1) {
        version = version.substring(start + 1);

        // version string should look like "3.1.14"
        int major;
        int minor;
        int release;

        start = version.indexOf('.');
        end = version.lastIndexOf('.');

        major = Integer.valueOf(version.substring(0, start));
        minor = Integer.valueOf(version.substring(start + 1, end));
        release = Integer.valueOf(version.substring(end + 1));

        checkedDriverVersion = major + "." + minor + "." + release;

        if (major >= 5
            || major == 3 && (minor > 1 || minor == 1 && release >= 14)) {
        }
        else {
          String message = L.l(
              "Your MySQL Connector/J JDBC {0} driver may "
                  + "have issues with character encoding.  The "
                  + "recommended JDBC version is 3.1.14/5+.", version);

          log.log(Level.WARNING, message);
          env.warning(message);
        }
      }
    }

    return checkedDriverVersion;
  }
  */

  public boolean close(Env env)
  {
    /*
    if (_isPersistent)
      return true;
    */

    close();

    return true;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    if (_conn != null && _conn.getConnection() != null) {
      Class<?> cls = _conn.getConnection().getClass();

      Method []methods = cls.getDeclaredMethods();

      for (int i = 0; i < methods.length; i++) {
        if (methods[i].getName().equals("toString")
            && methods[i].getParameterTypes().length == 0)
          return "Mysqli[" + _conn.getConnection() + "]";
      }

      return "Mysqli[" + cls.getCanonicalName() + "]";
    }
    else
      return "Mysqli[" + null + "]";
  }

  public enum LastSqlType {
    NONE,
    UPDATE,
    DESCRIBE
  };

  static class MysqlMetaDataMethod {
    private Class<?> _resultSetMetaDataClass;
    private Method _getColumnCharacterSetMethod;

    MysqlMetaDataMethod(Class<?> resultSetMetaDataClass)
    {
      _resultSetMetaDataClass = resultSetMetaDataClass;

      try {
        _getColumnCharacterSetMethod
          = _resultSetMetaDataClass.getMethod("getColumnCharacterSet",
                                              new Class[] { int.class });
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    Class<?> getMetaDataClass()
    {
      return _resultSetMetaDataClass;
    }

    Method getColumnCharacterSetMethod()
    {
      return _getColumnCharacterSetMethod;
    }
  }
}
