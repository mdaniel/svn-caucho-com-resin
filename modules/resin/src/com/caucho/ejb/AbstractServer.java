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

package com.caucho.ejb;

import com.caucho.config.BuilderProgram;
import com.caucho.bytecode.JClass;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.HandleEncoder;
import com.caucho.ejb.protocol.SameJVMClientContainer;
import com.caucho.ejb.session.AbstractSessionContext;
import com.caucho.ejb.session.AbstractSessionObject;
import com.caucho.ejb.session.SessionServer;
import com.caucho.ejb.session.StatelessServer;
import com.caucho.ejb.timer.EjbTimerService;
import com.caucho.ejb.xa.EjbTransactionManager;
import com.caucho.ejb.xa.TransactionContext;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.ejb.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base server for a single home/object bean pair.
 */
abstract public class AbstractServer implements EnvironmentBean {
  protected final static Logger log = Log.open(AbstractServer.class);
  private static final L10N L = new L10N(AbstractServer.class);

  protected String _id;
  protected String _ejbName;
  protected String _moduleName;
  protected String _handleServerId;

  // name for IIOP, Hessian, JNDI
  protected String _mappedName;

  private Context _jndiEnv;

  protected EjbServerManager _ejbManager;
  private BuilderProgram _serverProgram;

  protected HashMap<String,HandleEncoder> _protocolEncoderMap;
  protected HandleEncoder _handleEncoder;

  protected DataSource _dataSource;

  protected DynamicClassLoader _loader;

  protected SameJVMClientContainer _jvmClient;

  // The class for the extended bean
  protected Class _contextImplClass;

  protected Class _local21;
  protected Class _remote21;

  protected Class _remoteHomeClass;
  protected Class _remoteObjectClass;
  protected ArrayList<Class> _remoteObjectList;
  protected Class _primaryKeyClass;
  protected Class _localHomeClass;
  protected ArrayList<Class> _localApiList;

  protected Class _remoteStubClass;
  protected Class _homeStubClass;

  protected HomeHandle _homeHandle;
  protected EJBHome _remoteHome;
  protected EJBHome _remoteHomeView;
  protected EJBLocalHome _localHome;
  protected EJBMetaDataImpl _metaData;

  protected Class _serviceEndpointClass;

  protected long _transactionTimeout;

  private TimerService _timerService;

  protected BuilderProgram _initProgram;

  private AroundInvokeConfig _aroundInvokeConfig;

  private PreDestroyConfig _preDestroyConfig;
  private PostConstructConfig _postConstructConfig;

