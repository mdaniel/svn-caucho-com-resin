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

import javax.annotation.PostConstruct;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.network.NetworkClusterService;
import com.caucho.cloud.network.NetworkListenService;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.thread.ThreadPool;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.server.admin.Management;
import com.caucho.server.cache.AbstractCache;
import com.caucho.server.distcache.PersistentStoreConfig;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.host.HostConfig;
import com.caucho.server.host.HostExpandDeployGenerator;
import com.caucho.server.log.AccessLog;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.webapp.ErrorPage;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Configuration for the <cluster> and <server> tags.
 */
@Configurable
public class ServletContainerConfig implements EnvironmentBean, SchemaBean
{
  private static final L10N L = new L10N(ServletContainerConfig.class);

  private final ResinSystem _resinSystem;
  
  private final Resin _resin;
  
  private PersistentStoreConfig _persistentStoreConfig;
  
  // <host> configuration

  // <cluster> configuration
  
  // backwards compat

  private long _memoryFreeMin = 1024 * 1024;
  private long _permGenFreeMin = 1024 * 1024;

  private int _threadMax = 4096;
  private int _threadExecutorTaskMax = -1;
  private int _threadIdleMin = -1;
  private int _threadIdleMax = -1;
  
  private CloudServer _selfServer;
  private Server _servletContainer;

  /**
   * Creates a new servlet server.
   */
  public ServletContainerConfig(Server servletContainer)
  {
    _servletContainer = servletContainer;
    
    _resinSystem = servletContainer.getResinSystem();
    
    _resin = servletContainer.getResin();
    
    _selfServer = NetworkClusterService.getCurrentSelfServer();
    
    Config.setProperty("server", new ServerVar(_selfServer), getClassLoader());
    Config.setProperty("cluster", new ClusterVar(), getClassLoader());
  }

