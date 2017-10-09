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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import com.caucho.boot.BootCommand.ResultCommand;
import com.caucho.config.program.ConfigProgram;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.WatchdogMXBean;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Thread responsible for watching a backend server.
 */
class WatchdogChild
{
  private static final L10N L = new L10N(WatchdogChild.class);
  
  private static final long MINUTE = 60 * 1000L;
  private static final long HOUR = 60 * MINUTE;
  private static final long DAY = 24 * HOUR;
  
  private final String _id;

  private final ResinSystem _system;
  private final WatchdogConfig _config;
  private final WatchdogAdmin _admin;
  
  private AtomicReference<WatchdogChildTask> _taskRef
    = new AtomicReference<WatchdogChildTask>();
  
  private boolean _isConsole;

  // statistics
  private Date _initialStartTime;
  private Date _lastStartTime;
  private int _startCount;

  WatchdogChild(String id,
                ResinSystem system,
                WatchdogArgs args, 
                Path rootDirectory,
                BootClusterConfig cluster,
                int index)
  {
    this(system,
         new WatchdogConfig(id, cluster, args, rootDirectory, index));
  }

  WatchdogChild(ResinSystem system,
                WatchdogConfig config)
  {
    _id = config.getId();
    
    _config = config;
    
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
   * Returns the watchdog arguments.
   */
  WatchdogArgs getArgs()
  {
    return _config.getArgs();
  }
  
  /**
   * Returns the java startup args
   */
  String []getArgv()
  {
    return _config.getArgv();
  }

  /**
   * Returns the config state of the watchdog
   */
  public WatchdogConfig getConfig()
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
   * Returns true for a standalone start.
   */
  public boolean isConsole()
  {
    return _isConsole;
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

  Path getResinHome()
  {
    return _config.getResinHome();
  }

  Path getResinRoot()
  {
    return _config.getResinRoot();
  }
  
  Path getResinConf()
  {
    return _config.getResinConf();
  }
  
  ConfigProgram getStdoutLog()
  {
    if (_config.getCluster() != null)
      return _config.getCluster().getStdoutLog();
    else
      return null;
  }
  
  boolean isElasticServer()
  {
    return _config.isElastic();
  }
  
  int getElasticServerPort()
  {
    return _config.getElasticServerPort();
  }
  
  String getElasticServerCluster()
  {
    return _config.getElasticServerCluster();
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
    WatchdogChildTask task = _taskRef.get();
    
    if (task == null)
      return "inactive";
    else
      return task.getState();
  }

  int getPid()
  {
    WatchdogChildTask task = _taskRef.get();
    
    if (task != null)
      return task.getPid();
    else
      return 0;
  }
  
  String getUptimeString()
  {
    long uptime = 0;
    
    WatchdogChildTask task = _taskRef.get();
    
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
    WatchdogChildTask task = _taskRef.get();
    
    if (task != null)
      return task.isRestart();
    else
      return false;
  }
  
  String getRestartMessage()
  {
    WatchdogChildTask task = _taskRef.get();
    
    if (task != null)
      return task.getRestartMessage();
    else
      return null;
  }
  
  ExitCode getPreviousExitCode()
  {
    WatchdogChildTask task = _taskRef.get();
    
    if (task != null)
      return task.getPreviousExitCode();
    else
      return null;
  }
  
  Serializable queryGet(Serializable payload)
  {
    WatchdogChildTask task = _taskRef.get();
    
    if (task != null)
      return task.queryGet(payload);
    else
      return null;
  }

  boolean isVerbose()
  {
    return _config.isVerbose();
  }

  public ResultCommand startConsole()
  {
    _isConsole = true;
    
    WatchdogChildTask task = new WatchdogChildTask(_system, this);

    if (! _taskRef.compareAndSet(null, task)) {
      return ResultCommand.FAIL_RETRY;
    }
    
    task.start();

    return ResultCommand.FAIL_RETRY;
  }

  /**
   * Starts the watchdog instance.
   */
  public void start()
  {
    WatchdogChildTask task = new WatchdogChildTask(_system, this);

    if (! _taskRef.compareAndSet(null, task)) {
      WatchdogChildTask oldTask = _taskRef.get();
      
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
  public void stop()
  {
    WatchdogChildTask task = _taskRef.getAndSet(null);
    
    if (task != null)
      task.stop();
  }

  /**
   * Stops the watchdog instance
   */
  public void restart()
  {
    stop();
    
    start();
  }

  /**
   * Kills the watchdog instance
   */
  public void kill()
  {
    WatchdogChildTask task = _taskRef.getAndSet(null);
    
    if (task != null)
      task.kill();
  }

  public void close()
  {
    kill();

    _admin.unregister();
  }

  void notifyTaskStarted()
  {
    _startCount++;
    _lastStartTime = new Date(CurrentTime.getExactTime());
    
    if (_initialStartTime == null)
      _initialStartTime = _lastStartTime; 
  }

  void completeTask(WatchdogChildTask task)
  {
    _taskRef.compareAndSet(task, null);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }

  class WatchdogAdmin extends AbstractManagedObject implements WatchdogMXBean
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
      return WatchdogChild.this.getId();
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
      return WatchdogChild.this.getResinHome().getNativePath();
    }

    public String getResinRoot()
    {
      return WatchdogChild.this.getResinRoot().getNativePath();
    }

    public String getResinConf()
    {
      return WatchdogChild.this.getResinConf().getNativePath();
    }

    public String getUserName()
    {
      String userName = WatchdogChild.this.getUserName();

      if (userName != null)
        return userName;
      else
        return System.getProperty("user.name");
    }

    public String getState()
    {
      WatchdogChildTask task = _taskRef.get();
    
      if (task == null)
        return "inactive";
      else
        return task.getState();
    }

    //
    // statistics
    //

    public Date getInitialStartTime()
    {
      return _initialStartTime;
    }

    public Date getStartTime()
    {
      return _lastStartTime;
    }

    public int getStartCount()
    {
      return _startCount;
    }

    //
    // operations
    //

    public void start()
    {
      WatchdogChild.this.start();
    }

    public void stop()
    {
      WatchdogChild.this.stop();
    }

    public void kill()
    {
      WatchdogChild.this.kill();
    }
  }
}
