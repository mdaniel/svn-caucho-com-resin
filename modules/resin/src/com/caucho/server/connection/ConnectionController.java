/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.connection;

import com.caucho.loader.Environment;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.TcpConnectionMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.ThreadPool;
import com.caucho.util.ThreadTask;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls a tcp connection for comet.
 */
public class ConnectionController
{
  private static final Logger log
    = Logger.getLogger(ConnectionController.class.getName());

  private Connection _conn;
  private boolean _isTimeout;

  /**
   * Creates a new TcpConnectionController.
   *
   * @param conn The TCP connection
   */
  protected ConnectionController(Connection conn)
  {
    _conn = conn;

    conn.setController(this);
  }

  public Connection getConnection()
  {
    return _conn;
  }
  
  /**
   * Wakes the connection.
   */
  public final boolean wake()
  {
    Connection conn = _conn;

    if (conn != null)
      return conn.wake();
    else
      return false;
  }

  /**
   * Sets the timeout.
   */
  public final void timeout()
  {
    _isTimeout = true;
  }

  /**
   * Return true if timed out
   */
  public final boolean isTimeout()
  {
    return _isTimeout;
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isActive()
  {
    return _conn != null && ! _isTimeout;
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isClosed()
  {
    return _conn == null;
  }

  /**
   * Closes the connection.
   */
  public void close()
  {
    Connection conn = _conn;
    _conn = null;

    if (conn != null)
      conn.closeController(this);
  }
}
