/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jmx.server;

import java.util.Date;

import com.caucho.jmx.Counter;
import com.caucho.jmx.Description;
import com.caucho.jmx.Gauge;
import com.caucho.jmx.Units;

/**
 * MBean API for the JCA connection pool.
 *
 * <pre>
 * baratine:type=ConnectionPool,name=jdbc/resin,...
 * </pre>
 */
@Description("A pool of reusable connections to a database")
public interface ConnectionPoolMXBean extends ManagedObjectMXBean {
  //
  // Configuration
  //

  /**
   * Returns the maximum number of connections.
   */
  @Description("The configured maximum number of connections")
  public int getMaxConnections();
  
  /**
   * Returns the number of overflow connections.
   */
  @Description("The configured maximum number of overflow connections")
  public int getMaxOverflowConnections();
  
  /**
   * Returns the max number of connections trying to connect.
   */
  @Description("The configured maximum number of simultaneous connection creation")
  public int getMaxCreateConnections();
  
  /**
   * Returns the pool idle time in milliseconds.
   */
  @Units("milliseconds")
  @Description("The configured maximum time in milliseconds that a connection remains in the idle pool before it is closed")
  public long getMaxIdleTime();
  
  /**
   * Returns the maximum number of idle connections
   */
  @Description("The configured maximum number of idle connections")
  public int getMaxIdleCount();
  
  /**
   * Returns the minimum number of idle connections
   */
  @Description("The configured minimum number of idle connections")
  public int getMinIdleCount();
  
  /**
   * Returns the pool active time in milliseconds.
   */
  @Description("The configured maximum time in milliseconds that a connection is allowed to be active")
  @Units("milliseconds")
  public long getMaxActiveTime();
  
  /**
   * Returns the pool time in milliseconds.
   */
  @Description("The configured maximum age in milliseconds of a connection before it is closed regardless of it's usage pattern")
  @Units("milliseconds")
  public long getMaxPoolTime();
  
  /**
   * How long to wait for connections when timed out.
   */
  @Units("milliseconds")
  @Description("The configured maximum time in milliseconds to wait for a connection before a failure is returned to the client")
  public long getConnectionWaitTime();
  
  /**
   * Returns true for the JCA shared attribute.
   */
  public boolean isShareable();
  
  /**
   * Returns true if the local-transaction-optimization is allowed
   */
  public boolean isLocalTransactionOptimization();
  
  //
  // Statistics
  //
  
  /**
   * Returns the total number of connections.
   */
  @Description("The current number of idle and active connections")
  @Gauge
  public int getConnectionCount();

  /**
   * Returns the number of active connections.
   */
  @Description("The current number of active connections")
  @Gauge
  public int getConnectionActiveCount();

  /**
   * Returns the number of idle connections.
   */
  @Description("The current number of idle connections")
  @Gauge
  public int getConnectionIdleCount();
  
  /**
   * Returns the number of connections being created.
   */
  @Description("The number of creatingconnections")
  @Gauge
  public int getConnectionCreateCount();

  /**
   * Returns the total number of connections.
   */
  @Description("The total number of connections")
  @Counter
  public long getConnectionCountTotal();

  /**
   * Returns the total number of created connections.
   */
  @Description("The current number of created connections")
  @Counter
  public long getConnectionCreateCountTotal();

  /**
   * Returns the total number of failed connections.
   */
  @Description("The current number of failed connections")
  @Counter
  public long getConnectionFailCountTotal();

  /**
   * Returns the last failed connection time.
   */
  @Description("The last time of connection failure")
  public Date getLastFailTime();

  //
  // Operations
  //

  /**
   * Clears all idle connections in the pool.
   */
  @Description("Clear idle connections in the pool")
  public void clear();
}
