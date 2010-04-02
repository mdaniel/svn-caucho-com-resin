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

package com.caucho.ejb.server;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.ejb.cfg.AroundInvokeConfig;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.session.AbstractSessionContext;
import com.caucho.jca.pool.UserTransactionProxy;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;

/**
 * Base server for a single home/object bean pair.
 */
abstract public class AbstractServer<T> implements EnvironmentBean {
  private final static Logger log
    = Logger.getLogger(AbstractServer.class.getName());
  private static final L10N L = new L10N(AbstractServer.class);

  protected final EjbContainer _ejbContainer;

  protected final UserTransaction _ut = UserTransactionProxy.getInstance();

  protected String _filename;
  protected int _line;
  protected String _location;

  // The original bean implementation class
  protected Class<T> _ejbClass;

  // introspected bean information
  private AnnotatedType<T> _annotatedType;
  private Bean<T> _bean;

  protected String _id;
  protected String _ejbName;
  protected String _moduleName;
  protected String _handleServerId;

  // name for IIOP, Hessian, JNDI
  protected String _mappedName;

  protected ArrayList<Class<?>> _remoteApiList = new ArrayList<Class<?>>();
  protected ArrayList<Class<?>> _localApiList = new ArrayList<Class<?>>();

  protected Class<?> _serviceEndpointClass;

  private Context _jndiEnv;
  
  // server-specific classloader
  protected EnvironmentClassLoader _loader;

  private ConfigProgram _serverProgram;

  // injection/postconstruct from Java Injection
  private EjbProducer<T> _producer;

  private boolean _isContainerTransaction = true;
  protected long _transactionTimeout;

  // Generated classes
  protected Class<? extends T> _contextImplClass;

  // generated Java Injection bean
  protected Bean<T> _component;

  private final Lifecycle _lifecycle = new Lifecycle();;

  /**
   * Creates a new server container
   *
   * @param manager
   *          the owning server container
   */
  public AbstractServer(EjbContainer container, 
                        AnnotatedType<T> annotatedType)
  {
    _annotatedType = annotatedType;
    _ejbContainer = container;

    _loader = EnvironmentClassLoader.create(container.getClassLoader());
    _loader.setAttribute("caucho.inject", false);
    
    _producer = new EjbProducer<T>(this, annotatedType);
  }

  /**
   * Returns the id, module-path#ejb-name.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Sets the id, module-path#ejb-name.
   */
  public void setId(String id)
  {
    _id = id;

    int p = id.lastIndexOf('/');
    if (p > 0)
      _loader.setId(getType() + id.substring(p + 1));
    else
      _loader.setId(getType() + id);
  }

  public void setConfigLocation(String filename, int line)
  {
    _filename = filename;
    _line = line;
  }

  public void setLocation(String location)
  {
    _location = location;
  }

  protected String getType()
  {
    return "ejb:";
  }

  public Bean<T> getDeployBean()
  {
    return _bean;
  }
  
  public EjbProducer<T> getProducer()
  {
    return _producer;
  }

  public void setAroundInvoke(AroundInvokeConfig aroundInvoke)
  {
  }

  /**
   * Sets the ejb name.
   */
  public void setEJBName(String ejbName)
  {
    _ejbName = ejbName;
  }

  /**
   * Returns the ejb's name
   */
  public String getEJBName()
  {
    return _ejbName;
  }

  /**
   * Set's the module that defined this ejb.
   */
  public void setModuleName(String moduleName)
  {
    _moduleName = moduleName;
  }

  /**
   * Returns's the module that defined this ejb.
   */
  public String getModuleName()
  {
    return _moduleName;
  }

  /**
   * Sets the mapped name, default is to use the EJBName. This is the name for
   * both JNDI and the protocols such as IIOP and Hessian.
   */
  public void setMappedName(String mappedName)
  {
    if (mappedName == null) {
      _mappedName = null;
      return;
    }

    while (mappedName.startsWith("/"))
      mappedName = mappedName.substring(1);

    while (mappedName.endsWith("/"))
      mappedName = mappedName.substring(0, mappedName.length() - 1);

    _mappedName = mappedName;
  }

  /**
   * Returns the mapped name.
   */
  public String getMappedName()
  {
    return _mappedName == null ? getEJBName() : _mappedName;
  }

  /**
   * The name to use for remoting protocols, such as IIOP and Hessian.
   */
  public String getProtocolId()
  {
    return "/" + getMappedName();
  }

  /**
   * The name to use for remoting protocols, such as IIOP and Hessian.
   */
  public String getProtocolId(Class<?> cl)
  {
    if (cl == null)
      return getProtocolId();

    // XXX TCK:
    // ejb30/bb/session/stateless/callback/defaultinterceptor/descriptor/defaultInterceptorsForCallbackBean1
    if (cl.getName().startsWith("java."))
      return getProtocolId();

    // Adds the suffix "#com_sun_ts_tests_ejb30_common_sessioncontext_Three1IF";
    String url = getProtocolId() + "#" + cl.getName().replace(".", "_");

    return url;
  }

  public AnnotatedType<T> getAnnotatedType()
  {
    return _annotatedType;
  }

  /**
   * Sets the ejb class
   */
  public void setEjbClass(Class<T> cl)
  {
    _ejbClass = cl;
  }

  /**
   * Sets the ejb class
   */
  protected Class<T> getEjbClass()
  {
    return _ejbClass;
  }

  /**
   * Sets the context implementation class.
   */
  public void setContextImplClass(Class cl)
  {
    _contextImplClass = cl;
  }

  public void setBeanImplClass(Class<T> cl)
  {
  }

