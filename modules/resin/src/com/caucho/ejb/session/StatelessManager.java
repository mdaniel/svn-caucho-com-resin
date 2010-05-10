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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Timer;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.timer.ScheduleIntrospector;
import com.caucho.config.timer.TimeoutCaller;
import com.caucho.config.timer.TimerTask;
import com.caucho.ejb.SessionPool;
import com.caucho.ejb.inject.StatelessBeanImpl;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.util.L10N;

/**
 * Server home container for a stateless session bean
 */
public class StatelessManager<X> extends AbstractSessionManager<X> {
  private static final L10N L = new L10N(StatelessManager.class);

  protected static Logger log
    = Logger.getLogger(StatelessManager.class.getName());
  
  private int _sessionIdleMax = 16;
  private int _sessionConcurrentMax = -1;
  private long _sessionConcurrentTimeout = -1;

  /**
   * Creates a new stateless server.
   *
   * @param urlPrefix
   *          the url prefix for any request to the server
   * @param allowJVMCall
   *          allows fast calls to the same JVM (with serialization)
   * @param config
   *          the session configuration from the ejb.xml
   */
  public StatelessManager(EjbManager ejbContainer, 
                          AnnotatedType<X> annotatedType,
                          Class<?> proxyImplClass)
  {
    super(ejbContainer, annotatedType, proxyImplClass);
    
    introspect();
  }

  @Override
  protected String getType()
  {
    return "stateless:";
  }
  
  public int getSessionIdleMax()
  {
    return _sessionIdleMax;
  }
  
  public int getSessionConcurrentMax()
  {
    return _sessionConcurrentMax;
  }
  
  public long getSessionConcurrentTimeout()
  {
    return _sessionConcurrentTimeout;
  }
  
  @Override
  protected <T> StatelessContext<X,T> getSessionContext(Class<T> api)
  {
    return (StatelessContext<X,T>) super.getSessionContext(api);
  }

  /**
   * Returns the JNDI proxy object to create instances of the local interface.
   */
  @Override
  public <T> Object getLocalJndiProxy(Class<T> api)
  {
    StatelessContext<X,T> context = getSessionContext(api);

    return new StatelessProviderProxy<X,T>(context.createProxy(null));
  }

  /**
   * Returns the object implementation
   */
  @Override
  public <T> T getLocalProxy(Class<T> api)
  {
    return getSessionContext(api).createProxy(null);
  }

  @Override
  protected <T> Bean<T> createBean(ManagedBeanImpl<X> mBean,
                                   Class<T> api,
                                   Set<Type> apiList)
  {
    StatelessContext<X,T> context = getSessionContext(api);

    if (context == null)
      throw new NullPointerException(L.l("'{0}' is an unknown api for {1}",
          api, this));

    StatelessBeanImpl<X,T> statelessBean
      = new StatelessBeanImpl<X,T>(this, mBean, api, apiList, context);

    return statelessBean;
  }

  /*
  @Override
  protected <T> InjectionTarget<T> createSessionComponent(Class<T> api, 
                                                          Class<X> beanClass)
  {
    StatelessProvider<?> provider = getContext().getProvider();

    return new StatelessComponent(provider, beanClass);
  }
  */
  
  @Override
  protected Class<?> getContextClass()
  {
    return StatelessContext.class;
  }
  
  /**
   * Called by the StatelessProxy on initialization.
   */
  public <T> StatelessPool<X,T> createStatelessPool(StatelessContext<X,T> context)
  {
    return new StatelessPool<X,T>(this, context);
  }

  /**
   * Returns the remote stub for the container
   */
  @Override
  public <T> T getRemoteObject(Class<T> api, String protocol)
  {
    if (api == null)
      return null;

    StatelessContext<X,T> context = getSessionContext(api);

    if (context != null) {
      T result = context.createProxy(null);

      return result;
    } else {
      log.fine(this + " unknown api " + api.getName());
      return null;
    }
  }

  /**
   * Returns the remote object.
   */
  /*
  @Override
  public Object getRemoteObject(Object key)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  @Override
  public void init() throws Exception
  {
    super.init();

    ArrayList<Class<?>> remoteApiList = getRemoteApiList();

    /*
    if (remoteApiList.size() > 0) {
      Class<?> api = remoteApiList.get(0);

      // XXX: concept of unique remote api not correct.
      _remoteProvider = getContext().getProvider();
    }
    */
  }

  private void introspect()
  {
    AnnotatedType<?> annType = getAnnotatedType();
    SessionPool sessionPool = annType.getAnnotation(SessionPool.class);

    if (sessionPool != null) {
      if (sessionPool.maxIdle() >= 0)
        _sessionIdleMax = sessionPool.maxIdle();
      
      if (sessionPool.maxConcurrent() >= 0)
        _sessionConcurrentMax = sessionPool.maxConcurrent();
      
      if (sessionPool.maxConcurrentTimeout() >= 0)
        _sessionConcurrentTimeout = sessionPool.maxConcurrentTimeout();
    }
  }

  @Override
  protected <T> StatelessContext<X,T> createSessionContext(Class<T> api)
  {
    return new StatelessContext<X,T>(this, api);
  }

  @Override
  protected void postStart()
  {
    ScheduleIntrospector introspector = new ScheduleIntrospector();

    /*
    InjectManager manager = InjectManager.create();
    AnnotatedType<X> type = manager.createAnnotatedType(getEjbClass());
    ArrayList<TimerTask> timers;

    timers = introspector.introspect(new StatelessTimeoutCaller(), type);

    if (timers != null) {
      for (TimerTask task : timers) {
        task.start();
      }
    }
    */
  }

  @Override
  public AbstractContext getContext(Object key, boolean forceLoad)
  {
    return getContext();
  }

  // XXX
  public Object[] getInterceptorBindings()
  {
    return null;
  }

  /*
  public void destroy()
  {
    super.destroy();
    
    try {
      getContext().destroy();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  */

  class StatelessTimeoutCaller implements TimeoutCaller {
    public void timeout(Method method, Timer timer)
      throws InvocationTargetException, IllegalAccessException
    {
      getContext().__caucho_timeout_callback(method, timer);
    }

    public void timeout(Method method) throws InvocationTargetException,
        IllegalAccessException
    {
      getContext().__caucho_timeout_callback(method);
    }
  }
}
