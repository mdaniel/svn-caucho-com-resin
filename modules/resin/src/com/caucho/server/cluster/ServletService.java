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

package com.caucho.server.cluster;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import com.caucho.VersionFactory;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.manager.BamManager;
import com.caucho.bam.stream.MessageStream;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.distcache.ClusterCache;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownSystem;
import com.caucho.env.thread.ThreadPool;
import com.caucho.hemp.broker.HempBrokerManager;
import com.caucho.hemp.servlet.ServerAuthManager;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.AlwaysModified;
import com.caucho.management.server.EnvironmentMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.rewrite.DispatchRule;
import com.caucho.security.AdminAuthenticator;
import com.caucho.security.PermissionManager;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationBuilder;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.dispatch.InvocationMatcher;
import com.caucho.server.dispatch.InvocationServer;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.PersistentStoreConfig;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.host.Host;
import com.caucho.server.host.HostConfig;
import com.caucho.server.host.HostContainer;
import com.caucho.server.host.HostController;
import com.caucho.server.host.HostExpandDeployGenerator;
import com.caucho.server.http.HttpBufferStore;
import com.caucho.server.httpcache.AbstractProxyCache;
import com.caucho.server.log.AbstractAccessLog;
import com.caucho.server.log.AccessLog;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.webapp.ErrorPage;
import com.caucho.server.webapp.ErrorPageManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CurrentTime;
import com.caucho.util.FreeRing;
import com.caucho.util.L10N;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.Vfs;

