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
import io.baratine.core.Service;
import io.baratine.core.ServiceRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.baratine.Remote;
import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.server.BootConfigParser;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.JmxUtil;
import com.caucho.v5.util.L10N;

/**
 * JAMP service managing the watchdog
 */
@Service("public:///watchdog")
@Remote
class WatchdogServiceImpl
{
  private static final L10N L = new L10N(WatchdogServiceImpl.class);
  private static final Logger log
    = Logger.getLogger(WatchdogServiceImpl.class.getName());
  
  private static final long STOP_TIMEOUT = 1000L;
  private static final long SHUTDOWN_TIMEOUT = 120 * 1000L;
  private static final long SHUTDOWN_END_TIMEOUT = 1000L;

  private final WatchdogManager _manager;

  /*
  private TreeMap<Integer,ChildWatchdog> _serverMap
    = new TreeMap<>();
    */

  private TreeMap<Integer,ChildWatchdogService> _childMap
    = new TreeMap<>();

  WatchdogServiceImpl(WatchdogManager manager)
  {
    _manager = manager;
  }
  
  /**
   * Returns the home directory of the watchdog for validation.
   */
  public String getHomeDirectory()
  {
    return _manager.getHomeDirectory().getFullPath();
  }

  /**
   * Starts a server based on the calling args and restricted to
   * the given serverPort.
   */
  public void start(int serverPort, 
                    String []argv, 
                    Result<String> result)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer("start port=" + serverPort + " args=" + Arrays.asList(argv));
    }
    
    result = new ResultCompletion(result);
    
    try {
      ArgsWatchdog args = parseChildArgs(argv);
    
      ServerConfigBoot serverConfig = readConfigServer(serverPort, args);
    
      start(args, serverConfig, result);
    } catch (Throwable e) {
      result.fail(e);
    }
  }
  
  public void waitForStart(int port, Result<String> result)
  {
    ChildWatchdogService child = getChild(port);
    
    child.waitForStart(result);
  }

  /**
   * Called cli to start a server.
   *
   * @param argv the command-line arguments to start the server
   */
  public void startAll(String []argv, Result<String> result)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer("start-all args=" + Arrays.asList(argv));
    }
    
    ArgsWatchdog args = parseChildArgs(argv);
    
    ArrayList<ServerConfigBoot> servers = parseConfig(args);

    if (servers.size() == 0) {
      throw error("No local servers found for start --server '{0}' in '{1}'",
                  args.getServerId(), _manager.getArgs().getConfigPath());
    }
    
    int port = servers.get(0).getPort();

    if (port <= 0) {
      throw error("Server started with invalid port {0}", port);
    }

    
    for (ServerConfigBoot serverConfig : servers) {
      start(args, serverConfig, Result.ignore());
    }

    waitForStart(port, result);
  }

  private void start(ArgsWatchdog args, 
                     ServerConfigBoot serverConfig,
                     Result<String> result)
  {
    int port = serverConfig.getPort();
    
    ChildWatchdogService childService = getChild(port);
    
    /* cli/6e01
    if (serverPort <= 0) {
      throw error("server {0} has an invalid port", serverConfig);
    }
    */

    childService.start(args, serverConfig, result);
  }

  /**
   * Called from the Hessian API to report the status of the watchdog
   *
   * @return a human-readable description of the current status
   */
  public String status()
  {
    StringBuilder sb = new StringBuilder();

    sb.append("\nwatchdog:\n");
    sb.append("  watchdog-pid: " + getWatchdogPid());

    for (ChildWatchdogService server : _childMap.values()) {
      sb.append("\n\n");
      sb.append("server '" + server.getId()
                + "' (" + server.getPort() + ") : " + server.getState() + "\n");

      if (_manager.getAdminCookie() == null)
        sb.append("  password: missing\n");
      else
        sb.append("  password: ok\n");
        
      sb.append("  watchdog-user: " + System.getProperty("user.name") + "\n");

      if (server.getUserName() != null)
        sb.append("  user: " + server.getUserName());
      else
        sb.append("  user: " + System.getProperty("user.name"));

      if (server.getGroupName() != null) {
        sb.append("(" + server.getGroupName() + ")");
      }

      sb.append("\n");

      sb.append("  root: " + server.getRootDir() + "\n");
      sb.append("  conf: " + server.getConfigPath() + "\n");

      if (server.getPid() > 0) {
        sb.append("  pid: " + server.getPid() + "\n");
        
        sb.append("  uptime: " + server.getUptimeString() + "\n");
      }
    }

    return sb.toString();
  }

  private int getWatchdogPid()
  {
    try {
      MBeanServer server = JmxUtil.getMBeanServer();
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
      log.log(Level.FINE, e.toString(), e);
      
      return 0;
    }

  }
  
  /**
   * Called from the hessian API to gracefully stop a Resin instance
   *
   * @param serverId the Resin instance to stop
   */
  public void stop(int port,
                   ShutdownModeAmp mode,
                   Result<String> result)
  {
    result = new ResultCompletion(result);
    
    ChildWatchdogService watchdog = getChild(port);

    if (watchdog == null) {
      throw error("No matching server port={0} found for stop",
                  port);
    }

    watchdog.stop(mode, result);
  }
  
  /**
   * Called from the HMTP API to restart a Resin instance.
   *
   * @param serverId the server identifier to restart
   * @param argv the command-line arguments to apply to the start
   */
  public void restart(int port, String []argv, Result<String> result)
  {
    result = new ResultCompletion(result);
    // serverId = getServerId(serverId, args);
    
    ChildWatchdogService child = getChild(port);

    if (child != null) {
      child.stop(ShutdownModeAmp.GRACEFUL, 
                 new ResultRestart(result, port, argv));
    }
    else {
      start(port, argv, result);
    }
  }

  /**
   * Called from the hessian API to forcibly kill a Resin instance
   *
   * @param serverId the server id to kill
   */
  public void kill(int port, Result<String> result)
  {
    ChildWatchdogService child = getChild(port);

    if (child == null) {
      throw error("No matching server found for serverPort {0} in {1}",
                  port, _manager.getArgs().getConfigPath());
    }

    child.kill(result);
  }
  
  public boolean ping()
  {
    return true;
  }
  
  /**
   * Checks to see if any server is active.
   */
  private boolean isServerActive()
  {
    for (ChildWatchdogService server : _childMap.values()) {
      if (server.isActive()) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Handles shutdown queries
   */
  public void shutdown(ShutdownModeAmp mode,
                       Result<String> result)
  {
    log.info(this + " shutdown from command-line");
    
    String msg = L.l("{0}: shutdown", this);
    
    ArrayList<ChildWatchdogService> servers
      = new ArrayList<>(_childMap.values());
    
    ThreadPool.getCurrent().schedule(new FailSafeKill(servers));
    ThreadPool.getCurrent().schedule(new FailSafeShutdown(SHUTDOWN_TIMEOUT));
    
    ShutdownCompletion cont
      = new ShutdownCompletion(servers.size(), result);
    
    try {
      for (ChildWatchdogService server : servers) {
        server.stop(mode, cont.createResult());
      }
      
      cont.validateCompletion();
    } catch (Throwable e) {
      System.exit(ExitCode.FAIL.ordinal());
    }
    
  }
  
  private ChildWatchdogService getChild(int port)
  {
    ChildWatchdogService child = _childMap.get(port);
    
    if (child == null) {
      ServiceManagerAmp serviceManager = AmpSystem.getCurrentManager();
      
      ChildWatchdogServiceImpl serviceImpl
        = new ChildWatchdogServiceImpl(_manager, port);
      
      child = serviceManager.service(serviceImpl)
                            .start()
                            .as(ChildWatchdogService.class);
      
      _childMap.put(port, child);
    }
    
    return child;
  }

  private ArgsWatchdog parseChildArgs(String []argv)
  {
    ArgsWatchdog args;
    
    args = _manager.getArgs().createArgsChild(argv);

    // args.setTop(false);
    
    args.parse();
    
    return args;
  }
  
  ServerConfigBoot readConfigServer(int serverPort, ArgsDaemon args)
  {
    ArrayList<ServerConfigBoot> servers = parseConfig(args);
    
    for (ServerConfigBoot serverConfig : servers) {
      if (serverPort == serverConfig.getPort()) {
        return serverConfig;
      }
    }
    
    throw error("Server with port={0} is an unknown server in {1}", 
                serverPort, servers);
  }
  
  private ArrayList<ServerConfigBoot> parseConfig(ArgsDaemon args)
  {
    ConfigBoot config = new BootConfigParser().parseBoot(args, _manager.getSystem());
    
    return config.findStartServers(args);
  }

  private ConfigException error(String msg, Object ...args)
  {
    return new ConfigException(L.l(msg, args));
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class FailSafeShutdown implements Runnable {
    private long _delay;
    
    FailSafeShutdown(long delay)
    {
      _delay = delay;
    }
    
    @Override
    public void run()
    {
      try {
        Thread.sleep(_delay);
      } catch (Exception e) {
      }

      System.exit(ExitCode.FAIL_SAFE_HALT.ordinal());
    }
  }
  
  class ResultCompletion extends Result.Wrapper<String,String> {
    ResultCompletion(Result<String> cont)
    {
      super(cont);
      
      Objects.requireNonNull(cont);
    }

    @Override
    public void ok(String result)
    {
      getNext().ok(result);
      
      if (! isServerActive()) {
        shutdown(ShutdownModeAmp.GRACEFUL, Result.ignore());
      }
    }

    @Override
    public void fail(Throwable exn)
    {
      getNext().fail(exn);
      
      if (! isServerActive()) {
        shutdown(ShutdownModeAmp.GRACEFUL, Result.ignore());
      }
    }
  }
  
  private class ResultRestart extends Result.Wrapper<String,String> {
    private int _port;
    private String []_argv;
    
    ResultRestart(Result<String> result,
                  int port,
                  String []argv)
    {
      super(result);
      
      _port = port;
      _argv = argv;
    }

    @Override
    public void ok(String result)
    {
      start(_port, _argv, getNext());
    }

    @Override
    public void fail(Throwable exn)
    {
      log.log(Level.FINE, exn.toString(), exn);
      
      start(_port, _argv, getNext());
    }
  }
  
  private static class ShutdownCompletion {
    private int _count;
    private final Result<String> _result;
    private AtomicInteger _completeCount = new AtomicInteger();
    
    private String _msg = "ok";
    
    ShutdownCompletion(int count, Result<String> result)
    {
      _count = count;
      _result = result;
      
      validateCompletion();
    }
    
    Result<String> createResult()
    {
      return new ShutdownResult(this);
    }
    
    void completed()
    {
      _completeCount.incrementAndGet();
      
      validateCompletion();
    }
    
    void validateCompletion()
    {
      if (_count <= _completeCount.get()) {
        _result.ok(_msg);
        
        ServiceRef.flushOutbox();
        
        log.warning("Watchdog shutting down: " + _msg);
        
        try {
          Thread.sleep(100);
        } catch (Exception e) {
        }
        
        System.exit(ExitCode.OK.ordinal());
      }
    }
  }

  private static class FailSafeKill implements Runnable
  {
    private final ArrayList<ChildWatchdogService> _watchdogList;
    
    FailSafeKill(ArrayList<ChildWatchdogService> watchdogList)
    {
      _watchdogList = watchdogList;
    }
    
    @Override
    public void run()
    {
      try {
        Thread.sleep(STOP_TIMEOUT);
      } catch (Exception e) {
      } finally {
        for (ChildWatchdogService server : _watchdogList) {
          server.kill(Result.ignore());
        }
      }
    }
  }
  
  private static class ShutdownResult implements Result<String> {
    private ShutdownCompletion _cont;
    
    ShutdownResult(ShutdownCompletion cont)
    {
      _cont = cont;
    }

    @Override
    public void handle(String result, Throwable exn)
    {
      _cont.completed();
      
      if (exn != null) {
        log.log(Level.FINE, exn.toString(), exn);
      }
    }
  }
}
