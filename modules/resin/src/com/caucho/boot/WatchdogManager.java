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

package com.caucho.boot;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.admin.RemoteAdminService;
import com.caucho.boot.BootResinConfig.ElasticServer;
import com.caucho.cloud.network.NetworkListenSystem;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.core.ResinProperties;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.config.types.Period;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.thread.ThreadPool;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.jmx.Jmx;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.DependencyCheckInterval;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.log.EnvironmentStream;
import com.caucho.log.LogHandlerConfig;
import com.caucho.log.RotateStream;
import com.caucho.network.listen.TcpPort;
import com.caucho.security.AdminAuthenticator;
import com.caucho.security.Authenticator;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.http.HttpProtocol;
import com.caucho.server.resin.Resin;
import com.caucho.server.resin.ResinArgs;
import com.caucho.server.resin.ResinELContext;
import com.caucho.server.resin.ResinWatchdog;
import com.caucho.server.util.JniCauchoSystem;
import com.caucho.server.webbeans.ResinServerConfigLibrary;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Process responsible for watching a backend watchdog.
 */
class WatchdogManager implements AlarmListener {
  private static L10N _L;
  private static Logger _log;

  private static WatchdogManager _watchdog;

  private Lifecycle _lifecycle = new Lifecycle();

  private WatchdogArgs _args;

  private int _watchdogPort;

  private String _adminCookie;
  private BootResinConfig _resinConfig;
  private BootManagementConfig _management;
  private final ResinSystem _system;

  private ServletService _server;
  private TcpPort _httpPort;

  private HashMap<String,WatchdogChild> _watchdogMap
    = new HashMap<String,WatchdogChild>();

  WatchdogManager(String []argv)
    throws Exception
  {
    _watchdog = this;

    _args = new WatchdogArgs(argv);
    
    String serverId = _args.getServerId();
    
    if (serverId == null) {
      // server/6e0d
      serverId = _args.getClientServerId();
    }
    
    ResinArgs resinArgs = new ResinArgs();
    resinArgs.setRootDirectory(_args.getRootDirectory());
    resinArgs.setServerId(serverId);
    
    if (_args.getDataDirectory() != null)
      resinArgs.setDataDirectory(_args.getDataDirectory());
    
    Resin resin = new ResinWatchdog(resinArgs);
    
    _system = resin.getResinSystem();

    Vfs.setPwd(_args.getRootDirectory());
    
    String logName = "watchdog-manager";
    Path logPath = getLogDirectory().lookup(logName + ".log");
    
    try {
      getLogDirectory().mkdirs();
    } catch (Exception e) {
      log().log(Level.ALL, e.toString(), e);
    }
    
    if (! getLogDirectory().isDirectory()) {
      log().warning("Watchdog can't open log directory: " + getLogDirectory().getNativePath()
                    + " as user " + System.getProperty("user.name"));
    }
    
    try {
      // #4333 - check watchdog-manager.log can be written
      WriteStream testOut = logPath.openAppend();
      testOut.close();
    } catch (Exception e) {
      log().log(Level.WARNING, "Log-file: " + logPath + " " + e, e);
    }
    
    if (! logPath.canWrite()) {
      throw new ConfigException("Cannot open " + logPath.getNativePath() + " as '" + System.getProperty("user.name") 
                                + "' required for Resin start. Please check permissions");
    }

    RotateStream logStream = RotateStream.create(logPath);
    logStream.setRolloverSize(64L * 1024 * 1024);
    logStream.init();
    WriteStream out = logStream.getStream();
    out.setDisableClose(true);
    
    boolean isLogDirectoryExists = getLogDirectory().exists();

    EnvironmentStream.setStdout(out);
    EnvironmentStream.setStderr(out);

    LogHandlerConfig log = new LogHandlerConfig();
    log.setName("");
    log.setPath(logPath);
    log.init();

    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(_system.getClassLoader());

    ThreadPool.getThreadPool().setIdleMin(8);
    ThreadPool.getThreadPool().setPriorityIdleMin(8);
    
    // add max with long timeout to avoid the manager spawning.
    ThreadPool.getThreadPool().setIdleMax(16);
    ThreadPool.getThreadPool().setIdleTimeout(Integer.MAX_VALUE);

    ResinELContext elContext = _args.getELContext();

    // resin.preConfigureInit();
    
    // XXX: needs to be config

    InjectManager cdiManager = InjectManager.create();

    Config.setProperty("resinHome", elContext.getResinHome());
    Config.setProperty("resin", elContext.getResinVar());
    Config.setProperty("server", elContext.getServerVar());
    Config.setProperty("java", elContext.getJavaVar());
    Config.setProperty("system", System.getProperties());
    Config.setProperty("getenv", System.getenv());
    
    Config.setProperty("rvar0", serverId);
    
    ResinConfigLibrary.configure(cdiManager);
    ResinServerConfigLibrary.configure(cdiManager);

    // read $HOME/.resin
    if (_args.getUserProperties() != null && _args.getUserProperties().canRead()) {
      ResinProperties properties = new ResinProperties();
      properties.setPath(_args.getUserProperties());
      properties.setMode(_args.getMode());
      
      properties.init();
    }
    
    _watchdogPort = _args.getWatchdogPort();
    
    _resinConfig = readConfig(_args);

    WatchdogChild server = findConfig(_resinConfig, serverId, _args);
    
    server = getWatchdog(server, serverId, _args);
    
    if (server == null) {
      if (serverId == null) {
        throw new IllegalStateException(L().l("Cannot find any <server> or <server-multi> matching a local IP address"));
      }
      else {
        throw new IllegalStateException(L().l("'{0}' is an unknown server",
                                              serverId));
      }
    }

    JniBoot boot = new JniBoot();
    Path logDirectory = getLogDirectory();

    if (boot.isValid()) {
      if (! isLogDirectoryExists) {
        logDirectory.mkdirs();

        boot.chown(logDirectory, server.getUserName(), server.getGroupName());
      }
    }

    server.getConfig().logInit(logName, logStream.getRolloverLog());
    
    startWatchdogSystem(resin, server);
  }
  
