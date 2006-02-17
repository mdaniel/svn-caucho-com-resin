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

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.resources.JdbcConnectionResource;
import com.caucho.quercus.resources.JdbcResultResource;
import com.caucho.quercus.resources.JdbcStatementResource;
import com.caucho.util.Log;
import com.caucho.util.L10N;


/**
 * Quercus mysql routines.
 */
public class QuercusMysqliModule extends AbstractQuercusModule {
  private static final Logger log = Log.open(QuercusMysqliModule.class);
  private static final L10N L = new L10N(QuercusMysqliModule.class);

  public static final int MYSQLI_ASSOC = 0x1;
  public static final int MYSQLI_NUM = 0x2;
  public static final int MYSQLI_BOTH = 0x3;
  public static final int MYSQLI_USE_RESULT = 0x0;
  public static final int MYSQLI_STORE_RESULT = 0x1;

  // The following flags are based on mysql_com.h
  // They are used by mysqli_fetch_field.
  public static final int NOT_NULL_FLAG = 0x1;
  public static final int PRI_KEY_FLAG = 0x2;
  public static final int UNIQUE_KEY_FLAG = 0x4;
  public static final int MULTIPLE_KEY_FLAG = 0x8;
  public static final int BLOB_FLAG = 0x10;
  public static final int UNSIGNED_FLAG = 0x20;
  public static final int ZEROFILL_FLAG = 0x40;
  public static final int BINARY_FLAG = 0x80;
  // the following are only sent to new clients
  // which I guess means mysqli vs. mysql
  public static final int ENUM_FLAG = 0x100;
  public static final int AUTO_INCREMENT_FLAG = 0x200;
  public static final int TIMESTAMP_FLAG = 0x400;
  public static final int SET_FLAG = 0x800;
  public static final int NUM_FLAG = 0x8000;
  public static final int PART_KEY_FLAG = 0x4000; //Intern: Part of some key???
  public static final int GROUP_FLAG = 0x8000;    //Intern: Group field???
  public static final int UNIQUE_FLAG = 0x10000;   //Intern: Used by sql_yacc???
  public static final int BINCMP_FLAG = 0x20000;  //Intern: Used by sql_yacc???

  // The following are numerical respresentations
  // of types returned by mysqli_fetch_field
  // mysqli_fetch_fields returns an int instead
  // of a meaningful string. Based on mysql_com.h
  public static final int MYSQL_TYPE_DECIMAL = 0x0;
  public static final int MYSQL_TYPE_TINY = 0x1;
  public static final int MYSQL_TYPE_SHORT = 0x2;
  public static final int MYSQL_TYPE_LONG = 0x3;
  public static final int MYSQL_TYPE_FLOAT = 0x4;
  public static final int MYSQL_TYPE_DOUBLE = 0x5;
  public static final int MYSQL_TYPE_NULL = 0x6;
  public static final int MYSQL_TYPE_TIMESTAMP = 0x7;
  public static final int MYSQL_TYPE_LONGLONG = 0x8;
  public static final int MYSQL_TYPE_INT24 = 0x9;
  public static final int MYSQL_TYPE_DATE = 0xA;
  public static final int MYSQL_TYPE_TIME = 0xB;
  public static final int MYSQL_TYPE_DATETIME = 0xC;
  public static final int MYSQL_TYPE_YEAR = 0xD;
  public static final int MYSQL_TYPE_NEWDATE = 0xE;
  public static final int MYSQL_TYPE_ENUM = 0xF7;
  public static final int MYSQL_TYPE_SET = 0xF8;
  public static final int MYSQL_TYPE_TINY_BLOB = 0xF9;
  public static final int MYSQL_TYPE_MEDIUM_BLOB = 0xFA;
  public static final int MYSQL_TYPE_LONG_BLOB = 0xFB;
  public static final int MYSQL_TYPE_BLOB = 0xFC;
  public static final int MYSQL_TYPE_VAR_STRING = 0xFD;
  public static final int MYSQL_TYPE_STRING = 0xFE;
  public static final int MYSQL_TYPE_GEOMETRY = 0xFF;


