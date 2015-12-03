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

package com.caucho.config.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;

import com.caucho.config.j2ee.EJBExceptionWrapper;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;


/**
 * Represents the @Asynchronous interception
 */
abstract public class AsyncItem<X> implements Runnable, Future<X> {
  private static final Logger log = Logger.getLogger(AsyncItem.class.getName());
  
  private static final ThreadLocal<AsyncItem<?>> _localItem
    = new ThreadLocal<AsyncItem<?>>();

  private boolean _isCancelled;
  private volatile boolean _isDone;
  private volatile Future<X> _result;
  
  private ExecutionException _executionException;
  
  public static boolean isThreadCancelled()
  {
    AsyncItem<?> item = _localItem.get();
    
    if (item != null)
      return item.isCancelled();
    else
      return false;
  }

  abstract public Future<X> runTask()
    throws Exception;
  
  @Override
  public final void run()
  {
    try {
      _localItem.set(this);
      
      _result = runTask();
      
      if (_result != null)
        _result.get();
    } catch (RuntimeException e) {
      log.log(Level.FINER, e.toString(), e);
      
      _executionException = new ExecutionException(new EJBException(e));
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      _executionException = new ExecutionException(e);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
      
      _executionException = new ExecutionException(new EJBExceptionWrapper(e));
    } finally {
      _isDone = true;
      _localItem.set(null);
      
      synchronized (this) {
        notifyAll();
      }
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    if (mayInterruptIfRunning)
      _isCancelled = true;
    
    return false;
  }

  @Override
  public X get() throws InterruptedException, ExecutionException
  {
    try {
      return get(Integer.MAX_VALUE, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public X get(long timeout, TimeUnit unit)
    throws InterruptedException,
           ExecutionException,
           TimeoutException
  {
    long timeoutMillis = unit.toMillis(timeout);
    
    long expires = CurrentTime.getCurrentTimeActual() + timeoutMillis;
    
    synchronized (this) {
      while (! _isDone) {
        long delta = expires - CurrentTime.getCurrentTimeActual();
        
        if (delta < 0)
          throw new TimeoutException(toString());

        Thread.interrupted();
        wait(delta);
      }
    }
    
    if (_executionException != null)
      throw _executionException;
    
    if (_result != null)
      return _result.get(0, TimeUnit.MILLISECONDS);
    else
      return null;
  }

  @Override
  public boolean isCancelled()
  {
    return _isCancelled;
  }

  @Override
  public boolean isDone()
  {
    return _isDone;
  }
  
  @Override
  public String toString()
  {
    if (getClass().getEnclosingMethod() != null) {
      return getClass().getEnclosingClass().getSimpleName() + "[" + getClass().getEnclosingMethod().getName() + "]";
    }
    else {
      return getClass().getSimpleName() + "[]";
    }
  }
}
