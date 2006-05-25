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

import com.caucho.util.L10N;

import com.caucho.quercus.env.*;

import com.caucho.quercus.module.Optional;

/**
 * mysqli object oriented API facade
 */
public class Mysqli extends JdbcConnectionResource {
  private static final Logger log = Logger.getLogger(Mysqli.class.getName());
  private static final L10N L = new L10N(Mysqli.class);

  public Mysqli(Env env,
                @Optional("localhost") String host,
                @Optional String user,
                @Optional String password,
                @Optional String db,
                @Optional("3306") int port,
                @Optional String socket,
                @Optional int flags,
                @Optional String driver,
                @Optional String url)
  {
    super(env);

    real_connect(env, host, user, password, db, port, socket, flags, driver, url);
  }

  public Mysqli(Env env)
  {
    super(env);
  }

  /**
   * Connects to the underlying database.
   */
  public boolean real_connect(Env env,
                              @Optional("localhost") String host,
                              @Optional String userName,
                              @Optional String password,
                              @Optional String dbname,
                              @Optional("3306") int port,
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
        driver = "com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource";
      }

      if (url == null || url.equals("")) {
        url = "jdbc:mysql://" + host + ":" + port + "/" + dbname;
      }

      Connection jConn = env.getConnection(driver, url, userName, password);

      setConnection(host, userName, password, dbname, port, jConn, driver, url, false);

      return true;

    } catch (SQLException e) {
      env.warning("A link to the server could not be established. " + e.toString());
      env.setSpecialValue("mysqli.connectErrno",new LongValue(e.getErrorCode()));
      env.setSpecialValue("mysqli.connectError", new StringValueImpl(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      env.warning("A link to the server could not be established. " + e.toString());
      env.setSpecialValue("mysqli.connectError", new StringValueImpl(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  public String toString()
  {
    if (isConnected())
      return "Mysqli[" + get_host_name() + "]";
    else
      return "Mysqli[]";
  }
}
