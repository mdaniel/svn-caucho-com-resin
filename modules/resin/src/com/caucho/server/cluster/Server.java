/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import com.caucho.VersionFactory;
import com.caucho.bam.ActorClient;
import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.bam.SimpleActorClient;
import com.caucho.cloud.bam.BamService;
import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.network.NetworkClusterService;
import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.Period;
import com.caucho.distcache.ClusterCache;
import com.caucho.distcache.GlobalCache;
import com.caucho.env.deploy.DeployUpdateService;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownService;
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
import com.caucho.management.server.CacheItem;
import com.caucho.management.server.EnvironmentMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.security.AdminAuthenticator;
import com.caucho.security.PermissionManager;
import com.caucho.server.cache.AbstractCache;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationMatcher;
import com.caucho.server.dispatch.ProtocolDispatchServer;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.host.Host;
import com.caucho.server.host.HostConfig;
import com.caucho.server.host.HostContainer;
import com.caucho.server.host.HostController;
import com.caucho.server.host.HostExpandDeployGenerator;
import com.caucho.server.log.AccessLog;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.webapp.ErrorPage;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class Server extends ProtocolDispatchServer
  implements AlarmListener, ClassLoaderListener
{
  private static final L10N L = new L10N(Server.class);
  private static final Logger log
    = Logger.getLogger(Server.class.getName());

  private static final long ALARM_INTERVAL = 60000;

  private static final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  private static final EnvironmentLocal<Server> _serverLocal
    = new EnvironmentLocal<Server>();

  private final Resin _resin;
  private final ResinSystem _resinSystem;
  private final NetworkClusterService _clusterService;
  private final ClusterServer _selfServer;

  private Throwable _configException;

  private ServerAuthManager _authManager;
  private AdminAuthenticator _adminAuth;

  private InjectManager _cdiManager;

  private BamService _bamService;

  private HostContainer _hostContainer;

  private String _stage = "production";
  private boolean _isPreview;

  private String _serverHeader;

  private int _urlLengthMax = 8192;

  private long _waitForActiveTime = 10000L;

  private boolean _isDevelopmentModeErrorPage;

  private long _shutdownWaitMax = 60 * 1000;

  // <cluster> configuration

  private String _connectionErrorPage;

  private ServerAdmin _admin;

  private Alarm _alarm;
  private AbstractCache _cache;

  //
  // internal databases
  //

  // reliable system store
  private ClusterCache _systemStore;
  private GlobalCache _globalStore;

  // stats

  private long _startTime;

  private final Lifecycle _lifecycle;

  /**
   * Creates a new servlet server.
   */
  public Server(Resin resin,
                ResinSystem resinSystem,
                NetworkClusterService clusterService)
  {
    if (resin == null)
      throw new NullPointerException();
    
    if (resinSystem == null)
      throw new NullPointerException();
    
    if (clusterService == null)
      throw new NullPointerException();
    
    _resin = resin;
    _resinSystem = resinSystem;
    _clusterService = clusterService;
    
    
    _selfServer = _clusterService.getSelfServer().getData(ClusterServer.class);

    // pod id can't include the server since it's used as part of
    // cache ids
    //String podId
    //  = (cluster.getId() + ":" + _selfServer.getClusterPod().getId());

    String id = _selfServer.getId();
    
    if ("".equals(id))
      throw new IllegalStateException();
    
    // cannot set the based on server-id because of distributed cache
    // _classLoader.setId("server:" + id);

    _serverLocal.set(this, getClassLoader());

    if (! Alarm.isTest())
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
    
    _bamService = BamService.create(getBamAdminName());

    _authManager = new ServerAuthManager();
    // XXX:
    _authManager.setAuthenticationRequired(false);
    _bamService.setLinkManager(_authManager);

    _resinSystem.addService(new DeployUpdateService());

    // _selfServer.getServerProgram().configure(this);
  }

  /**
   * Returns the current server
   */
  public static Server getCurrent()
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
  @Override
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
    return new HempBrokerManager();
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
    return _selfServer;
  }

  /**
   * Returns the self server's pod
   */
  public CloudPod getPod()
  {
    return _selfServer.getCloudServer().getPod();
  }

  /**
   * Returns the bam broker.
   */
  public Broker getBamBroker()
  {
    return _bamService.getBroker();
  }

  /**
   * Returns the stream to the public broker.
   */
  public ActorStream getBamStream()
  {
    return getBamBroker().getBrokerStream();
  }

  /**
   * Returns the bam broker.
   */
  public Broker getAdminBroker()
  {
    return getBamBroker();
  }
  
  /**
   * Creates a bam client to the admin.
   */
  public ActorClient createAdminClient(String uid)
  {
    return new SimpleActorClient(getAdminBroker(), uid, null);
  }

  /**
   * Returns the stream to the admin broker.
   */
  public ActorStream getAdminStream()
  {
    return getAdminBroker().getBrokerStream();
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
      } catch (Exception e) {
        if (log.isLoggable(Level.FINEST))
          log.log(Level.FINEST, e.toString(), e);
        else
          log.finer(e.toString());

        _adminAuth = new AdminAuthenticator();
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
   * Returns the id.
   */
  @Override
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
  public void setUrlLengthMax(int max)
  {
    _urlLengthMax = max;
  }

  /**
   * Gets the url-length-max
   */
  public int getUrlLengthMax()
  {
    return _urlLengthMax;
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

  public AbstractCache getProxyCache()
  {
    return _cache;
  }

  /**
   * Creates the proxy cache.
   */
  public AbstractCache createProxyCache()
    throws ConfigException
  {
    if (_cache == null)
      _cache = instantiateProxyCache();

    return _cache;
  }
  
  protected AbstractCache instantiateProxyCache()
  {
    log.warning(L.l("<proxy-cache> requires Resin Professional.  Please see http://www.caucho.com for Resin Professional information and licensing."));

    return new AbstractCache();
  }

  /**
   * Sets the access log.
   */
  public void setAccessLog(AccessLog log)
  {
    Environment.setAttribute("caucho.server.access-log", log);
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

  /**
   * Adds an error page
   */
  public void addErrorPage(ErrorPage errorPage)
  {
    getErrorWebApp().addErrorPage(errorPage);
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

  /**
   * Returns the cache stuff.
   */
  public ArrayList<CacheItem> getCacheStatistics()
  {
    ArrayList<Invocation> invocationList = getInvocations();

    if (invocationList == null)
      return null;

    HashMap<String,CacheItem> itemMap = new HashMap<String,CacheItem>();

    for (int i = 0; i < invocationList.size(); i++) {
      Invocation inv = (Invocation) invocationList.get(i);

      String uri = inv.getURI();
      int p = uri.indexOf('?');
      if (p >= 0)
        uri = uri.substring(0, p);

      CacheItem item = itemMap.get(uri);

      if (item == null) {
        item = new CacheItem();
        item.setUrl(uri);

        itemMap.put(uri, item);
      }
    }

    return null;
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
   * Returns the matching servlet pattern for a URL.
   */
  public String getServletPattern(String hostName, int port, String url)
  {
    try {
      Host host = _hostContainer.getHost(hostName, port);

      if (host == null)
        return null;

      WebApp app = host.findWebAppByURI(url);

      if (app == null)
        return null;

      String pattern = app.getServletPattern(url);

      return pattern;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
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

      return host.findWebAppByURI(url);
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
    HostContainer hostContainer = _hostContainer;

    if (hostContainer != null)
      return hostContainer.getErrorWebApp();
    else
      return null;
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
   * Returns the reliable system store
   */
  public ClusterCache getSystemStore()
  {
    synchronized (this) {
      if (_systemStore == null) {
        _systemStore = new ClusterCache("resin:system");
        _systemStore.setGuid("resin:system");
        // XXX: need to set reliability values
      }
    }

    _systemStore.init();

    return _systemStore;
  }

  /**
   * Returns the reliable system store
   */
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

    super.init();

    _admin = createAdmin();

    /*
    if (_resin != null) {
      createManagement().setCluster(getCluster());
      createManagement().setServer(this);
      createManagement().init();
    }
    */
    
    if (_selfServer.getStage() != null)
      setStage(_selfServer.getStage());
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

      _startTime = Alarm.getCurrentTime();

      if (! Alarm.isTest()) {
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

      startImpl();

      if (_resin != null && _resin.getManagement() != null)
        _resin.getManagement().start(this);

      // initialize the system distributed store
      if (isResinServer())
        getSystemStore();

      // start the repository
      // AbstractRepository repository = getRepository();
      // if (repository != null)
      //   repository.start();

      // getCluster().start();

      // handled by the network server itself
      // _classLoader.start();

      _hostContainer.start();

      // getCluster().startRemote();

      _alarm.queue(ALARM_INTERVAL);

      _lifecycle.toActive();

      logModules();
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

        ShutdownService.getCurrent().shutdown(ExitCode.MODIFIED, msg);
        return;
      }
    } finally {
      alarm.queue(ALARM_INTERVAL);
    }
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  @Override
  public boolean isModified()
  {
    boolean isModified = getClassLoader().isModified();
    
    if (isModified)
      getClassLoader().logModified(log);

    return isModified;
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModifiedNow()
  {
    boolean isModified = getClassLoader().isModifiedNow();

    if (isModified)
      log.fine("server is modified");

    return isModified;
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
  @Override
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

    invalidateMatchingInvocations(matcher);
  }

  /**
   * Clears the proxy cache.
   */
  @Override
  public void clearCache()
  {
    // skip the clear on restart
    if (isStopping())
      return;

    if (log.isLoggable(Level.FINEST))
      log.finest("ServletServer clearCache");

    // the invocation cache must be cleared first because the old
    // filter chain entries must not point to the cache's
    // soon-to-be-invalid entries
    super.clearCache();

    if (_cache != null)
      _cache.clear();
  }

  /**
   * Returns the proxy cache hit count.
   */
  public long getProxyCacheHitCount()
  {
    if (_cache != null)
      return _cache.getHitCount();
    else
      return 0;
  }

  /**
   * Returns the proxy cache miss count.
   */
  public long getProxyCacheMissCount()
  {
    if (_cache != null)
      return _cache.getMissCount();
    else
      return 0;
  }

  /**
   * Returns any HMTP stream
   */
  public ActorStream getHmtpStream()
  {
    return null;
  }

  @Override
  public void restart()
  {
    String msg = L.l("Server restarting due to configuration change");
    
    ShutdownService.shutdownActive(ExitCode.MODIFIED, msg);
  }
  /**
   * Closes the server.
   */
  @Override
  public void stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStopping())
        return;

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

      try {
        if (_globalStore != null)
          _globalStore.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      
      /*
      try {
        if (_domainManager != null)
          _domainManager.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      */

      try {
        ThreadPool.getThreadPool().interrupt();
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

      super.stop();
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

      super.destroy();

      log.fine(this + " destroyed");

      // getClassLoader().destroy();

      _hostContainer = null;
      _cache = null;
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
