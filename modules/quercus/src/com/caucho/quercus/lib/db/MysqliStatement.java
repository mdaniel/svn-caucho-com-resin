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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;


/**
 * mysqli object oriented API facade
 */
public class MysqliStatement extends JdbcPreparedStatementResource {
  private static final Logger log = Logger
    .getLogger(MysqliStatement.class.getName());
  private static final L10N L = new L10N(MysqliStatement.class);

  /**
   * Constructor for MysqliStatement
   *
   * @param conn a Mysqli connection
   */
  MysqliStatement(Mysqli conn)
  {
    super(conn);
  }

  @Override
  public boolean execute(Env env) {
    return super.execute(env);
  }

  /**
   * Quercus function to get the field 'affected_rows'.
   */

  public int getaffected_rows(Env env)
  {
    return affected_rows(env);
  }

  /**
   * Returns the total number of rows changed, deleted,
   * or inserted by the last executed statement.
   *
   * @param env the PHP executing environment
   * @return  an integer greater than zero indicates the number of
   * rows affected or retrieved. Zero indicates that no records were
   * updated for an UPDATE/DELETE statement, no rows matched the
   * WHERE clause in the query or that no query has yet been
   * executed. -1 indicates that the query has returned an error.
   */
  public int affected_rows(Env env)
  {
    return getConnection().getAffectedRows();
  }

  /**
   * Binds variables to a prepared statement as parameters.
   *
   * @param env the PHP executing environment
   * @param types string of i,d,s,b (ie: "idds")
   * @param params array of values (probably Vars)
   * @return true on success or false on failure
   */
  public boolean bind_param(Env env,
                            StringValue typeStr,
                            @Reference Value[] params)
  {
    int len = typeStr.length();

    ColumnType[] types = new ColumnType[len];

    for (int i = 0; i < len; i++) {
      char ch = typeStr.charAt(i);

      if (ch == 's') {
        types[i] = ColumnType.STRING;
      }
      else if (ch == 'b') {
        types[i] = ColumnType.BLOB;
      }
      else if (ch == 'i') {
        types[i] = ColumnType.LONG;
      }
      else if (ch == 'd') {
        types[i] = ColumnType.DOUBLE;
      }
      else {
        env.warning(L.l("invalid param type '{0}' in string '{1}'", ch, typeStr));
        return false;
      }
    }

    return bindParams(env, types, params);
  }

  /**
   * Binds variables to a prepared statement for result storage.
   *
   * @param env the PHP executing environment
   * @param outParams the output variables
   * @return true on success or false on failure
   */
  public boolean bind_result(Env env,
                             @Reference Value[] outParams)
  {
    return bindResults(env, outParams);
  }

  /**
   * Closes a prepared statement.
   *
   * @param env the PHP executing environment
   * @return true on success or false on failure
   */
  @Override
  public boolean close()
  {
    return super.close();
  }

  /**
   * Seeks to an arbitrary row in statement result set.
   *
   * @param env the PHP executing environment
   * @param offset row offset
   * @return NULL on sucess or FALSE on failure
   */
  public Value data_seek(Env env,
                         int offset)
  {
    if (dataSeek(offset)) {
      return NullValue.NULL;
    }

    return BooleanValue.FALSE;
  }

  /**
   * Returns the error code for the most recent statement call.
   *
   * @param env the PHP executing environment
   * @return the error code or zero if no error occurred
   */
  public int errno()
  {
    return getErrorCode();
  }

  /**
   * Quercus function to get the field 'errno'.
   */
  public int geterrno()
  {
    return errno();
  }

  /**
   * Returns a string description for last statement error
   *
   * @param env the PHP executing environment
   * @return a string that describes the error or
   * an empty string if no error occurred.
   */
  public StringValue error(Env env)
  {
    return env.createString(getErrorMessage());
  }

  /**
   * Quercus function to get the field 'error'.
   */

  @ReturnNullAsFalse
  public StringValue geterror(Env env)
  {
    return error(env);
  }

  /**
   * Fetch results from a prepared statement into the bound variables.
   *
   * @param env the PHP executing environment
   * @return true on success, false on failure or
   * null if no more rows/data exists
   */
  public Value fetch(Env env)
  {
    return super.fetch(env);
  }

  /**
   * Frees the associated result.
   *
   * @param env the PHP executing environment
   */
  public void free_result(Env env)
  {
    freeResult();
  }

  /**
   * Returns the MysqliResult
   */
  public JdbcResultResource get_result(Env env)
  {
    return getResultSet();
  }

