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

package com.caucho.php.lib;

import com.caucho.php.env.*;
import com.caucho.php.module.AbstractPhpModule;
import com.caucho.php.module.Optional;
import com.caucho.php.resources.JdbcConnectionResource;
import com.caucho.php.resources.JdbcResultResource;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PHP mysql routines.
 */
public class PhpMysqlModule extends AbstractPhpModule {

  private static final Logger log = Log.open(PhpMysqlModule.class);
  private static final L10N L = new L10N(PhpMysqlModule.class);

  public static final int MYSQL_ASSOC = 0x1;
  public static final int MYSQL_NUM = 0x2;
  public static final int MYSQL_BOTH = 0x3;

  public static final int MYSQL_USE_RESULT = 0x0;
  public static final int MYSQL_STORE_RESULT = 0x1;

  public PhpMysqlModule()
  {
  }

  /**
   * Returns true for the mysql extension.
   */
  public boolean isExtensionLoaded(String name)
  {
    return "mysql".equals(name);
  }

  /**
   * Returns the number of affected rows.
   */
  public Value mysql_affected_rows(Env env,
                                   @Optional JdbcConnectionResource connV)
    throws SQLException
  {
    JdbcConnectionResource newConnV = getConnection(env, connV);

    return PhpMysqliModule.mysqli_affected_rows(newConnV);
  }

  /**
   * returns JdbcConnectionResource and creates one if not there already
   */
  private JdbcConnectionResource getConnection(Env env,
                                            Value connV)
    throws IllegalStateException
  {
    Value value;

    JdbcConnectionResource conn = null;

    if (connV != null && connV.isset())
      conn = (JdbcConnectionResource) connV.toValue();

    if (conn == null) {
      value = env.getSpecialValue("caucho.mysql");

      if (value == null) { //Set for first time
        DataSource database = env.getDatabase();

        if (database == null)
          throw new IllegalStateException("no configured database");
        else {
          try {
            conn = new JdbcConnectionResource(database.getConnection());
            env.setSpecialValue("caucho.mysql", conn);
	    env.addResource(conn);
          } catch (SQLException e) {
            env.warning("A link to the server could not be established");
            log.log(Level.FINE, e.toString(), e);
          }
        }
      } else {
        conn = (JdbcConnectionResource) value;
      }
    }
    return conn;
  }

  /**
   * Returns the client encoding
   */
  public Value mysql_client_encoding(Env env,
                                     @Optional JdbcConnectionResource connV)
    throws SQLException
  {
    JdbcConnectionResource newConnV = getConnection(env, connV);

    return PhpMysqliModule.mysqli_character_set_name(newConnV);
  }

  /**
   * Closes a mysql connection.
   */
  public boolean mysql_close(Env env,
                             @Optional Value connV)
    throws Exception
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    if (conn == null)
      return false;

    conn.close();

    if (conn == getConnection(env, null))
      env.removeSpecialValue("caucho.mysql");

