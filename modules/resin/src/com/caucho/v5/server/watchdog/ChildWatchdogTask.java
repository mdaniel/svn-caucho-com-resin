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

import io.baratine.service.Result;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.L10N;

/**
 * Thread responsible for the Resin restart capability, managing and
 * restarting the WatchdogProcess.
 *
 * Each WatchdogProcess corresponds to a single Resin instantiation.  When
 * Resin exits, the WatchdogProcess completes, and WatchdogTask will
 * create a new one.
 */
class ChildWatchdogTask implements Runnable
{
  private static final Logger log
    = Logger.getLogger(ChildWatchdogTask.class.getName());
  
  private static final L10N L = new L10N(ChildWatchdogTask.class);
  
  private static final long BAD_CONFIG_DELAY_TIME = 30 * 1000L;

  private final SystemManager _system;
  private final ChildWatchdog _server;
  
  private final Lifecycle _lifecycle = new Lifecycle();

  private ChildWatchdogProcess _process;
  
  private boolean _isRestart;
  private String _restartMessage;
  private ExitCode _previousExitCode;
  
  private String _shutdownMessage;

  ChildWatchdogTask(SystemManager system,
                     ChildWatchdog server)
  {
    Objects.requireNonNull(system);
    Objects.requireNonNull(server);
    
    _system = system;
    _server = server;
  }

  /**
   * True if the Resin server is currently active.
   */
  boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Returns the state name.
   */
  String getState()
  {
    return _lifecycle.getStateName();
  }

  /**
   * Returns the pid of the current Resin process, when the pid is
   * available through JNI.
   */
  int getPid()
  {
    ChildWatchdogProcess process = _process;
    
    if (process == null)
      return 0;
    
    int pid = process.getPid();
      
    if (pid > 0)
      return pid;
    
    try {
      Integer result = process.getLinkServiceServer().queryPid();

      if (result != null) {
        return result;
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return 0;
  }

  /**
   * Returns the uptime of the current Resin process.
   */
  long getUptime()
  {
    ChildWatchdogProcess process = _process;
    
    if (process == null)
      return 0;
    
    return process.getUptime();
  }

  boolean isRestart()
  {
    return _isRestart;
  }
  
  String getRestartMessage()
  {
    return _restartMessage;
  }
  
  ExitCode getPreviousExitCode()
  {
    return _previousExitCode;
  }
  
  String getShutdownMessage()
  {
    return _shutdownMessage;
  }
  
  void setShutdownMessage(String msg)
  {
    _shutdownMessage = msg;
  }

  /**
   * Starts management of the watchdog process
   */
  public void start()
  {
    if (! _lifecycle.toActive())
      return;
    
    _isRestart = false;
    _restartMessage = "user start from watchdog";

    ThreadPool.getCurrent().schedule(this);
  }

  /**
   * Stops the watchdog process.  Once stopped, the WatchdogProcess will
   * not be reused.
   */
  public void stop(ShutdownModeAmp mode,
                   Result<String> result)
  {
    if (! _lifecycle.toDestroy()) {
      result.ok(L.l("Server {0} is already stopped", this));
      return;
    }

    ChildWatchdogProcess process = _process;
    _process = null;

    if (process != null) {
      process.stop(mode, result);

      // process.waitForExit(5, TimeUnit.SECONDS);
    }
    else {
      result.ok(L.l("Server {0} is already stopped", this));
    }
  }

  /**
   * Main thread watching over the health of the Resin instances.
   */
  @Override
  public void run()
  {
    try {
      Thread thread = Thread.currentThread();
      thread.setName("watchdog-" + _server.getId());
      
      int i = 0;
      long retry = Long.MAX_VALUE;
    
      while (_lifecycle.isActive() && i++ < retry) {
        String id = _server.getId();
        ChildWatchdogProcess process;

        _server.notifyTaskStarted();

        log.info(_server + " starting");

        process = new ChildWatchdogProcess(id, _system, _server,
                                           this);
        _process = process;
        
        String lastShutdownMessage = _shutdownMessage;

        try {
          _process.run();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        } finally {
          _process = null;

          if (process != null)
            process.kill();
        }
        
        _isRestart = true;
        _restartMessage = process.getExitMessage();
        
        if (_lifecycle.isActive()
            && process.getStatus() == ExitCode.BAD_CONFIG.ordinal()) {
          // pause before restarting a bad result
          try {
            Thread.sleep(BAD_CONFIG_DELAY_TIME);
          } catch (Exception e) {
            
          }
        }
         
        if (lastShutdownMessage == _shutdownMessage) {
          _shutdownMessage = null;
        }
      }

      log.info(_server + " stopped");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _lifecycle.toDestroy();

      _server.completeTask(this);
    }
  }
  
  /**
   * kills the task
   */
  void kill()
  {
    _lifecycle.toDestroy();
    
    ChildWatchdogProcess process = _process;
    _process = null;
    
    if (process != null)
      process.kill();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _server + "]";
  }
}
