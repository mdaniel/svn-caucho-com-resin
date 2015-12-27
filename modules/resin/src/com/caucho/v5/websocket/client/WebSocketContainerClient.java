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

package com.caucho.v5.websocket.client;

import java.io.Closeable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.Environment;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.websocket.common.ContainerWebSocketBase;
import com.caucho.v5.websocket.server.WebSocketEndpointSkeleton;

/**
 * websocket client container
 */
@Module
public class WebSocketContainerClient extends ContainerWebSocketBase
  implements WebSocketContainer, Closeable
{
  private static final Logger log
    = Logger.getLogger(WebSocketContainerClient.class.getName());
  
  private final AtomicLong _connId = new AtomicLong();
  
  private ClassLoader _loader;
    
  private HashSet<Session> _sessions
    = new HashSet<>();
  
  public WebSocketContainerClient()
  {
    Path resinHome = CauchoUtil.getHomeDir();
    boolean is64Bit = CauchoUtil.is64Bit();
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    _loader = oldLoader;
    
    /*
    if (oldLoader instanceof DynamicClassLoader) {
      _loader = oldLoader;
    }
    else {
      _loader = ProLoader.create(resinHome, is64Bit);
    }
    */

    try {
      thread.setContextClassLoader(_loader);

      // Environment.init();

      Vfs.initJNI();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
    
    Environment.addCloseListener(this);
  }
  
  @Override
  public Session connectToServer(Endpoint endpoint,
                                 ClientEndpointConfig cec,
                                 URI uri)
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_loader);
      
      WebSocketClientServlet client = null;
      
      if (true) { throw new UnsupportedOperationException(); }
      /*
      client = new WebSocketClient(uri.toString(), 
                                   endpoint, 
                                   this, 
                                   cec);
                                   */
      
      _sessions.add(client.getSession());
      
      Session session = client.connect();
      
      if (session != null) {
        synchronized (_sessions) {
          _sessions.add(session);
        }
      }
      
      return session;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  
  @Override
  public Session connectToServer(Object bean, URI uri)
  {
    return connectToServer(bean, null, uri);
  }
  
  @Override
  public Session connectToServer(Class<? extends Endpoint> cl,
                                 ClientEndpointConfig cfg, 
                                 URI uri)
  {
    Object bean;
    
    try {
      bean = cl.newInstance();
    } catch (Exception e) {
      throw new DeploymentException(e);
    }
    
    return connectToServer(bean, null, cfg, uri);
  }
  
  public Session connectToServer(Object bean, 
                                String host,
                                URI uri)
  {
    ClientEndpointConfig cfg = createConfig(bean.getClass());

    return connectToServer(bean, host, cfg, uri);
  }
  
  public Session connectToServer(Object bean,
                                 String host,
                                 URI uri,
                                 String uid,
                                 String password)
    throws DeploymentException
  {
    ClientEndpointConfig cfg = createConfig(bean.getClass());

    return connectToServer(bean, host, cfg, uri, uid, password);
  }
  
  @Override
  public boolean isMasked()
  {
    return true;
  }
  
  private ClientEndpointConfig createConfig(Class<?> api)
  {
    ClientEndpointConfig.Builder builder
      = ClientEndpointConfig.Builder.create();
    
    ClientEndpoint clientEndpoint = api.getAnnotation(ClientEndpoint.class);
    
    if (clientEndpoint == null) {
      return builder.build();
    }
    
    Class<?> configCl = clientEndpoint.configurator();
    
    try {
      builder.configurator((Configurator) configCl.newInstance());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    ArrayList<String> subprotocols = new ArrayList<>();
    
    for (String subprotocol : clientEndpoint.subprotocols()) {
      subprotocols.add(subprotocol);
    }
    
    builder.preferredSubprotocols(subprotocols);
    
    ArrayList<Class<? extends Decoder>> decoders = new ArrayList<>();
    
    for (Class<? extends Decoder> cl : clientEndpoint.decoders()) {
      decoders.add(cl);
    }
    
    builder.decoders(decoders);
    
    ArrayList<Class<? extends Encoder>> encoders = new ArrayList<>();
    
    for (Class<? extends Encoder> cl : clientEndpoint.encoders()) {
      encoders.add(cl);
    }
    
    builder.encoders(encoders);

    return builder.build();
  }
  
  public Session connectToServer(Object bean, String host,
                                 ClientEndpointConfig cfg,
                                 URI uri)
                                  throws DeploymentException
  {
    String uid = null;
    String password = null;
    
    return connectToServer(bean, host, cfg, uri, uid, password);
  }
  
  public Session connectToServer(Object bean, 
                                 String host,
                                 ClientEndpointConfig cfg,
                                 URI uri, 
                                 String uid,
                                 String password)
    throws DeploymentException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_loader);
      
      if (bean == null) {
        throw new NullPointerException("endpoint");
      }
    
      WebSocketEndpointSkeleton skel = new WebSocketEndpointSkeleton(bean.getClass());
    
      String []names = new String[0];
      Endpoint endpoint = skel.wrap(bean, cfg, names);
      
      WebSocketClientServlet client = null;
      
      client = new WebSocketClientServlet(uri.toString(), endpoint, this, cfg);
    
      if (host != null) {
        client.setVirtualHost(host);
      }

      // client.connect();
      Session session = client.connect(uid, password);
      
      if (session != null) {
        synchronized (_sessions) {
          _sessions.add(session);
        }
      }
      
      return session;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DeploymentException(e);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  public long nextSessionId()
  {
    return _connId.incrementAndGet();
  }
  
  @Override
  public Set<Session> getOpenSessions()
  {
    return new HashSet<>(_sessions);
  }
  
  @Override
  public void closeSession(Session session)
  {
    if (session == null) {
      return;
    }
    
    synchronized (_sessions) {
      _sessions.remove(session);
    }
  }
  
  @Override
  public void close()
  {
    HashSet<Session> sessions = new HashSet<>();
    
    synchronized (_sessions) {
      sessions.addAll(_sessions);
      sessions.clear();
    }
  
    for (Session session : sessions) {
      try {
        session.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
