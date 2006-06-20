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

/**
 * Represents a protocol connection.
 * A typical ObjectName for a ClusterMBean is
 * <tt>resin:type=Port,Server=default,port=80</tt>.
 */
public interface PortMBean {
  /**
   * Returns the {@link javax.management.ObjectName} of the mbean.
   */
  @Description("The JMX ObjectName for the MBean")
  public String getObjectName();

  /**
   * Returns the port's protocol name.
   */
  @Description("The protocol for the port")
  public String getProtocolName();

  /**
   * Returns the ip address or used to bind the port.
   */
  @Description("The ip address or host name used to bind the port")
  public String getHost();

  /**
   * Returns the port number used to bind the port.
   */
  @Description("The port number used to bind the port")
  public int getPort();

  /**
   * Returns the maximum number of active connections allowed for the port.
   */
  @Description("The maximum number of current connections")
  public int getConnectionMax();

  /**
   * Returns the maximum number of keepalive connections allowed for the port.
   */
  @Description("The maximum number of keepalive connections")
  public int getKeepaliveMax();

  @Description("True if the port is using SSL encryption")
  public boolean isSSL();

  /**
   * Returns the timeout for socket reads when waiting for data from a client.
   *
   * Corresponds to the functionality described in
   * {@link java.net.Socket#setSoTimeout(int)}, although the actual
   * socket connection may be handled in different ways.
   */
  @Description("Timeout for socket reads when waiting for data from a client")
  public long getReadTimeout();

  /**
   * Returns the timeout for socket writes when writing data to a client.
   */
  @Description("Timeout for socket writes when sending data to a client")
  public long getWriteTimeout();

  /*
   * Returns the lifecycle state.
   */
  @Description("The lifecycle state")
  public String getState();

  /**
   * Returns the current number of threads that are servicing requests.
   */
  @Description("The current number of threads that are servicing requests")
  public int getActiveThreadCount();

  /**
   * Returns the current number of threads that are idle and
   * waiting to service requests.
   */
  @Description("The current number of threads that are"
               + " idle and waiting to service requests")
  public int getIdleThreadCount();


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
  public long getLifetimeClientDisconnectCount();

  /**
   * Returns the total duration in milliseconds that requests serviced by
   * this port have taken.
   */
  @Description("The total duration in milliseconds that"
               + " requests serviced by this service have taken")
  public long getLifetimeRequestTime();

  /**
   * Returns the total number of bytes that requests serviced by this
   * port have read.
   */
  @Description("The total number of bytes that requests"
               + " serviced by this port have read")
  public long getLifetimeReadBytes();

  /**
   * Returns the total number of bytes that requests serviced by this
   * port have written.
   */
  @Description("The total number of bytes that requests"
               + " serviced by this port have written")
  public long getLifetimeWriteBytes();

}
