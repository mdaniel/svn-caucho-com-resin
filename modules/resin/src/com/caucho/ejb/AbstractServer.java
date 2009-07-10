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

package com.caucho.ejb;

import com.caucho.config.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.j2ee.InjectIntrospector;
import com.caucho.config.program.ConfigProgram;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.HandleEncoder;
import com.caucho.ejb.session.AbstractSessionContext;
import com.caucho.ejb.session.SessionServer;
import com.caucho.ejb.session.StatelessServer;
import com.caucho.ejb.timer.EjbTimerService;
import com.caucho.jca.UserTransactionProxy;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;

import javax.ejb.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

/**
 * Base server for a single home/object bean pair.
 */
abstract public class AbstractServer implements EnvironmentBean {
  private final static Logger log
    = Logger.getLogger(AbstractServer.class.getName());
  private static final L10N L = new L10N(AbstractServer.class);

  protected final EjbContainer _ejbContainer;
  
  protected final UserTransaction _ut
    = UserTransactionProxy.getInstance();

  protected String _filename;
  protected int _line;
  protected String _location;

  private AnnotatedType _annotatedType;
  private InjectionTarget _injectionTarget;
  
  protected String _id;
  protected String _ejbName;
  protected String _moduleName;
  protected String _handleServerId;

  // name for IIOP, Hessian, JNDI
  protected String _mappedName;

  private Context _jndiEnv;

  private ConfigProgram _serverProgram;

  protected HashMap<String,HandleEncoder> _protocolEncoderMap;
  protected HandleEncoder _handleEncoder;

  protected DataSource _dataSource;

  protected EnvironmentClassLoader _loader;

  // The original bean implementation class
  protected Class _ejbClass;
  
  // The class for the extended bean
  protected Class _contextImplClass;

  protected Class _local21;
  protected Class _remote21;

  protected Class _remoteHomeClass;
  protected Class _remoteObjectClass;
  protected ArrayList<Class> _remoteApiList = new ArrayList<Class>();
  protected Class _primaryKeyClass;
  protected Class _localHomeClass;
  protected ArrayList<Class> _localApiList = new ArrayList<Class>();

  protected Class _remoteStubClass;
  protected Class _homeStubClass;

  protected HomeHandle _homeHandle;
  protected EJBHome _remoteHome;
  protected EJBHome _remoteHomeView;
  protected EJBLocalHome _localHome;
  protected EJBMetaDataImpl _metaData;

  protected Class _serviceEndpointClass;

  protected long _transactionTimeout;

  protected Bean _component;

  private TimerService _timerService;

  protected ConfigProgram _initProgram;
  protected ConfigProgram []_initInject;
  protected ConfigProgram []_destroyInject;

  private AroundInvokeConfig _aroundInvokeConfig;

  private PreDestroyConfig _preDestroyConfig;
  private PostConstructConfig _postConstructConfig;

  private boolean _isContainerTransaction = true;

  private final Lifecycle _lifecycle = new Lifecycle();;
  private Class _beanImplClass;
  private Method _cauchoPostConstruct;

