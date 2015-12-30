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
import io.baratine.core.ResultFuture;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.cli.server.OpenPort;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.management.server.WatchdogMXBean;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

/**
 * Thread responsible for watching a backend server.
 */
class ChildWatchdog
{
  private static final L10N L = new L10N(ChildWatchdog.class);
  
  private static final long MINUTE = 60 * 1000L;
  private static final long HOUR = 60 * MINUTE;
  private static final long DAY = 24 * HOUR;
  
  private final String _id;
  private final int _port;

  private final SystemManager _system;
  private final ArgsWatchdog _args;
  private final ServerConfigBoot _serverConfig;
  
  private ChildWatchdogConfig _config;
  
  private final WatchdogAdmin _admin;
  
  private AtomicReference<ChildWatchdogTask> _taskRef
    = new AtomicReference<>();
  
  private ChildWatchdogService _serviceSelf;

  // statistics
  private Date _initialStartTime;
  private Date _lastStartTime;
  private int _startCount;

  ChildWatchdog(SystemManager system,
                ChildWatchdogService serviceSelf,
                 ServerConfigBoot server,
                 ArgsWatchdog args)
  {
    _id = server.getId();
    
    _serverConfig = server;
    _args = args;
    _port = server.getPort();
    
    _serviceSelf = serviceSelf;
    
    _config = new ChildWatchdogConfig(server, args);
    
    _admin = new WatchdogAdmin();
    
    _system = system;
  }

  /**
   * Returns the server id of the watchdog.
   */
  public String getId()
  {
    return _id;
  }
  
  /**
   * Returns the unique port of the watchdog.
   */
  public int getPort()
  {
    return _port;
  }

  public ChildWatchdogService getService()
  {
    return _serviceSelf;
  }

  /**
   * Returns the watchdog arguments.
   */
  ArgsWatchdog getArgs()
  {
    return _args;
  }
  
  /**
   * Returns the java startup args
   */
  String []getArgv()
  {
    return getArgs().getArgv();
  }

  /**
   * Returns the config state of the watchdog
   */
  public ChildWatchdogConfig getConfig()
  {
    return _config;
  }

  /**
   * Sets the config state of the watchdog
   */
  /*
  public void setConfig(WatchdogConfig config)
  {
    _config = config;
  }
  */
  
  /**
   * Returns the JAVA_HOME for the Resin instance
   */
  public Path getJavaHome()
  {
    return _config.getJavaHome();
  }
  
  /**
   * Returns the location of the java executable
   */
  public String getJavaExe()
  {
    return _config.getJavaExe();
  }

  /**
   * Returns the JVM mode
   */
  public String getJvmMode()
  {
    return _config.getJvmMode();
  }

  /**
   * Returns the JVM arguments for the instance
   */
  public ArrayList<String> getJvmArgs()
  {
    return _config.getJvmArgs();
  }

  /**
   * Returns the JVM classpath for the instance
   */
  public ArrayList<String> getJvmClasspath()
  {
    return _config.getJvmClasspath();
  }

  /**
   * Returns the system classloader to use for Resin.
   */
  public String getSystemClassLoader()
  {
    return _config.getSystemClassLoader();
  }

  /**
   * Returns the setuid user name.
   */
  public String getUserName()
  {
    return _config.getUserName();
  }

  /**
   * Returns the setgid group name.
   */
  public String getGroupName()
  {
    return _config.getGroupName();
  }

  /**
   * Returns the jvm-foo-log.log file path
   */
  public Path getLogPath()
  {
    return _config.getLogPath();
  }

  /**
   * Returns the maximum time to wait for a shutdown
   */
  public long getShutdownWaitTime()
  {
    return _config.getShutdownWaitTime();
  }

  /**
   * Returns the watchdog-port for this watchdog instance
   */
  public int getWatchdogPort()
  {
    return _config.getWatchdogPort();
  }

  /**
   * Returns the watchdog-address for this watchdog instance
   */
  public String getWatchdogAddress()
  {
    return _config.getWatchdogAddress();
  }

  Iterable<OpenPort> getPorts()
  {
    return _config.getPorts();
  }

  Path getChroot()
  {
    return _config.getChroot();
  }

  Path getPwd()
  {
    return _config.getPwd();
  }

  Path getHomeDir()
  {
    return _config.getHomeDirectory();
  }

  Path getRootDir()
  {
    return _config.getRootDir();
  }
  
  Path getConfigPath()
  {
    return _config.getResinConf();
  }
  
  ConfigProgram getStdoutLog()
  {
    /* XXX:
    if (_config.getCluster() != null)
      return _config.getCluster().getStdoutLog();
    else
      return null;
      */
    
    return null;
  }
  
  boolean isDynamic()
  {
    return _serverConfig.isDynamic();
  }

  boolean hasXmx()
  {
    return _config.hasXmx();
  }

  boolean hasXss()
  {
    return _config.hasXss();
  }

  boolean is64bit()
  {
    return _config.is64bit();
  }

  public String getState()
  {
    ChildWatchdogTask task = _taskRef.get();
    
    if (task == null)
      return "inactive";
    else
      return task.getState();
  }

  int getPid()
  {
    ChildWatchdogTask task = _taskRef.get();
    
    if (task != null)
      return task.getPid();
    else
      return 0;
  }
  
