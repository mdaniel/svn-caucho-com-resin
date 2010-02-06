/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.management.ObjectName;

import com.caucho.VersionFactory;
import com.caucho.amber.manager.PersistenceEnvironmentListener;
import com.caucho.bam.Broker;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.config.functions.FmtFunctions;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.WebBeansAddLoaderListener;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.hemp.broker.HempBrokerManager;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.license.LicenseCheck;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.EnvironmentProperties;
import com.caucho.log.EnvironmentStream;
import com.caucho.log.RotateStream;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ResinMXBean;
import com.caucho.management.server.ThreadPoolMXBean;
import com.caucho.naming.Jndi;
import com.caucho.repository.ModuleRepository;
import com.caucho.server.admin.Management;
import com.caucho.server.admin.TransactionManager;
import com.caucho.server.cache.TempFileManager;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterPod;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.Server;
import com.caucho.server.cluster.SingleCluster;
import com.caucho.server.repository.ModuleRepositoryImpl;
import com.caucho.server.util.JniCauchoSystem;
import com.caucho.server.webbeans.ResinWebBeansProducer;
import com.caucho.util.Alarm;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.util.Shutdown;
import com.caucho.util.ThreadPool;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.QJniServerSocket;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class Resin extends Shutdown implements EnvironmentBean, SchemaBean
{
  private static Logger _log;
  private static L10N _L;

  public static final int EXIT_OK = 0;
  public static final int EXIT_HALT = 3;

  private static final EnvironmentLocal<Resin> _resinLocal
    = new EnvironmentLocal<Resin>();

  private final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  private EnvironmentClassLoader _classLoader;
  private boolean _isGlobal;

  private String _serverId = "";
  private final boolean _isWatchdog;
  private DynamicServer _dynamicServer;

  private ClusterPod _dynPod;
  private String _dynCluster;
  private String _dynAddress;
  private int _dynPort;

  private Path _resinHome;
  private Path _rootDirectory;

  private Path _resinDataDirectory;

  private long _minFreeMemory = 2 * 1024L * 1024L;
  private long _shutdownWaitMax = 60000L;

  private SecurityManager _securityManager;

  private ArrayList<ConfigProgram> _clusterDefaults
    = new ArrayList<ConfigProgram>();

  private ArrayList<Cluster> _clusters
    = new ArrayList<Cluster>();

  private Lifecycle _lifecycle;

  private Server _server;

  private long _initialStartTime;
  private long _startTime;

  private String _licenseErrorMessage;

  private String _configFile;
  private Path _resinConf;

  private ClassLoader _systemClassLoader;

  private Thread _mainThread;
  private FailSafeHaltThread _failSafeHaltThread;
  private ShutdownThread _shutdownThread;

  private ArrayList<BoundPort> _boundPortList
    = new ArrayList<BoundPort>();

  protected Management _management;

  private ModuleRepositoryImpl _repository = new ModuleRepositoryImpl();
  private TempFileManager _tempFileManager;

  private ThreadPoolAdmin _threadPoolAdmin;
  private ObjectName _objectName;
  private ResinAdmin _resinAdmin;
  private ResinActor _resinActor;

  private InputStream _waitIn;

  private Socket _pingSocket;

  private String _stage = null;
  private boolean _isDumpHeapOnExit;

  /**
   * Creates a new resin server.
   */
  protected Resin(ClassLoader loader, boolean isWatchdog)
  {
    this(loader, isWatchdog, null);
  }

  /**
   * Creates a new resin server.
   */
  protected Resin(ClassLoader loader,
                  boolean isWatchdog,
                  String licenseErrorMessage)
  {
    _startTime = Alarm.getCurrentTime();

    _isWatchdog = isWatchdog;
    _licenseErrorMessage = licenseErrorMessage;

    // DynamicClassLoader.setJarCacheEnabled(true);
    Environment.init();

    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();

    _isGlobal = (loader == ClassLoader.getSystemClassLoader());

    if (loader instanceof EnvironmentClassLoader)
      _classLoader = (EnvironmentClassLoader) loader;
    else
      _classLoader = EnvironmentClassLoader.create();
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin create()
  {
    return create(Thread.currentThread().getContextClassLoader(), false);
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createWatchdog()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    Resin resin = create(loader, true);

    return resin;
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin create(ClassLoader loader, boolean isWatchdog)
  {
    String licenseErrorMessage = null;

    Resin resin = null;

    try {
      Class<?> cl = Class.forName("com.caucho.server.resin.ProResin");
      Constructor<?> ctor = cl.getConstructor(new Class[] { ClassLoader.class, boolean.class });

      resin = (Resin) ctor.newInstance(loader, isWatchdog);
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
        String msg = L().l("  Using Resin(R) Open Source under the GNU Public License (GPL).\n"
                           + "\n"
                           + "  See http://www.caucho.com for information on Resin Professional,\n"
                           + "  including caching, clustering, JNI acceleration, and OpenSSL integration.\n"
                           + "  Exception=" + cause);
      }
    } catch (Throwable e) {
      log().log(Level.FINER, e.toString(), e);

      String msg = L().l("  Using Resin(R) Open Source under the GNU Public License (GPL).\n"
                         + "\n"
                         + "  See http://www.caucho.com for information on Resin Professional,\n"
                         + "  including caching, clustering, JNI acceleration, and OpenSSL integration.\n"
                         + "  Exception=" + e);

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

      resin = new Resin(loader, isWatchdog, licenseErrorMessage);
    }

    _resinLocal.set(resin, loader);

    // resin.initEnvironment();

    return resin;
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createOpenSource()
  {
    return createOpenSource(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createOpenSource(ClassLoader loader)
  {
    return new Resin(loader, false, null);
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
      thread.setContextClassLoader(_classLoader);

      _resinLocal.set(this, _classLoader);

      _lifecycle = new Lifecycle(log(), "Resin[]");

      String resinHome = System.getProperty("resin.home");

      if (resinHome != null)
        setResinHome(Vfs.lookup(resinHome));
      else
        setResinHome(Vfs.getPwd());

      if (getRootDirectory() == null) {
        setRootDirectory(getResinHome());

        // server.root backwards compat
        String serverRoot = System.getProperty("server.root");

        if (serverRoot != null)
          setRootDirectory(Vfs.lookup(serverRoot));

        // resin.root backwards compat
        serverRoot = System.getProperty("resin.root");

        if (serverRoot != null)
          setRootDirectory(Vfs.lookup(serverRoot));
      }

      if (getRootDirectory() == null)
        throw new NullPointerException();

      // default server id
      if (getServerId() == null)
        setServerId("");

      // watchdog/0212
      // else
      //  setRootDirectory(Vfs.getPwd());

      Environment.addChildLoaderListener(new PersistenceEnvironmentListener());
      Environment.addChildLoaderListener(new WebBeansAddLoaderListener());
      InjectManager webBeans = InjectManager.create();

      Config.setProperty("resinHome", getResinHome());
      Config.setProperty("resin", new Var());
      Config.setProperty("server", new Var());
      Config.setProperty("java", new JavaVar());
      Config.setProperty("system", System.getProperties());
      Config.setProperty("getenv", System.getenv());

      new HempBrokerManager();

      _management = createResinManagement();

      if (webBeans.getBeans(ResinWebBeansProducer.class).size() == 0) {
        Config.setProperty("fmt", new FmtFunctions());

        ResinConfigLibrary.configure(webBeans);

        try {
          Method method = Jndi.class.getMethod("lookup", new Class[] { String.class });
          Config.setProperty("jndi", method);
          Config.setProperty("jndi:lookup", method);
        } catch (Exception e) {
          throw ConfigException.create(e);
        }

        webBeans.addManagedBean(webBeans.createManagedBean(ResinWebBeansProducer.class));
        webBeans.update();
      }

      _threadPoolAdmin = ThreadPoolAdmin.create();
      _resinAdmin = new ResinAdmin(this);

      _threadPoolAdmin.register();

      MemoryAdmin.create();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns the classLoader
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
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
   * Sets the classLoader
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader loader)
  {
    _classLoader = loader;
  }

  /**
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/resin/resin.rnc";
  }

  /**
   * Sets the server id.
   */
  public void setServerId(String serverId)
  {
    Config.setProperty("serverId", serverId);

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
   * Sets the server id.
   */
  public void setDynamicServer(String clusterId, String address, int port)
  {
    String id = address + ":" + port;

    _dynCluster = clusterId;
    _dynAddress = address;
    _dynPort = port;

    if (_serverId == null)
      setServerId(id);
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
    _configFile = configFile;
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
   * Sets the root directory.
   */
  public void setRootDirectory(Path root)
  {
    _rootDirectory = root;

    if (root == null)
      throw new NullPointerException();
  }

  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Returns the resin-data directory
   */
  public Path getResinDataDirectory()
  {
    Path path;

    if (_resinDataDirectory != null)
      path = _resinDataDirectory;
    else if (_isWatchdog)
      path = getRootDirectory().lookup("watchdog-data");
    else
      path = getRootDirectory().lookup("resin-data");

    if (path instanceof MemoryPath) { // QA
      path = Vfs.lookup("file:/tmp/caucho/qa/resin-data");
    }

    return path;
  }

  /**
   * Sets the resin-data directory
   */
  public void setResinDataDirectory(Path path)
  {
    if (path.isFile()) {
      throw new ConfigException(L().l("resin-data-directory '{0}' must not be a file",
                                    path));
    }

    _resinDataDirectory = path;
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

  /**
   * Returns the cluster names.
   */
  public ClusterMXBean []getClusters()
  {
    ClusterMXBean []clusters = new ClusterMXBean[_clusters.size()];

    for (int i = 0; i < _clusters.size(); i++)
      clusters[i] = _clusters.get(i).getAdmin();

    return clusters;
  }

  public void addClusterDefault(ContainerProgram program)
  {
    _clusterDefaults.add(program);
  }

  @Configurable
  public Cluster createCluster()
    throws ConfigException
  {
    Cluster cluster = instantiateCluster();

    for (int i = 0; i < _clusterDefaults.size(); i++)
      _clusterDefaults.get(i).configure(cluster);

    return cluster;
  }

  protected Cluster instantiateCluster()
  {
    return new SingleCluster(this);
  }

  @Configurable
  public void addCluster(Cluster cluster)
  {
    _clusters.add(cluster);
  }

  public ArrayList<Cluster> getClusterList()
  {
    return _clusters;
  }

  /**
   * Set true if the server should enable environment-based
   * system properties.
   */
  public void setEnvironmentSystemProperties(boolean isEnable)
  {
    EnvironmentProperties.enableEnvironmentSystemProperties(isEnable);
  }

  /**
   * Configures the thread pool
   */
  public ThreadPoolConfig createThreadPool()
    throws Exception
  {
    return new ThreadPoolConfig();
  }

  /**
   * Sets the user name for setuid.
   */
  public void setUserName(String userName)
  {
  }

  /**
   * Sets the group name for setuid.
   */
  public void setGroupName(String groupName)
  {
  }

  /**
   * Sets the minimum free memory allowed.
   */
  public void setMinFreeMemory(Bytes minFreeMemory)
  {
    _minFreeMemory = minFreeMemory.getBytes();
  }

  /**
   * Gets the minimum free memory allowed.
   */
  public long getMinFreeMemory()
  {
    return _minFreeMemory;
  }

  /**
   * Sets the shutdown time
   */
  public void setShutdownWaitMax(Period shutdownWaitMax)
  {
    _shutdownWaitMax = shutdownWaitMax.getPeriod();
  }

  /**
   * Gets the minimum free memory allowed.
   */
  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }

  /**
   * Set true if system properties are global.
   */
  public void setGlobalSystemProperties(boolean isGlobal)
  {
  }

  public SecurityManagerConfig createSecurityManager()
  {
    return new SecurityManagerConfig();
  }

  public void setWatchdogManager(ConfigProgram program)
  {
  }

  /**
   * Configures the TM.
   */
  @Deprecated
  public TransactionManager createTransactionManager()
    throws ConfigException
  {
    log().warning(L().l("<transaction-manager> tag belongs in <management>"));

    return new TransactionManager(this);
  }

  @Deprecated
  public void addManagement(ConfigProgram program)
  {
    _clusterDefaults.add(program);
  }

  @Deprecated
  public Management createResinManagement()
  {
    if (_management == null) {
      _management = new Management();

      _management.setResin(this);
    }

    return _management;
  }

  public ModuleRepository createModuleRepository()
  {
    return _repository;
  }

  public TempFileManager getTempFileManager()
  {
    if (_tempFileManager == null) {
      Path path = getResinDataDirectory();

      _tempFileManager = new TempFileManager(path);
    }

    return _tempFileManager;
  }

  /**
   * Adds a new security provider
   */
  public void addSecurityProvider(Class<?> providerClass)
    throws Exception
  {
    if (! Provider.class.isAssignableFrom(providerClass))
      throw new ConfigException(L().l("security-provider {0} must implement java.security.Provider",
                                    providerClass.getName()));

    Security.addProvider((Provider) providerClass.newInstance());
  }

  /**
   * Configures JSP (backwards compatibility).
   */
  public JspPropertyGroup createJsp()
  {
    return new JspPropertyGroup();
  }

  /**
   * Ignore the boot configuration
   */
  public void addBoot(ContainerProgram program)
    throws Exception
  {
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
    return _lifecycle;
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
    return _server;
  }

  public void setServer(Server server)
  {
    assert(_server == null);

    _server = server;
  }

  /**
   * Returns the management api.
   */
  public Management getManagement()
  {
    return _management;
  }

  public Server createServer()
  {
    if (_server == null) {
      ClusterServer clusterServer = null;

      if (_dynCluster != null) {
        clusterServer
          = loadDynamicServer(_dynPod, _serverId, _dynAddress, _dynPort);
      }

      if (clusterServer == null)
        clusterServer = findClusterServer(_serverId);

      if (clusterServer == null)
        throw new ConfigException(L().l("server-id '{0}' has no matching <server> definition.",
                                        _serverId));


      Server server = clusterServer.startServer();

      assert(server == _server);

      if (_stage != null)
        _server.setStage(_stage);

      // _server.start();
    }

    return _server;
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
      thread.setContextClassLoader(getClassLoader());

      // force a GC on start
      System.gc();

      ClusterServer clusterServer = null;

      if (_dynCluster != null) {
        clusterServer = findClusterServer(_serverId);

        if (clusterServer != null)
          throw new ConfigException(L().l("dynamic-server '{0}' must not have a static configuration configured in the resin.xml.",
                                          _serverId));

        Cluster cluster = findCluster(_dynCluster);

        if (cluster == null) {
          throw new ConfigException(L().l("dynamic-server cluster '{0}' does not exist.  Dynamic servers must be added to an existing cluster.",
                                          _dynamicServer.getCluster()));
        }

        if (! cluster.isDynamicServerEnable()) {
          throw new ConfigException(L().l("cluster '{0}' does not allow dynamic servers.  Add a <dynamic-server-enable/> tag to the <cluster> to enable it.",
                                          cluster.getId()));
        }

        _dynPod = cluster.getPodList()[0];

        if (_dynPod == null)
          throw new NullPointerException();
      }

      /*
      // XXX: get the server
      for (Cluster cluster : _clusters) {
        cluster.start();
      }
      */

      _server = createServer();

      for (int i = 0; i < _boundPortList.size(); i++) {
        BoundPort port = _boundPortList.get(i);

        _server.bind(port.getAddress(),
                     port.getPort(),
                     port.getServerSocket());
      }
      
      _server.start();

      Environment.start(getClassLoader());

      /*
        if (! hasListeningPort()) {
        log().warning(L().l("-server \"{0}\" has no matching http or srun ports.  Check the resin.xml and -server values.",
        _serverId));
        }
      */

      log().severe(this + " started in " + (Alarm.getExactTime() - _startTime) + "ms");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Starts the server.
   */
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;
  }

  public boolean isDynamicServer()
  {
    for (int i = 0; i < _clusters.size(); i++) {
      Cluster cluster = _clusters.get(i);

      if (cluster.isDynamicServerEnable())
        return true;
    }

    return false;
  }

  public Cluster findCluster(String id)
  {
    for (int i = 0; i < _clusters.size(); i++) {
      Cluster cluster = _clusters.get(i);

      if (cluster.getId().equals(id))
        return cluster;
    }

    return null;
  }

  public ClusterServer findClusterServer(String id)
  {
    for (int i = 0; i < _clusters.size(); i++) {
      Cluster cluster = _clusters.get(i);

      ClusterServer server = cluster.findServer(id);

      if (server != null)
        return server;
    }

    return null;
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
    return _lifecycle.isActive();
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

  public void startFailSafeShutdown(String msg)
  {
    // start the fail-safe thread in case the shutdown fails
    FailSafeHaltThread haltThread = _failSafeHaltThread;
    if (haltThread != null)
      haltThread.startShutdown();

    log().severe(msg);
  }
  /**
   * Start the server shutdown
   */
  public void startShutdown(String msg)
  {
    startFailSafeShutdown(msg);

    if (_lifecycle.isDestroying())
      return;

    ShutdownThread shutdownThread = _shutdownThread;
    if (shutdownThread != null)
      shutdownThread.startShutdown();
    else
      shutdownImpl();
  }

  public void destroy()
  {
    startFailSafeShutdown("Resin shutdown from destroy() call");

    if (_lifecycle.isDestroying())
      return;

    shutdownImpl();
  }

  /**
   * Closes the server.
   */
  private void shutdownImpl()
  {
    // start the fail-safe thread in case the shutdown fails
    FailSafeHaltThread haltThread = _failSafeHaltThread;
    if (haltThread != null)
      haltThread.startShutdown();

    try {
      if (_isDumpHeapOnExit) {
        dumpHeapOnExit();
      }

      try {
        Socket socket = _pingSocket;

        if (socket != null)
          socket.setSoTimeout(1000);
      } catch (Throwable e) {
        log().log(Level.WARNING, e.toString(), e);
      }

      try {
        Server server = _server;

        if (server != null)
          server.destroy();
      } catch (Throwable e) {
        log().log(Level.WARNING, e.toString(), e);
      } finally {
        _server = null;
      }

      try {
        Management management = _management;
        _management = null;

        if (management != null)
          management.destroy();
      } catch (Throwable e) {
        log().log(Level.WARNING, e.toString(), e);
      }

      _threadPoolAdmin.unregister();

      if (_isGlobal)
        Environment.closeGlobal();
      else
        _classLoader.destroy();
    } finally {
      _lifecycle.toDestroy();

      if (_mainThread != null)
        System.exit(EXIT_OK); // check exit code with config errors
    }
  }

  public void parseCommandLine(String []argv)
    throws Exception
  {
    preConfigureInit();

    int len = argv.length;
    int i = 0;

    while (i < len) {
      // RandomUtil.addRandom(argv[i]);

      if (i + 1 < len
          && (argv[i].equals("-stdout")
              || argv[i].equals("--stdout"))) {
        Path path = Vfs.lookup(argv[i + 1]);

        RotateStream stream = RotateStream.create(path);
        stream.init();
        WriteStream out = stream.getStream();
        out.setDisableClose(true);

        EnvironmentStream.setStdout(out);

        i += 2;
      }
      else if (i + 1 < len
               && (argv[i].equals("-stderr")
                   || argv[i].equals("--stderr"))) {
        Path path = Vfs.lookup(argv[i + 1]);

        RotateStream stream = RotateStream.create(path);
        stream.init();
        WriteStream out = stream.getStream();
        out.setDisableClose(true);

        EnvironmentStream.setStderr(out);

        i += 2;
      }
      else if (i + 1 < len
               && (argv[i].equals("-conf")
                   || argv[i].equals("--conf"))) {
        _configFile = argv[i + 1];
        i += 2;
      }
      else if (argv[i].equals("-log-directory")
               || argv[i].equals("--log-directory")) {
        i += 2;
      }
      else if (argv[i].equals("-config-server")
               || argv[i].equals("--config-server")) {
        i += 2;
      }
      else if (argv[i].equals("--dump-heap-on-exit")) {
        _isDumpHeapOnExit = true;

        i += 1;
      }
      else if (i + 1 < len
               && (argv[i].equals("-dynamic-server")
                   || argv[i].equals("--dynamic-server"))) {
        String []values = argv[i + 1].split(":");

        if (values.length == 3) {
          String clusterId = values[0];
          String address = values[1];
          int port = Integer.parseInt(values[2]);

          setDynamicServer(clusterId, address, port);
        } else {
          System.out.println("-dynamic-server requires 'cluster:address:port' at '" + argv[i + 1] + "'");

          System.exit(66);
        }

        i += 2;
      }
      else if (i + 1 < len
               && (argv[i].equals("-server")
                   || argv[i].equals("--server"))) {
        setServerId(argv[i + 1]);
        i += 2;
      }
      else if (argv[i].equals("-resin-home")
               || argv[i].equals("--resin-home")) {
        _resinHome = Vfs.lookup(argv[i + 1]);

        i += 2;
      }
      else if (argv[i].equals("-root-directory")
               || argv[i].equals("--root-directory")
               || argv[i].equals("-resin-root")
               || argv[i].equals("--resin-root")) {
        _rootDirectory = _resinHome.lookup(argv[i + 1]);

        i += 2;
      }
      else if (argv[i].equals("-server-root") // backwards compat
               || argv[i].equals("--server-root")) {
        _rootDirectory = _resinHome.lookup(argv[i + 1]);

        i += 2;
      }
      else if (argv[i].equals("-service")) {
        JniCauchoSystem.create().initJniBackground();
        // windows service
        i += 1;
      }
      else if (argv[i].equals("-version")
               || argv[i].equals("--version")) {
        System.out.println(VersionFactory.getFullVersion());
        System.exit(66);
      }
      else if (argv[i].equals("-watchdog-port")
               || argv[i].equals("--watchdog-port")) {
        // watchdog
        i += 2;
      }
      else if (argv[i].equals("-socketwait")
               || argv[i].equals("--socketwait")
               || argv[i].equals("-pingwait")
               || argv[i].equals("--pingwait")) {
        int socketport = Integer.parseInt(argv[i + 1]);

        Socket socket = null;
        for (int k = 0; k < 15 && socket == null; k++) {
          try {
            socket = new Socket("127.0.0.1", socketport);
          } catch (Throwable e) {
            System.out.println(new Date());
            e.printStackTrace();
          }

          if (socket == null)
            Thread.sleep(1000);
        }

        if (socket == null) {
          System.err.println("Can't connect to parent process through socket " + socketport);
          System.err.println("Resin needs to connect to its parent.");
          System.exit(0);
        }

        /*
        if (argv[i].equals("-socketwait") || argv[i].equals("--socketwait"))
          _waitIn = socket.getInputStream();
        */

        _pingSocket = socket;

        //socket.setSoTimeout(60000);

        i += 2;
      }
      else if ("-port".equals(argv[i]) || "--port".equals(argv[i])) {
        int fd = Integer.parseInt(argv[i + 1]);
        String addr = argv[i + 2];
        if ("null".equals(addr))
          addr = null;
        int port = Integer.parseInt(argv[i + 3]);

        _boundPortList.add(new BoundPort(QJniServerSocket.openJNI(fd, port),
                                         addr,
                                         port));

        i += 4;
      }
      else if ("start".equals(argv[i])
               || "restart".equals(argv[i])) {
        JniCauchoSystem.create().initJniBackground();
        i++;
      }
      else if (argv[i].equals("-verbose")
               || argv[i].equals("--verbose")) {
        i += 1;
      }
      else if (argv[i].equals("console")) {
        i += 1;
      }
      else if (argv[i].equals("-fine")
               || argv[i].equals("--fine")) {
        i += 1;
      }
      else if (argv[i].equals("-finer")
               || argv[i].equals("--finer")) {
        i += 1;
      }
      else if (argv[i].startsWith("-D")
               || argv[i].startsWith("-J")
               || argv[i].startsWith("-X")) {
        i += 1;
      }
      else if ("-stage".equals(argv[i])
               || "--stage".equals(argv[i])) {
        _stage = argv[i + 1];
        i += 2;
      }
      else if ("-preview".equals(argv[i])
               || "--preview".equals(argv[i])) {
        _stage = "preview";
        i += 1;
      }
      else if ("-debug-port".equals(argv[i])
               || "--debug-port".equals(argv[i])) {
        i += 2;
      }
      else if ("-jmx-port".equals(argv[i])
               || "--jmx-port".equals(argv[i])) {
        i += 2;
      }
      else {
        System.out.println(L().l("unknown argument '{0}'", argv[i]));
        System.out.println();
        usage();
        System.exit(66);
      }
    }
  }

  private static void usage()
  {
    System.err.println(L().l("usage: java -jar resin.jar [-options] [start | stop | restart]"));
    System.err.println(L().l(""));
    System.err.println(L().l("where options include:"));
    System.err.println(L().l("   -conf <file>          : select a configuration file"));
    System.err.println(L().l("   -log-directory <dir>  : select a logging directory"));
    System.err.println(L().l("   -resin-home <dir>     : select a resin home directory"));
    System.err.println(L().l("   -root-directory <dir> : select a root directory"));
    System.err.println(L().l("   -server <id>          : select a <server> to run"));
    System.err.println(L().l("   -watchdog-port <port> : override the watchdog-port"));
    System.err.println(L().l("   -verbose              : print verbose starting information"));
    System.err.println(L().l("   -preview              : run as a preview server"));
  }

  /**
   * Initialize the server, binding to TCP and starting the threads.
   */
  public void initMain()
    throws Throwable
  {
    preConfigureInit();

    _mainThread = Thread.currentThread();
    _mainThread.setContextClassLoader(_systemClassLoader);

    addRandom();

    _failSafeHaltThread = new FailSafeHaltThread();
    _failSafeHaltThread.start();

    _shutdownThread = new ShutdownThread();
    _shutdownThread.start();

    setShutdown(this);

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

    if (_classLoader != null)
      thread.setContextClassLoader(_classLoader);

    Path pwd = Vfs.getPwd();

    if (_rootDirectory == null)
      _rootDirectory = _resinHome;

    Vfs.setPwd(_rootDirectory);

    Path resinConf = null;

    if (_configFile != null) {
      if (log().isLoggable(Level.FINER))
        log().finer(this + " looking for conf in " +  pwd.lookup(_configFile));

      resinConf = pwd.lookup(_configFile);
    }

    if (_configFile == null) {
      if (pwd.lookup("conf/resin.xml").canRead())
        _configFile = "conf/resin.xml";
      else { // backward compat
        _configFile = "conf/resin.conf";
      }
    }

    if (resinConf == null || ! resinConf.exists()) {
      if (log().isLoggable(Level.FINER))
        log().finer(this + " looking for conf in " +  _rootDirectory.lookup(_configFile));

      resinConf = _rootDirectory.lookup(_configFile);
    }

    if (! resinConf.exists() && ! _resinHome.equals(_rootDirectory)) {
      if (log().isLoggable(Level.FINER))
        log().finer(this + " looking for conf in " +  _resinHome.lookup(_configFile));

      resinConf = _resinHome.lookup(_configFile);
    }

    // for error messages, show path relative to rootDirectory
    if (! resinConf.exists())
      resinConf = _rootDirectory.lookup(_configFile);

    _resinConf = resinConf;

    // server.setServerRoot(_serverRoot);

    Vfs.setPwd(getRootDirectory());

    Config config = new Config();
    // server/10hc
    // config.setResinInclude(true);

    config.configure(this, resinConf, getSchema());

    ClusterServer clusterServer = findClusterServer(_serverId);

    start();
  }

  private void addRandom()
  {
    /*
    RandomUtil.addRandom(System.currentTimeMillis());
    RandomUtil.addRandom(Runtime.getRuntime().freeMemory());

    RandomUtil.addRandom(System.identityHashCode(_mainThread));
    RandomUtil.addRandom(System.identityHashCode(_systemClassLoader));
    RandomUtil.addRandom(VersionFactory.getFullVersion());

    try {
      RandomUtil.addRandom(InetAddress.getLocalHost().toString());
    } catch (Throwable e) {
    }
    */

    /*
    // for systems with /dev/urandom, read more bits from it.
    try {
      InputStream is = new FileInputStream("/dev/urandom");

      for (int i = 0; i < 16; i++)
        RandomUtil.addRandom(is.read());

      is.close();
    } catch (Throwable e) {
    }

    RandomUtil.addRandom(System.currentTimeMillis());
    */
  }

  protected void dumpHeapOnExit()
  {

  }
  
  public void memoryShutdown(String msg)
  {
    _isDumpHeapOnExit = true;
    
    startShutdown(msg);
    
    try {
      Thread.sleep(10 * 60 * 1000L);
    } catch (Exception e) {
      // interrupted exception
    }
  }

  /**
   * Thread to wait until Resin should be stopped.
   */
  public void waitForExit()
  {
    int socketExceptionCount = 0;
    Integer memoryTest;
    Runtime runtime = Runtime.getRuntime();

    /*
     * If the server has a parent process watching over us, close
     * gracefully when the parent dies.
     */
    while (! isClosing()) {
      try {
        Thread.sleep(10);

        if (! checkMemory(runtime)) {
          startFailSafeShutdown("Resin shutdown from out of memory");
          dumpHeapOnExit();
          return;
        }

        if (! checkFileDescriptor()) {
          startFailSafeShutdown("Resin shutdown from out of file descriptors");
          dumpHeapOnExit();
          return;
        }

        if (_waitIn != null) {
          if (_waitIn.read() >= 0) {
            socketExceptionCount = 0;
          }
          else
            log().warning(L().l("Stopping due to watchdog or user."));

          return;
        }
        else {
          synchronized (this) {
            wait(10000);
          }
        }
      } catch (SocketTimeoutException e) {
        socketExceptionCount = 0;
      } catch (InterruptedIOException e) {
        socketExceptionCount = 0;
      } catch (InterruptedException e) {
        socketExceptionCount = 0;
      } catch (SocketException e) {
        // The Solaris JVM will throw SocketException periodically
        // instead of interrupted exception, so those exceptions need to
        // be ignored.

        // However, the Windows JVMs will throw connection reset by peer
        // instead of returning an end of file in the read.  So those
        // need to be trapped to close the socket.
        if (socketExceptionCount++ == 0) {
          log().log(Level.FINE, e.toString(), e);
        }
        else if (socketExceptionCount > 100)
          return;
      } catch (OutOfMemoryError e) {
        startFailSafeShutdown("Resin shutdown from out of memory");
        dumpHeapOnExit();

        try {
          EnvironmentStream.getOriginalSystemErr().println("Resin halting due to out of memory");
        } catch (Exception e1) {
        } finally {
          System.exit(1);
        }
      } catch (Throwable e) {
        log().log(Level.WARNING, e.toString(), e);

        return;
      }
    }
  }

  private boolean checkMemory(Runtime runtime)
    throws InterruptedException
  {
    long minFreeMemory = getMinFreeMemory();

    if (minFreeMemory <= 0) {
      // memory check disabled
      return true;
    }
    else if (2 * minFreeMemory < getFreeMemory(runtime)) {
      // plenty of free memory
      return true;
    }
    else {
      if (log().isLoggable(Level.FINER)) {
        log().finer(L().l("free memory {0} max:{1} total:{2} free:{3}",
                          "" + getFreeMemory(runtime),
                          "" + runtime.maxMemory(),
                          "" + runtime.totalMemory(),
                          "" + runtime.freeMemory()));
      }

      log().info(L().l("Forcing GC due to low memory. {0} free bytes.",
                       getFreeMemory(runtime)));

      runtime.gc();

      Thread.sleep(1000);

      runtime.gc();

      if (getFreeMemory(runtime) < minFreeMemory) {
        log().severe(L().l("Restarting due to low free memory. {0} free bytes",
                           getFreeMemory(runtime)));

        return false;
      }
    }

    // second memory check
    Object memoryTest = new Integer(0);

    return true;
  }

  private boolean checkFileDescriptor()
  {
    try {
      ReadStream is = _resinConf.openRead();
      is.close();

      return true;
    } catch (IOException e) {
      log().severe(L().l("Restarting due to file descriptor failure:\n{0}",
                         e));

      return false;
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + _serverId + "]";
  }

  private static long getFreeMemory(Runtime runtime)
  {
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();

    // Some JDKs (JRocket) return 0 for the maxMemory
    if (maxMemory < totalMemory)
      return freeMemory;
    else
      return maxMemory - totalMemory + freeMemory;
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

      final Resin resin = Resin.create();

      resin.preConfigureInit();

      resin.parseCommandLine(argv);

      resin.initMain();

      Server server = resin.getServer();

      resin.startResinActor();

      resin.waitForExit();

      resin.startShutdown("Resin shutdown from watchdog exit");

      Thread.sleep(resin.getShutdownWaitMax() + 600000);
      System.exit(0);
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

        System.exit(67);
      }
      else if (e instanceof CompileException) {
        System.err.println(e.getMessage());

        log().log(Level.CONFIG, e.toString(), e);
      }
      else {
        e.printStackTrace(System.err);
      }
    } finally {
      System.exit(1);
    }
  }

  private void startResinActor()
    throws IOException
  {
    _resinActor = new ResinActor(this);

    if (_pingSocket != null) {
      InputStream is = _pingSocket.getInputStream();
      OutputStream os = _pingSocket.getOutputStream();

      ResinLink link = new ResinLink(_resinActor, is, os);

      ThreadPool.getThreadPool().schedule(link);
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

  static class BoundPort {
    private QServerSocket _ss;
    private String _address;
    private int _port;

    BoundPort(QServerSocket ss, String address, int port)
    {
      if (ss == null)
        throw new NullPointerException();

      _ss = ss;
      _address = address;
      _port = port;
    }

    public QServerSocket getServerSocket()
    {
      return _ss;
    }

    public int getPort()
    {
      return _port;
    }

    public String getAddress()
    {
      return _address;
    }
  }

  /**
   * EL variables
   */
  public class Var {
    /**
     * Returns the resin.id
     */
    public String getId()
    {
      return _serverId;
    }

    /**
     * Returns the local address
     *
     * @return IP address
     */
    public String getAddress()
    {
      try {
        Server server = _server;

        if (server != null) {
          ClusterServer clusterServer = server.getSelfServer();

          return clusterServer.getAddress();
        }
        else
          return InetAddress.getLocalHost().getHostAddress();
      } catch (Exception e) {
        log().log(Level.FINE, e.toString(), e);

        return "localhost";
      }
    }

    /**
     * Returns the port (backward compat)
     */
    public int getPort()
    {
      Server server = _server;

      if (server != null) {
        ClusterServer clusterServer = server.getSelfServer();

        return clusterServer.getPort();
      }
      else
        return 0;
    }

    /**
     * Returns the port (backward compat)
     */
    public String getHttpAddress()
    {
      return getAddress();
    }

    /**
     * Returns the port (backward compat)
     */
    public String getHttpsAddress()
    {
      return getAddress();
    }

    /**
     * Returns the port (backward compat)
     */
    public int getHttpPort()
    {
      return 0;
    }

    /**
     * Returns the port (backward compat)
     */
    public int getHttpsPort()
    {
      return 0;
    }

    /**
     * Returns the resin config.
     */
    public Path getConf()
    {
      if (Alarm.isTest())
        return Vfs.lookup("file:/home/resin/conf/resin.xml");
      else
        return getResinConf();
    }

    /**
     * Returns the resin home.
     */
    public Path getHome()
    {
      if (Alarm.isTest())
        return Vfs.lookup("file:/home/resin");
      else
        return Resin.this.getResinHome();
    }

    /**
     * Returns the root directory.
     *
     * @return the root directory
     */
    public Path getRoot()
    {
      if (Alarm.isTest())
        return Vfs.lookup("file:/var/www");
      else
        return Resin.this.getRootDirectory();
    }

    public String getUserName()
    {
      return System.getProperty("user.name");
    }

    /**
     * Returns the version
     *
     * @return version
     */
    public String getVersion()
    {
      if (Alarm.isTest())
        return "3.1.test";
      else
        return VersionFactory.getVersion();
    }

    /**
     * Returns the version date
     *
     * @return version
     */
    public String getVersionDate()
    {
      if (Alarm.isTest())
        return "19980508T0251";
      else
        return VersionFactory.getVersionDate();
    }

    /**
     * Returns the local hostname
     *
     * @return version
     */
    public String getHostName()
    {
      try {
        if (Alarm.isTest())
          return "localhost";
        else
          return InetAddress.getLocalHost().getHostName();
      } catch (Exception e) {
        log().log(Level.FINE, e.toString(), e);

        return "localhost";
      }
    }

    /**
     * Returns the root directory.
     *
     * @return resin.home
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * Returns the root directory.
     *
     * @return resin.home
     */
    public Path getRootDirectory()
    {
      return getRoot();
    }

    /**
     * Returns true for Resin professional.
     */
    public boolean isProfessional()
    {
      return Resin.this.isProfessional();
    }

    /**
     * Returns the -server id
     */
    public String getServerId()
    {
      return _serverId;
    }
  }

  /**
   * Java variables
   */
  public class JavaVar {
    /**
     * Returns true for JDK 5
     */
    public boolean isJava5()
    {
      return true;
    }

    /**
     * Returns the JDK properties
     */
    public Properties getProperties()
    {
      return System.getProperties();
    }

    /**
     * Returns the user name
     */
    public String getUserName()
    {
      return System.getProperty("user.name");
    }

    /**
     * Returns the JDK version
     */
    public String getVersion()
    {
      return System.getProperty("java.version");
    }

    /**
     * Returns the JDK home
     */
    public Path getHome()
    {
      return Vfs.lookup(System.getProperty("java.home"));
    }
  }

  class SecurityManagerConfig {
    private boolean _isEnable = true;

    SecurityManagerConfig()
    {
      if (_securityManager == null)
        _securityManager = new SecurityManager();
    }

    public void setEnable(boolean enable)
    {
      _isEnable = enable;
    }

    public void setValue(boolean enable)
    {
      setEnable(enable);
    }

    public void setPolicyFile(Path path)
      throws ConfigException
    {
      if (! path.canRead())
        throw new ConfigException(L().l("policy-file '{0}' must be readable.",
                                      path));

    }

    @PostConstruct
    public void init()
    {
      if (_isEnable)
        System.setSecurityManager(_securityManager);
    }
  }

  class ShutdownThread extends Thread {
    private volatile boolean _isShutdown;

    ShutdownThread()
    {
      setName("resin-shutdown");
      setDaemon(true);
    }

    /**
     * Starts the destroy sequence
     */
    public void startShutdown()
    {
      _isShutdown = true;
      LockSupport.unpark(this);
    }

    public void run()
    {
      while (! _isShutdown) {
        try {
          LockSupport.park();
        } catch (Exception e) {
        }
      }

      shutdownImpl();
    }
  }

  class FailSafeHaltThread extends Thread {
    private volatile boolean _isShutdown;

    FailSafeHaltThread()
    {
      setName("resin-fail-safe-halt");
      setDaemon(true);
    }

    /**
     * Starts the shutdown sequence
     */
    public void startShutdown()
    {
      _isShutdown = true;
      LockSupport.unpark(this);
    }

    public void run()
    {
      while (! _isShutdown) {
        try {
          LockSupport.park();
        } catch (Exception e) {
        }
      }

      long expire = System.currentTimeMillis() + _shutdownWaitMax;
      long now;

      while ((now = System.currentTimeMillis()) < expire) {
        try {
          Thread.interrupted();
          Thread.sleep(expire - now);
        } catch (Exception e) {
        }
      }

      Runtime.getRuntime().halt(Resin.EXIT_HALT);
    }
  }

  static class DynamicServer {
    private final String _cluster;
    private final String _address;
    private final int _port;

    DynamicServer(String cluster, String address, int port)
    {
      _cluster = cluster;
      _address = address;
      _port = port;
    }

    String getCluster()
    {
      return _cluster;
    }

    String getAddress()
    {
      return _address;
    }

    int getPort()
    {
      return _port;
    }
  }
}