  @Override
  protected JdbcResultResource createResultSet(ResultSet rs)
  {
    return new MysqliResult(getPreparedStatement(), rs, (Mysqli) getConnection());
  }

  /**
   * Quercus function to get the field 'num_rows'.
   */
  public Value getnum_rows(Env env)
  {
    return num_rows(env);
  }

  /**
   * Returns the number of rows in the result.
   *
   * @param env the PHP executing environment
   * @return the number of rows in the result set
   */
  public Value num_rows(Env env)
  {
    JdbcResultResource rs = getResultSet();

    if (rs != null)
      return LongValue.create(rs.getNumRows());
    else
      return BooleanValue.FALSE;
  }

  /**
   * Quercus function to get the field 'param_count'.
   */
  public int getparam_count(Env env)
  {
    return param_count(env);
  }

  /**
   * Returns the number of parameter markers for this statement.
   *
   * @param env the PHP executing environment
   * @return the number of parameter markers for this statement
   */
  public int param_count(Env env)
  {
    return paramCount();
  }

  /**
   * Prepare a SQL statement for execution.
   *
   * @param env the PHP executing environment
   * @param query SQL query
   * @return true on success or false on failure
   */
  @Override
  public boolean prepare(Env env, String query)
  {
    return super.prepare(env, query);
  }

  /**
   * Resets the statement.
   *
   * @param env the PHP executing environment
   * @return true on success or false on failure
   */
  public boolean reset(Env env)
  {
    return true;
  }

  /**
   * mysqli_stmt_result_metadata seems to be some initial
   * step towards getting metadata from a resultset created
   * by a SELECT run by a prepared statement.
   *
   * NB: the $field variable in the following 2 PHP
   * scripts will be equivalent:
   *
   * $result = mysqli_query($link,"SELECT * FROM test");
   * $field = mysqli_fetch_field($result);
   *
   * AND
   *
   * $stmt = mysqli_prepare($link, "SELECT * FROM test");
   * mysqli_stmt_execute($stmt);
   * $metaData = mysqli_stmt_result_metadata($stmt);
   * $field = mysqli_fetch_field($metaData);
   *
   * So it seems that this function just provides a link into
   * the resultset.
   *
   * The PHP documentation is clear that this function returns
   * a mysqli_result with NO DATA.
   *
   * For simplicity, we return a mysqli_result with all the data.
   *
   * We check that mysqli_stmt_execute() has been run.
   *
   * From libmysql.c:
   *   This function should be used after mysql_stmt_execute().
   *   ...
   *   Next steps you may want to make:
   *   - find out number of columns is result set by calling
   *     mysql_num_fields(res)....
   *   - fetch metadata for any column with mysql_fetch_field...
   *
   * So basically, this function seems to exist only to be a
   * way to get at the metadata from a resultset generated
   * by a prepared statement.
   *
   * @param env the PHP executing environment
   * @return a result with meta data or false on failure
   */
  @ReturnNullAsFalse
  public MysqliResult result_metadata(Env env)
  {
    try {
      if (getResultSet() != null) {
        return new MysqliResult(getMetaData(), (Mysqli) getConnection());
      } else
        return null;

    } catch (SQLException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Send data in blocks.
   *
   * @param env the PHP executing environment
   * @param paramNumber indicates which parameter to associate the data with
   * @param data the data to be sent
   * @return true on success or false on failure
   */
  public boolean send_long_data(Env env,
                                int paramNumber,
                                String data)
  {
    throw new UnimplementedException("mysqli_stmt_send_long_data");
  }

  /**
   * Quercus function to get the field 'sqlstate'.
   */
  public StringValue getsqlstate(Env env)
  {
    return sqlstate(env);
  }

  /**
   * Returns SQLSTATE error from previous statement operation.
   *
   * @param env the PHP executing environment
   * @return the SQLSTATE (5-characters string) for
   * the last error. '00000' means no error
   */

  public StringValue sqlstate(Env env)
  {
    int code = errno();

    return env.createString(Mysqli.lookupSqlstate(code));
  }

  /**
   * Saves the result as buffered.
   *
   * @param env the PHP executing environment
   * @return true on success or false on failure
   */
  public boolean store_result(Env env)
  {
    return true;
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
    return getFieldCount();
  }

  /**
   * Quercus function to get the field 'insert_id'.
   */
  public Value getinsert_id(Env env)
  {
    return insert_id(env);
  }

  public Value insert_id(Env env)
  {
    return ((Mysqli) getConnection()).insert_id(env);
  }
}

