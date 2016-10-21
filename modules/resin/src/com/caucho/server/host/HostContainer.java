/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
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

package com.caucho.server.host;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;

import com.caucho.config.ConfigException;
import com.caucho.env.deploy.DeployContainer;
import com.caucho.env.deploy.DeployContainerApi;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.make.AlwaysModified;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.dispatch.InvocationBuilder;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.webapp.AccessLogFilterChain;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.server.webapp.WebAppFilterChain;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Resin's host container implementation.
 */
public class HostContainer implements InvocationBuilder {
  private static final Logger log
    = Logger.getLogger(HostContainer.class.getName());
  
  // the owning servlet container
  private final ServletService _server;
  
  // The environment class loader
  private EnvironmentClassLoader _classLoader;

  private Host _errorHost;

  private String _url = "";
  
  // The root directory.
  private Path _rootDir;

  // dispatch mapping
  private RewriteDispatch _rewriteDispatch;

  // List of default host configurations
  private ArrayList<HostConfig> _hostDefaultList = new ArrayList<HostConfig>();

  // The host deploy
  private DeployContainer<HostController> _hostDeploy
    = new DeployContainer<HostController>(HostController.class);
  
  // Cache of hosts
  private ConcurrentHashMap<String,HostController> _hostMap
    = new ConcurrentHashMap<String,HostController>();

  // Regexp host
  private ArrayList<HostConfig> _hostRegexpList = new ArrayList<HostConfig>();

  // List of default webApp configurations
  private ArrayList<WebAppConfig> _webAppDefaultList
    = new ArrayList<WebAppConfig>();
  
  // List of default ear configurations
  private ArrayList<EarConfig> _earDefaultList
    = new ArrayList<EarConfig>();

  // lifecycle
  private final Lifecycle _lifecycle = new Lifecycle();

  /**
   * Creates the webApp with its environment loader.
   */
  public HostContainer(ServletService server)
  {
    _server = server;
    _classLoader = server.getClassLoader();

    _rootDir = Vfs.lookup();

    /*
    _errorHost = createErrorHost();
    
    if (_errorHost == null)
      throw new NullPointerException();
      */
  }
  
  public String getStageTag()
  {
    return "production";
  }

  /**
   * Gets the environment class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Sets the URL for the default server.
   */
  public void setURL(String url)
  {
    _url = url;
  }

  /**
   * Gets the URL for the default server.
   */
  public String getURL()
  {
    return _url;
  }

