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

package com.caucho.sql.spy;

import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Spying on a driver.
 */
public class SpyXADataSource implements XADataSource {
  protected final static Logger log
    = Logger.getLogger(SpyXADataSource.class.getName());
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
  @Override
  public XAConnection getXAConnection()
    throws SQLException
  {
    long start = start();
    
    try {
      XAConnection conn = _dataSource.getXAConnection();

      String connId = _id + "." + _connCount++;

      log(start, "getXAConnection() -> " + connId + ":" + conn);

      return new SpyXAConnection(conn, connId);
    } catch (SQLException e) {
      log(start, "exn-connect(" + e + ")");
      
      throw e;
    }
  }

  /**
   * Returns the XAConnection.
   */
  @Override
  public XAConnection getXAConnection(String user, String password)
    throws SQLException
  {
    long start = start();
    
    try {
      XAConnection conn = _dataSource.getXAConnection(user, password);

      String connId = _id + "." + _connCount++;

      log(start, "getXAConnection(" + user + ") -> " + connId + ":" + conn);

      return new SpyXAConnection(conn, connId);
    } catch (SQLException e) {
      log(start, "exn-connect(" + e + ")");
      
      throw e;
    }
  }

  /**
   * Returns the login timeout.
   */
  @Override
  public int getLoginTimeout()
    throws SQLException
  {
    return _dataSource.getLoginTimeout();
  }

  /**
   * Sets the login timeout.
   */
  @Override
  public void setLoginTimeout(int timeout)
    throws SQLException
  {
    _dataSource.setLoginTimeout(timeout);
  }

  /**
   * Returns the log writer
   */
  @Override
  public PrintWriter getLogWriter()
    throws SQLException
  {
    return _dataSource.getLogWriter();
  }

  /**
   * Sets the log writer.
   */
  @Override
  public void setLogWriter(PrintWriter log)
    throws SQLException
  {
    _dataSource.setLogWriter(log);
  }
  
  public Logger getParentLogger() throws SQLFeatureNotSupportedException
  {
    return null;
  }

  protected long start()
  {
    return CurrentTime.getExactTime();
  }
  
  protected void log(long start, String msg)
  {
    long delta = CurrentTime.getExactTime() - start;
    
    log.fine("[" + delta + "ms] " + _id + ":" + msg);
  }

  @Override
  public String toString()
  {
    return "SpyXADataSource[id=" + _id + ",data-source=" + _dataSource + "]";
  }
}
