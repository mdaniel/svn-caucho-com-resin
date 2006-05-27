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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.db;

import java.sql.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.HashMap;

import com.caucho.util.L10N;

import com.caucho.quercus.QuercusModuleException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;

import com.caucho.quercus.module.Reference;

/**
 * mysqli object oriented API facade
 */
public class MysqliStatement extends JdbcStatementResource {
  private static final Logger log = Logger.getLogger(MysqliStatement.class.getName());
  private static final L10N L = new L10N(MysqliStatement.class);

  // Oracle Statement type:
  // (SELECT, UPDATE, DELETE, INSERT, CREATE, DROP, ALTER, BEGIN, DECLARE, UNKNOWN)
  private String _stmtType;

  // Binding variables for oracle statements
  private HashMap<String,Integer> _bindingVariables = new HashMap<String,Integer>();

  // Oracle internal result buffer
  private Value _resultBuffer;

  // Binding variables for oracle statements with define_by_name (TOTALLY DIFFERENT FROM ?)
  //
  // Example:
  //
  // $stmt = oci_parse($conn, "SELECT empno, ename FROM emp");
  //
  // /* the define MUST be done BEFORE oci_execute! */
  //
  // oci_define_by_name($stmt, "EMPNO", $empno);
  // oci_define_by_name($stmt, "ENAME", $ename);
  //
  // oci_execute($stmt);
  //
  // while (oci_fetch($stmt)) {
  //    echo "empno:" . $empno . "\n";
  //    echo "ename:" . $ename . "\n";
  // }
  //
  private HashMap<String,Value> _byNameVariables = new HashMap<String,Value>();

  MysqliStatement(Mysqli conn)
  {
    super(conn);
  }

  /**
   * returns the number of affected rows.
   */
  public int affected_rows()
  {
    return validateConnection().getAffectedRows();
  }

  /**
   * creates _types and _params array for this prepared statement.
   *
   * @param types  = string of i,d,s,b (ie: "idds")
   * @param params = array of values (probably Vars)
   * @return true on success false on failure
   */
  public boolean bind_param(Env env,
                            String types,
                            @Reference Value[] params)
  {
    return bindParams(env, types, params);
  }

  /**
   * binds outparams to result set
   */
  public boolean bind_result(Env env,
                             @Reference Value[] outParams)
  {
    return bindResults(env, outParams);
  }

  /**
   * Seeks to a given result
   */
  public void data_seek(int offset)
  {
    dataSeek(offset);
  }

  /**
   * Returns the mysql error number
   */
  public int errno()
  {
    return errorCode();
  }

  /**
   * Returns the mysql error message
   */
  public String error()
  {
    return errorMessage();
  }

  /**
   * Frees the associated result
   */
  public void free_result()
  {
    freeResult();
  }

  /**
   * Returns the number of rows in the result
   */
  public Value num_rows()
  {
    if (getResultSet() != null)
      return JdbcResultResource.getNumRows(getResultSet());
    else
      return BooleanValue.FALSE;
  }

  /**
   * counts the number of markers in the query string
   */
  public int param_count()
  {
    return paramCount();
  }

  /**
   * Resets the statement
   */
  public boolean reset()
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
   */
  public Value result_metadata(Env env)
  {
    try {

      if (getResultSet() != null) {
        return new MysqliResult(getMetaData(),
                                validateConnection());
      }
      else
        return null;

    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the SQLSTATE error
   */
  public String sqlstate()
  {
    return "HY" + errno();
  }

  /**
   * Saves the result as buffered.
   */
  public boolean store_result()
  {
    return true;
  }
}
