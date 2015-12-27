/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.websocket.common;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.caucho.v5.inject.Module;

/**
 * stub/adapter for WebSocketContainer.
 */
@Module
public class ContainerWebSocketBase implements WebSocketContainer
{
  private final ConcurrentSkipListSet<Extension> _extensions
    = new ConcurrentSkipListSet<>();
  
  private int _maxBinaryMessageBufferSize;

  private long _maxSessionIdleTimeout;

  private int _maxTextMessageBufferSize;

  private long _asyncSendTimeout;

  @Override
  public Set<Extension> getInstalledExtensions()
  {
    return Collections.unmodifiableSet(_extensions);
  }

  @Override
  public int getDefaultMaxBinaryMessageBufferSize()
  {
    return _maxBinaryMessageBufferSize;
  }

  @Override
  public void setDefaultMaxBinaryMessageBufferSize(int size)
  {
    _maxBinaryMessageBufferSize = size;
  }

  @Override
  public long getDefaultMaxSessionIdleTimeout()
  {
    return _maxSessionIdleTimeout;
  }

  @Override
  public void setDefaultMaxSessionIdleTimeout(long timeout)
  {
    _maxSessionIdleTimeout = timeout;
  }

  @Override
  public int getDefaultMaxTextMessageBufferSize()
  {
    return _maxTextMessageBufferSize;
  }

  @Override
  public void setDefaultMaxTextMessageBufferSize(int size)
  {
    _maxTextMessageBufferSize = size;
  }

  @Override
  public long getDefaultAsyncSendTimeout()
  {
    return _asyncSendTimeout;
  }

  public boolean isMasked()
  {
    return false;
  }
  
  /*
  @Override
  public void setDefaultAsyncSendTimeout(long timeout)
  {
    _asyncSendTimeout = timeout;
  }
  */
  
  @Override
  public void setAsyncSendTimeout(long timeout)
  {
    _asyncSendTimeout = timeout;
  }

  @Override
  public Session connectToServer(Class<?> annEndpointClass, URI path)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Session connectToServer(Class<? extends Endpoint> endpointClass,
                                 ClientEndpointConfig cec, URI path)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Session connectToServer(Object endpoint, URI path)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Session connectToServer(Endpoint endpoint, ClientEndpointConfig cec,
                              URI path)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void closeSession(Session session)
  {
  }

  public Set<Session> getOpenSessions()
  {
    return new HashSet<>();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
