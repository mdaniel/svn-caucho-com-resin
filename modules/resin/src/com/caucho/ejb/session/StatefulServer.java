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

package com.caucho.ejb.session;

import com.caucho.config.ConfigContext;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.EJBExceptionWrapper;
import java.util.*;

import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.util.LruCache;
import com.caucho.webbeans.component.*;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import javax.ejb.FinderException;
import javax.ejb.NoSuchEJBException;

/**
 * Server container for a session bean.
 */
public class StatefulServer extends SessionServer
{
  private StatefulContext _homeContext;
  
  // XXX: need real lifecycle
  private LruCache<Object,AbstractSessionContext> _sessions
    = new LruCache<Object,AbstractSessionContext>(8192);

  private Object _remoteObject21;
  private Object _remoteObject;
  private boolean _isInitRemote;
 
  public StatefulServer(EjbContainer ejbContainer)
  {
    super(ejbContainer);
  }

  @Override
  protected String getType()
  {
    return "stateful:";
  }

  @Override
  public AbstractSessionContext getSessionContext()
  {
    return getStatefulContext();
  }
    
  private StatefulContext getStatefulContext()
  {
    synchronized (this) {
      if (_homeContext == null) {
        try {
          Class []param = new Class[] { StatefulServer.class };
          Constructor cons = _contextImplClass.getConstructor(param);

          _homeContext = (StatefulContext) cons.newInstance(this);
        } catch (Exception e) {
          throw new EJBExceptionWrapper(e);
        }
      }
    }

    return _homeContext;
  }

  /**
   * Returns the JNDI proxy object to create instances of the
   * local interface.
   */
  public Object getLocalProxy(Class api)
  {
    SessionProvider provider = getStatefulContext().getProvider(api);

    return new StatefulProviderProxy(provider);
  }

  protected ComponentImpl createSessionComponent(Class api)
  {
    SessionProvider provider = getStatefulContext().getProvider(api);

    return new SessionComponent(provider);
  }
  
  /**
   * Creates a handle for a new session.
   */
  AbstractHandle createHandle(AbstractContext context)
  {
    throw new UnsupportedOperationException(getClass().getName());
    /*
    String key = ((StatelessContext) context).getPrimaryKey();

    return getHandleEncoder().createHandle(key);
     */
  }

  public void addSession(AbstractSessionContext context)
  {
    createSessionKey(context);
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
    if (key == null)
      return null;

    AbstractSessionContext cxt = _sessions.get(key);

    // ejb/0fe4
    if (cxt == null)
      throw new NoSuchEJBException("no matching object:" + key);
    // XXX ejb/0fe-: needs refactoring of 2.1/3.0 interfaces.
    // throw new FinderException("no matching object:" + key);

    return cxt;
  }

  /**
   * Creates a handle for a new session.
   */
  public String createSessionKey(AbstractSessionContext context)
  {
    String key = getHandleEncoder().createRandomStringKey();

    //System.out.println("SESSION-KEY: " + key);

    _sessions.put(key, context);

    return key;
  }

  /**
   * Returns the 3.0 local stub for the container
   */
  @Override
  public Object getLocalObject(ConfigContext env)
  {
    return _homeContext._caucho_newInstance(env);
  }
  
  /**
   * Returns the EJBHome stub for the container
   */
  @Override
  public Object getClientObject(Class businessInterface)
  {
    return newInstance();
  }

  /**
   * Returns a new instance.
   */
  Object newInstance()
  {
    return _homeContext._caucho_newInstance();
  }

  /**
   * Returns a new 2.1 instance.
   */
  Object newInstance21()
  {
    return _homeContext._caucho_newInstance21();
  }

  /**
   * Returns the remote stub for the container
   */
  @Override
  public Object getRemoteObject(Class businessInterface)
  {
    if (! hasRemoteObject())
      return null;

    if (_isInitRemote)
      return null;

    _isInitRemote = true;

    // EJB 3.0 only.
    if (businessInterface == null) {
      if (getRemote21() == null) {
        // Assumes EJB 3.0
        businessInterface = getRemoteApiList().get(0);
      }
    }

    if (businessInterface != null)
      _remoteObject = _homeContext._caucho_newRemoteInstance();

    // EJB 2.1
    if (_remoteObject == null) {
      _remoteObject21 = _homeContext._caucho_newRemoteInstance21();

      _isInitRemote = false;

      return _remoteObject21;
    }

    _isInitRemote = false;

    // EJB 3.0 only.
    if (businessInterface == null)
      return _remoteObject;

    if (businessInterface.isAssignableFrom(_remoteObject.getClass())) {
      setBusinessInterface(_remoteObject, businessInterface);

      return _remoteObject;
    }

    return null;
  }
  
    /**
   * Remove an object by its handle.
   */
  @Override
  public Object remove(AbstractHandle handle)
  {
    return _sessions.remove(handle.getObjectId());
    // _ejbManager.remove(handle);
  }

  /**
   * Remove an object.
   */
  @Override
  public void remove(Object key)
  {
    AbstractSessionContext cxt = _sessions.remove(key);

    // ejb/0fe2
    if (cxt == null)
      throw new NoSuchEJBException("no matching object:" + key);
  }
  
  
  /**
   * Cleans up the entity server nicely.
   */
  @Override
  public void destroy()
  {
    super.destroy();
 
    ArrayList<AbstractSessionContext> values;
    values = new ArrayList<AbstractSessionContext>();

    Iterator<AbstractSessionContext> iter = _sessions.values();
    while (iter.hasNext()) {
      values.add(iter.next());
    }

    _sessions = null;

    log.fine(this + " closing");

    for (AbstractSessionContext cxt : values) {
      try {
        cxt.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
