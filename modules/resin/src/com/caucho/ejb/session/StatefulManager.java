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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.ejb.NoSuchEJBException;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.ejb.inject.SessionBeanImpl;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Server container for a session bean.
 */
public class StatefulManager<X> extends AbstractSessionManager<X>
{
  private static final L10N L = new L10N(StatefulManager.class);
  private static final Logger log
    = Logger.getLogger(StatefulManager.class.getName());
  
  // XXX: need real lifecycle
  private LruCache<String,StatefulObject> _remoteSessions;

  public StatefulManager(EjbManager ejbContainer,
			AnnotatedType<X> annotatedType,
			Class<?> proxyImplClass)
  {
    super(ejbContainer, annotatedType, proxyImplClass);
  }

  @Override
  protected String getType()
  {
    return "stateful:";
  }
  
  @Override
  protected Class<?> getContextClass()
  {
    return StatefulContext.class;
  }

  @Override
  public <T> StatefulContext<X,T> getSessionContext(Class<T> api)
  {
    return (StatefulContext<X,T>) super.getSessionContext(api);
  }

  /**
   * Returns the JNDI proxy object to create instances of the
   * local interface.
   */
  @Override
  public <T> Object getLocalJndiProxy(Class<T> api)
  {
    StatefulContext<X,T> context = getSessionContext(api);

    return new StatefulProviderProxy<X,T>(context);
  }

  /**
   * Returns the object implementation
   */
  @Override
  public <T> T getLocalProxy(Class<T> api)
  {
    StatefulContext<X,T> context = getSessionContext(api);

    if (context != null) {
      CreationalContextImpl<T> env = 
        (CreationalContextImpl<T>) CreationalContextImpl.create();

      return context.createProxy(env);
    }
    else
      return null;
  }

  @Override
  protected <T> StatefulContext<X,T> createSessionContext(Class<T> api)
  {
    return new StatefulContext<X,T>(this, api);
  }

  @Override
  protected <T> Bean<T> createBean(ManagedBeanImpl<X> mBean, 
                                   Class<T> api,
                                   Set<Type> apiList)
  {
    StatefulContext<X,T> context = getSessionContext(api);

    if (context == null)
      throw new NullPointerException(L.l("'{0}' is an unknown api for {1}",
                                         api, getContext()));
    
    SessionBeanImpl<X,T> statefulBean
      = new SessionBeanImpl<X,T>(context, mBean, apiList);

    return statefulBean;
  }

  public void addSession(StatefulObject remoteObject)
  {
    createSessionKey(remoteObject);
  }

  /**
   * Finds the remote bean by its key.
   *
   * @param key the remote key
   *
   * @return the remote interface of the entity.
   */
  @Override
  public AbstractContext getContext(Object key, boolean forceLoad)
    throws FinderException
  {
    throw new NoSuchEJBException("no matching object:" + key);
    /*
    if (key == null)
      return null;

    StatefulContext cxt = _sessions.get(key);

    // ejb/0fe4
    if (cxt == null)
      throw new NoSuchEJBException("no matching object:" + key);
    // XXX ejb/0fe-: needs refactoring of 2.1/3.0 interfaces.
    // throw new FinderException("no matching object:" + key);

    return cxt;
    */
  }

  /**
   * Returns the remote object.
   */
  /*
  @Override
  public Object getRemoteObject(Object key)
  {
    StatefulObject remote = null;
    if (_remoteSessions != null) {
      remote = _remoteSessions.get(String.valueOf(key));
    }

    return remote;
  }
  */

  /**
   * Creates a handle for a new session.
   */
  public String createSessionKey(StatefulObject remote)
  {
    throw new UnsupportedOperationException(getClass().getName());
    /*
    String key = getHandleEncoder().createRandomStringKey();

    if (_remoteSessions == null)
      _remoteSessions = new LruCache<String,StatefulObject>(8192);
    
    _remoteSessions.put(key, remote);

    return key;
    */
  }

  /**
   * Returns the remote stub for the container
   */
  @Override
  public <T> T getRemoteObject(Class<T> api, String protocol)
  {
    StatefulContext<X,T> context = getSessionContext(api);

    if (context != null) {
      // XXX: bean?
      // T value = context.__caucho_createNew(null, null);
      
      // return value;
      
      throw new UnsupportedOperationException(getClass().getName());
    }
    else
      return null;
  }

  /**
   * Remove an object.
   */
  public void remove(String key)
  {
    if (_remoteSessions != null) {
      _remoteSessions.remove(key);

      /*
      // ejb/0fe2
      if (cxt == null)
	throw new NoSuchEJBException("no matching object:" + key);
      */
    }
  }
  
  /**
   * Cleans up the entity server nicely.
   */
  @Override
  public void destroy()
  {
    super.destroy();

    ArrayList<StatefulObject> values = new ArrayList<StatefulObject>();
    
    if (_remoteSessions != null) {
      Iterator<StatefulObject> iter = _remoteSessions.values();
      while (iter.hasNext()) {
        values.add(iter.next());
      }
    }

    _remoteSessions = null;

    /* XXX: may need to restore this
    for (StatefulObject obj : values) {
      try {
        obj.remove();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    */
    
    log.fine(this + " closed");
  }
}
