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

package com.caucho.v5.websocket.server;

import java.net.URI;
import java.util.Set;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;

import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.websocket.common.ContainerServerWebSocketWrapper;

/**
 * websocket server container
 */
@ModulePrivate
public class ServerContainerImpl extends ContainerServerWebSocketWrapper
{
  private static final EnvironmentLocal<ServerContainerDelegate> _currentContainer
    = new EnvironmentLocal<>();
    
  private final ServerContainerDelegate _delegate;
  
  public ServerContainerImpl()
  {
    ServerContainerDelegate delegate = _currentContainer.getLevel();
    
    if (delegate == null) {
      delegate = new ServerContainerDelegate();
      _currentContainer.set(delegate);
      delegate = _currentContainer.get();
    }
    
    _delegate = delegate;
  }
  
  protected ServerContainerDelegate getDelegate()
  {
    return _delegate;
  }

  @Override
  public Session connectToServer(Endpoint endpoint,
                                 ClientEndpointConfig cec, 
                                 URI path)
  {
    return _delegate.connectToServer(endpoint, cec, path);
  }

  @Override
  public Session connectToServer(Object endpointBean, URI uri)
  {
    return _delegate.connectToServer(endpointBean, uri);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _delegate + "]";
  }
}
