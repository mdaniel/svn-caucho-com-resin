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

import java.util.*;
import javax.servlet.*;

import com.caucho.servlet.comet.CometController;
import com.caucho.servlet.comet.CometCloseListener;

import com.caucho.server.port.*;
import com.caucho.server.connection.*;
import com.caucho.util.*;

/**
 * Public API to control a comet connection.
 */
public class HttpConnectionController extends ConnectionController
  implements CometController
{
  private AbstractHttpRequest _request;
  private HashMap<String,Object> _map = new HashMap<String,Object>(8);

  private ArrayList<CometCloseListener> _closeListeners;

  private long _maxIdleTime;

  public HttpConnectionController(ServletRequest request)
  {
    this(getAbstractHttpRequest(request));
  }

  public HttpConnectionController(AbstractHttpRequest request)
  {
    super(request.getConnection());

    _request = request;
  }

  private static AbstractHttpRequest
    getAbstractHttpRequest(ServletRequest request)
  {
    return (AbstractHttpRequest) request;
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

  public void addCloseListener(CometCloseListener listener)
  {
    if (_closeListeners == null)
      _closeListeners = new ArrayList<CometCloseListener>();

    _closeListeners.add(listener);
  }

  /**
   * Closes the connection.
   */
  @Override
  public void close()
  {
    ArrayList<CometCloseListener> listeners = _closeListeners;
    _closeListeners = null;

    if (listeners != null) {
      for (int i = 0; i < listeners.size(); i++) {
        CometCloseListener listener = listeners.get(i);

        listener.onClose(this);
      }
    }

    _request = null;

    super.close();

  }

  public String toString()
  {
    AbstractHttpRequest request = _request;

    if (request == null || request.getConnection() == null)
      return "HttpConnectionController[closed]";
    else if (Alarm.isTest())
      return "HttpConnectionController[]";
    else
      return "HttpConectionController[" + request.getConnection().getId() + "]";
  }
}