public class ServletService
  implements AlarmListener, ClassLoaderListener, InvocationBuilder, Dependency
{
  private static final L10N L = new L10N(ServletService.class);
  private static final Logger log
    = Logger.getLogger(ServletService.class.getName());

  private static final long ALARM_INTERVAL = 60000;

  private static final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  private static final EnvironmentLocal<ServletService> _serverLocal
    = new EnvironmentLocal<ServletService>();

  private final Resin _resin;
  private final ResinSystem _resinSystem;
  private final CloudServer _selfServer;

  private Throwable _configException;

  private ServerAuthManager _authManager;
  private AdminAuthenticator _adminAuth;

  private InjectManager _cdiManager;

  private BamSystem _bamService;

  private InvocationServer _invocationServer;
  private HostContainer _hostContainer;
  
  private ErrorPageManager _errorPageManager = new ErrorPageManager(this);

  private String _stage = "production";
  private boolean _isPreview;
  private boolean _isEnabled = true;

  private String _serverHeader;

  private int _urlLengthMax = 8192;
  private int _headerSizeMax = TempBuffer.isSmallmem() ? 4 * 1024 : 16 * 1024;
  private int _headerCountMax = TempBuffer.isSmallmem() ? 32 : 256;

  private long _waitForActiveTime = 10000L;

  private boolean _isDevelopmentModeErrorPage;

  private long _shutdownWaitMax = ShutdownSystem.shutdownWaitMax;//60 * 1000;
  
  private boolean _isIgnoreClientDisconnect = true;

  // <cluster> configuration

  private String _connectionErrorPage;

  private ServerAdmin _admin;

  private Alarm _alarm;
  private AbstractProxyCache _proxyCache;

  private PersistentStoreConfig _persistentStoreConfig;
  
  private boolean _isSendfileEnabled = true;
  // private long _sendfileMinLength = 128 * 1024L;
  private long _sendfileMinLength = 32 * 1024L;
  
  private final FreeRing<HttpBufferStore> _httpBufferFreeList
    = new FreeRing<HttpBufferStore>(256);

  
  //
  // internal databases
  //

  // reliable system store
  private ClusterCache _systemStore;
  // private GlobalCache _globalStore;

  // stats
  
  private final AtomicLong _sendfileCount = new AtomicLong();

  private long _startTime;

  private final Lifecycle _lifecycle;
  private AccessLog _accessLog;
  private int _accessLogBufferSize;
  private boolean _isErrorPageServerId = true;

  /**
   * Creates a new servlet server.
   */
  public ServletService(Resin resin)
  {
    if (resin == null)
      throw new NullPointerException();
    
    ResinSystem resinSystem = resin.getResinSystem();
    
    if (resinSystem == null)
      throw new NullPointerException();
    
    _resin = resin;
    _resinSystem = resinSystem;
    
    _invocationServer = new InvocationServer(this);
    
    _selfServer = resin.getSelfServer();

    // pod id can't include the server since it's used as part of
    // cache ids
    //String podId
    //  = (cluster.getId() + ":" + _selfServer.getClusterPod().getId());

    String id = _selfServer.getId();
    
    if ("".equals(id)) {
      throw new IllegalStateException();
    }
    
    // cannot set the based on server-id because of distributed cache
    // _classLoader.setId("server:" + id);

    _serverLocal.set(this, getClassLoader());

    if (! CurrentTime.isTest())
      _serverHeader = "Resin/" + VersionFactory.getVersion();
    else
      _serverHeader = "Resin/1.1";

    try {
      Thread thread = Thread.currentThread();

      Environment.addClassLoaderListener(this, getClassLoader());

      PermissionManager permissionManager = new PermissionManager();
      PermissionManager.setPermissionManager(permissionManager);

      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(getClassLoader());

        _serverIdLocal.set(_selfServer.getId());
        
        _lifecycle = new Lifecycle(log, toString(), Level.INFO);
        
        preInit();
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      // exceptions here must throw to the top because they're non-recoverable
      throw ConfigException.create(e);
    }
  }
  
  protected void preInit()
  {
    _cdiManager = InjectManager.create();
    
    _hostContainer = new HostContainer(this);

    _alarm = new Alarm(this);
    
    _bamService = BamSystem.getCurrent();

    _authManager = new ServerAuthManager();
    // XXX:
    _authManager.setAuthenticationRequired(false);
    _bamService.setLinkManager(_authManager);

    // _resinSystem.addService(new DeployUpdateService());

    // _selfServer.getServerProgram().configure(this);
  }

  /**
   * Returns the current server
   */
  public static ServletService getCurrent()
  {
    return _serverLocal.get();
  }
  
  public ResinSystem getResinSystem()
  {
    return _resinSystem;
  }
  
  public boolean isResinServer()
  {
    if (_resin != null)
      return _resin.isResinServer();
    else
      return false;
  }

  public String getUniqueServerName()
  {
    return _resin.getUniqueServerName();
  }

  /**
   * Returns the classLoader
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _resinSystem.getClassLoader();
  }

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
  public void setConfigException(Throwable exn)
  {
    _configException = exn;
  }

  /**
   * Returns the resin server
   */
  public Resin getResin()
  {
    return _resin;
  }

  /**
   * Returns true for the watchdog server.
   */
  public boolean isWatchdog()
  {
    return getResin().isWatchdog();
  }

  /**
   * Returns the cluster
   */
  public CloudCluster getCluster()
  {
    return _selfServer.getCluster();
  }

  /**
   * Returns all the clusters
   */
  public CloudCluster []getClusterList()
  {
    return _selfServer.getCluster().getSystem().getClusterList();
  }

  /**
   * Returns the admin path
   */
  public Path getResinDataDirectory()
  {
    return _resin.getResinDataDirectory();
  }

  /**
   * Creates the bam broker manager
   */
  protected HempBrokerManager createBrokerManager()
  {
    return new HempBrokerManager(_resinSystem);
  }

  /**
   * Returns the cluster server
   */
  protected ClusterServer getClusterServer()
  {
    return getSelfServer();
  }

  /**
   * Returns the self server
   */
  public ClusterServer getSelfServer()
  {
    return _selfServer.getData(ClusterServer.class);
  }

  /**
   * Returns the self server's pod
   */
  public CloudPod getPod()
  {
    return _selfServer.getPod();
  }

  public BamManager getBamManager()
  {
    return _bamService.getBamManager();
  }
  
  /**
   * Returns the bam broker.
   */
  public ManagedBroker getBamBroker()
  {
    return _bamService.getBroker();
  }

  /**
   * Returns the bam broker.
   */
  public ManagedBroker getAdminBroker()
  {
    return getBamBroker();
  }

  /**
   * Returns the bam broker.
   */
  public BamManager getAdminBrokerManager()
  {
    return _bamService.getBamManager();
  }
  
  /**
   * Creates a bam client to the admin.
   */
  public ActorSender createAdminClient(String uid)
  {
    return createAdminClient(uid, null);
  }
  
  /**
   * Creates a bam client to the admin.
   */
  public ActorSender createAdminClient(String uid, String resource)
  {
    return getAdminBrokerManager().createClient(uid, resource);
  }

  /**
   * Returns the bam name.
   */
  public String getBamAdminName()
  {
    return getClusterServer().getBamAdminName();
  }

  /**
   * Returns the admin broker.
   */
  public Broker getBroker()
  {
    return _bamService.getBroker();
  }

  public AdminAuthenticator getAdminAuthenticator()
  {
    if (_adminAuth == null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(getClassLoader());

        _adminAuth = _cdiManager.getReference(AdminAuthenticator.class);

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

        _adminAuth = new AdminAuthenticator();
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
   * Development mode error pages.
   */
  public boolean isErrorPageServerId()
  {
    return _isErrorPageServerId;
  }

  /**
   * Development mode error pages.
   */
  public void setErrorPageServerId(boolean isEnable)
  {
    _isErrorPageServerId  = isEnable;
  }

  /**
   * Sets the stage id
   */
  public void setStage(String stage)
  {
    if (stage == null || "".equals(stage))
      _stage = "production";
    else
      _stage = stage;

    _isPreview = "preview".equals(_stage);
  }

  /**
   * Returns the stage id
   */
  public String getStage()
  {
    return _stage;
  }

  /**
   * Returns true in preview mode
   */
  public boolean isPreview()
  {
    return _isPreview;
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
  public String getServerId()
  {
    return _selfServer.getId();
  }

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
  public void setUrlLengthMax(int length)
  {
    _urlLengthMax = length;

    getInvocationDecoder().setMaxURILength(length);
  }

  /**
   * Gets the url-length-max
   */
  public int getUrlLengthMax()
  {
    return _urlLengthMax;
  }

  /**
   * Sets the header-size-max
   */
  public void setHeaderSizeMax(int length)
  {
    _headerSizeMax = length;
  }

  /**
   * Gets the header-size-max
   */
  public int getHeaderSizeMax()
  {
    return _headerSizeMax;
  }

  /**
   * Sets the header-size-max
   */
  public void setHeaderCountMax(int length)
  {
    _headerCountMax = length;
  }

  /**
   * Gets the header-count-max
   */
  public int getHeaderCountMax()
  {
    return _headerCountMax;
  }

  /**
   * Adds a WebAppDefault.
   */
  public void addWebAppDefault(WebAppConfig init)
  {
    _hostContainer.addWebAppDefault(init);
  }

  /**
   * Adds an EarDefault
   */
  public void addEarDefault(EarConfig config)
  {
    _hostContainer.addEarDefault(config);
  }

  /**
   * Adds a HostDefault.
   */
  public void addHostDefault(HostConfig init)
  {
    _hostContainer.addHostDefault(init);
  }

  /**
   * Adds a HostDeploy.
   */
  public HostExpandDeployGenerator createHostDeploy()
  {
    return _hostContainer.createHostDeploy();
  }

  /**
   * Adds a HostDeploy.
   */
  public void addHostDeploy(HostExpandDeployGenerator deploy)
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
   * Adds rewrite-dispatch.
   */
  public RewriteDispatch createRewriteDispatch()
  {
    return _hostContainer.createRewriteDispatch();
  }
  
  public void add(DispatchRule rewriteRule)
  {
    createRewriteDispatch().addRule(rewriteRule);
  }

  public AbstractProxyCache getProxyCache()
  {
    return _proxyCache;
  }

  /**
   * Creates the http cache.
   */
  public final AbstractProxyCache createProxyCache()
    throws ConfigException
  {
    if (_proxyCache == null) {
      _proxyCache = instantiateProxyCache();
    }

    return _proxyCache;
  }
  
  protected AbstractProxyCache instantiateProxyCache()
  {
    log.warning(L.l("<proxy-cache> requires Resin Professional.  Please see http://www.caucho.com for Resin Professional information and licensing."));

    return new AbstractProxyCache();
  }
  
  /**
   * Returns true if sendfile is enabled.
   */
  public void setSendfileEnable(boolean isEnable)
  {
    _isSendfileEnabled = isEnable;
  }
  
  /**
   * Returns true if sendfile is enabled.
   */
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

  /**
   * Sets the access log.
   */
  public void setAccessLog(AccessLog log)
  {
    _accessLog = log;
    
    Environment.setAttribute("caucho.server.access-log", log);
  }

  /**
   * @return
   */
  public AbstractAccessLog getAccessLog()
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
  
  public String getURLCharacterEncoding()
  {
    return getInvocationDecoder().getEncoding();
  }
  
  public InvocationDecoder getInvocationDecoder()
  {
    return getInvocationServer().getInvocationDecoder();
  }
  
  public InvocationServer getInvocationServer()
  {
    return _invocationServer;
  }

  /**
   * Adds an error page
   */
  public void addErrorPage(ErrorPage errorPage)
  {
    WebApp webApp = getErrorWebApp();
    
    if (webApp != null)
      webApp.addErrorPage(errorPage);
  }
  
  public void setPersistentStore(PersistentStoreConfig config)
  {
    _persistentStoreConfig = config;
  }
  
  public PersistentStoreConfig getPersistentStore()
  {
    return _persistentStoreConfig;
  }

  //
  // cluster server information
  //
  public int getServerIndex()
  {
    return _selfServer.getIndex();
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

  //
  // runtime operations
  //

  /**
   * Sets the invocation
   */
  @Override
  public Invocation buildInvocation(Invocation invocation)
    throws ConfigException
  {
    if (_configException != null) {
      invocation.setFilterChain(new ExceptionFilterChain(_configException));
      invocation.setWebApp(getErrorWebApp());
      invocation.setDependency(AlwaysModified.create());

      return invocation;
    }
    else if (_lifecycle.waitForActive(_waitForActiveTime)) {
      return _hostContainer.buildInvocation(invocation);
    }
    else {
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

      invocation.setFilterChain(new ErrorFilterChain(code));
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
    
    if (errorWebApp != null) {
      return errorWebApp.getErrorPageManager();
    }
    else {
      return _errorPageManager;
    }
  }

  /**
   * Returns the host controllers.
   */
  public HostController []getHostControllers()
  {
    HostContainer hostContainer = _hostContainer;

    if (hostContainer == null)
      return new HostController[0];

    return hostContainer.getHostList();
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
  public HostController getHostController(String hostName, int port)
  {
    try {
      return _hostContainer.getHostController(hostName, port);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the reliable system store
   */
  public ClusterCache getSystemStore()
  {
    synchronized (this) {
      if (_systemStore == null) {
        _systemStore = new ClusterCache();
        _systemStore.setName("resin:system");
        _systemStore.setManagerName("resin");
        _systemStore.setModifiedExpireTimeoutMillis(CacheConfig.TIME_INFINITY);
        _systemStore.setAccessedExpireTimeoutMillis(CacheConfig.TIME_INFINITY);
        _systemStore.setLocalExpireTimeoutMillis(10);
        // XXX: need to set reliability values
      }
    }

    _systemStore.init();

    return _systemStore;
  }

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
   * Returns the reliable system store
   */
  /*
  public GlobalCache getGlobalStore()
  {
    synchronized (this) {
      if (_globalStore == null) {
        _globalStore = new GlobalCache();
        _globalStore.setName("resin:global");
        _globalStore.setGuid("resin:global");
        _globalStore.setLocalReadTimeoutMillis(60000);
        // XXX: need to set reliability values
      }
    }

    _globalStore.init();

    return _globalStore;
  }
  */

  /**
   * Handles the case where a class loader is activated.
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
    try {
      //Jmx.register(_controller.getThreadPool(), "resin:type=ThreadPool");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    /*
    try {
      Jmx.unregister("resin:name=default,type=Server");
      Jmx.unregister("resin:type=ThreadPool");
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }
    */
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
  {
    if (! _lifecycle.toInit())
      return;
    
    // _classLoader.init();
    
    if (_proxyCache != null
        && _invocationServer.getInvocationCacheSize() < _proxyCache.getEntries()) {
      _invocationServer.setInvocationCacheSize(_proxyCache.getEntries());
    }
    
    _invocationServer.init();

    _admin = createAdmin();

    /*
    if (_resin != null) {
      createManagement().setCluster(getCluster());
      createManagement().setServer(this);
      createManagement().init();
    }
    
    if (_selfServer.getStage() != null)
      setStage(_selfServer.getStage());
    */
  }
  
  protected ServerAdmin createAdmin()
  {
    return new ServerAdmin(this);
  }
  
  /**
   * Start the server.
   */
  public void start()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStarting())
        return;

      _startTime = CurrentTime.getCurrentTime();

      if (! CurrentTime.isTest()) {
        /*
        log.info("");

        log.info(System.getProperty("os.name")
                 + " " + System.getProperty("os.version")
                 + " " + System.getProperty("os.arch"));

        log.info(System.getProperty("java.runtime.name")
                 + " " + System.getProperty("java.runtime.version")
                 + ", " + System.getProperty("file.encoding")
                 + ", " + System.getProperty("user.language"));

        log.info(System.getProperty("java.vm.name")
                 + " " + System.getProperty("java.vm.version")
                 + ", " + System.getProperty("sun.arch.data.model")
                 + ", " + System.getProperty("java.vm.info")
                 + ", " + System.getProperty("java.vm.vendor"));
                 */

        log.info("");

        Resin resin = Resin.getCurrent();

        if (resin != null) {
          log.info("resin.home = " + resin.getResinHome().getNativePath());
          log.info("resin.root = " + resin.getRootDirectory().getNativePath());
          if (resin.getResinConf() != null)
            log.info("resin.conf = " + resin.getResinConf().getNativePath());

          log.info("");

          String serverType;

          if (resin.isWatchdog())
            serverType = "watchdog";
          else
            serverType = "server";

          log.info(serverType + "    = "
                   + _selfServer.getAddress()
                   + ":" + _selfServer.getPort()
                   + " (" + getCluster().getId()
                   + ":" + getServerId() + ")");
        }
        else {
          log.info("resin.home = " + System.getProperty("resin.home"));
        }

        // log.info("user.name  = " + System.getProperty("user.name"));
        log.info("stage      = " + _stage);
      }

      _lifecycle.toStarting();

      // initialize the system distributed store
      if (isResinServer()) {
        getSystemStore();
      }
      
      _hostContainer.start();

      // getCluster().startRemote();

      _alarm.queue(ALARM_INTERVAL);

      _lifecycle.toActive();

      logModules();

      AdminAuthenticator adminAuth = getAdminAuthenticator();
      if (adminAuth != null)
        adminAuth.init();
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      throw e;
    } catch (Exception e) {
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

  protected void startImpl()
  {  
  }
  
  /**
   * Display activated modules
   */
  protected void logModules()
  {
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
        ShutdownSystem.getCurrent().shutdown(ExitCode.MODIFIED, msg);
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

  /**
   * Clears the catch by matching the invocation.
   */
  public void clearCacheByPattern(String hostPattern, String uriPattern)
  {
    final Matcher hostMatcher;
    if (hostPattern != null)
      hostMatcher = Pattern.compile(hostPattern).matcher("");
    else
      hostMatcher = null;

    final Matcher uriMatcher;
    if (uriPattern != null)
      uriMatcher = Pattern.compile(uriPattern).matcher("");
    else
      uriMatcher = null;

    InvocationMatcher matcher = new InvocationMatcher() {
      public boolean isMatch(Invocation invocation)
      {
        if (hostMatcher != null) {
          hostMatcher.reset(invocation.getHost());
          if (! hostMatcher.find()) {
            return false;
          }
        }

        if (uriMatcher != null) {
          uriMatcher.reset(invocation.getURI());
          if (! uriMatcher.find()) {
            return false;
          }
        }

        return true;
      }
    };

    getInvocationServer().invalidateMatchingInvocations(matcher);
  }

  /**
   * Clears the proxy cache.
   */
  public void clearCache()
  {
    // skip the clear on restart
    if (isStopping())
      return;

    if (log.isLoggable(Level.FINER))
      log.finest("ServletServer clearCache");

    // the invocation cache must be cleared first because the old
    // filter chain entries must not point to the cache's
    // soon-to-be-invalid entries
    getInvocationServer().clearCache();

    if (_proxyCache != null) {
      _proxyCache.clear();
    }
  }

  /**
   * Returns the proxy cache hit count.
   */
  public long getProxyCacheHitCount()
  {
    if (_proxyCache != null)
      return _proxyCache.getHitCount();
    else
      return 0;
  }

  /**
   * Returns the proxy cache miss count.
   */
  public long getProxyCacheMissCount()
  {
    if (_proxyCache != null)
      return _proxyCache.getMissCount();
    else
      return 0;
  }

  /**
   * Returns any HMTP stream
   */
  public MessageStream getHmtpStream()
  {
    return null;
  }

  public void restart()
  {
    String msg = L.l("Server restarting due to configuration change");
    
    ShutdownSystem.shutdownActive(ExitCode.MODIFIED, msg);
  }
  /**
   * Closes the server.
   */
  public void stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStopping())
        return;

      // getInvocationServer().stop();
      // notify other servers that we've stopped
      notifyStop();

      Alarm alarm = _alarm;
      _alarm = null;

      if (alarm != null)
        alarm.dequeue();

      try {
        if (_systemStore != null)
          _systemStore.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      /*
      try {
        if (_globalStore != null)
          _globalStore.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      */
      
      /*
      try {
        if (_domainManager != null)
          _domainManager.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      */

      try {
        ThreadPool.getThreadPool().clearIdleThreads();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        Thread.sleep(1);
      } catch (Throwable e) {
      }

      try {
        if (_hostContainer != null)
          _hostContainer.stop();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        getClassLoader().stop();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
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
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      try {
        _hostContainer.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      log.fine(this + " destroyed");

      getInvocationServer().destroy();
      // getClassLoader().destroy();

      _proxyCache = null;
    } finally {
      DynamicClassLoader.setOldLoader(thread, oldLoader);

      Resin resin = _resin;

      if (resin != null)
        resin.destroy();
    }
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[id=" + getServerId()
            + ",cluster=" + _selfServer.getCluster().getId() + "]");
  }
}
