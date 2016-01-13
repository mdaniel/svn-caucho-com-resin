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

package com.caucho.v5.server.container;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.bartender.BartenderBuilder;
import com.caucho.v5.bartender.BartenderBuilderPod;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.files.BartenderFileSystem;
import com.caucho.v5.bartender.heartbeat.RackHeartbeat;
import com.caucho.v5.bartender.heartbeat.ServerHeartbeatBuilder;
import com.caucho.v5.bartender.journal.JournalSystem;
import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.cli.server.BootConfigParser;
import com.caucho.v5.cloud.security.SecuritySystem;
import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.deploy.DeploySystem;
import com.caucho.v5.env.system.RootDirectorySystem;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.shutdown.ShutdownSystem;
import com.caucho.v5.health.warning.WarningSystem;
import com.caucho.v5.http.cache.TempFileSystem;
import com.caucho.v5.http.container.HttpContainerBuilder;
import com.caucho.v5.http.container.HttpSystem;
import com.caucho.v5.http.container.ServerConfig;
import com.caucho.v5.http.pod.PodSystem;
import com.caucho.v5.inject.InjectManagerAmp;
import com.caucho.v5.javac.WorkDir;
import com.caucho.v5.kraken.KrakenSystem;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.profile.HeapDump;
import com.caucho.v5.server.config.ClusterConfigBoot;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.PodConfigBoot;
import com.caucho.v5.server.config.RootConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.JmxUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.QDate;
import com.caucho.v5.util.ThreadDump;
import com.caucho.v5.util.Version;
import com.caucho.v5.vfs.MemoryPath;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ServerSocketBar;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.net.SocketSystem;

import io.baratine.config.Config;

