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

import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.env.*;

import com.caucho.quercus.module.Optional;

/**
 * postgres connection class (postgres has NO object oriented API)
 */
public class Postgres extends JdbcConnectionResource {
  private static final Logger log = Logger.getLogger(Postgres.class.getName());
  private static final L10N L = new L10N(Postgres.class);

  PostgresResult _asyncResult;
  PostgresStatement _asyncStmt;

  // named prepared statements for postgres
  private HashMap<String,PostgresStatement> _stmtTable
    = new HashMap<String,PostgresStatement>();

  public Postgres(Env env,
                  @Optional("localhost") String host,
                  @Optional String user,
                  @Optional String password,
                  @Optional String db,
                  @Optional("5432") int port,
                  @Optional String driver,
                  @Optional String url)
  {
    super(env);

    realConnect(env, host, user, password, db, port, "", 0, driver, url);
  }

  /**
   * Connects to the underlying database.
   */
  public boolean realConnect(Env env,
                             @Optional("localhost") String host,
                             @Optional String userName,
                             @Optional String password,
                             @Optional String dbname,
                             @Optional("5432") int port,
                             @Optional String socket,
                             @Optional int flags,
                             @Optional String driver,
                             @Optional String url)
  {
    if (isConnected()) {
      env.warning(L.l("Connection is already opened to '{0}'", this));
      return false;
    }

    try {

      if (host == null || host.equals(""))
        host = "localhost";

      if (driver == null || driver.equals("")) {
        driver = "org.postgresql.Driver";
      }

      if (url == null || url.equals("")) {
        url = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
      }

      Connection jConn = env.getConnection(driver, url, userName, password);

      setConnection(host, userName, password, dbname, port, jConn, driver, url);

      return true;

    } catch (SQLException e) {
      env.warning("A link to the server could not be established. " + e.toString());
      env.setSpecialValue("postgres.connectErrno",new LongValue(e.getErrorCode()));
      env.setSpecialValue("postgres.connectError", new StringValueImpl(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      env.warning("A link to the server could not be established. " + e.toString());
      env.setSpecialValue("postgres.connectError", new StringValueImpl(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * returns a prepared statement
   */
  public PostgresStatement prepare(Env env, String query)
  {
    PostgresStatement stmt = new PostgresStatement((Postgres)validateConnection());

    stmt.prepare(query);

    return stmt;
  }

  /**
   * Creates a database-specific result.
   */
  protected JdbcResultResource createResult(Statement stmt,
                                            ResultSet rs)
  {
    return new PostgresResult(stmt, rs, this);
  }

  public void setAsynchronousResult(PostgresResult asyncResult)
  {
    _asyncResult = asyncResult;
  }

  public PostgresResult getAsynchronousResult()
  {
    return _asyncResult;
  }

  public PostgresStatement getAsynchronousStatement()
  {
    return _asyncStmt;
  }

  public void setAsynchronousStatement(PostgresStatement asyncStmt)
  {
    _asyncStmt = asyncStmt;
  }

  public void putStatement(String name,
                           PostgresStatement stmt)
  {
    _stmtTable.put(name, stmt);
  }

  public PostgresStatement getStatement(String name)
  {
    return _stmtTable.get(name);
  }

  public PostgresStatement removeStatement(String name)
  {
    return _stmtTable.remove(name);
  }

  /**
   * This function is overriden in Postgres to keep
   * result set references for php/4310 (see also php/1f33)
   */
  protected void keepResourceValues(Statement stmt)
  {
    setResultResource(createResult(stmt, null));
    addResultValue(getResultResource());
  }

  /**
   * This function is overriden in Postgres to keep
   * statement references for php/4310
   */
  protected boolean keepStatementOpen()
  {
    return true;
  }

  public String toString()
  {
    if (isConnected())
      return "Postgres[" + get_host_name() + "]";
    else
      return "Postgres[]";
  }
}
