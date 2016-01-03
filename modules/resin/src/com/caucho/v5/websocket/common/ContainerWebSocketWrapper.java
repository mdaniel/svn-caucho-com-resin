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
import java.util.Set;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.caucho.v5.util.ModulePrivate;

/**
 * stub/adapter for WebSocketContainer.
 */
@ModulePrivate
abstract public class ContainerWebSocketWrapper implements WebSocketContainer
{
  abstract protected WebSocketContainer getDelegate();

  @Override
  public Set<Extension> getInstalledExtensions()
  {
    return getDelegate().getInstalledExtensions();
  }

  @Override
  public int getDefaultMaxBinaryMessageBufferSize()
  {
    return getDelegate().getDefaultMaxBinaryMessageBufferSize();
  }

  @Override
  public void setDefaultMaxBinaryMessageBufferSize(int size)
  {
    getDelegate().setDefaultMaxBinaryMessageBufferSize(size);
  }

  @Override
  public long getDefaultMaxSessionIdleTimeout()
  {
    return getDelegate().getDefaultMaxSessionIdleTimeout();
  }

  @Override
  public void setDefaultMaxSessionIdleTimeout(long timeout)
  {
    getDelegate().setDefaultMaxSessionIdleTimeout(timeout);
  }

  @Override
  public int getDefaultMaxTextMessageBufferSize()
  {
    return getDelegate().getDefaultMaxTextMessageBufferSize();
  }

  @Override
  public void setDefaultMaxTextMessageBufferSize(int size)
  {
    getDelegate().setDefaultMaxTextMessageBufferSize(size);
  }

  @Override
  public long getDefaultAsyncSendTimeout()
  {
    return getDelegate().getDefaultAsyncSendTimeout();
  }
  
  @Override
  public void setAsyncSendTimeout(long timeout)
  {
    getDelegate().setAsyncSendTimeout(timeout);
  }

  @Override
  public Session connectToServer(Class<?> annEndpointClass, URI path)
  {
    return getDelegate().connectToServer(annEndpointClass, path);
  }

  @Override
  public Session connectToServer(Class<? extends Endpoint> endpointClass,
                                 ClientEndpointConfig cec, 
                                 URI path)
  {
    return getDelegate().connectToServer(endpointClass, cec, path);
  }

  @Override
  public Session connectToServer(Object endpoint, URI path)
  {
    return getDelegate().connectToServer(endpoint, path);
  }

  @Override
  public Session connectToServer(Endpoint endpoint, 
                                 ClientEndpointConfig cec,
                                 URI path)
  {
    return getDelegate().connectToServer(endpoint, cec, path);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
