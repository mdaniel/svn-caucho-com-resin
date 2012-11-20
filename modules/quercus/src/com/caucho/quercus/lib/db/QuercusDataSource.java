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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.db;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class QuercusDataSource implements DataSource {
  private final DataSource _ds;
  private final String _user;
  private final String _pass;

  private final boolean _isAllowPerConnectionUserPass;

  /**
   * @param ds
   * @param user
   * @param pass
   * @param isAllowPerConnectionUserPass true to pass through the user/pass arg
   *        sent to this class' getConnection(), otherwise use this class'
   *        preset user/pass if set
   */
  public QuercusDataSource(DataSource ds,
                           String user,
                           String pass,
                           boolean isAllowPerConnectionUserPass)
  {
    _ds = ds;

    _user = user;
    _pass = pass;

    _isAllowPerConnectionUserPass = isAllowPerConnectionUserPass;
  }

  public Connection getConnection()
    throws SQLException
  {
    if (_user != null) {
      return _ds.getConnection(_user, _pass);
    }
    else {
      return _ds.getConnection();
    }
  }

  public Connection getConnection(String user, String pass)
    throws SQLException
  {
    if (user != null && _isAllowPerConnectionUserPass) {
      return _ds.getConnection(user, pass);
    }
    else {
      return getConnection();
    }
  }

  @Override
  public int getLoginTimeout()
    throws SQLException
  {
    return _ds.getLoginTimeout();
  }

  @Override
  public PrintWriter getLogWriter()
    throws SQLException
  {
    return _ds.getLogWriter();
  }

  @Override
  public void setLoginTimeout(int seconds)
    throws SQLException
  {
    _ds.setLoginTimeout(seconds);
  }

  @Override
  public void setLogWriter(PrintWriter out)
    throws SQLException
  {
    _ds.setLogWriter(out);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    return _ds.isWrapperFor(iface);
  }

  @Override
  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    return _ds.unwrap(iface);
  }

  /**
   * new interface method in JDK 1.7 CommonDataSource
   */
  public Logger getParentLogger()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + _ds
                                      + "," + _user + "]";
  }
}
