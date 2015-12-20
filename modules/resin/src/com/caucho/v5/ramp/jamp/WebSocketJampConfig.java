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

package com.caucho.v5.ramp.jamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

import com.caucho.v5.amp.remote.ChannelServerFactory;

/**
 * JampWebSocketEndpoint responds to JAMP websocket messages.
 */
public class WebSocketJampConfig implements ServerEndpointConfig
{
  private ArrayList<String> _subprotocols;
  private ChannelServerFactory _registryFactory;
  private ServerEndpointConfig _delegate;
  private HashMap<String,Object> _properties = new HashMap<>();
  
  public WebSocketJampConfig(String path, 
                             ServletContext ctx)
  {
    _delegate = createServerDelegate(path);
    
    _subprotocols = new ArrayList<>();
    _subprotocols.add("jamp");
    _subprotocols.add("hamp");
    
    JampWebSocketDispatch wsDispatch = JampWebSocketDispatch.createLocal(ctx);
    
    _properties.put(JampWebSocketDispatch.class.getName(), wsDispatch);
    
    // _registryFactory = brokerFactory;
  }
  
  private static ServerEndpointConfig createServerDelegate(String path)
  {
    ServerEndpointConfig.Builder builder
      = ServerEndpointConfig.Builder.create(EndpointJamp.class, path);
    
    return builder.build();
  }
  
  protected ServerEndpointConfig getDelegate()
  {
    return _delegate;
  }

  @Override
  public Configurator getConfigurator()
  {
    return new JampConfigurator();
  }
  
  @Override
  public Class<?> getEndpointClass()
  {
    return getDelegate().getEndpointClass();
  }

  @Override
  public List<Extension> getExtensions()
  {
    return getDelegate().getExtensions();
  }

  @Override
  public String getPath()
  {
    return getDelegate().getPath();
  }

  @Override
  public List<String> getSubprotocols()
  {
    return _subprotocols;
  }

  @Override
  public Map<String, Object> getUserProperties()
  {
    return _properties;
  }

  @Override
  public List<Class<? extends Decoder>> getDecoders()
  {
    return getDelegate().getDecoders();
  }

  @Override
  public List<Class<? extends Encoder>> getEncoders()
  {
    return getDelegate().getEncoders();
  }
  
  private class JampConfigurator extends Configurator {
  }
}
