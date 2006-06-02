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

import com.caucho.util.L10N;

import com.caucho.quercus.env.*;

import com.caucho.quercus.module.Optional;

/**
 * oracle connection class (oracle has NO object oriented API)
 */
public class Oracle extends JdbcConnectionResource {
  private static final Logger log = Logger.getLogger(Oracle.class.getName());
  private static final L10N L = new L10N(Oracle.class);

  public Oracle(Env env,
                @Optional("localhost") String host,
                @Optional String user,
                @Optional String password,
                @Optional String db,
                @Optional("1521") int port,
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
        driver = "oracle.jdbc.driver.OracleDriver";
      }

      if (url == null || url.equals("")) {
        if (dbname.indexOf("//") == 0) {
          // db is the url itself: "//db_host[:port]/database_name"
          url = "jdbc:oracle:thin:@" + dbname.substring(2);
          url = url.replace('/', ':');
        } else {
          url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbname;
        }
      }

      Connection jConn = env.getConnection(driver, url, userName, password);

      setConnection(host, userName, password, dbname, port, jConn, driver, url);

      return true;

    } catch (SQLException e) {
      env.warning("A link to the server could not be established. " + e.toString());
      env.setSpecialValue("oracle.connectErrno",new LongValue(e.getErrorCode()));
      env.setSpecialValue("oracle.connectError", new StringValueImpl(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      env.warning("A link to the server could not be established. " + e.toString());
      env.setSpecialValue("oracle.connectError", new StringValueImpl(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * returns a prepared statement
   */
  public OracleStatement prepare(Env env, String query)
  {
    OracleStatement stmt = new OracleStatement((Oracle)validateConnection());

    stmt.prepare(query);

    return stmt;
  }

  /**
   * Creates a database-specific result.
   */
  protected JdbcResultResource createResult(Statement stmt,
                                            ResultSet rs)
  {
    return new OracleResult(stmt, rs, this);
  }

}
