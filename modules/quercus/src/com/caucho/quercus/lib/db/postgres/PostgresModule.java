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

package com.caucho.quercus.lib.db.postgres;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.caucho.util.Log;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.quercus.lib.db.JdbcConnectionResource;

import com.caucho.quercus.lib.db.JdbcResultResource;
import com.caucho.quercus.lib.db.JdbcTableMetaData;
import com.caucho.quercus.lib.db.JdbcColumnMetaData;

//@todo create a Postgresi and PostgresqliResult instead
import com.caucho.quercus.lib.db.mysql.Mysqli;
import com.caucho.quercus.lib.db.mysql.MysqliResult;
import com.caucho.quercus.lib.db.mysql.MysqliStatement;

// Do not add new compile dependencies (using reflection instead)
// import org.postgresql.largeobject.*;
import java.lang.reflect.*;
import java.io.*;

/**
 * PHP postgres routines.
 */
public class PostgresModule extends AbstractQuercusModule {

  private static final Logger log = Log.open(PostgresModule.class);

  public static final int PGSQL_ASSOC = 0x01;
  public static final int PGSQL_NUM = 0x02;
  public static final int PGSQL_BOTH = 0x03;
  public static final int PGSQL_CONNECT_FORCE_NEW = 0x04;
  public static final int PGSQL_CONNECTION_BAD = 0x05;
  public static final int PGSQL_CONNECTION_OK = 0x06;
  public static final int PGSQL_SEEK_SET = 0x07;
  public static final int PGSQL_SEEK_CUR = 0x08;
  public static final int PGSQL_SEEK_END = 0x09;
  public static final int PGSQL_EMPTY_QUERY = 0x0A;
  public static final int PGSQL_COMMAND_OK = 0x0B;
  public static final int PGSQL_TUPLES_OK = 0x0C;
  public static final int PGSQL_COPY_OUT = 0x0D;
  public static final int PGSQL_COPY_IN = 0x0E;
  public static final int PGSQL_BAD_RESPONSE = 0x0F;
  public static final int PGSQL_NONFATAL_ERROR = 0x10;
  public static final int PGSQL_FATAL_ERROR = 0x11;
  public static final int PGSQL_TRANSACTION_IDLE = 0x12;
  public static final int PGSQL_TRANSACTION_ACTIVE = 0x13;
  public static final int PGSQL_TRANSACTION_INTRANS = 0x14;
  public static final int PGSQL_TRANSACTION_INERROR = 0x15;
  public static final int PGSQL_TRANSACTION_UNKNOWN = 0x16;
  public static final int PGSQL_DIAG_SEVERITY = 0x17;
  public static final int PGSQL_DIAG_SQLSTATE = 0x18;
  public static final int PGSQL_DIAG_MESSAGE_PRIMARY = 0x19;
  public static final int PGSQL_DIAG_MESSAGE_DETAIL = 0x20;
  public static final int PGSQL_DIAG_MESSAGE_HINT = 0x21;
  public static final int PGSQL_DIAG_STATEMENT_POSITION = 0x22;
  public static final int PGSQL_DIAG_INTERNAL_POSITION = 0x23;
  public static final int PGSQL_DIAG_INTERNAL_QUERY = 0x24;
  public static final int PGSQL_DIAG_CONTEXT = 0x25;
  public static final int PGSQL_DIAG_SOURCE_FILE = 0x26;
  public static final int PGSQL_DIAG_SOURCE_LINE = 0x27;
  public static final int PGSQL_DIAG_SOURCE_FUNCTION = 0x28;
  public static final int PGSQL_ERRORS_TERSE = 0x29;
  public static final int PGSQL_ERRORS_DEFAULT = 0x2A;
  public static final int PGSQL_ERRORS_VERBOSE = 0x2B;
  public static final int PGSQL_STATUS_LONG = 0x2C;
  public static final int PGSQL_STATUS_STRING = 0x2D;
  public static final int PGSQL_CONV_IGNORE_DEFAULT = 0x2E;
  public static final int PGSQL_CONV_FORCE_NULL = 0x2F;

  public PostgresModule()
  {
  }

  /**
   * Returns true for the postgres extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "postgres" };
  }

  /**
   * Returns number of affected records (tuples)
   */
  public int pg_affected_rows(Env env,
                              @NotNull MysqliResult result)
  {
    try {

      return result.validateResult().getAffectedRows(); // num_rows().toInt();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return 0;
    }
  }

