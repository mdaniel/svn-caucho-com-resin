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

import com.caucho.jmx.MBeanAttribute;
import com.caucho.jmx.MBeanAttributeCategory;
import com.caucho.jmx.MBeanOperation;
import com.caucho.jmx.MBeanParameter;

/**
 * Management interface for the server.
 */
public interface ServerMBean extends DeployControllerMBean {
  /**
   * Returns true if a {@link com.caucho.server.port.AbstractSelectManager} is enabled and active
   */
  @MBeanAttribute(description="A SelectManager handles keepalive without requiring a thread",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public boolean isSelectManager();

  /**
   * Returns the array of ports.
   */
  @MBeanAttribute(description="Ports accept socket connections",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public String []getPortObjectNames();

  /**
   * Returns the array of hosts.
   */
  @MBeanAttribute(description="Hosts are containers that are uniquely identified"
                              + " by the hostname used in making an HTTP request",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public String []getHostObjectNames();

  /**
   * Returns the array of cluster.
   */
  @MBeanAttribute(description="Cluster members are used for load balancing and"
                              + " distributed sessions",
                  category=MBeanAttributeCategory.CONFIGURATION)
  public String []getClusterObjectNames();

  /**
   * Returns the current number of threads that are servicing requests.
   */
  @MBeanAttribute(description="The current number of threads that are"
                              + " servicing requests",
                  category=MBeanAttributeCategory.STATISTIC)
  public int getActiveThreadCount();

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using a thread to maintain the connection.
   */
  @MBeanAttribute(description="The current number of connections that are" +
                              " in the keepalive state and are using" +
                              " a thread to maintain the connection",
                  category=MBeanAttributeCategory.STATISTIC)
  public int getKeepaliveThreadCount();

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using select to maintain the connection.
   */
  @MBeanAttribute(description="The current number of connections that are" +
                              " in the keepalive state and are using" +
                              " select to maintain the connection",
                  category=MBeanAttributeCategory.STATISTIC)
  public int getKeepaliveSelectCount();

  /**
   * Returns the total number of requests serviced by the server
   * since it started.
   */
  @MBeanAttribute(description="The total number of requests serviced by the"
                              + " server since it started",
                  category=MBeanAttributeCategory.STATISTIC)
  public long getLifetimeRequestCount();

  /**
   * Returns the number of requests that have ended up in the keepalive state
   * for this server in it's lifetime.
   */
  @MBeanAttribute(description="The total number of requests that have ended"
                              + " up in the keepalive state",
                  category=MBeanAttributeCategory.STATISTIC)
  public long getLifetimeKeepaliveCount();

  /**
   * The total number of connections that have terminated with
   * {@link com.caucho.vfs.ClientDisconnectException}.
   */
  @MBeanAttribute(description="The total number of connections that have " +
                              " terminated with a client disconnect",
                  category=MBeanAttributeCategory.STATISTIC)
  long getLifetimeClientDisconnectCount();

  /**
   * Returns the total duration in milliseconds that requests serviced by
   * this server have taken.
   */
  @MBeanAttribute(description="The total duration in milliseconds that"
                              + " requests serviced by this service have taken",
                  category=MBeanAttributeCategory.STATISTIC)
  long getLifetimeRequestTime();

  /**
   * Returns the total number of bytes that requests serviced by this
   * server have read.
   */
  @MBeanAttribute(description="The total number of bytes that requests"
                              + " serviced by this server have read",
                  category=MBeanAttributeCategory.STATISTIC)
  long getLifetimeReadBytes();

  /**
   * Returns the total number of bytes that requests serviced by this
   * server have written.
   */
  @MBeanAttribute(description="The total number of bytes that requests"
                              + " serviced by this server have written",
                  category=MBeanAttributeCategory.STATISTIC)
  long getLifetimeWriteBytes();

  /**
   * Returns the invocation cache hit count.
   */
  @MBeanAttribute(description="The invocation cache is an internal cache used"
                              + " by Resin to optimize the handling of urls",
                  category=MBeanAttributeCategory.STATISTIC)
  public long getInvocationCacheHitCount();

  /**
   * Returns the invocation cache miss count.
   */
  @MBeanAttribute(description="The invocation cache is an internal cache used"
                              + " by Resin to optimize the handling of urls",
                  category=MBeanAttributeCategory.STATISTIC)
  public long getInvocationCacheMissCount();

  /**
   * Returns the proxy cache hit count.
   */
  @MBeanAttribute(description="The proxy cache is used to cache responses that"
                              + " set appropriate HTTP headers",
                  category=MBeanAttributeCategory.STATISTIC)
  public long getProxyCacheHitCount();

  /**
   * Returns the proxy cache miss count.
   */
  @MBeanAttribute(description="The proxy cache is used to cache responses that"
                              + " set appropriate HTTP headers",
                  category=MBeanAttributeCategory.STATISTIC)
  public long getProxyCacheMissCount();

  /**
   * Clears the cache.
   */
  @MBeanOperation(description="Clear the cache")
  public void clearCache();

  /**
   * Clears the cache by regexp patterns.
   *
   * @param hostRegexp the regexp to match the host.  Null matches all.
   * @param urlRegexp the regexp to match the url. Null matches all.
   */
  @MBeanOperation(description="Selectively clear the cache using patterns")
  public void clearCacheByPattern(
    @MBeanParameter(name="hostRegexp",
                    description="A regular expression that matches a host name,"
                                + " null to match all host names")
    String hostRegexp,
    @MBeanParameter(name="urlRegexp",
                    description="A regular expression that matches a url,"
                                + " null to match all urls")
    String urlRegexp);
}
