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
import java.util.List;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Extension;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.remote.ChannelAmp;
import com.caucho.v5.amp.remote.OutAmp;
import com.caucho.v5.json.ser.JsonSerializerFactory;

/**
 * JampWebSocketEndpoint responds to JAMP websocket messages.
 */
public class EndpointJampConfigClient extends EndpointJampConfig
  implements ClientEndpointConfig
{
  private ArrayList<String> _subprotocols;
  private ChannelAmp _broker;
  
  public EndpointJampConfigClient(ServiceManagerAmp rampManager, 
                                  SessionContextJamp channelContext,
                                  ChannelAmp broker,
                                  JsonSerializerFactory jsonFactory,
                                  String subprotocol)
  {
    super(createClientDelegate(), rampManager, channelContext, jsonFactory);
    
    _subprotocols = new ArrayList<>();
    _subprotocols.add(subprotocol);
    
    _broker = broker;
  }
  
  private static ClientEndpointConfig createClientDelegate()
  {
    ClientEndpointConfig.Builder builder
      = ClientEndpointConfig.Builder.create();
    
    return builder.build();
  }

  @Override
  public ChannelAmp createRegistry(OutAmp conn)
  {
    return _broker;
  }
  
  @Override
  protected ClientEndpointConfig getDelegate()
  {
    return (ClientEndpointConfig) super.getDelegate();
  }
  
  @Override
  public Configurator getConfigurator()
  {
    return getDelegate().getConfigurator();
  }

  @Override
  public List<Extension> getExtensions()
  {
    return getDelegate().getExtensions();
  }

  @Override
  public List<String> getPreferredSubprotocols()
  {
    return _subprotocols;
  }
}