  /**
   * Sets the remote object list.
   */
  public void setRemoteApiList(ArrayList<Class<?>> list)
  {
    _remoteApiList = new ArrayList<Class<?>>(list);
  }

  /**
   * Returns the remote object list.
   */
  public ArrayList<Class<?>> getRemoteApiList()
  {
    return _remoteApiList;
  }

  /**
   * Returns true if there is any remote object.
   */
  public boolean hasRemoteObject()
  {
    return _remoteApiList.size() > 0;
  }

  /**
   * Sets the local api class list
   */
  public void setLocalApiList(ArrayList<Class<?>> list)
  {
    _localApiList = new ArrayList<Class<?>>(list);
  }

  /**
   * Sets the remote object class.
   */
  public ArrayList<Class<?>> getLocalApiList()
  {
    return _localApiList;
  }

  /**
   * Returns the encoded id.
   */
  public String encodeId(Object primaryKey)
  {
    return String.valueOf(primaryKey);
  }

  /**
   * Looks up the JNDI object.
   */
  public Object lookup(String jndiName)
  {
    try {
      if (_jndiEnv == null)
        _jndiEnv = (Context) new InitialContext().lookup("java:comp/env");

      // XXX: not tested
      return _jndiEnv.lookup(jndiName);
    } catch (NamingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public UserTransaction getUserTransaction()
  {
    return _ut;
  }

  /**
   * Returns the owning container.
   */
  public EjbContainer getEjbContainer()
  {
    return _ejbContainer;
  }

  /**
   * Sets the server program.
   */
  public void setServerProgram(ConfigProgram serverProgram)
  {
    _serverProgram = serverProgram;
  }

  /**
   * Sets the server program.
   */
  public ConfigProgram getServerProgram()
  {
    return _serverProgram;
  }

  /**
   * Sets the transaction timeout.
   */
  public void setTransactionTimeout(long timeout)
  {
    _transactionTimeout = timeout;
  }

  /**
   * Gets the transaction timeout.
   */
  public long getTransactionTimeout()
  {
    return _transactionTimeout;
  }

  /**
   * Returns the timer service.
   */
  public TimerService getTimerService()
  {
    // ejb/0fj0
    throw new UnsupportedOperationException(L.l("'{0}' does not support a timer service because it does not have a @Timeout method",
                                                this));
  }

  /**
   * Invalidates caches.
   */
  public void invalidateCache()
  {
  }

  /**
   * Gets the class loader
   */
  public DynamicClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Gets the generated skeleton class
   */
  public Class getBeanSkelClass()
  {
    return _contextImplClass;
  }

  /**
   * Returns the session context.
   */
  public AbstractSessionContext getSessionContext()
  {
    return null;
  }

  /**
   * Returns the remote skeleton for the given API
   *
   * @param api
   *          the bean's api to return a value for
   * @param protocol
   *          the remote protocol
   */
  abstract public Object getRemoteObject(Class<?> api, String protocol);

  /**
   * Returns the a new local stub for the given API
   *
   * @param api
   *          the bean's api to return a value for
   */
  abstract public Object getLocalObject(Class<?> api);

  /**
   * Returns the local jndi proxy for the given API
   *
   * @param api
   *          the bean's api to return a value for
   */
  abstract public Object getLocalProxy(Class<?> api);

  /**
   * Returns the remote object.
   */
  public Object getRemoteObject(Object key) throws FinderException
  {
    // XXX TCK: ejb30/.../remove
    return getContext(key).createRemoteView();
  }

  public AbstractContext getContext()
  {
    return null;
  }

  public AbstractContext getContext(Object key) throws FinderException
  {
    return getContext(key, true);
  }

  /**
   * Returns the context with the given key
   */
  abstract public AbstractContext getContext(Object key, boolean forceLoad)
      throws FinderException;

  public void timeout(Timer timer)
  {
    /*
    throw new UnsupportedOperationException(L.l("EJB '{0}' does not support a timeout, because it does not have a @Timeout method",
                                                this));
    */
    getContext().__caucho_timeout_callback(timer);
  }

  public void init() throws Exception
  {
    _loader.init();
    // _loader.setId("EnvironmentLoader[ejb:" + getId() + "]");
  }

  public boolean start() throws Exception
  {
    if (! _lifecycle.toActive())
      return false;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      _loader.start();

      bindContext();

      if (_serverProgram != null)
        _serverProgram.configure(this);

      bindInjection();

      postStart();

      log.config(this + " active");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return true;
  }
  
  protected void bindContext()
  {
  }

  protected void bindInjection()
  {
    _producer.setEnvLoader(_loader);
    _producer.bindInjection();   
  }

  protected void postStart()
  {
  }

  /**
   * Returns true if container transaction is used.
   */
  public boolean isContainerTransaction()
  {
    return _isContainerTransaction;
  }

  /**
   * Sets true if container transaction is used.
   */
  public void setContainerTransaction(boolean isContainerTransaction)
  {
    _isContainerTransaction = isContainerTransaction;
  }

  /**
   * Returns true if the server is dead.
   */
  public boolean isDead()
  {
    return ! _lifecycle.isActive();
  }

  /**
   * Cleans up the server on shutdown
   */
  public void destroy()
  {
    _lifecycle.toDestroy();
  }

  public ConfigException error(String msg)
  {
    if (_filename != null)
      throw new LineConfigException(_filename, _line, msg);
    else
      throw new ConfigException(msg);
  }

  public String toString()
  {
    if (getMappedName() != null)
      return (getClass().getSimpleName()
              + "[" + getEJBName() + "," + getMappedName() + "]");
    else
      return getClass().getSimpleName() + "[" + getEJBName() + "]";
  }
}
