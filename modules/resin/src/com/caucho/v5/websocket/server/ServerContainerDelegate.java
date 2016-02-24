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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import com.caucho.v5.http.dispatch.ServletBuilder;
import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.websocket.common.ContainerWebSocketServer;

/**
 * websocket server container
 */
@ModulePrivate
public class ServerContainerDelegate 
  extends ContainerWebSocketServer
  implements ServerContainer
{
  private static final L10N L = new L10N(ServerContainerDelegate.class);
  private static final Logger log = Logger.getLogger(ServerContainerDelegate.class.getName());
  
  private final WebAppResinBase _webApp;
  
  private final WebSocketEndpointManager _endpointManager;
  
  private final AtomicInteger _idGen = new AtomicInteger();
  
  private final HashSet<Session> _sessions = new HashSet<>();
  
  public ServerContainerDelegate()
  {
    _webApp = WebAppResinBase.getCurrent();
    
    if (_webApp == null) {
      throw new IllegalStateException(L.l("{0} must be created in a web-app context.",
                                          ServerContainerDelegate.class.getName()));
    }
    
    _endpointManager = new WebSocketEndpointManager();
  }
  
  public WebSocketEndpointManager getEndpointManager()
  {
    return _endpointManager;
  }

  @Override
  public void addEndpoint(Class<?> endpointClass)
  {
    ServerEndpoint endpoint = endpointClass.getAnnotation(ServerEndpoint.class);
    
    if (endpoint == null) {
      return;
    }
    
    String path = endpoint.value();
    
    ServerEndpointConfig.Builder builder;
    builder = ServerEndpointConfig.Builder.create(endpointClass, path);
    
    Class<? extends Configurator> configCl = endpoint.configurator();
    
    try {
      builder.configurator((Configurator) configCl.newInstance());
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    ArrayList<Class<? extends Decoder>> decoders = new ArrayList<>();
    
    for (Class<? extends Decoder> decoder: endpoint.decoders()) {
      decoders.add(decoder);
    }
    
    builder.decoders(decoders);
    
    ArrayList<Class<? extends Encoder>> encoders = new ArrayList<>();
    
    for (Class<? extends Encoder> encoder: endpoint.encoders()) {
      encoders.add(encoder);
    }
    
    builder.encoders(encoders);
    
    ArrayList<String> subprotocols = new ArrayList<>();
    
    for (String subprotocol : endpoint.subprotocols()) {
      subprotocols.add(subprotocol);
    }
    
    builder.subprotocols(subprotocols);

    addEndpoint(builder.build());
  }

  @Override
  public void addEndpoint(ServerEndpointConfig config)
  {
    try {
      WebSocketEndpointFactory factory
        = WebSocketEndpointFactory.create(config);
      
      _endpointManager.addEndpoint(factory);
      
      if (log.isLoggable(Level.FINE)) {
        log.fine("@WebSocketEndpoint " + config.getPath() + " published " + config);
      }

      WebSocketServletDispatch servlet
        = new WebSocketServletDispatch(this, factory);
      
      String endpointName = config.getEndpointClass().getSimpleName();
      int id = _idGen.incrementAndGet();
    
      String servletName = "caucho-websocket-" + endpointName + "-" + id;
    
      ServletBuilder servletConfig = new ServletBuilder();
      servletConfig.setServlet(new WebSocketServlet(servlet));
      servletConfig.setServletName(servletName);
    
      _webApp.getBuilder().addServlet(servletConfig);

      _webApp.getBuilder().addServletDefaultMapper(new WebSocketDefaultMapper(config, servletName));
    
      // _webApp.addServlet("")
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  void addSession(Session session)
  {
    synchronized (_sessions) {
      _sessions.add(session);
    }
  }
  
  @Override
  public Set<Session> getOpenSessions()
  {
    Set<Session> sessions = new HashSet<>();
    
    synchronized (_sessions) {
      sessions.addAll(_sessions);
    }
    
    return sessions;
  }
  
  @Override
  public void closeSession(Session session)
  {
    synchronized (_sessions) {
      _sessions.remove(session);
    }
    
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _webApp + "]";
  }
}