  private void startWatchdogSystem(Resin resin,
                                   WatchdogChild server)
    throws Exception
  {
    // resin.preConfigureInit();
    // resin.setConfigFile(_args.getResinConf().getNativePath());

    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(resin.getClassLoader());

    /*
    CloudSystem cloudSystem = TopologyService.getCurrent().getSystem();
    
    CloudCluster cluster = cloudSystem.createCluster("watchdog");
    CloudPod pod = cluster.createPod();
    pod.createStaticServer("default", "localhost", -1, false);
    */

    _server = resin.createServer();
    
    thread.setContextClassLoader(_server.getClassLoader());
    
    
    NetworkListenSystem listenService 
      = _system.getService(NetworkListenSystem.class);
    
    _httpPort = new TcpPort();
    _httpPort.setProtocol(new HttpProtocol());

    if (_watchdogPort > 0)
      _httpPort.setPort(_watchdogPort);
    else
      _httpPort.setPort(server.getWatchdogPort());

    _httpPort.setAddress(server.getWatchdogAddress());

    _httpPort.setAcceptThreadMin(2);
    _httpPort.setAcceptThreadMax(3);

    _httpPort.init();

    listenService.addListener(_httpPort);
    
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_system.getClassLoader());

      InjectManager cdiManager = InjectManager.create();
      AdminAuthenticator auth = null;

      if (_management != null)
        auth = _management.getAdminAuthenticator();

      if (auth != null) {
        BeanBuilder<Authenticator> factory = cdiManager.createBeanFactory(Authenticator.class);

        factory.type(Authenticator.class);
        factory.type(AdminAuthenticator.class);
        factory.qualifier(DefaultLiteral.DEFAULT);

        cdiManager.addBeanDiscover(factory.singleton(auth));
      }

      DependencyCheckInterval depend = new DependencyCheckInterval();
      depend.setValue(new Period(-1));
      depend.init();

      RemoteAdminService adminService = new RemoteAdminService();
      adminService.setAuthenticationRequired(false);
      adminService.init();

      HempBroker broker = HempBroker.getCurrent();
      
      WatchdogActor service = new WatchdogActor(this);
      
      broker.getBamManager().createService("watchdog@admin.resin.caucho", service);


      ResinSystem.getCurrent().start();

      _lifecycle.toActive();
      
