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

package com.caucho.ejb.entity;

import com.caucho.amber.AmberException;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.FinderExceptionWrapper;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.ejb.protocol.JVMObject;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.HomeHandle;
import javax.ejb.ObjectNotFoundException;
import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;

/**
 * EntityServer is a container for the instances of an entity bean.
 */
public class EntityServer extends AbstractServer {
  private static final L10N L = new L10N(EntityServer.class);

  private EntityCache _entityCache;

  private DataSource _dataSource;

  private HomeHandle _homeHandle;
  private QEntityContext _homeContext;

  private int _random;

  private boolean _isInit;
  private boolean _isCMP;

  private boolean _isLoadLazyOnTransaction = true;

  private Field []_primaryKeyFields;

  private Throwable _exception;

  private AmberEntityHome _amberEntityHome;

  private ArrayList<RemoveListener> _removeListeners =
    new ArrayList<RemoveListener>();
  private ArrayList<EntityServer> _updateListeners;

  private Constructor _contextConstructor;

  private boolean _isCacheable = true;
  // private long _cacheTimeout = 5000;
  private long _cacheTimeout = 1000;
  private long _invalidateTime;

  private int _jdbcIsolation = -1;

  /**
   * Creates a new EntityServer.
   *
   * @param serverId the entity server's unique identifier
   * @param allowJVMCall allows fast calls to the same JVM (with serialization)
   * @param config the EJB configuration.
   */
  public EntityServer(EjbContainer ejbContainer)
  {
    super(ejbContainer);

    _entityCache = _ejbContainer.getEntityCache();
    
    // getPersistentManager().setHome(config.getName(), this);
    // _dataSource = config.getDataSource();

    //if (_dataSource == null)
    //  _dataSource = ejbManager.getDataSource();
  }

  /**
   * Gets the primary key class
   */
  public Class getPrimaryKeyClass()
  {
    return _primaryKeyClass;
  }
  
  //
  // configuration
  // 

  /**
   * Sets CMP.
   */
  public void setCMP(boolean isCMP)
  {
    _isCMP = isCMP;
  }

  /**
   * Sets CMP.
   */
  public boolean isCMP()
  {
    return _isCMP;
  }

  /**
   * Gets the persistence scheme for the entity bean
   */
  public boolean getBeanManagedPersistence()
  {
    return ! _isCMP;
  }

  /**
   * Sets true if entities should be loaded lazily on transaction.
   */
  public boolean isLoadLazyOnTransaction()
  {
    return _isLoadLazyOnTransaction;
  }

  /**
   * Sets true if entities should be loaded lazily on transaction.
   */
  public void setLoadLazyOnTransaction(boolean isLoadLazy)
  {
    _isLoadLazyOnTransaction = isLoadLazy;
  }

  public void setJdbcIsolation(int isolation)
  {
    _jdbcIsolation = isolation;
  }

  /**
   * Returns true if the bean should be loaded.
   *
   * @param loadTime the time the bean was last loaded.
   */
  public boolean doLoad(long loadTime)
  {
    if (loadTime <= _invalidateTime)
      return true;

    long expiresTime = Alarm.getCurrentTime() - _cacheTimeout;

    return loadTime <= expiresTime;
  }

  /**
   * Sets the amber entity home.
   */
  public void setAmberEntityHome(AmberEntityHome home)
  {
    _amberEntityHome = home;
  }

  /**
   * Sets the amber entity home.
   */
  public AmberEntityHome getAmberEntityHome()
  {
    return _amberEntityHome;
  }

  /**
   * Loads an amber entity.
   */
  /*
    public EntityItem loadEntityItem(Object key)
    throws AmberException
    {
    return _amberEntityHome.loadEntityItem(key);
    }
  */

  /**
   * Invalidates all cache entries, forcing them to reload.
   */
  public void invalidateCache()
  {
    _invalidateTime = Alarm.getCurrentTime();
  }

  /**
   * Sets the primary key class.
   */
  public void setPrimaryKeyClass(Class cl)
  {
    _primaryKeyClass = cl;
  }

