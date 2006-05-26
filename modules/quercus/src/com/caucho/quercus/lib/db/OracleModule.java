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
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Map;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.caucho.util.Log;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.lib.db.JdbcConnectionResource;

import com.caucho.quercus.lib.db.JdbcResultResource;
import com.caucho.quercus.lib.db.JdbcTableMetaData;
import com.caucho.quercus.lib.db.JdbcColumnMetaData;

//@todo create a Oraclei and OracleiResult instead
import com.caucho.quercus.lib.db.Mysqli;
import com.caucho.quercus.lib.db.MysqliResult;
import com.caucho.quercus.lib.db.MysqliStatement;
//@todo remove (still using MYSQL_xxx constants)
import com.caucho.quercus.lib.db.MysqlModule;

import java.lang.reflect.*;
import java.io.*;

/**
 * PHP oracle routines.
 *
 * NOTE from php.net:
 *
 * "...
 * These functions allow you to access Oracle 10, Oracle 9, Oracle 8 and Oracle 7
 * databases using the Oracle Call Interface (OCI). They support binding of PHP
 * variables to Oracle placeholders, have full LOB, FILE and ROWID support, and
 * allow you to use user-supplied define variables.
 *
 * Requirements
 *
 * You will need the Oracle client libraries to use this extension.
 * Windows users will need libraries with version at least 10 to use the php_oci8.dll.
 *
 * ..."
 *
 * PS: We use the Thin driver for the oci_xxx functions as opposed to the OCI driver
 * so you don't need the Oracle native client libraries.
 *
 */
public class OracleModule extends AbstractQuercusModule {

  private static final Logger log = Log.open(OracleModule.class);

  public static final int OCI_DEFAULT = 0x01;
  public static final int OCI_DESCRIBE_ONLY = 0x02;
  public static final int OCI_COMMIT_ON_SUCCESS = 0x03;
  public static final int OCI_EXACT_FETCH = 0x04;
  public static final int OCI_SYSDATE = 0x05;
  public static final int OCI_B_BFILE = 0x06;
  public static final int OCI_B_CFILEE = 0x07;
  public static final int OCI_B_CLOB = 0x08;
  public static final int OCI_B_BLOB = 0x09;
  public static final int OCI_B_ROWID = 0x0A;
  public static final int OCI_B_CURSOR = 0x0B;
  public static final int OCI_B_NTY = 0x0C;
  public static final int OCI_B_BIN = 0x0D;
  public static final int SQLT_BFILEE = 0x0E;
  public static final int SQLT_CFILEE = 0x0F;
  public static final int SQLT_CLOB = 0x10;
  public static final int SQLT_BLOB = 0x11;
  public static final int SQLT_RDD = 0x12;
  public static final int SQLT_NTY = 0x13;
  public static final int SQLT_LNG = 0x14;
  public static final int SQLT_LBI = 0x15;
  public static final int SQLT_BIN = 0x16;
  public static final int SQLT_NUM = 0x17;
  public static final int SQLT_INT = 0x18;
  public static final int SQLT_AFC = 0x19;
  public static final int SQLT_CHR = 0x1A;
  public static final int SQLT_VCS = 0x1B;
  public static final int SQLT_AVC = 0x1C;
  public static final int SQLT_STR = 0x1D;
  public static final int SQLT_LVC = 0x1E;
  public static final int SQLT_FLT = 0x1F;
  public static final int SQLT_ODT = 0x20;
  public static final int SQLT_BDOUBLE = 0x21;
  public static final int SQLT_BFLOAT = 0x22;
  public static final int OCI_FETCHSTATEMENT_BY_COLUMN = 0x23;
  public static final int OCI_FETCHSTATEMENT_BY_ROW = 0x24;
  public static final int OCI_ASSOC = 0x25;
  public static final int OCI_NUM = 0x26;
  public static final int OCI_BOTH = 0x27;
  public static final int OCI_RETURN_NULLS = 0x28;
  public static final int OCI_RETURN_LOBS = 0x29;
  public static final int OCI_DTYPE_FILE = 0x2A;
  public static final int OCI_DTYPE_LOB = 0x2B;
  public static final int OCI_DTYPE_ROWID = 0x2C;
  public static final int OCI_D_FILE = 0x2D;
  public static final int OCI_D_LOB = 0x2E;
  public static final int OCI_D_ROWID = 0x2F;
  public static final int OCI_SYSOPER = 0x30;
  public static final int OCI_SYSDBA = 0x31;
  public static final int OCI_LOB_BUFFER_FREE = 0x32;
  public static final int OCI_TEMP_CLOB = 0x33;
  public static final int OCI_TEMP_BLOB = 0x34;

