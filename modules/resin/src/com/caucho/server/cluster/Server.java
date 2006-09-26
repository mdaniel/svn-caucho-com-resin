/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.*;
import javax.resource.spi.ResourceAdapter;
import javax.servlet.http.HttpServletResponse;

import com.caucho.config.*;
import com.caucho.config.types.*;
import com.caucho.jca.ResourceManagerImpl;
import com.caucho.jmx.Jmx;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.*;
import com.caucho.log.Log;
import com.caucho.make.AlwaysModified;
import com.caucho.security.PermissionManager;
import com.caucho.server.cache.AbstractCache;
import com.caucho.server.deploy.EnvironmentDeployInstance;
import com.caucho.server.dispatch.*;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.host.*;
import com.caucho.server.http.HttpProtocol;
import com.caucho.server.log.AccessLog;
import com.caucho.server.port.AbstractSelectManager;
import com.caucho.server.port.Port;
import com.caucho.server.port.ProtocolDispatchServer;
import com.caucho.server.resin.*;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.ErrorPage;
import com.caucho.server.webapp.RewriteInvocation;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import com.caucho.management.j2ee.*;
import com.caucho.management.server.*;

public class Server extends ProtocolDispatchServer
  implements EnvironmentBean, SchemaBean, AlarmListener,
             ClassLoaderListener {
  private static final L10N L = new L10N(Server.class);
  private static final Logger log
    = Logger.getLogger(Server.class.getName());

  private static final long ALARM_INTERVAL = 60000;

  private static final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  private final ClusterServer _clusterServer;
  private final Resin _resin;
  
  private EnvironmentClassLoader _classLoader;

  private Throwable _configException;

  private HostContainer _hostContainer;

  private String _serverHeader = "Resin/" + com.caucho.Version.VERSION;

  private String _url = "";

  private int _srunCount;

  private AccessLog _accessLog;

  private long _waitForActiveTime = 10000L;

  // <server> configuration
  private int _acceptListenBacklog = 100;
  
  private int _acceptThreadMin = 5;
  private int _acceptThreadMax = 10;

  private int _keepaliveMax = 128;
  
  private long _keepaliveTimeout = 5000;
  
  private boolean _keepaliveSelectEnable = true;
  private long _keepaliveSelectThreadTimeout = 1000;

  private long _memoryFreeMin = 1024 * 1024;
  
  private long _socketTimeout = 65000L;
  
  private long _shutdownWaitMax = 60 * 1000;
  
  private int _threadMax = 4096;
  private int _threadIdleMin = 5;
  private int _threadIdleMax = 10;

  // <cluster> configuration

  private String _connectionErrorPage;

  private ServerAdmin _admin;

  private Alarm _alarm;
  private AbstractCache _cache;

  private boolean _isBindPortsAtEnd;
  private volatile boolean _isStartedPorts;

  private long _startTime;

  private final Lifecycle _lifecycle;

  /**
   * Creates a new servlet server.
   */
  public Server(ClusterServer clusterServer)
  {
    if (clusterServer == null)
      throw new NullPointerException();
    
    _clusterServer = clusterServer;
    _resin = _clusterServer.getCluster().getResin();

    try {
      Thread thread = Thread.currentThread();

      ClassLoader loader = clusterServer.getCluster().getClassLoader();
      _classLoader = (EnvironmentClassLoader) loader;

      Environment.addClassLoaderListener(this, _classLoader);

      PermissionManager permissionManager = new PermissionManager();
      PermissionManager.setPermissionManager(permissionManager);

      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(_classLoader);
	
	_serverIdLocal.set(_clusterServer.getId());

        _hostContainer = new HostContainer();
        _hostContainer.setClassLoader(_classLoader);
        _hostContainer.setDispatchServer(this);

	_clusterServer.getServerProgram().configure(this);

	_admin = new ServerAdmin(this);
	
	_alarm = new Alarm(this);
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
    _keepaliveTimeout = period.getPeriod();
  }

  /**
   * Sets the keepalive timeout
   */
  public long getKeepaliveTimeout()
  {
    return _keepaliveTimeout;
  }

  /**
   * Sets the select-based keepalive timeout
   */
  public void setKeepaliveSelectEnable(boolean enable)
  {
    _keepaliveSelectEnable = enable;
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
  public void getKeepaliveSelectThreadTimeout(Period period)
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

  /**
   * Sets the default read/write timeout for the request sockets.
   */
  public void setSocketTimeout(Period period)
  {
    _socketTimeout = period.getPeriod();
  }

  /**
   * Gets the read timeout for the request sockets.
   */
  public long getSocketTimeout()
  {
    return _socketTimeout;
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
    return _clusterServer.getId();
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
    throws Exception
  {
    _hostContainer.addHost(host);
  }

  /**
   * Returns the cluster.
   */
  public Cluster getCluster()
  {
    return _clusterServer.getCluster();
  }

  /**
   * Adds rewrite-dispatch.
   */
  public RewriteInvocation createRewriteDispatch()
  {
    return _hostContainer.createRewriteDispatch();
  }

  /**
   * Adds the cache.
   */
  public AbstractCache createCache()
    throws ConfigException
  {
    try {
      Class cl = Class.forName("com.caucho.server.cache.Cache");

      _cache = (AbstractCache) cl.newInstance();
    } catch (Throwable e) {
      e.printStackTrace();
    }

    if (_cache == null) {
      throw new ConfigException(L.l("<cache> requires Resin Professional.  Please see http://www.caucho.com for Resin Professional information and licensing."));
    }

    return _cache;
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
   * Adds the ping.
   */
  public Object createPing()
    throws ConfigException
  {
    try {
      Class pingClass = Class.forName("com.caucho.server.admin.PingThread");

      return pingClass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new ConfigException(L.l("<ping> is only available in Resin Professional."));
    } catch (Throwable e) {
      log.fine(e.toString());

      throw new ConfigException(e);
    }
  }

  /**
   * Adds the ping.
   */
  public void addPing(ResourceAdapter ping)
    throws ConfigException
  {
    ResourceManagerImpl.addResource(ping);
  }

  /**
   * Sets true if the select manager should be enabled
   */
  public boolean isSelectManagerEnabled()
  {
    return getSelectManager() != null;
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

  //
  // runtime operations
  //

  /**
   * Sets the invocation
   */
  public void buildInvocation(Invocation invocation)
    throws Throwable
  {
    if (_configException != null) {
      invocation.setFilterChain(new ExceptionFilterChain(_configException));
      invocation.setWebApp(getErrorWebApp());
      invocation.setDependency(AlwaysModified.create());
      return;
    }
    else if (_lifecycle.waitForActive(_waitForActiveTime)) {
      _hostContainer.buildInvocation(invocation);
    }
    else {
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

      invocation.setFilterChain(new ErrorFilterChain(code));
      invocation.setWebApp(getErrorWebApp());
      invocation.setDependency(AlwaysModified.create());
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
   * Returns the matching servlet pattern for a URL.
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
    return Collections.unmodifiableList(_clusterServer.getPorts());
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

    if (_threadMax < _threadIdleMax)
      throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})",
				    _threadIdleMax, _threadMax));

    if (_threadIdleMax < _threadIdleMin)
      throw new ConfigException(L.l("<thread-idle-min> ({0}) must be less than <thread-max> ({1})",
				    _threadIdleMin, _threadIdleMax));

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
        log.info(System.getProperty("os.name") + " " +
                 System.getProperty("os.version") + " " +
                 System.getProperty("os.arch"));
        log.info("Java " + System.getProperty("java.vm.version") + ", " +
                 System.getProperty("sun.arch.data.model") + ", " +
                 System.getProperty("java.vm.info") + ", " +
                 System.getProperty("file.encoding") + ", " +
                 System.getProperty("user.language") + ", " +
                 System.getProperty("java.vm.vendor"));
	log.info("user: " + System.getProperty("user.name"));

        log.info("resin.home = " + System.getProperty("resin.home"));
        log.info("server.root = " + System.getProperty("server.root"));
        log.info("");
      }

      AbstractSelectManager selectManager = getSelectManager();
      if (_keepaliveSelectEnable && selectManager != null)
        selectManager.start();

      if (! _isBindPortsAtEnd) {
        bindPorts();
      }

      _lifecycle.toActive();

      _classLoader.start();

      _hostContainer.start();

      // will only occur if bind-ports-at-end is true
      if (_isBindPortsAtEnd) {
        bindPorts();
      }

      startPorts();

      _alarm.queue(ALARM_INTERVAL);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      _configException = e;
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
    synchronized (this) {
      if (_isStartedPorts)
        return;

      _isStartedPorts = true;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      ArrayList<Port> ports = _clusterServer.getPorts();
      for (int i = 0; i < ports.size(); i++) {
	Port port = ports.get(i);

	port.setServer(this);
	
	port.bind();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Start the ports.
   */
  public void startPorts()
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      ArrayList<Port> ports = _clusterServer.getPorts();
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
        log.info("Resin restarting due to configuration change");

        _clusterServer.getCluster().getResin().destroy();
        return;
      }

      try {
      ArrayList<Port> ports = _clusterServer.getPorts();
        for (int i = 0; i < ports.size(); i++) {
          Port port = ports.get(i);

          if (port.isClosed()) {
            log.info("Resin restarting due to closed port: " + port);
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
      log.fine("server is modified");

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

      super.stop();

      Alarm alarm = _alarm;
      _alarm = null;

      if (alarm != null)
        alarm.dequeue();

      if (getSelectManager() != null)
        getSelectManager().stop();

      ArrayList<Port> ports = _clusterServer.getPorts();
      for (int i = 0; i < ports.size(); i++) {
        Port port = ports.get(i);

        try {
          port.close();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
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
    }
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
        _hostContainer.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      super.destroy();

      log.fine(this + " destroyed");

      _classLoader.destroy();

      _hostContainer = null;
      _accessLog = null;
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
    return ("Server[id=" + getServerId()
	    + ",cluster=" + _clusterServer.getCluster().getId() + "]");
  }
}