      // valid checker
      new Alarm(this).queue(60000);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  ClassLoader getClassLoader()
  {
    return _system.getClassLoader();
  }

  static WatchdogManager getWatchdog()
  {
    return _watchdog;
  }

  public void setAdminCookie(String cookie)
  {
    if (_adminCookie == null)
      _adminCookie = cookie;
  }

  public String getAdminCookie()
  {
    if (_adminCookie != null)
      return _adminCookie;
    else if (_management != null)
      return _management.getAdminCookie();
    else
      return null;
  }

  boolean isActive()
  {
    return _server.isActive() && _httpPort.isActive();
  }
  
  WatchdogArgs getArgs()
  {
    return _args;
  }

  Path getRootDirectory()
  {
    return _args.getRootDirectory();
  }

  Path getResinHome()
  {
    return _args.getResinHome();
  }

  Path getLogDirectory()
  {
    Path logDirectory = _args.getLogDirectory();

    if (logDirectory != null)
      return logDirectory;
    else
      return getRootDirectory().lookup("log");
  }

  boolean authenticate(String password)
  {
    String cookie = getAdminCookie();

    if (password == null && cookie == null)
      return true;
    else if  (password != null && password.equals(cookie))
      return true;
    else
      return false;
  }

  WatchdogChild findServer(String id)
  {
    return _watchdogMap.get(id);
  }
  
  BootResinConfig getManagerConfig()
  {
    return _resinConfig;
  }

  /**
   * Called from the Hessian API to report the status of the watchdog
   *
   * @return a human-readable description of the current status
   */
  String status()
  {
    StringBuilder sb = new StringBuilder();

    synchronized (_watchdogMap) {
      ArrayList<String> keys = new ArrayList<String>(_watchdogMap.keySet());
      Collections.sort(keys);
      
      sb.append("\nwatchdog:\n");
      sb.append("  watchdog-pid: " + getWatchdogPid());

      for (String key : keys) {
        WatchdogChild child = _watchdogMap.get(key);

        sb.append("\n\n");
        sb.append("server '" + child.getId() + "' : " + child.getState() + "\n");

        if (getAdminCookie() == null)
          sb.append("  password: missing\n");
        else
          sb.append("  password: ok\n");
        
        sb.append("  watchdog-user: " + System.getProperty("user.name") + "\n");

        if (child.getUserName() != null)
          sb.append("  user: " + child.getUserName());
        else
          sb.append("  user: " + System.getProperty("user.name"));

        if (child.getGroupName() != null)
          sb.append("(" + child.getGroupName() + ")");

        sb.append("\n");

        sb.append("  root: " + child.getResinRoot() + "\n");
        sb.append("  conf: " + child.getResinConf() + "\n");

        if (child.getPid() > 0)
          sb.append("  pid: " + child.getPid() + "\n");
        
        sb.append("  uptime: " + child.getUptimeString() + "\n");
      }
    }

    return sb.toString();
  }

