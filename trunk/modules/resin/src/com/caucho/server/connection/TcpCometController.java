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

import java.util.HashMap;

import com.caucho.servlet.comet.CometController;
import com.caucho.util.Alarm;

/**
 * Public API to control a comet connection.
 */
public class TcpCometController
  implements CometController
{
  private TcpConnection _conn;
  
  private HashMap<String,Object> _map;
  
  private boolean _isTimeout;

  private boolean _isInitial = true;
  private boolean _isSuspended;
  private boolean _isComplete;
  
  private long _maxIdleTime;

  public TcpCometController(TcpConnection conn)
  {
    _conn = conn;
  }
  
  /**
   * Sets the max idle time.
   */
  public void setMaxIdleTime(long idleTime)
  {
    if (idleTime < 0 || Long.MAX_VALUE / 2 < idleTime)
      _maxIdleTime = Long.MAX_VALUE / 2;
  }
  
  /**
   * Gets the max idle time.
   */
  public long getMaxIdleTime()
  {
    return _maxIdleTime;
  }

  public Connection getConnection()
  {
    return _conn;
  }

  /**
   * Returns true if the connection is the initial request
   */
  public final boolean isInitial()
  {
    return _isInitial;
  }

  /**
   * Returns true if the connection should be suspended
   */
  public final boolean isSuspended()
  {
    return _isSuspended;
  }

  /**
   * Suspend the connection on the next request
   */
  public final void suspend()
  {
    if (! _isComplete)
      _isSuspended = true;
  }

  /**
   * Returns true if the connection is complete.
   */
  public final boolean isComplete()
  {
    return _isComplete;
  }

  /**
   * Complete the connection
   */
  public final void complete()
  {
    close();
    /*
    _isComplete = true;
    _isSuspended = false;
    wake();
    */
  }

  /**
   * Suspend the connection on the next request
   */
  public final void startResume()
  {
    _isSuspended = false;
    _isInitial = false;
  }
  
  /**
   * Wakes the connection.
   */
  public final boolean wake()
  {
    TcpConnection conn = _conn;

    if (conn != null)
      return conn.wake();
    else
      return false;
  }

  /**
   * Returns true for a duplex controller
   */
  public boolean isDuplex()
  {
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
    return _conn != null;
  }

  /**
   * Returns true for an active comet connection.
   */
  public boolean isComet()
  {
    return _conn != null;
  }
  
  /**
   * Gets a request attribute.
   */
  public Object getAttribute(String name)
  {
    if (_map != null) {
      synchronized (_map) {
	return _map.get(name);
      }
    }
    else
      return null;
  }
  
  /**
   * Sets a request attribute.
   */
  public void setAttribute(String name, Object value)
  {
    if (_map != null) {
      synchronized (_map) {
	_map.put(name, value);
      }
    }
  }
  
  /**
   * Remove a request attribute.
   */
  public void removeAttribute(String name)
  {
    if (_map != null) {
      synchronized (_map) {
	_map.remove(name);
      }
    }
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
    // complete();
    
    TcpConnection conn = _conn;
    _conn = null;

    /*
    if (conn != null)
      conn.closeController(this);
    */
  }

  public String toString()
  {
    TcpConnection conn = _conn;

    if (conn == null)
      return getClass().getSimpleName() + "[closed]";
    else if (Alarm.isTest())
      return getClass().getSimpleName() + "[]";
    else
      return getClass().getSimpleName() + "[" + conn.getId() + "]";
  }
}
