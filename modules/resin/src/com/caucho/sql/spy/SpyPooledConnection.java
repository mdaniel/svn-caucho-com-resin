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

import com.caucho.sql.DriverConfig;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.*;
import javax.sql.StatementEventListener;

/**
 * Spying on a connection.
 */
public class SpyPooledConnection implements javax.sql.PooledConnection {
  protected final static Logger log
    = Logger.getLogger(SpyPooledConnection.class.getName());
  protected final static L10N L = new L10N(SpyPooledConnection.class);

  protected String _id;

  private SpyDataSource _spyDataSource;

  // The underlying connection
  private PooledConnection _pconn;

  /**
   * Creates a new SpyConnection.
   */
  public SpyPooledConnection(PooledConnection conn,
                             String id)
  {
    _spyDataSource = new SpyDataSource();
    
    _pconn = conn;
    _id = id;
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
  public void addConnectionEventListener(ConnectionEventListener listener)
  {
    _pconn.addConnectionEventListener(listener);
  }

  @Override
  public void removeConnectionEventListener(ConnectionEventListener listener)
  {
    _pconn.removeConnectionEventListener(listener);
  }

  @Override
  public void addStatementEventListener(StatementEventListener listener)
  {
    _pconn.addStatementEventListener(listener);
  }

  @Override
  public void removeStatementEventListener(StatementEventListener listener)
  {
    _pconn.removeStatementEventListener(listener);
  }
  
  @Override
  public Connection getConnection()
    throws SQLException
  {
    long start = start();
    
    try {
      Connection conn = _pconn.getConnection();

      String connId = null;
      
      if (log.isLoggable(Level.FINE)) {
        connId = _spyDataSource.createConnectionId(null);

        log(start, "connect() -> " + connId + ":" + conn);
      }

      // return new SpyConnection(conn, _spyDataSource, connId);
      
      return conn;
    } catch (SQLException e) {
      log(start, "exn-connect(" + e + ")");
      
      throw e;
    }
  }

  @Override
  public void close()
    throws SQLException
  {
    try {
      _pconn.close();

      log.fine(_id + ":pool-close()");
    } catch (SQLException e) {
      log.fine(_id + ":exn-close(" + e + ")");
      
      throw e;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + _id + ",conn=" + _pconn + "]";
  }
}
