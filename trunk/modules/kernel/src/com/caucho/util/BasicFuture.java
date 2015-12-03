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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

/**
 * basic future
 */
public class BasicFuture<V> implements Future<V> {
  private volatile V _value;
  private volatile boolean _isDone;
  private volatile RuntimeException _exn;
  private volatile Thread _thread;
  
  @Override
  public boolean isDone()
  {
    return _isDone;
  }

  @Override
  public V get() throws InterruptedException, ExecutionException
  {
    try {
      return get(Integer.MAX_VALUE, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException,
             ExecutionException, 
             TimeoutException
  {
    Thread thread = Thread.currentThread();
    
    try {
      _thread = thread;
      
      long expires = System.currentTimeMillis() + unit.toMillis(timeout);
      
      do {
        if (_isDone) {
          if (_exn != null) {
            throw _exn;
          }
          else {
            return _value;
          }
        }
        
        Thread.interrupted();
        LockSupport.parkUntil(expires);
      } while (System.currentTimeMillis() < expires);
      
      throw new TimeoutException(toString() + ": " + timeout + " " + unit);
    } finally {
      _thread = null;
    }
  }
  
  public void complete(V value)
  {
    _value = value;
    _isDone = true;
    
    Thread thread = _thread;
    
    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }
  
  public void complete(RuntimeException exn)
  {
    _exn = exn;
    _isDone = true;
    
    Thread thread = _thread;
    
    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }
  
  @Override
  public boolean cancel(boolean value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean isCancelled()
  {
    return false;
  }
}
