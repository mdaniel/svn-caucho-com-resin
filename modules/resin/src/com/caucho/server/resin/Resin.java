/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.management.ObjectName;

import com.caucho.VersionFactory;
import com.caucho.bam.broker.Broker;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.loadbalance.LoadBalanceFactory;
import com.caucho.cloud.loadbalance.LoadBalanceService;
import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.network.NetworkListenSystem;
import com.caucho.cloud.security.SecurityService;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.cloud.topology.CloudSystem;
import com.caucho.cloud.topology.TopologyService;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.functions.FmtFunctions;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.WebBeansAddLoaderListener;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.config.program.ConfigProgram;
import com.caucho.ejb.manager.EjbEnvironmentListener;
import com.caucho.env.deploy.DeployControllerService;
import com.caucho.env.git.GitSystem;
import com.caucho.env.health.HealthStatusService;
import com.caucho.env.jpa.ListenerPersistenceEnvironment;
import com.caucho.env.lock.*;
import com.caucho.env.log.LogSystem;
import com.caucho.env.repository.AbstractRepository;
import com.caucho.env.repository.LocalRepositoryService;
import com.caucho.env.repository.RepositorySystem;
import com.caucho.env.repository.RepositorySpi;
import com.caucho.env.service.*;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownSystem;
import com.caucho.env.vfs.RepositoryScheme;
import com.caucho.env.warning.WarningService;
import com.caucho.java.WorkDir;
import com.caucho.license.LicenseCheck;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ResinMXBean;
import com.caucho.management.server.ThreadPoolMXBean;
import com.caucho.naming.Jndi;
import com.caucho.server.admin.Management;
import com.caucho.server.admin.StatSystem;
import com.caucho.server.cache.TempFileService;
import com.caucho.server.cluster.ClusterPod;
import com.caucho.server.cluster.Server;
import com.caucho.server.cluster.ServerConfig;
import com.caucho.server.cluster.ServletContainerConfig;
import com.caucho.server.cluster.ServletSystem;
import com.caucho.server.distcache.CacheStoreManager;
import com.caucho.server.distcache.DistCacheSystem;
import com.caucho.server.resin.ResinArgs.BoundPort;
import com.caucho.server.webbeans.ResinCdiProducer;
import com.caucho.server.webbeans.ResinServerConfigLibrary;
import com.caucho.util.Alarm;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

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

  private final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  private boolean _isEmbedded;

  private String _serverId = "default";
  private final boolean _isWatchdog;
  
  private String _stage = "production";
  private String _dynamicJoinCluster;
  
  private String _dynamicAddress;
  private int _dynamicPort;
  
  private String _user;
  private String _password;
  
  private ResinArgs _args;

  private Path _resinHome;
  private Path _rootDirectory;

  private Path _resinDataDirectory;
  private Path _logDirectory;
  
  private final ResinSystem _resinSystem;
  
  private long _shutdownWaitMax = 60000L;

  private Lifecycle _lifecycle;

  private BootResinConfig _bootResinConfig;
  private CloudServer _selfServer;
  
  private ServletContainerConfig _servletContainerConfig;
  private Server _servletContainer;

  private long _initialStartTime;
  private long _startTime;
  
  private boolean _isRestart;
  private String _restartMessage;

  private String _licenseErrorMessage;

  private Path _resinConf;

  private ClassLoader _systemClassLoader;

  private Thread _mainThread;

  protected Management _management;

  private ThreadPoolAdmin _threadPoolAdmin;
  private ObjectName _objectName;
  private ResinAdmin _resinAdmin;

  private InputStream _waitIn;

  private Socket _pingSocket;
  
  private ResinWaitForExitService _waitForExitService;
  
  private final ArrayList<StartInfoListener> _startInfoListeners
    = new ArrayList<StartInfoListener>();

  /**
   * Creates a new resin server.
   */
  protected Resin(ResinSystem system, boolean isWatchdog)
  {
    this(system, isWatchdog, null);
  }

  /**
   * Creates a new resin server.
   */
  protected Resin(ResinSystem system,
                  boolean isWatchdog,
                  String licenseErrorMessage)
  {
    _startTime = Alarm.getCurrentTime();

    _isWatchdog = isWatchdog;
    _licenseErrorMessage = licenseErrorMessage;

    // DynamicClassLoader.setJarCacheEnabled(true);
    Environment.init();
    
    _resinSystem = system;

    initEnvironment();

    try {
      URL.setURLStreamHandlerFactory(ResinURLStreamHandlerFactory.create());
    } catch (java.lang.Error e) {
      //operation permitted once per jvm; catching for harness.
    }
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin create(String id)
  {
    ResinSystem system = new ResinSystem(id);
    
    return create(system, false);
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createWatchdog(ResinSystem system)
  {
    Resin resin = create(system, true);

    return resin;
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin create(ResinSystem system, boolean isWatchdog)
  {
    String licenseErrorMessage = null;

    Resin resin = null;

    try {
      Class<?> cl = Class.forName("com.caucho.server.resin.ProResin");
      Constructor<?> ctor = cl.getConstructor(new Class[] { ResinSystem.class, boolean.class });

      resin = (Resin) ctor.newInstance(system, isWatchdog);
    } catch (ConfigException e) {
      log().log(Level.FINER, e.toString(), e);

      licenseErrorMessage = e.getMessage();
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();

      log().log(Level.FINER, cause.toString(), cause);

      if (cause instanceof ConfigException) {
        licenseErrorMessage = cause.getMessage();
      }
      else {
        licenseErrorMessage= L().l("  Using Resin(R) Open Source under the GNU Public License (GPL).\n"
                                   + "\n"
                                   + "  See http://www.caucho.com for information on Resin Professional,\n"
                                   + "  including caching, clustering, JNI acceleration, and OpenSSL integration.\n"
                                   + "\n  Exception=" + cause + "\n");
      }
    } catch (Throwable e) {
      log().log(Level.FINER, e.toString(), e);

      String causeMsg = "";
      if (! (e instanceof ClassNotFoundException)) {
        causeMsg = "\n  Exception=" + e + "\n";
      }


      String msg = L().l("  Using Resin(R) Open Source under the GNU Public License (GPL).\n"
                         + "\n"
                         + "  See http://www.caucho.com for information on Resin Professional,\n"
                         + "  including caching, clustering, JNI acceleration, and OpenSSL integration.\n"
                         + causeMsg);

      licenseErrorMessage = msg;
    }

    if (resin == null) {
      try {
        Class<?> cl = Class.forName("com.caucho.license.LicenseCheckImpl");
        LicenseCheck license = (LicenseCheck) cl.newInstance();

        license.requirePersonal(1);

        licenseErrorMessage = license.doLogging();
      } catch (ConfigException e) {
        licenseErrorMessage = e.getMessage();
      } catch (Throwable e) {
        // message should already be set above
      }

      resin = new Resin(system, isWatchdog, licenseErrorMessage);
    }

    _resinLocal.set(resin, system.getClassLoader());

    // resin.initEnvironment();

    return resin;
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createOpenSource(String id)
  {
    ResinSystem system = new ResinSystem(id);
    
    return createOpenSource(system);
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createOpenSource(ResinSystem system)
  {
    return new Resin(system, false, null);
  }

  /**
   * Returns the resin server.
   */
  public static Resin getLocal()
  {
    return _resinLocal.get();
  }

  /**
   * Returns the resin server.
   */
  public static Resin getCurrent()
  {
    return getLocal();
  }

  public ResinSystem getResinSystem()
  {
    return _resinSystem;
  }
  
  public CloudSystem getCloudSystem()
  {
    ResinSystem resinSystem = _resinSystem;
    
    if (resinSystem != null)
      return resinSystem.getService(TopologyService.class).getSystem();
    else
      return null;
  }
  
  public void setRootDirectory(Path path)
  {
    _rootDirectory = path;
  }
  
  public void setPingSocket(Socket socket)
  {
    _pingSocket = socket;
  }
  
  public void setEmbedded(boolean isEmbedded)
  {
    _isEmbedded = isEmbedded;
  }
  
  public boolean isEmbedded()
  {
    return _isEmbedded;
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
  
  public void setDataDirectory(Path path)
  {
    _resinDataDirectory = path;
  }
  
  public String getRestartMessage()
  {
    return _restartMessage;
  }
  
  private void initEnvironment()
  {
    String resinHome = System.getProperty("resin.home");

    if (resinHome != null)
      _resinHome = Vfs.lookup(resinHome);
    else
      _resinHome = Vfs.getPwd();

    _rootDirectory = _resinHome;

    // server.root backwards compat
    String resinRoot = System.getProperty("server.root");

    if (resinRoot != null)
      _rootDirectory = Vfs.lookup(resinRoot);

    // resin.root backwards compat
    resinRoot = System.getProperty("resin.root");

    if (resinRoot != null)
      _rootDirectory = Vfs.lookup(resinRoot);
  }
  
  /**
   * Must be called after the Resin.create()
   */
  public void preConfigureInit()
  {
    if (_lifecycle != null)
      return;

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

      // default server id
      if (_args != null) {
        setServerId(_args.getServerId());

        if (_args.getRootDirectory() != null)
          setRootDirectory(_args.getRootDirectory());

        if (_args.getDataDirectory() != null)
          setDataDirectory(_args.getDataDirectory());

        _pingSocket = _args.getPingSocket();
        
        setJoinCluster(_args.getJoinCluster());
        setServerAddress(_args.getServerAddress());
        setServerPort(_args.getServerPort());
        
        if (_args.getStage() != null)
          setStage(_args.getStage());
      }
      
      if (getRootDirectory() == null)
        throw new NullPointerException();
      
      addPreTopologyServices();
      
      _bootResinConfig = new BootResinConfig(this);

      // watchdog/0212
      // else
      //  setRootDirectory(Vfs.getPwd());

      Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
      Environment.addChildLoaderListener(new WebBeansAddLoaderListener());
      Environment.addChildLoaderListener(new EjbEnvironmentListener());
      InjectManager cdiManager = InjectManager.create();
      
      ResinVar resinVar = new ResinVar(getServerId(),
                                       getResinHome(),
                                       getRootDirectory(),
                                       getLogDirectory(),
                                       getResinConf(),
                                       isProfessional(),
                                       null);

      Config.setProperty("resinHome", getResinHome());
      Config.setProperty("resin", resinVar);
      Config.setProperty("server", resinVar);
      Config.setProperty("java", new JavaVar());
      Config.setProperty("system", System.getProperties());
      Config.setProperty("getenv", System.getenv());
      // server/4342
      Config.setProperty("server_id", getServerId());

      // _management = createResinManagement();

      if (cdiManager.getBeans(ResinCdiProducer.class).size() == 0) {
        Config.setProperty("fmt", new FmtFunctions());

        ResinConfigLibrary.configure(cdiManager);
        ResinServerConfigLibrary.configure(cdiManager);

        try {
          Method method = Jndi.class.getMethod("lookup", new Class[] { String.class });
          Config.setProperty("jndi", method);
          Config.setProperty("jndi:lookup", method);
        } catch (Exception e) {
          throw ConfigException.create(e);
        }

        cdiManager.addManagedBean(cdiManager.createManagedBean(ResinCdiProducer.class));
        Class<?> resinValidatorClass = ResinCdiProducer.createResinValidatorProducer();
        
        if (resinValidatorClass != null)
          cdiManager.addManagedBean(cdiManager.createManagedBean(resinValidatorClass));

        cdiManager.update();
      }

      _threadPoolAdmin = ThreadPoolAdmin.create();
      _resinAdmin = new ResinAdmin(this);

      _threadPoolAdmin.register();

      MemoryAdmin.create();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  protected void addPreTopologyServices()
  {
    WarningService.createAndAddService();
    
    ShutdownSystem.createAndAddService(_isEmbedded);
    
    HealthStatusService.createAndAddService();
    
    TopologyService.createAndAddService(_serverId);
    
    SecurityService.createAndAddService();

    createDistCacheService();
  }
  
  protected void addServices()
  {
    TempFileService.createAndAddService();
    
    // LockService.createAndAddService(createLockManager());
  }
  
  protected DistCacheSystem createDistCacheService()
  {
    return DistCacheSystem.
      createAndAddService(new CacheStoreManager(getResinSystem()));
  }
  
  /*
  protected AbstractLockManager createLockManager()
  {
    return new SingleLockManager();
  }
  */
  
  private void setArgs(ResinArgs args)
  {
    _args = args;
  }

  /**
   * Returns the classLoader
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _resinSystem.getClassLoader();
  }

  public ObjectName getObjectName()
  {
    return _objectName;
  }

  public ResinMXBean getAdmin()
  {
    return _resinAdmin;
  }

  /**
   * Returns the admin broker
   */
  public Broker getAdminBroker()
  {
    return _management.getAdminBroker();
  }

  public ThreadPoolMXBean getThreadPoolAdmin()
  {
    return _threadPoolAdmin;
  }

  protected String getLicenseMessage()
  {
    return null;
  }

  protected String getLicenseErrorMessage()
  {
    return _licenseErrorMessage;
  }

  /**
   * Sets the server id.
   */
  public void setServerId(String serverId)
  {
    if (serverId == null || "".equals(serverId))
      serverId = "default";
    
    //Config.setProperty("serverId", serverId);
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    Config.setProperty("serverId", serverId, classLoader);
    Config.setProperty("server_id", serverId, classLoader);

    _serverId = serverId;
    _serverIdLocal.set(serverId);
  }

  /**
   * Returns the server id.
   */
  public String getServerId()
  {
    return _serverId;
  }

  /**
   * Returns true for a Resin server, false for a watchdog.
   */
  public boolean isResinServer()
  {
    return ! _isWatchdog;
  }

  public boolean isWatchdog()
  {
    return _isWatchdog;
  }

  public String getUniqueServerName()
  {
    String name;

    if (_isWatchdog)
      name = _serverId + "_watchdog";
    else
      name = _serverId;

    name = name.replace('-', '_');

    return name;
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
   * Sets the cluster for a dynamic cluster join.
   */
  public void setJoinCluster(String clusterId)
  {
    _dynamicJoinCluster = clusterId;
    
    if ("null".equals(clusterId))
      Thread.dumpStack();
  }
  
  /**
   * Returns the cluster to join for a dynamic cluster join.
   */
  public String getJoinCluster()
  {
    return _dynamicJoinCluster;
  }
  
  /**
   * Sets the server stage.
   */
  public void setStage(String stage)
  {
    _stage = stage;
  }
  
  /**
   * Returns the server stage.
   */
  public String getStage()
  {
    return _stage;
  }

  /**
   * Sets the IP cluster address for the dynamic server.
   */
  public void setServerAddress(String address)
  {
    _dynamicAddress= address;
  }
  
  public String getServerAddress()
  {
    return _dynamicAddress;
  }

  /**
   * Sets the TCP cluster port for the dynamic server.
   */
  public void setServerPort(int port)
  {
    _dynamicPort = port;
  }
  
  public int getServerPort()
  {
    return _dynamicPort;
  }
  
  public void setUser(String user)
  {
    _user = user;
  }
  
  public String getUser()
  {
    return _user;
  }
  
  public void setPassword(String password)
  {
    _password = password;
  }
  
  public String getPassword()
  {
    return _password;
  }

  /**
   * Returns the server id.
   */
  public String getDisplayServerId()
  {
    if ("".equals(_serverId))
      return "default";
    else
      return _serverId;
  }

  /**
   * Sets the config file.
   */
  public void setConfigFile(String configFile)
  {
  }

  /**
   * Sets resin.home
   */
  public void setResinHome(Path home)
  {
    _resinHome = home;
  }

  /**
   * Returns resin.home.
   */
  public Path getResinHome()
  {
    return _resinHome;
  }

  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
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

    if (_isWatchdog)
      path = root.lookup("watchdog-data");
    else
      path = root.lookup("resin-data");

    if (path instanceof MemoryPath) { // QA
      path = WorkDir.getTmpWorkDir().lookup("qa/resin-data");
    }

    return path;
  }

  /**
   * Sets the admin directory
   */
  public void setAdminPath(Path path)
  {
    // setResinDataDirectory(path);
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
    return "Resin";
  }

  /**
   * Set true for Resin pro.
   */
  public boolean isProfessional()
  {
    return false;
  }
  
  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }
  
  public void setShutdownWaitTime(long shutdownTime)
  {
    _shutdownWaitMax = shutdownTime;
  }
  
  /**
   * Returns the cluster names.
   */
  public ClusterMXBean []getClusters()
  {
    /*
    ClusterMXBean []clusters = new ClusterMXBean[_clusters.size()];

    for (int i = 0; i < _clusters.size(); i++)
      clusters[i] = _clusters.get(i).getAdmin();

    return clusters;
    */
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the initial start time.
   */
  void setInitialStartTime(long now)
  {
    _initialStartTime = now;
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
   * Initialize the server.
   */
  @PostConstruct
  public void init()
  {
    preConfigureInit();

    _lifecycle.toInit();
  }

  /**
   * Returns the active server.
   */
  public Server getServer()
  {
    return _servletContainer;
  }

  /**
   * Returns the management api.
   */
  public Management getManagement()
  {
    return _management;
  }
  
  public double getCpuLoad()
  {
    return 0;
  }
  
  public CloudServer getSelfServer()
  {
    return _selfServer;
  }

  public Server createServer()
  {
    if (_servletContainer == null) {
      configure();

      // _server.start();
    }

    return _servletContainer;
  }

  protected ClusterServer loadDynamicServer(ClusterPod pod,
                                            String dynId,
                                            String dynAddress,
                                            int dynPort)
  {
    throw new ConfigException(L().l("dynamic-server requires Resin Professional"));
  }

  /**
   * Starts the server.
   */
  public void start()
    throws Exception
  {
    preConfigureInit();

    if (! _lifecycle.toActive())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_resinSystem.getClassLoader());

      // force a GC on start
      System.gc();

      _servletContainer = createServer();
      
      NetworkListenSystem listenService 
        = _resinSystem.getService(NetworkListenSystem.class);

      if (_args != null) {
        for (BoundPort port : _args.getBoundPortList()) {
          listenService.bind(port.getAddress(),
                             port.getPort(),
                             port.getServerSocket());
        }
      }
      
      if (_management != null)
        _management.init();
      

      _resinSystem.start();

      /*
        if (! hasListeningPort()) {
        log().warning(L().l("-server \"{0}\" has no matching http or srun ports.  Check the resin.xml and -server values.",
        _serverId));
        }
      */

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

    AbstractRepository repository = createRepository(localRepository);
    RepositorySystem.createAndAddService(repository);
    
    // add the cluster repository
    try {
      RepositoryScheme.create("cluster-config", 
                              getStage() + "/config/resin",
                              RootDirectorySystem.getCurrentDataDirectory().lookup("config"));
    } catch (Exception e) {
      log().log(Level.WARNING, e.toString(), e);
    }
  }
  
  protected AbstractRepository createRepository(RepositorySpi localRepository)
  {
    return (AbstractRepository) localRepository;
  }

  /**
   * Starts the server.
   */
  public void stop()
  {
    _resinSystem.stop();
  }

  /**
   * Dump threads for debugging
   */
  public void dumpThreads()
  {
  }

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

  public void destroy()
  {
    _resinSystem.destroy();
  }

  /**
   * Initialize the server, binding to TCP and starting the threads.
   */
  public void initMain()
    throws Throwable
  {
    _mainThread = Thread.currentThread();
    _mainThread.setContextClassLoader(_systemClassLoader);

    preConfigureInit();

    addRandom();

    System.out.println(VersionFactory.getFullVersion());
    System.out.println(VersionFactory.getCopyright());
    System.out.println();

    String licenseMessage = getLicenseMessage();

    if (licenseMessage != null) {
      log().warning(licenseMessage);
      System.out.println(licenseMessage);
    }

    String licenseErrorMessage = getLicenseErrorMessage();

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
      
      if (_servletContainer == null) {
        BootResinConfig bootResin = configureBoot();
  
        _rootDirectory = bootResin.getRootDirectory();
  
        configureRoot(bootResin);
        
        configureServer();
      }
      
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Configures the boot structure, which parses the clusters and servers
   * for the system topology.
   */
  private BootResinConfig configureBoot()
  {
    Vfs.setPwd(_rootDirectory);
    
    Path resinConf = null;
    
    if (_args != null)
      resinConf = _args.getResinConfPath();

    _resinConf = resinConf;

    // server.setServerRoot(_serverRoot);

    Vfs.setPwd(getRootDirectory());

    if (resinConf != null)
      configureFile(resinConf);

    return _bootResinConfig;
  }
  
  public void configureFile(Path resinConf)
  {
    BootResinConfig bootResin = _bootResinConfig;
    
    Config config = new Config();
    // server/10hc
    // config.setResinInclude(true);

    config.configure(bootResin, resinConf, bootResin.getSchema());
  }
  
  public void configureProgram(ConfigProgram program)
  {
    program.configure(_bootResinConfig);
  }
  
  /**
   * Configures the root directory and dataDirectory.
   */
  private void configureRoot(BootResinConfig bootConfig) 
    throws IOException
  {
    Path dataDirectory = getResinDataDirectory();
  
    String serverName = _serverId;
  
    if (serverName == null || serverName.isEmpty())
      serverName = "default";
  
    dataDirectory = dataDirectory.lookup("./" + serverName);

    RootDirectorySystem.createAndAddService(_rootDirectory, dataDirectory);
  }
  
  /**
   * Configures the selected server from the boot config.
   */
  private void configureServer()
    throws IOException
  {
    if (_servletContainer != null)
      return;
    
    BootResinConfig bootResin = _bootResinConfig;
    
    bootResin.configureServers();
    
    String clusterId = "";
    
    if (_dynamicJoinCluster != null) {
      clusterId = _dynamicJoinCluster;
      
      CloudServer cloudServer = joinCluster(bootResin.getCloudSystem());

      if (cloudServer != null) {
        clusterId = cloudServer.getCluster().getId();
      }
    }
    
    
    String serverId = _serverId;
    
    BootServerConfig bootServer = bootResin.findServer(serverId);
    
    if (bootServer == null) {
      /*
      if (_serverId != null 
          && ! "".equals(_serverId)
          && ! "default".equals(_serverId)
          && ! isWatchdog()
          && _dynamicJoinCluster == null)
        throw new ConfigException(L().l("-server '{0}' is an unknown server in the configuration file.",
                                        _serverId));
                                        */
      
      BootClusterConfig clusterConfig;
      
      clusterConfig = bootResin.findCluster(clusterId);
      
      if (clusterConfig != null) {
      }
      else if (bootResin.getClusterList().size() == 0) {
        clusterConfig = bootResin.createCluster();
        clusterConfig.setId(clusterId);
        clusterConfig.init();
      }
      else if (serverId != null) {
        throw new ConfigException(L().l("'{0}' is an unknown server in the configuration file.",
                                        serverId));
      }
      else {
          throw new ConfigException(L().l("'{0}' is an unknown cluster in the configuration file.",
                                          clusterId));
      }
      
      /*
      if (clusterConfig.getPodList().size() > 0) {
        throw new ConfigException(L().l("'{0}' is an unknown server in the configuration file.",
                                        _serverId));
      }
      */
      
      bootServer = clusterConfig.createServer();
      bootServer.setId(serverId); // getServerId());
      
      if (_dynamicJoinCluster != null)
        bootServer.setDynamic(true);
      
      bootServer.init();
      clusterConfig.addServer(bootServer);
      // bootServer.configureServer();
    }
    
    _selfServer = bootServer.getCloudServer();
    
    validateServerCluster();
    
    NetworkClusterSystem networkService = 
      NetworkClusterSystem.createAndAddService(_selfServer);
    
    ClusterServer server = _selfServer.getData(ClusterServer.class);
    
    LoadBalanceService.createAndAddService(createLoadBalanceFactory());
    
    BamSystem.createAndAddService(server.getBamAdminName());
    
    DeployControllerService.createAndAddService();
    
    initRepository();
   
    _servletContainer = createServer(networkService);

    if (_args != null && _args.getStage() != null)
      _servletContainer.setStage(_args.getStage());
    else if (_stage != null)
      _servletContainer.setStage(_stage);
    
    NetworkListenSystem.createAndAddService(_selfServer);
    
    if (! isWatchdog()) {
      addServices();
    }
     
    ServletSystem.createAndAddService(_servletContainer);
    
    ResinConfig resinConfig = new ResinConfig(this);
    
    bootResin.getProgram().configure(resinConfig);
    
    _servletContainerConfig = new ServletContainerConfig(_servletContainer);
    
    BootClusterConfig cluster = bootServer.getPod().getCluster();

    cluster.getProgram().configure(_servletContainerConfig);
    
    ServerConfig config = new ServerConfig(_servletContainerConfig);
    cluster.getServerDefault().configure(config);
    bootServer.getServerProgram().configure(config);
    
    _servletContainerConfig.init();
    
    _servletContainer.init();
  }
  
  protected void validateServerCluster()
  {
    if (_selfServer.getPod().getServerLength() != 1) {
      throw new ConfigException(L().l("{0} does not support multiple <server> instances in a cluster.\nFor clustered servers, please use Resin Professional with a valid license.",
                                    this));
    }
  }
  
  protected CloudServer joinCluster(CloudSystem system)
  {
    throw new ConfigException(L().l("-join-cluster requires Resin Professional"));
  }
  
  public ServletContainerConfig getServletContainerConfig()
  {
    return _servletContainerConfig;
  }
  
  protected LoadBalanceFactory createLoadBalanceFactory()
  {
    return new LoadBalanceFactory();
  }
  
  protected Server createServer(NetworkClusterSystem clusterService)
  {
    return new Server(this, _resinSystem, clusterService);
  }

  public Management createResinManagement()
  {
    if (_management == null) 
      _management = new Management(this);

    return _management;
  }

  public StatSystem createStatSystem() {
    throw new ConfigException("StatSystem is available with Resin Professional");
  }
  
  public LogSystem createLogSystem() {
    throw new ConfigException("LogSystem is available with Resin Professional");
  }

  private void addRandom()
  {
  }

  public void dumpHeapOnExit()
  {

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

  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + _serverId + "]";
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
      
      ResinArgs args = new ResinArgs(argv);

      ResinSystem system = new ResinSystem(args.getServerId());
      
      final Resin resin = Resin.create(system, false);

      resin.setArgs(args);

      resin.initMain();

      resin.getServer();

      resin.waitForExit();

      if (! resin.isClosing()) {
        ShutdownSystem.shutdownActive(ExitCode.FAIL_SAFE_HALT,
                                       "Resin shutdown from unknown reason");
      }
    } catch (Throwable e) {
      Throwable cause;

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
