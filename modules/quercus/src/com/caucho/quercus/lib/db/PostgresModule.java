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

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.ResourceValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;

import com.caucho.quercus.UnimplementedException;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

// Do not add new compile dependencies (use reflection instead)
// import org.postgresql.largeobject.*;


/**
 * Quercus postgres routines.
 */
public class PostgresModule extends AbstractQuercusModule {

  private static final Logger log = Log.open(PostgresModule.class);
  private static final L10N L = new L10N(PostgresModule.class);

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

  /**
   * Constructor
   */
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
  public static int pg_affected_rows(Env env,
                                     @NotNull PostgresResult result)
  {
    try {

      return result.getAffectedRows();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return 0;
    }
  }

  /**
   * Cancel an asynchronous query
   */
  public static boolean pg_cancel_query(Env env,
                                        @NotNull Postgres conn)
  {
    try {

      conn.setAsynchronousStatement(null);
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
  @ReturnNullAsFalse
  public static String pg_client_encoding(Env env,
                                          @Optional Postgres conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      return conn.getClientEncoding();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Closes a PostgreSQL connection
   */
  public static boolean pg_close(Env env,
                                 @Optional Postgres conn)
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
  @ReturnNullAsFalse
  public static Postgres pg_connect(Env env,
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

      Postgres postgres
        = new Postgres(env, host, userName, password, dbName, port, driver, url);

      env.setSpecialValue("caucho.postgres", postgres);

      return postgres;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get connection is busy or not
   */
  public static boolean pg_connection_busy(Env env,
                                           @NotNull Postgres conn)
  {
    // Always return false, for now (pg_send_xxxx are not asynchronous)
    // so there should be no reason for a connection to become busy in
    // between different pg_xxx calls.

    return false;
  }

  /**
   * Reset connection (reconnect)
   */
  public static boolean pg_connection_reset(Env env,
                                            @NotNull Postgres conn)
  {
    try {

      conn.close(env);

      conn = new Postgres(env,
                          conn.getHost(),
                          conn.getUserName(),
                          conn.getPassword(),
                          conn.getDbName(),
                          conn.getPort(),
                          conn.getDriver(),
                          conn.getUrl());

      env.setSpecialValue("caucho.postgres", conn);

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Get connection status
   */
  public static int pg_connection_status(Env env,
                                         @NotNull Postgres conn)
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
  @ReturnNullAsFalse
  public static ArrayValue pg_convert(Env env,
                                      @NotNull Postgres conn,
                                      String tableName,
                                      ArrayValue assocArray,
                                      @Optional("-1") int options)
  {
    try {

      if (options > 0) {
        throw new UnimplementedException("pg_convert with options");
      }

      ArrayValueImpl newArray = new ArrayValueImpl();

      for (Map.Entry<Value,Value> entry : assocArray.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();

        newArray.put(k, new StringBuilderValue().append("'").append(v).append("'"));
      }

      return newArray;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Insert records into a table from an array
   */
  public static boolean pg_copy_from(Env env,
                                     @NotNull Postgres conn,
                                     String tableName,
                                     ArrayValue rows,
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
  @ReturnNullAsFalse
  public static ArrayValue pg_copy_to(Env env,
                                      @NotNull Postgres conn,
                                      String tableName,
                                      @Optional("\t") String delimiter,
                                      @Optional("\n") String nullAs)
  {
    try {

      PostgresResult result = pg_query(env, conn, "SELECT * FROM " + tableName);

      ArrayValueImpl newArray = new ArrayValueImpl();

      Object value;

      int curr = 0;

      while ((value = result.fetchArray(env, PGSQL_NUM)) != null) {

        ArrayValueImpl arr = (ArrayValueImpl) value;
        int count = arr.size();

        StringBuilderValue sb = new StringBuilderValue();

        LongValue currValue = LongValue.create(curr);

        for (int i=0; i<count; i++) {

          if (sb.length() > 0)
            sb.append(delimiter);

          Value v = newArray.get(currValue);

          Value fieldValue = arr.get(LongValue.create(i));

          if (fieldValue instanceof NullValue) {
            sb.append(nullAs);
          } else {
            sb.append(fieldValue.toString());
          }
        }

        newArray.put(currValue, sb);

        curr++;
      }

      return newArray;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get the database name
   */
  @ReturnNullAsFalse
  public static String pg_dbname(Env env,
                                 @Optional Postgres conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      return conn.getDbName();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Deletes records
   */
  public static boolean pg_delete(Env env,
                                  @NotNull Postgres conn,
                                  String tableName,
                                  ArrayValue assocArray,
                                  @Optional("-1") int options)
  {
    // From php.net: this function is EXPERIMENTAL.
    // This function is EXPERIMENTAL. The behaviour of this function,
    // the name of this function, and anything else documented about this function
    // may change without notice in a future release of PHP.
    // Use this function at your own risk.

    try {

      if (options > 0) {
        throw new UnimplementedException("pg_delete with options");
      }

      StringBuilder condition = new StringBuilder();

      boolean isFirst = true;

      for (Map.Entry<Value,Value> entry : assocArray.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          condition.append(" AND ");
        }
        condition.append(k.toString());
        condition.append("='");
        condition.append(v.toString());
        condition.append("'");
      }

      StringBuilder query = new StringBuilder();
      query.append("DELETE FROM ");
      query.append(tableName);
      query.append(" WHERE ");
      query.append(condition);

      pg_query(env, conn, query.toString());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Sync with PostgreSQL backend
   */
  public static boolean pg_end_copy(Env env,
                                    @Optional Postgres conn)
  {
    env.stub("pg_end_copy");

    return false;
  }

  /**
   * Escape a string for insertion into a bytea field
   */
  @ReturnNullAsFalse
  public static String pg_escape_bytea(Env env,
                                       String data)
  {
    try {

      Postgres conn = getConnection(env);

      if (conn == null)
        return null;

      byte dataBytes[] = data.getBytes();

      Class cl = Class.forName("org.postgresql.util.PGbytea");

      Method method = cl.getDeclaredMethod("toPGString", new Class[] {byte[].class});

      String s = (String) method.invoke(cl, new Object[] {dataBytes});

      return conn.realEscapeString((StringValue)StringValue.create(s)).toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Escape a string for insertion into a text field
   */
  @ReturnNullAsFalse
  public static String pg_escape_string(Env env,
                                        StringValue data)
  {
    try {

      Postgres conn = getConnection(env);

      if (conn == null)
        return null;

      return conn.realEscapeString(data).toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Sends a request to execute a prepared statement with given parameters,
   * and waits for the result
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_execute(Env env,
                                          @NotNull Postgres conn,
                                          String stmtName,
                                          ArrayValue params)
  {
    try {
      PostgresStatement pstmt = conn.getStatement(stmtName);

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
      pstmt.bindParams(env, types, value);

      if (!pstmt.execute(env))
        return null;

      if (pstmt.getStatementType().equals("SELECT")) {
        PostgresResult result = new PostgresResult(null, pstmt.getResultSet(), null);
        conn.setResultResource(result);
        return result;
      } else {
        // XXX: ??? return type?
        return null;
        // return pstmt;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      conn.setResultResource(null);
      return null;
    }
  }

  /**
   * Fetches all rows in a particular result column as an array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_all_columns(Env env,
                                                @NotNull PostgresResult result,
                                                @Optional("0") int column)
  {
    try {

      ArrayValueImpl newArray = new ArrayValueImpl();

      int curr = 0;

      for (ArrayValue row = result.fetchRow(env);
           row != null;
           row = result.fetchRow(env)) {

        newArray.put(LongValue.create(curr++),
                     row.get(LongValue.create(column)));

      }

      if (newArray.getSize() > 0) {
        return newArray;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Fetches all rows from a result as an array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_all(Env env,
                                        @NotNull PostgresResult result)
  {
    try {

      ArrayValueImpl newArray = new ArrayValueImpl();

      int curr = 0;

      for (ArrayValue row = result.fetchAssoc(env);
           row != null;
           row = result.fetchAssoc(env)) {

        newArray.put(LongValue.create(curr++), row);

      }

      if (newArray.getSize() > 0) {
        return newArray;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Fetch a row as an array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_array(Env env,
                                          @NotNull PostgresResult result,
                                          @Optional("-1") Value row,
                                          @Optional("PGSQL_BOTH") int resultType)
  {
    try {

      // NOTE: row is of type Value because row is optional and there is
      // only one way to specify that 'row' will not be used:
      //
      // pg_fetch_array(result, NULL, resultType)
      //
      // The resultType will be used above though.
      //
      // For such a case, the marshalling code passes row in as NullValue.NULL
      // If we used 'int row' there would be no way to distinguish row 'zero'
      // from row 'null'.

      if (result == null)
        return  null;

      if (row.isLongConvertible() && row.toInt() >= 0) {
        if (!result.seek(env, row.toInt())) {
          env.warning(L.l("Unable to jump to row {0} on PostgreSQL result",
                          row.toInt()));
          return null;
        }
      }

      return result.fetchArray(env, resultType);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Fetch a row as an associative array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_assoc(Env env,
                                          @NotNull PostgresResult result,
                                          @Optional("-1") Value row)
  {
    try {

      if ((row != null) && (!row.equals(NullValue.NULL)) && (row.toInt() >= 0)) {
        result.seek(env, row.toInt());
      }

      return result.fetchAssoc(env);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Fetch a row as an object
   */
  public static Value pg_fetch_object(Env env,
                                      @NotNull PostgresResult result,
                                      @Optional("-1") Value row,
                                      @Optional int resultType)
  {
    try {

      //@todo use optional resultType
      if ((row != null) && (!row.equals(NullValue.NULL)) && (row.toInt() >= 0)) {
        result.seek(env, row.toInt());
      }

      return result.fetchObject(env);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns values from a result resource
   */
  @ReturnNullAsFalse
  public static String pg_fetch_result(Env env,
                                       @NotNull PostgresResult result,
                                       Value row,
                                       @Optional("-1") Value fieldNameOrNumber)
  {
    try {

      // NOTE: row is of type Value because there is a case where
      // row is optional. In such a case, the row value passed in
      // is actually the field number or field name.

      int rowNumber = -1;

      // Handle the case: optional row with mandatory fieldNameOrNumber.
      if (fieldNameOrNumber.isLongConvertible() &&
          (fieldNameOrNumber.toInt() < 0)) {
        fieldNameOrNumber = row;
        rowNumber = -1;
      } else {
        rowNumber = row.toInt();
      }

      if (rowNumber >= 0) {
        result.seek(env, rowNumber);
      }

      Value fetchRow = result.fetchRow(env);

      int fieldNumber = result.getColumnNumber(fieldNameOrNumber, 0);

      return fetchRow.get(LongValue.create(fieldNumber)).toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get a row as an enumerated array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_row(Env env,
                                        @NotNull PostgresResult result,
                                        @Optional("-1") Value row)
  {
    try {

      if ((row != null) && (!row.equals(NullValue.NULL)) && (row.toInt() >= 0)) {
        result.seek(env, row.toInt());
      }

      return result.fetchRow(env);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Test if a field is SQL NULL
   */
  @ReturnNullAsFalse
  public static LongValue pg_field_is_null(Env env,
                                           @NotNull PostgresResult result,
                                           Value row,
                                           @Optional("-1") Value fieldNameOrNumber)
  {
    try {

      // NOTE: row is of type Value because there is a case where
      // row is optional. In such a case, the row value passed in
      // is actually the field number or field name.

      int rowNumber = -1;

      // Handle the case: optional row with mandatory fieldNameOrNumber.
      if (fieldNameOrNumber.isLongConvertible() &&
          (fieldNameOrNumber.toInt() == -1)) {
        fieldNameOrNumber = row;
        rowNumber = -1;
      } else {
        rowNumber = row.toInt();
      }

      if (rowNumber >= 0) {
        result.seek(env, rowNumber);
      }

      int fieldNumber = result.getColumnNumber(fieldNameOrNumber, 0);

      String field = pg_fetch_result(env,
                                     result,
                                     LongValue.create(-1),
                                     LongValue.create(fieldNumber));

      if (field == null) {
        return LongValue.create(1);
      }

      return LongValue.create(0);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the name of a field
   */
  public static Value pg_field_name(Env env,
                                    @NotNull PostgresResult result,
                                    int fieldNumber)
  {
    try {

      return result.getFieldName(env, fieldNumber);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the field number of the named field
   *
   * @return the field number (0-based) or -1 on error
   */
  public static int pg_field_num(Env env,
                                 @NotNull PostgresResult result,
                                 String fieldName)
  {
    try {

      return result.getColumnNumber(fieldName);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Returns the printed length
   */
  public static int pg_field_prtlen(Env env,
                                    @NotNull PostgresResult result,
                                    Value rowNumber,
                                    @Optional("-1") Value fieldNameOrNumber)
  {
    try {
      int row = rowNumber.toInt();

      if (fieldNameOrNumber.toString().equals("-1")) {
        fieldNameOrNumber = rowNumber;
        row = -1;
      }

      int fieldNumber = result.getColumnNumber(fieldNameOrNumber, 0);

      Object object = pg_fetch_result(env,
                                      result,
                                      LongValue.create(row),
                                      LongValue.create(fieldNumber));

      // Step the cursor back to the original position
      // See php/4325
      result.getResultSet().relative(-1);

      return object.toString().length();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Returns the internal storage size of the named field
   */
  public static Value pg_field_size(Env env,
                                    @NotNull PostgresResult result,
                                    int fieldNumber)
  {
    try {

      Value value = pg_field_type(env, result, fieldNumber);

      if (value.toString().equals("varchar")) {
        value = LongValue.create(-1);
      } else {
        value = result.getFieldLength(env, fieldNumber);
      }

      return value;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the name or oid of the tables field
   *
   * @return By default the tables name that field belongs to
   * is returned but if oid_only is set to TRUE,
   * then the oid will instead be returned.
   */
  @ReturnNullAsFalse
  public static String pg_field_table(Env env,
                                      @NotNull PostgresResult result,
                                      int fieldNumber,
                                      @Optional("false") boolean oidOnly)
  {
    // The Postgres JDBC driver doesn't have a concept of exposing to the client
    // what table maps to a particular select item in a result set, therefore the
    // driver cannot report anything useful to the caller. Thus the driver always
    // returns "" to ResultSetMetaData.getTableName(fieldNumber+1)

    env.stub("pg_field_table");

    return "";
  }

  /**
   * Returns the type ID (OID) for the corresponding field number
   */
  @ReturnNullAsFalse
  public static LongValue pg_field_type_oid(Env env,
                                            @NotNull PostgresResult result,
                                            int fieldNumber)
  {
    try {

      ResultSetMetaData metaData = result.getMetaData();

      String columnTypeName = metaData.getColumnTypeName(fieldNumber + 1);

      String metaQuery = ("SELECT oid FROM pg_type WHERE typname='"+columnTypeName+"'");

      result = pg_query(env, (Postgres) result.getConnection(), metaQuery);

      String s = pg_fetch_result(env,
                                 result,
                                 LongValue.create(-1),
                                 LongValue.create(0));

      return LongValue.create(new Integer(s));

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the type name for the corresponding field number
   */
  public static Value pg_field_type(Env env,
                                    @NotNull PostgresResult result,
                                    int fieldNumber)
  {
    try {

      Value value = result.getFieldType(env, fieldNumber);

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
  public static boolean pg_free_result(Env env,
                                       @NotNull PostgresResult result)
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
  @ReturnNullAsFalse
  public static ArrayValue pg_get_notify(Env env,
                                         @NotNull Postgres conn,
                                         @Optional("-1") int resultType)
  {
    try {

      if (resultType > 0) {
        throw new UnimplementedException("pg_get_notify with result type");
      }

      // org.postgresql.PGConnection
      Class cl = Class.forName("org.postgresql.PGConnection");

      // public PGNotification[] getNotifications() throws SQLException;
      Method method = cl.getDeclaredMethod("getNotifications", null);

      Connection pgconn = conn.getJavaConnection();

      // getNotifications()
      Object notifications[] = (Object[]) method.invoke(pgconn, new Object[] {});

      // org.postgresql.PGNotification
      cl = Class.forName("org.postgresql.PGNotification");

      // public String getName();
      Method methodGetName = cl.getDeclaredMethod("getName", null);

      // public int getPID();
      Method methodGetPID = cl.getDeclaredMethod("getPID", null);

      ArrayValueImpl arrayValue = new ArrayValueImpl();

      int n = notifications.length;

      StringValue k;
      LongValue v;

      for (int i=0; i<n; i++) {
        // getName()
        k = (StringValue) StringValue.create(methodGetName.invoke(notifications[i],
                                                                  new Object[] {}));
        // getPID()
        v = (LongValue) LongValue.create((Integer) methodGetPID.invoke(notifications[i],
                                                                       new Object[] {}));

        arrayValue.put(k, v);
      }

      return arrayValue;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Gets the backend's process ID
   */
  public static int pg_get_pid(Env env,
                               @NotNull Postgres conn)
  {
    try {

      // @todo create a random string
      String randomLabel = "caucho_pg_get_pid_random_label";

      pg_query(env, conn, "LISTEN "+randomLabel);
      pg_query(env, conn, "NOTIFY "+randomLabel);

      ArrayValue arrayValue = pg_get_notify(env, conn, -1);

      LongValue pid = (LongValue) arrayValue.get(StringValue.create(randomLabel));

      return pid.toInt();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Get asynchronous query result
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_get_result(Env env,
                                             @Optional Postgres conn)
  {
    // Three different scenarios for pg_get_result:
    //
    // 1. pg_send_prepare/pg_send_execute - php/4370
    //
    //    pg_send_prepare($conn, "my_query", 'SELECT * FROM test WHERE data = $1');
    //    $res1 = pg_get_result($conn);
    //
    //    pg_send_execute($conn, "my_query", array("Joe's Widgets"));
    //    $res2 = pg_get_result($conn);
    //
    //    pg_send_execute($conn, "my_query", array("Clothes Clothes Clothes"));
    //    $res3 = pg_get_result($conn);
    //
    // 2. Multiquery with pg_send_query - php/4334
    //
    //    pg_send_query($conn, "select * from test; select count(*) from test;");
    //
    //    // select * from test
    //    $res = pg_get_result($conn);
    //    $rows = pg_num_rows($res);
    //
    //    // select count(*) from test
    //    $res = pg_get_result($conn);
    //    $rows = pg_num_rows($res);
    //
    // 3. Individual pg_send_query - php/4364
    //
    //    pg_send_query($conn, "select * from test;");
    //    var_dump($res);
    //    $res = pg_get_result($conn);
    //    var_dump($res);
    //
    //    $res = pg_send_query($conn, "select * from doesnotexist;");
    //    var_dump($res);
    //    $res = pg_get_result($conn);
    //    var_dump($res);

    try {

      if (conn == null)
        conn = getConnection(env);

      PostgresResult result = (PostgresResult) conn.getResultResource();

      // 1. pg_send_prepare/pg_send_execute
      if (conn.getAsynchronousStatement() != null) {
        if (conn.getAsynchronousResult() != null) {
          conn.setAsynchronousResult(null);
          return result;
        }
        return null;
      }

      // 2. pg_send_query
      if (conn.getAsynchronousResult() != null) {

        // Check for next result
        // Ex: pg_send_query($conn, "select * from test; select count(*) from test;");

        Statement stmt = result.getJavaStatement();

        if (stmt.getMoreResults()) {
          result = (PostgresResult) conn.createResult(stmt, stmt.getResultSet());
        } else {
          // 3. Individual pg_send_query (clean up; no futher results)
          conn.setResultResource(null);
        }
      }

      conn.setAsynchronousResult(result);

      return result;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the host name associated with the connection
   */
  @ReturnNullAsFalse
  public static String pg_host(Env env,
                               @Optional Postgres conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      return conn.getHost();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Insert array into table
   */
  public static boolean pg_insert(Env env,
                                  @NotNull Postgres conn,
                                  String tableName,
                                  ArrayValue assocArray,
                                  @Optional("-1") int options)
  {
    try {

      if (options > 0) {
        throw new UnimplementedException("pg_convert with options");
      }

      StringBuilder names = new StringBuilder();
      StringBuilder values = new StringBuilder();

      boolean isFirst = true;

      for (Map.Entry<Value,Value> entry : assocArray.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          values.append("','");
          names.append(",");
        }
        values.append(v.toString());
        names.append(k.toString());
      }

      StringBuilder query = new StringBuilder();
      query.append("INSERT INTO ");
      query.append(tableName);
      query.append("(");
      query.append(names);
      query.append(") VALUES('");
      query.append(values);
      query.append("')");

      pg_query(env, conn, query.toString());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Get the last error message string of a connection
   */
  @ReturnNullAsFalse
  public static String pg_last_error(Env env,
                                     @Optional Postgres conn)
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
  @ReturnNullAsFalse
  public static String pg_last_notice(Env env,
                                      @NotNull Postgres conn)
  {
    try {

      return conn.getWarnings().toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the last row's OID
   *
   * Note that:
   * - OID is a unique id. It will not work if the table was created with "No oid".
   * - MySql's "mysql_insert_id" receives the conection handler as argument but
   * PostgreSQL's "pg_last_oid" uses the result handler.
   */
  @ReturnNullAsFalse
  public static String pg_last_oid(Env env,
                                   PostgresResult result)
  {
    try {

      Statement stmt = result.getJavaStatement();

      Class cl = Class.forName("org.postgresql.jdbc2.AbstractJdbc2Statement");

      Method method = cl.getDeclaredMethod("getLastOID", null);

      int oid = Integer.parseInt(method.invoke(stmt, new Object[] {}).toString());

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
  public static boolean pg_lo_close(Env env,
                                    Object largeObject)
  {
    try {

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("close", null);

      method.invoke(largeObject, new Object[] {});
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
  @ReturnNullAsFalse
  public static LongValue pg_lo_create(Env env,
                                       @Optional Postgres conn)
  {
    try {

      int oid = -1;

      if (conn == null)
        conn = getConnection(env);

      // LargeObjectManager lobManager;
      Object lobManager;

      // org.postgresql.PGConnection
      Class cl = Class.forName("org.postgresql.PGConnection");

      Method method = cl.getDeclaredMethod("getLargeObjectAPI", null);

      Connection pgconn = conn.getJavaConnection();

      // Large Objects may not be used in auto-commit mode.
      pgconn.setAutoCommit(false);

      lobManager = method.invoke(pgconn, new Object[] {});
      // lobManager = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

      // org.postgresql.largeobject.LargeObjectManager
      cl = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      method = cl.getDeclaredMethod("create", null);

      Object oidObj = method.invoke(lobManager, new Object[] {});

      oid = Integer.parseInt(oidObj.toString());

      // oid = lobManager.create();

      return LongValue.create(oid);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Export a large object to a file
   */
  public static boolean pg_lo_export(Env env,
                                     @NotNull Postgres conn,
                                     int oid,
                                     Path path)
  {
    try {

      //@todo conn should be optional

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class cl = Class.forName("org.postgresql.PGConnection");

      Method method = cl.getDeclaredMethod("getLargeObjectAPI", null);

      Connection pgconn = conn.getJavaConnection();

      lobManager = method.invoke(pgconn, new Object[] {});
      // lobManager = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

      cl = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      method = cl.getDeclaredMethod("open", new Class[] {Integer.TYPE});

      Object lobj = method.invoke(lobManager, new Object[] {oid});

      cl = Class.forName("org.postgresql.largeobject.LargeObject");

      method = cl.getDeclaredMethod("getInputStream", null);

      Object isObj = method.invoke(lobj, new Object[] {});

      InputStream is = (InputStream)isObj;

      // Open the file
      WriteStream os = path.openWrite();

      // copy the data from the large object to the file
      os.writeStream(is);

      os.close();
      is.close();

      // Close the large object
      method = cl.getDeclaredMethod("close", null);

      method.invoke(lobj, new Object[] {});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Import a large object from file
   */
  @ReturnNullAsFalse
  public static LongValue pg_lo_import(Env env,
                                       @NotNull Postgres conn,
                                       Path path)
  {
    try {

      //@todo conn should be optional

      LongValue value = pg_lo_create(env, conn);

      if (value != null) {

        int oid = value.toInt();
        Object largeObject = pg_lo_open(env, conn, oid, "w");

        String data = "";

        // Open the file
        ReadStream is = path.openRead();

        writeLobInternal(largeObject, is, Integer.MAX_VALUE);

        pg_lo_close(env, largeObject);

        is.close();

        return LongValue.create(oid);
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Open a large object
   */
  @ReturnNullAsFalse
  public static Object pg_lo_open(Env env,
                                  @NotNull Postgres conn,
                                  int oid,
                                  String mode)
  {
    try {

      Object largeObject = null;

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class cl = Class.forName("org.postgresql.PGConnection");

      Method method = cl.getDeclaredMethod("getLargeObjectAPI", null);

      Connection pgconn = conn.getJavaConnection();

      lobManager = method.invoke(pgconn, new Object[] {});

      cl = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      method = cl.getDeclaredMethod("open", new Class[] {Integer.TYPE, Integer.TYPE});

      boolean write = mode.indexOf("w") >= 0;
      boolean read = mode.indexOf("r") >= 0;

      int modeREAD = cl.getDeclaredField("READ").getInt(null);
      int modeREADWRITE = cl.getDeclaredField("READWRITE").getInt(null);
      int modeWRITE = cl.getDeclaredField("WRITE").getInt(null);

      int intMode = modeREAD;

      if (read) {
        if (write) {
          intMode = modeREADWRITE;
        }
      } else if (write) {
        intMode = modeWRITE;
      }

      largeObject = method.invoke(lobManager, new Object[] {oid, intMode});

      return largeObject;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Reads an entire large object and send straight to browser
   */
  @ReturnNullAsFalse
  public static LongValue pg_lo_read_all(Env env,
                                         Object largeObject)
  {
    try {

      String contents = pg_lo_read(env, largeObject, -1);
      if (contents != null) {
        env.getOut().print(contents);
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Read a large object
   */
  @ReturnNullAsFalse
  public static String pg_lo_read(Env env,
                                  Object largeObject,
                                  @Optional("-1") int len)
  {
    try {

      if (len < 0) {
        len = Integer.MAX_VALUE;
      }

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("getInputStream", null);

      InputStream is = (InputStream) method.invoke(largeObject, new Object[] {});

      BinaryBuilderValue binaryBuilder = new BinaryBuilderValue();

      int nbytes;
      byte buffer[] = new byte[128];
      while ((len > 0) && ((nbytes = is.read(buffer, 0, 128)) > 0)) {
        if (nbytes > len) {
          nbytes = len;
        }
        binaryBuilder.append(buffer, 0, nbytes);
        len -= nbytes;
      }

      is.close();

      return binaryBuilder.toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Seeks position within a large object
   */
  public static boolean pg_lo_seek(Env env,
                                   Object largeObject,
                                   int offset,
                                   @Optional int whence)
  {
    try {

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      int seekSET = cl.getDeclaredField("SEEK_SET").getInt(null);
      int seekEND = cl.getDeclaredField("SEEK_END").getInt(null);
      int seekCUR = cl.getDeclaredField("SEEK_CUR").getInt(null);

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

      Method method = cl.getDeclaredMethod("seek", new Class[]{Integer.TYPE,Integer.TYPE});

      method.invoke(largeObject, new Object[] {offset, whence});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns current seek position a of large object
   */
  public static int pg_lo_tell(Env env,
                               Object largeObject)
  {
    try {

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("tell", null);

      Object obj = method.invoke(largeObject, new Object[] {});

      return Integer.parseInt(obj.toString());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Delete a large object
   */
  public static boolean pg_lo_unlink(Env env,
                                     @NotNull Postgres conn,
                                     int oid)
  {
    try {

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class cl = Class.forName("org.postgresql.PGConnection");

      Method method = cl.getDeclaredMethod("getLargeObjectAPI", null);

      Connection pgconn = conn.getJavaConnection();

      lobManager = method.invoke(pgconn, new Object[] {});

      cl = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      method = cl.getDeclaredMethod("unlink", new Class[] {Integer.TYPE});

      method.invoke(lobManager, new Object[] {oid});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Write to a large object
   */
  @ReturnNullAsFalse
  public static LongValue pg_lo_write(Env env,
                                      @NotNull Object largeObject,
                                      String data,
                                      @Optional int len)
  {
    try {

      if (len <= 0) {
        len = data.length();
      }

      int written = len;

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("write",
                                           new Class[] {byte[].class,
                                                        Integer.TYPE,
                                                        Integer.TYPE});

      method.invoke(largeObject, new Object[] {data.getBytes(), 0, len});

      return LongValue.create(written);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get meta data for table
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_meta_data(Env env,
                                        @NotNull Postgres conn,
                                        String tableName)
  {
    env.stub("pg_meta_data");

    return null;
  }

  /**
   * Returns the number of fields in a result
   */
  public static int pg_num_fields(Env env,
                                  @NotNull PostgresResult result)
  {
    try {

      return result.getFieldCount();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Returns the number of rows in a result
   */
  public static LongValue pg_num_rows(Env env,
                                      @NotNull PostgresResult result)
  {
    try {

      // (result.getResultSet() == null) should be cleaned and
      // pg_get_result should return a null PostgresResult php/43c9
      if ((result == null) || (result.getResultSet() == null)) {
        env.warning(L.l("supplied argument is not a valid PostgreSQL result resource"));
        return null;
      }

      return LongValue.create(result.getNumRows());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return LongValue.create(-1);
    }
  }

  /**
   * Get the options associated with the connection
   */
  public static String pg_options(Env env,
                                  @Optional Postgres conn)
  {
    throw new UnimplementedException("pg_options");
  }

  /**
   * Looks up a current parameter setting of the server
   */
  @ReturnNullAsFalse
  public static String pg_parameter_status(Env env,
                                           @NotNull Postgres conn,
                                           String paramName)
  {
    try {

      PostgresResult result = pg_query(env, conn, "SHOW "+paramName);

      return pg_fetch_result(env, result, LongValue.create(0), LongValue.create(0));

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Open a persistent PostgreSQL connection
   */
  @ReturnNullAsFalse
  public static Postgres pg_pconnect(Env env,
                                     String connectionString,
                                     @Optional int connectType)
  {
    return pg_connect(env, connectionString, connectType);
  }

  /**
   * Ping database connection
   */
  public static boolean pg_ping(Env env,
                                @Optional Postgres conn)
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
  @ReturnNullAsFalse
  public static LongValue pg_port(Env env,
                                  @Optional Postgres conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      return LongValue.create(conn.getPort());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Submits a request to create a prepared statement with the given parameters,
   * and waits for completion
   */
  @ReturnNullAsFalse
  public static PostgresStatement pg_prepare(Env env,
                                             @NotNull Postgres conn,
                                             String stmtName,
                                             String query)
  {
    try {

      // Make the PHP query a JDBC like query replacing ($1 -> ?) with question marks.
      query = query.replaceAll("\\$[0-9]+", "?");
      PostgresStatement pstmt = conn.prepare(env, query);
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
  public static boolean pg_put_line(Env env,
                                    @NotNull Postgres conn,
                                    String data)
  {
    try {

      Class cl = Class.forName("org.postgresql.core.PGStream");

      Constructor constructor = cl.getDeclaredConstructor(new Class[] {
        String.class, Integer.TYPE});

      Object object = constructor.newInstance(new Object[] {conn.getHost(), conn.getPort()});

      byte dataArray[] = data.getBytes();

      Method method = cl.getDeclaredMethod("Send", new Class[] {byte[].class});

      method.invoke(object, new Object[] {dataArray});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }

  }

  /**
   * Submits a command to the server and waits for the result,
   * with the ability to pass parameters separately from the SQL command text
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_query_params(Env env,
                                               @NotNull Postgres conn,
                                               String query,
                                               ArrayValue params)
  {
    try {

      if (pg_send_query_params(env, conn, query, params)) {
        return (PostgresResult) conn.getResultResource();
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
  @ReturnNullAsFalse
  public static PostgresResult pg_query(Env env,
                                        @NotNull Postgres conn,
                                        String query)
  {
    try {

      // XXX: the PHP api allows conn to be optional but we
      // totally disallow this case.

      if (conn == null)
        conn = getConnection(env);

      PostgresResult result = conn.query(query);

      return result;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Returns an individual field of an error report
   */
  public static String pg_result_error_field(Env env,
                                             @NotNull PostgresResult result,
                                             int fieldCode)
  {
    try {

      // Get the postgres specific server error message
      // org.postgresql.util.ServerErrorMessage
      Object serverError = ((Postgres) result.getConnection()).getServerErrorMessage();

      Class cl = Class.forName("org.postgresql.util.ServerErrorMessage");

      String methodName;

      switch (fieldCode) {
      case PGSQL_DIAG_SEVERITY:
        methodName = "getSeverity";
        break;
      case PGSQL_DIAG_SQLSTATE:
        methodName = "getSQLState";
        break;
      case PGSQL_DIAG_MESSAGE_PRIMARY:
        methodName = "getMessage";
        break;
      case PGSQL_DIAG_MESSAGE_DETAIL:
        methodName = "getDetail";
        break;
      case PGSQL_DIAG_MESSAGE_HINT:
        methodName = "getHint";
        break;
      case PGSQL_DIAG_STATEMENT_POSITION:
        methodName = "getPosition";
        break;
      case PGSQL_DIAG_INTERNAL_POSITION:
        methodName = "getInternalPosition";
        break;
      case PGSQL_DIAG_INTERNAL_QUERY:
        methodName = "getInternalQuery";
        break;
      case PGSQL_DIAG_CONTEXT:
        methodName = "getWhere";
        break;
      case PGSQL_DIAG_SOURCE_FILE:
        methodName = "getFile";
        break;
      case PGSQL_DIAG_SOURCE_LINE:
        methodName = "getLine";
        break;
      case PGSQL_DIAG_SOURCE_FUNCTION:
        methodName = "getRoutine";
        break;
      default:
        return null;
      }

      Method method = cl.getDeclaredMethod(methodName, null);
      Object errorField = method.invoke(serverError, new Object[] {});

      return errorField.toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get error message associated with result
   */
  @ReturnNullAsFalse
  public static String pg_result_error(Env env,
                                       @NotNull PostgresResult result)
  {
    try {

      return result.getConnection().getErrorMessage();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Set internal row offset in result resource
   */
  public static boolean pg_result_seek(Env env,
                                       @NotNull PostgresResult result,
                                       int offset)
  {
    try {

      if (result == null)
        return false;

      return result.seek(env, offset);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Get status of query result
   */
  public static int pg_result_status(Env env,
                                     @NotNull PostgresResult result,
                                     @Optional("PGSQL_STATUS_LONG") int type)
  {
    try {

      if (type == PGSQL_STATUS_STRING) {
        throw new UnimplementedException("pg_result_status with PGSQL_STATUS_STRING");
      }

      if (result != null) {
        Statement stmt = result.getStatement();
        if (stmt.getUpdateCount() >= 0) {
          return PGSQL_COMMAND_OK;
        }

        ResultSet rs = result.getResultSet();
        if (rs == null) {
          return PGSQL_EMPTY_QUERY;
        }

        return PGSQL_TUPLES_OK;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return -1;
  }

  /**
   * Select records
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_select(Env env,
                                     @NotNull Postgres conn,
                                     String tableName,
                                     ArrayValue assocArray,
                                     @Optional("-1") int options)
  {
    try {

      if (conn == null)
        return null;

      StringBuilderValue whereClause = new StringBuilderValue();

      boolean isFirst = true;

      for (Map.Entry<Value,Value> entry : assocArray.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          whereClause.append(" AND ");
        }
        whereClause.append(k.toString()).append("='").append(v.toString()).append("'");
        // String pi = conn.realEscapeString(p).toString();
        // pi = pi.replaceAll("\\\\", "\\\\\\\\");
      }

      StringBuilderValue query = new StringBuilderValue();
      query.append("SELECT * FROM ").append(tableName).append(" WHERE ").append(whereClause);

      PostgresResult result = pg_query(env, conn, query.toString());

      return pg_fetch_all(env, result);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Sends a request to execute a prepared statement with given parameters,
   * without waiting for the result(s)
   */
  public static boolean pg_send_execute(Env env,
                                        @NotNull Postgres conn,
                                        String stmtName,
                                        ArrayValue params)
  {
    try {

      // Note: for now, this is essentially the same as pg_execute.

      PostgresResult result = pg_execute(env, conn, stmtName, params);

      conn.setAsynchronousResult(result);

      if (result != null) {
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
  public static boolean pg_send_prepare(Env env,
                                        @NotNull Postgres conn,
                                        String stmtName,
                                        String query)
  {
    try {

      // Note: for now, this is the same as pg_prepare.

      PostgresStatement stmt = pg_prepare(env, conn, stmtName, query);

      conn.setAsynchronousStatement(stmt);

      if (stmt != null) {
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
  public static boolean pg_send_query_params(Env env,
                                             @NotNull Postgres conn,
                                             String query,
                                             ArrayValue params)
  {
    try {

      ArrayValueImpl arrayImpl = (ArrayValueImpl)params;
      int sz = arrayImpl.size();

      for (int i=0; i<sz; i++) {
        StringValue p = arrayImpl.get(LongValue.create(i)).toStringValue();
        String pi = conn.realEscapeString(p).toString();
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
  public static boolean pg_send_query(Env env,
                                      @NotNull Postgres conn,
                                      String query)
  {
    try {

      PostgresResult result = pg_query(env, conn, query);

      // We probably won't need this for now. See pg_get_result().
      // conn.setAsynchronousResult(result);

      // This is to be compliant with real expected results.
      // Even a SELECT * FROM doesnotexist returns true from pg_send_query.
      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Set the client encoding
   */
  public static int pg_set_client_encoding(Env env,
                                           @NotNull Postgres conn,
                                           String encoding)
  {
    //@todo conn should be optional
    if (conn == null)
      conn = getConnection(env);

    if (pg_query(env, conn, "SET CLIENT_ENCODING TO '" + encoding +"'") == null) {
      return -1;
    }

    return 0;
  }

  /**
   * Determines the verbosity of messages returned by pg_last_error() and pg_result_error()
   */
  public static int pg_set_error_verbosity(Env env,
                                           @NotNull Postgres conn,
                                           int intVerbosity)
  {
    try {

      //@todo conn should be optional

      String verbosity;

      PostgresResult result = pg_query(env, conn, "SHOW log_error_verbosity");

      ArrayValue arr = pg_fetch_row(env, result, LongValue.create(0));

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
  public static boolean pg_trace(Env env,
                                 Path path,
                                 @Optional String mode,
                                 @Optional Postgres conn)
  {
    env.stub("pg_trace");

    return false;
  }

  /**
   * Returns the current in-transaction status of the server
   */
  public static int pg_transaction_status(Env env,
                                          @Optional Postgres conn)
  {
    return PGSQL_TRANSACTION_IDLE;
  }

  /**
   * Return the TTY name associated with the connection
   */
  public static String pg_tty(Env env,
                              @Optional Postgres conn)
  {
    // Note:  pg_tty() is obsolete, since the server no longer pays attention to
    // the TTY setting, but the function remains for backwards compatibility.

    env.stub("pg_tty");

    return "";
  }

  /**
   * Unescape binary for bytea type
   */
  @ReturnNullAsFalse
  public static String pg_unescape_bytea(Env env,
                                         String data)
  {
    try {

      byte dataBytes[] = data.getBytes();

      Class cl = Class.forName("org.postgresql.util.PGbytea");

      Method method = cl.getDeclaredMethod("toBytes", new Class[] {byte[].class});

      return new String((byte[]) method.invoke(cl, new Object[] {dataBytes}));

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Disable tracing of a PostgreSQL connection
   */
  public static boolean pg_untrace(Env env,
                                   @Optional Postgres conn)
  {
    // Always returns TRUE

    env.stub("pg_untrace");

    return true;
  }

  /**
   * Update table
   */
  public static boolean pg_update(Env env,
                                  @NotNull Postgres conn,
                                  String tableName,
                                  ArrayValue data,
                                  ArrayValue condition,
                                  @Optional int options)
  {
    // From php.net: This function is EXPERIMENTAL.

    // The behaviour of this function, the name of this function, and
    // anything else documented about this function may change without
    // notice in a future release of PHP. Use this function at your own risk.

    try {

      if (options > 0) {
        throw new UnimplementedException("pg_update with options");
      }

      StringBuilder values = new StringBuilder();

      boolean isFirst = true;

      for (Map.Entry<Value,Value> entry : data.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          values.append(", ");
        }
        values.append(k.toString());
        values.append("='");
        values.append(v.toString());
        values.append("'");
      }

      StringBuilder whereClause = new StringBuilder();

      isFirst = true;

      for (Map.Entry<Value,Value> entry : condition.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          whereClause.append(" AND ");
        }
        whereClause.append(k.toString());
        whereClause.append("='");
        whereClause.append(v.toString());
        whereClause.append("'");
      }

      StringBuilder query = new StringBuilder();
      query.append("UPDATE ");
      query.append(tableName);
      query.append(" SET ");
      query.append(values);
      query.append(" WHERE ");
      query.append(whereClause);

      pg_query(env, conn, query.toString());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns an array with client, protocol and server version (when available)
   */
  @ReturnNullAsFalse
  public static String pg_version(Env env,
                                  @Optional Postgres conn)
  {
    try {

      //@todo return an array

      if (conn == null)
        conn = getConnection(env);

      return conn.getServerInfo();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  private static Postgres getConnection(Env env)
  {
    Postgres conn = (Postgres) env.getSpecialValue("caucho.postgres");

    if (conn != null)
      return conn;

    String driver = "org.postgresql.Driver";
    String url = "jdbc:postgresql://localhost:5432/";

    conn = new Postgres(env, "localhost", "", "", "", 5432, driver, url);

    env.setSpecialValue("caucho.postgres", conn);

    return conn;
  }

  private static String escapeCharsToRegex(String []stringMap,
                                           Pattern pattern,
                                           String chars)
  {
    // Change to StringBuilder when Matcher.appendReplacement/appendTail is available
    // for StringBuilder. Currently, only StringBuffer is supported.
    StringBuffer stringBuilder = new StringBuffer();
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
      matcher.appendReplacement(stringBuilder, "\\\\"+replacement);
    }
    matcher.appendTail(stringBuilder);

    return stringBuilder.toString();
  }

  private static int writeLobInternal(Object largeObject,
                                      InputStream is,
                                      int len)
  {
    try {

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("getOutputStream", null);

      OutputStream os = (OutputStream) method.invoke(largeObject, new Object[] {});

      int written = 0;

      int b;

      while(((b = is.read()) >= 0) && (written++ < len)) {
        os.write(b);
      }

      os.close();

      return written;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }
}
