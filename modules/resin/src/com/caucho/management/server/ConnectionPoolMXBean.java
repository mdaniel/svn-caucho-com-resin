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
 * @author Scott Ferguson
 */

package com.caucho.management.server;

import com.caucho.jmx.*;

/**
 * MBean API for the JCA connection pool.
 *
 * <pre>
 * resin:type=ConnectionPool,name=jdbc/resin,...
 * </pre>
 */
@Description("The state of a JDBC connection pool")
public interface ConnectionPoolMXBean extends ManagedObjectMXBean {
  //
  // Configuration
  //
  
  /**
   * Returns the maximum number of connections.
   */
  @Description("Configured maximum connections")
  public int getMaxConnections();
  
  /**
   * Returns the number of overflow connections.
   */
  @Description("Configured maximum number of overflow connections")
  public int getMaxOverflowConnections();
  
  /**
   * Returns the max number of connections trying to connect.
   */
  public int getMaxCreateConnections();
  
  /**
   * Returns the pool idle time in milliseconds.
   */
  @Units("milliseconds")
  public long getMaxIdleTime();
  
  /**
   * Returns the pool active time in milliseconds.
   */
  @Units("milliseconds")
  public long getMaxActiveTime();
  
  /**
   * Returns the pool time in milliseconds.
   */
  @Units("milliseconds")
  public long getMaxPoolTime();
  
  /**
   * How long to wait for connections when timed out.
   */
  @Units("milliseconds")
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
  @Description("Returns the current number of idle and active connections")
  public int getConnectionCount();

  /**
   * Returns the number of active connections.
   */
  @Description("Returns the current number of active connections")
  public int getConnectionActiveCount();

  /**
   * Returns the number of idle connections.
   */
  @Description("Returns the current number of idle connections")
  public int getConnectionIdleCount();
}
