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

package com.caucho.util;

import com.caucho.config.ConfigException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
abstract public class TaskWorker implements Runnable {
  private final AtomicBoolean _isTask = new AtomicBoolean();
  private final AtomicBoolean _isActive = new AtomicBoolean();

  private final ClassLoader _classLoader;
  private long _idleTimeout = 30000L;
  
  private volatile Thread _thread;

  protected TaskWorker()
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
  }

  abstract public void runTask();

  public final void wake()
  {
    if (_isTask.getAndSet(true))
      return;

    if (! _isActive.getAndSet(true)) {
      ThreadPool.getCurrent().schedule(this);
    }
    else {
      Thread thread = _thread;

      if (thread != null)
        LockSupport.unpark(thread);
    }
  }

  public final void run()
  {
    try {
      _thread = Thread.currentThread();
      _thread.setContextClassLoader(_classLoader);
      
      do {
        while (_isTask.getAndSet(false)) {
          runTask();
        }

        Thread.interrupted();
        LockSupport.parkNanos(_idleTimeout * 1000000L);
      } while (_isTask.get());
    } finally {
      _thread = null;
      
      _isActive.set(false);

      if (_isTask.get()) {
        wake();
      }
    }
  }
}
