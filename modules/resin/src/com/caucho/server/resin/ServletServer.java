/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.resin;

import java.lang.ref.SoftReference;

import java.lang.reflect.Method;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.InputStream;
import java.io.IOException;

import java.net.*;
import java.util.*;
import java.util.LinkedHashMap;

import javax.naming.*;

import javax.management.ObjectName;
import javax.management.MBeanServer;

import javax.resource.spi.ResourceAdapter;

import javax.servlet.*;

import javax.servlet.http.HttpServletResponse;

import javax.servlet.jsp.el.VariableResolver;

import org.iso_relax.verifier.Schema;

import com.caucho.vfs.*;

import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;

import com.caucho.log.Log;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.Environment;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentBean;

import com.caucho.loader.enhancer.EnhancingClassLoader;

import com.caucho.jca.ResourceManagerImpl;

import com.caucho.config.SchemaBean;
import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;

import com.caucho.config.types.Period;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.make.AlwaysModified;

import com.caucho.naming.Jndi;

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.security.PermissionManager;

import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.dispatch.InvocationMatcher;
import com.caucho.server.dispatch.ErrorFilterChain;

import com.caucho.server.webapp.WebAppConfig;
import com.caucho.server.webapp.Application;
import com.caucho.server.webapp.ErrorPage;

import com.caucho.server.host.Host;
import com.caucho.server.host.HostExpandDeployGenerator;
import com.caucho.server.host.HostContainer;
import com.caucho.server.host.HostConfig;

import com.caucho.server.port.ProtocolDispatchServer;
import com.caucho.server.port.Port;
import com.caucho.server.port.AbstractSelectManager;
import com.caucho.server.port.AbstractSelectManager;

import com.caucho.server.port.mbean.PortMBean;

import com.caucho.server.log.AccessLog;

import com.caucho.server.http.HttpProtocol;
import com.caucho.server.http.SrunProtocol;

import com.caucho.server.hmux.HmuxProtocol;

import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterDef;
import com.caucho.server.cluster.ClusterContainer;
import com.caucho.server.cluster.ClusterPort;

import com.caucho.server.cache.AbstractCache;

import com.caucho.transaction.TransactionManagerImpl;

import com.caucho.lifecycle.Lifecycle;


import com.caucho.jmx.Jmx;

import com.caucho.server.resin.mbean.ServletServerMBean;

