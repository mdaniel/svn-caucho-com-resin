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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.websocket.server.ServerContainer;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.ramp.embed.EmbedBuilder;
import com.caucho.v5.ramp.jamp.WebSocketJampConfig;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

/**
 * Initializer for Baratine embedded as a war.
 */
public class ServletContainerInitBaratine implements ServletContainerInitializer
{
  private static final Logger log
    = Logger.getLogger(ServletContainerInitBaratine.class.getName());
  
  private static final String BARATINE_INIT = "com.caucho.baratine.init";
  
  private ServiceManagerAmp _manager;
  private EmbedBuilder _builder;

  @Override
  public void onStartup(Set<Class<?>> c, ServletContext ctx)
      throws ServletException
  {
    if (ctx.getAttribute(BARATINE_INIT) != null) {
      return;
    }
    
    ctx.setAttribute(BARATINE_INIT,  true);
    
    ServiceManagerAmp manager = ServiceManagerAmp.current();
    
    if (manager == null) {
      manager = createManager(ctx);
    }
    
    ServerContainer wsContainer
      = (ServerContainer) ctx.getAttribute("javax.websocket.server.ServerContainer");
    
    for (ServletRegistration reg : ctx.getServletRegistrations().values()) {
      if (isJampServlet(reg, JampServletWebApp.class)) {
        if (wsContainer != null) {
          Collection<String> maps = reg.getMappings();
          
          if (maps.size() > 0) {
            String path = maps.iterator().next();
            
            if (path.endsWith("/*")) {
              //ServerEndpointConfig.Builder builder;
              //builder = ServerEndpointConfig.Builder.create(asdf, path);
              
              path = path.substring(0, path.length() - 2);
              //path = "/foo";
              
              WebSocketJampConfig config
                = new WebSocketJampConfig(path, ctx);
              
              wsContainer.addEndpoint(config);
            }
          }
        }
      }
    }
  }
  
  private boolean isJampServlet(ServletRegistration reg, 
                                Class<?> jampClass)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class<?> cl = Class.forName(reg.getClassName(), false, loader);
      
      return jampClass.isAssignableFrom(cl);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return false;
    }
  }
  
  private ServiceManagerAmp createManager(ServletContext ctx)
  {
    ctx.addListener(new ServletContextListenerBaratine());
    
    EmbedBuilder builder = new EmbedBuilder();
    _builder = builder;
    
    if (ctx instanceof WebApp) {
      WebApp webApp = (WebApp) ctx;
      
      String podName = webApp.getPodName();
      
      builder.podName(podName);
    }
    
    String path = ctx.getContextPath();
    
    if (path.isEmpty()) {
      path = "/";
    }

    builder.name("web-app:" + path);
    
    String dataPath = ctx.getRealPath("/WEB-INF/baratine");
    
    if (dataPath == null) {
      dataPath = ctx.getRealPath("/");
      
      if (dataPath != null) {
        dataPath +="/WEB-INF/baratine";
      }
    }

    if (dataPath != null) { 
      builder.root(dataPath);
      builder.data(dataPath);
    }
    
    String configPathName = ctx.getRealPath("/WEB-INF/baratine.cf");
    Path configPath = Vfs.lookup(configPathName);
    
    if (configPath.canRead()) {
      builder.config(configPathName);
    }
    
    // XXX: needs to be check for actual cdi
    /*
    if (CandiManager.getCurrent() == null) {
      builder.scanClassLoader();
    }
    */

    builder.scanClassLoader();

    _manager = builder.build();
    
    Objects.requireNonNull(_manager);
    
    return _manager;
  }
  
  private class ServletContextListenerBaratine implements ServletContextListener
  {
    @Override
    public void contextInitialized(ServletContextEvent event)
    {
      if (_manager != null) {
        _manager.start();
      }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event)
    {
      ServletContext context = event.getServletContext();
      
      if (context.getAttribute(WebApp.IMMEDIATE) != null) {
        _builder.closeImmediate();
      }
      else {
        _builder.close();
      }
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