  /**
   * Gets the dispatch server.
   */
  public ServletService getServer()
  {
    return _server;
  }

  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDir;
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path path)
  {
    _rootDir = path;
  }

  /**
   * Sets the root directory (obsolete).
   * @deprecated
   */
  public void setRootDir(Path path)
  {
    setRootDirectory(path);
  }

  /**
   * Adds a host default
   */
  public void addHostDefault(HostConfig init)
  {
    _hostDefaultList.add(init);
  }

  /**
   * Returns the list of host defaults
   */
  public ArrayList<HostConfig> getHostDefaultList()
  {
    return _hostDefaultList;
  }

  /**
   * Creates a host deploy
   */
  public HostExpandDeployGenerator createHostDeploy()
  {
    String id = getStageTag() + "/host";
    
    return new HostExpandDeployGenerator(id, _hostDeploy, this);
  }

  public DeployContainerApi<HostController> getHostDeployContainer()
  {
    return _hostDeploy;
  }
  
  /**
   * Adds a host deploy
   */
  public void addHostDeploy(HostExpandDeployGenerator hostDeploy)
  {
    _hostDeploy.add(hostDeploy);
  }

  /**
   * Adds a host.
   */
  public void addHost(HostConfig hostConfig)
  {
    if (hostConfig.getRegexp() != null) {
      _hostDeploy.add(new HostRegexpDeployGenerator(_hostDeploy, this, hostConfig));
      return;
    }

    HostSingleDeployGenerator deploy;
    deploy = new HostSingleDeployGenerator(_hostDeploy, this, hostConfig);
    
    _hostDeploy.add(deploy);
  }

  /**
   * Adds a web-app default
   */
  public void addWebAppDefault(WebAppConfig init)
  {
    _webAppDefaultList.add(init);
  }

  /**
   * Returns the list of web-app defaults
   */
  public ArrayList<WebAppConfig> getWebAppDefaultList()
  {
    return _webAppDefaultList;
  }

  /**
   * Adds an ear default
   */
  public void addEarDefault(EarConfig init)
  {
    _earDefaultList.add(init);
  }

  /**
   * Returns the list of ear defaults
   */
  public ArrayList<EarConfig> getEarDefaultList()
  {
    return _earDefaultList;
  }

  /**
   * Adds rewrite-dispatch.
   */
  public RewriteDispatch createRewriteDispatch()
  {
    if (_rewriteDispatch == null) {
      _rewriteDispatch = new RewriteDispatch(getErrorWebApp());
    }

    return _rewriteDispatch;
  }

  /**
   * Clears the cache.
   */
  public void clearCache()
  {
    _hostMap.clear();
    
    getServer().clearCache();
  }

  /**
   * Creates the invocation.
   */
  @Override
  public Invocation buildInvocation(Invocation invocation)
  {
    String rawHost = invocation.getHost();
    int rawPort = invocation.getPort();

    String hostName;

    if (rawHost == null)
      hostName = "";
    else
      hostName = DomainName.fromAscii(rawHost);

    invocation.setHostName(hostName);

    boolean isAlwaysModified;

    Host host = getHost(hostName, rawPort);

    if (host != null) {
      invocation = host.buildInvocation(invocation);
      isAlwaysModified = false;
    }
    else {
      FilterChain chain = new ErrorFilterChain(404);
      invocation.setFilterChain(chain);
      invocation.setWebApp(getErrorWebApp());
      isAlwaysModified = true;
    }

    if (_rewriteDispatch != null) {
      String url;

      if (invocation.isSecure())
        url = "https://" + hostName + invocation.getURI();
      else
        url = "http://" + hostName + invocation.getURI();

      String queryString = invocation.getQueryString();

      FilterChain chain = invocation.getFilterChain();
      FilterChain rewriteChain = _rewriteDispatch.map(DispatcherType.REQUEST,
                                                      url,
                                                      queryString,
                                                      chain);

      if (rewriteChain != chain) {
        ServletService server = getServer();
        WebApp webApp = server.getDefaultWebApp();
        invocation.setWebApp(webApp);

        if (webApp != null) {
          rewriteChain = new WebAppFilterChain(rewriteChain, webApp);

          if (webApp.getAccessLog() != null)
            rewriteChain = new AccessLogFilterChain(rewriteChain, webApp);
        }

        invocation.setFilterChain(rewriteChain);
        isAlwaysModified = false;
      }
    }

    if (isAlwaysModified)
      invocation.setDependency(AlwaysModified.create());

    return invocation;
  }

  public HostController []getHostList()
  {
    return _hostDeploy.getControllers();
  }

  /**
   * Returns the matching host.
   */
  public Host getHost(String hostName, int port)
  {
    try {
      HostController controller = findHost(hostName, port);

      if (controller != null)
        return controller.request();
      else
        return null;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Returns the matching host.
   */
  public HostController getHostController(String hostName, int port)
  {
    try {
      return findHost(hostName, port);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Finds the best matching host entry.
   */
  private HostController findHost(String rawHost, int rawPort)
    throws Exception
  {
    if (rawHost == null)
      rawHost = "";

    int p = rawHost.lastIndexOf(':');
    int q = rawHost.lastIndexOf(']');

    String shortHost = rawHost;

    if (p > 0 && q < p)
      shortHost = rawHost.substring(0, p);
    
    if (shortHost.startsWith("["))
      shortHost = Host.calculateCanonicalIPv6(shortHost);
    
    String fullHost = shortHost + ':' + rawPort;

    HostController hostController = null;
    
    hostController = _hostMap.get(fullHost);

    if (hostController != null && ! hostController.getState().isDestroyed())
      return hostController;

    if (hostController == null || hostController.getState().isDestroyed())
      hostController = _hostMap.get(shortHost);

    if (hostController == null || hostController.getState().isDestroyed())
      hostController = findHostController(fullHost);

    if (hostController == null || hostController.getState().isDestroyed())
      hostController = findHostController(shortHost);

    if (hostController == null || hostController.getState().isDestroyed())
      hostController = findHostController(":" + rawPort);

    if (hostController == null || hostController.getState().isDestroyed())
      hostController = findHostController("");

    if (hostController == null) {
    }
    else if (! hostController.getState().isDestroyed())
      _hostMap.put(fullHost, hostController);
    else {
      hostController = null;
      _hostMap.remove(fullHost);
    }

    return hostController;
  }

  /**
   * Returns the HostController based on a host name.  The canonical name
   * and the host aliases are tested for the match.
   *
   * @param hostName name to match on
   * @return the host entry or null if none are found.
   */
  private HostController findHostController(String hostName)
    throws Exception
  {
    HostController controller = _hostDeploy.findController(hostName);
    
    return controller;
  }

  /**
   * Returns the error webApp during startup.
   */
  public WebApp getErrorWebApp()
  {
    Host errorHost = getErrorHost();
    
    if (errorHost != null)
      return errorHost.getWebAppContainer().getErrorWebApp();
    else
      return null;
  }
  
  private Host createErrorHost()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      Path rootDirectory = Vfs.lookup("memory:/error");
      HostController controller
        = new HostController("error/host/error", rootDirectory, "error", 
                             null, this, null);
      controller.init();
      controller.startOnInit();
      controller.start();
      
      Host host = controller.request();

      return host;
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  
  private Host getErrorHost()
  {
    if (! _lifecycle.isActive())
      return null;
    
    Host defaultHost = getHost("", 0);
    
    if (defaultHost != null)
      return defaultHost;
    else {
      if (_errorHost == null)
        _errorHost = createErrorHost();

      return _errorHost;
    }
  }

  /**
   * Starts the container.
   */
  public void start()
  {
    if (! _lifecycle.toActive()) {
      return;
    }

    _lifecycle.toActive();

    _hostDeploy.start();
  }

  /**
   * Stops the container.
   */
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;

    _hostDeploy.stop();
    
    if (_errorHost != null) {
      _errorHost.stop();
    }
    
    // _classLoader.stop();
  }

  /**
   * Closes the container.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    _hostDeploy.destroy();
    
    if (_errorHost != null) {
      System.out.println("DEST: " + _errorHost);
      _errorHost.destroy();
    }

    _classLoader.destroy();
  }
}