  /**
   * Creates a new server container
   *
   * @param manager the owning server container
   */
  public AbstractServer(EjbServerManager manager)
  {
    _ejbManager = manager;

    _loader = new EnvironmentClassLoader(manager.getClassLoader());

    _timerService = EjbTimerService.getLocal(manager.getClassLoader());
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

  /**
   * Sets the context implementation class.
   */
  public void setContextImplClass(Class cl)
  {
    _contextImplClass = cl;
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
   * Sets the 2.1 remote interface.
   */
  public void setRemote21(Class remote21)
  {
    _remote21 = remote21;
  }

  /**
   * Gets the 2.1 local interface.
   */
  public Class getLocal21()
  {
    return _local21;
  }

  /**
   * Sets the 2.1 local interface.
   */
  public void setLocal21(Class local21)
  {
    _local21 = local21;
  }

  /**
   * Sets the remote object list.
   */
  public void setRemoteObjectList(ArrayList<JClass> list)
  {
    _remoteObjectList = new ArrayList<Class>();
    for (int i = 0; i < list.size(); i++)
      _remoteObjectList.add(list.get(i).getJavaClass());

    if (_remoteObjectList.size() > 0)
      _remoteObjectClass = _remoteObjectList.get(0);
  }

  /**
   * Returns the remote object list.
   */
  public ArrayList<Class> getRemoteObjectList()
  {
    return _remoteObjectList;
  }

  /**
   * Returns true if there is any remote object.
   */
  public boolean hasRemoteObject()
  {
    if (_remoteObjectList == null)
      return false;

    if (_remoteObjectList.size() == 0)
      return false;

    return true;
  }

  /**
   * Sets the remote object class.
   */
  public void setRemoteObjectClass(Class cl)
  {
    _remoteObjectClass = cl;

    if (_remoteObjectList == null) {
      _remoteObjectList = new ArrayList<Class>();
      _remoteObjectList.add(cl);
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
  public void setLocalApiList(ArrayList<JClass> list)
  {
    _localApiList = new ArrayList<Class>();
    for (int i = 0; i < list.size(); i++)
      _localApiList.add(list.get(i).getJavaClass());
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

      encoder = _ejbManager.getProtocolManager().createHandleEncoder(this, keyClass, protocol);
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
    return _ejbManager.getTransactionManager().getUserTransaction();
  }

  /**
   * Returns the owning container.
   */
  public EjbServerManager getContainer()
  {
    return _ejbManager;
  }

  /**
   * Returns the owning container.
   */
  public EjbServerManager getServerManager()
  {
    return _ejbManager;
  }

  /**
   * Sets the server program.
   */
  public void setServerProgram(BuilderProgram serverProgram)
  {
    _serverProgram = serverProgram;
  }

  /**
   * Sets the server program.
   */
  public BuilderProgram getServerProgram()
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
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the class loader
   */
  public DynamicClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Sets the class loader
   */
  public void setClassLoader(DynamicClassLoader loader)
  {
    _loader = loader;
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
        _metaData = new EJBMetaDataImpl(getEJBHome(),
                                        getRemoteHomeClass(),
                                        getRemoteObjectClass(),
                                        getPrimaryKeyClass());
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
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
    return _remoteHomeView;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  EJBHome getClientHome()
  {
    return getEJBHome();
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public Object getHomeObject()
  {
    return _remoteHomeView;
  }

  /**
   * Returns the session context.
   */
  public AbstractSessionContext getSessionContext()
  {
    return null;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public Object getRemoteObject21()
  {
    return getHomeObject();
  }

  /**
   * Returns the 3.0 remote stub for the container
   */
  public Object getRemoteObject()
  {
    throw new UnsupportedOperationException("3.0 remote interface not found");
  }

  /**
   * Returns the 3.0 remote stub for the container
   */
  public Object getRemoteObject(Class businessInterface)
  {
    throw new UnsupportedOperationException("3.0 remote interface not found");
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public Object getClientLocalHome()
  {
    return _localHome;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public Object getClientObject(Class businessInterface)
  {
    return getClientLocalHome();
  }

  /**
   * Returns the 3.0 local stub for the container
   */
  public Object getLocalObject()
  {
    throw new UnsupportedOperationException("3.0 local interface not found");
  }

  /**
   * Returns the 3.0 local stub for the container
   */
  public Object getLocalObject(Class businessInterface)
  {
    throw new UnsupportedOperationException("3.0 local interface not found");
  }

  /**
   * Returns the EJBLocalHome stub for the container
   */
  public EJBLocalHome getEJBLocalHome()
  {
    return _localHome;
  }

  /**
   * Returns the object key from a handle.
   */
  public Class getPrimaryKeyClass()
  {
    return _primaryKeyClass;
  }

  /**
   * Creates the local stub for the object in the context.
   */
  EJBLocalObject getEJBLocalObject(AbstractContext context)
  {
    throw new UnsupportedOperationException();
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
   * Returns the UserTransaction for the request.
   */
  /*
    public UserTransaction getUserTransaction()
    {
    return _ejbManager.getUserTransaction();
    }
  */

  /**
   * Returns the currrent transaction context.
   *
   * @return the transaction context for the request
   */
  public EjbTransactionManager getTransactionManager()
  {
    return _ejbManager.getTransactionManager();
  }

  /**
   * Returns the currrent transaction context.
   *
   * @return the transaction context for the request
   */
  public TransactionContext getTransaction()
  {
    return _ejbManager.getTransactionManager().getTransactionContext();
  }

  /**
   * Returns the server's DataSource
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Sets the server's DataSource
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Sets the init program.
   */
  public void setInitProgram(BuilderProgram init)
  {
    _initProgram = init;
  }

  /**
   * Gets the init program.
   */
  public BuilderProgram getInitProgram()
  {
    return _initProgram;
  }

  /**
   * Initialize an instance
   */
  public void initInstance(Object instance)
    throws Throwable
  {
    if (_initProgram != null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(_loader);

        _initProgram.configure(instance);

      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
  }

  public void init()
    throws Exception
  {
    // _loader.setId("EnvironmentLoader[ejb:" + getId() + "]");
  }

  public void start()
    throws Exception
  {
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
   * Returns true is there is a remote home or remote client object for the bean.
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
    return _ejbManager == null;
  }

  /**
   * Cleans up the server on shutdown
   */
  protected void destroy()
  {
    _ejbManager = null;
  }

  public Connection getConnection()
    throws SQLException
  {
    return getDataSource().getConnection();
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

  public void setBusinessInterface(Object obj, Class businessInterface)
  {
    if (obj instanceof AbstractSessionObject) {
      AbstractSessionObject sessionObject = (AbstractSessionObject) obj;
      sessionObject.__caucho_setBusinessInterface(businessInterface);
    }
  }

  public String toString()
  {
    if (getMappedName() != null)
      return getClass().getSimpleName() + "[" + getEJBName() + "," + getMappedName() + "]";
    else
      return getClass().getSimpleName() + "[" + getEJBName() + "]";
  }
}
