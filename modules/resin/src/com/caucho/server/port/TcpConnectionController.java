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

package com.caucho.server.port;

import com.caucho.loader.Environment;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.TcpConnectionMXBean;
import com.caucho.server.connection.BroadcastTask;
import com.caucho.server.connection.Connection;
import com.caucho.util.Alarm;
import com.caucho.util.ThreadPool;
import com.caucho.util.ThreadTask;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls a tcp connection for comet.
 */
public class TcpConnectionController
{
  private static final Logger log
    = Logger.getLogger(TcpConnectionController.class.getName());

  private Connection _conn;

  /**
   * Creates a new TcpConnectionController.
   *
   * @param conn The TCP connection
   */
  protected TcpConnectionController(Connection conn)
  {
    _conn = conn;
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isActive()
  {
    return _conn != null;
  }

  /**
   * Closes the connection.
   */
  public void close()
  {
    Connection conn = _conn;
    _conn = null;
  }
}
