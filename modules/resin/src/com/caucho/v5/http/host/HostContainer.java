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

package com.caucho.v5.http.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.deploy.DeployContainerService;
import com.caucho.v5.deploy.DeployContainerServiceImpl;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.dispatch.FilterChainError;
import com.caucho.v5.http.dispatch.InvocationRouter;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.http.webapp.WebAppConfig;
import com.caucho.v5.io.AlwaysModified;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * Resin's host container implementation.
 */
public class HostContainer implements InvocationRouter<InvocationServlet>
{
  private static final Logger log
    = Logger.getLogger(HostContainer.class.getName());
  
  // the owning servlet container
  private final HttpContainerServlet _server;
  
  // The environment class loader
  private EnvironmentClassLoader _classLoader;

  private Host _errorHost;

  private String _url = "";
  
  // The root directory.
  private PathImpl _rootDir;

  // List of default host configurations
  private ArrayList<HostConfig> _hostDefaultList = new ArrayList<HostConfig>();

  /*
  // The host deploy
  private DeployContainerImpl<Host,HostController> _hostDeploy
    = new DeployContainerImpl<>(HostController.class);
    */
  
  private DeployContainerService<Host,HostController> _hostDeploy;
  
  // Cache of hosts
  private ConcurrentHashMap<String,DeployHandle<Host>> _hostMap
    = new ConcurrentHashMap<>();

  // Regexp host
  private ArrayList<HostConfig> _hostRegexpList = new ArrayList<HostConfig>();

  // List of default webApp configurations
  private ArrayList<WebAppConfig> _webAppDefaultList
    = new ArrayList<>();

  // lifecycle
  private final Lifecycle _lifecycle = new Lifecycle();

  /**
   * Creates the webApp with its environment loader.
   */
  public HostContainer(HttpContainerServlet server)
  {
    _server = server;
    _classLoader = server.classLoader();

    _rootDir = VfsOld.lookup();

    /*
    _errorHost = createErrorHost();
    
    if (_errorHost == null)
      throw new NullPointerException();
      */
    
    DeployContainerServiceImpl<Host, HostController> deployServiceImpl
      = new DeployContainerServiceImpl<Host,HostController>(HostController.class);
    
    ServiceManagerAmp ampManager = AmpSystem.currentManager();
    
    _hostDeploy = ampManager.newService(deployServiceImpl)
                            .start()
                            .as(DeployContainerService.class);
  }
  
