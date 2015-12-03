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

package com.caucho.env.thread1;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.warning.WarningService;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
abstract public class AbstractTaskWorker1 implements Runnable {
  private static final Logger log
    = Logger.getLogger(AbstractTaskWorker1.class.getName());
  
  private static final int TASK_PARK = 0;
  private static final int TASK_SLEEP = 1;
  private static final int TASK_READY = 2;
  
  private static final AtomicLong _idGen = new AtomicLong();
  
  private final AtomicInteger _taskState = new AtomicInteger();
  private final AtomicBoolean _isActive = new AtomicBoolean();

  private final ClassLoader _classLoader;
  
  private final String _threadName;
  
  private long _workerIdleTimeout = 30000L;
  private boolean _isClosed;
  
  private int _loopCount = 0;

  private volatile Thread _thread;

  protected AbstractTaskWorker1(ClassLoader classLoader)
  {
    _classLoader = classLoader;
    
    _threadName = toString() + "-" + _idGen.incrementAndGet();
  }

  protected boolean isPermanent()
  {
    return false;
  }
  
  protected void setWorkerIdleTimeout(long timeout)
  {
    if (timeout < 0)
      throw new IllegalArgumentException();
    
    _workerIdleTimeout = timeout;
  }
  
  protected void setLoopCount(int count)
  {
    _loopCount = count;
  }
  
  public boolean isTaskActive()
  {
    return _isActive.get();
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  abstract public long runTask();

  public void destroy()
  {
    _isClosed = true;

    wake();
    
    Thread thread = _thread;

    if (thread != null)
      LockSupport.unpark(thread);
  }

  public final void wake()
  {
    int oldState = _taskState.getAndSet(TASK_READY);

    if (! _isActive.getAndSet(true)) {
      startWorkerThread();
    }

    if (oldState == TASK_PARK) {
      Thread thread = _thread;

      if (thread != null) {
        LockSupport.unpark(thread);
      }
    }
  }
  
  abstract protected void startWorkerThread();

  protected String getThreadName()
  {
    // return getClass().getSimpleName() + "-" + _idGen.incrementAndGet();
    return _threadName;
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
      _thread.setName(getThreadName());
      
      onThreadStart();

      long now = getCurrentTimeActual();
      
      long expires;
      
      if (_workerIdleTimeout > 0)
        expires = now + _workerIdleTimeout;
      else
        expires = 0;
      
      do {
        while (_taskState.getAndSet(TASK_SLEEP) == TASK_READY) {
          thread.setContextClassLoader(_classLoader);

          long delta = runTask();
          
          now = getCurrentTimeActual();
          
          if (delta > 0) {
            expires = now + delta;
          }
          else if (delta == 0) {
            expires = 0;
          }
          else if (_workerIdleTimeout > 0) {
            expires = now + _workerIdleTimeout;
          }
          else {
            expires = 0;
          }
        }

        if (isClosed())
          return;
        
        for (int i = _loopCount; i >= 0; i--) {
          if (_taskState.get() == TASK_READY)
            break;
        }
        
        if (expires > 0 && _taskState.compareAndSet(TASK_SLEEP, TASK_PARK)) {
          Thread.interrupted();
          LockSupport.parkUntil(expires);
        }
        
        if (isPermanent())
          _taskState.set(TASK_READY);
      } while (_taskState.get() == TASK_READY
               || isPermanent()
               || getCurrentTimeActual() < expires);
    } catch (Throwable e) {
      WarningService.sendCurrentWarning(this, e);
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _thread = null;

      _isActive.set(false);

      onThreadComplete();
      
      if (_taskState.get() == TASK_READY)
        wake();
      
      thread.setName(oldName);
    }
  }
  
  protected long getCurrentTimeActual()
  {
    return CurrentTime.getCurrentTimeActual();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
