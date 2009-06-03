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

package com.caucho.boot;

import com.caucho.admin.RemoteAdminService;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.SingletonBean;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.config.types.RawString;
import com.caucho.config.types.Period;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.loader.*;
import com.caucho.log.EnvironmentStream;
import com.caucho.log.LogConfig;
import com.caucho.log.RotateStream;
import com.caucho.security.Authenticator;
import com.caucho.security.AdminAuthenticator;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.SingleCluster;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.host.Host;
import com.caucho.server.host.HostConfig;
import com.caucho.server.port.Port;
import com.caucho.server.port.ProtocolDispatchServer;
import com.caucho.server.resin.Resin;
import com.caucho.server.resin.ResinELContext;
import com.caucho.server.util.*;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.*;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process responsible for watching a backend watchdog.
 */
class WatchdogManager implements AlarmListener {
  private static L10N _L;
  private static Logger _log;

  private static WatchdogManager _watchdog;

  private WatchdogArgs _args;

  private int _watchdogPort;

  private String _adminCookie;
  private BootManagementConfig _management;

  private Server _server;
  private Port _httpPort;
  
  private HashMap<String,Watchdog> _watchdogMap
    = new HashMap<String,Watchdog>();

  WatchdogManager(String []argv)
    throws Exception
  {
    _watchdog = this;

    _args = new WatchdogArgs(argv);

    Vfs.setPwd(_args.getRootDirectory());

    Path logPath = getLogDirectory().lookup("watchdog-manager.log");

    RotateStream logStream = RotateStream.create(logPath);
    logStream.init();
    WriteStream out = logStream.getStream();
    out.setDisableClose(true);

    EnvironmentStream.setStdout(out);
    EnvironmentStream.setStderr(out);

    LogConfig log = new LogConfig();
    log.setName("");
    log.setPath(logPath);
    log.setLevel("all");
    log.init();

    Logger.getLogger("").setLevel(Level.INFO);

    ThreadPool.getThreadPool().setThreadIdleMin(1);
    ThreadPool.getThreadPool().setThreadIdleMax(5);

    ResinELContext elContext = _args.getELContext();

    // XXX: needs to be config
    
    InjectManager webBeans = InjectManager.create();

    Config.setProperty("resinHome", elContext.getResinHome());
    Config.setProperty("resin", elContext.getResinVar());
    Config.setProperty("server", elContext.getServerVar());
    Config.setProperty("java", elContext.getJavaVar());
    Config.setProperty("system", System.getProperties());
    Config.setProperty("getenv", System.getenv());

    ResinConfigLibrary.configure(webBeans);

    _watchdogPort = _args.getWatchdogPort();
    
    readConfig(_args);

    Watchdog server = null;
    
    if (_args.isDynamicServer()) {
      String serverId = _args.getDynamicAddress() + "-" 
                        + _args.getDynamicPort();
      server = _watchdogMap.get(serverId);
    }
    else
      server = _watchdogMap.get(_args.getServerId());

    if (server == null)
      throw new IllegalStateException(L().l("'{0}' is an unknown server",
					    _args.getServerId()));

    server.getConfig().logInit(logStream);

    Resin resin = Resin.createWatchdog();

    resin.setConfigFile(_args.getResinConf().getNativePath());

    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(resin.getClassLoader());
    
    Cluster cluster = resin.createCluster();
    ClusterServer clusterServer = cluster.createServer();
    // cluster.addServer(clusterServer);

    clusterServer.setId("");
    clusterServer.setPort(0);

    _httpPort = clusterServer.createHttp();
    if (_watchdogPort > 0)
      _httpPort.setPort(_watchdogPort);
    else
      _httpPort.setPort(server.getWatchdogPort());

    _httpPort.setAddress(server.getWatchdogAddress());

    _httpPort.setMinSpareListen(1);
    _httpPort.setMaxSpareListen(2);

    _httpPort.init();

    // clusterServer.addHttp(http);

    cluster.addServer(clusterServer);

    resin.addCluster(cluster);

    _server = resin.createServer();
    _server.bindPorts();

    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_server.getClassLoader());

      AdminAuthenticator auth = null;

      if (_management != null)
	auth = _management.getAdminAuthenticator();

      if (auth != null) {
	BeanFactory factory = webBeans.createBeanFactory(Authenticator.class);

	factory.type(Authenticator.class);
	factory.type(AdminAuthenticator.class);
	
	webBeans.addBean(factory.singleton(auth));
      }

      DependencyCheckInterval depend = new DependencyCheckInterval();
      depend.setValue(new Period(-1));
      depend.init();
      
      RemoteAdminService adminService = new RemoteAdminService();
      adminService.setAuthenticationRequired(false);
      adminService.init();

      WatchdogService service
	= new WatchdogService(this, "watchdog@admin.resin.caucho");

      HempBroker broker = HempBroker.getCurrent();

      /*
      broker.setAdmin(true);
      broker.setAllowNullAdminAuthenticator(true);
      */

      service.setBrokerStream(broker.getBrokerStream());

      broker.addActor(service);

      _server.start();

      // valid checker
      new Alarm(this).queue(60000);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
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

