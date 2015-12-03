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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.sql;

import com.caucho.util.L10N;
import com.caucho.util.SQLExceptionWrapper;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.sql.DataSource;

/**
 * The User DataSource returned from Resin's pool.
 */
public class UserDataSource implements DataSource {
  private static final L10N L = new L10N(UserDataSource.class);
  protected static final Logger log
    = Logger.getLogger(UserDataSource.class.getName());
  private final ManagedFactoryImpl _managedFactory;
  private final ConnectionManager _connManager;

  UserDataSource(ManagedFactoryImpl factory, ConnectionManager cm)
  {
    _managedFactory = factory;
    _connManager = cm;
  }

  /**
   * Returns the primary URL for the connection
   */
  public String getURL()
  {
    return _managedFactory.getURL();
  }

  /**
   * Returns a connection.
   */
  @Override
  public Connection getConnection()
    throws SQLException
  {
    try {
      return (Connection) _connManager.allocateConnection(_managedFactory,
                                                          null);
    } catch (ResourceException e) {
      Throwable cause;

      for (cause = e; cause != null; cause = cause.getCause()) {
        if (cause instanceof SQLException)
          throw (SQLException) cause;
      }

      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Returns a connection.
   */
  @Override
  public Connection getConnection(String username, String password)
    throws SQLException
  {
    try {
      Credential credential = null;

      if (username != null || password != null)
        credential = new Credential(username, password);

      return (Connection)  _connManager.allocateConnection(_managedFactory,
                                                           credential);
    } catch (ResourceException e) {
      Throwable cause;

      for (cause = e; cause != null; cause = cause.getCause()) {
        if (cause instanceof SQLException)
          throw (SQLException) cause;
      }

      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Returns the login timeout.
   */
  @Override
  public int getLoginTimeout()
  {
    return 0;
  }

  /**
   * Returns the login timeout.
   */
  @Override
  public void setLoginTimeout(int seconds)
  {
  }

  /**
   * Returns the log writer.
   */
  @Override
  public PrintWriter getLogWriter()
  {
    return null;
  }

  /**
   * Sets the log writer.
   */
  @Override
  public void setLogWriter(PrintWriter out)
  {
  }

  /**
   * Returns true if the impl has closed.
   */
  boolean isClosed()
  {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    if (iface.isAssignableFrom(this.getClass()))
      return (T) this;

    throw new SQLException(L.l("Can't unwrap `{0}' to `{1}'", this, iface));
  }

  @Override
  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    if (iface.isAssignableFrom(this.getClass()))
      return true;
    else
      return false;
  }
  
  public Logger getParentLogger()
  {
    return null;
  }

  @Override
  public String toString()
  {
    return "UserDataSource[" + _managedFactory + "]";
  }
}

