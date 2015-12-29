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

import java.util.List;
import java.util.Map;

/**
 * Configuration for a client endpoint that can send new connections.
 */
public interface ClientEndpointConfig extends EndpointConfig
{
  Configurator getConfigurator();
  
  List<Extension> getExtensions();
  
  List<String> getPreferredSubprotocols();
  
  static class Builder
  {
    private Configurator _configurator;
    private List<Class<? extends Decoder>> _decoders;
    private List<Class<? extends Encoder>> _encoders;
    private List<Extension> _extensions;
    private List<String> _subprotocols;
    
    public static Builder create()
    {
      return new Builder();
    }
    
    public Builder configurator(Configurator configurator)
    {
      _configurator = configurator;
      
      return this;
    }
    
    public Builder decoders(List<Class<? extends Decoder>> decoders)
    {
      _decoders = decoders;
      
      return this;
    }
    
    public Builder encoders(List<Class<? extends Encoder>> encoders)
    {
      _encoders = encoders;
      
      return this;
    }
    
    public Builder extensions(List<Extension> extensions)
    {
      _extensions = extensions;
      
      return this;
    }
    
    public Builder preferredSubprotocols(List<String> subprotocols)
    {
      _subprotocols = subprotocols;
      
      return this;
    }
    
    public ClientEndpointConfig build()
    {
      Configurator configurator = _configurator;
      
      if (configurator == null) {
        configurator = new Configurator();
      }
      
      return new ClientEndpointConfigImpl(configurator,
                                          _decoders, 
                                          _encoders, 
                                          _subprotocols, 
                                          _extensions);
    }
  }
  
  static class Configurator
  {
    public void beforeRequest(Map<String,List<String>> headers)
    {
    }
    
    public void afterResponse(HandshakeResponse hr)
    {
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[]";
    }
  }
}
