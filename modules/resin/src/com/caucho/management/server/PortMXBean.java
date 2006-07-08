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

import javax.management.*;

import com.caucho.jmx.*;

/**
 * Represents a protocol connection.
 *
 * A typical ObjectName for a Port is
 *
 * <pre>
 * resin:type=Port,port=80,address=INADDR_ANY
 * </pre>
 */
@Description("The protocol listening to an IP address.")
public interface PortMXBean extends ManagedObjectMXBean {
  /**
   * Returns the port's protocol name.
   */
  @Description("The protocol for the port")
  public String getProtocolName();

  /**
   * Returns the ip address or used to bind the port.
   */
  @Description("The ip address or host name used to bind the port")
  public String getAddress();

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
  @Description("Configured timeout for socket reads when waiting for data from a client")
  @Units("milliseconds")    
  public long getReadTimeout();

  /**
   * Returns the timeout for socket writes when writing data to a client.
   */
  @Description("Configured timeout for socket writes when sending data to a client")
  @Units("milliseconds")    
  public long getWriteTimeout();

  //
  // State attributes
  //

  /*
   * Returns the lifecycle state.
   */
  @Description("The lifecycle state")
  public String getState();

  //
  // Statistics
  //

  /**
   * Returns the current number of threads that are servicing requests.
   */
  @Description("The current number of threads used by the port")
  public int getThreadCount();

  /**
   * Returns the current number of threads that are servicing requests.
   */
  @Description("The current number of threads that are servicing requests")
  public int getThreadActiveCount();

  /**
   * Returns the current number of threads that are idle and
   * waiting to service requests.
   */
  @Description("The current number of threads that are"
               + " idle and waiting to service requests")
  public int getThreadIdleCount();

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using a thread to maintain the connection.
   */
  @Description("The current number of connections that are" +
               " in the keepalive state and are using" +
               " a thread to maintain the connection")
  public int getThreadKeepaliveCount();

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using select to maintain the connection.
   */
  @Description("The current number of connections that are" +
               " in the keepalive state and are using" +
               " select to maintain the connection")
  public int getSelectKeepaliveCount();

  /**
   * Returns the total number of requests serviced by the server
   * since it started.
   */
  @Description("The total number of requests serviced by the"
               + " server since it started")
  public long getRequestCountTotal();

  /**
   * Returns the number of requests that have ended up in the keepalive state
   * for this server in it's lifetime.
   */
  @Description("The total number of requests that have ended"
               + " up in the keepalive state")
  public long getKeepaliveCountTotal();

  /**
   * The total number of connections that have terminated with
   * {@link com.caucho.vfs.ClientDisconnectException}.
   */
  @Description("The total number of connections that have " +
               " terminated with a client disconnect")
  public long getClientDisconnectCountTotal();

  /**
   * Returns the total duration in milliseconds that requests serviced by
   * this port have taken.
   */
  @Description("The total duration in milliseconds that"
               + " requests serviced by this service have taken")
  @Units("milliseconds")
  public long getRequestTimeTotal();

  /**
   * Returns the total number of bytes that requests serviced by this
   * port have read.
   */
  @Description("The total number of bytes that requests"
               + " serviced by this port have read")
  @Units("milliseconds")
  public long getReadBytesTotal();

  /**
   * Returns the total number of bytes that requests serviced by this
   * port have written.
   */
  @Description("The total number of bytes that requests"
               + " serviced by this port have written")
  @Units("milliseconds")
  public long getWriteBytesTotal();

}
