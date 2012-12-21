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
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Oracle statement class. Since Oracle has no object oriented API,
 * this is essentially a JdbcStatementResource.
 */
public class OracleStatement extends JdbcPreparedStatementResource {
  private static final Logger log
    = Logger.getLogger(OracleStatement.class.getName());

  private Oracle _conn;

  // Oracle statement has a notion of number of fetched rows
  // (See also: OracleModule.oci_num_rows)
  private int _fetchedRows;

  // Binding variables for Oracle statements
  private HashMap<String,Integer> _bindingVariables =
      new HashMap<String,Integer>();
  private OracleOciLob _outParameter;

  // Oracle internal result buffer
  private Value _resultBuffer;

  // Binding variables for Oracle statements with define_by_name
  // (TOTALLY DIFFERENT FROM ?)
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

  /**
   * Constructs an OracleStatement
   *
   * @param conn Oracle connection
   */
  OracleStatement(Oracle conn)
  {
    super(conn);
    _fetchedRows = 0;
    _outParameter = null;
  }

  protected boolean execute(Env env, int mode)
  {
    try {

      //  Scenarios for oci_execute.
      //
      //
      //  1. Simple query: oci_parse / oci_execute
      //
      //  $query = 'SELECT * FROM TEST';
      //
      //  $stmt = oci_parse($conn, $query);
      //
      //  oci_execute($stmt, OCI_DEFAULT);
      //
      //  $result = oci_fetch_array($stid, OCI_BOTH);
      //
      //
      //  2. Define by name: oci_parse / oci_define_by_name / oci_execute
      //
      //  $stmt = oci_parse($conn, "SELECT id, data FROM test");
      //
      //  /* the define MUST be done BEFORE oci_execute! */
      //
      //  oci_define_by_name($stmt, "ID", $myid);
      //  oci_define_by_name($stmt, "DATA", $mydata);
      //
      //  oci_execute($stmt, OCI_DEFAULT);
      //
      //  while ($row = oci_fetch($stmt)) {
      //     echo "id:" . $myid . "\n";
      //     echo "data:" . $mydata . "\n";
      //  }
      //
      //
      //  3. Cursors: oci_new_cursor / oci_parse /
      //     oci_bind_by_name / oci_execute($stmt) / oci_execute($cursor)
      //
      //  $stmt = oci_parse($conn,
      //  "CREATE OR REPLACE PACKAGE cauchopkgtestoci AS ".
      //  "TYPE refcur IS REF CURSOR; ".
      //  "PROCEDURE testproc (var_result out cauchopkgtestoci.refcur); ".
      //  "END cauchopkgtestoci; ");
      //
      //  oci_execute($stmt);
      //
      //  $stmt = oci_parse($conn,
      //  "CREATE OR REPLACE PACKAGE BODY cauchopkgtestoci IS ".
      //  "PROCEDURE testproc (var_result out cauchopkgtestoci.refcur) IS ".
      //  "BEGIN ".
      //  "OPEN var_result FOR SELECT data FROM caucho.test; ".
      //  "END testproc; ".
      //  "END; ");
      //
      //  oci_execute($stmt);
      //
      //  $curs = oci_new_cursor($conn);
      //
      //  $stmt = oci_parse($conn,
      //     "begin cauchopkgtestoci.testproc(:dataCursor); end;");
      //
      //  oci_bind_by_name($stmt, "dataCursor", $curs, 255, SQLT_RSET);
      //
      //  oci_execute($stmt);
      //
      //  oci_execute($curs);
      //
      //  while ($data = oci_fetch_row($curs)) {
      //     var_dump($data);
      //  }

      // Get the underlying JDBC connection.
      Connection conn = getJavaConnection(env);

      // Large Objects can not be used in auto-commit mode.
      conn.setAutoCommit(false);

      // Use the underlying callable statement to check different scenarios.
      CallableStatement callableStatement = getCallableStatement();

      // Check for case (3) oci_execute($cursor);
      // A previous statement has been executed and holds the result set.
      Object cursorResult = null;
      try {
        cursorResult = callableStatement.getObject(1);
        if ((cursorResult != null) && (cursorResult instanceof ResultSet)) {
          ResultSet rs = (ResultSet) cursorResult;

          setResultSet(rs);
          return true;
        }
      } catch (Exception e) {
        // We assume this is not case (3). No error.
      }

      // Case (1) or executing a more complex query.
      execute(env, false);

      OracleOciLob ociLob = getOutParameter();
      if (ociLob != null) {
        // Ex: java.sql.Clob outParameter = callableStatement.getClob(1);
        ociLob.setLob(callableStatement.getObject(1));
      }

      // Fetch and assign values to corresponding binded variables
      // This is necessary for LOB support and
      // should be reworked calling a private fetch method instead.
      // oci_fetch(env, stmt);

      if (mode == OracleModule.OCI_COMMIT_ON_SUCCESS) {
        conn.commit();
      }

      return true;
    }
    catch (SQLException e) {
      env.warning(e);

      resetBindingVariables();
      resetByNameVariables();

      return false;
    }
  }

