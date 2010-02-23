/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.session;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.caucho.config.gen.BeanProducer;
import com.caucho.ejb.server.EjbProducer;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;

/**
 * Pool of stateless session beans.
 */
public class StatelessPool<T> {
  private static final L10N L = new L10N(StatelessPool.class);

  private static Logger log
    = Logger.getLogger(StatelessPool.class.getName());
  
  private final SessionServer<T> _server;
  
  private final FreeList<T> _freeList;
  private final Semaphore _concurrentSemaphore;
  private final long _concurrentTimeout;

  private EjbProducer<T> _ejbProducer;
 
  StatelessPool(SessionServer<T> server,
                BeanProducer<T> producer)
  {
    _server = server;
    
    _ejbProducer = server.getProducer();
    _ejbProducer.setBeanProducer(producer);
    
    int idleMax = server.getSessionIdleMax();
    int concurrentMax = server.getSessionConcurrentMax();
    
    if (idleMax < 0)
      idleMax = concurrentMax;
    
    if (idleMax < 0)
      idleMax = 16;
    
    _freeList = new FreeList<T>(idleMax);
    
    if (concurrentMax == 0)
      throw new IllegalArgumentException(L.l("maxConcurrent may not be zero")); 
    
    long concurrentTimeout = server.getSessionConcurrentTimeout();
    
    if (concurrentTimeout < 0)
      concurrentTimeout = Long.MAX_VALUE / 2;
    
    _concurrentTimeout = concurrentTimeout;
    
    if (concurrentMax > 0)
      _concurrentSemaphore = new Semaphore(concurrentMax);
    else
      _concurrentSemaphore = null;
  }
  
  public T allocate()
  {
    Semaphore semaphore = _concurrentSemaphore;
    
    if (semaphore != null) {
      try {
        Thread.interrupted();
        if (! semaphore.tryAcquire(_concurrentTimeout, TimeUnit.MILLISECONDS))
          throw new RuntimeException(L.l("{0} concurrent max exceeded", this));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    boolean isValid = false;
    
    try {
      T bean = _freeList.allocate();
    
      if (bean == null) {
        bean = _ejbProducer.newInstance();
      }
      
      isValid = true;
    
      return bean;
    } finally {
      if (! isValid && semaphore != null)
        semaphore.release();
    }
  }

  public void free(T bean)
  {
    Semaphore semaphore = _concurrentSemaphore;
    if (semaphore != null)
      semaphore.release();
    
    if (! _freeList.free(bean)) {
      destroyImpl(bean);
    }
  }
  
  public void destroy(T bean)
  {
    if (bean == null)
      return;
    
    Semaphore semaphore = _concurrentSemaphore;
    if (semaphore != null)
      semaphore.release();
    
    destroyImpl(bean);
  }
  
  public void discard(T bean)
  {
    if (bean == null)
      return;
    
    Semaphore semaphore = _concurrentSemaphore;
    if (semaphore != null)
      semaphore.release();
  }
  
  private void destroyImpl(T bean)
  {
    _ejbProducer.destroyInstance(bean);
  }
  
  public void destroy()
  {
    T bean;
    
    while ((bean = _freeList.allocate()) != null) {
      destroyImpl(bean);
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _server + "]";
  }
}