/**
 * The server builder is used to configure and build a server.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class ServerBuilderOld
{
  private static Logger log = Logger.getLogger(ServerBuilderOld.class.getName());
  private static L10N L = new L10N(ServerBuilderOld.class);
  
  private final ArgsServerBase _args;
  private final ServerConfigBoot _serverConfig;
  private final ConfigBoot _configBoot;

  // private ServerBartender _selfServer;
  private boolean _isEmbedded;
  private boolean _isWatchdog;
  
  // private final StatProbeManager _statProbeManager;

  private HeapDump _heapDump;
  private MBeanServer _mbeanServer;
  private ObjectName _hotSpotName;
  private String[]_heapDumpArgs;

  private long _shutdownWaitTime = 60000L;
  private int _serverPort;
  private boolean _isSSL;
  private int _portBartender = -1;
  private int _dynamicDataIndex = -1;
  private PathImpl _dataDirectory;
  private Config _env;
  
  /**
   * Creates a new server builder.
   */
  public ServerBuilderOld(ArgsServerBase args, ServerConfigBoot serverConfig)
  {
    Objects.requireNonNull(args);
    Objects.requireNonNull(serverConfig);
    
    _args = args;

    _env = _args.config();
    
    _serverConfig = serverConfig;
    
    _configBoot = new ConfigBoot(serverConfig.getRoot());
    // _statProbeManager = new StatProbeManager();
  }
  
  public ServerBuilderOld(String []argv)
  {
    this(new ArgsServerBase(argv));
  }
    
  public ServerBuilderOld(ArgsServerBase args)
  {
    _args = args;
    
    _env = _args.config();

    _args.parse();

    BootConfigParser configParser = createConfigParser();
    ConfigBoot bootConfig;

    try {
      SystemManager resinSystem = new SystemManager(_args.getServerId());
      
      bootConfig = configParser.parseBoot(_args, resinSystem);

      _configBoot = bootConfig;
      // RootConfigBoot rootConfig = bootConfig.getConfig();

      ArrayList<ServerConfigBoot> serverList
        = bootConfig.findStartServers(_args);
      
      ServerConfigBoot serverConfig = null;
      
      if (serverList.size() == 1) {
        serverConfig = serverList.get(0);
      }
      else if (serverList.size() == 0) { // && isEmbedded()) {
        serverConfig = bootConfig.createEmbeddedServer(args);
      }
      else {
        throw new ConfigException(L.l("Invalid server list. Too many servers match {0}", serverList));
      }
      
      _serverConfig = serverConfig; 

      // _statProbeManager = new StatProbeManager();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  protected BootConfigParser createConfigParser()
  {
    return new BootConfigParser();
  }
  
  protected ArgsServerBase getArgs()
  {
    return _args;
  }

  long getStartTime()
  {
    return _args.getStartTime();
  }
  
  ServerConfigBoot getServerConfig()
  {
    return _serverConfig;
  }

  Socket getPingSocket()
  {
    return _args.getPingSocket();
  }
  
  public void init(ServerBaseOld server)
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(server.getClassLoader());
    } finally {
      thread.setContextClassLoader(loader);
    }

    if (HeapDump.isAvailable()) {
      _heapDump = HeapDump.create();
    }
    
    _mbeanServer = JmxUtil.getMBeanServer();
    
    try {
      _hotSpotName = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
      _heapDumpArgs = new String[] { String.class.getName(), boolean.class.getName() };
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  public static ServerBuilderOld create(String[] args)
  {
    return create(new ArgsServerBase(args));
  }

  public static ServerBuilderOld create(ArgsServerBase args)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new Resin instance
   */
  public static ServerBuilderOld create(String programName,
                                     ArgsServerBase args,
                                     ServerConfigBoot serverConfig)
  {
    // String programName = args.getProgramName();
    programName = Character.toUpperCase(programName.charAt(0)) + programName.substring(1);
    
    try {
      String className = ServerBuilderOld.class.getName() + programName;
      Class<?> cl = Class.forName(className);
      Constructor<?> ctor = cl.getConstructor(ArgsServerBase.class,
                                              ServerConfigBoot.class);
      
      return (ServerBuilderOld) ctor.newInstance(args, serverConfig);
    } catch (Exception e) {
      e.printStackTrace();
      log.log(Level.FINEST, e.toString(), e);
    }
    
    return new ServerBuilderOld(args, serverConfig);
  }

  String getClusterId()
  {
    // return getConfigBoot().getHomeCluster(_args);

    return _serverConfig.getCluster().getId();
  }
  
  protected RootConfigBoot getRootConfig()
  {
    return getConfigBoot().getRoot();
  }

  PathImpl getHomeDirectory()
  {
    return _args.getHomeDirectory();
  }
  
  PathImpl getRootDirectory()
  {
    PathImpl rawRoot =  getConfigBoot().getRootDirectory(_args);
    
    //String name = getClusterId() + '-' + _serverConfig.getPort();
    
    //return rawRoot.lookup(name);
    
    return rawRoot;
  }

  public PathImpl getLogDirectory()
  {
    return getConfigBoot().getLogDirectory(_args);
  }

  public void setShutdownWaitTime(long period)
  {
    _shutdownWaitTime = period;
  }
  
  public long getShutdownWaitTime()
  {
    return _shutdownWaitTime;
  }

  PathImpl getConfigPath()
  {
    PathImpl confPath = _args.getConfigPath();

    if (confPath == null) {
      confPath = _args.getConfigPathDefault();
    }

    return confPath;
  }

  /*
  public StatProbeManager getStatProbeManager()
  {
    return _statProbeManager;
  }
  */

  public ServerBaseOld build()
  {
    EnvLoader.init();
    
    SystemManager systemManager = createSystemManager();
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    boolean isValid = false;
    try {
      thread.setContextClassLoader(systemManager.getClassLoader());
      
      Vfs.setPwd(getRootDirectory());
      
      if (! isEmbedded()) {
        logCopyright();
      }

      preConfigureInit();
      
      configureRootDirectory();
      
      ServerBartender selfServer = initNetwork();

      initHttpSystem(systemManager, selfServer);
      
      ServerBaseOld server = build(systemManager,
                                selfServer);

      init(server);
      
      isValid = true;
      
      return server;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
      
      if (! isValid) {
        systemManager.shutdown(ShutdownModeAmp.IMMEDIATE);
      }
    }
  }
  
  private void logCopyright()
  {
    if (CurrentTime.isTest() || getArgs().isQuiet()) {
      return;
    }
    
    System.out.println(Version.getFullVersion());
    System.out.println(Version.getCopyright());
    System.out.println();

    System.out.println("Starting " + getProgramName()
                       + " on " + QDate.formatLocal(_args.getStartTime()));
    System.out.println();
  }
  
  protected ServerBaseOld build(SystemManager systemManager,
                             ServerBartender serverSelf)
    throws Exception
  {
    return new ServerBaseOld(this, systemManager, serverSelf);
  }

  /**
   * Must be called after the Resin.create()
   */
  void preConfigureInit()
  {
    // _resinLocal.set(this, getClassLoader());

    /*
    if (getRootDirectory() == null) {
      throw new NullPointerException();
    }
    */

    if (isEmbedded()) {
      String serverId = getServerIdVar(); // getServerDisplayName();

      // JmxUtilResin.addContextProperty("Server", serverId);
    }

    addPreTopologyServices();

      /*
      if (! isWatchdog()) {
        // server/p603
        initRepository();
      }
      */

      // watchdog/0212
      // else
      //  setRootDirectory(Vfs.getPwd());

    if (! isWatchdog()) {
      // Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
      // Environment.addChildLoaderListener(new CdiAddLoaderListener());
      // Environment.addChildLoaderListener(new EjbEnvironmentListener());
    }

    InjectManagerAmp.create();
    
    initCdiEnvironment();

    // readUserProperties();

    ConfigContext.setProperty("rvar0", getServerId()); // getServerDisplayName());
    ConfigContext.setProperty("rvar1", getClusterId()); // getServerDisplayName());
      /*
      _bootConfig
        = new BootConfig(_resinSystem,
                         getDisplayServerId(),
                         getResinHome(),
                         getRootDirectory(),
                         getLogDirectory(),
                         getResinConf(),
                         isProfessional(),
                         isWatchdog() ? BootType.WATCHDOG : BootType.RESIN);

     _bootResinConfig = _bootConfig.getBootResin();
     */
  }

  /*
  protected ServerBuilder createDelegate()
  {
    return ServerBuilder.create(this);
  }
  */
  
  private String getServerIdVar()
  {
    String serverId = _args.getServerId();
    
    if (serverId == null) {
      int port = getServerPort();
      
      if (port > 0 && ! _serverConfig.isEphemeral()) {
        serverId = "embed-" + port;
      }
      else {
        serverId = "embed";
      }
    }
    
    return serverId;
  }

  protected void initCdiEnvironment()
  {
  }
  
  protected void addChampSystem(ServerBartender selfServer)
  {
    // ChampSystem.createAndAddSystem(selfServer);
  }
  
  protected void addJournalSystem()
  {
    JournalSystem.createAndAddSystem();
  }

  /**
   * Configures the selected server from the boot config.
   */
  protected void initHttpSystem(SystemManager system,
                                ServerBartender selfServer)
    throws IOException
  {
    RootConfigBoot rootConfig = getRootConfig();
    
    String clusterId = selfServer.getClusterId();
    
    ClusterConfigBoot clusterConfig = rootConfig.findCluster(clusterId);
    
    String serverHeader;
    
    if (clusterConfig.getServerHeader() != null) {
      serverHeader = clusterConfig.getServerHeader();
    }
    else if (! CurrentTime.isTest()) {
      serverHeader = getProgramName() + "/" + Version.getVersion();
    }
    else {
      serverHeader = getProgramName() + "/1.1";
    }
    
    // XXX: need cleaner config class names (root vs cluster at minimum)
    ServerContainerConfig serverConfig
      = new ServerContainerConfig(this, system, selfServer);

    // rootConfig.getProgram().configure(serverConfig);
    
    clusterConfig.getProgram().configure(serverConfig);

    ServerConfig config = null;//new ServerConfig(serverConfig);
    clusterConfig.getServerDefault().configure(config);

    ServerConfigBoot serverConfigBoot
      = rootConfig.findServer(selfServer.getDisplayName());

    if (serverConfigBoot != null) {
      serverConfigBoot.getServerProgram().configure(config);
    }

    _args.getProgram().configure(config);
    // _bootServerConfig.getServerProgram().configure(config);

    serverConfig.init();

    HttpContainerBuilder httpBuilder
      = createHttpBuilder(selfServer, serverHeader);
    
    serverConfig.getProgram().configure(httpBuilder);

    //httpBuilder.init();
    
    PodSystem.createAndAddSystem(httpBuilder);
    HttpSystem.createAndAddSystem(httpBuilder);
    
    //return http;
  }

  protected HttpContainerBuilder createHttpBuilder(ServerBartender selfServer,
                                                   String serverHeader)
  {
    /*
    HttpContainerBuilder builder
      = new HttpContainerBuilderServlet(selfServer, serverHeader);
    
    return builder;
    */
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void setEmbedded(boolean isEmbedded)
  {
    _isEmbedded = isEmbedded;
  }
  
  public boolean isEmbedded()
  {
    return _isEmbedded;
  }
  
  protected boolean isWatchdog()
  {
    return _isWatchdog;
  }
  
  private ConfigBoot getConfigBoot()
  {
    return _configBoot;
  }

  /**
   * Configures the root directory and dataDirectory.
   */
  void configureRootDirectory()
    throws IOException
  {
    _dataDirectory = calculateDataDirectory();

    RootDirectorySystem.createAndAddSystem(getRootDirectory(), _dataDirectory);
  }

  protected PathImpl getDataDirectory()
  {
    Objects.requireNonNull(_dataDirectory);
    
    return _dataDirectory;
  }
  
  private PathImpl calculateDataDirectory()
  {
    PathImpl root = getConfigBoot().getDataDirectory(_args);
    
    PathImpl path;
    /* XXX:
    if (_resinDataDirectory != null)
      root = _resinDataDirectory;
      */

    /*
    if (isWatchdog()) {
      path = root.lookup("watchdog-data");
    }
    else {
      path = root.lookup("baratine-data");
    }
    */
    
    int serverPort = _serverConfig.getPort();

    if (root instanceof MemoryPath) { // QA
      root = WorkDir.getTmpWorkDir().lookup("qa");
    }
    
    if (serverPort > 0 && ! _serverConfig.isEphemeral()) {
      path = root.lookup("data-" + serverPort);
      
      if (_serverConfig.isRemoveDataOnStart(_args)) {
        removeDataDirectory(path);
      }
    }
    else if (serverPort < 0 && ! _serverConfig.isEphemeral()) {
      path = root.lookup("data-embed");
      
      if (_serverConfig.isRemoveDataOnStart(_args)) {
        removeDataDirectory(path);
      }
    }
    else {
      return openDynamicDataDirectory(root);
    }

    return path;
  }
  
  private PathImpl openDynamicDataDirectory(PathImpl root)
  {
    cleanDynamicDirectory(root);
    
    for (int i = 0; i < 10000; i++) {
      PathImpl dir = root.lookup("data-dyn-" + i);
      
      if (! dir.exists() || RootDirectorySystem.isFree(dir)) {
        try {
          dir.mkdirs();
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
        
        _dynamicDataIndex  = i;
        
        return dir;
      }
    }
    
    throw new IllegalStateException(L.l("Can't create working directory."));
  }
  
  private void cleanDynamicDirectory(PathImpl root)
  {
    String []list;
    
    try {
      list = root.list();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      return;
    }
    
    for (String path : list) {
      try {
        if (! path.startsWith("data-dyn-")) {
          continue;
        }
        
        PathImpl dir = root.lookup(path);
        
        if (! RootDirectorySystem.isFree(dir)) {
          continue;
        }
        
        dir.removeAll();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }
  
  private void removeDataDirectory(PathImpl path)
  {
    String name = "data-" + _serverConfig.getPort();
    
    if (! path.getTail().equals(name)) {
      return;
    }
    
    if (! RootDirectorySystem.isFree(path)) {
      return;
    }
    
    try {
      path.removeAll();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  protected ServerBartender initNetwork()
    throws Exception
  {
    SystemManager systemManager = SystemManager.getCurrent();
    
    if (getClusterSystemKey() != null) {
      SecuritySystem security = SecuritySystem.getCurrent();
      security.setSignatureSecret(getClusterSystemKey());
    }
    
    ServerSocketBar ss = null;
    
    if (getServerPort() == 0) {
      ss = _serverConfig.getServerSocket();
      //ss = openEphemeralServerSocket();

      if (ss != null) {
        _serverPort = ss.getLocalPort();
      }
      else {
        throw new IllegalStateException(L.l("server-port 0 requires an ephemeral port"));
      }
    }
    
    ServerBartender serverSelf = initBartender();
    
    NetworkSystem networkSystem = NetworkSystem.createAndAddSystem(systemManager,
                                                                   serverSelf,
                                                                   getArgs().config());
    
    if (ss != null) {
      networkSystem.bind(_serverConfig.getAddress(),
                         ss.getLocalPort(),
                         ss);
    }

    if (_args != null) {
      for (BoundPort port : _args.getBoundPortList()) {
        networkSystem.bind(port.getAddress(),
                           port.getPort(),
                           port.getServerSocket());
      }
    }
    
    KrakenSystem.createAndAddSystem(serverSelf);
    
    // DeploySystem.createAndAddSystem();
    
    addServices(serverSelf);
  
    return serverSelf;
  }
  
  /*
  private QServerSocket openEphemeralServerSocket()
  {
    int port = getServerPort();
    
    if (port != 0) {
      throw new IllegalStateException();
    }
    
    try {
      QServerSocket ss = QJniServerSocket.create(0, 0);
      
      _serverPort = ss.getLocalPort();
      
      ServerConfigBoot serverConfig = getServerConfig();
      
      serverConfig.setPort(_serverPort);
      serverConfig.setEphemeral(true);;
      
      return ss;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  */

  protected void addServices(ServerBartender selfServer)
  {
    TempFileSystem.createAndAddSystem();
    
    // createManagementMBean(server);

    // WebSocketClusterSystem.createAndAddSystem();
    
    addChampSystem(selfServer);
    
    BartenderFileSystem.createAndAddSystem();
    
    addJournalSystem();
    
    // XXX: NautilusSystem.createAndAddSystem();
    
    /*
    if (true || ! CurrentTime.isTest()) {
      createLogSystem();
      createStatSystem();
    }
    */
  }

  /**
   * Configures the selected server from the boot config.
   * @return 
   */
  private ServerBartender initBartender()
  {
    // BootResinConfig bootResin = _bootResinConfig;

    // XXX: _clusterSystemKey = _bootConfig.getClusterSystemKey(_args);
    
    int machinePort = getServerPort();
    
    if (_dynamicDataIndex >= 0) {
      machinePort = _dynamicDataIndex;
    }
    
    int portBartender = getPortBartender();
    
    ServerHeartbeatBuilder selfBuilder = new ServerHeartbeatBuilder();
    selfBuilder.dynamic(_serverConfig.isDynamic());
    
    if (_args.isClient()) {
      selfBuilder.pod("client");
    }
    
    for (String pod : _args.getArgList("pod")) {
      selfBuilder.pod(pod);
    }
    
    if (_args.getArgFlag("pod-any")) {
      selfBuilder.podAny(true);
    }

    BartenderBuilder builder
      = BartenderSystem.createBuilder(_args.config(),
                                      getServerAddress(),
                                      getServerPort(),
                                      isSSL(),
                                      portBartender,
                                      _serverConfig.getCluster().getId(),
                                      getServerId(),
                                      machinePort,
                                      selfBuilder);

    initTopologyStatic(builder);
    
    BartenderSystem system = builder.build();
    
    /*
    initTopology(builder, 
                 getServerId(),
                 _serverConfig.getCluster().getId(), 
                 _serverConfig.getPort());
                 */
    
    return system.getServerSelf();
  }

  private void initTopologyStatic(BartenderBuilder rootBuilder)
  {
    RootConfigBoot rootConfig = getRootConfig();
    
    for (ClusterConfigBoot clusterConfig : rootConfig.getClusters()) {
      String clusterName = clusterConfig.getId();
      
      for (ServerConfigBoot serverConfig : clusterConfig.getServerList()) {
        rootBuilder.server(serverConfig.getAddress(),
                           serverConfig.getPort(),
                           serverConfig.isSSL(),
                           clusterName,
                           serverConfig.getDisplayName(),
                           serverConfig.isDynamic());
      }
      
      if (! clusterConfig.getId().equals(_serverConfig.getCluster().getId())) {
        continue;
      }
      
      for (PodConfigBoot podConfig : clusterConfig.getPodList()) {
        BartenderBuilderPod builderPod
          = rootBuilder.pod(podConfig.getId(), clusterConfig.getId());

        if (podConfig.getType() != null) {
          builderPod.type(podConfig.getType());
        }
        
        for (ServerConfigBoot serverConfig : podConfig.getServers()) {
          builderPod.server(serverConfig.getAddress(),
                            serverConfig.getPort(),
                            serverConfig.isSSL(),
                            serverConfig.getDisplayName(),
                            serverConfig.isDynamic());
        }
        
        builderPod.build();
      }
    }
  }

  /*
  public StatSystem createStatSystem()
  {
    StatSystem statSystem = SystemManager.getCurrentSystem(StatSystem.class);

    if (statSystem == null) {
      statSystem = StatSystem.createAndAddSystem();
    }

    return statSystem;
  }
  
  public LogSystem createLogSystem()
  {
    LogSystem logSystem = SystemManager.getCurrentSystem(LogSystem.class);

    if (logSystem == null) {
      logSystem = LogSystemImpl.createAndAddService();
    }

    return logSystem;
  }
  */

  protected String getProgramName()
  {
    return "Baratine";
  }

  public boolean isProfessional()
  {
    return false;
  }

  /**
   * @param pod
   * @param dynId
   * @param dynAddress
   * @param dynPort
   * @return
   */
  protected ServerBartender loadDynamicServer(RackHeartbeat pod, String dynId,
                                              String dynAddress, int dynPort)
  {
    throw new ConfigException(L.l("dynamic-server requires Resin Professional"));
  }

  /**
   * Dump threads for debugging
   */
  public void dumpThreads()
  {
    ThreadDump.create().dumpThreads();
  }

  /**
   * Dump heap on exit.
   */
  public void dumpHeapOnExit(ServerBaseOld server)
  {
    RootDirectorySystem rootService
      = server.getSystemManager().getSystem(RootDirectorySystem.class);
    
    if (rootService != null && _mbeanServer != null) {
      try {
        String pathName = rootService.getDataDirectory().lookup("resin.hprof").getNativePath();

        _mbeanServer.invoke(_hotSpotName, "dumpHeap", 
                            new Object[] { pathName, true },
                            _heapDumpArgs);
        
        log.warning("Java Heap dumped to " + pathName);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    if (_heapDump != null) {
      _heapDump.logHeapDump(log, Level.SEVERE);
    }
  }

  protected NetworkSystem createNetworkSystem(SystemManager systemManager,
                                              ServerBartender selfServer,
                                              RootConfigBoot rootConfig)
  {
    return new NetworkSystem(systemManager,
                                    selfServer,
                                    _args.config());
  }

  /**
   * 
   */
  protected void addPreTopologyServices()
  {
    WarningSystem.createAndAddSystem();
    
    ShutdownSystem.createAndAddSystem(isEmbedded());
    
    AmpSystem.createAndAddSystem(_args.getServerId());
    DeploySystem.createAndAddSystem();
    
    SecuritySystem security = SecuritySystem.createAndAddSystem();
    
    security.setSignatureSecret(_serverConfig.getRoot().getClusterSystemKey());
    
    // HealthStatusService.createAndAddService();
    
    // BlockManagerSubSystem.createAndAddService();

    //createKrakenStoreSystem();
      
    // ShutdownSystem.getCurrent().addMemoryFreeTask(new BlockManagerMemoryFreeTask());
    
    /*
    if (! isWatchdog()) {
      HealthSubSystem health = HealthSubSystem.createAndAddSystem();
      
      if (isEmbedded()) {
        health.setEnabled(false);
      }
    }
    */
  }
  
  protected String getDynamicServerAddress()
  {
    return  null;
  }

  protected int getServerPort()
  {
    return _env.get("server.port", int.class, -1);
    /*
    if (_serverPort > 0) {
      return _serverPort;
    }
    else {
      return _serverConfig.getPort();
    }
    */
  }

  protected boolean isSSL()
  {
    if (_isSSL) {
      return _isSSL;
    }
    else {
      return _serverConfig.isSSL();
    }
  }

  protected int getPortBartender()
  {
    if (_portBartender > 0) {
      return _portBartender;
    }

    //int port = _serverConfig.getPortBartender();
    
    int port = _env.get("bartender.port", int.class, 0);
      
    if (port > 0) {
      return port;
    }

    try {
      SocketSystem socketSystem = SocketSystem.current();
      //JniServerSocketFactory ssFactory = new JniServerSocketFactory();
      ServerSocketBar ssBartender = socketSystem.openServerSocket(0);
    
      _serverConfig.setSocketBartender(ssBartender);
      _serverConfig.setBartenderPort(ssBartender.getLocalPort());
    
      return _serverConfig.getPortBartender();
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  protected String getServerAddress()
  {
    // address needs to be internal address, not the configured external one.
    // The external address will be discovered by the join protocol.
    String address = SocketSystem.current().getHostAddress();
    
    return address; 
  }

  protected String getServerDisplayName()
  {
    return _serverConfig.getDisplayName();
  }

  protected String getServerId()
  {
    String sid = _serverConfig.getId();
    
    if (sid != null && ! "".equals(sid)) {
      return sid;
    }
    
    sid = getArgs().getServerId();
    
    if (sid != null && ! "".equals(sid)) {
      return sid;
    }
    
    int port = getServerPort();
    
    if (port <= 0) {
      return getClusterId() + "-embed";
    }
    
    String address = getServerAddress();
    
    if ("".equals(address)) {
      address = SocketSystem.current().getHostAddress();
    }
    
    // return getClusterId() + '-' + _serverConfig.getPort();
    return address + ":" + port;
  }

  protected String getClusterSystemKey()
  {
    return null; // XXX:
  }

  SystemManager createSystemManager()
  {
    String serverId = getServerId();
    
    return new SystemManager(serverId);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
