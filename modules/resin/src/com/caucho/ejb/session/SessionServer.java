/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.ejb.protocol.JVMObject;
import com.caucho.naming.Jndi;
import com.caucho.util.Log;
import com.caucho.util.LruCache;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server container for a session bean.
 */
public class SessionServer extends AbstractServer {
  protected final static Logger log = Log.open(SessionServer.class);

  private StatefulContext _homeContext;

  private LruCache<Object,AbstractSessionContext> _sessions
    = new LruCache<Object,AbstractSessionContext>(1024);

  private boolean _isClosed;

  public SessionServer(EjbServerManager manager)
  {
    super(manager);
  }

  /**
   * Initialize the server
   */
  @Override
  public void init()
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);
    
      super.init();

      // XXX: from TCK, s/b local or remote?
      String prefix = getServerManager().getLocalJndiName();
      if (prefix != null)
	Jndi.rebindDeep(prefix + "/sessionContext", getSessionContext());
      
      prefix = getServerManager().getRemoteJndiName();
      if (prefix != null)
	Jndi.rebindDeep(prefix + "/sessionContext", getSessionContext());
      
      _localHome = getSessionContext().createLocalHome();
      _remoteHomeView = getSessionContext().createRemoteHomeView();
      
      log.config("initialized session bean: " + this);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns the object key from a handle.
   */
  public Class getPrimaryKeyClass()
  {
    return null;
  }

  /**
   * Returns the EJBLocalHome stub for the container
   */
  public EJBLocalHome getEJBLocalHome()
  {
    return _localHome;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public EJBHome getEJBHome()
    throws RemoteException
  {
    return _remoteHomeView;
  }

  /**
   * Returns the home object for jndi.
   */
  public Object getHomeObject()
  {
    return _remoteHomeView;
  }

  /**
   * Returns the EJBRemote stub for the container
   */
  public Object getRemoteObject()
  {
    if (_remoteHomeView != null)
      return _remoteHomeView;
    else
      return _homeContext._caucho_newRemoteInstance();
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public Object getClientObject()
  {
    return new StatefulJndiFactory(this);
  }

  /**
   * Returns a new instance.
   */
  Object newInstance()
  {
    return _homeContext._caucho_newInstance();
  }

  /**
   * Creates the local stub for the object in the context.
   */
  SessionObject getEJBLocalObject(SessionBean bean)
  {
    try {
      SessionObject obj = null;

      /*
      obj = (SessionObject) bean.getLocal();
      obj._setObject(bean);
      */
      if (obj == null)
        throw new IllegalStateException("bean has no local interface");

      return obj;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates a handle for a new session.
   */
  JVMObject createEJBObject(Object primaryKey)
  {
    try {
      JVMObject obj = (JVMObject) _remoteStubClass.newInstance();
      obj._init(this, primaryKey);

      return obj;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates a handle for a new session.
   */
  AbstractHandle createHandle(AbstractContext context)
  {
    String key = ((AbstractSessionContext) context).getPrimaryKey();
    
    return getHandleEncoder().createHandle(key);
  }

  /**
   * Creates a handle for a new session.
   */
  public String createSessionKey(AbstractSessionContext context)
  {
    String key = getHandleEncoder().createRandomStringKey();
    
    _sessions.put(key, context);
    
    return key;
  }
  
  /**
   * Finds the remote bean by its key.
   *
   * @param key the remote key
   *
   * @return the remote interface of the entity.
   */
  public AbstractContext getContext(Object key, boolean forceLoad)
    throws FinderException
  {
    if (key == null)
      return null;
    
    AbstractSessionContext cxt = _sessions.get(key);
    if (cxt == null)
      throw new FinderException("no matching object:" + key);

    return cxt;
  }

  private AbstractSessionContext getSessionContext()
  {
    synchronized (this) {
      if (_homeContext == null) {
	try {
	  Class []param = new Class[] { SessionServer.class };
	  Constructor cons = _contextImplClass.getConstructor(param);

	  _homeContext = (StatefulContext) cons.newInstance(this);
	} catch (Exception e) {
	  throw new EJBExceptionWrapper(e);
	}
      }
    }

    return _homeContext;
  }

  public void addSession(AbstractSessionContext context)
  {
    createSessionKey(context);
  }

  /**
   * Remove an object by its handle.
   */
  public void remove(AbstractHandle handle)
  {
    _sessions.remove(handle.getObjectId());
    // _ejbManager.remove(handle);
  }
  
  /**
   * Cleans up the entity server nicely.
   */
  public void destroy()
  {
    synchronized (this) {
      if (_isClosed)
        return;

      _isClosed = true;
    }

    ArrayList<AbstractSessionContext> values;
    values = new ArrayList<AbstractSessionContext>();

    Iterator<AbstractSessionContext> iter = _sessions.values();
    while (iter.hasNext()) {
      values.add(iter.next());
    }

    _sessions = null;
    
    log.fine("closing session server " + this);

    for (AbstractSessionContext cxt : values) {
      try {
	cxt.destroy();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    super.destroy();
  }
}
