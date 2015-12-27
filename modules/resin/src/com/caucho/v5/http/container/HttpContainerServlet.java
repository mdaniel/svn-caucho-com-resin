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

package com.caucho.v5.http.container;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.amp.manager.ServerAuthManager;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.config.AdminLiteral;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.env.shutdown.ShutdownSystem;
import com.caucho.v5.http.cache.HttpCache;
import com.caucho.v5.http.cache.HttpCacheBase;
import com.caucho.v5.http.dispatch.FilterChainError;
import com.caucho.v5.http.dispatch.FilterChainException;
import com.caucho.v5.http.dispatch.InvocationDecoderServlet;
import com.caucho.v5.http.dispatch.InvocationManager;
import com.caucho.v5.http.dispatch.InvocationManagerBuilder;
import com.caucho.v5.http.dispatch.InvocationRouter;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.host.DeployGeneratorHostExpand;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.host.HostConfig;
import com.caucho.v5.http.host.HostContainer;
import com.caucho.v5.http.log.AccessLogServlet;
import com.caucho.v5.http.log.AccessLogBase;
import com.caucho.v5.http.protocol.HttpBufferStore;
import com.caucho.v5.http.protocol.RequestFacade;
import com.caucho.v5.http.protocol.RequestHttpBase;
import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.http.protocol.ResponseFacade;
import com.caucho.v5.http.protocol.ResponseServlet;
import com.caucho.v5.http.security.AuthenticatorRole;
import com.caucho.v5.http.webapp.ErrorPage;
import com.caucho.v5.http.webapp.ErrorPageManager;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.http.webapp.WebAppBuilder;
import com.caucho.v5.http.webapp.WebAppConfig;
import com.caucho.v5.http.webapp.WebAppController;
import com.caucho.v5.inject.InjectManager;
import com.caucho.v5.jmx.server.EnvironmentMXBean;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.ClassLoaderListener;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.Environment;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.make.AlwaysModified;
import com.caucho.v5.management.server.ServerMXBean;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.FreeRing;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ClientDisconnectException;
import com.caucho.v5.vfs.Dependency;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

