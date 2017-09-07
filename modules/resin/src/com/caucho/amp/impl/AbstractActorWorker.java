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

package com.caucho.amp.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.TaskWorker;
import com.caucho.env.warning.WarningService;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;

/**
 * A manager for actor-created threads.
 */
abstract class AbstractActorWorker implements Runnable {
  private static final Logger log
    = Logger.getLogger(AbstractActorWorker.class.getName());
  
  private static final int TASK_PARK = 0;
  private static final int TASK_SLEEP = 1;
  private static final int TASK_READY = 2;
  
  private static final AtomicLong _idGen = new AtomicLong();
  
  private final AtomicInteger _taskState = new AtomicInteger();
  private final AtomicBoolean _isActive = new AtomicBoolean();

  private final Executor _executor;
  private final ClassLoader _classLoader;
  
  private final String _name;
  
  private long _idleTimeoutNanos = 0;
  private boolean _isClosed;
  
  private volatile Thread _thread;

  protected AbstractActorWorker(String name,
                                Executor executor,
                                ClassLoader classLoader)
  {
    _name = name;
    _executor= executor;
    _classLoader = classLoader;
  }
  
  public final boolean isTaskActive()
  {
    return _isActive.get();
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  public void setIdleTimeout(long idleTimeout, TimeUnit timeUnit)
  {
    _idleTimeoutNanos = timeUnit.toNanos(idleTimeout);
  }
  
  public long getIdleTimeoutNanos()
  {
    return _idleTimeoutNanos;
  }
  
  public String getName()
  {
    return _name;
  }
  
  abstract public void runTask();

  public void close()
  {
    _isClosed = true;

    wake();
    
    Thread thread = _thread;

    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }

  public final void wake()
  {
    int oldState = _taskState.getAndSet(TASK_READY);

    if (_isActive.compareAndSet(false, true)) {
      startWorkerThread();
    }

    if (oldState == TASK_PARK) {
      Thread thread = _thread;

      if (thread != null) {
        unpark(thread);
      }
    }
  }
  
  protected void startWorkerThread()
  {
    _executor.execute(this);
  }
  
  protected void unpark(Thread thread)
  {
    LockSupport.unpark(thread);
  }

  protected String getThreadName()
  {
    return getName();
  }
  
  protected void onThreadStart()
  {
  }
  
  protected void onThreadComplete()
  {
  }

  @Override
  public final void run()
  {
    Thread thread = Thread.currentThread(); 
    String oldName = thread.getName();
    
    try {
      _thread = thread;
      thread.setContextClassLoader(_classLoader);
      thread.setName(getThreadName());
      
      onThreadStart();

      do {
        while (_taskState.getAndSet(TASK_SLEEP) == TASK_READY) {
          thread.setContextClassLoader(_classLoader);

          runTask();
        }
        
        long idleTimeout = getIdleTimeoutNanos();

        if (isClosed())
          return;
        
        if (idleTimeout > 0 && _taskState.compareAndSet(TASK_SLEEP, TASK_PARK)) {
          Thread.interrupted();
          LockSupport.parkNanos(idleTimeout);
        }
      } while (_taskState.get() == TASK_READY);
    } catch (Throwable e) {
      onException(e);
    } finally {
      _thread = null;

      _isActive.set(false);

      onThreadComplete();
      
      if (_taskState.get() == TASK_READY) {
        wake();
      }
      
      thread.setName(oldName);
    }
  }
  
  protected void onException(Throwable e)
  {
    log.log(Level.WARNING, e.toString(), e);
  }
  
  protected long getCurrentTimeActual()
  {
    return CurrentTime.getCurrentTimeActual();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