  /**
   * Cancel an asynchronous query
   */
  public boolean pg_cancel_query(Env env,
                                 @NotNull Mysqli conn)
  {
    try {
      conn.setAsynchronousResult(null);

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Gets the client encoding
   */
  public @ReturnNullAsFalse Object pg_client_encoding(Env env,
                                                      @Optional Mysqli conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      return conn.client_encoding();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Closes a PostgreSQL connection
   */
  public boolean pg_close(Env env,
                          @Optional Mysqli conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      if (conn != null) {

        if (conn == getConnection(env))
          env.removeSpecialValue("caucho.postgres");

        conn.close(env);

        return true;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Open a PostgreSQL connection
   */
  public @ReturnNullAsFalse Object pg_connect(Env env,
                                              String connectionString,
                                              @Optional int connectionType)
  {
    try {
      String host = "localhost";
      int port = 5432;
      String dbName = "";
      String userName = "";
      String password = "";

      String s = connectionString.trim();

      String sp[];

      sp = s.split("(host=)");

      if (sp.length >= 2)
        host = sp[1].replaceAll("\\s(.*)$", "");

      sp = s.split("(port=)");

      if (sp.length >= 2) {
        String portS = sp[1].replaceAll("\\s(.*)$", "");
        try {
          port = Integer.parseInt(portS);
        } catch (Exception ex) {
        }
      }

      sp = s.split("(dbname=)");

      if (sp.length >= 2)
        dbName = sp[1].replaceAll("\\s(.*)$", "");

      sp = s.split("(user=)");

      if (sp.length >= 2)
        userName = sp[1].replaceAll("\\s(.*)$", "");

      sp = s.split("(password=)");

      if (sp.length >= 2)
        password = sp[1].replaceAll("\\s(.*)$", "");

      String driver = "org.postgresql.Driver";
      String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

      Mysqli mysqli = new Mysqli(env, host, userName, password, dbName, port, "", driver, url);

      env.setSpecialValue("caucho.postgres", mysqli);

      return mysqli;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get connection is busy or not
   */
  public boolean pg_connection_busy(Env env,
                                    @NotNull Mysqli conn)
  {
    throw new UnimplementedException("pg_connection_busy");
  }

  /**
   * Reset connection (reconnect)
   */
  public boolean pg_connection_reset(Env env,
                                     @NotNull Mysqli conn)
  {
    throw new UnimplementedException("pg_connection_reset");
  }

  /**
   * Get connection status
   */
  public int pg_connection_status(Env env,
                                  @NotNull Mysqli conn)
  {
    try {

      boolean ping = pg_ping(env, conn);

      return ping ? PGSQL_CONNECTION_OK : PGSQL_CONNECTION_BAD;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return PGSQL_CONNECTION_BAD;
    }
  }

  /**
   * Convert associative array values into suitable for SQL statement
   */
  public Value pg_convert(Env env,
                          @NotNull Mysqli conn,
                          String tableName,
                          Value assocArray,
                          @Optional int options)
  {
    throw new UnimplementedException("pg_convert");
  }

  /**
   * Insert records into a table from an array
   */
  public boolean pg_copy_from(Env env,
                              @NotNull Mysqli conn,
                              String tableName,
                              Value rows,
                              @Optional("") String delimiter,
                              @Optional("") String nullAs)
  {
    try {
      // The character that makes the special char: '\u0009' .. '\u001F'
      String stringMap[] = new String[] {"t",  // u0009 (horizontal tab)
                                         "n",  // u000A (new line)
                                         "v",  // u000B (vertical tab)
                                         "f",  // u000C (form feed)
                                         "r",  // u000D (carriage return)
                                         "\u000E-\u001B",          //@todo
                                         "\u001Cfileseparator",    //@todo
                                         "\u001Dgroupseparator",   //@todo
                                         "\u001Erecordseparator",  //@todo
                                         "\u001Funitseparator"};   //@todo

      Pattern pattern = Pattern.compile("(\\p{Cntrl}|\\\\|\\.|\\-|\\*|\\+|\\?|\\]|\\[|\\^|\\{|\\}|\\(|\\)|\\|)");

      String delimiterRegex;
      if (delimiter.equals("")) {
        delimiter = "\t";
        delimiterRegex = "\\t";
      } else {
        delimiterRegex = escapeCharsToRegex(stringMap, pattern, delimiter);
      }

      String nullAsRegex;
      if (nullAs.equals("")) {
        nullAs = "\n";
        nullAsRegex = "\\n";
      } else {
        nullAsRegex = escapeCharsToRegex(stringMap, pattern, nullAs);
      }

      ArrayValueImpl newArray = (ArrayValueImpl) rows;
      int nasize = newArray.size();

      for (int i=0; i<nasize; i++) {
        String values = newArray.get(LongValue.create(i)).toString();
        StringBuffer stringBuffer = new StringBuffer();
        // For testing only:
        // String values = "\n\tNUMBER1stcolumn\t\n\t\n\tNUMBER2ndcolumn\tNUMBER3rdcolumn\tNUMBER4thcolumn\t\n";
        values = "'"+values.replaceAll(delimiterRegex, "','")+"'";
        values = values.replaceAll(",'"+nullAsRegex+"',", ",NULL,");
        // We need to call it twice because the replaceAll(regexp) would not
        // replace adjacent ",'\\n'," matches.
        values = values.replaceAll(",'"+nullAsRegex+"',", ",NULL,");
        values = values.replaceAll("^'"+nullAsRegex+"',", "NULL,");
        values = values.replaceAll(",'"+nullAsRegex+"'$", ",NULL");

        String query = "INSERT INTO "+tableName+" VALUES("+values+")";
        pg_query(env, conn, query);
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Copy a table to an array
   */
  public Value pg_copy_to(Env env,
                          @NotNull Mysqli conn,
                          String tableName,
                          @Optional("") String delimiter,
                          @Optional("") String nullAs)
  {
    try {
      if (delimiter.equals("")) {
        delimiter = "\t";
      }

      if (nullAs.equals("")) {
        nullAs = "\n";
      }

      Object value = pg_query(env, conn, "SELECT * FROM " + tableName);

      MysqliResult result = (MysqliResult) value;

      ArrayValueImpl newArray = new ArrayValueImpl();

      value = result.fetch_array(PGSQL_BOTH);

      if (value != NullValue.NULL) {
        int curr = 0;

        do {
          ArrayValueImpl arr = (ArrayValueImpl)value;
          int count = arr.size() / 2;
          for(int i=0; i<count; i++) {
            Value v = newArray.get(LongValue.create(curr));
            if (!v.toString().equals("")) {
              v = StringValue.create(v.toString() + delimiter);
            }
            LongValue lv = LongValue.create(i);
            Value fieldValue = arr.get(lv);
            String fieldString;
            if (fieldValue instanceof NullValue) {
              fieldString = nullAs;
            } else {
              fieldString = fieldValue.toString();
            }
            newArray.put(LongValue.create(curr),
                         StringValue.create(v.toString() + fieldString));
          }

          curr++;

          value = result.fetch_array(PGSQL_BOTH);
        } while (value != NullValue.NULL);
      }

      return newArray;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Get the database name
   */
  public Value pg_dbname(Env env,
                         @Optional Mysqli conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      return StringValue.create(conn.get_dbname());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Deletes records
   */
  public Value pg_delete(Env env,
                         @NotNull Mysqli conn,
                         String tableName,
                         Value assocArray,
                         @Optional int options)
  {
    // @todo from php.net: this function is EXPERIMENTAL.
    // @This function is EXPERIMENTAL. The behaviour of this function,
    // the name of this function, and anything else documented about this function
    // may change without notice in a future release of PHP.
    // Use this function at your own risk.

    throw new UnimplementedException("pg_delete");
  }

  /**
   * Sync with PostgreSQL backend
   */
  public boolean pg_end_copy(Env env, @Optional Mysqli conn)
  {
    throw new UnimplementedException("pg_end_copy");
  }

  /**
   * Escape a string for insertion into a bytea field
   */
  public String pg_escape_bytea(Env env,
                                String data)
  {
    throw new UnimplementedException("pg_escape_bytea");
  }

  /**
   * Escape a string for insertion into a text field
   */
  public Value pg_escape_string(Env env,
                                String data)
  {
    try {
      Mysqli conn = getConnection(env);

      if (conn == null)
        return BooleanValue.FALSE;

      return conn.real_escape_string(data);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Sends a request to execute a prepared statement with given parameters,
   * and waits for the result
   */
  public @ReturnNullAsFalse Object pg_execute(Env env,
                                              @NotNull Mysqli conn,
                                              String stmtName,
                                              Value params)
  {
    try {
      MysqliStatement pstmt = conn.getStatement(stmtName);


      ArrayValueImpl arr = (ArrayValueImpl)params;
      int sz = arr.size();

      for (int i=0; i<sz; i++) {
        String p = arr.get(LongValue.create(i)).toString();
      }

      char buf[] = new char[sz];
      for (int i=0; i<sz; i++)
        buf[i] = 's';
      String types = new String(buf);

      Value value[] = arr.getValueArray(env);
      pstmt.bind_param(env, types, value);

      if (!pstmt.execute(env))
        return null;

      if (pstmt.getStatementType().equals("SELECT")) {
        return new MysqliResult(new JdbcResultResource(null, pstmt.getResultSet(), null));
      } else {
        return pstmt;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Fetches all rows in a particular result column as an array
   */
  public Value pg_fetch_all_columns(Env env,
                                    @NotNull MysqliResult result,
                                    @Optional("0") int column)
  {
    try {
      ArrayValueImpl newArray = new ArrayValueImpl();

      Value row = result.fetch_row();

      int curr = 0;

      while(row != NullValue.NULL) {
        newArray.put(LongValue.create(curr),
                     ((ArrayValueImpl)row).get(LongValue.create(column)));

        curr++;

        row = result.fetch_row();
      }

      return newArray;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Fetches all rows from a result as an array
   */
  public Value pg_fetch_all(Env env,
                            @NotNull MysqliResult result)
  {
    try {
      ArrayValueImpl newArray = new ArrayValueImpl();

      Value value = result.fetch_assoc();

      int curr = 0;

      while(value != NullValue.NULL) {
        newArray.put(LongValue.create(curr), value);

        curr++;

        value = result.fetch_assoc();
      }

      return newArray;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Fetch a row as an array
   */
  public Value pg_fetch_array(Env env,
                              @NotNull MysqliResult result,
                              @Optional("-1") Value row,
                              @Optional("PGSQL_BOTH") int resultType)
  {
    try {
      Value value = BooleanValue.FALSE;

      if (result == null)
        return value;

      if ((row != null) && (!row.equals(NullValue.NULL)) && (row.toInt() >= 0)) {
        result.data_seek(env, row.toInt());
      }

      value = result.fetch_array(resultType);

      if (value.equals(NullValue.NULL)) {
        value = BooleanValue.FALSE;
      }

      return value;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Fetch a row as an associative array
   */
  public Value pg_fetch_assoc(Env env,
                              @NotNull MysqliResult result,
                              @Optional("-1") Value row)
  {
    try {

      if ((row != null) && (!row.equals(NullValue.NULL)) && (row.toInt() >= 0)) {
        result.data_seek(env, row.toInt());
      }

      return result.fetch_assoc();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Fetch a row as an object
   */
  public Value pg_fetch_object(Env env,
                               @NotNull MysqliResult result,
                               @Optional("-1") Value row,
                               @Optional int resultType)
  {
    try {

      //@todo use optional resultType
      if ((row != null) && (!row.equals(NullValue.NULL)) && (row.toInt() >= 0)) {
        result.data_seek(env, row.toInt());
      }

      return result.fetch_object(env);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns values from a result resource
   */
  public @ReturnNullAsFalse Object pg_fetch_result(Env env,
                                                   @NotNull MysqliResult result,
                                                   int row,
                                                   @Optional("-1") int fieldNumber)
  {
    try {

      // Handle the case: optional row with mandatory fieldNumber.
      if (fieldNumber < 0) {
        fieldNumber = row;
        row = -1;
      }

      if (row >= 0) {
        result.data_seek(env, row);
      }

      Value fetchRow = result.fetch_row();

      return ((ArrayValueImpl)fetchRow).get(LongValue.create(fieldNumber)).toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get a row as an enumerated array
   */
  public Value pg_fetch_row(Env env,
                            @NotNull MysqliResult result,
                            @Optional("-1") Value row)
  {
    try {

      if ((row != null) && (!row.equals(NullValue.NULL)) && (row.toInt() >= 0)) {
        result.data_seek(env, row.toInt());
      }

      return result.fetch_row();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Test if a field is SQL NULL
   */
  public Value pg_field_is_null(Env env,
                                @NotNull MysqliResult result,
                                @Optional("-1") int row,
                                Value mixedField)
  {
    throw new UnimplementedException("pg_field_is_null");
  }

  /**
   * Returns the name of a field
   */
  public Value pg_field_name(Env env,
                             @NotNull MysqliResult result,
                             int fieldNumber)
  {
    try {

      return result.fetch_field_name(env, fieldNumber);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the field number of the named field
   */
  public int pg_field_num(Env env,
                          @NotNull MysqliResult result,
                          String fieldName)
  {
    int columnNumber = -1;

    try {

      ResultSetMetaData metaData = result.validateResult().getMetaData();

      int n = metaData.getColumnCount();

      for (int i=1; i<=n; i++) {
        if (metaData.getColumnName(i).equals(fieldName)) {
          columnNumber = i-1;
        }
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return columnNumber;
  }

  /**
   * Returns the printed length
   */
  public @ReturnNullAsFalse Object pg_field_prtlen(Env env,
                                                   @NotNull MysqliResult result,
                                                   Value rowNumber,
                                                   @Optional("-1") Value fieldNameOrNumber)
  {
    try {
      int row = rowNumber.toInt();

      if (fieldNameOrNumber.toString().equals("-1")) {
        fieldNameOrNumber = rowNumber;
        row = -1;
      }

      int fieldNumber = -1;

      try {
        fieldNumber = Integer.parseInt(fieldNameOrNumber.toString());
      } catch (Exception ex) {
      }

      if (fieldNumber < 0) {
        fieldNumber = pg_field_num(env, result, fieldNameOrNumber.toString());
      }

      Object object = pg_fetch_result(env, result, row, fieldNumber);

      // Step the cursor back to the original position
      // See php/4325
      result.validateResult().getResultSet().relative(-1);

      return object.toString().length();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Returns the internal storage size of the named field
   */
  public Value pg_field_size(Env env,
                             @NotNull MysqliResult result,
                             int fieldNumber)
  {
    try {

      Value value = pg_field_type(env, result, fieldNumber);

      if (value.toString().equals("varchar")) {
        value = LongValue.create(-1);
      } else {
        value = result.fetch_field_length(env, fieldNumber);
      }

      return value;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the name or oid of the tables field
   */
  public int pg_field_table(Env env,
                            @NotNull MysqliResult result,
                            int fieldNumber,
                            boolean oidOnly)
  {
    throw new UnimplementedException("pg_field_table");
  }

  /**
   * Returns the type ID (OID) for the corresponding field number
   */
  public int pg_field_type_oid(Env env,
                               @NotNull MysqliResult result,
                               int fieldNumber)
  {
    throw new UnimplementedException("pg_field_type_oid");
  }

  /**
   * Returns the type name for the corresponding field number
   */
  public Value pg_field_type(Env env,
                             @NotNull MysqliResult result,
                             int fieldNumber)
  {
    try {

      Value value = result.fetch_field_type(env, fieldNumber);

      if (value instanceof StringValue) {
        if (value.toString().equals("string")) {
          value = StringValue.create("varchar");
        }
      }

      return value;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Free result memory
   */
  public boolean pg_free_result(Env env,
                                @NotNull MysqliResult result)
  {
    try {

      result.close();
      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Gets SQL NOTIFY message
   */
  public Value pg_get_notify(Env env,
                             @NotNull Mysqli conn,
                             @Optional int resultType)
  {
    throw new UnimplementedException("pg_get_notify");
  }

  /**
   * Gets the backend's process ID
   */
  public int pg_get_pid(Env env,
                        @NotNull Mysqli conn)
  {
    throw new UnimplementedException("pg_get_pid");
  }

  /**
   * Get asynchronous query result
   */
  public @ReturnNullAsFalse Object pg_get_result(Env env,
                                                 @Optional Mysqli conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      MysqliResult result = conn.getAsynchronousResult();

      conn.setAsynchronousResult(null);

      return result;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the host name associated with the connection
   */
  public @ReturnNullAsFalse Object pg_host(Env env,
                                           @Optional Mysqli conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      return conn.get_host_name();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Insert array into table
   */
  public boolean pg_insert(Env env,
                           @NotNull Mysqli conn,
                           String tableName,
                           Value assocArray,
                           @Optional int options)
  {
    try {

      //@todo use options

      ArrayValueImpl newArray = (ArrayValueImpl) assocArray;
      int nasize = newArray.size();

      Value keyArr[] = newArray.getKeyArray();

      String names = "";
      String values = "";
      for (int i=0; i<nasize; i++) {
        Value k = keyArr[i];
        Value v = newArray.get(k);
        values = values + "','" + v.toString();
        names = names + "," + k.toString();
      }

      names = names.substring(1);
      values = values.substring(2) + "'";

      String query = "INSERT INTO "+tableName+"("+names+") VALUES("+values+")";

      pg_query(env, conn, query);

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Get the last error message string of a connection
   */
  public @ReturnNullAsFalse Object pg_last_error(Env env,
                                                 @Optional Mysqli conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      return conn.error();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the last notice message from PostgreSQL server
   */
  public @ReturnNullAsFalse Object pg_last_notice(Env env,
                                                  @NotNull Mysqli conn)
  {
    try {

      return conn.validateConnection().getWarnings().toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the last row's OID
   *
   * @todo Note that:
   * - OID is a unique id. It will not work if the table was created with "No oid".
   * - MySql's "mysql_insert_id" receives the conection handler as argument but
   * PostgreSQL's "pg_last_oid" uses the result handler.
   */
  public @ReturnNullAsFalse Object pg_last_oid(Env env,
                                               MysqliResult result)
  {
    try {

      Statement stmt = result.validateResult().getStatement();

      stmt = ((com.caucho.sql.UserStatement)stmt).getStatement();

      stmt = ((com.caucho.sql.spy.SpyStatement)stmt).getStatement();

      Class c = Class.forName("org.postgresql.jdbc2.AbstractJdbc2Statement");

      Method m = c.getDeclaredMethod("getLastOID", null);

      int oid = Integer.parseInt(m.invoke(stmt, new Object[]{}).toString());

      if (oid > 0)
        return ""+oid;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Close a large object
   */
  public boolean pg_lo_close(Env env,
                             Object largeObject)
  {
    try {

      Class c = Class.forName("org.postgresql.largeobject.LargeObject");

      Method m = c.getDeclaredMethod("close", null);

      m.invoke(largeObject, new Object[]{});
      // largeObject.close();

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Create a large object
   */
  public @ReturnNullAsFalse Object pg_lo_create(Env env,
                                                @Optional Mysqli conn)
  {
    try {

      int oid = -1;

      if (conn == null)
        conn = getConnection(env);

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class c = Class.forName("org.postgresql.PGConnection");

      Method m = c.getDeclaredMethod("getLargeObjectAPI", null);

      Object userconn = conn.validateConnection().getConnection();

      Object spyconn = ((com.caucho.sql.UserConnection)userconn).getConnection();

      Object pgconn = ((com.caucho.sql.spy.SpyConnection)spyconn).getConnection();

      // Large Objects may not be used in auto-commit mode.
      ((java.sql.Connection)pgconn).setAutoCommit(false);

      lobManager = m.invoke(pgconn, new Object[]{});
      // lobManager = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

      c = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      m = c.getDeclaredMethod("create", null);

      Object oidObj = m.invoke(lobManager, new Object[]{});

      oid = Integer.parseInt(oidObj.toString());

      // oid = lobManager.create();

      return new Integer(oid);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Export a large object to a file
   */
  public boolean pg_lo_export(Env env,
                              @NotNull Mysqli conn,
                              int oid,
                              String pathName)
  {
    try {

      //@todo conn should be optional

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class c = Class.forName("org.postgresql.PGConnection");

      Method m = c.getDeclaredMethod("getLargeObjectAPI", null);

      Object userconn = conn.validateConnection().getConnection();

      Object spyconn = ((com.caucho.sql.UserConnection)userconn).getConnection();

      Object pgconn = ((com.caucho.sql.spy.SpyConnection)spyconn).getConnection();

      lobManager = m.invoke(pgconn, new Object[]{});
      // lobManager = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

      c = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      m = c.getDeclaredMethod("open", new Class[]{Integer.TYPE});

      Object lobj = m.invoke(lobManager, new Object[]{oid});

      c = Class.forName("org.postgresql.largeobject.LargeObject");

      m = c.getDeclaredMethod("getInputStream", null);

      Object isObj = m.invoke(lobj, new Object[]{});

      InputStream is = (InputStream)isObj;

      // Open the file
      File file = new File(pathName);
      FileOutputStream fos = new FileOutputStream(file);

      // copy the data from the large object to the file
      byte buf[] = new byte[2048];
      int s = 0;
      while ((s = is.read(buf, 0, 2048)) > 0) {
        fos.write(buf, 0, s);
      }

      fos.close();
      is.close();

      // Close the large object
      m = c.getDeclaredMethod("close", null);

      m.invoke(lobj, new Object[]{});

      //lobj.close();

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Import a large object from file
   */
  public @ReturnNullAsFalse Object pg_lo_import(Env env,
                                                @NotNull Mysqli conn,
                                                String pathName)
  {
    try {

      //@todo conn should be optional

      int oid = ((Integer)pg_lo_create(env, conn)).intValue();
      Object largeObject = pg_lo_open(env, conn, oid, "w");

      String data = "";

      // Open the file
      File file = new File(pathName);
      DataInputStream fis = new DataInputStream(new FileInputStream(file));

      // copy the data from the large object to the file
      byte buf[] = new byte[(int)file.length()];

      fis.readFully(buf);

      data = new String(buf);

      fis.close();

      pg_lo_write(env, largeObject, data, 0);
      pg_lo_close(env, largeObject);

      return new Integer(oid);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Open a large object
   */
  public @ReturnNullAsFalse Object pg_lo_open(Env env,
                                              @NotNull Mysqli conn,
                                              int oid,
                                              String mode)
  {
    try {

      Object largeObject = null;

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class c = Class.forName("org.postgresql.PGConnection");

      Method m = c.getDeclaredMethod("getLargeObjectAPI", null);

      Object userconn = conn.validateConnection().getConnection();

      Object spyconn = ((com.caucho.sql.UserConnection)userconn).getConnection();

      Object pgconn = ((com.caucho.sql.spy.SpyConnection)spyconn).getConnection();

      lobManager = m.invoke(pgconn, new Object[]{});
      // lobManager = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

      c = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      m = c.getDeclaredMethod("open", new Class[]{Integer.TYPE, Integer.TYPE});

      boolean write = mode.indexOf("w") >= 0;
      boolean read = mode.indexOf("r") >= 0;

      int modeREAD = c.getDeclaredField("READ").getInt(null);
      int modeREADWRITE = c.getDeclaredField("READWRITE").getInt(null);
      int modeWRITE = c.getDeclaredField("WRITE").getInt(null);

      int intMode = modeREAD;

      if (read) {
        if (write) {
          intMode = modeREADWRITE;
        }
      } else if (write) {
        intMode = modeWRITE;
      }

      largeObject = m.invoke(lobManager, new Object[]{oid, intMode});
      // LargeObject largeObject = lobManager.open(oid, mode);

      return largeObject;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Reads an entire large object and send straight to browser
   */
  public int pg_lo_read_all(Env env,
                            Object largeObject)
  {
    //@todo pg_lo_read_all() reads a large object and passes it straight through
    // to the browser after sending all pending headers. Mainly intended for sending
    // binary data like images or sound.

    throw new UnimplementedException("pg_lo_read_all");

    /*
      InputStream in = largeObject.getInputStream();

      byte buf[] = new byte[2048];
      int s, tl = 0;
      while ((s = fis.read(buf, 0, 2048)) > 0)
      {
      obj.write(buf, 0, s);
      tl += s;
      }

      return 0;
    */
  }

  /**
   * Read a large object
   */
  public @ReturnNullAsFalse Object pg_lo_read(Env env,
                                              Object largeObject,
                                              @Optional("8192") int len)
  {
    try {

      Class c = Class.forName("org.postgresql.largeobject.LargeObject");

      Method m = c.getDeclaredMethod("read", new Class[]{Integer.TYPE});

      byte data[] = (byte[])m.invoke(largeObject, new Object[]{len});

      return new String(data);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Seeks position within a large object
   */
  public boolean pg_lo_seek(Env env,
                            Object largeObject,
                            int offset,
                            @Optional int whence)
  {
    try {

      Class c = Class.forName("org.postgresql.largeobject.LargeObject");

      int seekSET = c.getDeclaredField("SEEK_SET").getInt(null);
      int seekEND = c.getDeclaredField("SEEK_END").getInt(null);
      int seekCUR = c.getDeclaredField("SEEK_CUR").getInt(null);

      switch (whence) {
      case PGSQL_SEEK_SET:
        whence = seekSET;
        break;
      case PGSQL_SEEK_END:
        whence = seekEND;
        break;
      default:
        whence = seekCUR;
      }

      Method m = c.getDeclaredMethod("seek", new Class[]{Integer.TYPE,Integer.TYPE});

      m.invoke(largeObject, new Object[]{offset,whence});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns current seek position a of large object
   */
  public int pg_lo_tell(Env env,
                        Object largeObject)
  {
    try {

      Class c = Class.forName("org.postgresql.largeobject.LargeObject");

      Method m = c.getDeclaredMethod("tell", null);

      Object obj = m.invoke(largeObject, new Object[]{});

      return Integer.parseInt(obj.toString());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Delete a large object
   */
  public boolean pg_lo_unlink(Env env,
                              @NotNull Mysqli conn,
                              int oid)
  {
    try {

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class c = Class.forName("org.postgresql.PGConnection");

      Method m = c.getDeclaredMethod("getLargeObjectAPI", null);

      Object userconn = conn.validateConnection().getConnection();

      Object spyconn = ((com.caucho.sql.UserConnection)userconn).getConnection();

      Object pgconn = ((com.caucho.sql.spy.SpyConnection)spyconn).getConnection();

      lobManager = m.invoke(pgconn, new Object[]{});
      // lobManager = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

      c = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      m = c.getDeclaredMethod("unlink", new Class[]{Integer.TYPE});

      m.invoke(lobManager, new Object[]{oid});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Write to a large object
   */
  public @ReturnNullAsFalse Object pg_lo_write(Env env,
                                               @NotNull Object largeObject,
                                               String data,
                                               @Optional int len)
  {
    try {

      if (len <= 0) {
        len = data.length();
      }

      int written = len;

      Class c = Class.forName("org.postgresql.largeobject.LargeObject");

      Method m = c.getDeclaredMethod("write",
                                     new Class[]{byte[].class, Integer.TYPE, Integer.TYPE});

      m.invoke(largeObject, new Object[]{data.getBytes(), 0, len});
      // largeObject.write(data.getBytes(), 0, len);

      return new Integer(written);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get meta data for table
   */
  public Value pg_meta_data(Env env,
                            @NotNull Mysqli conn,
                            String tableName)
  {
    try {

      String metaQuery = "SELECT a.attnum,t.typname,a.attlen,t.typnotnull,t.typdefault,a.attndims FROM pg_class c, pg_attribute a, pg_type t WHERE c.relname='"+tableName+"' AND a.attnum > 0 AND a.attrelid = c.oid AND a.atttypid = t.oid ORDER BY a.attnum";

      Object value = pg_query(env, conn, metaQuery);

      MysqliResult result = (MysqliResult) value;

      return pg_fetch_all(env, result);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the number of fields in a result
   */
  public int pg_num_fields(Env env,
                           @NotNull MysqliResult result)
  {
    try {

      return result.num_fields();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Returns the number of rows in a result
   */
  public int pg_num_rows(Env env,
                         @NotNull MysqliResult result)
  {
    try {

      return result.num_rows().toInt();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Get the options associated with the connection
   */
  public String pg_options(Env env,
                           @Optional Mysqli conn)
  {
    throw new UnimplementedException("pg_options");
  }

  /**
   * Looks up a current parameter setting of the server
   */
  public @ReturnNullAsFalse Object pg_parameter_status(Env env,
                                                       @NotNull Mysqli conn,
                                                       String paramName)
  {
    try {

      Object object = pg_query(env, conn, "SHOW "+paramName);

      MysqliResult result = (MysqliResult) object;

      object = pg_fetch_result(env, result, 0, 0);

      return object;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Open a persistent PostgreSQL connection
   */
  public @ReturnNullAsFalse Object pg_pconnect(Env env,
                                               String connectionString,
                                               @Optional int connectType)
  {
    return pg_connect(env, connectionString, connectType);
  }

  /**
   * Ping database connection
   */
  public boolean pg_ping(Env env,
                         @Optional Mysqli conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      return conn.ping();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Return the port number associated with the connection
   */
  public @ReturnNullAsFalse Object pg_port(Env env,
                                           @Optional Mysqli conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      return new Integer(conn.get_port_number());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Submits a request to create a prepared statement with the given parameters,
   * and waits for completion
   */
  public @ReturnNullAsFalse Object pg_prepare(Env env,
                                              @NotNull Mysqli conn,
                                              String stmtName,
                                              String query)
  {
    try {

      // Make the PHP query a JDBC like query replacing ($1 -> ?) with question marks.
      query = query.replaceAll("\\$[0-9]+", "?");
      MysqliStatement pstmt = conn.prepare(env, query);
      conn.putStatement(stmtName, pstmt);
      return pstmt;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Send a NULL-terminated string to PostgreSQL backend
   */
  public boolean pg_put_line(Env env,
                             @NotNull Mysqli conn,
                             String data)
  {
    throw new UnimplementedException("pg_put_line");
  }

  /**
   * Submits a command to the server and waits for the result,
   * with the ability to pass parameters separately from the SQL command text
   */
  public @ReturnNullAsFalse Object pg_query_params(Env env,
                                                   @NotNull Mysqli conn,
                                                   String query,
                                                   ArrayValue params)
  {
    try {

      if (pg_send_query_params(env, conn, query, params)) {
        return conn.getAsynchronousResult();
      }

      return null;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Execute a query
   */
  public @ReturnNullAsFalse Object pg_query(Env env,
                                            @NotNull Mysqli conn,
                                            String query)
  {
    try {

      //@todo conn should be optional
      if (conn == null)
        conn = getConnection(env);

      return conn.query(query);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns an individual field of an error report
   */
  public String pg_result_error_field(Env env,
                                      @NotNull MysqliResult result,
                                      int fieldCode)
  {
    throw new UnimplementedException("pg_result_error_field");
  }

  /**
   * Get error message associated with result
   */
  public String pg_result_error(Env env,
                                @NotNull MysqliResult result)
  {
    throw new UnimplementedException("pg_result_error");
  }

  /**
   * Set internal row offset in result resource
   */
  public boolean pg_result_seek(Env env,
                                @NotNull MysqliResult result,
                                int offset)
  {
    try {

      return result.data_seek(env, offset);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Get status of query result
   */
  public Value pg_result_status(Env env,
                                @NotNull MysqliResult result,
                                @Optional int type)
  {
    throw new UnimplementedException("pg_result_status");
  }

  /**
   * Select records
   */
  public @ReturnNullAsFalse Object pg_select(Env env,
                                             @NotNull Mysqli conn,
                                             String tableName,
                                             Value assocArray,
                                             @Optional("-1") int options)
  {
    try {

      String where = "";

      System.out.println("assocArray: "+assocArray);

      ArrayValueImpl arrayImpl = (ArrayValueImpl)assocArray;
      int size = arrayImpl.size();

      for (int i=0; i<size; i++) {
        String p = arrayImpl.get(LongValue.create(i)).toString();
        String pi = conn.real_escape_string(p).toString();
        pi = pi.replaceAll("\\\\", "\\\\\\\\");
        where += "\\'"+pi+"\\' AND";
      }

      String query = "SELECT * FROM " + tableName;

      if (!where.equals("")) {
        query = " WHERE " + query;
      }

      pg_query(env, conn, query);

      return null;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Sends a request to execute a prepared statement with given parameters,
   * without waiting for the result(s)
   */
  public boolean pg_send_execute(Env env,
                                 @NotNull Mysqli conn,
                                 String stmtName,
                                 Value params)
  {
    try {

      // Note: for now, this is the same as pg_execute.

      Object object = pg_execute(env, conn, stmtName, params);

      if (object != null) {
        return true;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Sends a request to create a prepared statement with the given parameters,
   * without waiting for completion
   */
  public boolean pg_send_prepare(Env env,
                                 @NotNull Mysqli conn,
                                 String stmtName,
                                 String query)
  {
    try {

      // Note: for now, this is the same as pg_prepare.

      Object object = pg_prepare(env, conn, stmtName, query);

      if (object != null) {
        return true;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Submits a command and separate parameters to the server without waiting for the result(s)
   */
  public boolean pg_send_query_params(Env env,
                                      @NotNull Mysqli conn,
                                      String query,
                                      ArrayValue params)
  {
    try {

      ArrayValueImpl arrayImpl = (ArrayValueImpl)params;
      int sz = arrayImpl.size();

      for (int i=0; i<sz; i++) {
        String p = arrayImpl.get(LongValue.create(i)).toString();
        String pi = conn.real_escape_string(p).toString();
        pi = pi.replaceAll("\\\\", "\\\\\\\\");
        query = query.replaceAll("\\$"+(i+1), "\\'"+pi+"\\'");
      }

      pg_send_query(env, conn, query);

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Sends asynchronous query
   */
  public boolean pg_send_query(Env env,
                               @NotNull Mysqli conn,
                               String query)
  {
    try {

      Object result = pg_query(env, conn, query);

      if ((result != null) && (result instanceof MysqliResult)) {
        conn.setAsynchronousResult((MysqliResult)result);
        return true;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Set the client encoding
   */
  public int pg_set_client_encoding(Env env,
                                    @NotNull Mysqli conn,
                                    String encoding)
  {
    //@todo conn should be optional

    if (pg_query(env, conn, "SET CLIENT_ENCODING TO '" + encoding +"'") == null) {
      return -1;
    }

    return 0;
  }

  /**
   * Determines the verbosity of messages returned by pg_last_error() and pg_result_error()
   */
  public int pg_set_error_verbosity(Env env,
                                    @NotNull Mysqli conn,
                                    int intVerbosity)
  {
    try {

      //@todo conn should be optional

      String verbosity;

      Object value = pg_query(env, conn, "SHOW log_error_verbosity");

      Value row = pg_fetch_row(env, (MysqliResult)value, LongValue.create(0));

      ArrayValueImpl arr = (ArrayValueImpl)row;

      String prevVerbosity = arr.get(LongValue.create(0)).toString();

      switch (intVerbosity) {
      case PGSQL_ERRORS_TERSE:
        verbosity = "TERSE";
        break;
      case PGSQL_ERRORS_VERBOSE:
        verbosity = "VERBOSE";
        break;
      default:
        verbosity = "DEFAULT";
      }

      pg_query(env, conn, "SET log_error_verbosity TO '"+verbosity+"'");

      if (prevVerbosity.equals("TERSE")) {
        return PGSQL_ERRORS_TERSE;
      } else if (prevVerbosity.equals("VERBOSE")) {
        return PGSQL_ERRORS_VERBOSE;
      } else {
        return PGSQL_ERRORS_DEFAULT;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Enable tracing a PostgreSQL connection
   */
  public boolean pg_trace(Env env,
                          String pathName,
                          @Optional String mode,
                          @Optional Mysqli conn)
  {
    throw new UnimplementedException("pg_trace");
  }

  /**
   * Returns the current in-transaction status of the server
   */
  public int pg_transaction_status(Env env,
                                   @Optional Mysqli conn)
  {
    throw new UnimplementedException("pg_transaction_status");
  }

  /**
   * Return the TTY name associated with the connection
   */
  public String pg_tty(Env env,
                       @Optional Mysqli conn)
  {
    throw new UnimplementedException("pg_tty");
  }

  /**
   * Unescape binary for bytea type
   */
  public String pg_unescape_bytea(Env env,
                                  String data)
  {
    throw new UnimplementedException("pg_unescape_bytea");
  }

  /**
   * Disable tracing of a PostgreSQL connection
   */
  public Value pg_untrace(Env env,
                          @Optional Mysqli conn)
  {
    // Always returns TRUE

    throw new UnimplementedException("pg_untrace");
  }

  /**
   * Update table
   */
  public Value pg_update(Env env,
                         @NotNull Mysqli conn,
                         String tableName,
                         Value data,
                         Value condition,
                         @Optional int options)
  {
    // @todo from php.net: This function is EXPERIMENTAL.

    // The behaviour of this function, the name of this function, and
    // anything else documented about this function may change without
    // notice in a future release of PHP. Use this function at your own risk.

    throw new UnimplementedException("pg_update");
  }

  /**
   * Returns an array with client, protocol and server version (when available)
   */
  public @ReturnNullAsFalse Object pg_version(Env env,
                                              @Optional Mysqli conn)
  {
    try {

      //@todo return an array

      if (conn == null)
        conn = getConnection(env);

      return conn.get_server_info();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  private Mysqli getConnection(Env env)
  {
    Mysqli conn = (Mysqli) env.getSpecialValue("caucho.postgres");

    if (conn != null)
      return conn;

    String driver = "org.postgresql.Driver";
    String url = "jdbc:postgresql://localhost:5432/";

    conn = new Mysqli(env, "localhost", "", "", "", 5432, "", driver, url);

    env.setSpecialValue("caucho.postgres", conn);

    return conn;
  }

  private String escapeCharsToRegex(String []stringMap,
                                    Pattern pattern,
                                    String chars)
  {
    StringBuffer stringBuffer = new StringBuffer();
    // For testing only:
    // String delimiter = "\n\tNUMBER\r1stco*lumn\t\n\t\n\tNUM|BER(2nd)column\tNUMBER\\3rdcolumn\tNUMBER4thcolumn\t\naa";
    Matcher matcher = pattern.matcher(chars);
    if (matcher.find()) {
      String replacement = matcher.group();
      char c = replacement.charAt(0);
      if (c < 32) {
        replacement = stringMap[c-9];
      } else if (c == '\\') {
        replacement = "\\\\";
      }
      matcher.appendReplacement(stringBuffer, "\\\\"+replacement);
    }
    matcher.appendTail(stringBuffer);

    return stringBuffer.toString();
  }
}
