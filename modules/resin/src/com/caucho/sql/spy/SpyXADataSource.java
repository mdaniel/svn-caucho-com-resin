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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.sql.spy;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

import javax.sql.*;

import javax.transaction.*;
import javax.transaction.xa.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

/**
 * Spying on a driver.
 */
public class SpyXADataSource implements XADataSource {
  protected final static Logger log = Log.open(SpyXADataSource.class);
  protected final static L10N L = new L10N(SpyXADataSource.class);

  private static int _staticId;
  private int _id;
  private int _connCount;

  // The underlying data source
  private XADataSource _dataSource;

  /**
   * Creates a new SpyDriver.
   */
  public SpyXADataSource(XADataSource dataSource)
  {
    _dataSource = dataSource;
    _id = _staticId++;
  }

  /**
   * Returns the XAConnection.
   */
  public XAConnection getXAConnection()
    throws SQLException
  {
    try {
      XAConnection conn = _dataSource.getXAConnection();

      int connId = _connCount++;

      log.info(_id + ":getXAConnection() -> " + connId + ":" + conn);

      return new SpyXAConnection(conn, connId);
    } catch (SQLException e) {
      log.info(_id + ":exn-connect(" + e + ")");
      
      throw e;
    }
  }

  /**
   * Returns the XAConnection.
   */
  public XAConnection getXAConnection(String user, String password)
    throws SQLException
  {
    try {
      XAConnection conn = _dataSource.getXAConnection(user, password);

      int connId = _connCount++;

      log.info(_id + ":getXAConnection(" + user + ") -> " + connId + ":" + conn);

      return new SpyXAConnection(conn, connId);
    } catch (SQLException e) {
      log.info(_id + ":exn-connect(" + e + ")");
      
      throw e;
    }
  }

  /**
   * Returns the login timeout.
   */
  public int getLoginTimeout()
    throws SQLException
  {
    return _dataSource.getLoginTimeout();
  }

  /**
   * Sets the login timeout.
   */
  public void setLoginTimeout(int timeout)
    throws SQLException
  {
    _dataSource.setLoginTimeout(timeout);
  }

  /**
   * Returns the log writer
   */
  public PrintWriter getLogWriter()
    throws SQLException
  {
    return _dataSource.getLogWriter();
  }

  /**
   * Sets the log writer.
   */
  public void setLogWriter(PrintWriter log)
    throws SQLException
  {
    _dataSource.setLogWriter(log);
  }

  public String toString()
  {
    return "SpyXADataSource[id=" + _id + ",data-source=" + _dataSource + "]";
  }
}
