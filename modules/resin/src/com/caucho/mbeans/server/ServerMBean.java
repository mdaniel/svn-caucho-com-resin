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

package com.caucho.mbeans.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.Name;

/**
 * Management interface for the server.
 */
public interface ServerMBean extends DeployControllerMBean {
  /**
   * Returns true if a {@link com.caucho.server.port.AbstractSelectManager} is enabled and active
   */
  @Description("A SelectManager handles keepalive without requiring a thread")
  public boolean isSelectManager();

  /**
   * Returns the array of ports.
   */
  @Description("Ports accept socket connections")
  public String []getPortObjectNames();

  /**
   * Returns the array of hosts.
   */
  @Description("Hosts are containers that are uniquely identified"
               + " by the hostname used in making an HTTP request")
  public String []getHostObjectNames();

  /**
   * Returns the array of cluster.
   */
  @Description("Cluster members are used for load balancing and"
               + " distributed sessions")
  public String []getClusterObjectNames();

  /**
   * Returns the current number of threads that are servicing requests.
   */
  @Description("The current number of threads that are servicing requests")
  public int getActiveThreadCount();

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using a thread to maintain the connection.
   */
  @Description("The current number of connections that are" +
               " in the keepalive state and are using" +
               " a thread to maintain the connection")
  public int getKeepaliveThreadCount();

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using select to maintain the connection.
   */
  @Description("The current number of connections that are" +
               " in the keepalive state and are using" +
               " select to maintain the connection")
  public int getKeepaliveSelectCount();

  /**
   * Returns the total number of requests serviced by the server
   * since it started.
   */
  @Description("The total number of requests serviced by the"
               + " server since it started")
  public long getLifetimeRequestCount();

  /**
   * Returns the number of requests that have ended up in the keepalive state
   * for this server in it's lifetime.
   */
  @Description("The total number of requests that have ended"
               + " up in the keepalive state")
  public long getLifetimeKeepaliveCount();

  /**
   * The total number of connections that have terminated with
   * {@link com.caucho.vfs.ClientDisconnectException}.
   */
  @Description("The total number of connections that have " +
               " terminated with a client disconnect")
  long getLifetimeClientDisconnectCount();

  /**
   * Returns the total duration in milliseconds that requests serviced by
   * this server have taken.
   */
  @Description("The total duration in milliseconds that"
               + " requests serviced by this service have taken")
  long getLifetimeRequestTime();

  /**
   * Returns the total number of bytes that requests serviced by this
   * server have read.
   */
  @Description("The total number of bytes that requests"
               + " serviced by this server have read")
  long getLifetimeReadBytes();

  /**
   * Returns the total number of bytes that requests serviced by this
   * server have written.
   */
  @Description("The total number of bytes that requests"
               + " serviced by this server have written")
  long getLifetimeWriteBytes();

  /**
   * Returns the invocation cache hit count.
   */
  @Description("The invocation cache is an internal cache used"
               + " by Resin to optimize the handling of urls")
  public long getInvocationCacheHitCount();

  /**
   * Returns the invocation cache miss count.
   */
  @Description("The invocation cache is an internal cache used"
               + " by Resin to optimize the handling of urls")
  public long getInvocationCacheMissCount();

  /**
   * Returns the proxy cache hit count.
   */
  @Description("The proxy cache is used to cache responses that"
               + " set appropriate HTTP headers")
  public long getProxyCacheHitCount();

  /**
   * Returns the proxy cache miss count.
   */
  @Description("The proxy cache is used to cache responses that"
               + " set appropriate HTTP headers")
  public long getProxyCacheMissCount();

  /**
   * Clears the cache.
   */
  @Description("Clear the cache")
  public void clearCache();

  /**
   * Clears the cache by regexp patterns.
   *
   * @param hostRegexp the regexp to match the host.  Null matches all.
   * @param urlRegexp the regexp to match the url. Null matches all.
   */
  @Description("Selectively clear the cache using patterns")
  public void clearCacheByPattern(
    @Name("hostRegexp")
    @Description("A regular expression that matches a host name, null to match all host names")
    String hostRegexp,

    @Name("urlRegexp")
    @Description("A regular expression that matches a url, null to match all urls")
    String urlRegexp);
}
