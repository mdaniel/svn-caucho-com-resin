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
 * oracle statement class (oracle has NO object oriented API)
 */
public class OracleStatement extends JdbcStatementResource {
  private static final Logger log = Logger.getLogger(OracleStatement.class.getName());
  private static final L10N L = new L10N(OracleStatement.class);

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

  OracleStatement(Oracle conn)
  {
    super(conn);
  }

  public void putBindingVariable(String name, Integer value)
  {
    _bindingVariables.put(name, value);
  }

  public Integer getBindingVariable(String name)
  {
    return _bindingVariables.get(name);
  }

  public Integer removeBindingVariable(String name)
  {
    return _bindingVariables.remove(name);
  }

  public HashMap<String,Integer> getBindingVariables()
  {
    return _bindingVariables;
  }

  public void resetBindingVariables()
  {
    _bindingVariables = new HashMap<String,Integer>();
  }

  public void setResultBuffer(Value resultBuffer)
  {
    _resultBuffer = resultBuffer;
  }

  public Value getResultBuffer()
  {
    return _resultBuffer;
  }

  public void putByNameVariable(String name, Value value)
  {
    _byNameVariables.put(name, value);
  }

  public Value getByNameVariable(String name)
  {
    return _byNameVariables.get(name);
  }

  public Value removeByNameVariable(String name)
  {
    return _byNameVariables.remove(name);
  }

  public HashMap<String,Value> getByNameVariables()
  {
    return _byNameVariables;
  }

  public void resetByNameVariables()
  {
    _byNameVariables = new HashMap<String,Value>();
  }
}
