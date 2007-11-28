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
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.ejb.protocol.JVMObject;
import com.caucho.ejb.webbeans.StatefulComponent;
import com.caucho.naming.Jndi;
import com.caucho.soa.client.WebServiceClient;
import com.caucho.util.Log;
import com.caucho.util.LruCache;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import javax.ejb.*;
import javax.webbeans.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server container for a session bean.
 */
public class SessionServer extends AbstractServer
{
  protected final static Logger log = Log.open(SessionServer.class);

  private StatefulContext _homeContext;

  // XXX: need real lifecycle
  private LruCache<Object,AbstractSessionContext> _sessions
    = new LruCache<Object,AbstractSessionContext>(8192);

  private boolean _isClosed;

  private Object _remoteObject21;
  private Object _remoteObject;
  private boolean _isInitRemote;

  private PrePassivateConfig _prePassivateConfig;
  private PostActivateConfig _postActivateConfig;

  public SessionServer(EjbContainer manager)
  {
    super(manager);
  }

  protected String getType()
  {
    return "session:";
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
      
      WebBeansContainer webBeans = WebBeansContainer.create();

      SingletonComponent comp
        = new SingletonComponent(webBeans, getSessionContext());
      comp.setTargetType(SessionContext.class);
      comp.init();
      webBeans.addComponent(comp);

      _localHome = getSessionContext().createLocalHome();
      _remoteHomeView = getSessionContext().createRemoteHomeView();

      log.config(this + " starting");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    registerWebBeans();
  }

  private void registerWebBeans()
  {
    Class beanClass = getBeanSkelClass();
    ArrayList<Class> localApiList = getLocalApiList();

    if (beanClass != null && localApiList != null) {
      WebBeansContainer webBeans = WebBeansContainer.create();
      StatefulComponent comp = new StatefulComponent(this);

      _component = comp;
    
      comp.setTargetType(getEjbClass());
    
      Named named = (Named) beanClass.getAnnotation(Named.class);

      String name;

      if (named != null) {
	name = named.value();
      }
      else {
	name = getEJBName();

	comp.setName(name);
        comp.addNameBinding(name);
      }

      comp.init();

      webBeans.addComponentByName(name, comp);

      for (Class api : localApiList) {
	webBeans.addComponentByType(api, comp);
      }
    }
  }

  /**
   * Returns the object key from a handle.
   */
  @Override
  public Class getPrimaryKeyClass()
  {
    return null;
  }

  /**
   * Returns the EJBLocalHome stub for the container
   */
  @Override
  public EJBLocalHome getEJBLocalHome()
  {
    return _localHome;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  @Override
  public EJBHome getEJBHome()
  {
    return _remoteHomeView;
  }

  /**
   * Returns the home object for jndi.
   */
  @Override
  public Object getHomeObject()
  {
    return _remoteHomeView;
  }

  /**
   * Returns the EJBRemote stub for the container
   */
  @Override
  public Object getRemoteObject21()
  {
    if (_remoteHomeView != null)
      return _remoteHomeView;

    //if (_remoteHomeView == null)
    //  return null;

    return getRemoteObject();
  }

  /**
   * Returns the EJBRemote stub for the container
   */
  @Override
  public Object getRemoteObject()
  {
    return getRemoteObject(null);
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
        businessInterface = getRemoteObjectList().get(0);
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
   * Returns the EJBHome stub for the container
   */
  @Override
  public Object getClientObject(Class businessInterface)
  {
    return newInstance();
  }

  /**
   * Returns the 3.0 local stub for the container
   */
  @Override
  public Object getLocalObject()
  {
    return getClientObject(null);
  }

  /**
   * Returns the 3.0 local stub for the container
   */
  @Override
  public Object getLocalObject(Class businessInterface)
  {
    return getClientObject(businessInterface);
  }

  /**
   * Returns the 3.0 local stub for the container
   */
  @Override
  public Object getLocalObject(DependentScope scope)
  {
    return _homeContext._caucho_newInstance(scope);
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

    //System.out.println("SESSION-KEY: " + key);

    _sessions.put(key, context);

    return key;
  }

  public AbstractContext getContext()
  {
    return getSessionContext();
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

  public AbstractSessionContext getSessionContext()
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

  @Override
  public void setBusinessInterface(Object obj, Class businessInterface)
  {
    if (obj instanceof AbstractSessionObject) {
      AbstractSessionObject sessionObject = (AbstractSessionObject) obj;
      sessionObject.__caucho_setBusinessInterface(businessInterface);
    }
  }

  public void addSession(AbstractSessionContext context)
  {
    createSessionKey(context);
  }

  /**
   * Adds a web service client.
   */
  public WebServiceClient createWebServiceClient()
  {
    return new WebServiceClient();
  }

  public PostActivateConfig getPostActivate()
  {
    return _postActivateConfig;
  }

  public PrePassivateConfig getPrePassivate()
  {
    return _prePassivateConfig;
  }

  public void setPostActivate(PostActivateConfig postActivate)
  {
    _postActivateConfig = postActivate;
  }

  public void setPrePassivate(PrePassivateConfig prePassivate)
  {
    _prePassivateConfig = prePassivate;
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