  public ResinSystem getResinSystem()
  {
    return _resinSystem;
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
   * Returns the relax schema.
   */
  @Override
  public String getSchema()
  {
    return "com/caucho/server/resin/cluster.rnc";
  }
  
  //
  // <cluster> configuration
  //

  /**
   * Development mode error pages.
   */
  @Configurable
  public void setDevelopmentModeErrorPage(boolean isEnable)
  {
    _servletContainer.setDevelopmentModeErrorPage(isEnable);
  }

  /**
   * Creates a persistent store instance.
   */
  @Configurable
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

  @Configurable
  public Object createJdbcStore()
    throws ConfigException
  {
    return null;
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addJavaExe(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addJvmArgLine(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addJvmClasspath(String args)
  {
  }

  /**
   * Sets the stage id
   */
  @Configurable
  public void setStage(String stage)
  {
    if (stage == null || "".equals(stage))
      stage = "production";
    
    _servletContainer.setStage(stage);
  }

  /**
   * The Resin system classloader
   */
  @Configurable
  public void setSystemClassLoader(String loader)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogLog(ConfigProgram args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogPassword(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogPort(int port)
  {
  }

  /**
   * Sets the minimum free memory after a GC
   */
  @Configurable
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
  @Configurable
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

  @Configurable
  public Management createManagement()
  {
    if (_resin != null)
      return _resin.createResinManagement();
    else
      return null;
    
    /*
    if (_management == null && _resin != null) {
      _management = _resin.createResinManagement();

      // _management.setCluster(getCluster());
    }

    return _management;
    */
  }

  /**
   * Sets the redeploy mode
   */
  @Configurable
  public void setRedeployMode(String redeployMode)
  {
  }

  /**
   * Sets the max wait time for shutdown.
   */
  @Configurable
  public void setShutdownWaitMax(Period waitTime)
  {
    _servletContainer.setShutdownWaitMax(waitTime);
  }

  /**
   * Sets the maximum thread-based keepalive
   */
  @Configurable
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
  @Configurable
  public void setThreadExecutorTaskMax(int max)
  {
    _threadExecutorTaskMax = max;
  }

  /**
   * Sets the minimum number of idle threads in the thread pool.
   */
  @Configurable
  public void setThreadIdleMin(int min)
  {
    _threadIdleMin = min;
  }

  /**
   * Sets the maximum number of idle threads in the thread pool.
   */
  @Configurable
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
  @Configurable
  public void setConnectionErrorPage(String errorPage)
  {
    _servletContainer.setConnectionErrorPage(errorPage);
  }

  /**
   * Sets the root directory.
   */
  @Configurable
  public void setRootDirectory(Path path)
  {
    _servletContainer.setRootDirectory(path);

    Vfs.setPwd(path, getClassLoader());
  }

  /**
   * Sets the root directory.
   */
  @Configurable
  public void setRootDir(Path path)
  {
    setRootDirectory(path);
  }

  /**
   * Sets the server header.
   */
  @Configurable
  public void setServerHeader(String serverHeader)
  {
    _servletContainer.setServerHeader(serverHeader);
  }

  /**
   * Sets the url-length-max
   */
  @Configurable
  public void setUrlLengthMax(int max)
  {
    _servletContainer.setUrlLengthMax(max);
  }
  
  @Configurable
  public void setIgnoreClientDisconnect(boolean isIgnore)
  {
    _servletContainer.setIgnoreClientDisconnect(isIgnore);
  }

  /**
   * Adds a WebAppDefault.
   */
  @Configurable
  public void addWebAppDefault(WebAppConfig webAppConfig)
  {
    _servletContainer.addWebAppDefault(webAppConfig);
  }

  /**
   * Adds an EarDefault
   */
  @Configurable
  public void addEarDefault(EarConfig earConfig)
  {
    _servletContainer.addEarDefault(earConfig);
  }

  /**
   * Adds a HostDefault.
   */
  @Configurable
  public void addHostDefault(HostConfig hostConfig)
  {
    _servletContainer.addHostDefault(hostConfig);
  }

  /**
   * Adds a HostDeploy.
   */
  @Configurable
  public HostExpandDeployGenerator createHostDeploy()
  {
    return _servletContainer.createHostDeploy();
  }

  /**
   * Adds a HostDeploy.
   */
  @Configurable
  public void addHostDeploy(HostExpandDeployGenerator deploy)
  {
    _servletContainer.addHostDeploy(deploy);
  }

  /**
   * Adds the host.
   */
  @Configurable
  public void addHost(HostConfig host)
  {
    _servletContainer.addHost(host);
  }

  /**
   * Adds rewrite-dispatch.
   */
  @Configurable
  public RewriteDispatch createRewriteDispatch()
  {
    return _servletContainer.createRewriteDispatch();
  }

  /**
   * Creates the proxy cache.
   */
  @Configurable
  public AbstractCache createProxyCache()
    throws ConfigException
  {
    return _servletContainer.createProxyCache();
  }

  /**
   * backward compatibility for proxy cache
   */
  @Configurable
  public AbstractCache createCache()
    throws ConfigException
  {
    return createProxyCache();
  }

  /**
   * Sets the access log.
   */
  @Configurable
  public void setAccessLog(AccessLog accessLog)
  {
    _servletContainer.setAccessLog(accessLog);
  }

  /**
   * Sets the session cookie
   */
  @Configurable
  public void setSessionCookie(String sessionCookie)
  {
    _servletContainer.setSessionCookie(sessionCookie);
  }

  /**
   * Sets the ssl session cookie
   */
  @Configurable
  public void setSSLSessionCookie(String cookie)
  {
    _servletContainer.setSSLSessionCookie(cookie);
  }

  /**
   * Sets the session url prefix.
   */
  @Configurable
  public void setSessionUrlPrefix(String urlPrefix)
  {
    _servletContainer.setSessionURLPrefix(urlPrefix);
  }

  /**
   * Sets the alternate session url prefix.
   */
  @Configurable
  public void setAlternateSessionUrlPrefix(String urlPrefix)
    throws ConfigException
  {
    _servletContainer.setAlternateSessionURLPrefix(urlPrefix);
  }

  /**
   * Sets URL encoding.
   */
  @Configurable
  public void setUrlCharacterEncoding(String encoding)
    throws ConfigException
  {
    _servletContainer.setURLCharacterEncoding(encoding);
  }

  /**
   * Creates the ping.
   */
  public Object createPing()
    throws ConfigException
  {
    return createManagement().createPing();
  }

  public void addSelectManager(SelectManagerCompat selectManager)
  {

  }

  /**
   * Adds an error page
   */
  public void addErrorPage(ErrorPage errorPage)
  {
    _servletContainer.addErrorPage(errorPage);
  }
  
  public void addLoadBalanceWeight(ConfigProgram program)
  {
  }

  public void setLoadBalanceWarmupTime(ConfigProgram program)
  {
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
  {
    if (_threadIdleMax > 0
        && _threadMax > 0
        && _threadMax < _threadIdleMax)
      throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})",
                                    _threadIdleMax, _threadMax));

    if (_threadIdleMin > 0
        && _threadIdleMax > 0
        && _threadIdleMax < _threadIdleMin)
      throw new ConfigException(L.l("<thread-idle-min> ({0}) must be less than <thread-idle-max> ({1})",
                                    _threadIdleMin, _threadIdleMax));

    if (_threadMax > 0
        && _threadExecutorTaskMax > 0
        && _threadMax < _threadExecutorTaskMax)
      throw new ConfigException(L.l("<thread-executor-task-max> ({0}) must be less than <thread-max> ({1})",
                                    _threadExecutorTaskMax, _threadMax));

    ThreadPool threadPool = ThreadPool.getThreadPool();

    if (_threadMax > 0)
      threadPool.setThreadMax(_threadMax);

    if (_threadIdleMin > 0)
      threadPool.setIdleMin(_threadIdleMin);

    threadPool.setExecutorTaskMax(_threadExecutorTaskMax);
    
    /*
    if (_keepaliveSelectEnable) {
      try {
        Class<?> cl = Class.forName("com.caucho.server.connection.JniSelectManager");
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
    */
  }

  /**
   * EL variables
   */
  public class ClusterVar {
    /**
     * Returns the resin.id
     */
    public String getId()
    {
      return _selfServer.getCluster().getId();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRoot()
    {
      return getRootDirectory();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRootDir()
    {
      return getRootDirectory();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRootDirectory()
    {
      return _servletContainer.getRootDirectory();
    }
  }

  public class ServerVar {
    private final ClusterServer _server;

    public ServerVar(CloudServer server)
    {
      ClusterServer clusterServer = server.getData(ClusterServer.class);
      
      if (clusterServer == null)
        throw new NullPointerException();
      
      _server = clusterServer;
    }

    public String getId()
    {
      return _server.getId();
    }

    private int getPort(SocketLinkListener port)
    {
      if (port == null)
        return 0;

      return port.getPort();
    }

    private String getAddress(SocketLinkListener port)
    {
      if (port == null)
        return null;

      String address = port.getAddress();

      if (address == null || address.length() == 0)
        address = "INADDR_ANY";

      return address;
    }

    private SocketLinkListener getFirstPort(String protocol, boolean isSSL)
    {
      ResinSystem resinSystem = getResinSystem();
      NetworkListenService listenService 
        = resinSystem.getService(NetworkListenService.class);
      
      for (SocketLinkListener port : listenService.getListeners()) {
        if (protocol.equals(port.getProtocolName()) && (port.isSSL() == isSSL))
          return port;
      }

      return null;
    }

    public String getAddress()
    {
      return _selfServer.getAddress();
    }

    public int getPort()
    {
      return _selfServer.getPort();
    }

    public String getHttpAddress()
    {
      return getAddress(getFirstPort("http", false));
    }

    public int getHttpPort()
    {
      return getPort(getFirstPort("http", false));
    }


    public String getHttpsAddress()
    {
      return getAddress(getFirstPort("http", true));
    }

    public int getHttpsPort()
    {
      return getPort(getFirstPort("http", true));
    }

    /**
     * @deprecated backwards compat.
     */
    public Path getRoot()
    {
      Resin resin =  Resin.getLocal();

      return resin == null ? Vfs.getPwd() : resin.getRootDirectory();
    }
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
