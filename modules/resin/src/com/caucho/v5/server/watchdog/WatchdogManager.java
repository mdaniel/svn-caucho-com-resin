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

package com.caucho.v5.server.watchdog;

import io.baratine.core.Result;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.baratine.InService;
import com.caucho.v5.bartender.BartenderBuilder;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.heartbeat.ServerHeartbeatBuilder;
import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.cloud.security.SecuritySystem;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.jni.JniBoot;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.DependencyCheckInterval;
import com.caucho.v5.log.impl.EnvironmentStream;
import com.caucho.v5.log.impl.LogHandlerConfig;
import com.caucho.v5.log.impl.RotateStream;
import com.caucho.v5.network.listen.PortTcp;
import com.caucho.v5.server.config.RootConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.WriteStream;

/**
 * Process responsible for watching a backend watchdog.
 */
@InService(WatchdogService.class)
public class WatchdogManager implements AlarmListener
{
  private static final L10N L = new L10N(WatchdogManager.class);
  private static final Logger log
    = Logger.getLogger(WatchdogManager.class.getName());
  
  private static WatchdogManager _watchdog;

  private Lifecycle _lifecycle = new Lifecycle();

  private final ArgsWatchdog _args;
  private final ServerConfigBoot _serverConfig;
  private final RootConfigBoot _watchdogRoot;

  // private int _watchdogPort;

  private String _adminCookie;
  
  // private BootManagementConfig _management;
  private final SystemManager _system;

  private PortTcp _httpPort;
  private WatchdogService _service;
  private ConfigException _startException;

  WatchdogManager(ArgsWatchdog args,
                  ServerConfigBoot serverConfig)
    throws Exception
  {
    Objects.requireNonNull(args);
    
    _watchdog = this;

    _args = args;
    _serverConfig = serverConfig;
    _watchdogRoot = serverConfig.getRoot();
    
    _system = new SystemManager("watchdog");

    Vfs.setPwd(getRootDirectory());
    
    Path logPath = getLogDirectory().lookup("watchdog-manager.log");
    
    try {
      getLogDirectory().mkdirs();
    } catch (Exception e) {
      log.log(Level.ALL, e.toString(), e);
    }
    
    // #4333 - check watchdog-manager.log can be written
    WriteStream testOut = logPath.openAppend();
    testOut.close();
    
    if (! logPath.canWrite()) {
      throw new ConfigException("Cannot open " + logPath.getNativePath()
                                + " required for Watchdog start. Please check permissions");
    }

    RotateStream logStream = RotateStream.create(logPath);
    logStream.setRolloverSize(64L * 1024 * 1024);
    logStream.init();
    WriteStream out = logStream.getStream();
    out.setDisableClose(true);

    EnvironmentStream.setStdout(out);
    EnvironmentStream.setStderr(out);

    LogHandlerConfig log = new LogHandlerConfig();
    log.addName("");
    log.setPath(logPath);
    log.init();
    
    if (args.isVerbose()) {
      Logger.getLogger("").setLevel(Level.FINER);
      Logger.getLogger("javax.management").setLevel(Level.INFO);
    }

    ThreadPool.getThreadPool().setIdleMin(4);
    ThreadPool.getThreadPool().setPriorityIdleMin(4);
    
    boolean isLogDirectoryExists = getLogDirectory().exists();

    JniBoot boot = new JniBoot();
    Path logDirectory = getLogDirectory();

    if (boot.isValid()) {
      if (! isLogDirectoryExists) {
        logDirectory.mkdirs();

        boot.chown(logDirectory,
                   _serverConfig.getUserName(_args), 
                   _serverConfig.getGroupName(_args));
      }
    }
    
    startWatchdogSystem(_serverConfig);
  }
  