  public OracleModule()
  {
  }

  /**
   * Returns true for the oracle extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "oracle" };
  }

  /**
   * Binds PHP array to Oracle PL/SQL array by name
   */
  public Value oci_bind_array_by_name(Env env,
                                      @NotNull MysqliStatement stmt,
                                      @NotNull String name,
                                      @NotNull String varArray,
                                      @NotNull int maxTableLength,
                                      @Optional("0") int maxItemLength,
                                      @Optional("0") int type)
  {
    throw new UnimplementedException("oci_bind_array_by_name");
  }

  /**
   * Binds the PHP variable to the Oracle placeholder
   */
  public Value oci_bind_by_name(Env env,
                                @NotNull MysqliStatement stmt,
                                @NotNull String variable,
                                @NotNull Value value,
                                @Optional("0") int maxLength,
                                @Optional("0") int type)
  {
    try {
      Integer index = (Integer)stmt.removeBindingVariable(variable);
      stmt.getPreparedStatement().setString(index.intValue(), value.toString());
      return BooleanValue.TRUE;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);

      try {
        stmt.resetBindingVariables();
      } catch (Exception ex2) {
        log.log(Level.FINE, ex2.toString(), ex2);
      }
    }

    return BooleanValue.FALSE;
  }

  /**
   * Cancels reading from cursor
   */
  public Value oci_cancel(Env env,
                          @NotNull MysqliStatement stmt)
  {
    return oci_free_statement(env, stmt);
  }

  /**
   * Closes Oracle connection
   */
  public Value oci_close(Env env,
                         @NotNull Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    if (conn != null) {
      if (conn == getConnection(env))
        env.removeSpecialValue("caucho.oracle");

      conn.close(env);

      return BooleanValue.TRUE;
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Commits outstanding statements
   */
  public Value oci_commit(Env env,
                          @NotNull Mysqli conn)
  {
    try {
      return BooleanValue.create(conn.commit());
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Establishes a connection to the Oracle server
   */
  public Value oci_connect(Env env,
                           @NotNull String username,
                           @NotNull String password,
                           @Optional String db,
                           @Optional String charset,
                           @Optional("0") int sessionMode)
  {
    // Note:  The second and subsequent calls to oci_connect() with the same parameters
    // will return the connection handle returned from the first call. This means that
    // queries issued against one handle are also applied to the other handles, because
    // they are the same handle. (source: php.net)

    if (!((charset == null) || charset.length() == 0)) {
      throw new UnimplementedException("oci_connect with charset");
    }

    if ((sessionMode == OCI_DEFAULT) ||
        (sessionMode == OCI_SYSOPER) ||
        (sessionMode == OCI_SYSDBA)) {
      throw new UnimplementedException("oci_connect with session mode");
    }

    return internal_oci_connect(env, true, username, password, db, charset, sessionMode);
  }

  /**
   * Uses a PHP variable for the define-step during a SELECT
   */
  public Value oci_define_by_name(Env env,
                                  @NotNull MysqliStatement stmt,
                                  @NotNull String columnName,
                                  @NotNull @Reference Value variable,
                                  @Optional("0") int type)
  {
    try {
      stmt.putByNameVariable(columnName, variable);
      return BooleanValue.TRUE;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the last error found
   */
  public String oci_error(Env env,
                          @Optional Value resource)
  {
    JdbcConnectionResource conn = null;

    if (resource == null) {
      conn = getConnection(env).validateConnection();
    } else {
      try {
        conn = ((Mysqli)(((JavaValue)resource).toJavaObject())).validateConnection();
      } catch(Exception ex) {
        log.log(Level.FINE, ex.toString(), ex);
      }

      if (conn == null) {
        try {
          MysqliStatement stmt = (MysqliStatement)(((JavaValue)resource).toJavaObject());

          conn = stmt.validateConnection();
        } catch(Exception ex) {
          log.log(Level.FINE, ex.toString(), ex);
        }
      }
    }

    return conn.getErrorMessage();
  }

  /**
   * Executes a statement
   */
  public Value oci_execute(Env env,
                           @NotNull MysqliStatement stmt,
                           @Optional("0") int mode)
  {
    try {
      if (mode == OCI_COMMIT_ON_SUCCESS) {
        throw new UnimplementedException("oci_execute with mode OCI_COMMIT_ON_SUCCESS");
      }

      stmt.execute(env);

      return BooleanValue.TRUE;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      try {
        stmt.resetBindingVariables();
      } catch (Exception ex2) {
        log.log(Level.FINE, ex2.toString(), ex2);
      }

      return BooleanValue.FALSE;
    }
  }

  /**
   * Fetches all rows of result data into an array
   */
  public Value oci_fetch_all(Env env,
                             @NotNull MysqliStatement stmt,
                             @NotNull Value output,
                             @Optional int skip,
                             @Optional int maxrows,
                             @Optional int flags)
  {
    JdbcResultResource resource = null;

    ArrayValueImpl newArray = new ArrayValueImpl();

    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      resource = new JdbcResultResource(null, stmt.getResultSet(), null);

      Value value = resource.fetchArray(JdbcResultResource.FETCH_ASSOC);

      int curr = 0;

      while(value != NullValue.NULL) {
        newArray.put(LongValue.create(curr), value);

        curr++;

        value = resource.fetchArray(JdbcResultResource.FETCH_ASSOC);
      }
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }

    return newArray;
  }

  /**
   * Returns the next row from the result data as an associative or numeric array, or both
   */
  public Value oci_fetch_array(Env env,
                               @NotNull MysqliStatement stmt,
                               @Optional("OCI_BOTH") int mode)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);
      return resource.fetchArray(mode);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the next row from the result data as an associative array
   */
  public Value oci_fetch_assoc(Env env,
                               @NotNull MysqliStatement stmt)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);
      return resource.fetchArray(JdbcResultResource.FETCH_ASSOC);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the next row from the result data as an object
   */
  public Value oci_fetch_object(Env env,
                                @NotNull MysqliStatement stmt)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);
      return resource.fetchObject(env);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the next row from the result data as a numeric array
   */
  public Value oci_fetch_row(Env env,
                             @NotNull MysqliStatement stmt)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);
      return resource.fetchArray(JdbcResultResource.FETCH_NUM);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Fetches the next row into result-buffer
   */
  public Value oci_fetch(Env env,
                         @NotNull MysqliStatement stmt)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);

      Value result = resource.fetchArray(OCI_BOTH);

      stmt.setResultBuffer(result);

      if (!(result instanceof ArrayValue)) {
        return BooleanValue.FALSE;
      }

      ArrayValue arrayValue = (ArrayValue) result;

      for (Map.Entry<String,Value> entry : stmt.getByNameVariables().entrySet()) {
        String fieldName = entry.getKey();
        Value var = entry.getValue();

        Value newValue = arrayValue.get(StringValue.create(fieldName));
        var.set(newValue);
      }

      return BooleanValue.TRUE;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Checks if the field is NULL
   */
  public Value oci_field_is_null(Env env,
                                 @NotNull MysqliStatement stmt,
                                 @NotNull Value field)
  {
    if (stmt == null)
      return BooleanValue.FALSE;

    try {
      ResultSet rs = stmt.getResultSet();
      ResultSetMetaData metaData = rs.getMetaData();

      int columnNumber = 0;

      try {
        columnNumber = field.toInt();
      } catch(Exception ex2) {
        log.log(Level.FINE, ex2.toString(), ex2);
      }

      if (columnNumber <= 0) {
        String fieldName = field.toString();

        int n = metaData.getColumnCount();

        for (int i=1; i<=n; i++) {
          if (metaData.getColumnName(i).equals(fieldName)) {
            columnNumber = i;
          }
        }
      }

      boolean isNull = metaData.isNullable(columnNumber) == ResultSetMetaData.columnNullable;
      return BooleanValue.create( isNull );

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);

      return BooleanValue.FALSE;

    }
  }

  /**
   * Returns the name of a field from the statement
   */
  public Value oci_field_name(Env env,
                              @NotNull MysqliStatement stmt,
                              @NotNull int fieldNumber)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);

      return resource.getFieldName(env, fieldNumber);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Tell the precision of a field
   */
  public Value oci_field_precision(Env env,
                                   @NotNull MysqliStatement stmt,
                                   @NotNull int field)
  {
    if (stmt == null)
      return BooleanValue.FALSE;

    try {
      ResultSet rs = stmt.getResultSet();
      ResultSetMetaData metaData = rs.getMetaData();

      int precision = metaData.getPrecision(field);
      return LongValue.create(precision);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);

      return BooleanValue.FALSE;

    }
  }

  /**
   * Tell the scale of the field
   */
  public Value oci_field_scale(Env env,
                               @NotNull MysqliStatement stmt,
                               @NotNull int field)
  {
    if (stmt == null)
      return BooleanValue.FALSE;

    try {
      ResultSet rs = stmt.getResultSet();
      ResultSetMetaData metaData = rs.getMetaData();

      int precision = metaData.getScale(field);
      return LongValue.create(precision);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);

      return BooleanValue.FALSE;

    }
  }

  /**
   * Returns field's size
   */
  public Value oci_field_size(Env env,
                              @NotNull MysqliStatement stmt,
                              @Optional Value field)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      ResultSet rs = stmt.getResultSet();
      ResultSetMetaData metaData = rs.getMetaData();

      JdbcResultResource resource = new JdbcResultResource(null, rs, null);

      int columnNumber = 0;

      try {
        columnNumber = field.toInt();
      } catch(Exception ex2) {
      }

      if (columnNumber <= 0) {
        String fieldName = field.toString();

        int n = metaData.getColumnCount();

        for (int i=1; i<=n; i++) {
          if (metaData.getColumnName(i).equals(fieldName)) {
            columnNumber = i;
          }
        }
      }

      return resource.getFieldLength(env, columnNumber);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Tell the raw Oracle data type of the field
   */
  public Value oci_field_type_raw(Env env,
                                  @NotNull MysqliStatement stmt,
                                  @Optional int field)
  {
    throw new UnimplementedException("oci_field_type_raw");
  }

  /**
   * Returns field's data type
   */
  public Value oci_field_type(Env env,
                              @NotNull MysqliStatement stmt,
                              @Optional int fieldNumber)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);

      return resource.getFieldType(env, fieldNumber);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   *  Frees all resources associated with statement or cursor
   */
  public Value oci_free_statement(Env env,
                                  @NotNull MysqliStatement stmt)
  {
    try {
      return BooleanValue.create(stmt.close());
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Enables or disables internal debug output
   */
  public void oci_internal_debug(Env env,
                                 @NotNull int onoff)
  {
    throw new UnimplementedException("oci_internal_debug");
  }

  /**
   * Copies large object
   */
  public Value oci_lob_copy(Env env,
                            @NotNull Value lobTo,
                            @NotNull Value lobFrom,
                            @Optional("-1") int length)
  {
    throw new UnimplementedException("oci_lob_copy");
  }

  /**
   * Compares two LOB/FILE locators for equality
   */
  public Value oci_lob_is_equal(Env env,
                                @NotNull Value lob1,
                                @NotNull Value lob2)
  {
    throw new UnimplementedException("oci_lob_is_equal");
  }

  /**
   * Allocates new collection object
   */
  public Value oci_new_collection(Env env,
                                  @NotNull Mysqli conn,
                                  @NotNull String tdo,
                                  @Optional String schema)
  {
    throw new UnimplementedException("oci_new_collection");
  }

  /**
   * Establishes a new connection to the Oracle server
   */
  public Value oci_new_connect(Env env,
                               @NotNull String username,
                               @NotNull String password,
                               @Optional String db,
                               @Optional String charset,
                               @Optional("0") int sessionMode)
  {
    if (!((charset == null) || charset.length() == 0)) {
      throw new UnimplementedException("oci_new_connect with charset");
    }

    if ((sessionMode == OCI_DEFAULT) ||
        (sessionMode == OCI_SYSOPER) ||
        (sessionMode == OCI_SYSDBA)) {
      throw new UnimplementedException("oci_new_connect with session mode");
    }

    return internal_oci_connect(env, false, username, password, db, charset, sessionMode);
  }

  /**
   * Allocates and returns a new cursor (statement handle)
   */
  public Value oci_new_cursor(Env env,
                              @NotNull Mysqli conn)
  {
    throw new UnimplementedException("oci_new_cursor");
  }

  /**
   * Initializes a new empty LOB or FILE descriptor
   */
  public Value oci_new_descriptor(Env env,
                                  @NotNull Mysqli conn,
                                  @Optional("-1") int type)
  {
    throw new UnimplementedException("oci_new_descriptor");
  }

  /**
   *  Returns the number of result columns in a statement
   */
  public Value oci_num_fields(Env env,
                              @NotNull MysqliStatement stmt)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);

      return LongValue.create(resource.getFieldCount());
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns number of rows affected during statement execution
   */
  public Value oci_num_rows(Env env,
                            @NotNull MysqliStatement stmt)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource = new JdbcResultResource(null, stmt.getResultSet(), null);

      return resource.getNumRows();
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Prepares Oracle statement for execution
   */
  public Value oci_parse(Env env,
                         @NotNull Mysqli conn,
       String query)
  {
    try {
      // Make the PHP query a JDBC like query replacing (:mydata -> ?) with question marks.
      // Store binding names for future reference (see oci_execute)
      String regex = ":[a-zA-Z]+";
      String jdbcQuery = query.replaceAll(regex, "?");
      MysqliStatement pstmt = conn.prepare(env, jdbcQuery);

      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(query);
      int i = 0;
      while (matcher.find()) {
        String group = matcher.group();
        pstmt.putBindingVariable(group, new Integer(++i));
      }

      return env.wrapJava(pstmt);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Changes password of Oracle's user
   */
  public Value oci_password_change(Env env,
                                   @NotNull Mysqli conn,
                                   @NotNull String username,
                                   @NotNull String oldPassword,
                                   @NotNull String newPassword)
  {
    throw new UnimplementedException("oci_password_change");
  }

  /**
   * Connect to an Oracle database using a persistent connection
   */
  public Value oci_pconnect(Env env,
                            @NotNull String username,
                            @NotNull String password,
                            @Optional String db,
                            @Optional String charset,
                            @Optional("0") int sessionMode)
  {
    if (!((charset == null) || charset.length() == 0)) {
      throw new UnimplementedException("oci_pconnect with charset");
    }

    if ((sessionMode == OCI_DEFAULT) ||
        (sessionMode == OCI_SYSOPER) ||
        (sessionMode == OCI_SYSDBA)) {
      throw new UnimplementedException("oci_pconnect with session mode");
    }

    return internal_oci_connect(env, true, username, password, db, charset, sessionMode);
  }

  /**
   * Returns field's value from the fetched row
   */
  public Value oci_result(Env env,
                          @NotNull MysqliStatement stmt,
                          @NotNull Value field)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      Value result = stmt.getResultBuffer();

      return ((ArrayValueImpl)result).get(field);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Rolls back outstanding transaction
   */
  public Value oci_rollback(Env env,
                            @NotNull Mysqli conn)
  {
    try {
      return BooleanValue.create(conn.rollback());
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns server version
   */
  public String oci_server_version(Env env,
                                   @NotNull Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.get_server_info();
  }

  /**
   * Sets number of rows to be prefetched
   */
  public Value oci_set_prefetch(Env env,
                                @NotNull MysqliStatement stmt,
                                @Optional("1") int rows)
  {
    throw new UnimplementedException("oci_set_prefetch");
  }

  /**
   * Returns the type of an OCI statement
   */
  public String oci_statement_type(Env env,
                                   @NotNull MysqliStatement stmt)
  {
    return stmt.getStatementType();
  }

  /**
   * Alias of oci_bind_by_name()
   */
  public Value ocibindbyname(Env env,
                             @NotNull MysqliStatement stmt,
                             @NotNull String variable,
                             @NotNull Value value,
                             @Optional("0") int maxLength,
                             @Optional("0") int type)
  {
    return oci_bind_by_name(env, stmt, variable, value, maxLength, type);
  }

  /**
   * Alias of oci_cancel()
   */
  public Value ocicancel(Env env,
                         @NotNull MysqliStatement stmt)
  {
    return oci_cancel(env, stmt);
  }

  /**
   * Alias of OCI-Lob->close
   */
  public Value ocicloselob(Env env,
                           @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocicloselob");
  }

  /**
   * Alias of OCI-Collection->append
   */
  public Value ocicollappend(Env env,
                             @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocicollappend");
  }

  /**
   * Alias of OCI-Collection->assign
   */
  public Value ocicollassign(Env env,
                             @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocicollassign");
  }

  /**
   * Alias of OCI-Collection->assignElem
   */
  public Value ocicollassignelem(Env env,
                                 @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocicollassignelem");
  }

  /**
   * Alias of OCI-Collection->getElem
   */
  public Value ocicollgetelem(Env env,
                              @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocicollgetelem");
  }

  /**
   * Alias of OCI-Collection->max
   */
  public Value ocicollmax(Env env,
                          @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocicollmax");
  }

  /**
   * Alias of OCI-Collection->size
   */
  public Value ocicollsize(Env env,
                           @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocicollsize");
  }

  /**
   * Alias of OCI-Collection->trim
   */
  public Value ocicolltrim(Env env,
                           @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocicolltrim");
  }

  /**
   * Alias of oci_field_is_null()
   */
  public Value ocicolumnisnull(Env env,
                               @NotNull MysqliStatement stmt,
                               @NotNull Value field)
  {
    return oci_field_is_null(env, stmt, field);
  }

  /**
   * Alias of oci_field_name()
   */
  public Value ocicolumnname(Env env,
                             @NotNull MysqliStatement stmt,
                             @NotNull int fieldNumber)
  {
    return oci_field_name(env, stmt, fieldNumber);
  }

  /**
   * Alias of oci_field_precision()
   */
  public Value ocicolumnprecision(Env env,
                                  @NotNull MysqliStatement stmt,
                                  @NotNull int field)
  {
    return oci_field_precision(env, stmt, field);
  }

  /**
   * Alias of oci_field_scale()
   */
  public Value ocicolumnscale(Env env,
                              @NotNull MysqliStatement stmt,
                              @NotNull int field)
  {
    return oci_field_scale(env, stmt, field);
  }

  /**
   * Alias of oci_field_size()
   */
  public Value ocicolumnsize(Env env,
                             @NotNull MysqliStatement stmt,
                             @Optional Value field)
  {
    return oci_field_size(env, stmt, field);
  }

  /**
   * Alias of oci_field_type()
   */
  public Value ocicolumntype(Env env,
                             @NotNull MysqliStatement stmt,
                             @Optional int fieldNumber)
  {
    return oci_field_type(env, stmt, fieldNumber);
  }

  /**
   * Alias of oci_field_type_raw()
   */
  public Value ocicolumntyperaw(Env env,
                                @NotNull MysqliStatement stmt,
                                @Optional int field)
  {
    return oci_field_type_raw(env, stmt, field);
  }

  /**
   * Alias of oci_commit()
   */
  public Value ocicommit(Env env,
                         @NotNull Mysqli conn)
  {
    return oci_commit(env, conn);
  }

  /**
   * Alias of oci_define_by_name()
   */
  public Value ocidefinebyname(Env env,
                               @NotNull MysqliStatement stmt,
                               @NotNull String columnName,
                               @NotNull Value variable,
                               @Optional("0") int type)
  {
    return oci_define_by_name(env, stmt, columnName, variable, type);
  }

  /**
   * Alias of oci_error()
   */
  public String ocierror(Env env,
                         @Optional Value resource)
  {
    return oci_error(env, resource);
  }

  /**
   * Alias of oci_execute()
   */
  public Value ociexecute(Env env,
                          @NotNull MysqliStatement stmt,
                          @Optional("0") int mode)
  {
    return oci_execute(env, stmt, mode);
  }

  /**
   * Alias of oci_fetch()
   */
  public Value ocifetch(Env env,
                        @NotNull MysqliStatement stmt)
  {
    return oci_fetch(env, stmt);
  }

  /**
   * Fetches the next row into an array
   */
  public Value ocifetchinto(Env env,
                            @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocifetchinto");
  }

  /**
   * Alias of oci_fetch_all()
   */
  public Value ocifetchstatement(Env env,
                                 @NotNull MysqliStatement stmt,
                                 @NotNull Value output,
                                 @Optional int skip,
                                 @Optional int maxrows,
                                 @Optional int flags)
  {
    return oci_fetch_all(env, stmt, output, skip, maxrows, flags);
  }

  /**
   * Alias of OCI-Collection->free
   */
  public Value ocifreecollection(Env env,
                                 @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocifreecollection");
  }

  /**
   * Alias of oci_free_statement()
   */
  public Value ocifreecursor(Env env,
                             @NotNull MysqliStatement stmt)
  {
    return oci_free_statement(env, stmt);
  }

  /**
   * Alias of OCI-Lob->free
   */
  public Value ocifreedesc(Env env,
                           @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocifreedesc");
  }

  /**
   * Alias of oci_free_statement()
   */
  public Value ocifreestatement(Env env,
                                @NotNull MysqliStatement stmt)
  {
    return oci_free_statement(env, stmt);
  }

  /**
   * Alias of oci_internal_debug()
   */
  public void ociinternaldebug(Env env,
                               @NotNull int onoff)
  {
    oci_internal_debug(env, onoff);
  }

  /**
   * Alias of OCI-Lob->load
   */
  public Value ociloadlob(Env env,
                          @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ociloadlob");
  }

  /**
   * Alias of oci_close()
   */
  public Value ocilogoff(Env env,
                         @NotNull Mysqli conn)
  {
    return oci_close(env, conn);
  }

  /**
   * Alias of oci_connect()
   */
  public Value ocilogon(Env env,
                        @NotNull String username,
                        @NotNull String password,
                        @Optional String db,
                        @Optional String charset,
                        @Optional("0") int sessionMode)
  {
    return oci_connect(env, username, password, db, charset, sessionMode);
  }

  /**
   * Alias of oci_new_collection()
   */
  public Value ocinewcollection(Env env,
                                @NotNull Mysqli conn,
                                @NotNull String tdo,
                                @Optional String schema)
  {
    return oci_new_collection(env, conn, tdo, schema);
  }

  /**
   * Alias of oci_new_cursor()
   */
  public Value ocinewcursor(Env env,
                            @NotNull Mysqli conn)
  {
    return oci_new_cursor(env, conn);
  }

  /**
   * Alias of oci_new_descriptor()
   */
  public Value ocinewdescriptor(Env env,
                                @NotNull Mysqli conn,
                                @Optional("-1") int type)
  {
    return oci_new_descriptor(env, conn, type);
  }

  /**
   * Alias of oci_new_connect()
   */
  public Value ocinlogon(Env env,
                         @NotNull String username,
                         @NotNull String password,
                         @Optional String db,
                         @Optional String charset,
                         @Optional("0") int sessionMode)
  {
    return oci_new_connect(env, username, password, db, charset, sessionMode);
  }

  /**
   * Alias of oci_num_fields()
   */
  public Value ocinumcols(Env env,
                          @NotNull MysqliStatement stmt)
  {
    return oci_num_fields(env, stmt);
  }

  /**
   * Alias of oci_parse()
   */
  public Value ociparse(Env env,
                        @NotNull Mysqli conn,
                        @NotNull String query)
  {
    return oci_parse(env, conn, query);
  }

  /**
   * Alias of oci_pconnect()
   */
  public Value ociplogon(Env env,
                         @NotNull String username,
                         @NotNull String password,
                         @Optional String db,
                         @Optional String charset,
                         @Optional("0") int sessionMode)
  {
    return oci_pconnect(env, username, password, db, charset, sessionMode);
  }

  /**
   * Alias of oci_result()
   */
  public Value ociresult(Env env,
                         @NotNull MysqliStatement stmt,
                         @NotNull Value field)
  {
    return oci_result(env, stmt, field);
  }

  /**
   * Alias of oci_rollback()
   */
  public Value ocirollback(Env env,
                           @NotNull Mysqli conn)
  {
    return oci_rollback(env, conn);
  }

  /**
   * Alias of oci_num_rows()
   */
  public Value ocirowcount(Env env,
                           @NotNull MysqliStatement stmt)
  {
    return oci_num_rows(env, stmt);
  }

  /**
   * Alias of OCI-Lob->save
   */
  public Value ocisavelob(Env env,
                          @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocisavelob");
  }

  /**
   * Alias of OCI-Lob->import
   */
  public Value ocisavelobfile(Env env,
                              @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ocisavelobfile");
  }

  /**
   * Alias of oci_server_version()
   */
  public String ociserverversion(Env env,
                                 @NotNull Mysqli conn)
  {
    return oci_server_version(env, conn);
  }

  /**
   * Alias of oci_set_prefetch()
   */
  public Value ocisetprefetch(Env env,
                              @NotNull MysqliStatement stmt,
                              @Optional("1") int rows)
  {
    return oci_set_prefetch(env, stmt, rows);
  }

  /**
   * Alias of oci_statement_type()
   */
  public String ocistatementtype(Env env,
                                 @NotNull MysqliStatement stmt)
  {
    return oci_statement_type(env, stmt);
  }

  /**
   * Alias of OCI-Lob->export
   */
  public Value ociwritelobtofile(Env env,
                                 @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ociwritelobtofile");
  }

  /**
   * Alias of OCI-Lob->writeTemporary
   */
  public Value ociwritetemporarylob(Env env,
                                    @NotNull Mysqli conn)
  {
    throw new UnimplementedException("ociwritetemporarylob");
  }

  private Mysqli getConnection(Env env)
  {
    Mysqli conn = null;

    Object connectionInfo[] = (Object[])env.getSpecialValue("caucho.oracle");

    if (connectionInfo != null) {
      // Reuse the cached connection
      conn = (Mysqli)connectionInfo[1];
      return conn;
    }

    String driver = "oracle.jdbc.driver.OracleDriver";
    String url = "jdbc:oracle:thin:@localhost:1521";

    conn = new Mysqli(env, "localhost", "", "", "", 1521, "", 0, driver, url);

    env.setSpecialValue("caucho.oracle", conn);

    return conn;
  }

  private Value internal_oci_connect(Env env,
                                     boolean reuseConnection,
                                     String username,
                                     String password,
                                     String db,
                                     String charset,
                                     int sessionMode)
  {
    String host = "localhost";
    int port = 1521;

    String driver = "org.postgresql.Driver";

    String url;

    if (db.indexOf("//") == 0) {
      // db is the url itself: "//db_host[:port]/database_name"
      url = "jdbc:oracle:thin:@" + db.substring(2);
      url = url.replace('/', ':');
    } else {
      url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + db;
    }

    Mysqli mysqli = null;

    Object connectionInfo[] = (Object[])env.getSpecialValue("caucho.oracle");

    if (reuseConnection && (connectionInfo != null) && url.equals(connectionInfo[0])) {
      // Reuse the cached connection
      mysqli = (Mysqli) connectionInfo[1];
    } else {
      mysqli = new Mysqli(env, host, username, password, db, port, "", 0,
			  driver, url);

      connectionInfo = new Object[2];
      connectionInfo[0] = url;
      connectionInfo[1] = mysqli;

      env.setSpecialValue("caucho.oracle", connectionInfo);
    }

    Value value = env.wrapJava(mysqli);

    return value;
  }
}
