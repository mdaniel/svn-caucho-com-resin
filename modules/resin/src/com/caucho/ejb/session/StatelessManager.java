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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Timer;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.timer.ScheduleIntrospector;
import com.caucho.config.timer.TimeoutCaller;
import com.caucho.config.timer.TimerTask;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.inject.StatelessBeanImpl;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.util.L10N;

/**
 * Server home container for a stateless session bean
 */
public class StatelessManager<T> extends SessionServer<T> {
  private static final L10N L = new L10N(StatelessManager.class);

  protected static Logger log
    = Logger.getLogger(StatelessManager.class.getName());

  private StatelessContext<T> _homeContext;
  private StatelessProvider<T> _remoteProvider;

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
  public StatelessManager(EjbContainer ejbContainer, 
                         AnnotatedType<T> annotatedType)
  {
    super(ejbContainer, annotatedType);
  }

  @Override
  protected String getType()
  {
    return "stateless:";
  }

  /**
   * Returns the JNDI proxy object to create instances of the local interface.
   */
  @Override
  public Object getLocalProxy(Class api)
  {
    StatelessProvider provider = getStatelessContext().getProvider(api);

    return new StatelessProviderProxy(provider);
  }

  /**
   * Returns the object implementation
   */
  @Override
  public Object getLocalObject(Class<?> api)
  {
    return getStatelessContext().getProvider(api);
  }

  @Override
  protected Bean<T> createBean(ManagedBeanImpl<T> mBean, Class<?> api)
  {
    StatelessProvider<T> provider = getStatelessContext().getProvider(api);

    if (provider == null)
      throw new NullPointerException(L.l("'{0}' is an unknown api for {1}",
          api, getStatelessContext()));

    StatelessBeanImpl<T> statelessBean
      = new StatelessBeanImpl<T>(this, mBean, api, provider);

    return statelessBean;
  }

  protected InjectionTarget createSessionComponent(Class api, Class beanClass)
  {
    StatelessProvider provider = getStatelessContext().getProvider(api);

    return new StatelessComponent(provider, beanClass);
  }

  /**
   * Returns the 3.0 remote stub for the container
   */
  public Object getRemoteObject()
  {
    if (_remoteProvider != null)
      return _remoteProvider.__caucho_get();
    else
      return null;
  }

  /**
   * Returns the remote stub for the container
   */
  @Override
  public Object getRemoteObject(Class api, String protocol)
  {
    if (api == null)
      return null;

    StatelessProvider provider = getStatelessContext().getProvider(api);

    if (provider != null) {
      Object result = provider.__caucho_get();

      return result;
    } else {
      log.fine(this + " unknown api " + api.getName());
      return null;
    }
  }

  /**
   * Returns the remote object.
   */
  @Override
  public Object getRemoteObject(Object key)
  {
    if (_remoteProvider != null)
      return _remoteProvider.__caucho_get();
    else
      return null;
  }

  @Override
  public void init() throws Exception
  {
    super.init();

    ArrayList<Class<?>> remoteApiList = getRemoteApiList();

    if (remoteApiList.size() > 0) {
      Class<?> api = remoteApiList.get(0);

      // XXX: concept of unique remote api not correct.
      _remoteProvider = getStatelessContext().getProvider(api);
    }
  }

  @Override
  protected void postStart()
  {
    ScheduleIntrospector introspector = new ScheduleIntrospector();

    InjectManager manager = InjectManager.create();
    AnnotatedType<T> type = manager.createAnnotatedType(getEjbClass());
    ArrayList<TimerTask> timers;

    timers = introspector.introspect(new StatelessTimeoutCaller(), type);

    if (timers != null) {
      for (TimerTask task : timers) {
        task.start();
      }
    }
  }

  public AbstractContext getContext()
  {
    return getStatelessContext();
  }

  @Override
  public AbstractContext getContext(Object key, boolean forceLoad)
  {
    return getStatelessContext();
  }

  @Override
  public AbstractSessionContext getSessionContext()
  {
    return getStatelessContext();
  }
  
  public void destroy()
  {
    super.destroy();
    
    try {
      getStatelessContext().destroy();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  private StatelessContext getStatelessContext()
  {
    synchronized (this) {
      if (_homeContext == null) {
        try {
          Class<?>[] param = new Class[] { StatelessManager.class };
          Constructor<?> cons = _contextImplClass.getConstructor(param);

          _homeContext = (StatelessContext) cons.newInstance(this);
        } catch (Exception e) {
          throw new EJBExceptionWrapper(e);
        }
      }
    }
    
    return _homeContext;
  }

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