  public String getClusterId()
  {
    return _server.getSelfServer().getClusterId();
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
  public HttpContainerServlet getServer()
  {
    return _server;
  }

  /**
   * Gets the root directory.
   */
  public PathImpl getRootDirectory()
  {
    return _rootDir;
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(PathImpl path)
  {
    _rootDir = path;
  }

  /**
   * Sets the root directory (obsolete).
   * @deprecated
   */
  public void setRootDir(PathImpl path)
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
  public DeployGeneratorHostExpand createHostDeploy()
  {
    //String id = "host/" + getClusterId();
    String id = "hosts";
    
    return new DeployGeneratorHostExpand(id, _hostDeploy, this);
  }

  public DeployContainerService<Host,HostController> getHostDeployContainer()
  {
    return _hostDeploy;
  }
  
  /**
   * Adds a host deploy
   */
  public void addHostDeploy(DeployGeneratorHostExpand hostDeploy)
  {
    _hostDeploy.add(hostDeploy);
  }

  /**
   * Adds a host.
   */
  public void addHost(HostConfig hostConfig)
  {
    if (hostConfig.getRegexp() != null) {
      _hostDeploy.add(new DeployGeneratorHostRegexp(_hostDeploy, this, hostConfig));
      return;
    }

    DeployGeneratorHostSingle deploy;
    deploy = new DeployGeneratorHostSingle(_hostDeploy, this, hostConfig);
    
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
  public InvocationServlet routeInvocation(InvocationServlet invocation)
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
      invocation = host.routeInvocation(invocation);
      isAlwaysModified = false;
    }
    else {
      FilterChain chain = new FilterChainError(404);
      invocation.setFilterChain(chain);
      invocation.setWebApp(getErrorWebApp());
      isAlwaysModified = true;
    }

    if (isAlwaysModified) {
      // XXX: rewrite might need to remove
      invocation.setDependency(AlwaysModified.create());
    }

    return invocation;
  }
  
  public HostController []getHostList()
  {
    return _hostDeploy.getControllers();
  }

  public DeployHandle<Host>[] getHostHandles()
  {
    return _hostDeploy.getHandles();
  }

  /**
   * Returns the matching host.
   */
  public Host getHost(String hostName, int port)
  {
    try {
      DeployHandle<Host> handle = findHost(hostName, port);

      if (handle != null) {
        return handle.request();
      }
      else {
        return null;
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Returns the matching host.
   */
  public DeployHandle<Host> getHostHandle(String hostName, int port)
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
  private DeployHandle<Host> findHost(String rawHost, int rawPort)
    throws Exception
  {
    if (rawHost == null) {
      rawHost = "";
    }

    int p = rawHost.lastIndexOf(':');
    int q = rawHost.lastIndexOf(']');

    String shortHost = rawHost;

    if (p > 0 && q < p) {
      shortHost = rawHost.substring(0, p);
    }
    
    if (shortHost.startsWith("[")) {
      shortHost = Host.calculateCanonicalIPv6(shortHost);
    }
    
    String fullHost = shortHost + ':' + rawPort;

    DeployHandle<Host> hostHandle = null;
    
    hostHandle = _hostMap.get(fullHost);
    
    if (hostHandle != null && ! hostHandle.getState().isDestroyed()) {
      return hostHandle;
    }

    if (hostHandle == null || hostHandle.getState().isDestroyed()) {
      hostHandle = _hostMap.get(shortHost);
    }

    if (hostHandle == null || hostHandle.getState().isDestroyed()) {
      hostHandle = findHostHandle(fullHost);
    }

    if (hostHandle == null || hostHandle.getState().isDestroyed()) {
      hostHandle = findHostHandle(shortHost);
    }

    if (hostHandle == null || hostHandle.getState().isDestroyed()) {
      hostHandle = findHostHandle(":" + rawPort);
    }

    if (hostHandle == null || hostHandle.getState().isDestroyed()) {
      hostHandle = findHostHandle("");
    }

    if (hostHandle == null) {
    }
    else if (! hostHandle.getState().isDestroyed())
      _hostMap.put(fullHost, hostHandle);
    else {
      hostHandle = null;
      _hostMap.remove(fullHost);
    }

    return hostHandle;
  }

  /**
   * Returns the HostController based on a host name.  The canonical name
   * and the host aliases are tested for the match.
   *
   * @param hostName name to match on
   * @return the host entry or null if none are found.
   */
  private DeployHandle<Host> findHostHandle(String hostName)
    throws Exception
  {
    //HostController controller = _hostDeploy.findController(hostName);
    DeployHandle<Host> handle = _hostDeploy.findHandle(hostName);
    
    return handle;
  }

  /**
   * Returns the error webApp during startup.
   */
  public WebAppResinBase getErrorWebApp()
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

      String id = "hosts/error";
      PathImpl rootDirectory = VfsOld.lookup("memory:/error");
      HostController controller
        = createController(id, rootDirectory, "error", 
                          null, null);
      
      DeployHandle<Host> handle = _hostDeploy.createHandle(id);
      handle.getService().setController(controller);
      
      //controller.init(null);
      //controller.startOnInit();
      //controller.start();
      
      //Host host = controller.request();
      Host host = handle.request();

      return host;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  protected HostController createController(String id,
                                            PathImpl rootDir,
                                            String hostName,
                                            HostConfig config,
                                            HashMap<String, Object> varMap)
  {
    // HostHandle handle = createHandle(id);
    
    return new HostController(id, rootDir, hostName, config, this, varMap);
  }
  
  /*
  protected HostHandle createHandle(String id)
  {
    HostHandle handle = _hostMap.get(id);
    
    if (handle == null) {
      DeployControllerService<Host> service = DeployControllerServiceImpl.create(id, log);
    
      handle = new HostHandle(id, service);
      
      _hostMap.putIfAbsent(id, handle);
      
      handle = _hostMap.get(id);
    }
    
    return handle;
  }
  */

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
  public void stop(ShutdownModeAmp mode)
  {
    if (! _lifecycle.toStop()) {
      return;
    }

    _hostDeploy.stop(mode);
    
    // _classLoader.stop();
  }

  /**
   * Closes the container.
   */
  public void destroy(ShutdownModeAmp mode)
  {
    stop(mode);

    if (! _lifecycle.toDestroy())
      return;

    _hostDeploy.destroy(mode);

    _classLoader.destroy();
  }
}