  /**
   * Initializes the EntityServer, generating and compiling the skeletons
   * if necessary.
   */
  public void init()
    throws Exception
  {
    super.init();

    try {
      // _isCacheable = _config.isCacheable();
      //_cacheTimeout = _config.getCacheTimeout();

      /*
        if (cacheSize < 1024)
        cacheSize = 1024;
      */

      // _jdbcIsolation = _ejbManager.getJDBCIsolation();
      Class []param = new Class[] { EntityServer.class };
      _contextConstructor = _contextImplClass.getConstructor(param);

      _homeContext = (QEntityContext) _contextConstructor.newInstance(this);

      _localHome = _homeContext.createLocalHome();
      _remoteHomeView = _homeContext.createRemoteHomeView();

      /*
        if (_homeStubClass != null) {
        _remoteHomeView = _homeContext.createRemoteHomeView();

        if (_config.getJndiName() != null) {
        Context ic = new InitialContext();
        ic.rebind(_config.getJndiName(), this);
        }
        }
      */

      initRelations();

      if (_isCMP) {
        // _amberEntityHome = getServerManager().getAmberEntityHome(name);
        //_amberEntityHome = getContainer().getAmberEntityHome(_config.getName());
        _amberEntityHome.setEntityFactory(new AmberEntityFactory(this));
      }

      Class primaryKeyClass = getPrimaryKeyClass();

      if (primaryKeyClass != null &&
          ! primaryKeyClass.isPrimitive() &&
          ! primaryKeyClass.getName().startsWith("java.lang.")) {
        _primaryKeyFields = primaryKeyClass.getFields();
      }

      log.config("initialized entity bean: " + this);
    } catch (Exception e) {
      _exception = e;

      throw e;
    }
  }

  /**
   * Initialize the callbacks required to manage relations.  Primarily
   * the callbacks are need to make sure collections are updated when
   * the target changes or is deleted.
   */
  private void initRelations()
  {
    if (! isCMP())
      return;

    /*
      PersistentBean bean = _config.getPersistentBean();
      ArrayList<PersistentBean> oldBeans = new ArrayList<PersistentBean>();

      Iterator iter = bean.getRelations();
      while (iter.hasNext()) {
      PersistentRelation rel = (PersistentRelation) iter.next();
      PersistentBean targetBean = rel.getTargetBean();

      addTargetBean(oldBeans, targetBean);
      }
    */
  }
  /*
    private void addTargetBean(ArrayList<PersistentBean> oldBeans,
    PersistentBean targetBean)
    {
    if (! oldBeans.contains(targetBean)) {
    oldBeans.add(targetBean);

    String ejbName = targetBean.getName();

    EntityServer targetServer = (EntityServer) _ejbManager.getServer(ejbName);

    if (targetServer == null) {
    PersistentBean bean = _config.getPersistentBean();
    throw new RuntimeException("unknown ejb '" + ejbName + "' in '" +
    bean.getName() + "'");
    }

    targetServer.addRemoveListener(_homeContext);
    }
    }
  */

  /**
   * Creates a handle for the primary key.
   */
  protected AbstractHandle createHandle(Object primaryKey)
  {
    return getHandleEncoder().createHandle(encodeId(primaryKey));
  }