public class ServletServer extends ProtocolDispatchServer
  implements EnvironmentBean, SchemaBean, AlarmListener,
	     ClassLoaderListener, ServletServerMBean {
  private static final L10N L = new L10N(ServletServer.class);
  private static final Logger log = Log.open(ServletServer.class);

  private static final long ALARM_INTERVAL = 60000;

  private static SoftReference<Schema> _schemaRef;
  
  private static final EnvironmentLocal<String> _serverIdLocal =
    new EnvironmentLocal<String>("caucho.server-id");

  private EnvironmentClassLoader _classLoader;
  
  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();
  private VariableResolver _varResolver;

  private LinkedHashMap<String,String> _jmxContext;

  private HostContainer _hostContainer;

  private ObjectName _objectName;

  private String _serverId = "";

  private String _serverHeader = "Resin/" + com.caucho.Version.VERSION;

  private String _id = "";

  private String _url = "";
  
  private int _srunCount;

  private AccessLog _accessLog;

  private long _waitForActiveTime = 10000L;

  private int _keepaliveMax = -1;
  private ArrayList<Port> _ports = new ArrayList<Port>();

  private Alarm _alarm = new Alarm(this);
  private AbstractCache _cache;

  private boolean _isBindPortsAtEnd;
  private volatile boolean _isStartedPorts;

  private final Lifecycle _lifecycle;
  
  /**
   * Creates a new servlet server.
   */
  public ServletServer()
    throws Exception
  {
    _classLoader = new EnhancingClassLoader();
    _classLoader.setOwner(this);

    Environment.addClassLoaderListener(this, _classLoader);

    /*
    try {
      Jndi.rebindDeepShort("jmx/MBeanServer",
			   new MBeanServerProxy(_classLoader));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    */
    
    _jmxContext = Jmx.copyContextProperties();
    
    VariableResolver parentResolver = EL.getEnvironment();
    _varResolver = new MapVariableResolver(_variableMap, parentResolver);
    EL.setEnvironment(_varResolver, _classLoader);
    EL.setVariableMap(_variableMap, _classLoader);
    
    try {
      Method method = Jndi.class.getMethod("lookup",
                                           new Class[] { String.class });
      _variableMap.put("jndi:lookup", method);
    } catch (Throwable e) {
    }
    _variableMap.put("server", new Var());

    PermissionManager permissionManager = new PermissionManager();
    PermissionManager.setPermissionManager(permissionManager);
    
    String serverId = _serverIdLocal.get();
    if (serverId == null)
      serverId = "";

    setServerId(serverId);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      LinkedHashMap<String,String> props = Jmx.copyContextProperties();
      props.put("Server", "default");
    
      Jmx.setContextProperties(props);
      
      _hostContainer = new HostContainer();
      _hostContainer.setClassLoader(_classLoader);
      _hostContainer.setDispatchServer(this);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    try {
      Class cl = Class.forName("com.caucho.server.port.JniSelectManager");
      Method method = cl.getMethod("create", new Class[0]);

      setSelectManager((AbstractSelectManager) method.invoke(null, null));
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString());
    }
    
    _lifecycle = new Lifecycle(log, toString(), Level.INFO);
  }

  /**
   * Returns the classLoader
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Sets the classLoader
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader loader)
  {
    _classLoader = loader;
    _classLoader.setOwner(this);
  }

  /**
   * Returns the relax schema.
   */
  public Schema getSchema()
  {
    Schema schema = null;
    
    if (_schemaRef == null || (schema = _schemaRef.get()) == null) {
      String schemaName = "com/caucho/server/resin/server.rnc";

      try {
	schema = CompactVerifierFactoryImpl.compileFromResource(schemaName);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);
	log.warning(e.toString());
      }

      _schemaRef = new SoftReference<Schema>(schema);
    }

    return schema;
  }

  /**
   * Sets the id.
   */
  public void setId(String id)
  {
    if (id == null)
      id = "";

    _id = id;

    LinkedHashMap<String,String> props = Jmx.copyContextProperties();
    if (_id.equals(""))
      props.put("Server", "default-server");
    else
      props.put("Server", _id);
    
    Jmx.setContextProperties(props);
  }

  /**
   * Sets the id.
   */
  public void setServerId(String serverId)
  {
    if (serverId == null)
      serverId = "";
    
    _serverIdLocal.set(serverId, _classLoader);
    _serverId = serverId;

    _classLoader.setId("servlet-server:" + serverId);
    _variableMap.put("serverId", serverId);

    if (_lifecycle != null)
      _lifecycle.setName(toString());
  }

  /**
   * Returns the id.
   */
  public String getServerId()
  {
    return _serverId;
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path path)
  {
    _hostContainer.setRootDirectory(path);

    Vfs.setPwd(path, _classLoader);

    _variableMap.put("root-dir", path);
    _variableMap.put("server-root", path);
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
   * Returns the keepalive max.
   *
   * @return the keepalive max.
   */
  public int getKeepaliveMax()
  {
    return _keepaliveMax;
  }

  /**
   * Sets the maximum keepalive
   */
  public void setKeepaliveMax(int max)
  {
    _keepaliveMax = max;

    AbstractSelectManager selectManager = getSelectManager();
    if (selectManager != null)
      selectManager.setSelectMax(max);
  }

  /**
   * Returns the number of select keepalives available.
   */
  public int getFreeSelectKeepalive()
  {
    AbstractSelectManager selectManager = getSelectManager();

    if (selectManager != null)
      return selectManager.getFreeKeepalive();
    else
      return Integer.MAX_VALUE / 2;
  }

  /**
   * Sets the keepalive timeout
   */
  public void setKeepaliveTimeout(Period period)
  {
    AbstractSelectManager selectManager = getSelectManager();
    if (selectManager != null)
      selectManager.setSelectTimeout(period.getPeriod());
  }

  /**
   * Adds an error page
   */
  public void addErrorPage(ErrorPage errorPage)
  {
    getErrorApplication().addErrorPage(errorPage);
  }

  /**
   * Sets the invocation
   */
  public void buildInvocation(Invocation invocation)
    throws Throwable
  {
    if (_lifecycle.waitForActive(_waitForActiveTime)) {
      _hostContainer.buildInvocation(invocation);
    }
    else {
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

      invocation.setFilterChain(new ErrorFilterChain(code));
      invocation.setApplication(getErrorApplication());
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

      Application app = host.findApplicationByURI(url);

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
   * Returns the matching servlet pattern for a URL.
   */
  public Application getApplication(String hostName, int port, String url)
  {
    try {
      HostContainer hostContainer = _hostContainer;
      
      if (hostContainer == null)
	return null;
      
      Host host = hostContainer.getHost(hostName, port);

      if (host == null)
        return null;

      return host.findApplicationByURI(url);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the error application during startup.
   */
  public Application getErrorApplication()
  {
    HostContainer hostContainer = _hostContainer;

    if (hostContainer != null)
      return hostContainer.getErrorApplication();
    else
      return null;
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
   * Creates a http port.
   */
  public void addHttp(Port port)
    throws Exception
  {
    if (_url.equals("") && _serverId.equals(port.getServerId())) {
      if (port.getHost() == null || port.getHost().equals("") ||
          port.getHost().equals("*"))
        _url = "http://localhost";
      else
        _url = "http://" + port.getHost();

      if (port.getPort() != 0)
        _url += ":" + port.getPort();
      
      if (_hostContainer != null)
        _hostContainer.setURL(_url);
    }
    
    if (port.getProtocol() == null) {
      HttpProtocol protocol = new HttpProtocol();
      protocol.setParent(port);
      port.setProtocol(protocol);
    }

    addPort(port);
  }

  /**
   * Adds a port.
   */
  public void addPort(Port port)
    throws Exception
  {
    _ports.add(port);
  }

  /**
   * Returns the ports.
   */
  public ObjectName []getPortObjectNames()
  {
    ObjectName []portNames = new ObjectName[_ports.size()];

    for (int i = 0; i < _ports.size(); i++)
      portNames[i] = _ports.get(i).getObjectName();

    return portNames;
  }

  /**
   * Returns the clusters.
   */
  public ObjectName []getClusterObjectNames()
  {
    ClusterContainer clusterContainer = ClusterContainer.getLocal();
    if (clusterContainer == null)
      return new ObjectName[0];
    
    ArrayList<Cluster> clusterList = clusterContainer.getClusterList();
    ObjectName []clusterNames = new ObjectName[clusterList.size()];

    for (int i = 0; i < clusterList.size(); i++) {
      Cluster subCluster = clusterList.get(i);

      clusterNames[i] = subCluster.getObjectName();
    }

    return clusterNames;
  }

  /**
   * Gives a better error message for old srun.
   */
  public void addSrun(Object object)
    throws ConfigException
  {
    throw new ConfigException(L.l("<srun> must be in a <cluster> for Resin 3.0.  See http://www.caucho.com/resin-3.0/config/balance.xtp for the new syntax."));
  }
  
  /**
   * Handles the case where a class loader is activated.
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
    try {
      Jmx.register(this, "resin:type=Server,name=default");

      //ObjectName name = new ObjectName("resin:type=ThreadPool");
      ThreadPoolAdmin threadPoolAdmin = new ThreadPoolAdmin();
      
      Jmx.register(threadPoolAdmin, "resin:type=ThreadPool");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    try {
      Jmx.unregister("resin:name=default,type=Server");
      Jmx.unregister("resin:type=ThreadPool");
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  /**
   * Initialization.
   */
  public void init()
  {
    _classLoader.init();

    super.init();
  }
    
  /**
   * Start the server.
   */
  public void start()
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);
      
      if (! _lifecycle.toStarting())
	return;

      AbstractSelectManager selectManager = getSelectManager();
      if (selectManager != null)
	selectManager.start();

      Cluster cluster = Cluster.getLocal();

      if (cluster == null) {
        cluster = new Cluster();
        cluster.init();
      }

      ClusterContainer clusterContainer = ClusterContainer.getLocal();
      if (clusterContainer != null) {
	ArrayList<Cluster> clusterList = clusterContainer.getClusterList();

	for (int i = 0; i < clusterList.size(); i++) {
	  Cluster subCluster = clusterList.get(i);

	  if (subCluster instanceof ClusterDef)
	    continue;
	  
	  ArrayList<ClusterPort> clusterPorts;
	  clusterPorts = subCluster.getServerPorts(_serverId);

	  for (int j = 0; j < clusterPorts.size(); j++) {
	    ClusterPort port = clusterPorts.get(j);

	    port.setServer(this);

	    addPort(port);
	  }
	}
      }
      
      if (! _isBindPortsAtEnd)
	bindPorts();
      
      _lifecycle.toActive();

      _classLoader.start();

      _hostContainer.start();

      // will only occur if bind-ports-at-end is true
      bindPorts();

      startPorts();

      _alarm.queue(ALARM_INTERVAL);
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
      
      for (int i = 0; i < _ports.size(); i++) {
        Port port = _ports.get(i);
	
        if (_serverId.equals(port.getServerId()))
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
      
      for (int i = 0; i < _ports.size(); i++) {
        Port port = _ports.get(i);
	
        if (_serverId.equals(port.getServerId()))
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
      
	stop();
	destroy();
	return;
      }

      try {
	for (int i = 0; i < _ports.size(); i++) {
	  Port port = _ports.get(i);

	  if (port.isClosed()) {
	    log.info("Resin restarting due to closed port: " + port);
	    destroy();
	  }
	}
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
	destroy();
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
      log.fine("servlet server is modified");

    return isModified;
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
   * Clears the invocation cache.
   */
  public void clearCache()
  {
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
      
      for (int i = 0; i < _ports.size(); i++) {
        Port port = _ports.get(i);

        try {
          port.close();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    
      try {
	ThreadPool.interrupt();
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
      _ports = null;
      _variableMap = null;
      _varResolver = null;
      _accessLog = null;
      _cache = null;
    } finally {
      DynamicClassLoader.setOldLoader(thread, oldLoader);
    }
  }

  public String toString()
  {
    return "ServletServer[" + _serverId + "]";
  }

  /**
   * EL variables for the server.
   */
  public class Var {
    public String getId()
    {
      return _serverId;
    }
    
    public Path getRootDir()
    {
      return _hostContainer.getRootDirectory();
    }
    
    public Path getRootDirectory()
    {
      return _hostContainer.getRootDirectory();
    }
    
    public String toString()
    {
      return ServletServer.this.toString();
    }
  }
}