  Path getRootDirectory()
  {
    return _args.getRootDirectory();
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
  
  Watchdog findServer(String id)
  {
    return _watchdogMap.get(id);
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
    
      for (String key : keys) {
	Watchdog watchdog = _watchdogMap.get(key);

	sb.append("\n");
	sb.append("server '" + key + "' : " + watchdog.getState() + "\n");

	if (getAdminCookie() == null)
	  sb.append("  password: missing\n");
	else
	  sb.append("  password: ok\n");
      
	sb.append("  user: " + System.getProperty("user.name"));
        
	if (watchdog.getGroupName() != null)
	  sb.append("(" + watchdog.getGroupName() + ")");
        
	sb.append("\n");
      
	sb.append("  root: " + watchdog.getResinRoot() + "\n");
	sb.append("  conf: " + watchdog.getResinConf() + "\n");

	if (watchdog.getPid() > 0)
	  sb.append("  pid: " + watchdog.getPid());
      }
    }
    
    return sb.toString();
  }

  /**
   * Called from the Hessian API to start a server.
   * 
   * @param argv the command-line arguments to start the server
   */
  void startServer(String []argv)
    throws ConfigException
  {
    synchronized (_watchdogMap) {
      WatchdogArgs args = new WatchdogArgs(argv);

      Vfs.setPwd(_args.getRootDirectory());

      try {
	readConfig(args);
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    
      String serverId = args.getServerId();

      if (args.isDynamicServer())
	serverId = args.getDynamicAddress() + "-" + args.getDynamicPort();
    
      Watchdog watchdog = _watchdogMap.get(serverId);

      if (watchdog == null)
	throw new ConfigException(L().l("No matching <server> found for -server '{0}' in '{1}'",
					serverId, _args.getResinConf()));

      watchdog.start();
    }
  }

  /**
   * Called from the hessian API to gracefully stop a Resin instance
   * 
   * @param serverId the Resin instance to stop
   */
  void stopServer(String serverId)
  {
    synchronized (_watchdogMap) {
      Watchdog watchdog = _watchdogMap.get(serverId);
    
      if (watchdog == null)
	throw new ConfigException(L().l("No matching <server> found for -server '{0}' in {1}",
					serverId, _args.getResinConf()));
    
      watchdog.stop();
    }
  }

  /**
   * Called from the hessian API to forcibly kill a Resin instance
   * 
   * @param serverId the server id to kill
   */
  void killServer(String serverId)
  {
    // no synchronization because kill must avoid blocking
    
    Watchdog watchdog = _watchdogMap.get(serverId);
    
    if (watchdog == null)
      throw new ConfigException(L().l("No matching <server> found for -server '{0}' in {1}",
				      serverId, _args.getResinConf()));
    
    watchdog.kill();
  }

  /**
   * Called from the hessian API to restart a Resin instance.
   * 
   * @param serverId the server identifier to restart
   * @param argv the command-line arguments to apply to the start
   */
  void restartServer(String serverId, String []argv)
  {
    synchronized (_watchdogMap) {
      Watchdog server = _watchdogMap.get(serverId);
    
      if (server != null)
	server.stop();
    
      startServer(argv);
    }
  }

  boolean isValid()
  {
    return _server != null && _server.isActive();
  }

  private Watchdog readConfig(WatchdogArgs args)
    throws Exception
  {
    Config config = new Config();
    // ignore since we don't want to start databases
    config.setIgnoreEnvironment(true);

    Vfs.setPwd(args.getRootDirectory());
    BootResinConfig resin = new BootResinConfig(args);

    config.configure(resin,
		     args.getResinConf(),
		     "com/caucho/server/resin/resin.rnc");

    if (_management == null)
      _management = resin.getManagement();
    
    /*
    // The configuration file has already been validated by ResinBoot, so
    // it doesn't need a second validation
    config.configure(resin,
		     args.getResinConf());
    */

    String serverId = args.getServerId();
    WatchdogConfig server = null;

    if (args.isDynamicServer()) {
      String clusterId = args.getDynamicCluster();
      String address = args.getDynamicAddress();
      int port = args.getDynamicPort();

      BootClusterConfig cluster = resin.findCluster(clusterId);

      if (cluster == null) {
	throw new ConfigException(L().l("'{0}' is an unknown cluster",
				      clusterId));
      }
      
      server = cluster.createServer();
      serverId = address + "-" + port;
      server.setId(serverId);
      server.setAddress(address);
      server.setPort(port);
      cluster.addServer(server);
    }
    else {
      WatchdogClient client = resin.findClient(serverId);

      if (client != null)
        server = client.getConfig();
      else
        server = resin.findServer(serverId);
    }

    Watchdog watchdog = _watchdogMap.get(server.getId());

    if (watchdog != null) {
      if (watchdog.isActive()) {
	throw new ConfigException(L().l("server '{0}' cannot be started because a running instance already exists.  stop or restart the old server first.",
					server.getId()));
      }

      watchdog = _watchdogMap.remove(server.getId());

      if (watchdog != null)
	watchdog.close();
    }

    watchdog = new Watchdog(server);

    _watchdogMap.put(server.getId(), watchdog);

    return watchdog;
  }

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
    
      JniCauchoSystem.create().initJniBackground();
      
      WatchdogManager manager = new WatchdogManager(argv);
      manager.startServer(argv);

      isValid = manager.isActive() && manager.isValid();
    } catch (Exception e) {
      log().log(Level.WARNING, e.toString(), e);
    } finally {
      if (! isValid)
	System.exit(1);
    }
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(ResinBoot.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(ResinBoot.class.getName());

    return _log;
  }
}