  protected JdbcResultResource createResultSet(ResultSet rs)
  {
    return new OracleResult(rs, _conn);
  }

  /**
   * Assigns a variable name to the corresponding index
   *
   * @param name the variable name
   * @param value the corresponding index
   */
  public void putBindingVariable(String name, Integer value)
  {
    _bindingVariables.put(name, value);
  }

  /**
   * Returns a binding variable index
   *
   * @param name the variable name
   * @return the binding variable index
   */
  public Integer getBindingVariable(String name)
  {
    return _bindingVariables.get(name);
  }

  /**
   * Removes a binding variable
   *
   * @param name the binding variable name
   * @return the binding variable index
   */
  public Integer removeBindingVariable(String name)
  {
    return _bindingVariables.remove(name);
  }

  /**
   * Returns all binding variables
   *
   * @return a HashMap of variable name to index values
   */
  public HashMap<String,Integer> getBindingVariables()
  {
    return _bindingVariables;
  }

  /**
   * Removes all binding variables
   */
  public void resetBindingVariables()
  {
    _bindingVariables = new HashMap<String,Integer>();
  }

  /**
   * Sets the internal result buffer
   */
  public void setResultBuffer(Value resultBuffer)
  {
    _resultBuffer = resultBuffer;
  }

  /**
   * Returns the internal result buffer
   *
   * @return the result buffer
   */
  public Value getResultBuffer()
  {
    return _resultBuffer;
  }

  /**
   * Assigns a value to a variable
   *
   * @param name a variable name
   * @param value the variable value
   */
  public void putByNameVariable(String name, Value value)
  {
    _byNameVariables.put(name, value);
  }

  /**
   * Returns the variable value by name
   *
   * @param name the variable name
   * @return the variable value
   */
  public Value getByNameVariable(String name)
  {
    return _byNameVariables.get(name);
  }

  /**
   * Removes a variable given the corresponding name
   *
   * @param name the variable name
   * @return the variable value
   */
  public Value removeByNameVariable(String name)
  {
    return _byNameVariables.remove(name);
  }

  /**
   * Returns all variable names and corresponding values
   *
   * @return a HashMap of variable names to corresponding values
   */
  public HashMap<String,Value> getByNameVariables()
  {
    return _byNameVariables;
  }

  /**
   * Removes all variables
   */
  public void resetByNameVariables()
  {
    _byNameVariables = new HashMap<String,Value>();
  }

  /**
   * Increases the number of fetched rows.
   *
   * @return the new number of fetched rows
   */
  protected int increaseFetchedRows() {
    return ++_fetchedRows;
  }

  /**
   * Gets the underlying callable statement.
   */
  protected CallableStatement getCallableStatement()
  {
    return (CallableStatement) getPreparedStatement();
  }

  /**
   * Gets the number of fetched rows.
   *
   * @return the number of fetched rows
   */
  protected int getFetchedRows() {
    return _fetchedRows;
  }

  /**
   * Gets the out parameter.
   *
   * @return the out parameter
   */
  protected OracleOciLob getOutParameter() {
    return _outParameter;
  }

  /**
   * Sets the out parameter.
   *
   * @param outParameter the new out parameter
   */
  protected void setOutParameter(OracleOciLob outParameter) {
    _outParameter = outParameter;
  }
}