  private static final String MYSQL_CONNECTION = "caucho.mysql";

  /**
   * Returns true for the mysql extension.
   */
  public boolean isExtensionLoaded(String name)
  {
    return "mysqli".equals(name);
  }

  public QuercusMysqliModule()
  {
  }

  /**
   * returns the number of affected rows.
   */
  public static int mysqli_affected_rows(@NotNull Mysqli conn)
  {
    if (conn == null)
      return -1;

    return conn.affected_rows();
  }

  /**
   * Turns on or off auto-commiting daabase modifications
   */
  public static boolean mysqli_autocommit(Env env,
					  @NotNull Mysqli conn,
                                          boolean mode)
  {
    if (conn == null)
      return false;

    return conn.autocommit(mode);
  }

  /**
   * Alias for mysqli_stmt_bind_param
   */
  public static boolean mysqli_bind_param(Env env,
                                          @NotNull MysqliStatement stmt,
                                          String types,
                                          @Reference Value[] params)
  {
    return mysqli_stmt_bind_param(env, stmt, types, params);
  }

  /**
   * commits the current transaction to the database connection specified
   * by the connV parameter
   *
   * returns true on success or false on failure
   */
  public static boolean mysqli_commit(Env env,
                                      @NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.commit();
  }

  /**
   * Returns the client encoding.
   *
   * XXX: stubbed out. has to be revised once we
   * figure out what to do with character encoding
   */
  public static Value mysqli_character_set_name(@NotNull Mysqli conn)
  {
    if (conn == null)
      return BooleanValue.FALSE;

    return new StringValue("latin1");
  }

  /**
   * Alias for mysqli_character_set_name
   */
  public static Value mysqli_client_encoding(@NotNull Mysqli conn)
  {
    return mysqli_character_set_name(conn);
  }

  /**
   * Closes a mysqli connection
   */
  public static boolean mysqli_close(Env env, Mysqli conn)
  {

    if (conn != null)
      return conn.close(env);
    else
      return false;
  }

  /**
   * returns new mysqli connection
   */
  public static Mysqli mysqli_connect(Env env,
				      @Optional("localhost") String host,
				      @Optional String userName,
				      @Optional String password,
				      @Optional String dbname,
				      @Optional("3306") int port,
				      @Optional String socket)
    throws IllegalStateException
  {
    return new Mysqli(env, host, userName, password, dbname, port, socket);
  }

  /**
   * returns an error code value for the last call to mysqli_connect().
   */
  public static int mysqli_connect_errno(Env env)
  {
    Value value = (Value) env.getSpecialValue("mysqli.connectErrno");

    if (value != null)
      return value.toInt();
    else
      return 0;
  }

  /**
   * returns a string that describes the connection error
vv   */
  public static String mysqli_connect_error(Env env)
  {
    Object error = env.getSpecialValue("mysqli.connectError");

    if (error != null)
      return error.toString();
    else
      return "";
  }

  /**
   * seeks to an arbitrary result pointer specified
   * by the offset in the result set represented by result.
   * Returns TRUE on success or FALSE on failure
   */
  public static boolean mysqli_data_seek(Env env,
                                         @NotNull MysqliResult result,
                                         int rowNumber)
  {
    if (result == null)
      return false;

    return result.data_seek(env, rowNumber);
  }

  /**
   * Returns the error code for the most recent function call
   */
  public static int mysqli_errno(@NotNull Mysqli conn)
  {
    if (conn == null)
      return 0;

    return conn.errno();
  }

  /**
   * alias for mysqli_real_escape_string()
   */
  public static String mysqli_escape_string(@NotNull Mysqli conn,
                                            String unescapedString)
  {
    if (conn == null)
      return null;

    return conn.real_escape_string(unescapedString);
  }

