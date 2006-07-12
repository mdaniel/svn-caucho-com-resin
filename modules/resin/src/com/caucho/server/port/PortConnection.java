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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.port;

import java.io.*;
import java.net.*;

import java.nio.channels.SelectableChannel;

import com.caucho.vfs.*;
import com.caucho.util.*;

import com.caucho.server.connection.Connection;

/**
 * Represents a protocol-independent connection.  Prococol servers and
 * their associated Requests use Connection to retrieve the read and
 * write streams and to get information about the connection.
 *
 * <p>TcpConnection is the most common implementation.  The test harness
 * provides a string based Connection.
 */
public abstract class PortConnection extends Connection
{
  private static int _connectionCount;

  private Port _port;

  private ServerRequest _request;

  private int _connectionId;  // The connection's id
  private long _accessTime;   // Time the current request started

  /**
   * Creates a new connection
   */
  protected PortConnection()
  {
    _connectionId = _connectionCount++;
  }

  /**
   * Returns the connection id.  Primarily for debugging.
   */
  public int getId()
  {
    return _connectionId;
  }

  /**
   * Returns the port which generated the connection.
   */
  public Port getPort()
  {
    return _port;
  }

  /**
   * Sets the connection's port.
   */
  public void setPort(Port port)
  {
    _port = port;
  }

  /**
   * Returns the request for the connection.
   */
  public final ServerRequest getRequest()
  {
    return _request;
  }

  /**
   * Sets the connection's request.
   */
  public final void setRequest(ServerRequest request)
  {
    _request = request;
  }

  /**
   * Returns true if secure (ssl)
   */
  public boolean isSecure()
  {
    return false;
  }
  /**
   * Returns the static virtual host
   */
  public String getVirtualHost()
  {
    return null;
  }
  
  /**
   * Returns the local address of the connection
   */
  public abstract InetAddress getLocalAddress();

  /**
   * Returns the local port of the connection
   */
  public abstract int getLocalPort();

  /**
   * Returns the remote address of the connection
   */
  public abstract InetAddress getRemoteAddress();

  /**
   * Returns the remove port of the connection
   */
  public abstract int getRemotePort();

  /**
   * Sets the time of the request start.  ServerRequests can use
   * setAccessTime() to put off connection reaping.  HttpRequest calls
   * setAccessTime() at the beginning of each request.
   *
   * @param now the current time in milliseconds as by Alarm.getCurrentTime().
   */
  public void setAccessTime(long now)
  {
    _accessTime = now;
  }

  /**
   * Returns the time the last Request began in milliseconds.
   */
  public long getAccessTime()
  {
    return _accessTime;
  }

  /**
   * Returns the selectable channel.
   */
  public SelectableChannel getSelectableChannel()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Closes the connection()
   */
  public void close()
  {
  }
}
