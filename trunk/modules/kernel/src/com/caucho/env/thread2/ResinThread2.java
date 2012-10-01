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

package com.caucho.env.thread2;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
final class ResinThread2 extends Thread {
  private static final Logger log 
    = Logger.getLogger(ResinThread2.class.getName());
  
  private final String _name;
  
  private final ThreadPool2 _pool;
  private final ThreadLauncher2 _launcher;
  
  private boolean _isClose;
  
  private volatile ClassLoader _taskLoader;
  
  private final AtomicReference<Runnable> _taskRef
    = new AtomicReference<Runnable>();

  ResinThread2(int id, ThreadPool2 pool, ThreadLauncher2 launcher)
  {
    _name = "resin-" + getId();

    _pool = pool;
    _launcher = launcher;

    setDaemon(true);
  }

  /**
   * Returns the name.
   */
  public String getDebugName()
  {
    return _name;
  }

  /**
   * Returns the thread id.
   */
  public long getThreadId()
  {
    return getId();
  }

  /**
   * Sets the thread's task.  Must be called inside _idleLock
   */
  final boolean scheduleTask(Runnable task, ClassLoader loader)
  {
    if (_isClose)
      return false;
    
    _taskLoader = loader;
    
    if (_taskRef.getAndSet(task) != null) {
      System.out.println("BAD: getandset");
    }

    return true;
  }

  /**
   * Wake the thread.  Called outside of _idleLock
   */
  final void close()
  {
    _isClose = true;
    LockSupport.unpark(this);
  }

  /**
   * The main thread execution method.
   */
  @Override
  public void run()
  {
    try {
      _launcher.onChildIdleBegin();
      _launcher.onChildThreadLaunchBegin();
      
      runTasks();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _launcher.onChildIdleEnd();
      _launcher.onChildThreadLaunchEnd();
    }
  }

  private void runTasks()
  {
    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    
    Thread thread = this;
    setName(_name);
    
    while (! _isClose) {
      Runnable task = null;
      ClassLoader classLoader = null;

      if (_taskRef.get() != null) {
        
      }
      else if (_launcher.isIdleExpire()) {
        return;
      }
      else if (_pool.execute(this)) {
      }
      else {
        park();
        
        if (_taskRef.get() == null) {
          continue;
        }
      }
      
      classLoader = _taskLoader;
      _taskLoader = null;
      
      task = _taskRef.getAndSet(null);
      
      if (_isClose) {
        return;
      }
      
      if (task == null) {
        System.out.println("LAUNCH error: " + this);
        continue;
      }
      
      try {
        // if the task is available, run it in the proper context
        thread.setContextClassLoader(classLoader);
        _launcher.onChildIdleEnd();

        task.run();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        _launcher.onChildIdleBegin();
        thread.setContextClassLoader(systemClassLoader);
      }
    }
  }
  
  void unpark()
  {
    // task must already be assigned
    LockSupport.unpark(this);
  }
  
  private void park()
  {
    // _pool.beginIdle(this);
    setName(_name);

    while (! _isClose && _taskRef.get() == null) {
      Thread.interrupted();
      LockSupport.park();
    }
  }
}