  public static boolean mysqli_fetch(MysqliStatement stmt)
  {
    return mysqli_stmt_fetch(stmt);
  }

  /**
   * @see Value JdbcResultResource.fetchFieldDirect
   *
   * @param fieldOffset 0 <= fieldOffset < number of fields
   * @return an object or BooleanValue.FALSE
   *
   */
  public static Value mysqli_fetch_field_direct(Env env,
                                                @NotNull MysqliResult result,
                                                int fieldOffset)
    throws Exception
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_field_direct(env, fieldOffset);
  }

  /**
   * @see Value JdbcResultResource.fetchFieldDirect
   *
   * @return an object or BooleanValue.FALSE if no more columns
   */
  public static Value mysqli_fetch_field(Env env,
                                         @NotNull MysqliResult result)
    throws Exception
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_field(env);
  }

  /**
   * returns an array of fetch_field Objects
   *
   * @see Value JdbcResultResource.fetchFieldDirect
   *
   * @param result result set
   */
  public static Value mysqli_fetch_fields(Env env,
                                          @NotNull MysqliResult result)
    throws Exception
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_fields(env);
  }

  /**
   * returns an array of integers respresenting the size of each column
   * FALSE if an error occurred.
   */
  public static Value mysqli_fetch_lengths(@NotNull MysqliResult result)
    throws Exception
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_lengths();
  }

  /**
   * seeks to the specified field offset.
   * If the next call to mysql_fetch_field() doesn't include
   * a field offset, the field offset specified in
   * mysqli_field_seek() will be returned.
   */
  public static boolean mysqli_field_seek(Env env,
                                          @NotNull MysqliResult result,
                                          int fieldOffset)
    throws Exception
  {
    if (result == null)
      return false;

    result.field_seek(env, fieldOffset);

    return true;
  }

  /**
   * returns the position of the field cursor used for the last
   * mysqli_fetch_field() call. This value can be used as an
   * argument to mysqli_field_seek()
   */
  public static int mysqli_field_tell(Env env,
				      @NotNull MysqliResult result)
    throws Exception
  {
    if (result == null)
      return -1;

    return result.field_tell(env);
  }

  /**
   * frees a mysqli result
   */
  public static boolean mysqli_free_result(MysqliResult result)
  {
    if (result == null)
      return false;

    result.close();

    return true;
  }
  /**
   * returns ID generated for an AUTO_INCREMENT column by the previous
   * INSERT query on success, 0 if the previous query does not generate
   * an AUTO_INCREMENT value, or FALSE if no MySQL connection was established
   *
   */
  public static Value mysqli_insert_id(@NotNull Mysqli conn)
     throws SQLException
  {
    if (conn == null)
      return BooleanValue.FALSE;

    return conn.insert_id();
  }

  /**
   * returns the number of fields from specified result set
   */
  public static int mysqli_num_fields(Env env,
				      @NotNull MysqliResult result)
  {
    if (result == null)
      return -1;

    return result.num_fields();
  }

  /**
   * executes one or multiple queires which are
   * concatenated by a semicolon.
   */
  public static boolean mysqli_multi_query(@NotNull Mysqli conn,
                                           String query)
  {
    if (conn == null)
      return false;

    return conn.multi_query(query);
  }

  /**
   * indicates if one or more result sets are available from
   * a previous call to mysqli_multi_query
   */
  public static boolean mysqli_more_results(@NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.more_results();
  }

  /**
   * prepares next result set from a previous call to
   * mysqli_multi_query
   */
  public static boolean mysqli_next_result(@NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.next_result();
  }

  /**
   * Returns the error code for the prepared statement
   */
  public static int mysqli_stmt_errno(@NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return 0;

    return stmt.errno();
  }

  /**
   * Returns the error message for the prepared statement
   */
  public static String mysqli_stmt_error(@NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return "";

    return stmt.error();
  }
  /**
   * Returns the most recent error
   */
  public static String mysqli_error(@NotNull Mysqli conn)
  {
    if (conn == null)
      return "";

    return conn.error();
  }

  /**
   * returns a boolean to determine if the last query
   * to the connection returned a resultset.
   *
   * XXX: This is slightly different from the actual PHP
   * description.  It seems that in PHP, mysqli_field_count
   * returns the actual number of columns.  However, we
   * do NOT do this to avoid an extra call to getMetaData().
   *
   * It seems that the only time this function is used in
   * practice is to check to see if it is non-zero anyway.
   *
   * @return false if no result set, true otherwise
   */
  public static int mysqli_field_count(@NotNull Mysqli conn)
  {
    if (conn == null)
      return 0;

    return conn.field_count();
  }

  /**
   * returns a row for the result
   */
  public static Value mysqli_fetch_array(Env env,
                                         @NotNull MysqliResult result,
                                         @Optional("MYSQLI_BOTH") int type)
    throws Exception
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_array(type);
  }

  /**
   * returns an associative array from the result
   */
  public static Value mysqli_fetch_assoc(Env env,
                                         @NotNull MysqliResult result)
    throws Exception
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_assoc();
  }

  /**
   * returns a row for the resultV
   */
  public static Value mysqli_fetch_row(Env env,
                                       @NotNull MysqliResult result)
    throws Exception
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_row();
  }

  /**
   * Returns an object with properties that correspond
   * to the fetched row and moves the data pointer ahead.
   */
  public static Value mysqli_fetch_object(Env env,
                                          @NotNull MysqliResult result)
    throws Exception
  {
    if (result == null)
      return NullValue.NULL;

    return result.fetch_object(env);
  }

  /**
   * returns the MySQL client version
   */
  public static Value mysqli_get_client_info(Env env)
    throws SQLException
  {
    JdbcConnectionResource conn = QuercusMysqliModule.getConnection(env, null);

    if (conn == null)
      return BooleanValue.FALSE;
    else
      return new StringValue(conn.getClientInfo());
  }

  /**
   * helper function for mysqli_get_client_version
   * and mysqli_get_server_version
   */
  private static int infoToVersion(String info)
  {
    String[] result = info.split("[.]");

    if (result.length < 3)
      return 0;

    return (Integer.parseInt(result[0]) * 10000 + Integer.parseInt(result[1]) * 100) + Integer.parseInt(result[2].replaceAll("[a-zA-Z]",""));
  }

  /**
   * returns a number that represents the MySQL client library
   * version in format:
   *
   * main_version*10000 + minor_version*100 + sub_version.
   *
   * For example 4.1.0 is returned as 40100.
   */
   public static Value mysqli_get_client_version(Env env)
    throws SQLException
   {
     JdbcConnectionResource conn = QuercusMysqliModule.getConnection(env, null);

     if (conn == null)
       return BooleanValue.FALSE;
     else
       return new LongValue(QuercusMysqliModule.infoToVersion(conn.getClientInfo()));
   }

  /**
   * returns a string describing the type of MySQL
   * connection in use
   */
  public static String mysqli_get_host_info(@NotNull Mysqli conn)
    throws SQLException
  {
    if (conn == null)
      return null;

    return conn.get_host_info();
  }

  /**
   * XXX: always returns protocol of 10
   */
  public static Value mysqli_get_proto_info(@NotNull Mysqli conn)
  {
    return new LongValue(10);
  }

  /**
   * returns the MySQL server version on success
   */
  public static String mysqli_get_server_info(@NotNull Mysqli conn)
    throws SQLException
  {
    if (conn == null)
      return null;

    return conn.get_server_info();
  }

  /**
   * returns a number that represents the MySQL client library version in format:
   * <p/>
   * main_version*10000 + minor_version*100 + sub_version.
   * <p/>
   * For example 4.1.0 is returned as 40100.
   */
  public static Value mysqli_get_server_version(JdbcConnectionResource connV)
    throws SQLException
  {
    if (connV == null)
      return BooleanValue.FALSE;
    else
      return new LongValue(QuercusMysqliModule.infoToVersion(connV.getServerInfo()));
  }

  /**
   * returns the number of rows in the result set.
   */
  public static Value mysqli_num_rows(@NotNull MysqliResult result)
    throws SQLException
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.num_rows();
  }

  /**
   * Sets the options for a mysqli object
   */
  public static boolean mysqli_options(@NotNull Mysqli mysqli,
				       int option,
				       Value value)
  {
    if (mysqli == null)
      return false;

    return mysqli.options(option, value);
  }

  /**
   * Alias of mysqli_stmt_param_count
   */
  public static int mysqli_param_count(@NotNull MysqliStatement stmt)
  {
    return mysqli_stmt_param_count(stmt);
  }

  /**
   * rolls back the current transaction to the database
   * connection specified by the connV parameter
   *
   * returns true on success or false on failur
   *
   * NOTE: PHP does not seem to support the idea of
   * savepoints.
   */
  public static boolean mysqli_rollback(Env env,
					@NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.rollback();
  }

  /**
   * Sets the character set for a mysqli object
   */
  public static boolean mysqli_set_charset(@NotNull Mysqli mysqli,
					   String charset)
  {
    if (mysqli == null)
      return false;

    return mysqli.set_charset(charset);
  }

  /**
   * Sets the options for a mysqli object
   */
  public static boolean mysqli_set_opt(@NotNull Mysqli mysqli,
				       int option,
				       Value value)
  {
    return mysqli_options(mysqli, option, value);
  }

  /**
   * returns the number of rows in the result set
   * of a prepared statement
   */
  public static Value mysqli_stmt_num_rows(@NotNull MysqliStatement stmt)
    throws SQLException
  {
    if (stmt == null)
      return BooleanValue.FALSE;

    return stmt.num_rows();
  }

  /**
   * returns an integer representing the number of parameters
   * or -1 if no query has been prepared
   */
  public static int mysqli_stmt_param_count(@NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return -1;

    return stmt.param_count();
  }

  /**
   * prepares a statment with a query.
   */
  public static boolean mysqli_stmt_prepare(MysqliStatement stmt,
					    String query)
  {
    if (stmt != null)
      return stmt.prepare(query);
    else
      return false;
  }

  /**
   * resets a statment
   */
  public static boolean mysqli_stmt_reset(MysqliStatement stmt)
  {
    if (stmt != null)
      return stmt.reset();
    else
      return false;
  }

  /**
   * Returns result information for metadata
   */
  public static Value mysqli_stmt_result_metadata(Env env,
						  MysqliStatement stmt)
    throws SQLException
  {
    if (stmt != null)
      return stmt.result_metadata(env);
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns an error string
   */
  public static String mysqli_stmt_sqlstate(MysqliStatement stmt)
  {
    if (stmt != null)
      return stmt.sqlstate();
    else
      return "";
  }

  /**
   * Saves the result.
   */
  public static boolean mysqli_stmt_store_result(Env env,
						 @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    return stmt.store_result();
  }

  /**
   * Transfers the result set from the last query on the
   * database connection represented by conn.
   *
   * Used in conjunction with mysqli_multi_query
   */
   public static Value mysqli_store_result(Env env,
					   @NotNull Mysqli conn)
  {
    if (conn == null)
      return BooleanValue.FALSE;

    return conn.store_result(env);
  }

  public static Value mysqli_use_result(Env env,
					@NotNull Mysqli conn)
  {
    if (conn == null)
      return BooleanValue.FALSE;

    return conn.use_result(env);
  }

  /**
   * returns the number of warnings from the last query
   * in the connection object.
   *
   * @return number of warnings
   */
  public static int mysqli_warning_count(@NotNull Mysqli conn)
    throws SQLException
  {
    if (conn == null)
      return 0;

    return conn.warning_count();
  }

  /**
   * Checks if the connection is still valid
   */
  public static boolean mysqli_ping(@NotNull Mysqli conn)
    throws SQLException
  {
    return conn != null && conn.ping();
  }

  /**
   * returns JdbcResultResource representing the results of the query.
   *
   * <i>resultMode</i> is ignored, MYSQLI_USE_RESULT would represent
   * an unbuffered query, but that is not supported.
   */
  public static Value mysqli_query(@NotNull Mysqli conn,
                                   String sql,
                                   @Optional("MYSQLI_STORE_RESULT") int resultMode)
  {
    return query(conn, sql);
  }

  private static Value query(Mysqli conn,
                             String sql)
  {
    if (conn == null)
      return BooleanValue.FALSE;

    try {
      Value result = conn.query(sql, MYSQLI_STORE_RESULT);
      return result;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Connects to the database.
   */
  public static boolean mysqli_real_connect(Env env,
					    @NotNull Mysqli mysqli,
					    @Optional("localhost") String host,
					    @Optional String userName,
					    @Optional String password,
					    @Optional String dbname,
					    @Optional("3306") int port,
					    @Optional String socket,
					    @Optional int flags)

  {
    if (mysqli != null)
      return mysqli.real_connect(env, host, userName, password,
				 dbname, port, socket, flags);
    else
      return false;
  }

  /**
   * Escapes the following special character in unescapedString.
   * (all of the comments are from the MySQL source code).
   * <p/>
   * case 0:      NULL = ASCII 0 Useful for log files
   * case '\n':   Useful for log files
   * case '\r':   Useful for log files
   * case '\\':   STRICTLY NEEDED BY SQL
   * case '\'':   STRICTLY NEEDED BY SQL
   * case '"':    Better safe than sorry
   * case '\032': This gives problems on Win32
   * <p/>
   * PHP documents that their version takes "into account
   * the current character set of the connection."
   * <p/>
   * XXX: taking the encoding used by a particular instance
   * of MySQL seems like an incorrect assumption.
   * <p/>
   * In resin's implementation, the strings passed to this function will
   * be generated within Java not MySQL, and all strings in Java are
   * encoded using UTF-16, so there is no question whether a character needs
   * to be escaped or is part of a multibyte character.
   * <p/>
   * Even in the PHP world, iconv, UTF8_encode, or recode_string would
   * be responsible for changing a string from a series of characters
   * (ie: a series of bytes) -- not MySQL.
   * <p/>
   * So why assume the string is encoded with the same
   * setting as a particular MySQL instance to
   * check for multibyte characters?
   * <p/>
   * In any case, checking whether a particular byte was part of a multibyte
   * character would only be important if a string of bytes were read
   * one at a time, and you didn't know in advance what encoding had been used
   * or if you didn't know the length of a code unit (ie: 1,2,3, or more bytes).
   * This seems to be the approach taken in the MySQL source code.
   * <p/>
   * @return the escaped string.
   */
  public static String mysqli_real_escape_string(Mysqli conn,
                                                 String unescapedString)
  {
    StringBuilder buf = new StringBuilder();

    escapeString(buf, unescapedString);

    return buf.toString();
  }

  static void escapeString(StringBuilder buf,
                           String unescapedString)
  {
    char c;

    final int strLength = unescapedString.length();

    for (int i = 0; i < strLength; i++) {
      c = unescapedString.charAt(i);
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
  }

  /**
   * mysqli_real_query is "used to excute only a query
   * against the database." I assume this means just a
   * SELECT query.
   *
   * It is implemented here as an alias for mysqli_query
   */
  public static Value mysqli_real_query(@NotNull Mysqli conn,
                                        String query)
  {
    return query(conn, query);
  }

  /**
   * returns JdbcResultResource representing the results of the query.
   */
  static Value mysqli_query(Mysqli conn,
                            String query,
                            Object ... args)
  {
    StringBuilder buf = new StringBuilder();

    int size = query.length();

    int argIndex = 0;

    for (int i = 0; i < size; i++) {
      char ch = buf.charAt(i);

      if (ch == '?') {
        Object arg = args[argIndex++];

        if (arg == null)
          throw new IllegalArgumentException(L.l("argument `{0}' cannot be null", arg));

        buf.append('\'');
        escapeString(buf, String.valueOf(arg));
        buf.append('\'');
      }
      else
        buf.append(ch);
    }

    return query(conn, buf.toString());
  }


  /**
   * Selects the database
   */
  public static boolean mysqli_select_db(Env env,
                                         Mysqli conn,
                                         String dbName)
  {
    if (conn == null)
      return false;

    return conn.select_db(dbName);
  }

  /**
   * returns a string with the status of the connection
   * or FALSE if error
   */
  public static Value mysqli_stat(Env env,
                                  @NotNull Mysqli conn)
    throws SQLException
  {
    if (conn == null)
      return BooleanValue.FALSE;

    return conn.stat(env);
  }

  /**
   * returns the number of affected rows.
   */
  public int mysql_stmt_affected_rows(@NotNull Mysqli mysqli)
  {
    if (mysqli == null)
      return -1;

    return mysqli.affected_rows();
  }

  /**
   * binds variables for the parameter markers
   * in SQL statement that was passed to
   * mysqli_prepare().
   *
   * Type specification chars:
   * i: corresponding variable has type integer;
   * d: corresponding variable has type double;
   * b: corresponding variable is a blob and will be sent in packages
   * s: corresponding variable has type string (which really means all other types);
   */
  public static boolean mysqli_stmt_bind_param(Env env,
                                               @NotNull MysqliStatement stmt,
                                               String types,
                                               @Reference Value[] params)
  {
    if (stmt == null)
      return false;

    return stmt.bind_param(env, types, params);
  }

  /**
   * binds outparams to result set
   */
  public static boolean mysqli_stmt_bind_result(Env env,
                                                @NotNull MysqliStatement stmt,
                                                @Reference Value[] outParams)
  {
    if (stmt == null)
      return false;

    return stmt.bind_result(env, outParams);
  }

  /**
   * closes the statement
   */
  public boolean mysql_stmt_close(MysqliStatement stmt)
    throws SQLException
  {
    if (stmt == null)
      return false;

    return stmt.close();
  }

  /**
   * Seeks to a given result
   */
  public Value mysqli_stmt_data_seek(@NotNull MysqliStatement stmt, int offset)
  {
    if (stmt == null)
      return BooleanValue.FALSE;

    stmt.data_seek(offset);

    return NullValue.NULL;
  }

  /**
   * returns the error number
   */
  public int mysql_stmt_errno(MysqliStatement stmt)
  {
    if (stmt != null)
      return stmt.errno();
    else
      return 0;
  }

  /**
   * Returns a descrption of the error or an empty strng for no error.
   */
  public String mysql_stmt_error(MysqliStatement stmt)
  {
    if (stmt == null)
      return null;

    return stmt.error();
  }

  /**
   * executes statement stored in resultV. The statement has been prepared using mysqli_prepare.
   * <p/>
   * returns true on success or false on failure
   */
  public static boolean mysqli_stmt_execute(Env env,
					    @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    return stmt.execute(env);
  }

  /**
   * Fetch results from a prepared statement into bound variables.
   * <p/>
   * returns true on success, false on error null if no more rows
   */
  public static boolean mysqli_stmt_fetch(@NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    return stmt.fetch();
  }

  /**
   * Frees the associated result
   */
  public static boolean mysqli_stmt_free_result(MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    stmt.free_result();

    return true;
  }

  public static boolean mysqli_bind_result(Env env,
                                           @NotNull MysqliStatement stmt,
                                           @Reference Value[] outParams)
  {
    return mysqli_stmt_bind_result(env, stmt, outParams);
  }

  /**
   * changes the user and database.
   */
  public static boolean mysqli_change_user(Env env,
					   @NotNull Mysqli mysqli,
					   String user,
					   String password,
					   String db)
  {
    if (mysqli == null)
      return false;

    return mysqli.change_user(user, password, db);
  }

  public static boolean mysqli_execute(Env env,
                                       @NotNull MysqliStatement stmt)
  {
    return mysqli_stmt_execute(env, stmt);
  }

  public static Value mysqli_get_metadata(Env env,
					  @NotNull MysqliStatement stmt)
    throws SQLException
  {
    return mysqli_stmt_result_metadata(env, stmt);
  }

  /**
   * Creates a new mysqli object.
   */
  public static Mysqli mysqli_init(Env env)
  {
    return new Mysqli(env);
  }

  /**
   * Prepare a statement.
   */
  public static MysqliStatement mysqli_prepare(Env env,
					       @NotNull Mysqli conn,
					       String query)
    throws SQLException
  {
    if (conn == null)
      return null;

    return conn.prepare(env, query);
  }

  /**
   * closes a statement
   */
  public static boolean mysqli_stmt_close(MysqliStatement stmt)
    throws SQLException
  {
    if (stmt == null)
      return false;

    return stmt.close();
  }

  /**
   * returns a statement for use with
   * mysqli_stmt_prepare
   */
  public static MysqliStatement mysqli_stmt_init(Env env,
						 @NotNull Mysqli conn)
  {
    if (conn == null)
      return null;

    return conn.stmt_init(env);
  }

  /**
   * returns JdbcConnectionResource and creates one if not there already
   */
  private static JdbcConnectionResource getConnection(Env env,
						      JdbcConnectionResource conn)
    throws SQLException
  {
    if (conn != null)
      return conn;

    JdbcConnectionResource value = (JdbcConnectionResource) env.getSpecialValue(MYSQL_CONNECTION);

    if (value != null)
      return value;

    //Set for first time
    DataSource database = env.getDatabase();

    if (database == null)
      throw new IllegalStateException(L.l("no configured database"));

    try {
      conn = new JdbcConnectionResource(database.getConnection());
      env.setSpecialValue(MYSQL_CONNECTION, conn);
      env.addResource(conn);

      return conn;
    } catch (SQLException e) {
      env.warning(L.l("A link to the server could not be established"));
      log.log(Level.FINE, e.toString(), e);
      throw new SQLException(L.l("no link could be established"));
    }
  }

  //@todo mysqli_debug
  //@todo mysqli_disable_reads_from_master
  //@todo mysqli_disable_rpl_parse
  //@todo mysqli_dump_debug_info
  //@todo mysqli_embedded_connect
  //@todo mysqli_enable_reads_from_master
  //@todo mysqli_enable_rpl_parse
  //@todo mysqli_info
  //@todo mysqli_kill
  //@todo mysqli_master_query
  //@todo mysqli_report
  //@todo mysqli_rpl_parse_enable
  //@todo mysqli_rpl_probe
  //@todo mysqli_rpl_query_type
  //@todo mysqli_send_long_data
  //@todo mysqli_send_query
  //@todo mysqli_server_end
  //@todo mysqli_server_init
  //@todo mysqli_set_charset
  //@todo mysqli_set_opt
  //@todo mysqli_sqlstate
  //@todo mysqli_ssl_set
  //@todo mysqli_stat
  //@todo mysqli_stmt_reset
  //@todo mysqli_stmt_send_long_data
  //@todo mysqli_stmt_sqlstate
  //@todo mysqli_thread_id
  //@todo mysqli_thread_safe
}
