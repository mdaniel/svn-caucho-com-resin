/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.sql.spy;

import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Spying on a driver.
 */
public class SpyConnectionPoolDataSource implements ConnectionPoolDataSource {
  protected final static Logger log
    = Logger.getLogger(SpyConnectionPoolDataSource.class.getName());
  protected final static L10N L = new L10N(SpyConnectionPoolDataSource.class);

  private String _id;
  private AtomicInteger _connCount = new AtomicInteger();

  // The underlying data source
  private ConnectionPoolDataSource _dataSource;

  /**
   * Creates a new SpyDriver.
   */
  public SpyConnectionPoolDataSource(ConnectionPoolDataSource dataSource,
                                     String id)
  {
    _dataSource = dataSource;
    _id = id;
  }

  /**
   * Returns the pooled connection.
   */
  @Override
  public PooledConnection getPooledConnection()
    throws SQLException
  {
    long start = start();
    
    try {
      PooledConnection conn = _dataSource.getPooledConnection();

      String connId = _id + "." + _connCount.getAndIncrement();

      log(start, "getConnectionPool() -> " + connId + ":" + conn);

      return new SpyPooledConnection(conn, connId);
    } catch (SQLException e) {
      log(start, "exn-connect(" + e + ")");
      
      throw e;
    }
  }

  /**
   * Returns the XAConnection.
   */
  @Override
  public PooledConnection getPooledConnection(String user, String password)
    throws SQLException
  {
    long start = start();
    
    try {
      PooledConnection conn = _dataSource.getPooledConnection(user, password);

      String connId = _id + "." + _connCount.getAndIncrement();

      log(start, "getPooledConnection(" + user + ") -> " + connId + ":" + conn);

      return new SpyPooledConnection(conn, connId);
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

  protected long start()
  {
    return CurrentTime.getExactTime();
  }
  
  protected void log(long start, String msg)
  {
    long delta = CurrentTime.getExactTime() - start;
    
    log.fine("[" + delta + "ms] " + _id + ":" + msg);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + _id + ",data-source=" + _dataSource + "]";
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException
  {
    // TODO Auto-generated method stub
    return null;
  }
}
