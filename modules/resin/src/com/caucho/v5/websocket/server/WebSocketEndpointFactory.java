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

import java.util.function.Supplier;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

public final class WebSocketEndpointFactory 
  implements Supplier<Endpoint>
{
  private final ServerEndpointConfig _config;
  private final Supplier<Endpoint> _supplier;
  
  private WebSocketEndpointFactory(ServerEndpointConfig config,
                                   Supplier<Endpoint> supplier)
  {
    _config = config;
    _supplier = supplier;
  }
  
  public static WebSocketEndpointFactory create(ServerEndpointConfig config)
  {
    Supplier<Endpoint> supplier = WebSocketEndpointSkeleton.wrap(config);
    
    return new WebSocketEndpointFactory(config, supplier);
  }
  
  public static WebSocketEndpointFactory create(ServerEndpointConfig config,
                                                Class<?> endpointClass,
                                                Supplier<?> beanSupplier)
  {
    Supplier<Endpoint> supplier;
    supplier = WebSocketEndpointSkeleton.wrap(config);

    return new WebSocketEndpointFactory(config, supplier);
  }
  
  @Override
  public Endpoint get()
  {
    return _supplier.get();
  }

  public final ServerEndpointConfig getConfig()
  {
    return _config;
  }
  
  public String toString()
  {
    return getClass().getName() + "[" + getConfig() + "]";
  }
}