  /**
   * Called from the Hessian API to start a server.
   *
   * @param argv the command-line arguments to start the server
   */
  String startServerAll(String cliServerId, String []argv)
    throws ConfigException
  {
    WatchdogArgs args = new WatchdogArgs(argv, null, false);

    Vfs.setPwd(_args.getRootDirectory());
    
    String serverId = args.getServerId();

    WatchdogChild server;

    try {
      BootResinConfig resin = readConfig(args);
      
      if (resin.isElasticServer()) {
        int totalCount = 0;
        
        for (ElasticServer elasticServer : resin.getElasticServers()) {
          String cluster = elasticServer.getCluster();
          int count = elasticServer.getCount();
          
          for (int index = 0; index < count; index++) {
            server = addElasticServer(resin, args, cluster, index, totalCount);
            
            startServer(server, serverId, args);
            
            totalCount++;
          }
          
        }
        
        return "test";
        
        // server = findConfig(resin, serverId, args);
      } else {
        server = findConfig(resin, serverId, args);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    
    startServer(server, serverId, args);
    
    return serverId;
  }
  
  /**
   * Called from the Hessian API to start a server.
   *
   * @param argv the command-line arguments to start the server
   */
  String startServer(String cliServerId, String []argv)
      throws ConfigException
  {
    String serverId = cliServerId;
    
    synchronized (_watchdogMap) {
      WatchdogArgs args = new WatchdogArgs(argv, null, false);

      Vfs.setPwd(_args.getRootDirectory());
      
      if (serverId == null) {
        serverId = args.getServerId();
      }

      WatchdogChild server;

      try {
        BootResinConfig resin = readConfig(args);
        
        server = findConfig(resin, serverId, args);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
      
      startServer(server, serverId, args);
    }
    
    return serverId;
  }

  void startServer(WatchdogChild watchdog,
                   String serverId,
                   WatchdogArgs args)
  {
    watchdog = getWatchdog(watchdog, serverId, args);

    if (watchdog == null) {
      throw new ConfigException(L().l("No matching <server> found for start -server '{0}' in '{1}'",
                                      serverId, _args.getResinConf()));
    }

    watchdog.start();
  }
  
  /**
   * Called from the hessian API to gracefully stop a Resin instance
   *
   * @param serverId the Resin instance to stop
   */
  void stopServer(String serverId, String []argv)
  {
    WatchdogArgs args = new WatchdogArgs(argv, false);
    
    // serverId = getServerId(serverId, args);
    
    synchronized (_watchdogMap) {
      WatchdogChild watchdog = getWatchdog(null, serverId, args);

      if (watchdog == null) {
        throw new ConfigException(L().l("No matching <server> found for stop -server '{0}' in {1}",
                                        serverId, _args.getResinConf()));
      }

      watchdog.stop();
    }
  }
  
  /**
   * Called from the hessian API to gracefully stop a Resin instance
   *
   * @param serverId the Resin instance to stop
   */
  private void stopServer(WatchdogChild watchdog)
  {
    // serverId = getServerId(serverId, args);
    
    watchdog.stop();
  }

  WatchdogChild getWatchdog(WatchdogChild watchdog,
                            String serverId,
                            WatchdogArgs args)
  {
    if (watchdog != null)
      return watchdog;
    
    if (serverId == null)
      watchdog = findLocalServer();
    
    if (serverId == null)
      serverId = "default";
  
    // server/6e09
    String defaultServerId = getServerId(serverId, args);
    
    if (watchdog == null) {
      watchdog = getWatchdog(defaultServerId);
    }

    if (watchdog == null) {
      watchdog = getWatchdog(serverId);
      // env/0fp7
      
      if (watchdog == null) {
        watchdog = _watchdogMap.get(defaultServerId);
      }
    }
    
    return watchdog;
  }

  private String getServerId(String serverId, WatchdogArgs args)
  {
    /*
    if (serverId == null)
      serverId = args.getServerId();

    if (isDynamicServer(args)) {
      serverId = args.getDynamicServerId();
    }

    if (serverId == null)
      serverId = "default";

    return serverId;
    */
    if (serverId == null && ! isDynamicServer(args))
      serverId = args.getServerId();
    else if (serverId == null && isDynamicServer(args))
      serverId = args.getElasticServerId();

    if (serverId == null)
      serverId = "default";

    return serverId;
  }
  
  private boolean isDynamicServer(WatchdogArgs args)
  {
    if (args.isElasticServer())
      return true;
    else
      return false;
  }
  
  private WatchdogChild getWatchdog(String serverId)
  {
    WatchdogChild watchdog = _watchdogMap.get(serverId);
  
    if (watchdog == null 
        && (serverId == null || "".equals(serverId))
        && _watchdogMap.size() == 1) {
      watchdog = _watchdogMap.values().iterator().next();
    }

    return watchdog;
  }

  /**
   * Called from the hessian API to forcibly kill a Resin instance
   *
   * @param serverId the server id to kill
   */
  void killServer(String serverId)
  {
    // no synchronization because kill must avoid blocking

    WatchdogChild watchdog = getWatchdog(serverId);

    if (watchdog == null)
      throw new ConfigException(L().l("No matching <server> found for -server '{0}' in {1}",
                                      serverId, _args.getResinConf()));

    watchdog.kill();
  }
  
  /**
   * Checks to see if any watchdog is active.
   */
  boolean isEmpty()
  {
    synchronized (_watchdogMap) {
      for (WatchdogChild child : _watchdogMap.values()) {
        if (child.isActive())
          return false;
      }
    }
    
    return true;
  }

  /**
   * Called from the hessian API to forcibly kill a Resin instance
   *
   * @param serverId the server id to kill
   */
  void shutdown()
  {
    ArrayList<String> keys = new ArrayList<String>();
    
    keys.addAll(_watchdogMap.keySet());
    
    for (String serverId : keys) {
      stopServer(_watchdogMap.get(serverId));
      killServer(serverId);
    }
  }
  
  /**
   * Called from the HMTP API to restart a Resin instance.
   *
   * @param serverId the server identifier to restart
   * @param argv the command-line arguments to apply to the start
   */
  void restartServer(String serverId, String []argv)
  {
    WatchdogArgs args = new WatchdogArgs(argv, false);
    
    // serverId = getServerId(serverId, args);
    
    synchronized (_watchdogMap) {
      WatchdogChild watchdog = getWatchdog(null, serverId, args);

      if (watchdog != null) {
        watchdog.stop();
      }

      startServer(serverId, argv);
    }
  }

  boolean isValid()
  {
    return _server != null && _server.isActive();
  }

  private BootResinConfig readConfig(WatchdogArgs args)
    throws Exception
  {
    Config config = new Config();
    // ignore since we don't want to start databases
    config.setIgnoreEnvironment(true);

    Vfs.setPwd(args.getRootDirectory());
    BootResinConfig resin = new BootResinConfig(_system, args);
    
    Config.setProperty("rvar0", args.getServerId());

    config.configure(resin,
                     args.getResinConf(),
                     "com/caucho/server/resin/resin.rnc");

    if (_management == null)
      _management = resin.getManagement();
    
    return resin;
  }
  
  private WatchdogChild findConfig(BootResinConfig resin,
                                   String cliServerId,
                                   WatchdogArgs args)
  {
    WatchdogClient client = findClient(resin, cliServerId, args);
    
    if (client == null) {
      throw new ConfigException(L().l("server '{0}' cannot be started because no configuration has been found.",
                                      cliServerId != null ? cliServerId : ""));
    }
    
    WatchdogChild watchdog = new WatchdogChild(_system, client.getConfig());
    
    updateChild(watchdog);

    return watchdog;
  }

  private WatchdogClient findClient(BootResinConfig resin,
                                    String cliServerId,
                                    WatchdogArgs args)
  {
    String serverId = resin.getServerId(args);
    
    WatchdogClient client = resin.findClient(cliServerId, args);
    
    if (client != null) {
      return client;
    }

    if (! resin.isElasticServer(args)) {
      if (serverId != null) {
        return null;
      }
      
      client = resin.findUniqueLocalClient(cliServerId, args);
      
      if (client != null) {
        // server/6e12
        return client;
      }
    }
    
    if (resin.isDynamicServerAllowed(args)) {
      for (ElasticServer server : resin.getElasticServerList()) {
        return addElasticServerChild(resin, args, server.getCluster(), 0, 0);
      }
    }

    return null;
  }
  
  private WatchdogChild addElasticServer(BootResinConfig resin,
                                         WatchdogArgs args,
                                         String cluster,
                                         int index,
                                         int totalCount)
  {
    WatchdogClient client = addElasticServerChild(resin, args, cluster, index, totalCount);
    
    WatchdogChild watchdog = new WatchdogChild(_system, client.getConfig());
    
    updateChild(watchdog);
    
    return watchdog;
  }
  
  private WatchdogClient addElasticServerChild(BootResinConfig resin,
                                               WatchdogArgs args,
                                               String clusterId,
                                               int index,
                                               int totalCount)
  {
    if (clusterId == null) {
      clusterId = resin.getClusterId(args);
    }
    
    String address = args.getDynamicAddress();
    int port = resin.getElasticServerPort(args, totalCount);

    BootClusterConfig cluster = resin.findCluster(clusterId);

    if (cluster == null) {
      throw new ConfigException(L().l("server cannot be started because '{0}' is an unknown cluster",
                                      clusterId));
    }
    
    clusterId = cluster.getId();
    String serverId = args.getServerId();
    
    if (serverId == null) {
      serverId = "dyn-" + clusterId + "-" + index;
    }
    else if (totalCount > 1) {
      throw new ConfigException(L().l("--elastic-server with --server '{0}' and multiple elastic servers is not allowed.",
                                      serverId));
    }

    WatchdogConfigHandle configHandle = cluster.createServer();
    // String serverId = args.getElasticServerId();
    configHandle.setId(serverId);
    configHandle.setAddress(address);
    configHandle.setPort(port);

    WatchdogConfig serverConfig = cluster.addServer(configHandle);
    
    serverConfig.setElastic(true);
    serverConfig.setElasticServerCluster(clusterId);
    serverConfig.setElasticServerPort(port);

    return resin.findClient(serverConfig.getId());
  }
  
  private WatchdogChild updateChild(WatchdogChild newWatchdog)
  {
    String serverId = newWatchdog.getId();

    WatchdogChild oldWatchdog = _watchdogMap.get(serverId);
    
    if (oldWatchdog != null) {
      if (oldWatchdog.isActive()) {
        throw new ConfigException(L().l("server '{0}' cannot be started because a running instance already exists.  stop or restart the old server first.",
                                        serverId));
      }

      oldWatchdog = _watchdogMap.remove(serverId);

      if (oldWatchdog != null)
        oldWatchdog.close();
    }
    
    _watchdogMap.put(serverId, newWatchdog);

    return newWatchdog;
  }

  private int getWatchdogPid()
  {
    try {
      MBeanServer server = Jmx.getGlobalMBeanServer();
      ObjectName objName = new ObjectName("java.lang:type=Runtime");
      
      String runtimeName = (String) server.getAttribute(objName, "Name");
      
      if (runtimeName == null) {
        return 0;
      }
      
      int p = runtimeName.indexOf('@');
      
      if (p > 0) {
        int pid = Integer.parseInt(runtimeName.substring(0, p));

        return pid;
      }
      
      return 0;
    } catch (Exception e) {
      log().log(Level.FINE, e.toString(), e);
      
      return 0;
    }

  }

  public void waitForExit()
  {
    while (_lifecycle.isActive()) {
      try {
        synchronized (this) {
          wait();
        }
      } catch (Exception e) {
      }
    }
  }
  
  private WatchdogChild findLocalServer()
  {
    ArrayList<InetAddress> localAddresses = BootResinConfig.getLocalAddresses();
    
    for (WatchdogChild child : _watchdogMap.values()) {
      if (BootResinConfig.isLocalClient(localAddresses, child.getConfig())) {
        return child;
      }
    }
    
    return null;
  }
  
  private WatchdogChild findUniqueLocalServer()
  {
    ArrayList<InetAddress> localAddresses = BootResinConfig.getLocalAddresses();
    
    WatchdogChild server = null;
    
    for (WatchdogChild child : _watchdogMap.values()) {
      if (BootResinConfig.isLocalClient(localAddresses, child.getConfig())) {
        if (server != null)
          return null;
        
        server = child;
      }
    }
    
    return server;
  }

  @Override
  public void handleAlarm(Alarm alarm)
  {
    try {
      if (! _args.getResinConf().canRead()) {
        log().severe(L().l("{0} exiting because '{1}' is no longer valid",
                           this, _args.getResinConf()));

        System.exit(1);
      }
    } finally {
      alarm.queue(60000);
    }
  }

  /**
   * The launching program for the watchdog manager, generally called
   * from ResinBoot.
   */
  public static void main(String []argv)
    throws Exception
  {
    boolean isValid = false;

    try {
      DynamicClassLoader.setJarCacheEnabled(false);
      DynamicClassLoader.setGlobalDependencyCheckInterval(-1);

      JniCauchoSystem.create().initJniBackground();

      WatchdogManager manager = new WatchdogManager(argv);
      
      WatchdogArgs args = manager.getArgs();
      
      args.getCommand().doWatchdogStart(manager);
      /*
      String serverId = manager.getArgs().getClientServerId();
      manager.startServer(serverId, argv);
      */

      isValid = manager.isActive() && manager.isValid();

      if (isValid) {
        manager.waitForExit();
      }
    } catch (Exception e) {
      log().log(Level.WARNING, e.toString(), e);
      System.out.println(e);
      e.printStackTrace();
    } finally {
      System.exit(1);
    }
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(WatchdogManager.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(WatchdogManager.class.getName());

    return _log;
  }
}