    return true;
  }

  /**
   * Creates a database.
   * XXX: seems to be unsupported in PHP 5.0.4
   */
  public boolean mysql_create_db(Env env,
                                 String name,
                                 @Optional Value connV)
    throws SQLException
  {
    JdbcConnectionResource conn = getConnection(env, connV);
    Statement stmt = null;

    if (conn == null)
      return false;

    try {
      stmt = conn.getConnection().createStatement();
      stmt.executeUpdate("CREATE DATABASE " + name);
    } finally {
      if (stmt != null)
        stmt.close();
    }

    return true;
  }

  /**
   * Moves the intenal row pointer of the MySQL result to the
   * specified row number.
   * 0 <= rowNumber <= mysql_num_rows() - 1
   * Returns TRUE on success or FALSE on failure
   */
  public boolean mysql_data_seek(Env env,
                                 JdbcResultResource resultV,
                                 int rowNumber)
  {
    return PhpMysqliModule.mysqli_data_seek(env, resultV, rowNumber);
  }

  /**
   * Retrieves the database name from a call to mysql_list_dbs()
   * row 0 is the first row
   * only valid value from mixedFieldV is 'Database'
   */
  public Value mysql_db_name(Env env,
                             JdbcResultResource resultV,
                             int row,
                             @Optional("0") Value mixedFieldV)
  {
    return mysql_result(env, resultV, row, mixedFieldV);
  }

  /*
  **
  */
  public Value mysql_result(Env env,
                            JdbcResultResource resultV,
                            int row,
                            @Optional("0") Value mixedFieldV)
  {
    Value result = BooleanValue.FALSE;

    if (resultV != null)
      result = resultV.getResultField(row, mixedFieldV);

    if (result == BooleanValue.FALSE)
      env.warning("Either row number or column index is invalid");

    return result;
  }

  /**
   * deletes databaseName
   */
  public boolean mysql_drop_db(Env env,
                               String databaseName,
                               @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    return mysql_query(env, "DROP DATABASE " + databaseName, connV) != BooleanValue.FALSE;
  }

  /**
   * Returns the error number of the most recent error
   */
  public int mysql_errno(Env env,
                         @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_errno(conn);
  }

  /**
   * Returns the most recent error
   */
  public String mysql_error(Env env,
                            @Optional Value connV)
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_error(conn);
  }

  public String mysql_escape_string(Env env,
                                    String unescapedString)
  {
    return PhpMysqliModule.mysqli_real_escape_string(null, unescapedString);
  }

  /**
   * Escapes the following special character in unescapedString.
   *
   * @see String PhpMysqliModule.mysqli_real_escape_string(JdbcConnectionResource, String)
   * @return the escaped string
   */

  public String mysql_real_escape_string(Env env,
                                         String unescapedString,
                                         @Optional JdbcConnectionResource conn)
  {
    JdbcConnectionResource newConn = getConnection(env, conn);

    return PhpMysqliModule.mysqli_real_escape_string(newConn, unescapedString);
  }

  /**
   * Returns a row from the connection
   */
  public Value mysql_fetch_array(Env env,
                                 JdbcResultResource result,
                                 @Optional("MYSQL_BOTH") int type)
    throws Exception
  {
    return PhpMysqliModule.mysqli_fetch_array(env, result, type);
  }

  /**
   * Returns a row from the connection
   */
  public Value mysql_fetch_assoc(Env env,
                                 JdbcResultResource result)
    throws Exception
  {
    return PhpMysqliModule.mysqli_fetch_assoc(env, result);
  }

  /**
   * returns an object containing field information. The
   * 12 properties according to PHP documentation are:
   * <p/>
   * name, table, max_length, not_null, primary_key,
   * unique_key, multiple_key, numeric, blob, type,
   * unsigned and zerofill.
   * <p/>
   * This function does not return the unique_key flag.
   *
   * On success, this method increments result.fieldOffset
   */
  public Value mysql_fetch_field(Env env,
                                 JdbcResultResource result,
                                 @Optional("-1") int fieldOffset)
    throws Throwable
  {
    if (result == null)
      return BooleanValue.FALSE;

    if (fieldOffset == -1)
      fieldOffset = result.getFieldOffset();

    Value fieldTable = result.getFieldTable(fieldOffset);
    Value fieldName = result.getFieldName(fieldOffset);
    Value fieldType = result.getFieldType(fieldOffset);
    Value fieldLength = result.getFieldLength(fieldOffset);
    Value fieldCatalog = result.getFieldCatalog(fieldOffset);

    if ((fieldTable == BooleanValue.FALSE)
        || (fieldName == BooleanValue.FALSE)
        || (fieldType == BooleanValue.FALSE)
        || (fieldLength == BooleanValue.FALSE)
        || (fieldCatalog == BooleanValue.FALSE)) {
      return BooleanValue.FALSE;
    }

    // Store current catalog
    JdbcConnectionResource conn = getConnection(env, null);
    if (!(conn instanceof JdbcConnectionResource))
      return BooleanValue.FALSE;

    Value currentCatalog = conn.getCatalog();

    String sql = "SHOW FULL COLUMNS FROM " + fieldTable + " LIKE \'" + fieldName + "\'";

    Value metaResult = mysql_db_query(env, fieldCatalog.toString(), sql, null);

    if (!(metaResult instanceof JdbcResultResource))
      return BooleanValue.FALSE;

    Value fieldResult = ((JdbcResultResource) metaResult).fetchField(env, fieldLength.toInt(), fieldTable.toString(), fieldType.toString());

    result.setFieldOffset(fieldOffset + 1);

    // Reset current catalog
    conn.setCatalog(currentCatalog.toString());
    
    return fieldResult;
  }

  /**
   * Returns a new mysql connection.
   */
  public Value mysql_query(Env env,
                           String sql,
                           @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_query(conn, sql, MYSQL_STORE_RESULT);
  }

  /**
   * returns an array of lengths on success or FALSE on failure.
   */
  public Value mysql_fetch_lengths(Env env,
                                   JdbcResultResource resultV)
  {
    return PhpMysqliModule.mysqli_fetch_lengths(resultV);
  }

  /**
   * Returns an object with properties that correspond to the fetched row
   * and moves the data pointer ahead.
   */
  public Value mysql_fetch_object(Env env,
                                  JdbcResultResource resultV)
    throws Exception
  {
    return PhpMysqliModule.mysqli_fetch_object(env, resultV);
  }

  /**
   * Returns a row from the connection
   */
  public Value mysql_fetch_row(Env env,
                               JdbcResultResource resultV)
    throws Exception
  {
    return PhpMysqliModule.mysqli_fetch_row(env, resultV);
  }

  /**
   * returns the field flags of the specified field.  The flags are reported as
   * a single word per flag spearated by a single space, so that you can split the
   * returned value using explode()
   * <p/>
   * the following flages are reported, if your version of MySQL is current enough
   * to support them:
   * <p/>
   * "not_null","primary_key","unique_key","multiple_key","blob","unsigned",
   * "zerofill","binary","enum","auto_increment" and "timestamp"
   * <p/>
   * This version does not return the unique_key flag
   */
  public Value mysql_field_flags(Env env,
                                 JdbcResultResource resultV,
                                 int fieldOffset)
    throws Throwable
  {
    if (resultV == null)
      return BooleanValue.FALSE;

    Value fieldTable = resultV.getFieldTable(fieldOffset);
    Value fieldName = resultV.getFieldName(fieldOffset);

    if ((fieldTable == BooleanValue.FALSE) || (fieldName == BooleanValue.FALSE))
      return BooleanValue.FALSE;

    String sql = "SHOW FULL COLUMNS FROM " + fieldTable.toString() + " LIKE \'" + fieldName.toString() + "\'";
    Value metaResult = mysql_query(env, sql, null);
    if (metaResult instanceof JdbcResultResource)
      return ((JdbcResultResource) metaResult).getFieldFlags();

    return BooleanValue.FALSE;
  }

  /**
   * Returns field name at given offset.
   *
   * @param env
   * @param resultV
   * @param fieldOffset
   * @return
   * @throws Exception
   */
  public Value mysql_field_name(Env env,
                                JdbcResultResource resultV,
                                int fieldOffset)
    throws Exception
  {
    if (resultV == null)
      return BooleanValue.FALSE;

    Value nameV = resultV.getFieldName(fieldOffset);

    if (nameV == BooleanValue.FALSE) {
      // XXX: may need to include some field from resultV in string
      // passed to warning.  Left out because of number following @
      // ie: com.caucho.php.resources.JdbcResultResource@1148603
      env.warning(L.l("Field {0} is invalid for MySQL", fieldOffset));
    }

    return nameV;
  }

  /**
   * seeks to the specified field offset.
   * If the next call to mysql_fetch_field() doesn't include
   * a field offset, the field offset specified in
   * mysql_field_seek() will be returned.
   */
  public boolean mysql_field_seek(Env env,
                                  JdbcResultResource resultV,
                                  int fieldOffset)
  {
    return PhpMysqliModule.mysqli_field_seek(env, resultV, fieldOffset);
  }

  /**
   * returns the name of the table on success.
   * XXX: PHP seems to return the left most column
   * no matter what string you put in the second
   * parameter
   */
  public Value mysql_field_table(Env env,
                                 JdbcResultResource resultV,
                                 int fieldOffset)
  {
    if (resultV == null)
      return BooleanValue.FALSE;

    Value nameV = resultV.getFieldTable(fieldOffset);

    if (nameV == BooleanValue.FALSE) {
      // XXX: may need to include some field from resultV in string
      // passed to warning.  Left out because of number following @
      // ie: com.caucho.php.resources.JdbcResultResource@1148603
      env.warning("Field " + fieldOffset + " is invalid for MySQL");
    }

    return nameV;
  }

  /**
   * returns field type
   */
  public Value mysql_field_type(Env env,
                                JdbcResultResource resultV,
                                int fieldOffset)
    throws Exception
  {
    if (resultV == null)
      return BooleanValue.FALSE;

    Value nameV = resultV.getFieldType(fieldOffset);

    if (nameV == BooleanValue.FALSE) {
      // XXX: may need to include some field from resultV in string
      // passed to warning.  Left out because of number following @
      // ie: com.caucho.php.resources.JdbcResultResource@1148603
      env.warning("Field " + fieldOffset + " is invalid for MySQL");
    }

    return nameV;
  }

  /**
   * Decprecated alias for mysql_field_len
   */
  public Value mysql_fieldlen(Env env,
                              JdbcResultResource resultV,
                              int fieldOffset)
  {
    return mysql_field_len(env, resultV, fieldOffset);
  }

  /**
   * returns the length of the specified field
   * XXX: returns a value of 10 for datatypes DEC and
   * NUMERIC where the actual PHP function
   * returns 11.
   */
  public Value mysql_field_len(Env env,
                               JdbcResultResource resultV,
                               int fieldOffset)
  {
    Value result = BooleanValue.FALSE;

    if (resultV != null) {
      result = resultV.getFieldLength(fieldOffset);

      if (result == BooleanValue.FALSE) {
        // XXX: may need to include some field from resultV in string
        // passed to warning.  Left out because of number following @
        // ie: com.caucho.php.resources.JdbcResultResource@1148603
        env.warning("Field " + fieldOffset + " is invalid for MySQL");
      }
    }

    return result;
  }

  /**
   * Frees a mysql result.
   */
  public boolean mysql_free_result(JdbcResultResource resultV)
  {
    return PhpMysqliModule.mysqli_free_result(resultV);
  }

  /**
   * returns the MySQL client version
   */
  public Value mysql_get_client_info(Env env)
    throws SQLException
  {
    return PhpMysqliModule.mysqli_get_client_info(env);
  }

  /**
   * returns a string describing the type of MySQL connection in use
   */
  public Value mysql_get_host_info(Env env,
                                   @Optional JdbcConnectionResource connV)
    throws SQLException
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_get_host_info(conn);
  }

  /**
   * returns an integer respresenting the MySQL protocol
   * version used by the connection.
   */
  public Value mysql_get_proto_info(Env env,
                                    @Optional JdbcConnectionResource connV)
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_get_proto_info(conn);
  }

  /**
   * returns the MySQL server version on success
   */
  public Value mysql_get_server_info(Env env,
                                     @Optional JdbcConnectionResource connV)
    throws SQLException
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_get_server_info(conn);
  }

  /**
   * returns ID generated for an AUTO_INCREMENT column by the previous
   * INSERT query on success, 0 if the previous query does not generate
   * an AUTO_INCREMENT value, or FALSE if no MySQL connection was established
   */
  public Value mysql_insert_id(Env env,
                               @Optional JdbcConnectionResource connV)
    throws SQLException
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_insert_id(conn);
  }

  /*
  ** Returns a result pointer containing the databases available from the current mysql daemon.
  */
  public Value mysql_list_dbs(Env env,
                              @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    Value result;
    JdbcConnectionResource conn = getConnection(env, connV);

    if (conn == null)
      return BooleanValue.FALSE;

    try {
      result = conn.getCatalogs();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
      result = BooleanValue.FALSE;
    }

    return result;
  }

  /**
   * retrieves information about the given table name
   * This function is deprecated.  It is preferable to use
   * mysql_query to issue 'SHOW COLUMNS FROM table LIKE 'name'.
   *
   * In fact, this function is just a wrapper for mysql_db_query.
   */ 
  public Value mysql_list_fields(Env env,
                                 String databaseName,
                                 String tableName,
                                 @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    return mysql_db_query(env,databaseName,"SHOW COLUMNS FROM " + tableName,connV);
  }

  /**
   * returns result set or false on error
   */
  public Value mysql_db_query(Env env,
                              String databaseName,
                              String query,
                              @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    if (mysql_select_db(env, databaseName, connV))
      return mysql_query(env, query, connV);
    else
      return BooleanValue.FALSE;
  }

  /**
   * Selects the database
   */
  public boolean mysql_select_db(Env env,
                                 String name,
                                 @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_select_db(env, conn, name);
  }

  /**
   * retrieves a list of table names from a MySQL database.
   */ 
  public Value mysql_list_tables(Env env,
                                 String databaseName,
                                 @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    return mysql_query(env, "SHOW TABLES FROM " + databaseName,connV);
  }

  public Value mysql_num_fields(Env env,
                                JdbcResultResource resultV)
  {
    return PhpMysqliModule.mysqli_num_fields(env, resultV);
  }

  /**
   * Retrieves number of rows from a result set.
   */
  public Value mysql_num_rows(Env env,
                              JdbcResultResource resultV)
    throws SQLException
  {
    return PhpMysqliModule.mysqli_num_rows(resultV);
  }

  /**
   * Returns a new mysql connection.
   */
  public Value mysql_pconnect(Env env,
                              @Optional Value serverV,
                              @Optional Value userV,
                              @Optional Value passwordV,
                              @Optional Value newLinkV,
                              @Optional Value flagsV)
  {
    return mysql_connect(env, serverV, userV,
      passwordV, newLinkV,
      flagsV);
  }

  /**
   * Returns a new mysql connection.
   */
  public Value mysql_connect(Env env,
                             @Optional Value serverV,
                             @Optional Value userV,
                             @Optional Value passwordV,
                             @Optional Value newLinkV,
                             @Optional Value flagsV)
  {
    try {
      DataSource database = env.getDatabase();

      if (database == null)
	throw new IllegalStateException("no configured database");

      JdbcConnectionResource value;

      value = new JdbcConnectionResource(database.getConnection());
      env.addResource(value);
      env.setSpecialValue("caucho.mysql", value);

      return value;
    } catch (Exception e) {
      env.warning("A link to the server could not be established. " + e.toString());
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Checks if the connection is still valid
   */
  public boolean mysql_ping(Env env,
                            @Optional Value connV)
    throws Exception
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    return PhpMysqliModule.mysqli_ping(conn);
  }

  /**
   * returns a string with the status of the connection
   * or NULL if error
   */
  public Value mysql_stat(Env env,
                          JdbcConnectionResource connV)
  {
    JdbcConnectionResource conn = getConnection(env, connV);

    Value result = PhpMysqliModule.mysqli_stat(env, conn);

    if (result == BooleanValue.FALSE)
      return NullValue.NULL;
    else
      return result;
  }
  
  /**
   * retrieves the table name from a call to mysql_list_tables()
   */ 
  public Value mysql_tablename(Env env,
                               JdbcResultResource resultV,
                               int i)
  {
    return resultV.getTable(i);
  }

  /**
   * Queries the database.
   */
  public Value mysql_unbuffered_query(Env env,
                                      String name,
                                      @Optional JdbcConnectionResource connV)
    throws Throwable
  {
    return mysql_query(env, name, connV);
  }

  //@todo mysql_change_user()
  //@todo mysql_info()
  //@todo mysql_list_processes()
  //@todo mysql_thread_id()
}

