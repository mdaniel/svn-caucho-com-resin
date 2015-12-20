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

import java.util.Objects;

import javax.websocket.EndpointConfig;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.remote.ChannelAmp;
import com.caucho.v5.amp.remote.OutAmp;
import com.caucho.v5.json.ser.JsonSerializerFactory;
import com.caucho.v5.websocket.common.EndpointConfigWebSocketBase;

/**
 * JampWebSocketEndpoint responds to JAMP websocket messages.
 */
public class EndpointJampConfig extends EndpointConfigWebSocketBase
{
  private EndpointConfig _delegate;
  private ServiceManagerAmp _rampManager;
  private SessionContextJamp _channelContext;
  private JsonSerializerFactory _jsonFactory;
    
  EndpointJampConfig(EndpointConfig delegate,
                     ServiceManagerAmp rampManager,
                     SessionContextJamp channelContext,
                     JsonSerializerFactory jsonFactory)
  {
    Objects.requireNonNull(delegate);
    Objects.requireNonNull(rampManager);
    Objects.requireNonNull(channelContext);
      
    _delegate = delegate;
    _rampManager = rampManager;
    _channelContext = channelContext;
    _jsonFactory = jsonFactory;
  }

  public ChannelAmp createRegistry(OutAmp conn)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public ServiceManagerAmp getRampManager()
  {
    return _rampManager;
  }

  public SessionContextJamp getChannelContext()
  {
    return _channelContext;
  }

  public JsonSerializerFactory getJsonFactory()
  {
    return _jsonFactory;
  }
  
  @Override
  protected EndpointConfig getDelegate()
  {
    return _delegate;
  }
}
