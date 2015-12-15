/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.admin;

import io.baratine.core.Startup;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.RawString;
import com.caucho.v5.deploy.DeployMode;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.host.HostConfig;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.http.webapp.WebAppConfig;
import com.caucho.v5.util.L10N;

/**
 * Enables remote administration
 */
@Startup
public class RemoteAdminService
{
  private static final Logger log
    = Logger.getLogger(RemoteAdminService.class.getName());
  private static final L10N L = new L10N(RemoteAdminService.class);

  private String _hostName = "admin.resin";
  private HttpContainer _server;
  private boolean _isAuthenticationRequired = true;

  private WebApp _webApp;

  public void setAuthenticationRequired(boolean isAuthenticationRequired)
  {
    _isAuthenticationRequired = isAuthenticationRequired;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    _server = HttpContainer.current();

    if (_server == null) {
      if (true) return;
      
      throw new ConfigException(L.l("<admin:{0}> may only be instantiated in an active server",
                                    getClass().getSimpleName()));
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      HostConfig hostConfig = new HostConfig();
      hostConfig.setHostName(new RawString(_hostName));
      hostConfig.setRootDirectory(new RawString("memory:/error/" + _hostName));
      hostConfig.setSkipDefaultConfig(true);
      hostConfig.setRedeployMode(DeployMode.MANUAL);

      WebAppConfig webAppConfig = new WebAppConfig();
      webAppConfig.setId("/");
      webAppConfig.setRootDirectory(new RawString("memory:/error/ROOT"));
      webAppConfig.setSkipDefaultConfig(true);
      webAppConfig.setRedeployMode(DeployMode.MANUAL);

      hostConfig.addPropertyProgram("web-app", webAppConfig);

      // host.addWebApp(webAppConfig);
      
      /*
      ServletMapping mapping = new ServletMapping();
      mapping.addURLPattern("/hmtp");
      mapping.setServletClass("com.caucho.remote.HmtpServlet");
      mapping.setInitParam("authentication-required",
                           String.valueOf(_isAuthenticationRequired));
      mapping.setInitParam("admin", "true");
      mapping.init();

      webAppConfig.addPropertyProgram("servlet-mapping", mapping);
      */
      
      try {
        /*
        Class<?> hampListenerClass = HampListener.class;
        
        ListenerConfig listenerCfg = new ListenerConfig();
        listenerCfg.setListenerClass(hampListenerClass);
        listenerCfg.setProperty("authentication-required",
                                _isAuthenticationRequired);
        listenerCfg.setProperty("admin", true);

        webAppConfig.addPropertyProgram("listener", listenerCfg);
        */
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }
      
      _server.addHost(hostConfig);
      
      if (log.isLoggable(Level.FINER))
        log.finer(this + " enabled at http://" + _hostName + "/hmtp");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public WebApp getWebApp()
  {
    return _webApp;
  }

  @Override
  public String toString()
  {
    if (_server != null)
      return getClass().getSimpleName() + "[" + _server.getServerId() + "]";
    else
      return getClass().getSimpleName() + "[" + null + "]";
  }
}
