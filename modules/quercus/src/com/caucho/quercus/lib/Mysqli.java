/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import java.sql.SQLException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.resources.JdbcConnectionResource;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.Optional;

/**
 * mysqli object oriented API facade
 */
public class Mysqli {
  private static final Logger log = Logger.getLogger(Mysqli.class.getName());
  private static final L10N L = new L10N(Mysqli.class);

  private Env _env;
  private JdbcConnectionResource _conn;
  
  public Mysqli(Env env)
  {
    _env = env;
  }

  /**
   * returns JdbcResultResource representing the results of the query.
   *
   * <i>resultMode</i> is ignored, MYSQLI_USE_RESULT would represent
   * an unbuffered query, but that is not supported.
   */
  public Value query(String sql,
    		     @Optional("MYSQLI_STORE_RESULT") int resultMode)
  {
    return QuercusMysqliModule.mysqli_query(_conn, sql, resultMode);
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
			      @Optional int flags)
  {
    if (_conn != null) {
      env.warning(L.l("Connection is already opened to '{0}'", _conn));
      return false;
    }

    Value value = QuercusMysqliModule.mysqli_connect(env, host,
						     userName, password,
						     dbname, port, socket);

    if (value instanceof JdbcConnectionResource)
      _conn = (JdbcConnectionResource) value;

    return _conn != null;
  }

  /**
   * Selects the underlying database/catalog to use.
   *
   * @param dbname the name of the database to select.
   */
  public boolean select_db(String dbname)
  {
    validateConnection();
    
    try {
      _conn.setCatalog(dbname);

      return true;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      _env.warning(e.getMessage());
      return false;
    }
  }

  /**
   * Closes the connection.
   */
  public boolean close(Env env)
  {
    JdbcConnectionResource conn = _conn;
    _conn = null;
    
    if (conn != null)
      return QuercusMysqliModule.mysqli_close(env, conn);
    else
      return false;
  }

  private void validateConnection()
  {
    if (_conn == null)
      _env.error(L.l("Connection is not properly initialized"));
  }

  public String toString()
  {
    if (_conn != null)
      return "Mysqli[" + _conn + "]";
    else
      return "Mysqli[]";
  }
}
