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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.thread.ThreadPool;
import com.caucho.lifecycle.Lifecycle;

/**
 * Thread responsible for the Resin restart capability, managing and
 * restarting the WatchdogProcess.
 *
 * Each WatchdogProcess corresponds to a single Resin instantiation.  When
 * Resin exits, the WatchdogProcess completes, and WatchdogTask will
 * create a new one.
 */
class WatchdogChildTask implements Runnable
{
  private static final Logger log
    = Logger.getLogger(WatchdogChildTask.class.getName());
  
  private static final long BAD_CONFIG_DELAY_TIME = 30 * 1000L;

  private final ResinSystem _system;
  private final WatchdogChild _watchdog;
  
  private final Lifecycle _lifecycle = new Lifecycle();

  private WatchdogChildProcess _process;
  
  private boolean _isRestart;
  private String _restartMessage;
  private ExitCode _previousExitCode;
  
  private String _shutdownMessage;

  WatchdogChildTask(ResinSystem system,
                    WatchdogChild watchdog)
  {
    _system = system;
    _watchdog = watchdog;

    if (watchdog == null)
      throw new NullPointerException();
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
    WatchdogChildProcess process = _process;
    
    if (process == null)
      return 0;
    
    int pid = process.getPid();
      
    if (pid > 0)
      return pid;
    
    PidQuery pidQuery = new PidQuery();
    
    try {
      PidQuery result = (PidQuery) process.queryGet(pidQuery);

      if (result != null)
        return result.getPid();
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
    WatchdogChildProcess process = _process;
    
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
  
  Serializable queryGet(Serializable payload)
  {
    WatchdogChildProcess process = _process;
    
    if (process != null)
      return process.queryGet(payload);
    else
      return null;
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
  public void stop()
  {
    if (! _lifecycle.toDestroy())
      return;

    WatchdogChildProcess process = _process;
    _process = null;
    
    if (process != null) {
      process.stop();

      process.waitForExit();
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
      thread.setName("watchdog-" + _watchdog.getId());
      
      int i = 0;
      long retry = Long.MAX_VALUE;
    
      while (_lifecycle.isActive() && i++ < retry) {
        String id = String.valueOf(_watchdog.getId());
        WatchdogChildProcess process;

        _watchdog.notifyTaskStarted();

        log.info(_watchdog + " starting");

        process = new WatchdogChildProcess(id, _system, _watchdog,
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

      log.info(_watchdog + " stopped");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _lifecycle.toDestroy();

      _watchdog.completeTask(this);
    }
  }
  
  /**
   * kills the task
   */
  void kill()
  {
    _lifecycle.toDestroy();
    
    WatchdogChildProcess process = _process;
    _process = null;
    
    if (process != null)
      process.kill();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _watchdog + "]";
  }
}
