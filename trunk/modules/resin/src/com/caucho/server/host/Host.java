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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.caucho.bam.broker.Broker;
import com.caucho.bam.broker.ManagedBroker;
import com.caucho.cloud.network.NetworkListenSystem;
import com.caucho.cloud.topology.CloudCluster;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.env.deploy.EnvironmentDeployInstance;
import com.caucho.env.service.ResinSystem;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hemp.broker.HempBrokerManager;
import com.caucho.http.log.AccessLog;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.AlwaysModified;
import com.caucho.management.server.HostMXBean;
import com.caucho.network.listen.TcpPort;
import com.caucho.rewrite.DispatchRule;
import com.caucho.rewrite.RewriteFilter;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationBuilder;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.e_app.EarDeployGenerator;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.webapp.ErrorPage;
import com.caucho.server.webapp.ErrorPageManager;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.server.webapp.WebAppContainer;
import com.caucho.server.webapp.WebAppExpandDeployGenerator;
import com.caucho.util.HostUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;

/**
 * Resin's virtual host implementation.
 */
public class Host
  implements EnvironmentBean, Dependency, SchemaBean,
             EnvironmentDeployInstance, InvocationBuilder
{
  private static final L10N L = new L10N(Host.class);
  private static final Logger log = Logger.getLogger(Host.class.getName());
  
  private static EnvironmentLocal<Host> _hostLocal
    = new EnvironmentLocal<Host>("caucho.host");

  private final ServletService _servletContainer;
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

  private HempBroker _bamBroker;

  private Throwable _configException;

  private String _configETag = null;
  
  private final Lifecycle _lifecycle;

  /**
   * Creates the webApp with its environment loader.
   */
  public Host(HostContainer parent, 
              HostController controller, 
              String hostName)
  {
    _servletContainer = parent.getServer();
    
    if (_servletContainer == null)
      throw new IllegalStateException(L.l("Host requires an active Servlet container"));
    
    _classLoader = EnvironmentClassLoader.create("host:" + controller.getName());

    _parent = parent;
    _controller = controller;
    
    _rootDirectory = controller.getRootDirectory();
    
    if (controller.getId().startsWith("error/"))
      _lifecycle = new Lifecycle(log, "Host[" + controller.getId() + "]", Level.FINEST);
    else
      _lifecycle = new Lifecycle(log, "Host[" + controller.getId() + "]", Level.INFO);
    
    InjectManager.create(_classLoader);
    
    _webAppContainer = new WebAppContainer(_servletContainer, 
                                           this, 
                                           _rootDirectory,
                                           getClassLoader(), 
                                           _lifecycle);
    
    try {
      setHostName(hostName);

      _hostLocal.set(this, getClassLoader());
    } catch (Exception e) {
      _configException = e;
    }
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

    if (name.equals(""))
      _isDefaultHost = true;

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
   * Returns the bam broker.
   */
  public Broker getBamBroker()
  {
    return _bamBroker;
  }

  /**
   * Returns the relax schema.
   */
  @Override
  public String getSchema()
  {
    return "com/caucho/server/host/host.rnc";
  }

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
    
    int p = id.indexOf("/host/");
    
    return id.substring(p + 6);
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
             || _hostName.equals("default")) {
      ServletService server = getServer();

      if (server == null)
        return "http://localhost";
      
      ResinSystem resinSystem = server.getResinSystem();
      NetworkListenSystem listenService 
        = resinSystem.getService(NetworkListenSystem.class);

      for (TcpPort port : listenService.getListeners()) {
        if ("http".equals(port.getProtocolName())) {
          String address = port.getAddress();

          if (address == null || address.equals(""))
            address = "localhost";

          return "http://" + address + ":" + port.getPort();
        }
      }

      for (TcpPort port : listenService.getListeners()) {
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
    else if (_hostName.equals("") || _hostName.equals("default"))
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

    if (! _aliasList.contains(name))
      _aliasList.add(name);

    if (name.equals("") || name.equals("*"))
      _isDefaultHost = true;


    _controller.addExtHostAlias(name);
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
  
  @Configurable
  public void setAccessLog(AccessLog log)
  {
    _webAppContainer.setAccessLog(log);
  }
  
  /**
   * Sets the doc dir.
   */
  public void setDocumentDirectory(Path docDir)
  {
    _webAppContainer.setDocumentDirectory(docDir);
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
      _errorPageManager = new ErrorPageManager(getServer(), this, null);
    }
    
    return _errorPageManager;
  }
  
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
   * Sets the war-expansion
   */
  @Configurable
  public WebAppExpandDeployGenerator createWarDeploy()
  {
    return getWebAppContainer().createWebAppDeploy();
  }

  /**
   * Sets the war-expansion
   */
  @Configurable
  public void addWarDeploy(WebAppExpandDeployGenerator webAppDeploy)
    throws ConfigException
  {
    getWebAppContainer().addWarDeploy(webAppDeploy);
  }

  /**
   * Sets the war-expansion
   */
  @Configurable
  public WebAppExpandDeployGenerator createWebAppDeploy()
  {
    return getWebAppContainer().createWebAppDeploy();
  }

  /**
   * Sets the war-expansion
   */
  public void addWebAppDeploy(WebAppExpandDeployGenerator deploy)
    throws ConfigException
  {
    getWebAppContainer().addWebAppDeploy(deploy);
  }

  /**
   * Sets the ear-expansion
   */
  @Configurable
  public EarDeployGenerator createEarDeploy()
    throws Exception
  {
    return getWebAppContainer().createEarDeploy();
  }

  /**
   * Adds the ear-expansion
   */
  @Configurable
  public void addEarDeploy(EarDeployGenerator earDeploy)
    throws Exception
  {
    getWebAppContainer().addEarDeploy(earDeploy);
  }
  
  @Configurable
  public void addEarDefault(EarConfig config)
  {
    getWebAppContainer().addEarDefault(config);
  }

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
   * Adds a rewrite dispatch rule
   */
  public void add(DispatchRule dispatchRule)
  {
    _webAppContainer.add(dispatchRule);
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(RewriteFilter dispatchAction)
  {
    _webAppContainer.add(dispatchAction);
  }

  /**
   * Adds rewrite-dispatch (backward compat).
   */
  public RewriteDispatch createRewriteDispatch()
  {
    return _webAppContainer.createRewriteDispatch();
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
  public ServletService getServer()
  {
    return _parent.getServer();
  }

  /**
   * Returns the current cluster.
   */
  public CloudCluster getCluster()
  {
    ServletService server = getServer();

    if (server != null)
      return server.getCluster();
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

      initBam();

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

  private void initBam()
  {
    if (Resin.getCurrent() == null)
      return;

    String hostName = _hostName;

    if ("".equals(hostName)) {
      hostName = HostUtil.getLocalHostName();
    }

    HempBrokerManager brokerManager = HempBrokerManager.getCurrent();

    _bamBroker = new HempBroker(brokerManager, hostName);

    if (brokerManager != null)
      brokerManager.addBroker(hostName, _bamBroker);

    for (String alias : _aliasList) {
      _bamBroker.addAlias(alias);

      if (brokerManager != null)
        brokerManager.addBroker(alias, _bamBroker);
    }

    InjectManager cdiManager = InjectManager.getCurrent();

    cdiManager.addBeanDiscover(cdiManager.createBeanFactory(ManagedBroker.class)
                       .name("bamBroker").singleton(_bamBroker));

    // webBeans.addExtension(_bamBroker);

    // XXX: webBeans.addRegistrationListener(new BamRegisterListener());
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
  public Invocation buildInvocation(Invocation invocation)
    throws ConfigException
  {
    invocation.setHostName(_serverName);
    invocation.setPort(_serverPort);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (_configException == null)
        return _webAppContainer.buildInvocation(invocation);
      else {
        invocation.setFilterChain(new ExceptionFilterChain(_configException));
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
  public boolean stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      EnvironmentClassLoader envLoader = _classLoader;
      thread.setContextClassLoader(envLoader);

      if (! _lifecycle.toStopping())
        return false;

      _webAppContainer.stop();

      if (_bamBroker != null)
        _bamBroker.close();

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
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    EnvironmentClassLoader classLoader = _classLoader;

    thread.setContextClassLoader(classLoader);

    try {
      _webAppContainer.destroy();
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