  String getUptimeString()
  {
    long uptime = 0;
    
    ChildWatchdogTask task = _taskRef.get();
    
    if (task != null)
      uptime = task.getUptime();
    
    if (uptime <= 0)
      return "--";
    
    long d = uptime / DAY;
    
    StringBuilder sb = new StringBuilder();
    
    sb.append(d + " day" + (d == 1 ? "" : "s"));
    
    long h = uptime % DAY / HOUR;
    
    sb.append(" ").append(h / 10).append(h % 10).append("h");
    
    long m = uptime % HOUR / MINUTE;
    
    sb.append(m / 10).append(m % 10);
    
    return sb.toString();
  }
  
  boolean isRestart()
  {
    ChildWatchdogTask task = _taskRef.get();
    
    if (task != null)
      return task.isRestart();
    else
      return false;
  }
  
  String getRestartMessage()
  {
    ChildWatchdogTask task = _taskRef.get();
    
    if (task != null)
      return task.getRestartMessage();
    else
      return null;
  }
  
  ExitCode getPreviousExitCode()
  {
    ChildWatchdogTask task = _taskRef.get();
    
    if (task != null)
      return task.getPreviousExitCode();
    else
      return null;
  }

  boolean isVerbose()
  {
    return _config.isVerbose();
  }

  /**
   * Starts the watchdog instance.
   */
  public void start()
  {
    ChildWatchdogTask task = new ChildWatchdogTask(_system, this);

    if (! _taskRef.compareAndSet(null, task)) {
      ChildWatchdogTask oldTask = _taskRef.get();
      
      if (oldTask != null && ! oldTask.isActive()) {
        _taskRef.set(task);
      }
      else if (_taskRef.compareAndSet(null, task)) {
      }
      else {
        throw new IllegalStateException(L.l("Can't start new Resin server '{0}' because one is already running '{1}'", _id, task));
      }
    }

    task.start();
  }

  public boolean isActive()
  {
    return _taskRef.get() != null;
  }

  /**
   * Stops the watchdog instance
   */
  public void stop(ShutdownModeAmp mode,
                   Result<String> result)
  {
    ChildWatchdogTask task = _taskRef.getAndSet(null);
    
    if (task != null) {
      task.stop(mode, result);
    }
    else {
      result.ok(L.l("Server {0} is already stopped.", this));
    }
  }

  /**
   * Stops the watchdog instance
   */
  /*
  private void restart()
  {
    ServiceFuture<Boolean> future = new ServiceFuture<>();
    
    ShutdownMode mode = ShutdownMode.GRACEFUL;
    
    stop(mode, future);
    
    future.get(1, TimeUnit.SECONDS);
    
    start();
  }
  */

  /**
   * Kills the watchdog instance
   */
  public void kill(Result<String> result)
  {
    if (result != null) {
      result.ok(L.l("Server {0} killed", this));
    }
    
    ChildWatchdogTask task = _taskRef.getAndSet(null);
    
    if (task != null) {
      task.kill();
    }
  }

  public void close()
  {
    kill(null);

    _admin.unregister();
  }

  void notifyTaskStarted()
  {
    _startCount++;
    _lastStartTime = new Date(CurrentTime.getExactTime());
    
    if (_initialStartTime == null)
      _initialStartTime = _lastStartTime; 
  }

  void completeTask(ChildWatchdogTask task)
  {
    _taskRef.compareAndSet(task, null);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "," + _port + "]";
  }

  class WatchdogAdmin extends ManagedObjectBase implements WatchdogMXBean
  {
    WatchdogAdmin()
    {
      registerSelf();
    }

    void unregister()
    {
      unregisterSelf();
    }

    public String getId()
    {
      return ChildWatchdog.this.getId();
    }
    
    public String getName()
    {
      return getId();
    }

    @Override
    public String getType()
    {
      return "Watchdog";
    }

    public String getResinHome()
    {
      return ChildWatchdog.this.getHomeDir().getNativePath();
    }

    public String getResinRoot()
    {
      return ChildWatchdog.this.getRootDir().getNativePath();
    }

    public String getResinConf()
    {
      return ChildWatchdog.this.getConfigPath().getNativePath();
    }

    public String getUserName()
    {
      String userName = ChildWatchdog.this.getUserName();

      if (userName != null)
        return userName;
      else
        return System.getProperty("user.name");
    }

    public String getState()
    {
      ChildWatchdogTask task = _taskRef.get();
    
      if (task == null)
        return "inactive";
      else
        return task.getState();
    }

    //
    // statistics
    //

    @Override
    public Date getInitialStartTime()
    {
      return _initialStartTime;
    }

    @Override
    public Date getStartTime()
    {
      return _lastStartTime;
    }

    @Override
    public int getStartCount()
    {
      return _startCount;
    }

    //
    // operations
    //

    @Override
    public void start()
    {
      ChildWatchdog.this.start();
    }

    @Override
    public void stop()
    {
      ResultFuture<String> future = new ResultFuture<>();
      
      ChildWatchdog.this.stop(ShutdownModeAmp.GRACEFUL, future);
      
      future.get(10, TimeUnit.SECONDS);
    }

    public void kill()
    {
      ChildWatchdog.this.kill(null);
    }
  }
}