  /**
   * Creates a new server container
   *
   * @param manager the owning server container
   */
  public AbstractServer(EjbContainer container,
			AnnotatedType annotatedType)
  {
    _annotatedType = annotatedType;
    _ejbContainer = container;

    _loader = EnvironmentClassLoader.create(container.getClassLoader());

    InjectManager beanManager = InjectManager.create();
    ManagedBeanImpl managedBean = beanManager.createManagedBean(annotatedType);

    _injectionTarget = managedBean.getInjectionTarget();
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

  public void setAroundInvoke(AroundInvokeConfig aroundInvoke)
  {
    _aroundInvokeConfig = aroundInvoke;
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
  public String getProtocolId(Class cl)
  {
    if (cl == null)
      return getProtocolId();

    // XXX TCK: ejb30/bb/session/stateless/callback/defaultinterceptor/descriptor/defaultInterceptorsForCallbackBean1
    if (cl.getName().startsWith("java."))
      return getProtocolId();

    // Adds the suffix "#com_sun_ts_tests_ejb30_common_sessioncontext_Three1IF";
    String url = getProtocolId() + "#" + cl.getName().replace(".", "_");

    return url;
  }

  public AnnotatedType getAnnotatedType()
  {
    return _annotatedType;
  }

  public InjectionTarget getInjectionTarget()
  {
    return _injectionTarget;
  }
  
  /**
   * Sets the ejb class
   */
  public void setEjbClass(Class cl)
  {
    _ejbClass = cl;
  }

  /**
   * Sets the ejb class
   */
  protected Class getEjbClass()
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

  public void setBeanImplClass(Class cl)
  {
    _beanImplClass = cl;

    try {
      _cauchoPostConstruct = cl.getDeclaredMethod("__caucho_postConstruct");
      _cauchoPostConstruct.setAccessible(true);
    }
    catch (NoSuchMethodException e) {
    }
  }

  /**
   * Sets the remote home class.
   */
  public void setRemoteHomeClass(Class cl)
  {
    _remoteHomeClass = cl;
  }

  /**
   * Gets the remote home class.
   */
  public Class getRemoteHomeClass()
  {
    return _remoteHomeClass;
  }

  /**
   * Gets the 2.1 remote interface.
   */
  public Class getRemote21()
  {
    return _remote21;
  }

  /**
   * Sets the remote object list.
   */
  public void setRemoteApiList(ArrayList<Class> list)
  {
    _remoteApiList = new ArrayList<Class>(list);

    if (_remoteApiList.size() > 0)
      _remoteObjectClass = _remoteApiList.get(0);
  }

  /**
   * Returns the remote object list.
   */
  public ArrayList<Class> getRemoteApiList()
  {
    return _remoteApiList;
  }

  /**
   * Returns true if there is any remote object.
   */
  public boolean hasRemoteObject()
  {
    if (_remoteApiList == null)
      return false;

    if (_remoteApiList.size() == 0)
      return false;

    return true;
  }

  /**
   * Sets the remote object class.
   */
  public void setRemoteObjectClass(Class cl)
  {
    _remoteObjectClass = cl;

    if (_remoteApiList == null) {
      _remoteApiList = new ArrayList<Class>();
      _remoteApiList.add(cl);
    }
  }

  /**
   * Gets the remote object class.
   */
  public Class getRemoteObjectClass()
  {
    return _remoteObjectClass;
  }

  /**
   * Sets the local home class.
   */
  public void setLocalHomeClass(Class cl)
  {
    _localHomeClass = cl;
  }

  /**
   * Gets the local home class.
   */
  public Class getLocalHomeClass()
  {
    return _localHomeClass;
  }

  /**
   * Sets the service endpoint.
   */
  public void setServiceEndpoint(Class cl)
  {
    _serviceEndpointClass = cl;
  }

  /**
   * Gets the service endpoint
   */
  public Class getServiceEndpoint()
  {
    return _serviceEndpointClass;
  }

  /**
   * Sets the local api class list
   */
  public void setLocalApiList(ArrayList<Class> list)
  {
    _localApiList = new ArrayList<Class>(list);
  }

  /**
   * Sets the remote object class.
   */
  public ArrayList<Class> getLocalApiList()
  {
    return _localApiList;
  }

  public HandleEncoder getHandleEncoder(String protocol)
  {
    HandleEncoder encoder;

    if (_protocolEncoderMap != null) {
      encoder = _protocolEncoderMap.get(protocol);

      if (encoder != null)
        return encoder;
    }

    try {
      Class keyClass = getPrimaryKeyClass();

      encoder = _ejbContainer.getProtocolManager().createHandleEncoder(this, keyClass, protocol);
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }

    if (_protocolEncoderMap == null)
      _protocolEncoderMap = new HashMap<String,HandleEncoder>(8);

    _protocolEncoderMap.put(protocol, encoder);

    return encoder;
  }

  /**
   * Returns the encoded id.
   */
  public String encodeId(Object primaryKey)
  {
    return String.valueOf(primaryKey);
  }

  public HandleEncoder addHandleEncoder(String protocol, String serverId)
  {
    HandleEncoder encoder;

    if (_protocolEncoderMap != null) {
      encoder = _protocolEncoderMap.get(protocol);

      if (encoder != null)
        return encoder;
    }

    try {
      Class keyClass = getPrimaryKeyClass();

      encoder = new HandleEncoder(this, serverId + _ejbName);
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }

    if (_protocolEncoderMap == null)
      _protocolEncoderMap = new HashMap<String,HandleEncoder>(8);

    _protocolEncoderMap.put(protocol, encoder);

    return encoder;
  }

  public HandleEncoder getHandleEncoder()
  {
    return getHandleEncoder(EjbProtocolManager.getThreadProtocol());
  }

  public void setHandleEncoder(HandleEncoder encoder)
  {
    if (_homeHandle != null)
      _homeHandle = null;

    _handleEncoder = encoder;
  }

  public String getHandleServerId()
  {
    if (_handleServerId == null)
      _handleServerId = getHandleEncoder().getServerId();

    return _handleServerId;
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
    if (_timerService == null) {
      _timerService = EjbTimerService.getLocal(_ejbContainer.getClassLoader(),
                                               getContext());
    }

    return _timerService;
  }

  /**
   * Invalidates caches.
   */
  public void invalidateCache()
  {
  }

  /**
   * Remove an object.
   */
  public Object remove(AbstractHandle handle)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Remove an object.
   */
  public void remove(Object primaryKey)
  {
    //throw new UnsupportedOperationException();
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

  public Class getRemoteStubClass()
  {
    return _remoteStubClass;
  }

  public Class getHomeStubClass()
  {
    return _homeStubClass;
  }

  /**
   * Returns the meta data
   */
  public EJBMetaData getEJBMetaData()
  {
    if (_metaData == null) {
      try {
	EJBHome home = getEJBHome();
	
        _metaData = new EJBMetaDataImpl(home,
                                        getRemoteHomeClass(),
                                        getRemoteObjectClass(),
                                        getPrimaryKeyClass());
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
        throw new EJBException(e);
      }

      if (this instanceof StatelessServer) {
        _metaData.setSession(true);
        _metaData.setStatelessSession(true);
      }
      else if (this instanceof SessionServer) {
        _metaData.setSession(true);
      }
    }

    return _metaData;
  }

  /**
   * Returns the home handle for the container
   */
  public HomeHandle getHomeHandle()
  {
    if (_homeHandle == null)
      _homeHandle = getHandleEncoder().createHomeHandle();

    return _homeHandle;
  }

  void setEJBHome(EJBHome remoteHome)
  {
    _remoteHome = remoteHome;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public EJBHome getEJBHome()
  {
    if (_remoteHome != null)
      return _remoteHome;
    else {
      EJBHome home = (EJBHome) getRemoteObject(getRemoteHomeClass(), null);

      return home;
    }
  }

  /**
   * Returns the EJBHome stub for the container
   */
  EJBHome getClientHome()
  {
    return getEJBHome();
  }

  /**
   * Returns the session context.
   */
  public AbstractSessionContext getSessionContext()
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
   * Returns the remote skeleton for the given API
   *
   * @param api the bean's api to return a value for
   * @param protocol the remote protocol
   */
  abstract public Object getRemoteObject(Class api, String protocol);

  /**
   * Returns the a new local stub for the given API
   *
   * @param api the bean's api to return a value for
   */
  abstract public Object getLocalObject(Class api);
  
  /**
   * Returns the local jndi proxy for the given API
   *
   * @param api the bean's api to return a value for
   */
  abstract public Object getLocalProxy(Class api);

  /**
   * Returns the object key from a handle.
   */
  public Class getPrimaryKeyClass()
  {
    return _primaryKeyClass;
  }

  public EJBObject getEJBObject(Object key)
    throws FinderException
  {
    return getContext(key).getEJBObject();
  }

  /**
   * Returns the remote object.
   */
  public Object getRemoteObject(Object key)
    throws FinderException
  {
    // XXX TCK: ejb30/.../remove
    return getContext(key).createRemoteView();
  }

  public AbstractContext getContext()
  {
    return null;
  }

  public AbstractContext getContext(Object key)
    throws FinderException
  {
    return getContext(key, true);
  }

  public AbstractContext getContext(long key)
    throws FinderException
  {
    return getContext(new Long(key));
  }

  /**
   * Returns the context with the given key
   */
  abstract public AbstractContext getContext(Object key, boolean forceLoad)
    throws FinderException;

  /**
   * Sets the init program.
   */
  public void setInitProgram(ConfigProgram init)
  {
    _initProgram = init;
  }

  /**
   * Gets the init program.
   */
  public ConfigProgram getInitProgram()
  {
    return _initProgram;
  }

  /**
   * Initialize an instance
   */
  public void initInstance(Object instance)
  {
    initInstance(instance, _injectionTarget, null, new ConfigContext());
  }

  /**
   * Initialize an instance
   */
  public void initInstance(Object instance,
			   InjectionTarget target,
			   Object proxy,
			   CreationalContext cxt)
  {
    ConfigContext env = (ConfigContext) cxt;
    Bean comp = (Bean) target;

    if (env != null && comp != null) {
      env.put((AbstractBean) comp, proxy);
      env.push(proxy);
    }

    if (target != null)
      target.inject(instance, env);

    if (_initInject != null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(_loader);

	if (env == null)
	  env = new ConfigContext();

	for (ConfigProgram inject : _initInject)
	  inject.inject(instance, env);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }

    try {
      if (_cauchoPostConstruct != null)
	_cauchoPostConstruct.invoke(instance, null);
    }
    catch (Throwable e) {
      log.log(Level.FINER,
              L.l("Error invoking method {0}", _cauchoPostConstruct),
              e);
    }

    if (env != null && comp != null)
      env.remove(comp);
  }

  /**
   * Remove an object.
   */
  public void destroyInstance(Object instance)
  {
    if (_destroyInject != null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(_loader);

	ConfigContext env = null;
	if (env == null)
	  env = new ConfigContext();

	for (ConfigProgram inject : _destroyInject)
	  inject.inject(instance, env);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
  }

  public void init()
    throws Exception
  {
    _loader.init();
    // _loader.setId("EnvironmentLoader[ejb:" + getId() + "]");
  }

  public boolean start()
    throws Exception
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
    // Injection binding occurs in the start phase

    ArrayList<ConfigProgram> injectList = new ArrayList<ConfigProgram>();
    InjectIntrospector.introspectInject(injectList, getEjbClass());
    // XXX: add inject from xml here

    if (_initProgram != null)
      injectList.add(_initProgram);
    
    InjectIntrospector.introspectInit(injectList, getEjbClass(), null);
    // XXX: add init from xml here

    ConfigProgram []injectArray = new ConfigProgram[injectList.size()];
    injectList.toArray(injectArray);

    if (injectArray.length > 0)
      _initInject = injectArray;

    injectList = new ArrayList<ConfigProgram>();

    introspectDestroy(injectList, getEjbClass());

    injectArray = new ConfigProgram[injectList.size()];
    injectList.toArray(injectArray);

    if (injectArray.length > 0)
      _destroyInject = injectArray;
  }

  protected void introspectDestroy(ArrayList<ConfigProgram> injectList,
				   Class ejbClass)
  {
    InjectIntrospector.introspectDestroy(injectList, getEjbClass());
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
   * Returns true is there is a local home or local client object for the bean.
   */
  public boolean isLocal()
  {
    return (_localHome != null
            || _localApiList != null && _localApiList.size() > 0);
  }

  /**
   * Returns true is there is a remote home or remote client object
   * for the bean.
   */
  public boolean isRemote()
  {
    return _remoteHome != null || _remoteHomeView != null;
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

  public PostConstructConfig getPostConstruct()
  {
    return _postConstructConfig;
  }

  public PreDestroyConfig getPreDestroy()
  {
    return _preDestroyConfig;
  }

  public void setPostConstruct(PostConstructConfig postConstruct)
  {
    _postConstructConfig = postConstruct;
  }

  public void setPreDestroy(PreDestroyConfig preDestroy)
  {
    _preDestroyConfig = preDestroy;
  }

  /**
   * Client information for connecting to the server.
   */
  public void addClientRemoteConfig(StringBuilder sb)
  {
    if (_remoteApiList != null && _remoteApiList.size() > 0) {
      sb.append("<ejb-ref>\n");
      sb.append("<ejb-ref-name>" + getEJBName() + "</ejb-ref-name>\n");

      if (_remoteHomeClass != null)
        sb.append("<home>" + _remoteHomeClass.getName() + "</home>\n");
      
      sb.append("<remote>" + _remoteApiList.get(0).getName() + "</remote>\n");
      sb.append("</ejb-ref>\n");
    }
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
      return getClass().getSimpleName() + "[" + getEJBName() + "," + getMappedName() + "]";
    else
      return getClass().getSimpleName() + "[" + getEJBName() + "]";
  }
}
