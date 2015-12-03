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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.management.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.Units;

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
   * Returns the ip address or used to bind the port.
   */
  @Description("The configured ip address or host name used to bind the port")
  public String getAddress();
  
  /**
   * Returns the port's protocol name.
   */
  @Description("The configured protocol for the port")
  public String getProtocolName();

  /**
   * Returns the port number used to bind the port.
   */
  @Description("The configured port number used to bind the port")
  public int getPort();

  @Description("True if the port is using SSL encryption")
  public boolean isSSL();

  //
  // Tuning/Configuration
  //

  /**
   * Returns the minimum number of accept threads
   */
  @Description("The minimum number of accept threads")
  public int getAcceptThreadMin();

  /**
   * Returns the maximum number of accept threads
   */
  @Description("The maximum number of accept threads")
  public int getAcceptThreadMax();

  /**
   * Returns the active threads for the port.
   */
  @Description("The maximum number of active threads for hte port")
  public int getPortThreadMax();

  /**
   * Returns the operating system listen backlog
   */
  @Description("The operating system listen backlog")
  public int getAcceptListenBacklog();

  /**
   * Returns the maximum number of active connections allowed for the port.
   */
  @Description("The configured maximum number of current connections")
  public int getConnectionMax();
  
  /**
   * Returns true if JNI is enabled
   */
  @Description("True if JNI is enabled for this port")
  public boolean isJniEnabled();

  /**
   * Returns the maximum number keepalive connections allowed for the port.
   */
  @Description("The configured maximum number of keepalive connections")
  public int getKeepaliveMax();

  /**
   * Returns the maximum number of select keepalive connections allowed for the port.
   */
  @Description("The configured maximum number of select keepalive connections")
  public int getKeepaliveSelectMax();

  /**
   * Returns the timeout for a keepalive using its own thread before 
   * going to the select.
   */
  @Description("The timeout for a keepalive to use its own thread")
  public long getKeepaliveThreadTimeout();

  /**
   * Returns the maximum total time for keepalive connections
   */
  @Description("The maximum total time for keepalive connections")
  public long getKeepaliveConnectionTimeMax();

  /**
   * Returns the timeout for a keepalive connection
   */
  @Description("The configured timeout for keepalive connections")
  public long getKeepaliveTimeout();

  /**
   * Returns the timeout for socket reads when waiting for data from a client.
   *
   * Corresponds to the functionality described in
   * {@link java.net.Socket#setSoTimeout(int)}, although the actual
   * socket connection may be handled in different ways.
   */
  @Description("The configured timeout for socket reads when waiting for data from a client")
  @Units("milliseconds")    
  public long getSocketTimeout();

  /**
   * Returns the suspend/comet time max
   */
  @Description("The maximum suspend/comet time")
  @Units("milliseconds")    
  public long getSuspendTimeMax();
  
  /**
   * Returns true if tcp-no-delay is enabled.
   */
  @Description("The TCP no-delay (Nagle) socket option")
  public boolean isTcpNoDelay();
  
  /**
   * Returns true if tcp-keepalive is enabled.
   */
  @Description("The TCP keepalive socket option")
  public boolean isTcpKeepalive();

  //
  // State attributes
  //

  /*
   * Returns the lifecycle state.
   */
  @Description("The current lifecycle state")
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
   * Returns the current number of threads that are starting and
   * waiting to service requests.
   */
  @Description("The current number of threads that are"
               + " starting and waiting to service requests")
  public int getThreadStartCount();

  /**
   * Returns the current number of connections that are in the keepalive
   * state
   */
  @Description("The current number of connections that are" +
               " in the keepalive state")
  public int getKeepaliveCount();

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
   * Returns the current number of comet-socket idle and
   * waiting to service requests.
   */
  @Description("The current number of comet sockets that are"
               + " idle and waiting to service requests")
  public int getCometIdleCount();

  /**
   * Returns the total number of requests serviced by the server
   * since it started.
   */
  @Description("The total number of requests serviced by the"
               + " server since it started")
  public long getRequestCountTotal();

  /**
   * Returns the number of requests that have ended up in the keepalive state
   * for this server in its lifetime.
   */
  @Description("The total number of requests that have ended"
               + " up in the keepalive state")
  public long getKeepaliveCountTotal();

  /**
   * Returns the number of requests that have ended up in the 
   * keepalive select state for this server in its lifetime.
   */
  @Description("The total number of requests that have ended"
               + " up in the keepalive select state")
  public long getKeepaliveSelectCountTotal();

  /**
   * The total number of connections that have terminated with
   * {@link com.caucho.vfs.ClientDisconnectException}.
   */
  @Description("The total number of connections that have"
               + " terminated with a client disconnect")
  public long getClientDisconnectCountTotal();

  /**
   * The total number of connections that have been disconnected
   * by throttling.
   */
  @Description("The total number of connections that have"
               + " been throttled by disconnectin")
  public long getThrottleDisconnectCountTotal();

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
  @Units("bytes")
  public long getReadBytesTotal();

  /**
   * Returns the total number of bytes that requests serviced by this
   * port have written.
   */
  @Description("The total number of bytes that requests"
               + " serviced by this port have written")
  @Units("bytes")
  public long getWriteBytesTotal();

  //
  // Operations
  //

  /**
   * Enables the port, letting it listen to new connections.
   */
  public void start();

  /**
   * Disables the port, stopping it from listening to connections.
   */
  public void stop();
  
  /**
   * Returns the connection info
   */
  public TcpConnectionInfo []connectionInfo();
}
