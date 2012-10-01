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

package com.caucho.ejb.session;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.SessionBean;
import javax.enterprise.inject.spi.Interceptor;

import com.caucho.config.gen.CandiUtil;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.OwnerCreationalContext;
import com.caucho.inject.Module;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;

/**
 * Pool of stateless session beans.
 */
@Module
public class StatelessPool<X,T> {
  private static final Logger log
    = Logger.getLogger(StatelessPool.class.getName());
  private static final L10N L = new L10N(StatelessPool.class);

  private final StatelessManager<X> _manager;
  private final StatelessContext<X,T> _context;
  private final List<Interceptor<?>> _interceptorBeans;
  
  private final FreeList<Item<X>> _freeList;
  
  private final ThreadLocal<Item<X>> _lifecycleInstanceLocal
    = new ThreadLocal<Item<X>>();
  private final StatelessProxyFactory _proxy;
  
  private final Semaphore _concurrentSemaphore;
  private final long _concurrentTimeout;

  StatelessPool(StatelessManager<X> manager,
                StatelessProxyFactory proxy,
                StatelessContext<X,T> context,
                List<Interceptor<?>> interceptorBeans)
  {
    _manager = manager;
    _proxy = proxy;
    _context = context;
    _interceptorBeans = interceptorBeans;
    
    int idleMax = manager.getSessionIdleMax();
    int concurrentMax = manager.getSessionConcurrentMax();
    
    if (idleMax < 0)
      idleMax = concurrentMax;
    
    if (idleMax < 0)
      idleMax = 16;
    
    _freeList = new FreeList<Item<X>>(idleMax);
    
    if (concurrentMax == 0)
      throw new IllegalArgumentException(L.l("maxConcurrent may not be zero")); 
    
    long concurrentTimeout = manager.getSessionConcurrentTimeout();
    
    if (concurrentTimeout < 0)
      concurrentTimeout = Long.MAX_VALUE / 2;
    
    _concurrentTimeout = concurrentTimeout;
    
    if (concurrentMax > 0)
      _concurrentSemaphore = new Semaphore(concurrentMax);
    else
      _concurrentSemaphore = null;
  }
  
  public Item<X> allocate()
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
      Item<X> beanItem = _freeList.allocate();
    
      if (beanItem == null) {
        CreationalContextImpl<X> env = new OwnerCreationalContext<X>(_manager.getBean());
        Object []bindings;

        X instance = _context.newInstance(env);
        
        bindings = _manager.getInterceptorBindings(_interceptorBeans, env);
               
        if (instance instanceof SessionBean) {
          try {
            ((SessionBean) instance).setSessionContext(_context);
          } catch (Exception e) {
            log.log(Level.WARNING, e.toString(), e);
          }
        }
        
        Object []delegates = _manager.createDelegates(env);

        beanItem = new Item<X>(instance, bindings, delegates);
        
        Item<X> oldInstance = _lifecycleInstanceLocal.get();
        try {
          _lifecycleInstanceLocal.set(beanItem);
          
          _proxy.__caucho_postConstruct();
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          _lifecycleInstanceLocal.set(oldInstance);
        }
        // _ejbProducer.newInstance();
      }
      
      isValid = true;
      
      _manager.setLocalStatelessPool(this);
    
      return beanItem;
    } finally {
      if (! isValid && semaphore != null)
        semaphore.release();
    }
  }
  
  public X getLifecycleInstance()
  {
    Item<X> item = _lifecycleInstanceLocal.get();
    
    if (item != null)
      return item.getValue();
    else
      return null;
  }
  
  public Item<X> getLifecycleItem()
  {
    return _lifecycleInstanceLocal.get();
  }

  public void free(Item<X> beanItem)
  {
    Semaphore semaphore = _concurrentSemaphore;
    if (semaphore != null)
      semaphore.release();
    
    _manager.setLocalStatelessPool(null);
    
    if (! _freeList.free(beanItem)) {
      destroyImpl(beanItem);
    }
  }
  
  public void destroy(Item<X> beanItem)
  {
    if (beanItem == null)
      return;
    
    Semaphore semaphore = _concurrentSemaphore;
    if (semaphore != null)
      semaphore.release();
    
    _manager.setLocalStatelessPool(null);
    
    destroyImpl(beanItem);
  }
  
  public void discard(Item<X> beanItem)
  {
    if (beanItem == null)
      return;
    
    Semaphore semaphore = _concurrentSemaphore;
    if (semaphore != null)
      semaphore.release();
  }
  
  public Class<?> getLocalApi()
  {
    return _context.getApi();
  }
  
  private void destroyImpl(Item<X> beanItem)
  {
    _manager.destroyInstance(beanItem.getValue());
  }
  
  public void destroy()
  {
    Item<X> beanItem;
    
    while ((beanItem = _freeList.allocate()) != null) {
      destroyImpl(beanItem);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _manager + "]";
  }
  
  public static class Item<X> {
    private X _value;
    private Object []_interceptorObjects;
    private Object []_delegates;
    
    Item(X value, 
         Object []interceptorObjects,
         Object []delegates)
    {
      _value = value;
      _interceptorObjects = interceptorObjects;
      _delegates = delegates;
    }
    
    public X getValue()
    {
      return _value;
    }
    
    public Object []_caucho_getInterceptorObjects()
    {
      return _interceptorObjects;
    }
    
    public Object []__caucho_getDelegates()
    {
      return _delegates;
    }
  }
}
