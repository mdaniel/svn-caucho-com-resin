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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.ejb.NoSuchEJBException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.inject.SingletonBeanImpl;
import com.caucho.ejb.inject.StatefulBeanImpl;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Server container for a session bean.
 */
public class SingletonManager<T> extends SessionServer<T> {
  private static final L10N L = new L10N(SingletonManager.class);
  private static final Logger log =
    Logger.getLogger(SingletonManager.class.getName());

  private SingletonContext<T> _sessionContext;

  public SingletonManager(EjbContainer ejbContainer,
                          AnnotatedType<T> annotatedType)
  {
    super(ejbContainer, annotatedType);
  }

  @Override
  protected String getType()
  {
    return "stateful:";
  }

  @Override
  public SingletonContext<T> getSessionContext()
  {
    synchronized (this) {
      if (_sessionContext == null) {
        try {
          Class<?>[] param = new Class[] { SingletonManager.class };
          Constructor<?> cons = _contextImplClass.getConstructor(param);

          _sessionContext = (SingletonContext) cons.newInstance(this);
        } catch (Exception e) {
          throw new EJBExceptionWrapper(e);
        }
      }
    }

    return _sessionContext;
  }

  /**
   * Returns the JNDI proxy object to create instances of the local interface.
   */
  @Override
  public Object getLocalProxy(Class api)
  {
    SingletonProxyFactory factory = getSessionContext().getProxyFactory(api);
    /*
     * if (factory != null) return new SingletonFactoryJndiProxy(factory); else
     * return null;
     */

    throw new UnsupportedOperationException();
  }

  /**
   * Returns the object implementation
   */
  @Override
  public T getLocalObject(Class<?> api)
  {
    SingletonProxyFactory factory = getSessionContext().getProxyFactory(api);

    if (factory != null) {
      CreationalContextImpl env = new CreationalContextImpl();
      // XXX: should be bean
      return (T) factory.__caucho_createNew(null, env);
    } else
      return null;
  }

  protected Bean createBean(ManagedBeanImpl mBean, Class api)
  {
    SingletonProxyFactory factory = getSessionContext().getProxyFactory(api);

    if (factory == null)
      throw new NullPointerException(L.l("'{0}' is an unknown api for {1}",
                                         api, getSessionContext()));

    SingletonBeanImpl singletonBean
      = new SingletonBeanImpl(this, mBean, factory);

    return singletonBean;
  }

  protected InjectionTarget<T> createSessionComponent(Class api, Class beanClass)
  {
    SingletonProxyFactory factory = getSessionContext().getProxyFactory(api);

    return new SingletonComponent(factory);
  }

  /**
   * Finds the remote bean by its key.
   * 
   * @param key
   *          the remote key
   * 
   * @return the remote interface of the entity.
   */
  @Override
  public AbstractContext getContext(Object key, boolean forceLoad)
      throws FinderException
  {
    throw new NoSuchEJBException("no matching object:" + key);
    /*
     * if (key == null) return null;
     * 
     * StatefulContext cxt = _sessions.get(key);
     * 
     * // ejb/0fe4 if (cxt == null) throw new
     * NoSuchEJBException("no matching object:" + key); // XXX ejb/0fe-: needs
     * refactoring of 2.1/3.0 interfaces. // throw new
     * FinderException("no matching object:" + key);
     * 
     * return cxt;
     */
  }

  /**
   * Initialize an instance
   */
  public <X> void initInstance(T instance, InjectionTarget<T> target, 
                               X proxy, CreationalContext<X> cxt)
  {
    getProducer().initInstance(instance, target, proxy, cxt);
  }

  /**
   * Initialize an instance
   */
  public void destroyInstance(T instance)
  {
    getProducer().destroyInstance(instance);
  }

 
  /**
   * Cleans up the entity server nicely.
   */
  @Override
  public void destroy()
  {
    super.destroy();

    log.fine(this + " closed");
  }

  /* (non-Javadoc)
   * @see com.caucho.ejb.server.AbstractServer#getRemoteObject(java.lang.Class, java.lang.String)
   */
  @Override
  public Object getRemoteObject(Class<?> api, String protocol)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
