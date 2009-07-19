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

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.*;

import com.caucho.servlet.comet.CometController;

import com.caucho.server.port.*;
import com.caucho.server.connection.*;
import com.caucho.util.*;

/**
 * Public API to control a comet connection.
 */
public class ConnectionCometController extends ConnectionController
  implements CometController
{
  private static final L10N L = new L10N(ConnectionCometController.class);
  private static final Logger log
    = Logger.getLogger(ConnectionCometController.class.getName());

  private Connection _conn;

  private HashMap<String,Object> _map;

  private ServletRequest _request;
  private ServletResponse _response;

  private AsyncListenerNode _listenerNode;

  private boolean _isTimeout;

  private boolean _isTop;
  private boolean _isInitial = true;
  private boolean _isSuspended;
  private boolean _isComplete;

  private String _forwardPath;

  private long _maxIdleTime;

  public ConnectionCometController(Connection conn,
                                   boolean isTop,
                                   ServletRequest request,
                                   ServletResponse response)
  {
    _conn = conn;

    _isTop = isTop;
    _request = request;
    _response = response;
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
  @Override
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
    _isComplete = true;
    _isSuspended = false;

    for (AsyncListenerNode node = _listenerNode;
         node != null;
         node = node.getNext()) {
      try {
        node.onComplete();
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    wake();
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
    Connection conn = _conn;

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
    _isComplete = true;

    for (AsyncListenerNode node = _listenerNode;
         node != null;
         node = node.getNext()) {
      try {
        node.onTimeout();
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    wake();
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
    return _conn != null && ! _isComplete;
  }

  /**
   * Sets the async listener
   */
  public void setAsyncListenerNode(AsyncListenerNode node)
  {
    _listenerNode = node;
  }

  public void addAsyncListener(AsyncListener listener,
                               ServletRequest request,
                               ServletResponse response)
  {
    _listenerNode
      = new AsyncListenerNode(listener, request, response, _listenerNode);
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
    if (_map == null) {
      synchronized (this) {
        if (_map == null)
          _map = new HashMap<String,Object>(8);
      }
    }

    synchronized (_map) {
      _map.put(name, value);
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
    return _conn == null || _isComplete;
  }

  public ServletRequest getRequest()
  {
    return _request;
  }

  public ServletResponse getResponse()
  {
    return _response;
  }

  public boolean hasOriginalRequestAndResponse()
  {
    return true;
  }

  public String getForwardPath()
  {
    return _forwardPath;
  }

  public void dispatch()
  {
    Connection conn = _conn;

    if (conn != null) {
      conn.wake();
    }
  }

  public void dispatch(String path)
  {
    _forwardPath = path;

    dispatch();
  }

  public void dispatch(ServletContext context, String path)
  {
    _forwardPath = path;

    dispatch();
  }

  public void start(Runnable task)
  {
    if (_conn != null)
      ThreadPool.getCurrent().schedule(task);
    else
      throw new IllegalStateException(L.l("AsyncContext.start() is not allowed because the AsyncContext has been completed."));
  }

  /**
   * Closes the connection.
   */
  public void close()
  {
    complete();
  }

  @Override
  public void closeImpl()
  {
    // complete();

    Connection conn = _conn;
    _conn = null;

    _request = null;
    _response = null;

    _isComplete = true;

    if (conn != null)
      conn.closeController(this);
  }

  public String toString()
  {
    Connection conn = _conn;

    if (conn == null)
      return getClass().getSimpleName() + "[closed]";

    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[");

    if (Alarm.isTest())
      sb.append("test");
    else
      sb.append(conn.getId());

    if (_isComplete)
      sb.append(",complete");

    if (_isSuspended)
      sb.append(",suspended");

    if (_conn instanceof TcpConnection
        && ((TcpConnection) _conn).isWake())
      sb.append(",wake");

    sb.append("]");

    return sb.toString();
  }
}
