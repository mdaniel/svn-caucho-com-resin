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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.bartender.ClusterBartender;
import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.deploy.DeployInstanceEnvironment;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.dispatch.FilterChainException;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.dispatch.InvocationRouter;
import com.caucho.v5.http.log.AccessLogServlet;
import com.caucho.v5.http.webapp.DeployGeneratorWebAppExpand;
import com.caucho.v5.http.webapp.ErrorPage;
import com.caucho.v5.http.webapp.ErrorPageManager;
import com.caucho.v5.http.webapp.WebAppConfig;
import com.caucho.v5.http.webapp.WebAppContainer;
import com.caucho.v5.inject.InjectManager;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.lifecycle.LifecycleState;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.make.AlwaysModified;
import com.caucho.v5.management.server.HostMXBean;
import com.caucho.v5.network.listen.PortTcp;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Dependency;
import com.caucho.v5.vfs.Path;

/**
 * Resin's virtual host implementation.
 */
public class Host
  implements EnvironmentBean, Dependency, // , XmlSchemaBean,
             DeployInstanceEnvironment, InvocationRouter<InvocationServlet>
{
  private static final L10N L = new L10N(Host.class);
  private static final Logger log = Logger.getLogger(Host.class.getName());
  
  public static final String DEFAULT_NAME = "host";
  
  private static EnvironmentLocal<Host> _hostLocal
    = new EnvironmentLocal<Host>("caucho.host");

  private final HttpContainerServlet _httpContainer;
  private final HostContainer _parent;

  // The Host entry
  private final HostController _controller;
  private final Path _rootDirectory;
  
  private EnvironmentClassLoader _classLoader;

  // The canonical host name.  The host name may include the port.
  private String _hostName = "";
  // The canonical URL
  private String _url;

  private String _serverName = "";
  private int _serverPort = 0;
  
  private final WebAppContainer _webAppContainer;
  private ErrorPageManager _errorPageManager;

  // The secure host
  private String _secureHostName;

  private Boolean _isSecure;
  
  private boolean _isDefaultHost;

  // Alises
  private ArrayList<String> _aliasList = new ArrayList<String>();

  private Throwable _configException;

  private String _configETag = null;
  
  private final Lifecycle _lifecycle;

  /**
   * Creates the webApp with its environment loader.
   */
  public Host(HostBuilder builder)
  {
    _parent = builder.getHostContainer();
    
    _controller = builder.getController();
    
    String hostName = builder.getHostName();
    
    _httpContainer = _parent.getServer();
    
    if (_httpContainer == null)
      throw new IllegalStateException(L.l("Host requires an active Servlet container"));
    
    _classLoader = builder.getClassLoader();

    _rootDirectory = _controller.getRootDirectory();
    
    if (_controller.getId().equals("host/error"))
      _lifecycle = new Lifecycle(log, "Host[" + _controller.getId() + "]", Level.FINEST);
    else
      _lifecycle = new Lifecycle(log, "Host[" + _controller.getId() + "]", Level.INFO);
    
    InjectManager.create(_classLoader);
    
    _webAppContainer = createWebAppContainer();
    
    try {
      setHostName(hostName);

      _hostLocal.set(this, getClassLoader());
    } catch (Exception e) {
      _configException = e;
    }
  }
  
  protected WebAppContainer createWebAppContainer()
  {
    return new WebAppContainer(_httpContainer, 
                               this, 
                               _rootDirectory,
                               getClassLoader(), 
                               _lifecycle);
  }
  
  protected HttpContainerServlet getHttpContainer()
  {
    return _httpContainer;
  }
  
  protected Lifecycle getLifecycle()
  {
    return _lifecycle;
  }

  /**
   * Returns the local host.
   */
  public static Host getLocal()
  {
    return _hostLocal.get();
  }

  /**
   * Sets the canonical host name.
   */
  @Configurable
  public void setHostName(String name)
    throws ConfigException
  {
    _hostName = name;

    if (name.equals("")) {
      _isDefaultHost = true;
    }

    addHostAlias(name);

    int p = name.indexOf("://");

    if (p >= 0)
      name = name.substring(p + 3);

    _serverName = name;

    p = name.lastIndexOf(':');
    if (p > 0) {
      _serverName = name.substring(0, p);

      boolean isPort = true;
      int port = 0;
      for (p++; p < name.length(); p++) {
        char ch = name.charAt(p);

        if ('0' <= ch && ch <= '9')
          port = 10 * port + ch - '0';
        else
          isPort = false;
      }

      if (isPort)
        _serverPort = port;
    }
  }

  /**
   * Returns the entry name
   */
  public String getName()
  {
    return _controller.getName();
  }

  /**
   * Returns the canonical host name.  The canonical host name may include
   * the port.
   */
  public String getHostName()
  {
    return _hostName;
  }
  
  /**
   * Returns the secure host name.  Used for redirects.
   */
  public String getSecureHostName()
  {
    return _secureHostName;
  }

  /**
   * Sets the secure host name.  Used for redirects.
   */
  public void setSecureHostName(String secureHostName)
  {
    _secureHostName = secureHostName;
  }
  
  public void setSetRequestSecure(boolean isSecure)
  {
    _isSecure = isSecure;
  }
  
  public Boolean isRequestSecure()
  {
    return _isSecure;
  }

  /**
   * Returns the relax schema.
   */
  /*
  @Override
  public String getSchema()
  {
    return "com/caucho/http/host/host.rnc";
  }
  */

  /**
   * Returns id for the host
   */
  public String getId()
  {
    return _controller.getId();
  }
  
  public String getIdTail()
  {
    String id = _controller.getId();
    
    int p1 = id.indexOf("/");
    // int p2 = id.indexOf("/", p1 + 1);
    
    return id.substring(p1 + 1);
  }

  /**
   * Returns the URL for the container.
   */
  public String getURL()
  {
    if (_url != null && ! "".equals(_url))
      return _url;
    else if (_hostName == null
             || _hostName.equals("")
             || _hostName.equals(DEFAULT_NAME)) {
      HttpContainerServlet server = getHttp();

      if (server == null)
        return "http://localhost";
      
      SystemManager resinSystem = server.getSystemManager();
      NetworkSystem networkSystem = NetworkSystem.getCurrent();

      for (PortTcp port : networkSystem.getPorts()) {
        if ("http".equals(port.getProtocolName())) {
          String address = port.getAddress();

          if (address == null || address.equals(""))
            address = "localhost";

          return "http://" + address + ":" + port.getPort();
        }
      }

      for (PortTcp port : networkSystem.getPorts()) {
        if ("https".equals(port.getProtocolName())) {
          String address = port.getAddress();
          if (address == null || address.equals(""))
            address = "localhost";

          return "https://" + address + ":" + port.getPort();
        }
      }

      return "http://localhost";
    }
    else if (_hostName.startsWith("http:")
             || _hostName.startsWith("https:"))
      return _hostName;
    else if (_hostName.equals("") || _hostName.equals(DEFAULT_NAME))
      return "http://localhost";
    else
      return "http://" + _hostName;
  }

  /**
   * Adds an alias.
   */
  public void addHostAlias(String name)
  {
    name = name.toLowerCase(Locale.ENGLISH);

    if (! _aliasList.contains(name)) {
      _aliasList.add(name);
    }

    if (name.equals("") || name.equals("*")) {
      _isDefaultHost = true;
    }

    // _controller.addExtHostAlias(name);
  }
  
  public LifecycleState getState()
  {
    return _lifecycle.getState();
  }

  /**
   * Gets the alias list.
   */
  public ArrayList<String> getAliasList()
  {
    return _aliasList;
  }

  /**
   * Adds an alias.
   */
  @Configurable
  public void addHostAliasRegexp(String name)
  {
    name = name.trim();

    Pattern pattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);

    _controller.addExtHostAliasRegexp(pattern);
  }

  /**
   * Returns true if matches the default host.
   */
  public boolean isDefaultHost()
  {
    return _isDefaultHost;
  }

  /**
   * Gets the environment class loader.
   */
  @Override
  public EnvironmentClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public Path getRootDirectory()
  {
    return _rootDirectory;
  }
  
  //
  // configuration
  //
  
  /**
   * access-log: configures the HTTP access log. 
   */
  @Configurable
  public void setAccessLog(AccessLogServlet log)
  {
    _webAppContainer.setAccessLog(log);
  }

  public WebAppContainer getWebAppContainer()
  {
    return _webAppContainer;
  }
  
  @Configurable
  public void addErrorPage(ErrorPage errorPage)
  {
    getErrorPageManager().addErrorPage(errorPage);
  }
  
  public ErrorPageManager getErrorPageManager()
  {
    if (_errorPageManager == null) {
      _errorPageManager = new ErrorPageManager(getHttp(), this, null);
    }
    
    return _errorPageManager;
  }
  
  /**
   * web-app: adds a static webapp
   */
  @Configurable
  public void addWebApp(WebAppConfig config)
  {
    getWebAppContainer().addWebApp(config);
  }
  
  @Configurable
  public void addWebAppDefault(WebAppConfig config)
  {
    getWebAppContainer().addWebAppDefault(config);
  }

  /**
   * war-deploy: configures a war expansion directory
   */
  @Configurable
  public DeployGeneratorWebAppExpand createWarDeploy()
  {
    return getWebAppContainer().createWebAppDeploy();
  }

  /**
   * Sets the war-expansion
   */
  @Configurable
  public void addWarDeploy(DeployGeneratorWebAppExpand webAppDeploy)
    throws ConfigException
  {
    getWebAppContainer().addWarDeploy(webAppDeploy);
  }

  /**
   * Sets the war-expansion
   */
  @Configurable
  public DeployGeneratorWebAppExpand createWebAppDeploy()
  {
    return getWebAppContainer().createWebAppDeploy();
  }

  /**
   * Sets the war-expansion
   */
  public void addWebAppDeploy(DeployGeneratorWebAppExpand deploy)
    throws ConfigException
  {
    getWebAppContainer().addWebAppDeploy(deploy);
  }
  
  //
  // services/baratine
  //
  
  
  @Configurable
  public void addServiceDefault(WebAppConfig config)
  {
    getWebAppContainer().addServiceDefault(config);
  }

  /**
   * Sets the service-deploy
   */
  /*
  @Configurable
  public DeployGeneratorService createServiceDeploy()
  {
    return getWebAppContainer().createServiceDeploy();
  }
  */

  /**
   * Sets the service-deploy
   */
  /*
  public void addServiceDeploy(DeployGeneratorService deploy)
    throws ConfigException
  {
    getWebAppContainer().addServiceDeploy(deploy);
  }
  */


  /**
   * Sets the war-dir for backwards compatibility.
   */
  @Configurable
  public void setWarDir(Path warDir)
    throws ConfigException
  {
    getWebAppContainer().setWarDir(warDir);
  }

  /**
   * Sets the war-expand-dir.
   */
  public void setWarExpandDir(Path warDir)
  {
    getWebAppContainer().setWarExpandDir(warDir);
  }
  
  /**
   * Sets the config exception.
   */
  @Override
  public void setConfigException(Throwable e)
  {
    if (e != null) {
      _configException = e;
      _classLoader.addDependency(AlwaysModified.create());

      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Gets the config exception.
   */
  @Override
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns the owning server.
   */
  public HttpContainerServlet getHttp()
  {
    return _parent.getServer();
  }

  /**
   * Returns the current cluster.
   */
  public ClusterBartender getCluster()
  {
    HttpContainerServlet http = getHttp();

    if (http != null)
      return http.getSelfServer().getCluster();
    else
      return null;
  }

  /**
   * Returns the config etag.
   */
  public String getConfigETag()
  {
    return _configETag;
  }

  /**
   * Returns the config etag.
   */
  public void setConfigETag(String etag)
  {
    _configETag = etag;
  }

  /**
   * Returns the admin.
   */
  public HostMXBean getAdmin()
  {
    return _controller.getAdmin();
  }

  /**
   * Initialization before configuration
   */
  public void preConfigInit()
  {
  }
  
  @Override
  public void init()
  {
    
  }

  /**
   * Starts the host.
   */
  @Override
  public void start()
  {
    if (! _lifecycle.toStarting())
      return;
    
    if (getURL().equals("") && _parent != null) {
      _url = _parent.getURL();
    }

    EnvironmentClassLoader loader = _classLoader;

    // server/1al2
    // loader.setId("host:" + getURL());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);

      // initBam();

      // ioc/04010
      // loader needs to start first, so Host managed beans will be
      // initialized before the webappd
      loader.start();
      
      getWebAppContainer().start();

      if (_parent != null)
        _parent.clearCache();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
    
    _lifecycle.toActive();
  }

  /**
   * Clears the cache
   */
  public void clearCache()
  {
    _parent.clearCache();
    
    _webAppContainer.clearCache();

    setConfigETag(null);
  }

  /**
   * Builds the invocation for the host.
   */
  @Override
  public InvocationServlet routeInvocation(InvocationServlet invocation)
    throws ConfigException
  {
    invocation.setHostName(_serverName);
    invocation.setPort(_serverPort);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (_configException == null) {
        return _webAppContainer.routeInvocation(invocation);
      }
      else {
        invocation.setFilterChain(new FilterChainException(_configException));
        invocation.setDependency(AlwaysModified.create());

        return invocation;
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns true if the host is modified.
   */
  @Override
  public boolean isModified()
  {
    return (_lifecycle.isDestroyed() || _classLoader.isModified());
  }

  /**
   * Returns true if the host is modified.
   */
  public boolean isModifiedNow()
  {
    return (_lifecycle.isDestroyed() || _classLoader.isModifiedNow());
  }

  /**
   * Log the reason for modification.
   */
  @Override
  public boolean logModified(Logger log)
  {
    if (_lifecycle.isDestroyed()) {
      log.finer(this + " is destroyed");
      
      return true;
    }
    else
      return _classLoader.logModified(log);
  }

  /**
   * Returns true if the host deploy was an error
   */
  public boolean isDeployError()
  {
    return _configException != null;
  }

  /**
   * Returns true if the host is idle
   */
  @Override
  public boolean isDeployIdle()
  {
    return false;
  }

  /**
   * Stops the host.
   */
  public boolean stop(ShutdownModeAmp mode)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      EnvironmentClassLoader envLoader = _classLoader;
      thread.setContextClassLoader(envLoader);

      if (! _lifecycle.toStopping()) {
        return false;
      }
      
      _webAppContainer.stop(mode);

      envLoader.stop();

      return true;
    } finally {
      _lifecycle.toStop();

      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Closes the host.
   */
  @Override
  public void shutdown(ShutdownModeAmp mode)
  {
    stop(mode);

    if (! _lifecycle.toDestroy()) {
      return;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    EnvironmentClassLoader classLoader = _classLoader;

    thread.setContextClassLoader(classLoader);

    try {
      _webAppContainer.destroy(mode);
    } finally {
      thread.setContextClassLoader(oldLoader);

      classLoader.destroy();
    }
  }

  /**
   * @param shortHost
   * @return
   */
  public static String calculateCanonicalIPv6(String host)
  {
    try {
      InetAddress addr = InetAddress.getByName(host);

      return "[" + addr.getHostAddress() + "]";
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return host;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }
}
