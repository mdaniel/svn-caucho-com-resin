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

package com.caucho.server.resin;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.bam.broker.Broker;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.license.LicenseClient;
import com.caucho.cloud.loadbalance.LoadBalanceService;
import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.network.NetworkListenSystem;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.cloud.topology.CloudSystem;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.WebBeansAddLoaderListener;
import com.caucho.config.program.ConfigProgram;
import com.caucho.ejb.manager.EjbEnvironmentListener;
import com.caucho.env.deploy.DeployControllerService;
import com.caucho.env.git.GitSystem;
import com.caucho.env.jpa.ListenerPersistenceEnvironment;
import com.caucho.env.log.LogSystem;
import com.caucho.env.repository.AbstractRepository;
import com.caucho.env.repository.LocalRepositoryService;
import com.caucho.env.repository.RepositorySpi;
import com.caucho.env.repository.RepositorySystem;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.service.RootDirectorySystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownSystem;
import com.caucho.env.vfs.RepositoryScheme;
import com.caucho.java.WorkDir;
import com.caucho.license.LicenseCheck;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.admin.Management;
import com.caucho.server.admin.StatSystem;
import com.caucho.server.cluster.Server;
import com.caucho.server.cluster.ServerConfig;
import com.caucho.server.cluster.ServletContainerConfig;
import com.caucho.server.cluster.ServletSystem;
import com.caucho.server.resin.BootConfig.BootType;
import com.caucho.server.resin.ResinArgs.BoundPort;
import com.caucho.util.Alarm;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class Resin
{
  private static Logger _log;
  private static L10N _L;

  public static final int EXIT_OK = 0;

  private static final EnvironmentLocal<Resin> _resinLocal
    = new EnvironmentLocal<Resin>();

  private final ResinArgs _args;
  
  private final ResinSystem _resinSystem;
  
  private String _serverId = null;

  private Path _resinHome;
  private Path _resinConf;
  
  private Path _rootDirectory;
  private Path _resinDataDirectory;
  private Path _serverDataDirectory;
  
  private Path _logDirectory;
  
  private String _homeCluster;
  private String _dynamicAddress;
  private int _dynamicPort;
  
  private String _resinSystemAuthKey;
  
  private String _stage = "production";

  private Socket _pingSocket;
  
  private long _shutdownWaitMax = 60000L;
  
  private ResinDelegate _resinDelegate;

  private Lifecycle _lifecycle;

  private BootConfig _bootConfig;
  private BootResinConfig _bootResinConfig;
  private BootServerConfig _bootServerConfig;
  
  private CloudServer _selfServer;
  
  private ServletContainerConfig _servletContainerConfig;
  private Server _servletSystem;

  private long _initialStartTime;
  private long _startTime;

  private ClassLoader _systemClassLoader;

  private Thread _mainThread;

  protected Management _management;

  private ResinAdmin _resinAdmin;

  private InputStream _waitIn;
  
  private boolean _isRestart;
  private String _restartMessage;
  private Path _licenseDirectory;
  
  private ResinWaitForExitService _waitForExitService;
  
  private final ArrayList<StartInfoListener> _startInfoListeners
    = new ArrayList<StartInfoListener>();

  /**
   * Creates a new resin server.
   */
  public Resin(String []args)
  {
    this(new ResinArgs(args));
  }

  /**
   * Creates a new resin server.
   */
  public Resin(ResinArgs args)
  {
    _startTime = Alarm.getCurrentTime();
    
    _args = args;
    
    _resinSystem = new ResinSystem(args.getServerId());

    // _licenseErrorMessage = licenseErrorMessage;
    
    _resinLocal.set(this, _resinSystem.getClassLoader());

    Environment.init();
 
    _serverId = args.getServerId();
    
    _resinHome = args.getResinHome();
    
    _resinConf = args.getResinConfPath();
    
    _rootDirectory = args.getRootDirectory();
    _resinDataDirectory = args.getDataDirectory();

    _licenseDirectory = args.getLicenseDirectory();
    
    _homeCluster = args.getHomeCluster();
    _dynamicAddress = args.getServerAddress();
    _dynamicPort = args.getServerPort();
    
    _stage = args.getStage();
 
    _pingSocket = _args.getPingSocket();
    
    preConfigureInit();
    
    if (! isWatchdog())
      configureFile(_resinConf);
  }

  /**
   * Returns the resin server.
   */
  public static Resin getCurrent()
  {
    return _resinLocal.get();
  }

  public ResinSystem getResinSystem()
  {
    return _resinSystem;
  }

  /**
   * Returns the classLoader
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _resinSystem.getClassLoader();
  }
  
  ResinArgs getArgs()
  {
    return _args;
  }

  /**
   * Returns the server id.
   */
  public String getServerId()
  {
    return _serverId;
  }

  /**
   * Sets the server id.
   */
  void setServerId(String serverId)
  {
    if (serverId == null || "".equals(serverId))
      return;//      serverId = "default";

    _serverId = serverId;

    _resinSystem.setId(_serverId);
  }

  public String getUniqueServerName()
  {
    String name;
    
    String serverId = getDisplayServerId();

    if (isWatchdog())
      name = serverId + "_watchdog";
    else
      name = serverId;

    name = name.replace('-', '_');

    return name;
  }

  /**
   * Returns the server id.
   */
  public String getDisplayServerId()
  {
    if (_serverId == null || "".equals(_serverId))
      return "default";
    else
      return _serverId;
  }

  public static String getCurrentServerId()
  {
    Resin resin = getCurrent();

    if (resin != null)
      return resin.getServerId();
    else
      return "";
  }

  /**
   * Returns true for a Resin server, false for a watchdog.
   */
  public boolean isResinServer()
  {
    return ! isWatchdog();
  }

  public boolean isWatchdog()
  {
    return false;
  }

  /**
   * The configuration file used to start the server.
   */
  public Path getResinConf()
  {
    return _resinConf;
  }

  protected String getResinName()
  {
    return getDelegate().getResinName();
  }

  /**
   * Returns resin.home.
   */
  public Path getResinHome()
  {
    return _resinHome;
  }

  /**
   * Set true for Resin pro.
   */
  public boolean isProfessional()
  {
    return getDelegate().isProfessional();
  }
  
  //
  // configuration
  //
  
  public boolean isEmbedded()
  {
    return false;
  }

  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }
  
  public void setRootDirectory(Path path)
  {
    _rootDirectory = path;
  }

  public Path getLicenseDirectory()
  {
    return _licenseDirectory;
  }

  public void setLicenseDirectory(Path licenseDirectory)
  {
    _licenseDirectory = licenseDirectory;
  }

  /**
   * Sets the cluster for a dynamic cluster join.
   */
  void setHomeCluster(String clusterId)
  {
    _homeCluster = clusterId;
  }
  
  /**
   * Returns the cluster to join for a dynamic cluster join.
   */
  public String getHomeCluster()
  {
    return _homeCluster;
  }
  
  public String getResinSystemAuthKey()
  {
    return _resinSystemAuthKey;
  }
  
  void setResinSystemAuthKey(String key)
  {
    _resinSystemAuthKey = key;
  }
  
  /**
   * Returns the server stage.
   */
  public String getStage()
  {
    return _stage;
  }
  
  public String getServerAddress()
  {
    return _dynamicAddress;
  }
  
  public int getServerPort()
  {
    return _dynamicPort;
  }

  public Path getLogDirectory()
  {
    if (_logDirectory != null)
      return _logDirectory;
    else
      return _rootDirectory.lookup("log");
  }

  /**
   * Returns the resin-data directory
   */
  public Path getResinDataDirectory()
  {
    Path path;

    Path root = getRootDirectory();
    
    if (_resinDataDirectory != null)
      root = _resinDataDirectory;

    if (isWatchdog())
      path = root.lookup("watchdog-data");
    else
      path = root.lookup("resin-data");

    if (path instanceof MemoryPath) { // QA
      path = WorkDir.getTmpWorkDir().lookup("qa/resin-data");
    }

    return path;
  }
  
  protected Path getServerDataDirectory()
  {
    synchronized (this) {
      Path dataDirectory = getResinDataDirectory();

      if (_serverDataDirectory == null) {
        String serverName = getDisplayServerId();
  
        _serverDataDirectory = dataDirectory.lookup("./" + serverName);
      }
    }

    return _serverDataDirectory;
  }

  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }
  
  public void setShutdownWaitTime(long shutdownTime)
  {
    _shutdownWaitMax = shutdownTime;
  }
  
  //
  // deprecated configuration
  //

  public Management createResinManagement()
  {
    if (_management == null) 
      _management = new Management(this);

    return _management;
  }

  public StatSystem createStatSystem()
  {
    return getDelegate().createStatSystem();
  }
  
  public LogSystem createLogSystem()
  {
    return getDelegate().createLogSystem();
  }

  /**
   * Returns the initial start time.
   */
  public Date getInitialStartTime()
  {
    return new Date(_initialStartTime);
  }

  /**
   * Returns the start time.
   */
  public Date getStartTime()
  {
    return new Date(_startTime);
  }

  /**
   * Returns the current lifecycle state.
   */
  public LifecycleState getLifecycleState()
  {
    return _lifecycle.getState();
  }

  /**
   * Returns the active server.
   */
  public Server getServer()
  {
    return _servletSystem;
  }

  /**
   * Returns the management api.
   */
  public Management getManagement()
  {
    return _management;
  }
  
  public CloudServer getSelfServer()
  {
    return _selfServer;
  }

  public Server createServer()
  {
    if (_servletSystem == null) {
      configure();

      // _server.start();
    }

    return _servletSystem;
  }
  
  //
  // lifecycle
  //

  /**
   * Returns true if active.
   */
  public boolean isActive()
  {
    return _resinSystem.isActive();
  }

  /**
   * Returns true if the server is closing.
   */
  public boolean isClosing()
  {
    return _lifecycle.isDestroying();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isClosed()
  {
    return _lifecycle.isDestroyed();
  }

  public void addStartInfoListener(StartInfoListener listener)
  {
    _startInfoListeners.add(listener);
  }
  
  void setStartInfo(boolean isRestart, 
                    String startMessage,
                    ExitCode exitCode)
  {
    _isRestart = isRestart;
    _restartMessage = startMessage;
    
    for (StartInfoListener listener : _startInfoListeners) {
      listener.setStartInfo(isRestart, startMessage, exitCode);
    }
  }
  
  public boolean isRestart()
  {
    return _isRestart;
  }
  
  public String getRestartMessage()
  {
    return _restartMessage;
  }
  
  //
  // initialization code
  //
  
  /**
   * Must be called after the Resin.create()
   */
  private void preConfigureInit()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      /*
      String serverName = getServerId();
      if (serverName == null || "".equals(serverName))
        serverName = "default";
      
      _resinSystem = new ResinSystem(serverName);
      */
      
      thread.setContextClassLoader(getClassLoader());

      _resinLocal.set(this, getClassLoader());

      _lifecycle = new Lifecycle(log(), "Resin[]");

      if (getRootDirectory() == null)
        throw new NullPointerException();
      
      _resinDelegate = ResinDelegate.create(this);

      getDelegate().addPreTopologyServices();

      // server/p603
      initRepository();

      // watchdog/0212
      // else
      //  setRootDirectory(Vfs.getPwd());
      
      if (! isWatchdog()) {
        Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
        Environment.addChildLoaderListener(new WebBeansAddLoaderListener());
        Environment.addChildLoaderListener(new EjbEnvironmentListener());
      }

      _bootConfig
        = new BootConfig(_resinSystem,
                         getServerId(),
                         getResinHome(),
                         getRootDirectory(),
                         getLogDirectory(),
                         getResinConf(),
                         isProfessional(),
                         isWatchdog() ? BootType.WATCHDOG : BootType.RESIN);

     _bootResinConfig = _bootConfig.getBootResin();
     
      _resinAdmin = new ResinAdmin(this);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /*
  private String findLocalServerId()
  {
    BootServerConfig server = _bootResinConfig.findLocalServer();
    
    if (server != null)
      return server.getId();
    else
      return null;
  }
  */

  /**
   * Starts the server.
   */
  public void start()
    throws Exception
  {
    if (! _lifecycle.toActive())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_resinSystem.getClassLoader());

      // force a GC on start
      System.gc();

      _servletSystem = createServer();
      
      NetworkListenSystem listenService 
        = _resinSystem.getService(NetworkListenSystem.class);

      if (_args != null) {
        for (BoundPort port : _args.getBoundPortList()) {
          listenService.bind(port.getAddress(),
                             port.getPort(),
                             port.getServerSocket());
        }
      }
      
      _resinSystem.start();

      log().info(this + " started in " + (Alarm.getExactTime() - _startTime) + "ms");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private void initRepository()
  {
    GitSystem.createAndAddService();
    
    LocalRepositoryService localRepositoryService = 
      LocalRepositoryService.createAndAddService();

    RepositorySpi localRepository = localRepositoryService.getRepositorySpi();

    AbstractRepository repository = getDelegate().createRepository(localRepository);
    RepositorySystem.createAndAddService(repository);
    
    // add the cluster repository
    try {
      RepositoryScheme.create("cloud", 
                              getStage() + "/config/resin",
                              getServerDataDirectory().lookup("config"));
    } catch (Exception e) {
      log().log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Starts the server.
   */
  public void stop()
  {
    _resinSystem.stop();
  }

  public void destroy()
  {
    _resinSystem.destroy();
  }
  
  //
  // JMX/Admin
  //

  public ResinAdmin getAdmin()
  {
    return _resinAdmin;
  }

  /**
   * Initialize the server, binding to TCP and starting the threads.
   */
  public void initMain()
    throws Throwable
  {
    _mainThread = Thread.currentThread();
    _mainThread.setContextClassLoader(_systemClassLoader);
    
    // preConfigureInit();

    System.out.println(VersionFactory.getFullVersion());
    System.out.println(VersionFactory.getCopyright());
    System.out.println();

    String licenseMessage = getDelegate().getLicenseMessage();

    if (licenseMessage != null) {
      log().warning(licenseMessage);
      System.out.println(licenseMessage);
    }

    String licenseErrorMessage = getDelegate().getLicenseErrorMessage();

    if (licenseErrorMessage != null) {
      log().warning(licenseErrorMessage);
      System.err.println(licenseErrorMessage);
    }

    System.out.println("Starting " + getResinName()
                       + " on " + QDate.formatLocal(_startTime));
    System.out.println();

    Environment.init();

    // buildResinClassLoader();

    // validateEnvironment();

    Thread thread = Thread.currentThread();

    thread.setContextClassLoader(_systemClassLoader);

    if (_rootDirectory == null)
      _rootDirectory = _resinHome;

    configure();

    start();
  }
  
  private void configure()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_resinSystem.getClassLoader());
      
      if (_servletSystem == null) {
        BootResinConfig bootResin = _bootResinConfig;
  
        configureRootDirectory(bootResin);
        
        initServletSystem();
      }
      
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  public void configureFile(Path path)
  {
    _bootConfig.configureFile(path);
  }
  
  public void configureProgram(ConfigProgram program)
  {
    program.configure(_bootResinConfig);
  }
  
  /**
   * Configures the root directory and dataDirectory.
   */
  private void configureRootDirectory(BootResinConfig bootConfig) 
    throws IOException
  {
    Path dataDirectory = getServerDataDirectory();

    RootDirectorySystem.createAndAddService(_rootDirectory, dataDirectory);
  }
  
  /**
   * Configures the selected server from the boot config.
   */
  private synchronized void initTopology()
  {
    if (_selfServer != null)
      return;
    
    BootResinConfig bootResin = _bootResinConfig;
    
    _resinSystemAuthKey = bootResin.getResinSystemAuthKey();
    
    String serverId = _serverId;
    
    if ("".equals(serverId)) {
      serverId = "default";
    }

    if (serverId != null) {
      _bootServerConfig = bootResin.findServer(serverId);
    }
    
    if (serverId == null) {
      _bootServerConfig = bootResin.findServer("default");
    }

    CloudSystem cloudSystem = bootResin.initTopology();
    
    if (_bootServerConfig != null) {
    }
    else if (isEmbedded()) { 
      _bootServerConfig = joinEmbed();
    }
    else if (Alarm.isTest()) {
      _bootServerConfig = joinTest();
    }
    else if (isWatchdog()) {
      _bootServerConfig = joinWatchdog();
    }
    else if (_serverId != null && getHomeCluster() == null) {
      throw new ConfigException(L().l("-server '{0}' is an unknown server in the configuration file.",
                                      _serverId));
      
    }
    else if ((_bootServerConfig = bootResin.findLocalServer()) != null) {
    }
    
    if (_bootServerConfig == null && getHomeCluster() != null) {
      _bootServerConfig = joinCluster(cloudSystem);
    }
    
    if (_bootServerConfig == null) {
      throw new ConfigException(L().l("unknown server {0} in unknown cluster",
                                      _serverId));
    }
    
    _selfServer = cloudSystem.findServer(_bootServerConfig.getId());
    
    if (_selfServer ==  null)
      throw new ConfigException(L().l("unexpected empty server"));
    
    Config.setProperty("rvar0", _selfServer.getId());
    Config.setProperty("rvar1", _selfServer.getCluster().getId());

    getDelegate().validateServerCluster();
    
    // NetworkClusterSystem.createAndAddService(_selfServer);
    
    // ClusterServer server = _selfServer.getData(ClusterServer.class);
  }
  
  private BootServerConfig joinCluster(CloudSystem cloudSystem)
  {
    String clusterId = _homeCluster;
    
    BootResinConfig bootResin = _bootResinConfig;
    
    BootClusterConfig bootCluster = bootResin.findCluster(clusterId);
    
    if (bootCluster == null) {
      throw new ConfigException(L().l("-cluster {0} is an unknown cluster.",
                                      clusterId));
    }
    
    CloudServer cloudServer = getDelegate().joinCluster(cloudSystem);

    if (cloudServer == null) {
      throw new ConfigException(L().l("unable to join cluster {0}",
                                      clusterId));
    }
    
    return bootCluster.addDynamicServer(cloudServer);
  }
  
  private BootServerConfig joinWatchdog()
  {
    BootResinConfig bootResin = _bootResinConfig;
    
    BootClusterConfig bootCluster = bootResin.addClusterById("watchdog");
    
    BootServerConfig bootServer = bootCluster.createServer();
    bootServer.setId("default");
    bootServer.setAddress("127.0.0.1");
    bootCluster.addServer(bootServer);
    
    bootResin.initTopology();
    
    return bootServer;
  }
  
  private BootServerConfig joinTest()
  {
    BootResinConfig bootResin = _bootResinConfig;
    
    BootClusterConfig bootCluster = findDefaultCluster();
    
    if (bootCluster.getPodList().size() == 0) {
    }
    else if (bootCluster.getPodList().get(0).getServerList().size() == 1) {
      // server/6b07
      return bootCluster.getPodList().get(0).getServerList().get(0);
    }
    else if (bootCluster.getPodList().get(0).getServerList().size() > 0) {
        // server/0342
        return null;
    }
    
    BootServerConfig bootServer = bootCluster.createServer();
    bootServer.setId("default");
    bootServer.setAddress("127.0.0.1");
    bootCluster.addServer(bootServer);
    
    bootResin.initTopology();
    
    return bootServer;
  }
  
  private BootClusterConfig findDefaultCluster()
  {
    BootResinConfig bootResin = _bootResinConfig;
    
    if (bootResin.getClusterList().size() == 1)
      return bootResin.getClusterList().get(0);
    
    return bootResin.findCluster("");
  }
  
  private BootServerConfig joinEmbed()
  {
    BootResinConfig bootResin = _bootResinConfig;
    
    BootClusterConfig bootCluster = null;
    
    if (bootResin.getClusterList().size() == 1)
      bootCluster = bootResin.getClusterList().get(0);
    
    if (bootCluster == null)
      bootCluster = bootResin.findCluster("");

    if (bootCluster == null)
      return null;
    
    if (bootCluster.getPodList().size() == 0) {
      
    }
    else if (bootCluster.getPodList().get(0).getServerList().size() == 1) {
      // server/1e06
      return bootCluster.getPodList().get(0).getServerList().get(0);
    }
    else if (bootCluster.getPodList().get(0).getServerList().size() > 0) {
      // server/0342
      return null;
    }
    
    BootServerConfig bootServer = bootCluster.createServer();
    bootServer.setId("default");
    bootServer.setAddress("127.0.0.1");
    bootCluster.addServer(bootServer);
    
    bootResin.initTopology();
    
    return bootServer;
  }

  private void initClusterNetwork()
  {
    NetworkClusterSystem clusterSystem
      = getDelegate().createNetworkSystem(_selfServer);
    
    NetworkClusterSystem.createAndAddService(clusterSystem);
    
    ClusterServer server = _selfServer.getData(ClusterServer.class);
    
    // initRepository();
    
    LoadBalanceService.createAndAddService(getDelegate().createLoadBalanceFactory());
    
    BamSystem.createAndAddService(server.getBamAdminName());
  }
  
  /**
   * Configures the selected server from the boot config.
   */
  private void initServletSystem()
    throws IOException
  {
    if (_servletSystem != null)
      return;

    initTopology();
    
    initClusterNetwork();
    
    _servletSystem = getDelegate().createServer();

    if (_args != null && _args.getStage() != null)
      _servletSystem.setStage(_args.getStage());
    else if (_stage != null)
      _servletSystem.setStage(_stage);
    
    NetworkListenSystem.createAndAddService(_selfServer);
    
    DeployControllerService.createAndAddService();
    
    if (! isWatchdog()) {
      getDelegate().addServices();
    }
     
    ServletSystem.createAndAddService(_servletSystem);
    
    ResinConfig resinConfig = new ResinConfig(this);
    
    BootResinConfig bootResin = _bootResinConfig;
    
    bootResin.getProgram().configure(resinConfig);
    
    _servletContainerConfig = new ServletContainerConfig(_servletSystem);
    
    BootClusterConfig cluster = _bootServerConfig.getPod().getCluster();

    cluster.getProgram().configure(_servletContainerConfig);
      
    ServerConfig config = new ServerConfig(_servletContainerConfig);
    cluster.getServerDefault().configure(config);
    _bootServerConfig.getServerProgram().configure(config);
    
    _servletContainerConfig.init();
    
    _servletSystem.init();
  }
  
  ResinDelegate getDelegate()
  {
    return _resinDelegate;
  }

  public LicenseCheck getLicenseCheck() {
    return _resinDelegate.getLicenseCheck();
  }
  
  protected boolean loadCloudLicenses()
  {
    try {
      Class<?> cl = Class.forName("com.caucho.cloud.license.LicenseClientImpl");
      LicenseClient client = (LicenseClient) cl.newInstance();
      
      Path licenses = getServerDataDirectory().lookup("licenses");
      
      return client.loadLicenses(licenses, _selfServer.getPod());
    } catch (ClassNotFoundException e) {
      log().log(Level.ALL, e.toString(), e);
    } catch (Exception e) {
      log().log(Level.FINER, e.toString(), e);
    }
    
    return false;
  }

  /**
   * Returns the admin broker
   */
  public Broker getAdminBroker()
  {
    return _management.getAdminBroker();
  }

  protected String getLicenseMessage()
  {
    return getDelegate().getLicenseMessage();
  }

  protected String getLicenseErrorMessage()
  {
    return getDelegate().getLicenseMessage();
  }
  
  public ServletContainerConfig getServletContainerConfig()
  {
    return _servletContainerConfig;
  }

  //
  // statistics
  //
  
  public double getCpuLoad()
  {
    return 0;
  }

  /**
   * Thread to wait until Resin should be stopped.
   */
  public void waitForExit()
    throws IOException
  {
    _waitForExitService = new ResinWaitForExitService(this, _resinSystem,
                                                      _waitIn, _pingSocket);
    
    _waitForExitService.startResinActor();
    
    _waitForExitService.waitForExit();
  }
  
  /**
   * Called from the embedded server
   */
  public void close()
  {
    log().info("Resin closed from the embedded server");
    
    _resinSystem.destroy();
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    
    if (isProfessional())
      sb.append("Pro");

    sb.append(getClass().getSimpleName());
    
    sb.append("[id=" + getDisplayServerId() + "]");
    
    return sb.toString();
  }

  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.xml   : alternate configuration file
   * -port port        : set the server's portt
   * <pre>
   */
  public static void main(String []argv)
  {
    try {
      Environment.init();
      
      validateEnvironment();
      
      final Resin resin = new Resin(argv);

      resin.initMain();

      resin.getServer();

      resin.waitForExit();

      if (! resin.isClosing()) {
        ShutdownSystem.shutdownActive(ExitCode.FAIL_SAFE_HALT,
                                       "Resin shutdown from unknown reason");
      }
    } catch (Throwable e) {
      Throwable cause;

      e.printStackTrace();
      
      for (cause = e;
           cause != null && cause.getCause() != null;
           cause = cause.getCause()) {
        if (cause instanceof CompileException) {
          break;
        }
      }

      if (cause instanceof BindException) {
        System.err.println(e.getMessage());

        log().severe(e.toString());

        log().log(Level.FINE, e.toString(), e);

        System.exit(ExitCode.BIND.ordinal());
      }
      else if (e instanceof ConfigException) {
        System.err.println(e.getMessage());

        log().log(Level.CONFIG, e.toString(), e);
        
        System.exit(ExitCode.BAD_CONFIG.ordinal());
      }
      else {
        System.err.println(e.getMessage());

        log().log(Level.WARNING, e.toString(), e);
        
        e.printStackTrace(System.err);
      }
    } finally {
      System.exit(ExitCode.UNKNOWN.ordinal());
    }
  }

  /**
   * Validates the environment.
   */
  private static void validateEnvironment()
    throws ConfigException
  {
    String loggingManager = System.getProperty("java.util.logging.manager");

    if (loggingManager == null
        || ! loggingManager.equals("com.caucho.log.LogManagerImpl")) {
      log().warning(L().l("The following system property must be set:\n  -Djava.util.logging.manager=com.caucho.log.LogManagerImpl\nThe JDK 1.4 Logging manager must be set to Resin's log manager."));
    }

    /*
    validatePackage("javax.servlet.Servlet", new String[] {"2.5", "1.5"});
    validatePackage("javax.servlet.jsp.jstl.core.Config", new String[] {"1.1"});
    validatePackage("javax.management.MBeanServer", new String[] { "1.2", "1.5" });
    validatePackage("javax.resource.spi.ResourceAdapter", new String[] {"1.5", "1.4"});
    */
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(Resin.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(Resin.class.getName());

    return _log;
  }
}
