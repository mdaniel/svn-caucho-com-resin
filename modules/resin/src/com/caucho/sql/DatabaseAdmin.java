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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */


package com.caucho.sql;

import java.util.Date;

import com.caucho.env.dbpool.ConnectionPool;
import com.caucho.management.server.DatabaseMXBean;
import com.caucho.management.server.JdbcDriverMXBean;
import com.caucho.v5.jmx.server.ManagedObjectBase;

public class DatabaseAdmin extends ManagedObjectBase
  implements DatabaseMXBean
{
  private final DBPool _dbPool;
  private final ConnectionPool _jcaPool;

  public DatabaseAdmin(DBPool dbPool, ConnectionPool jcaPool)
  {
    _dbPool = dbPool;
    _jcaPool = jcaPool;
  }

  @Override
  public String getUrl()
  {
    return _dbPool.getURL();
  }

  /**
   * Returns true if spy is enabled
   */
  @Override
  public boolean isSpy()
  {
    return _dbPool.isSpy();
  }

  /**
   * Returns the pool's jdbc drivers
   */
  @Override
  public JdbcDriverMXBean []getDrivers()
  {
    return _dbPool.getDriverAdmin();
  }
  
  @Override
  public String getName()
  {
    return _dbPool.getName();
  }

  //
  // ConnectionPoolMXBean
  //

  /**
   * Returns the maximum number of connections.
   */
  @Override
  public int getMaxConnections()
  {
    return _jcaPool.getMaxConnections();
  }
  
  /**
   * Returns the number of overflow connections.
   */
  @Override
  public int getMaxOverflowConnections()
  {
    return _jcaPool.getMaxOverflowConnections();
  }
  
  /**
   * Returns the max number of connections trying to connect.
   */
  @Override
  public int getMaxCreateConnections()
  {
    return _jcaPool.getMaxCreateConnections();
  }
  
  /**
   * Returns the pool idle time in milliseconds.
   */
  @Override
  public long getMaxIdleTime()
  {
    return _jcaPool.getMaxIdleTime();
  }
  
  /**
   * Returns the maximum number of idle connections
   */
  @Override
  public int getMaxIdleCount()
  {
    return _jcaPool.getMaxIdleCount();
  }
  
  /**
   * Returns the maximum number of idle connections
   */
  @Override
  public int getMinIdleCount()
  {
    return _jcaPool.getMinIdleCount();
  }
  
  /**
   * Returns the pool active time in milliseconds.
   */
  @Override
  public long getMaxActiveTime()
  {
    return _jcaPool.getMaxActiveTime();
  }
  
  /**
   * Returns the pool time in milliseconds.
   */
  @Override
  public long getMaxPoolTime()
  {
    return _jcaPool.getMaxPoolTime();
  }
  
  /**
   * How long to wait for connections when timed out.
   */
  @Override
  public long getConnectionWaitTime()
  {
    return _jcaPool.getConnectionWaitTime();
  }
  
  /**
   * Returns true for the JCA shared attribute.
   */
  @Override
  public boolean isShareable()
  {
    return _jcaPool.isShareable();
  }
  
  /**
   * Returns true if the local-transaction-optimization is allowed
   */
  @Override
  public boolean isLocalTransactionOptimization()
  {
    return _jcaPool.isLocalTransactionOptimization();
  }
  
  //
  // Statistics
  //
  
  /**
   * Returns the total number of connections.
   */
  @Override
  public int getConnectionCount()
  {
    return _jcaPool.getConnectionCount();
  }

  /**
   * Returns the number of active connections.
   */
  @Override
  public int getConnectionActiveCount()
  {
    return _jcaPool.getConnectionActiveCount();
  }

  /**
   * Returns the number of idle connections.
   */
  @Override
  public int getConnectionIdleCount()
  {
    return _jcaPool.getConnectionIdleCount();
  }

  /**
   * Returns the number of idle connections.
   */
  @Override
  public int getConnectionCreateCount()
  {
    return _jcaPool.getConnectionCreateCount();
  }

  /**
   * Returns the total number of connections.
   */
  @Override
  public long getConnectionCountTotal()
  {
    return _jcaPool.getConnectionCountTotal();
  }

  /**
   * Returns the total number of created connections.
   */
  @Override
  public long getConnectionCreateCountTotal()
  {
    return _jcaPool.getConnectionCreateCountTotal();
  }

  /**
   * Returns the total number of failed connections.
   */
  @Override
  public long getConnectionFailCountTotal()
  {
    return _jcaPool.getConnectionFailCountTotal();
  }

  /**
   * Returns the last failed connection time.
   */
  @Override
  public Date getLastFailTime()
  {
    return _jcaPool.getLastFailTime();
  }

  //
  // Operations
  //

  /**
   * Clears all idle connections in the pool.
   */
  @Override
  public void clear()
  {
    _jcaPool.clear();
  }

  void register()
  {
    registerSelf();
  }

  /**
   * 
   */
  public void close()
  {
    unregisterSelf();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