public class HttpContainerServlet extends HttpContainerBase<InvocationServlet>
  implements AlarmListener, ClassLoaderListener, 
             InvocationRouter<InvocationServlet>, 
             Dependency
{
  private static final L10N L = new L10N(HttpContainerServlet.class);
  private static final Logger log
    = Logger.getLogger(HttpContainerServlet.class.getName());

  private static final long ALARM_INTERVAL = 60000;

  private static final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  private static final EnvironmentLocal<HttpContainerServlet> _serverLocal
    = new EnvironmentLocal<HttpContainerServlet>();

  private Throwable _configException;

  private ServerAuthManager _authManager;
  private AuthenticatorRole _adminAuth;

  private InjectManager _injectManager;

  private HostContainer _hostContainer;
  
  private ErrorPageManager _errorPageManager = new ErrorPageManager(this);

  private boolean _isEnabled = true;

  private String _serverHeader;

  //private int _urlLengthMax = 8192;
  //private int _headerSizeMax = TempBuffer.isSmallmem() ? 4 * 1024 : 16 * 1024;
  //private int _headerCountMax = TempBuffer.isSmallmem() ? 32 : 256;

  private long _waitForActiveTime = 10000L;

  private boolean _isDevelopmentModeErrorPage = true;

  private long _shutdownWaitMax = 60 * 1000;
  
  private boolean _isIgnoreClientDisconnect = true;

  // <cluster> configuration

  private String _connectionErrorPage;

  private HttpAdmin _admin;

  private Alarm _alarm;
  private HttpCacheBase _httpCache;
  
  private boolean _isSendfileEnabled = true;
  // private long _sendfileMinLength = 128 * 1024L;
  private long _sendfileMinLength = 32 * 1024L;
  
  private final FreeRing<HttpBufferStore> _httpBufferFreeList
    = new FreeRing<HttpBufferStore>(256);

  
  //
  // internal databases
  //

  // stats
  
  private final AtomicLong _sendfileCount = new AtomicLong();

  private long _startTime;

  private final Lifecycle _lifecycle;
  private AccessLogServlet _accessLog;
  private int _accessLogBufferSize;

  /**
   * Creates a new servlet server.
   */
  public HttpContainerServlet(HttpContainerBuilderServlet builder)
  {
    super(builder);
    
    //_systemManager = SystemManager.getCurrent();
    //Objects.requireNonNull(_systemManager);
    
    //_selfServer = builder.getServerSelf();
    //Objects.requireNonNull(_selfServer);
    
    _serverHeader = builder.getServerHeader();
    Objects.requireNonNull(_serverHeader);
    
    _httpCache = new HttpCache(this);
    
    /*
    InvocationManagerBuilder invocationBuilder
      = new InvocationManagerBuilder();
    
    if (_httpCache != null
        && invocationBuilder.getCacheSize() < _httpCache.getEntries()) {
      invocationBuilder.cacheSize(_httpCache.getEntries());
    }
    
    invocationBuilder.router(this);
    */
    // invocationBuilder.cacheSize(builder.get)
    
    //_invocationServer = invocationBuilder.build();
    
    // pod id can't include the server since it's used as part of
    // cache ids
    //String podId
    //  = (cluster.getId() + ":" + _selfServer.getClusterPod().getId());

    String id = getSelfServer().getId();
    
    if ("".equals(id)) {
      throw new IllegalStateException();
    }
    
    // cannot set the based on server-id because of distributed cache
    // _classLoader.setId("server:" + id);

    _serverLocal.set(this, getClassLoader());

    try {
      Thread thread = Thread.currentThread();

      Environment.addClassLoaderListener(this, getClassLoader());

      /*
      PermissionManager permissionManager = new PermissionManager();
      PermissionManager.setPermissionManager(permissionManager);
      */

      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(getClassLoader());

        _serverIdLocal.set(getSelfServer().getId());
        
        _lifecycle = new Lifecycle(log, toString(), Level.FINE);
        
        preInit();
        
        builder.getHttpProgram().configure(this);
        //builder.getHostProgram().inject(_hostContainer);
        builder.getHostProgram().configure(_hostContainer);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      // exceptions here must throw to the top because they're non-recoverable
      throw ConfigException.create(e);
    }
  }
  
  public static HttpContainerServlet current()
  {
    return (HttpContainerServlet) HttpContainer.current();
  }
  
  @Override
  protected InvocationManager<InvocationServlet>
  createInvocationManager(HttpContainerBuilder builder)
  {
    InvocationManagerBuilder<InvocationServlet> invocationBuilder
      = new InvocationManagerBuilder<>();
    
    if (_httpCache != null
        && invocationBuilder.getCacheSize() < _httpCache.getEntries()) {
      invocationBuilder.cacheSize(_httpCache.getEntries());
    }
    
    invocationBuilder.decoder(new InvocationDecoderServlet());
    
    invocationBuilder.router(this);
    
    return invocationBuilder.build();
  }

  protected void preInit()
  {
    _injectManager = InjectManager.create();
    
    BartenderSystem bartender = BartenderSystem.getCurrent();
    
    // _podContainer = new PodContainer(bartender, this);
    _hostContainer = createHostContainer();

    _alarm = new Alarm(this);
    
    // _bamService = BamSystem.getCurrent();

    _authManager = new ServerAuthManager();
    // XXX:
    _authManager.setAuthenticationRequired(false);
    // _bamService.setLinkManager(_authManager);

    // _resinSystem.addService(new DeployUpdateService());

    // _selfServer.getServerProgram().configure(this);
  }
  
  protected HostContainer createHostContainer()
  {
    return new HostContainer(this);
  }
  
  /*
  public SystemManager getSystemManager()
  {
    return _systemManager;
  }
  */

  public String getUniqueServerName()
  {
    return getSelfServer().getId();
  }

  /**
   * Returns the classLoader
   */
  /*
  public EnvironmentClassLoader getClassLoader()
  {
    return _systemManager.getClassLoader();
  }
  */

  /**
   * Returns the configuration exception
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns the configuration instance.
   */
  /*
  public void setConfigException(Throwable exn)
  {
    _configException = exn;
  }
  */

  public long getCacheMaxLength()
  {
    // XXX:
    return 1024 * 1024;
  }

  /**
   * Returns the cluster
   */
  /*
  public ClusterBartender getCluster()
  {
    return _selfServer.getCluster();
  }
  */
  
  protected HostContainer getHostContainer()
  {
    return _hostContainer;
  }
  
  public InvocationDecoderServlet getInvocationDecoder()
  {
    return (InvocationDecoderServlet) super.getInvocationDecoder();
  }

  /*
  public PodContainer getPodContainer()
  {
    return _podContainer;
  }
  */

  /**
   * Returns all the clusters
   */
  /*
  public Iterable<? extends ClusterBartender> getClusterList()
  {
    return _selfServer.getRoot().getClusters();
  }
  */

  /**
   * Returns the self server
   */
  /*
  public ServerBartender getSelfServer()
  {
    return _selfServer;
  }
  */

  public AuthenticatorRole getAdminAuthenticator()
  {
    if (_adminAuth == null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(getClassLoader());

        _adminAuth = _injectManager.lookup(AuthenticatorRole.class,
                                              new AdminLiteral());

        /*
        if (_adminAuth != null)
          _adminAuth.initCache();
          */
      } catch (Exception e) {
        e.printStackTrace();
        if (log.isLoggable(Level.FINEST))
          log.log(Level.FINEST, e.toString(), e);
        else
          log.finer(e.toString());

        //_adminAuth = new AdminAuthenticator();
        // _adminAuth.initCache();
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }

    return _adminAuth;
  }
  
  //
  // <cluster>
  //

  /**
   * Development mode error pages.
   */
  public boolean isDevelopmentModeErrorPage()
  {
    return _isDevelopmentModeErrorPage;
  }

  /**
   * Development mode error pages.
   */
  public void setDevelopmentModeErrorPage(boolean isEnable)
  {
    _isDevelopmentModeErrorPage = isEnable;
  }

  /**
   * Returns the stage id
   */
  public String getClusterName()
  {
    return getSelfServer().getClusterId();
  }
  
  /**
   * Returns true for an enabled service.
   */
  public boolean isEnabled()
  {
    return _isEnabled;
  }
  
  public void setEnabled(boolean isEnabled)
  {
    _isEnabled = isEnabled;
    
    clearCache();
  }

  /**
   * Sets the max wait time for shutdown.
   */
  public void setShutdownWaitMax(Period waitTime)
  {
    _shutdownWaitMax = waitTime.getPeriod();
  }

  /**
   * Gets the max wait time for a shutdown.
   */
  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }

  //
  // Configuration from <cluster>
  //

  /**
   * Sets the connection error page.
   */
  public void setConnectionErrorPage(String errorPage)
  {
    _connectionErrorPage = errorPage;
  }

  /**
   * Gets the connection error page.
   */
  public String getConnectionErrorPage()
  {
    return _connectionErrorPage;
  }

  /**
   * Return true if idle.
   */
  public boolean isDeployError()
  {
    return _configException != null;
  }
  
  /**
   * True if client disconnects should be invisible to servlets.
   */
  public boolean isIgnoreClientDisconnect()
  {
    return _isIgnoreClientDisconnect;
  }
  
  /**
   * True if client disconnections should be invisible to servlets.
   */
  public void setIgnoreClientDisconnect(boolean isIgnore)
  {
    _isIgnoreClientDisconnect = isIgnore;
  }

  /**
   * Returns the id.
   */
  /*
  public String getServerId()
  {
    return _selfServer.getId();
  }
  */

  /**
   * Returns the id.
   */
  /*
  public String getServerDisplayName()
  {
    return _selfServer.getDisplayName();
  }
  */

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path path)
  {
    _hostContainer.setRootDirectory(path);

    Vfs.setPwd(path, getClassLoader());
  }

  /**
   * Sets the root directory.
   */
  public Path getRootDirectory()
  {
    return _hostContainer.getRootDirectory();
  }

  /**
   * Sets the server header.
   */
  public void setServerHeader(String server)
  {
    _serverHeader = server;
  }

  /**
   * Gets the server header.
   */
  public String getServerHeader()
  {
    return _serverHeader;
  }

  /**
   * Sets the url-length-max
   */
  /*
  public void setUrlLengthMax(int length)
  {
    _urlLengthMax = length;

    getInvocationDecoder().setMaxURILength(length);
  }
  */

  /**
   * Gets the url-length-max
   */
  /*
  public int getUrlLengthMax()
  {
    // XXX:
    return getInvocationDecoder().getMaxURILength();
  }
  */

  /**
   * Sets the header-size-max
   */
  /*
  public void setHeaderSizeMax(int length)
  {
    _headerSizeMax = length;
  }
  */

  /**
   * Gets the header-size-max
   */
  /*
  public int getHeaderSizeMax()
  {
    return _headerSizeMax;
  }
  */

  /**
   * Sets the header-size-max
   */
  /*
  public void setHeaderCountMax(int length)
  {
    _headerCountMax = length;
  }
  */

  /**
   * Gets the header-count-max
   */
  /*
  public int getHeaderCountMax()
  {
    return _headerCountMax;
  }
  */

  /**
   * Adds a WebAppDefault.
   */
  public void addWebAppDefault(WebAppConfig init)
  {
    _hostContainer.addWebAppDefault(init);
  }

  /**
   * Adds a HostDefault.
   */
  public void addHostDefault(HostConfig hostDefault)
  {
    _hostContainer.addHostDefault(hostDefault);
  }

  /**
   * Adds a HostDeploy.
   */
  public DeployGeneratorHostExpand createHostDeploy()
  {
    return _hostContainer.createHostDeploy();
  }

  /**
   * Adds a HostDeploy.
   */
  public void addHostDeploy(DeployGeneratorHostExpand deploy)
  {
    _hostContainer.addHostDeploy(deploy);
  }

  /**
   * Adds the host.
   */
  public void addHost(HostConfig host)
  {
    _hostContainer.addHost(host);
  }

  /**
   * Adds a pod-default
   */
  /*
  public void addPodDefault(PodConfig init)
  {
    _podContainer.addPodDefault(init);
  }
  */

  public HttpCacheBase getHttpCache()
  {
    return _httpCache;
  }

  /**
   * Creates the http cache.
   */
  public final HttpCacheBase createProxyCache()
    throws ConfigException
  {
    return createHttpCache();
  }

  /**
   * Creates the http cache.
   */
  public final HttpCacheBase createCache()
    throws ConfigException
  {
    return createHttpCache();
  }

  /**
   * Creates the http cache.
   */
  public final HttpCacheBase createHttpCache()
    throws ConfigException
  {
    return _httpCache;
  }
  
  /**
   * Returns true if sendfile is enabled.
   */
  /*
  public void setSendfileEnable(boolean isEnable)
  {
    _isSendfileEnabled = isEnable;
  }
  */
  
  /**
   * Returns true if sendfile is enabled.
   */
  /*
  public boolean isSendfileEnable()
  {
    return _isSendfileEnabled;
  }
  
  public long getSendfileMinLength()
  {
    return _sendfileMinLength;
  }
  
  public void setSendfileMinLength(long bytes)
  {
    _sendfileMinLength = bytes;
  }
  
  public long getSendfileCount()
  {
    return _sendfileCount.get();
  }
  
  public void addSendfileCount()
  {
    _sendfileCount.incrementAndGet();
  }
  */

  @Override
  public RequestFacade createFacade(RequestHttpBase requestHttp)
  {
    return new RequestServlet(requestHttp);
  }

  /**
   * Sets the access log.
   */
  public void setAccessLog(AccessLogServlet log)
  {
    _accessLog = log;
    
    Environment.setAttribute("caucho.server.access-log", log);
  }

  /**
   * @return
   */
  public AccessLogBase getAccessLog()
  {
    return _accessLog;
  }

  public void setAccessLogBufferSize(Bytes bytes)
  {
    _accessLogBufferSize = (int) bytes.getBytes();
  }

  public int getAccessLogBufferSize()
  {
    if (_accessLogBufferSize > 0) {
      return _accessLogBufferSize;
    }
    else if (_accessLog != null) {
      return _accessLog.getBufferSize();
    }
    else {
      return 1024;
    }
  }

  /**
   * Returns the dependency check interval.
   */
  public long getDependencyCheckInterval()
  {
    return Environment.getDependencyCheckInterval(getClassLoader());
  }

  /**
   * Sets the session cookie
   */
  public void setSessionCookie(String cookie)
  {
    getInvocationDecoder().setSessionCookie(cookie);
  }

  /**
   * Gets the session cookie
   */
  public String getSessionCookie()
  {
    return getInvocationDecoder().getSessionCookie();
  }

  /**
   * Sets the ssl session cookie
   */
  public void setSSLSessionCookie(String cookie)
  {
    getInvocationDecoder().setSSLSessionCookie(cookie);
  }

  /**
   * Gets the ssl session cookie
   */
  public String getSSLSessionCookie()
  {
    return getInvocationDecoder().getSSLSessionCookie();
  }

  /**
   * Sets the session url prefix.
   */
  public void setSessionURLPrefix(String urlPrefix)
  {
    getInvocationDecoder().setSessionURLPrefix(urlPrefix);
  }

  /**
   * Gets the session url prefix.
   */
  public String getSessionURLPrefix()
  {
    return getInvocationDecoder().getSessionURLPrefix();
  }

  /**
   * Sets the alternate session url prefix.
   */
  public void setAlternateSessionURLPrefix(String urlPrefix)
    throws ConfigException
  {
    getInvocationDecoder().setAlternateSessionURLPrefix(urlPrefix);
  }

  /**
   * Gets the alternate session url prefix.
   */
  public String getAlternateSessionURLPrefix()
  {
    return getInvocationDecoder().getAlternateSessionURLPrefix();
  }

  /**
   * Sets URL encoding.
   */
  public void setURLCharacterEncoding(String encoding)
    throws ConfigException
  {
    getInvocationDecoder().setEncoding(encoding);
  }
  
  /*
  public String getURLCharacterEncoding()
  {
    return getInvocationDecoder().getEncoding();
  }
  
  public InvocationDecoder getInvocationDecoder()
  {
    return getInvocationManager().getInvocationDecoder();
  }
  
  public InvocationManager getInvocationManager()
  {
    return _invocationServer;
  }
  */

  /**
   * Adds an error page
   */
  public void addErrorPage(ErrorPage errorPage)
  {
    WebApp webApp = getErrorWebApp();
    
    if (webApp != null)
      webApp.addErrorPage(errorPage);
  }

  //
  // cluster server information
  //
  public int getServerIndex()
  {
    // return _selfServer.getServerIndex();
    return 0;
  }

  //
  // statistics
  //

  /**
   * Returns the time the server started in ms.
   */
  public long getStartTime()
  {
    return _startTime;
  }

  /**
   * Returns the lifecycle state
   */
  public String getState()
  {
    return _lifecycle.getStateName();
  }

  public double getCpuLoad()
  {
    return 0;
  }

  protected HttpAdmin createServerAdmin()
  {
    return new HttpContainerAdmin(this);
  }

  //
  // runtime operations
  //

  @Override 
  public InvocationServlet createInvocation()
  {
    return new InvocationServlet();
  }
  
  /**
   * Sets the invocation
   */
  @Override
  public InvocationServlet routeInvocation(InvocationServlet invocation)
    throws ConfigException
  {
    if (_configException != null) {
      invocation.setFilterChain(new FilterChainException(_configException));
      invocation.setWebApp(getErrorWebApp());
      invocation.setDependency(Dependency.alwaysModified()); // AlwaysModified.create());

      return invocation;
    }
    else if (_lifecycle.waitForActive(_waitForActiveTime)) {
      return _hostContainer.routeInvocation(invocation);
    }
    else {
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

      invocation.setFilterChain(new FilterChainError(code));
      invocation.setWebApp(getErrorWebApp());
      invocation.setDependency(AlwaysModified.create());

      return invocation;
    }
  }

  /**
   * Returns the admin.
   */
  public ServerMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Returns the admin for the current environment.
   */
  public EnvironmentMXBean getEnvironmentAdmin()
  {
    return getClassLoader().getAdmin();
  }

  /**
   * Returns the default web-app or error web-app for top-level errors
   */
  public WebApp getDefaultWebApp()
  {
    WebApp webApp = getWebApp("", 80, "");

    if (webApp != null)
      return webApp;
    else
      return getErrorWebApp();
  }

  /**
   * Returns the matching web-app for a URL.
   */
  public WebApp getWebApp(String hostName, int port, String url)
  {
    try {
      HostContainer hostContainer = _hostContainer;

      if (hostContainer == null)
        return null;

      Host host = hostContainer.getHost(hostName, port);

      if (host == null)
        return null;

      return host.getWebAppContainer().findWebAppByURI(url);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  public WebAppController findWebAppController(String id)
  {
    for (DeployHandle<Host> hostHandle : getHostHandles()) {
      Host host = hostHandle.getDeployInstance();
      
      if (host == null) {
        continue;
      }
      
      for (WebAppController webAppCtrl : host.getWebAppContainer().getWebAppList()) {
        if (webAppCtrl.getId().equals(id)) {
          return webAppCtrl;
        }
      }
    }
    
    return null;
  }

  public DeployHandle<WebApp> findWebAppHandle(String id)
  {
    for (DeployHandle<Host> hostHandle : getHostHandles()) {
      Host host = hostHandle.getDeployInstance();
      
      if (host == null) {
        continue;
      }
      
      for (DeployHandle<WebApp> webAppCtrl : host.getWebAppContainer().getWebAppHandles()) {
        if (webAppCtrl.getId().equals(id)) {
          return webAppCtrl;
        }
      }
    }
    
    return null;
  }

  /**
   * Returns the error webApp during startup.
   */
  public WebApp getErrorWebApp()
  {
    if (isActive())
      return _hostContainer.getErrorWebApp();
    else
      return null;
  }
  
  public ErrorPageManager getErrorPageManager()
  {
    WebApp errorWebApp = getErrorWebApp();
    
    if (errorWebApp != null)
      return errorWebApp.getErrorPageManager();
    else
      return _errorPageManager;
  }

  /**
   * Returns the host controllers.
   */
  public DeployHandle<Host> []getHostHandles()
  {
    HostContainer hostContainer = _hostContainer;

    if (hostContainer == null)
      return new DeployHandle[0];

    return hostContainer.getHostHandles();
  }

  /**
   * Returns the matching servlet pattern for a URL.
   */
  public Host getHost(String hostName, int port)
  {
    try {
      return _hostContainer.getHost(hostName, port);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the matching servlet pattern for a URL.
   */
  public DeployHandle<Host> getHostHandle(String hostName, int port)
  {
    try {
      return _hostContainer.getHostHandle(hostName, port);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /*
  public PodAppHandle getPodApp(String podNodeName)
  {
    return _podContainer.getPodAppHandle(podNodeName);
  }
  */

  public HttpBufferStore allocateHttpBuffer()
  {
    HttpBufferStore buffer = _httpBufferFreeList.allocate();

    if (buffer == null) {
      buffer = new HttpBufferStore(getUrlLengthMax(),
                                   getHeaderSizeMax(),
                                   getHeaderCountMax());
    }

    return buffer;
  }

  public void freeHttpBuffer(HttpBufferStore buffer)
  {
    _httpBufferFreeList.free(buffer);
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
  {
    if (! _lifecycle.toInit())
      return;

    _admin = new HttpAdmin(this);
  }
  
  /**
   * Start the server.
   */
  @Override
  public void startImpl()
  {
    super.startImpl();
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStarting()) {
        return;
      }

      _startTime = CurrentTime.getCurrentTime();

      _lifecycle.toStarting();
      
      _httpCache.start();
      
      // _podContainer.start();
      _hostContainer.start();

      // getCluster().startRemote();

      _alarm.queue(ALARM_INTERVAL);

      _lifecycle.toActive();

      // logModules();

      AuthenticatorRole adminAuth = getAdminAuthenticator();
      if (adminAuth != null) {
        // XXX: adminAuth.init();
      }
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      throw e;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      // if the server can't start, it needs to completely fail, especially
      // for the watchdog
      throw new RuntimeException(e);

      // log.log(Level.WARNING, e.toString(), e);

      // _configException = e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Handles the alarm.
   */
  @Override
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;

    try {
      if (isModified()) {
        // XXX: message slightly wrong
        String msg = L.l("Resin restarting due to configuration change");
        ShutdownSystem.getCurrent().shutdown(ShutdownModeAmp.GRACEFUL,
                                             ExitCode.MODIFIED, 
                                             msg);
        return;
      }
    } finally {
      if (_lifecycle.isActive()) {
        alarm.queue(ALARM_INTERVAL);
      }
    }
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  @Override
  public boolean isModified()
  {
    DynamicClassLoader classLoader = getClassLoader();
    
    if (classLoader != null)
      return classLoader.isModified();
    else
      return true;
  }
  
  @Override
  public boolean logModified(Logger log)
  {
    DynamicClassLoader classLoader = getClassLoader();
    
    if (classLoader != null)
      return classLoader.logModified(log);
    else {
      log.info(this + " is closed");

      return true;
    }
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModifiedNow()
  {
    DynamicClassLoader classLoader = getClassLoader();
    
    if (classLoader != null)
      return classLoader.isModifiedNow();
    else
      return true;
  }

  /**
   * Returns true if the server is starting or active
   */
  public boolean isAfterStarting()
  {
    return _lifecycle.getState().isAfterStarting();
  }

  /**
   * Returns true if the server is stopped.
   */
  public boolean isStopping()
  {
    return _lifecycle.isStopping();
  }

  /**
   * Returns true if the server is stopped.
   */
  public boolean isStopped()
  {
    return _lifecycle.isStopped();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isDestroying()
  {
    return _lifecycle.isDestroying();
  }

  /**
   * Returns true if the server is currently active and accepting requests
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  public WebAppBuilder createWebAppBuilder(WebAppController controller)
  {
    return new WebAppBuilder(controller);
  }

  /**
   * Clears the proxy cache.
   */
  public void clearCache()
  {
    // skip the clear on restart
    if (isStopping()) {
      return;
    }

    if (log.isLoggable(Level.FINER)) {
      log.finest("clearCache");
    }

    // the invocation cache must be cleared first because the old
    // filter chain entries must not point to the cache's
    // soon-to-be-invalid entries
    getInvocationManager().clearCache();

    if (_httpCache != null) {
      _httpCache.clear();
    }
  }

  /**
   * Returns the proxy cache hit count.
   */
  public long getHttpCacheHitCount()
  {
    if (_httpCache != null)
      return _httpCache.getHitCount();
    else
      return 0;
  }

  /**
   * Returns the proxy cache miss count.
   */
  public long getHttpCacheMissCount()
  {
    if (_httpCache != null)
      return _httpCache.getMissCount();
    else
      return 0;
  }

  public void sendRequestError(Throwable e, 
                               RequestFacade requestFacade,
                               ResponseFacade responseFacade)
     throws ClientDisconnectException
  {
    RequestServlet req = (RequestServlet) requestFacade;
    ResponseServlet res = (ResponseServlet) responseFacade;
    
    try {
      ErrorPageManager errorManager = getErrorPageManager();
      
      if (errorManager != null) {
        errorManager.sendServletError(e, req, res);
      }
      else {
        res.sendError(503);
      }
    } catch (ClientDisconnectException e1) {
      throw e1;
    } catch (Throwable e1) {
      log.log(Level.FINE, e1.toString(), e1);
    }

    WebApp webApp = getDefaultWebApp();

    if (webApp != null && req != null) {
      webApp.accessLog(req, res);
    }
  }

  public void restart()
  {
    String msg = L.l("Server restarting due to configuration change");
    
    ShutdownSystem.shutdownActive(ExitCode.MODIFIED, msg);
  }
  
  @Override
  public void shutdown(ShutdownModeAmp mode)
  {
    super.shutdown(mode);
    
    stop(mode);
    destroy(mode);
  }
  
  /**
   * Closes the server.
   * @param mode 
   */
  public void stop(ShutdownModeAmp mode)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStopping()) {
        return;
      }
      
      // getInvocationServer().stop();
      // notify other servers that we've stopped
      notifyStop();

      Alarm alarm = _alarm;
      _alarm = null;

      if (alarm != null) {
        alarm.dequeue();
      }
      
      _httpCache.close();

      try {
        if (_hostContainer != null) {
          _hostContainer.stop(mode);
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      /*
      try {
        _podContainer.stop(mode);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      */

      try {
        getClassLoader().stop();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        ThreadPool.getThreadPool().clearIdleThreads();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        Thread.sleep(1);
      } catch (Throwable e) {
      }

      _lifecycle.toStop();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Notifications to cluster servers that we've stopped.
   */
  protected void notifyStop()
  {
  }

  /**
   * Closes the server.
   */
  public void destroy(ShutdownModeAmp mode)
  {
    stop(mode);

    if (! _lifecycle.toDestroy()) {
      return;
    }
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      try {
        _hostContainer.destroy(mode);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      log.fine(this + " destroyed");

      // getInvocationServer().destroy();
      // getClassLoader().destroy();

      _httpCache = null;
      // _invocationServer = null;
      
      //_podContainer.destroy();
    } finally {
      DynamicClassLoader.setOldLoader(thread, oldLoader);
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[id=" + getServerDisplayName()
            + ",cluster=" + getSelfServer().getCluster().getId() + "]");
  }

  /*
  @Override
  public boolean isSendfileEnabled()
  {
    // TODO Auto-generated method stub
    return false;
  }
  */
}
