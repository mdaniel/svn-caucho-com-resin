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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;

/**
 * Configuration for a server endpoint that can receive new connections.
 */
public interface ServerEndpointConfig extends EndpointConfig 
{
  Configurator getConfigurator();
  
  Class<?> getEndpointClass();
  
  List<Extension> getExtensions();
  
  String getPath();
  
  List<String> getSubprotocols();
  
  public static final class Builder
  {
    private Class<?> _endpointClass;
    private String _path;
    private Configurator  _configurator;
    private List<Extension> _extensions;
    private List<String> _subprotocols;
    private List<Class<? extends Encoder>> _encoders;
    private List<Class<? extends Decoder>> _decoders;

    public Builder()
    {
    }
    
    private Builder(Class<?> endpointClass, String path)
    {
      _endpointClass = endpointClass;
      _path = path;
    }
    
    public ServerEndpointConfig build()
    {
      Configurator configurator = _configurator;
      
      if (configurator == null) {
        configurator = new Configurator();
      }

      return new ServerEndpointConfigImpl(_path,
                                          _endpointClass,
                                          configurator,
                                          _extensions,
                                          _subprotocols,
                                          _decoders,
                                          _encoders);
    }
    
    public Builder configurator(Configurator configurator)
    {
      _configurator = configurator;
      
      return this;
    }
    
    public static Builder create(Class<?> endpointClass, String path)
    {
      return new Builder(endpointClass, path);
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
    
    public Builder subprotocols(List<String> subprotocols)
    {
      _subprotocols = subprotocols;
      
      return this;
    }
  }

  public static class Configurator
  {
    public boolean checkOrigin(String origin)
    {
      return true;
    }
    
    public <T> T getEndpointInstance(Class<T> endpointClass)
    {
      try {
        return (T) endpointClass.newInstance();
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    
    public List<Extension> getNegotiatedExtensions(List<Extension> installed,
                                                   List<Extension> requested)
    {
      ArrayList<Extension> ext = new ArrayList<>();
      
      for (Extension req : requested) {
        if (findExtension(req, installed) != null) {
          ext.add(req);
        }
      }
      
      return ext;
    }
    
    private Extension findExtension(Extension req, List<Extension> installed)
    {
      for (Extension ext : installed) {
        if (req.getName().equals(ext.getName())) {
          return ext;
        }
      }
      
      return null;
    }

    public boolean matchesURI(String path,
                              URI requestUri,
                              Map<String,String> templateExpansion)
    {
      return path.equals(requestUri.getPath());
    }

    public String getNegotiatedSubprotocol(List<String> supported,
                                           List<String> requested)
    {
      if (requested == null || supported == null) {
        return null;
      }
      
      for (String req : requested) {
        if (supported.contains(req)) {
          return req;
        }
      }
      
      return null;
    }
    
    public void modifyHandshake(ServerEndpointConfig sec,
                                HandshakeRequest request,
                                HandshakeResponse response)
    {
    }
  }
}
