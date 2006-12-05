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

import com.caucho.config.*;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.HandleEncoder;
import com.caucho.ejb.protocol.SameJVMClientContainer;
import com.caucho.ejb.session.SessionServer;
import com.caucho.ejb.session.StatelessServer;
import com.caucho.ejb.xa.EjbTransactionManager;
import com.caucho.ejb.xa.TransactionContext;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.ejb.*;
import javax.naming.*;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import java.lang.reflect.*;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base server for a single home/object bean pair.
 */
abstract public class AbstractServer implements EnvironmentBean {
  protected final static Logger log = Log.open(AbstractServer.class);
  private static final L10N L = new L10N(AbstractServer.class);

  protected String _ejbName;
  protected String _jndiName;
  protected String _serverId;
  protected String _handleServerId;

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

  protected Class _remoteHomeClass;
  protected Class _remoteObjectClass;
  protected Class _primaryKeyClass;

  protected Class _remoteStubClass;
  protected Class _homeStubClass;

  protected HomeHandle _homeHandle;
  protected EJBHome _remoteHome;
  protected EJBHome _remoteHomeView;
  protected EJBLocalHome _localHome;
  protected EJBMetaDataImpl _metaData;

  protected long _transactionTimeout;

  protected BuilderProgram _initProgram;

  /**
   * Creates a new server container
   *
   * @param manager the owning server container
   */
  public AbstractServer(EjbServerManager manager)
  {
    _ejbManager = manager;

    _loader = new EnvironmentClassLoader(manager.getClassLoader());
  }

  public String getId()
  {
    // XXX: s/b  module-path#ejb-name
    return getEJBName();
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
   * Sets the jndi name.
   */
  public void setJndiName(String jndiName)
  {
    if (jndiName == null)
      return;

    while (jndiName.startsWith("/"))
      jndiName = jndiName.substring(1);

    while (jndiName.endsWith("/"))
      jndiName = jndiName.substring(0, jndiName.length() - 1);


    _jndiName = jndiName;
  }

  /**
   * Returns the jndi name.  The jndi name does not include the java:comp/env
   * prefix, and does not start or end with a '/'.
   */
  public String getJndiName()
  {
    return _jndiName;
  }

  /**
   * Returns the protocol id, used by implementations of
   * {@link com.caucho.ejb.protocol.ProtocolContainer} as the identity of the
   * home.  Always starts with a "/" but does not end with a "/".
   */
  public String getProtocolId()
  {
    if (_serverId == null) {
      String serverId = getJndiName();

      if (serverId == null)
	serverId = getEJBName();

      if (serverId.startsWith("java:comp/env"))
        serverId = serverId.substring(13);

      if (! serverId.startsWith("/"))
        serverId = "/" + serverId;

      while (serverId.endsWith("/"))
        serverId = serverId.substring(0, serverId.length() - 1);

      _serverId = serverId;
    }

    return _serverId;
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
   * Sets the remote object class.
   */
  public void setRemoteObjectClass(Class cl)
  {
    _remoteObjectClass = cl;
  }

  /**
   * Gets the remote object class.
   */
  public Class getRemoteObjectClass()
  {
    return _remoteObjectClass;
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
   * Invalidates caches.
   */
  public void invalidateCache()
  {
  }

  /**
   * Remove an object.
   */
  public void remove(AbstractHandle handle)
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
    throws RemoteException
  {
    return _remoteHomeView;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  EJBHome getClientHome()
    throws RemoteException
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
   * Returns the EJBHome stub for the container
   */
  public Object getRemoteObject()
  {
    return getHomeObject();
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
  public Object getClientObject()
  {
    return getClientLocalHome();
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
    _loader.setId("EnvironmentLoader[ejb:" + getId() + "]");
  }

  public void start()
    throws Exception
  {
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

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getEJBName() + "," + getJndiName() + "]";
  }
}
