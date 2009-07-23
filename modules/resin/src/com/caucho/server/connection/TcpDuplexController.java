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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.server.port.TcpConnection;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Public API to control a http upgrade connection.
 */
public class TcpDuplexController extends ConnectionController {
  private static final L10N L = new L10N(TcpDuplexController.class);
  private static final Logger log = Logger.getLogger(TcpDuplexController.class
      .getName());

  private ClassLoader _loader;

  private TcpConnection _conn;

  private ReadStream _is;
  private WriteStream _os;

  private TcpDuplexHandler _handler;
  private String _readThreadName;

  public TcpDuplexController(TcpConnection conn, TcpDuplexHandler handler)
  {
    if (handler == null)
      throw new NullPointerException(L.l("handler is a required argument"));

    _conn = conn;
    _handler = handler;

    _loader = Thread.currentThread().getContextClassLoader();

    _is = _conn.getReadStream();
    _os = _conn.getWriteStream();

    _readThreadName = ("resin-" + _handler.getClass().getSimpleName()
        + "-read-" + conn.getId());
  }

  /**
   * Returns true for a duplex controller
   */
  public boolean isDuplex()
  {
    return true;
  }

  /**
   * Sets the max idle time.
   */
  public void setIdleTimeMax(long idleTime)
  {
    if (idleTime < 0 || Long.MAX_VALUE / 2 < idleTime)
      idleTime = Long.MAX_VALUE / 2;

    TcpConnection conn = _conn;
    if (conn != null)
      conn.setIdleTimeMax(idleTime);
  }

  /**
   * Gets the max idle time.
   */
  public long getIdleTimeMax()
  {
    TcpConnection conn = _conn;

    if (conn != null)
      return conn.getIdleTimeMax();
    else
      return -1;
  }

  /**
   * Returns the read stream. The read stream should only be used by the read
   * handler.
   */
  public ReadStream getReadStream()
  {
    return _is;
  }

  /**
   * Returns the write stream. The write stream must be synchronized if multiple
   * threads can write to it.
   */
  public WriteStream getWriteStream()
  {
    return _os;
  }

  /**
   * Returns the handler
   */
  public TcpDuplexHandler getHandler()
  {
    return _handler;
  }

  public void close()
  {
    closeImpl();
  }

  /**
   * Closes the connection.
   */
  @Override
  public void closeImpl()
  {
    _conn = null;
    _is = null;
    _os = null;
    _handler = null;
    _loader = null;

    super.closeImpl();
  }

  public boolean serviceRead()
  {
    Thread thread = Thread.currentThread();

    boolean isValid = false;

    String oldName = thread.getName();

    try {
      thread.setName(_readThreadName);
      thread.setContextClassLoader(_loader);

      TcpConnection conn = _conn;
      ReadStream is = _is;
      TcpDuplexHandler handler = _handler;

      if (conn == null || is == null || handler == null)
        return false;

      isValid = handler.serviceRead(is, this);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      thread.setName(oldName);

      if (!isValid)
        close();
    }

    return isValid;
  }

  @Override
  public String toString()
  {
    TcpConnection conn = _conn;

    if (conn == null)
      return getClass().getSimpleName() + "[closed]";
    else if (Alarm.isTest())
      return getClass().getSimpleName() + "[" + _handler + "]";
    else
      return (getClass().getSimpleName() + "[" + conn.getId() + "," + _handler + "]");
  }
}
