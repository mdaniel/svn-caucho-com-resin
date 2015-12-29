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

package javax.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a client endpoint that can send new connections.
 */
class ClientEndpointConfigImpl implements ClientEndpointConfig
{
  private Configurator _configurator;
  private List<Class<? extends Decoder>> _decoders;
  private List<Class<? extends Encoder>> _encoders;
  private List<Extension> _extensions;
  private List<String> _preferredSubprotocols;
  private HashMap<String,Object> _userProperties = new HashMap<>();
  
  ClientEndpointConfigImpl(Configurator configurator,
                           List<Class<? extends Decoder>> decoders,
                           List<Class<? extends Encoder>> encoders,
                           List<String> preferredSubprotocols,
                           List<Extension> extensions)
  {
    _configurator = configurator;
    _decoders = new ArrayList<>();
    
    if (decoders != null) {
      _decoders.addAll(decoders);
    }
    
    _encoders = new ArrayList<>();
    
    if (encoders != null) {
      _encoders.addAll(encoders);
    }

    _extensions = new ArrayList<>();
    
    if (extensions != null) {
      _extensions.addAll(extensions);
    }
    
    _preferredSubprotocols = new ArrayList<>();
    
    if (preferredSubprotocols != null) {
      _preferredSubprotocols.addAll(preferredSubprotocols);
    }
  }
  
  @Override
  public Map<String, Object> getUserProperties()
  {
    return _userProperties;
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
  public Configurator getConfigurator()
  {
    return _configurator;
  }

  @Override
  public List<Extension> getExtensions()
  {
    return _extensions;
  }

  @Override
  public List<String> getPreferredSubprotocols()
  {
    return _preferredSubprotocols;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
