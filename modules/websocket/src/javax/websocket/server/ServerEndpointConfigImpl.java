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

package javax.websocket.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;

/**
 * Configuration for a server endpoint that can receive new connections.
 */
class ServerEndpointConfigImpl implements ServerEndpointConfig
{
  private final Configurator _configurator;
  private final Class<?> _endpointClass;
  private final List<Extension> _extensions;
  private final String _path;
  private final List<String> _subprotocols;
  private final List<Class<? extends Decoder>> _decoders;
  private final List<Class<? extends Encoder>> _encoders;
  
  ServerEndpointConfigImpl(String path,
                           Class<?> endpointClass,
                           Configurator configurator,
                           List<Extension> extensions,
                           List<String> subprotocols,
                           List<Class<? extends Decoder>> decoders,
                           List<Class<? extends Encoder>> encoders)
  {
    _path = path;
    _endpointClass = endpointClass;
    _configurator = configurator;
    
    _extensions = new ArrayList<>();
    
    if (extensions != null) {
      _extensions.addAll(extensions);
    }
    
    _subprotocols = new ArrayList<>();
    
    if (subprotocols != null) {
      _subprotocols.addAll(subprotocols);
    }

    _decoders = new ArrayList<>();
    
    if (decoders != null) {
      _decoders.addAll(decoders);
    }
    
    _encoders = new ArrayList<>();
    
    if (encoders != null) {
      _encoders.addAll(encoders);
    }
  }

  @Override
  public Configurator getConfigurator()
  {
    return _configurator;
  }
  
  @Override
  public Class<?> getEndpointClass()
  {
    return _endpointClass;
  }
  
  @Override
  public List<Extension> getExtensions()
  {
    return _extensions;
  }
  
  @Override
  public String getPath()
  {
    return _path;
  }
  
  @Override
  public List<String> getSubprotocols()
  {
    return _subprotocols;
  }

  /* (non-Javadoc)
   * @see javax.websocket.EndpointConfig#getUserProperties()
   */
  @Override
  public Map<String, Object> getUserProperties()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Class<? extends Decoder>> getDecoders()
  {
    return _decoders;
  }

  @Override
  public List<Class<? extends Encoder>> getEncoders()
  {
    return _encoders;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getPath() + "]"; 
  }
}