  /**
   * Returns the encoded id.
   */
  public String encodeId(Object primaryKey)
  {
    if (_primaryKeyFields == null)
      return String.valueOf(primaryKey);

    try {
      CharBuffer cb = new CharBuffer();

      for (int i = 0; i < _primaryKeyFields.length; i++) {
        if (i != 0)
          cb.append(',');

        cb.append(_primaryKeyFields[i].get(primaryKey));
      }

      return cb.toString();
    } catch (IllegalAccessException e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Associate the skeleton with a key.
   */
  public void postCreate(Object key, QEntityContext cxt)
  {
    _entityCache.putEntityIfNew(this, key, cxt);
  }

  /**
   * Adds a remove listener.
   *
   * @param listener the home context that's listening for events.
   * @param listenClass the class for this server.
   */
  public void addRemoveListener(QEntityContext context)
  {
    //removeListeners.add(new RemoveListener(listener, listenClass));
    RemoveListener listener = null;
    //listener = new RemoveListener(context, _config.getLocalObjectClass());

    if (! _removeListeners.contains(listener))
      _removeListeners.add(listener);
  }

  /**
   * Adds an update listener.
   */
  public void addUpdateListener(EntityServer listener)
  {
    if (_updateListeners == null)
      _updateListeners = new ArrayList<EntityServer>();

    if (! _updateListeners.contains(listener))
      _updateListeners.add(listener);
  }

  /**
   * Remove an object.
   */
  public void remove(Object key)
  {
    for (int i = _removeListeners.size() - 1; i >= 0; i--) {
      RemoveListener listener = _removeListeners.get(i);

      try {
        listener._listener._caucho_remove_callback(listener._listenClass, key);
      } catch (Throwable e) {
        EJBExceptionWrapper.createRuntime(e);
      }
    }
  }

  public void removeCache(Object key)
  {
    _entityCache.removeEntity(this, key);
  }

  /**
   * Returns the underlying object by searching the primary key.
   */
  public AbstractContext findByPrimaryKey(Object key)
    throws FinderException
  {
    return getContext(key, _isCMP);
  }

  /**
   * Returns the underlying object by searching the primary key.
   */
  public AbstractContext findByPrimaryKey(int key)
    throws FinderException
  {
    return getContext(new Long(key), _isCMP);
  }

  /**
   * Returns the underlying object by searching the primary key.
   *
   * In this case, the object is known to exist.
   */
  public AbstractContext findKnownObjectByPrimaryKey(Object key)
  {
    // XXX:
    if (key == null)
      return null;

    try {
      return getContext(key, false);
    } catch (FinderException e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Returns the underlying object by searching the primary key.
   *
   * In this case, the object is known to exist.
   */
  public Object findKnownObjectByPrimaryKey(int key)
  {
    return findKnownObjectByPrimaryKey(new Integer(key));
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public EJBHome getClientHome()
    throws RemoteException
  {
    if (! _isInit) {
      /*
        try {
        _config.validateJDBC();
        } catch (ConfigException e) {
        throw new EJBExceptionWrapper(e);
        }
      */

      _isInit = true;
    }

    if (_remoteHome == null) {
      try {
        _remoteHome = _jvmClient.createHomeStub();
      } catch (Exception e) {
        EJBExceptionWrapper.createRuntime(e);
      }
    }

    return _remoteHome;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public EJBHome getEJBHome()
  {
    return _remoteHomeView;
    /*
      if (_remoteHome == null) {
      if (! _isInit) {
      try {
      _config.validateJDBC();
      } catch (ConfigException e) {
      throw new EJBExceptionWrapper(e);
      }

      _isInit = true;
      }

      try {
      _remoteHome = _jvmClient.createHomeStub();
      } catch (Exception e) {
      EJBExceptionWrapper.createRuntime(e);
      }
      }

      return _remoteHome;
    */
  }

  /**
   * Returns the EJBLocalHome stub for the container
   */
  public EJBLocalHome getClientLocalHome()
  {
    if (! _isInit) {
      /*
        try {
        _config.validateJDBC();
        } catch (ConfigException e) {
        throw new EJBExceptionWrapper(e);
        }
      */

      _isInit = true;
    }

    return _localHome;
  }

  public Object getHomeObject()
  {
    if (! _isInit) {
      /*
        try {
        _config.validateJDBC();
        } catch (ConfigException e) {
        throw new EJBExceptionWrapper(e);
        }
      */

      _isInit = true;
    }

    return _remoteHomeView;
  }

  /**
   * Creates the local stub for the object in the context.
   */
  /*
    JVMObject getEJBObject(AbstractHandle handle)
    {
    if (remoteStubClass == null)
    throw new IllegalStateException(L.l("'{0}' has no remote interface.  Local beans must be called from a local context. Remote beans need a home and a remote interface.",
    getEJBName()));

    try {
    JVMObject obj = (JVMObject) remoteStubClass.newInstance();
    obj._init(this, handle);

    return obj;
    } catch (Exception e) {
    throw new EJBExceptionWrapper(e);
    }
    }
  */

  /**
   * Creates a handle for a new session.
   */
  JVMObject createEJBObject(Object primaryKey)
  {
    if (_remoteStubClass == null)
      throw new IllegalStateException(L.l("'{0}' has no remote interface.  Local beans must be called from a local context. Remote beans need a home and a remote interface.",
                                          getEJBName()));

    try {
      JVMObject obj = (JVMObject) _remoteStubClass.newInstance();
      obj._init(this, primaryKey);

      return obj;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  public AbstractContext getContext(Object key)
    throws FinderException
  {
    return getContext(key, _isCMP);
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
    return getContext(key, forceLoad, true);
  }

  /**
   * Finds the remote bean by its key.
   *
   * @param key the remote key
   *
   * @return the remote interface of the entity.
   */
  public AbstractContext getContext(Object key,
                                    boolean forceLoad,
                                    boolean isFindEntityItem)
    throws FinderException
  {
    if (key == null)
      return null;

    QEntityContext cxt = _entityCache.getEntity(this, key);

    try {
      if (cxt == null) {
        cxt = (QEntityContext) _contextConstructor.newInstance(this);
        cxt.setPrimaryKey(key);

        cxt = _entityCache.putEntityIfNew(this, key, cxt);

        EntityItem amberItem = null;

	  /*
        // ejb/061c
        if (isFindEntityItem && _isCMP) {
          AmberConnection aConn;
          aConn = _amberEntityHome.getManager().getCacheConnection();

          try {
            // ejb/06d3
            amberItem = _amberEntityHome.findEntityItem(aConn,
                                                        key,
                                                        forceLoad);
          } catch (AmberException e) {
            String name = getEJBName();

            FinderException exn = new ObjectNotFoundException(L.l("'{0}' is an unknown entity.",
                                                                  name + "[" + key + "]"));
            exn.initCause(e);
            throw exn;
          } finally {
            aConn.freeConnection();
          }

          if (amberItem == null) {
            throw new ObjectNotFoundException(L.l("'{0}' is an unknown entity.",
                                                  getEJBName() + "[" + key + "]"));
          }
        }

        if (amberItem != null)
          cxt.__caucho_setAmber(amberItem);
	  */
      }

      // ejb/0d33 vs ejb/0d00 vs ejb/0500
      if (forceLoad &&
          (! _isLoadLazyOnTransaction ||
           getTransactionManager().getTransaction() == null)) {
        try {
          cxt._caucho_load();
        } catch (Exception e) {
          throw e;
        }
      }

      /*
        try {
        cxt._caucho_load();
        } catch (Exception e) {
        throw e;
        }
      */

      // XXX: ejb/061c, moved to the top.
      // cxt = _ejbManager.putEntityIfNew(this, key, cxt);

      return cxt;
    } catch (FinderException e) {
      throw e;
    } catch (Exception e) {
      throw FinderExceptionWrapper.create(e);
    }
  }

  public EntityItem getAmberCacheItem(Object key)
  {
    if (key == null)
      throw new NullPointerException();
    
    AmberConnection aConn;
    aConn = _amberEntityHome.getManager().getCacheConnection();

    try {
      // ejb/06d3
      EntityItem amberItem
	= aConn.loadCacheItem(_amberEntityHome.getJavaClass(),
			      key, _amberEntityHome);

      return amberItem;
    } catch (AmberException e) {
      throw new EJBException(e);
    } finally {
      aConn.freeConnection();
    }
  }

  /**
   * Returns a new connection.
   */
  public Connection getConnection(boolean isReadOnly)
    throws java.sql.SQLException
  {
    if (isReadOnly)
      return _dataSource.getConnection();
    else {
      Connection conn = _dataSource.getConnection();

      if (_jdbcIsolation > 0)
        conn.setTransactionIsolation(_jdbcIsolation);

      return conn;
    }
  }

  /**
   * Updates the named entity bean
   */
  public void update(Object key)
  {
    if (key == null)
      return;

    QEntityContext cxt = _entityCache.getEntity(this, key);

    if (cxt == null)
      return;

    // XXX: only update in the transaction?
    // why doesn't the transaction have the update?
    cxt.update();
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

  public Connection getConnection()
    throws SQLException
  {
    return getDataSource().getConnection();
  }

  /**
   * Updates the named entity bean
   */
  public void updateView(Object key)
  {
    if (_updateListeners == null)
      return;

    for (int i = _updateListeners.size() - 1; i >= 0; i--) {
      EntityServer server = _updateListeners.get(i);

      server.update(key);
    }
  }

  /**
   * Returns a random string for a new id
   *
   * @param max the previous maximum value
   * @param i the repetition index
   */
  public String randomString(int max, int i)
  {
    return String.valueOf(++_random);
  }

  /**
   * Returns a random integer for a new id
   *
   * @param max the previous maximum value
   * @param i the repetition index
   */
  public int randomInt(int max, int i)
  {
    return max;
  }

  public static IllegalStateException createGetStateException(int state)
  {
    switch (state) {
    case QEntity._CAUCHO_IS_REMOVED:
      return new IllegalStateException(L.l("Can't access CMP field for a removed object."));

    case QEntity._CAUCHO_IS_DEAD:
      return new IllegalStateException(L.l("Can't access CMP field for a dead object.  The object is dead due to a runtime exception and rollback."));

    case QEntity._CAUCHO_IS_NEW:
      return new IllegalStateException(L.l("Can't access CMP field for an uninitialized object."));

    case QEntity._CAUCHO_IS_HOME:
      return new IllegalStateException(L.l("Can't access CMP field from a Home object."));

    default:
      return new IllegalStateException(L.l("Can't access CMP field from an unknown state."));
    }
  }

  public static IllegalStateException createSetStateException(int state)
  {
    switch (state) {
    case QEntity._CAUCHO_IS_REMOVED:
      return new IllegalStateException(L.l("Can't set CMP field for a removed object."));

    case QEntity._CAUCHO_IS_DEAD:
      return new IllegalStateException(L.l("Can't set CMP field for a dead object.  The object is dead due to a runtime exception and rollback."));

    case QEntity._CAUCHO_IS_NEW:
      return new IllegalStateException(L.l("Can't set CMP field for an uninitialized object."));

    case QEntity._CAUCHO_IS_HOME:
      return new IllegalStateException(L.l("Can't set CMP field from a Home object."));

    default:
      return new IllegalStateException(L.l("Can't set CMP field from an unknown state."));
    }
  }

  /**
   * Cleans up the entity server nicely.
   */
  public void destroy()
  {
    ArrayList<QEntityContext> beans = new ArrayList<QEntityContext>();

    _entityCache.removeBeans(beans, this);

    // only purpose of the sort is to make the qa order consistent
    Collections.sort(beans, new EntityCmp());

    for (int i = 0; i < beans.size(); i++) {
      QEntityContext cxt = beans.get(i);

      try {
        cxt.destroy();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    QEntityContext homeContext = _homeContext;
    _homeContext = null;

    try {
      if (homeContext != null)
        homeContext.destroy();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    super.destroy();
  }

  static class RemoveListener {
    QEntityContext _listener;
    Class _listenClass;

    RemoveListener(QEntityContext listener, Class listenClass)
    {
      _listener = listener;
      _listenClass = listenClass;
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof RemoveListener))
        return false;

      RemoveListener listener = (RemoveListener) o;

      return (_listener.equals(listener._listener) &&
              _listenClass.equals(listener._listenClass));
    }

    public String toString()
    {
      return "RemoveListener[" + _listener  + "]";
    }
  }

  static class EntityCmp
    implements Comparator<QEntityContext> {
    public int compare(QEntityContext ca, QEntityContext cb)
    {
      try {
        String sa = String.valueOf(ca.getPrimaryKey());
        String sb = String.valueOf(cb.getPrimaryKey());

        return sa.compareTo(sb);
      } catch (Exception e) {
        return 0;
      }
    }
  }
}