  private void startWatchdogSystem(ServerConfigBoot serverConfig)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_system.getClassLoader());
      
      if (_args.isVerbose()) {
        Logger.getLogger("").setLevel(Level.FINER);
      }
      
      SystemManager systemManager = _system;
      
      AmpSystem.createAndAddSystem("system:");
      
      SecuritySystem.createAndAddSystem();
      
      String address = serverConfig.getWatchdogAddress(_args);
      int port = serverConfig.getWatchdogPort(_args);
      int portBartender = port;
      
      ServerHeartbeatBuilder selfBuilder = new ServerHeartbeatBuilder();
      selfBuilder.pod("watchdog");

      BartenderBuilder bartenderBuilder
        = BartenderSystem.createBuilder(address, port, false, 
                                        portBartender, 
                                        "watchdog", "watchdog", 
                                        port,
                                        selfBuilder);

      BartenderSystem system = bartenderBuilder.build();
      
      ServerBartender selfServer = system.getServerSelf();
      
      NetworkSystem.createAndAddSystem(systemManager, selfServer, serverConfig, _args);

      //InjectManager cdiManager = InjectManager.create();
      //AuthenticatorRole auth = null;

      /*
      if (_management != null) {
        auth = _management.getAdminAuthenticator();
      }
      */

      /*
      if (auth != null) {
        BeanBuilder<AuthenticatorRole> factory = cdiManager.createBeanBuilder(AuthenticatorRole.class);

        factory.type(AuthenticatorRole.class);
        // factory.type(AdminAuthenticator.class);
        factory.qualifier(new AdminLiteral());

        cdiManager.addBeanDiscover(factory.singleton(auth));
      }
      */

      DependencyCheckInterval depend = new DependencyCheckInterval();
      depend.setValue(new Period(-1));
      depend.init();
      
      WatchdogServiceImpl service = new WatchdogServiceImpl(this);
      
      ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
      
      _service = rampManager.service(service)
                            .address("public:///watchdog")
                            .as(WatchdogService.class);
      
      _system.start();

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
  
  public SystemManager getSystem()
  {
    return _system;
  }

  static WatchdogManager getWatchdog()
  {
    return _watchdog;
  }

  public void setAdminCookie(String cookie)
  {
    if (_adminCookie == null) {
      _adminCookie = cookie;
    }
  }

  public String getAdminCookie()
  {
    if (_adminCookie != null) {
      return _adminCookie;
    }
    /*
    else if (_management != null) {
      return _management.getAdminCookie();
    }
    */
    else {
      return null;
    }
  }

  boolean isActive()
  {
    return _system.isActive() && _httpPort.isActive();
  }
  
  public ArgsWatchdog getArgs()
  {
    return _args;
  }

  Path getRootDirectory()
  {
    return _serverConfig.getRootDirectory(_args);
  }

  Path getHomeDirectory()
  {
    return _args.getHomeDirectory();
  }

  Path getLogDirectory()
  {
    return _serverConfig.getRoot().getLogDirectory(_args);
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
  
  public RootConfigBoot getRootConfig()
  {
    return _watchdogRoot;
  }

  /**
   * Called cli to start a server.
   *
   * @param argv the command-line arguments to start the server
   */
  public void startServerAll(String []argv, Result<String> result)
  {
    _service.startAll(argv, result);
  }
  
  public String startServerAll(String []argv)
  {
    return _service.startAll(argv);
  }

  void shutdownPort()
  {
    PortTcp port = _httpPort;
  
    if (port != null) {
      port.close();
    }
  }

  boolean isValid()
  {
    return _system != null && _system.isActive();
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

  @Override
  public void handleAlarm(Alarm alarm)
  {
    try {
      if (! _args.getConfigPath().canRead()) {
        log.severe(L.l("{0} exiting because '{1}' is no longer valid",
                       this, _args.getConfigPath()));

        System.exit(1);
      }
    } finally {
      alarm.queue(60000);
    }
  }
  
  private ConfigException error(String msg, Object ...args)
  {
    return new ConfigException(L.l(msg, args));
  }

  public void setStartException(ConfigException exn)
  {
    _startException = exn;
  }

  public ConfigException getStartException(ConfigException exn)
  {
    return _startException;
  }
}
