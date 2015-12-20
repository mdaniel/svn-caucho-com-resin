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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.server.ServerEndpointConfig;

import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.dispatch.ServletDefaultMapper;
import com.caucho.v5.inject.Module;

/**
 * websocket server container
 */
@Module
class WebSocketDefaultMapper implements ServletDefaultMapper {
  private static final Logger log
    = Logger.getLogger(WebSocketDefaultMapper.class.getName());
  
  private ServerEndpointConfig _config;
  private String _servletName;
  
  WebSocketDefaultMapper(ServerEndpointConfig config,
                         String servletName)
  {
    _config = config;
    _servletName = servletName;
  }
  

  @Override
  public String map(InvocationServlet servletInvocation)
  {
    if (! (servletInvocation instanceof InvocationServlet)) {
      return null;
    }
    
    try {
      InvocationServlet invocation = (InvocationServlet) servletInvocation;
      /*
      String url = "ws://" + invocation.getHostName() + invocation.getContextPath()
            + invocation.getContextURI();
      
      if (invocation.getQueryString() != null) {
        url = url + "?" + invocation.getQueryString();
      }
      */
      
      String path = invocation.getContextURI();
    
      /*
      */

      URI uri = new URI(path);

      if (! matchesURI(_config.getPath(), uri, null)) {
        if (invocation.getQueryString() != null) {
          path = path + "?" + invocation.getQueryString();
          uri = new URI(path);
          
          if (! matchesURI(_config.getPath(), uri, null)) {
            return null;
          }
        }
        
        return null;
      }

      return _servletName;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    }
  }

  public boolean matchesURI(String path,
                            URI requestUri,
                            Map<String,String> templateExpansion)
  {
    return path.equals(requestUri.getPath());
  }

  /**
   * /a     \ /a       -> 001
   * /a/b/c \ /a/b/c   -> 111
   * /a/b/c \ /a/{}/c  -> 101
   * /a/b/c \ /{}/{}/c -> 001
   * /a/b/c \ /{}/b/{} -> 010
   * /a/x/y \ /a/b/c   -> 0xFFFFFF
   * @param invocation
   * @return
   */
  @Override
  public int weigh(InvocationServlet invocation)
  {
    String path = invocation.getContextURI();
    String end = _config.getPath();

    String []partsPath = path.split("/");
    String []partsEnd = end.split("/");

    if (partsPath.length != partsEnd.length)
      return -1;

    int weight = 0;

    for (int i = 1; i < partsPath.length; i++) {
      String partPath = partsPath[i];
      String partEnd = partsEnd[i];

      weight = weight << 1;

      if (partPath.equals(partEnd)) {
        weight |= 1;
      }
      else if (partEnd.startsWith("{")) {
      }
      else {
        return -1;
      }
    }

    return weight;
  }

  public String getServletName()
  {
    return _servletName;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _servletName + "]";
  }
}
