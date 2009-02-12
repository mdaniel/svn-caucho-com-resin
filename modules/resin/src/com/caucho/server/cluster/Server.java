/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.cluster.ClusterCache;
import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.git.GitRepository;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hemp.broker.HempBrokerManager;
import com.caucho.hemp.broker.DomainManager;
import com.caucho.hemp.servlet.ServerLinkManager;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.AlwaysModified;
import com.caucho.management.server.CacheItem;
import com.caucho.management.server.EnvironmentMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.security.PermissionManager;
import com.caucho.security.AdminAuthenticator;
import com.caucho.server.admin.Management;
import com.caucho.server.cache.AbstractCache;
import com.caucho.server.cache.TempFileManager;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationMatcher;
import com.caucho.server.distcache.DistributedCacheManager;
import com.caucho.server.distcache.FileCacheManager;
import com.caucho.server.distcache.PersistentStoreConfig;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.host.Host;
import com.caucho.server.host.HostConfig;
import com.caucho.server.host.HostContainer;
import com.caucho.server.host.HostController;
import com.caucho.server.host.HostExpandDeployGenerator;
import com.caucho.server.log.AccessLog;
import com.caucho.server.port.AbstractSelectManager;
import com.caucho.server.port.Port;
import com.caucho.server.port.ProtocolDispatchServer;
import com.caucho.server.repository.Repository;
import com.caucho.server.repository.FileRepository;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.webapp.ErrorPage;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server extends ProtocolDispatchServer
  implements EnvironmentBean, SchemaBean, AlarmListener,
             ClassLoaderListener
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
  private final ClusterServer _selfServer;

  private final String _id;
  
  private EnvironmentClassLoader _classLoader;

  private Throwable _configException;

  private HostContainer _hostContainer;

  private String _serverHeader;

  private AdminAuthenticator _adminAuth;

  private InjectManager _webBeans;
  
  private HempBrokerManager _brokerManager;
  private DomainManager _domainManager;
  private HempBroker _broker;
  private ServerLinkManager _serverLinkManager
    = new ServerLinkManager();

  private GitRepository _git;
  private Repository _repository;
  private FileRepository _localRepository;
  private PersistentStoreConfig _persistentStoreConfig;

  private DistributedCacheManager _distributedCacheManager;

  private int _urlLengthMax = 8192;

  private long _waitForActiveTime = 10000L;

  private boolean _isDevelopmentModeErrorPage;

  // <server> configuration compat
  private int _acceptListenBacklog = 100;
  
  private int _acceptThreadMin = 5;
  private int _acceptThreadMax = 10;

  private int _connectionMax = 1024 * 1024;

  // default is in Port
  private int _keepaliveMax = -1;
  
  private long _keepaliveConnectionTimeMax = 10 * 60 * 1000L;
  
  private boolean _keepaliveSelectEnable = true;
  private int _keepaliveSelectMax = -1;
  private long _keepaliveSelectThreadTimeout = 1000;

  private Management _management;
  
  private long _suspendTimeMax = 600000L;

  private long _memoryFreeMin = 1024 * 1024;
  private long _permGenFreeMin = 1024 * 1024;
  
  private long _shutdownWaitMax = 60 * 1000;
  
  private int _threadMax = 4096;
  private int _threadExecutorTaskMax = -1;
  private int _threadIdleMin = 5;
  private int _threadIdleMax = 10;

  // <cluster> configuration

  private String _connectionErrorPage;

  private ServerAdmin _admin;

  private Alarm _alarm;
  protected AbstractCache _cache;

  private boolean _isBindPortsAtEnd = true;
  private AtomicBoolean _isStartedPorts = new AtomicBoolean();

  //
  // internal databases
  //

  // reliable system store
  private ClusterCache _systemStore;

  // stats

  private long _startTime;

  private final Lifecycle _lifecycle;

  /**
   * Creates a new servlet server.
   */
  public Server(ClusterServer clusterServer)
  {
    if (clusterServer == null)
      throw new NullPointerException();

    _selfServer = clusterServer;
    Cluster cluster = clusterServer.getCluster();
    _resin = cluster.getResin();

    _id = cluster.getId() + ":" + clusterServer.getId();

    // triad id can't include the server since it's used as part of
    // cache ids
    String triadId
      = (cluster.getId() + ":" + _selfServer.getClusterTriad().getId());

    _classLoader = (EnvironmentClassLoader) cluster.getClassLoader();

    _classLoader.setId("server:" + triadId);

    _serverLocal.set(this, _classLoader);
    
    if (! Alarm.isTest())
      _serverHeader = "Resin/" + com.caucho.Version.VERSION;
    else
      _serverHeader = "Resin/1.1";
    
    try {
      Thread thread = Thread.currentThread();

      Environment.addClassLoaderListener(this, _classLoader);

      PermissionManager permissionManager = new PermissionManager();
      PermissionManager.setPermissionManager(permissionManager);

      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(_classLoader);
	
	_serverIdLocal.set(_selfServer.getId());

        _hostContainer = new HostContainer();
        _hostContainer.setClassLoader(_classLoader);
        _hostContainer.setDispatchServer(this);

	_admin = new ServerAdmin(this);

	_alarm = new Alarm(this);

	_webBeans = InjectManager.create();

	_brokerManager = createBrokerManager();
	_domainManager = createDomainManager();

	_broker = new HempBroker(getBamAdminName());
	
	_brokerManager.addBroker(getBamAdminName(), _broker);
	_brokerManager.addBroker("resin.caucho", _broker);

	// Config.setProperty("server", new ServerVar(server), _classLoader);
	
	_selfServer.getServerProgram().configure(this);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      _configException = e;
    } finally {
      _lifecycle = new Lifecycle(log, toString(), Level.INFO);
    }
  }

  /**
   * Returns the current server
   */
  public static Server getCurrent()
  {
    return _serverLocal.get();
  }

  /**
   * Returns the classLoader
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
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
   * Returns the cluster
   */
  public Cluster getCluster()
  {
    return _selfServer.getCluster();
  }

  /**
   * Returns the admin path
   */
  public Path getResinDataDirectory()
  {
    return _resin.getResinDataDirectory();
  }

  /**
   * Returns the HMTP link manager
   */
  public ServerLinkManager getServerLinkManager()
  {
    return _serverLinkManager;
  }

  /**
   * Returns the repository
   */
  public GitRepository getGit()
  {
    synchronized (this) {
      if (_git == null && _resin != null) {
	// initialize git repository
	Path root = _resin.getResinDataDirectory();

	// QA
	if (root instanceof MemoryPath)
	  root = Vfs.lookup("file:/tmp/caucho/qa");

	_git = new GitRepository(root.lookup(".git"));

	try {
	  _git.initDb();
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
      
      return _git;
    }
  }

  /**
   * Returns the deployment repository
   */
  public Repository getRepository()
  {
    synchronized (this) {
      if (_repository == null)
	_repository = createRepository();
    }

    _repository.init();

    return _repository;
  }

  /**
   * Returns the local repository
   */
  public FileRepository getLocalRepository()
  {
    synchronized (this) {
      if (_localRepository == null)
	_localRepository = new FileRepository(this);
    }

    return _localRepository;
  }

  /**
   * Creates a new deployment repository
   */
  protected Repository createRepository()
  {
    return getLocalRepository();
  }

  /**
   * Creates the bam domain manager
   */
  protected DomainManager createDomainManager()
  {
    return null;
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
   * Returns the self server's triad
   */
  public ClusterTriad getTriad()
  {
    return _selfServer.getClusterTriad();
  }

  /**
   * Returns the distributed cache manager
   */
  public DistributedCacheManager getDistributedCacheManager()
  {
    if (_distributedCacheManager == null)
      _distributedCacheManager = createDistributedCacheManager();

    return _distributedCacheManager;
  }

  /**
   * Returns the distributed cache manager
   */
  protected DistributedCacheManager createDistributedCacheManager()
  {
    return new FileCacheManager(this);
  }

  public TempFileManager getTempFileManager()
  {
    return _resin.getTempFileManager();
  }

  /**
   * Returns the bam broker.
   */
  public Broker getBamBroker()
  {
    return _broker;
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
    return _broker;
  }

  public String getAdminCookie()
  {
    AdminAuthenticator auth = getAdminAuthenticator();

    if (auth != null)
      return auth.getHash();
    else
      return null;
  }

  public AdminAuthenticator getAdminAuthenticator()
  {
    if (_adminAuth == null) {
      try {
	_adminAuth = (AdminAuthenticator) _webBeans.getInstanceByType(AdminAuthenticator.class);
      } catch (Exception e) {
	if (log.isLoggable(Level.FINEST))
	  log.log(Level.FINEST, e.toString(), e);
	else
	  log.finer(e.toString());

	_adminAuth = new AdminAuthenticator();
      }
    }
    
    return _adminAuth;
  }

  //
  // Configuration from <server>
  //

  /**
   * Sets the socket's listen property
   */
  public void setAcceptListenBacklog(int backlog)
  {
    _acceptListenBacklog = backlog;
  }

  /**
   * Gets the socket's listen property
   */
  public int getAcceptListenBacklog()
  {
    return _acceptListenBacklog;
  }

  /**
   * Sets the minimum spare listen.
   */
  public void setAcceptThreadMin(int minSpare)
    throws ConfigException
  {
    if (minSpare < 1)
      throw new ConfigException(L.l("accept-thread-max must be at least 1."));

    _acceptThreadMin = minSpare;
  }

  /**
   * Gets the minimum spare listen.
   */
  public int getAcceptThreadMin()
  {
    return _acceptThreadMin;
  }

  /**
   * Sets the maximum spare listen.
   */
  public void setAcceptThreadMax(int maxSpare)
    throws ConfigException
  {
    if (maxSpare < 1)
      throw new ConfigException(L.l("accept-thread-max must be at least 1."));

    _acceptThreadMax = maxSpare;
  }

  /**
   * Sets the maximum spare listen.
   */
  public int getAcceptThreadMax()
  {
    return _acceptThreadMax;
  }

  /**
   * Sets the maximum connections per port
   */
  public void setConnectionMax(int max)
  {
    _connectionMax = max;
  }

  /**
   * Returns the port-based connection max.
   *
   * @return the connection max.
   */
  public int getConnectionMax()
  {
    return _connectionMax;
  }

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
   * Creates a persistent store instance.
   */
  public PersistentStoreConfig createPersistentStore()
  {
    if (_persistentStoreConfig == null)
      _persistentStoreConfig = new PersistentStoreConfig();
    
    return _persistentStoreConfig;
  }

  /**
   * Creates a persistent store instance.
   */
  public PersistentStoreConfig getPersistentStoreConfig()
  {
    return _persistentStoreConfig;
  }
  
  public void startPersistentStore()
  {
  }

  public Object createJdbcStore()
    throws ConfigException
  {
    return null;
  }

  /**
   * Arguments on boot
   */
  public void addJavaExe(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addJvmClasspath(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogLog(ConfigProgram args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogPassword(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogPort(int port)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogAddress(String addr)
  {
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public void setMemoryFreeMin(Bytes min)
  {
    _memoryFreeMin = min.getBytes();
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public long getMemoryFreeMin()
  {
    return _memoryFreeMin;
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public void setPermGenFreeMin(Bytes min)
  {
    _permGenFreeMin = min.getBytes();
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public long getPermGenFreeMin()
  {
    return _permGenFreeMin;
  }

  /**
   * Sets the maximum keepalive
   */
  public void setKeepaliveMax(int max)
  {
    _keepaliveMax = max;
  }

  /**
   * Returns the thread-based keepalive max.
   *
   * @return the keepalive max.
   */
  public int getKeepaliveMax()
  {
    return _keepaliveMax;
  }

  /**
   * Sets the keepalive timeout
   */
  public void setKeepaliveTimeout(Period period)
  {
    _selfServer.setKeepaliveTimeout(period);
  }

  /**
   * Sets the keepalive timeout
   */
  public long getKeepaliveTimeout()
  {
    return _selfServer.getKeepaliveTimeout();
  }

  /**
   * Sets the keepalive connection timeout
   */
  public void setKeepaliveConnectionTimeMax(Period period)
  {
    _keepaliveConnectionTimeMax = period.getPeriod();
  }

  /**
   * Sets the keepalive timeout
   */
  public long getKeepaliveConnectionTimeMax()
  {
    return _keepaliveConnectionTimeMax;
  }

  /**
   * Sets the select-based keepalive timeout
   */
  public void setKeepaliveSelectEnable(boolean enable)
  {
    _keepaliveSelectEnable = enable;
  }

  /**
   * Sets the select-based keepalive timeout
   */
  public void setKeepaliveSelectMax(int max)
  {
    _keepaliveSelectMax = max;
  }

  /**
   * Gets the select-based keepalive timeout
   */
  public boolean isKeepaliveSelectEnable()
  {
    return _keepaliveSelectEnable;
  }

  /**
   * Sets the select-based keepalive timeout
   */
  public void setKeepaliveSelectThreadTimeout(Period period)
  {
    _keepaliveSelectThreadTimeout = period.getPeriod();
  }

  /**
   * Sets the select-based keepalive timeout
   */
  public long getKeepaliveSelectThreadTimeout()
  {
    return _keepaliveSelectThreadTimeout;
  }

  public Management createManagement()
  {
    if (_management == null && _resin != null) {
      _management = _resin.createManagement();

      _management.setCluster(getCluster());
    }

    return _management;
  }

  /**
   * Sets the redeploy mode
   */
  public void setRedeployMode(String redeployMode)
  {
  }

  /**
   * Sets the max wait time for shutdown.
   */
  public void setShutdownWaitMax(Period waitTime)
  {
    _shutdownWaitMax = waitTime.getPeriod();
  }

  /**
   * Sets the suspend timeout
   */
  public void setSuspendTimeMax(Period period)
  {
    _suspendTimeMax = period.getPeriod();
  }

  /**
   * Sets the suspend timeout
   */
  public long getSuspendTimeMax()
  {
    return _suspendTimeMax;
  }

  /**
   * Gets the max wait time for a shutdown.
   */
  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }

  /**
   * Sets the default read/write timeout for the request sockets.
   */
  public void setSocketTimeout(Period period)
  {
    _selfServer.setSocketTimeout(period);
  }

  /**
   * Gets the read timeout for the request sockets.
   */
  public long getSocketTimeout()
  {
    return _selfServer.getSocketTimeout();
  }

  /**
   * Sets the maximum thread-based keepalive
   */
  public void setThreadMax(int max)
  {
    if (max < 0)
      throw new ConfigException(L.l("<thread-max> ({0}) must be greater than zero.",
				    max));
    
    _threadMax = max;
  }

  /**
   * Sets the maximum executor (background) thread.
   */
  public void setThreadExecutorTaskMax(int max)
  {
    _threadExecutorTaskMax = max;
  }

  /**
   * Sets the minimum number of idle threads in the thread pool.
   */
  public void setThreadIdleMin(int min)
  {
    _threadIdleMin = min;
  }

  /**
   * Sets the maximum number of idle threads in the thread pool.
   */
  public void setThreadIdleMax(int max)
  {
    _threadIdleMax = max;
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
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/resin/cluster.rnc";
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

    Vfs.setPwd(path, _classLoader);
  }

  /**
   * Sets the root directory.
   */
  public Path getRootDirectory()
  {
    return _hostContainer.getRootDirectory();
  }

  /**
   * Sets the root directory.
   */
  public void setRootDir(Path path)
  {
    setRootDirectory(path);
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

  /**
   * Adds the cache.
   */
  public AbstractCache createCache()
    throws ConfigException
  {
    log.warning(L.l("<cache> requires Resin Professional.  Please see http://www.caucho.com for Resin Professional information and licensing."));
    
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
   * Creates the ping.
   */
  public Object createPing()
    throws ConfigException
  {
    return createManagement().createPing();
  }

  /**
   * Adds the ping.
   */
  public void addPing(Object ping)
    throws ConfigException
  {
    createManagement().addPing(ping);
  }

  /**
   * Sets true if the select manager should be enabled
   */
  @Override
  public boolean isSelectManagerEnabled()
  {
    return getSelectManager() != null;
  }

  public void addSelectManager(SelectManagerCompat selectManager)
  {

  }

  /**
   * Returns the number of select keepalives available.
   */
  public int getFreeKeepaliveSelect()
  {
    AbstractSelectManager selectManager = getSelectManager();

    if (selectManager != null)
      return selectManager.getFreeKeepalive();
    else
      return Integer.MAX_VALUE / 2;
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
   * Returns the select keepalive count.
   */
  public int getKeepaliveSelectCount()
  {
    AbstractSelectManager selectManager = getSelectManager();

    if (selectManager != null)
      return selectManager.getSelectCount();
    else
      return -1;
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

  //
  // runtime operations
  //

  /**
   * Sets the invocation
   */
  public Invocation buildInvocation(Invocation invocation)
    throws Throwable
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
    return _classLoader.getAdmin();
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
  public Collection<HostController> getHostControllers()
  {
    HostContainer hostContainer = _hostContainer;

    if (hostContainer == null)
      return Collections.emptyList();

    return Collections.unmodifiableList(hostContainer.getHostList());
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
   * If true, ports are bound at end.
   */
  public void setBindPortsAfterStart(boolean bindAtEnd)
  {
    _isBindPortsAtEnd = bindAtEnd;
  }

  /**
   * If true, ports are bound at end.
   */
  public boolean isBindPortsAfterStart()
  {
    return _isBindPortsAtEnd;
  }

  /**
   * Returns the {@link Port}s for this server.
   */
  public Collection<Port> getPorts()
  {
    return Collections.unmodifiableList(_selfServer.getPorts());
  }

  /**
   * Returns the reliable system store
   */
  public ClusterCache getSystemStore()
  {
    synchronized (this) {
      if (_systemStore == null) {
	_systemStore = new ClusterCache("resin:system");
	// XXX: need to set reliability values
      }
    }

    _systemStore.init();
    
    return _systemStore;
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
    _classLoader.init();

    super.init();

    if (_resin != null) {
      createManagement().setCluster(getCluster());
      createManagement().setServer(this);
      createManagement().init();
    }

    if (_threadMax < _threadIdleMax)
      throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})",
				    _threadIdleMax, _threadMax));

    if (_threadIdleMax < _threadIdleMin)
      throw new ConfigException(L.l("<thread-idle-min> ({0}) must be less than <thread-idle-max> ({1})",
				    _threadIdleMin, _threadIdleMax));

    if (_threadMax < _threadExecutorTaskMax)
      throw new ConfigException(L.l("<thread-executor-task-max> ({0}) must be less than <thread-max> ({1})",
				    _threadExecutorTaskMax, _threadMax));
    
    ThreadPool threadPool = ThreadPool.getThreadPool();
    
    threadPool.setThreadMax(_threadMax);
    threadPool.setThreadIdleMax(_threadIdleMax);
    threadPool.setThreadIdleMin(_threadIdleMin);
    threadPool.setExecutorTaskMax(_threadExecutorTaskMax);

    if (_keepaliveSelectEnable) {
      try {
        Class cl = Class.forName("com.caucho.server.port.JniSelectManager");
        Method method = cl.getMethod("create", new Class[0]);

        initSelectManager((AbstractSelectManager) method.invoke(null, null));
      } catch (ClassNotFoundException e) {
        log.warning(L.l("'select-manager' requires Resin Professional.  See http://www.caucho.com for information and licensing."));
      } catch (Throwable e) {
        log.warning(L.l("Cannot enable select-manager {0}", e.toString()));

        log.log(Level.FINER, e.toString());
      }

      if (getSelectManager() != null) {
	if (_keepaliveSelectMax > 0)
	  getSelectManager().setSelectMax(_keepaliveSelectMax);
      }
    }
  }

  /**
   * Start the server.
   */
  public void start()
  {
    init();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      if (! _lifecycle.toStarting())
        return;

      _startTime = Alarm.getCurrentTime();

      if (! Alarm.isTest()) {
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

	log.info("");

        Resin resin = Resin.getCurrent();

        if (resin != null) {
          log.info("resin.home = " + resin.getResinHome().getNativePath());
          log.info("resin.root = " + resin.getRootDirectory().getNativePath());
          log.info("resin.conf = " + resin.getResinConf());
          log.info("");

          log.info("server     = "
		   + _selfServer.getClusterPort().getAddress()
		   + ":" + _selfServer.getClusterPort().getPort()
		   + " (" + getCluster().getId()
		   + ":" + getServerId() + ")");
        }
        else {
          log.info("resin.home = " + System.getProperty("resin.home"));
        }
	
        log.info("user.name  = " + System.getProperty("user.name"));

        log.info("");
      }

      _lifecycle.toStarting();

      getAdminAuthenticator();

      if (_resin != null && _resin.getManagement() != null)
	_resin.getManagement().start(this);

      AbstractSelectManager selectManager = getSelectManager();
      
      if (! _keepaliveSelectEnable
	  || selectManager == null
	  || ! selectManager.start()) {
	initSelectManager(null);
      }

      startClusterPort();

      notifyStart();

      if (! _isBindPortsAtEnd) {
        bindPorts();
	startPorts();
      }
    
      if (_distributedCacheManager == null)
	_distributedCacheManager = createDistributedCacheManager();

      // initialize the store
      getSystemStore();

      getCluster().start();

      _classLoader.start();

      _hostContainer.start();
      
      // initialize the environment admin
      _classLoader.getAdmin();

      startPersistentStore();

      // will only occur if bind-ports-at-end is true
      if (_isBindPortsAtEnd) {
        bindPorts();
	startPorts();
      }

      getCluster().startRemote();

      _alarm.queue(ALARM_INTERVAL);
      
      _lifecycle.toActive();

      // dynamic updates from the cluster start after we're capable of
      // handling messages
      startClusterUpdate();
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

  public void startClusterUpdate()
  {
    /*
    try {
      if (_clusterStore != null)
        _clusterStore.startUpdate();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    */
  }

  /**
   * Start the cluster port
   */
  private void startClusterPort()
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      Port port = _selfServer.getClusterPort();

      if (port != null && port.getPort() != 0) {
	log.info("");
	port.setServer(this);
	port.bind();
	port.start();
	log.info("");
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Bind the ports.
   */
  public void bindPorts()
    throws Exception
  {
    if (_isStartedPorts.getAndSet(true))
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      ArrayList<Port> ports = _selfServer.getPorts();
      if (ports.size() > 0
	  && (ports.get(0) != _selfServer.getClusterPort()
	      || ports.size() > 1)) {
	log.info("");

	for (int i = 0; i < ports.size(); i++) {
	  Port port = ports.get(i);

	  port.setServer(this);

	  port.bind();
	}
      
	log.info("");
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Start the ports.
   */
  public void startPorts()
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      ArrayList<Port> ports = _selfServer.getPorts();
      for (int i = 0; i < ports.size(); i++) {
        Port port = ports.get(i);

	port.start();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Handles the alarm.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;

    try {
      long now = Alarm.getCurrentTime();

      if (isModified()) {
        // XXX: message slightly wrong
        log.warning("Resin restarting due to configuration change");

        _selfServer.getCluster().getResin().destroy();
        return;
      }

      try {
	ArrayList<Port> ports = _selfServer.getPorts();
	
        for (int i = 0; i < ports.size(); i++) {
          Port port = ports.get(i);

          if (port.isClosed()) {
            log.severe("Resin restarting due to closed port: " + port);
            // destroy();
            //_controller.restart();
          }
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
        // destroy();
        //_controller.restart();
        return;
      }
    } finally {
      alarm.queue(ALARM_INTERVAL);
    }
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModified()
  {
    boolean isModified = _classLoader.isModified();

    if (isModified)
      log.fine(this + " is modified");

    return isModified;
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModifiedNow()
  {
    boolean isModified = _classLoader.isModifiedNow();

    if (isModified)
      log.fine("server is modified");

    return isModified;
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

    invalidateMatchingInvocations(matcher);
  }

  /**
   * Clears the proxy cache.
   */
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
  
  public void addDynamicServer(String clusterId,
			       String serverId,
			       String address,
			       int port)
  {
  }
  
  public void removeDynamicServer(String clusterId,
				  String address,
				  int port)
  {
  }

  /**
   * Closes the server.
   */
  public void stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      if (! _lifecycle.toStopping())
        return;

      // notify other servers that we've stopped
      notifyStop();

      Alarm alarm = _alarm;
      _alarm = null;

      if (alarm != null)
        alarm.dequeue();

      if (getSelectManager() != null)
        getSelectManager().stop();

      ArrayList<Port> ports = _selfServer.getPorts();
      for (int i = 0; i < ports.size(); i++) {
        Port port = ports.get(i);

        try {
          port.close();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      try {
	if (_selfServer.getClusterPort() != null)
	  _selfServer.getClusterPort().close();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      try {
        ThreadPool.getThreadPool().interrupt();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        Thread.yield();
      } catch (Throwable e) {
      }

      try {
	if (_hostContainer != null)
	  _hostContainer.stop();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        _classLoader.stop();
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
   * Notifications to cluster servers that we've started
   */
  protected void notifyStart()
  {
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
      thread.setContextClassLoader(_classLoader);

      try {
	Management management = _management;
	_management = null;

	if (management != null)
	  management.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        _hostContainer.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      super.destroy();

      log.fine(this + " destroyed");

      _classLoader.destroy();

      if (_distributedCacheManager != null)
	_distributedCacheManager.close();

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
    return (getClass().getSimpleName() + "[id=" + getServerId()
	    + ",cluster=" + _selfServer.getCluster().getId() + "]");
  }

  public static class SelectManagerCompat {
    private boolean _isEnable = true;

    public void setEnable(boolean isEnable)
    {
      _isEnable = isEnable;
    }

    public boolean isEnable()
    {
      return _isEnable;
    }
  }
}
