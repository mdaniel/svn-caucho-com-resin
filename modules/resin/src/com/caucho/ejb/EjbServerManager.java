/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.io.*;

import java.util.*;

import java.util.logging.*;

import java.rmi.*;

import java.sql.*;

import javax.ejb.*;
import javax.sql.*;
import javax.naming.*;
import javax.transaction.*;

import javax.jms.ConnectionFactory;

import com.caucho.bytecode.JClassLoader;

import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.util.LruCache;

import com.caucho.vfs.Path;
import com.caucho.vfs.JarPath;

import com.caucho.java.WorkDir;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.SimpleLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.EnvironmentListener;

import com.caucho.loader.enhancer.EnhancingClassLoader;

import com.caucho.config.ConfigException;

import com.caucho.config.types.FileSetType;

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.amber.AmberManager;

import com.caucho.amber.entity.AmberEntityHome;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.ejb.cfg.EjbConfig;

import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.ProtocolContainer;

import com.caucho.ejb.xa.EjbTransactionManager;

import com.caucho.ejb.admin.EJBAdmin;

import com.caucho.ejb.entity.EntityServer;
import com.caucho.ejb.entity.QEntityContext;
import com.caucho.ejb.entity.EntityKey;

import com.caucho.ejb.enhancer.EjbEnhancer;

import com.caucho.ejb.entity2.EntityManagerProxy;

/**
 * Manages the EJBs.
 */
public class EjbServerManager implements EJBServerInterface, EnvironmentListener {
  private static final L10N L = new L10N(EjbServerManager.class);
  protected static final Logger log = Log.open(EjbServerManager.class);
  
  private static EnvironmentLocal<EjbServerManager> _localServerManager =
    new EnvironmentLocal<EjbServerManager>("caucho.ejb-server");

  private EnvironmentClassLoader _classLoader;

  private Path _workPath;
  private boolean _autoCompile = true;

  private int _entityCacheSize = 32 * 1024;
  private long _entityCacheTimeout = 5000L;
  
  private boolean _entityLoadLazyOnTransaction = true;

  protected boolean _allowJVMCall = true;
  protected boolean _allowReferenceCall = true;

  protected boolean _createDatabaseSchema;
  protected boolean _validateDatabaseSchema;

  private boolean _hasInitJdbc;
  private ConfigException _initException;

  private EjbConfig _ejbConfig;
  
  private EJBAdmin _ejbAdmin;

  private AmberManager _amberManager;
  
  private EjbTransactionManager _ejbTransactionManager;
  
  private EjbProtocolManager _protocolManager;

  protected ConnectionFactory _jmsConnectionFactory;
  private int _messageConsumerMax = 5;

  private Hashtable<String,AbstractServer> _serverMap
    = new Hashtable<String,AbstractServer>();

  // handles remote stuff
  protected ProtocolContainer _protocolContainer;
  protected HashMap<String,ProtocolContainer> _protocolMap =
    new HashMap<String,ProtocolContainer>();

  private LruCache<EntityKey,QEntityContext> _entityCache;
  
  private EntityKey _entityKey = new EntityKey();

  private final Lifecycle _lifecycle = new Lifecycle(log, "ejb-manager");

  /**
   * Create a server with the given prefix name.
   */
  private EjbServerManager()
  {
    try {
      _amberManager = new AmberManager();
      _amberManager.initLoaders();
      _amberManager.setTableCacheTimeout(_entityCacheTimeout);

      _classLoader = (EnvironmentClassLoader) Thread.currentThread().getContextClassLoader();
      _workPath = WorkDir.getLocalWorkDir(_classLoader).lookup("ejb");
      _classLoader.addLoader(new SimpleLoader(_workPath));

      _ejbTransactionManager = new EjbTransactionManager(this);

      _ejbConfig = new EjbConfig(this);
    
      _ejbAdmin = new EJBAdmin(this);

      _protocolManager = new EjbProtocolManager(this);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      Class entityManagerClass = Class.forName("com.caucho.ejb.entity2.EntityManagerProxy");
      
      Object entityProxy = entityManagerClass.newInstance();

      new InitialContext().rebind("java:comp/EntityManager", entityProxy);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Gets the local server.
   */
  public static EjbServerManager getLocal()
  {
    return _localServerManager.get();
  }

  /**
   * Creates the local server.
   */
  public static EjbServerManager createLocal()
  {
    synchronized (EjbServerManager.class) {
      EjbServerManager serverManager = _localServerManager.getLevel();

      if (serverManager == null) {
	serverManager = new EjbServerManager();
	_localServerManager.set(serverManager);
      }

      return serverManager;
    }
  }

  /**
   * Returns the loader.
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Returns the protocol manager.
   */
  public EjbProtocolManager getProtocolManager()
  {
    return _protocolManager;
  }

  /**
   * Returns the transaction manager.
   */
  public EjbTransactionManager getTransactionManager()
  {
    return _ejbTransactionManager;
  }

  /**
   * Sets the data source for the container.
   */
  public void setDataSource(DataSource dataSource)
  {
    _amberManager.setDataSource(dataSource);
  }

  /**
   * Sets the data source for the container.
   */
  public DataSource getDataSource()
  {
    return _amberManager.getDataSource();
  }

  /**
   * Sets the Resin isolation.
   */
  public void setResinIsolation(int resinIsolation)
  {
    _ejbTransactionManager.setResinIsolation(resinIsolation);
  }

  /**
   * Sets the Resin isolation for the container.
   */
  public int getResinIsolation()
  {
    return _ejbTransactionManager.getResinIsolation();
  }

  /**
   * Sets the JDBC isolation.
   */
  public void setJDBCIsolation(int jdbcIsolation)
  {
    _ejbTransactionManager.setJDBCIsolation(jdbcIsolation);
  }

  /**
   * Gets the JDBC isolation level.
   */
  public int getJDBCIsolation()
  {
    return _ejbTransactionManager.getJDBCIsolation();
  }

  /**
   * Gets the transaction timeout
   */
  public long getTransactionTimeout()
  {
    return _ejbTransactionManager.getTransactionTimeout();
  }

  /**
   * Sets the transaction timout.
   */
  public void setTransactionTimeout(long transactionTimeout)
  {
    _ejbTransactionManager.setTransactionTimeout(transactionTimeout);
  }

  /**
   * Gets the lazy-load on transaction
   */
  public boolean isEntityLoadLazyOnTransaction()
  {
    return _entityLoadLazyOnTransaction;
  }

  /**
   * Sets the lazy-load on transaction
   */
  public void setEntityLoadLazyOnTransaction(boolean isLazy)
  {
    _entityLoadLazyOnTransaction = isLazy;
  }

  /**
   * Sets the queue connection factory for the container.
   */
  public void setJMSConnectionFactory(ConnectionFactory factory)
  {
    _jmsConnectionFactory = factory;
  }

  /**
   * Sets the consumer maximum for the container.
   */
  public void setMessageConsumerMax(int consumerMax)
  {
    _messageConsumerMax = consumerMax;
  }

  /**
   * Sets the consumer maximum for the container.
   */
  public int getMessageConsumerMax()
  {
    return _messageConsumerMax;
  }

  /**
   * Gets the queue connection factory for the container.
   */
  public ConnectionFactory getConnectionFactory()
  {
    return _jmsConnectionFactory;
  }

  /**
   * Returns the work path.
   */
  public Path getWorkPath()
  {
    return _workPath;
  }

  /**
   * Sets the work path.
   */
  public void setWorkPath(Path workPath)
  {
    // _workPath = workPath;
  }

  /**
   * Returns true if the server should auto-compile.
   */
  public boolean isAutoCompile()
  {
    return _autoCompile;
  }

  /**
   * Sets true if should auto-compile
   */
  public void setAutoCompile(boolean autoCompile)
  {
    _autoCompile = autoCompile;
  }

  /**
   * Returns true if the server should allow POJO beans.
   */
  public boolean isAllowPOJO()
  {
    return _ejbConfig.isAllowPOJO();
  }

  /**
   * Sets true if should allow POJO beans
   */
  public void setAllowPOJO(boolean allowPOJO)
  {
    _ejbConfig.setAllowPOJO(allowPOJO);
  }

  /**
   * Sets whether clients in the same JVM can use a fast in-memory call.
   */
  public void setAllowJVMCall(boolean allowJVMCall)
  {
    _allowJVMCall = allowJVMCall;
  }

  /**
   * Sets whether clients in the same class loader call can pass arguments
   * by reference.
   */
  public void setAllowReferenceCall(boolean allowReferenceCall)
  {
    _allowReferenceCall = allowReferenceCall;
  }

  /**
   * Sets true if database schema should be generated automatically.
   */
  public void setCreateDatabaseSchema(boolean create)
  {
    _createDatabaseSchema = create;

    _amberManager.setCreateDatabaseTables(create);
  }

  /**
   * True if database schema should be generated automatically.
   */
  public boolean getCreateDatabaseSchema()
  {
    return _createDatabaseSchema;
  }

  /**
   * Sets true if database schema should be generated automatically.
   */
  public void setValidateDatabaseSchema(boolean validate)
  {
    _validateDatabaseSchema = validate;
    
    _amberManager.setValidateDatabaseTables(validate);
  }

  /**
   * True if database schema should be generated automatically.
   */
  public boolean getValidateDatabaseSchema()
  {
    return _validateDatabaseSchema;
  }

  /**
   * Gets the cache timeout.
   */
  public long getCacheTimeout()
  {
    return _entityCacheTimeout;
  }

  /**
   * Sets the cache timeout.
   */
  public void setCacheTimeout(long cacheTimeout)
  {
    _entityCacheTimeout = cacheTimeout;
    _amberManager.setTableCacheTimeout(cacheTimeout);
  }

  /**
   * Gets the cache size.
   */
  public int getCacheSize()
  {
    return _entityCacheSize;
  }

  /**
   * Sets the cache size.
   */
  public void setCacheSize(int cacheSize)
  {
    _entityCacheSize = cacheSize;
  }

  /**
   * Returns the admin class.
   */
  public EJBAdmin getAdmin()
  {
    return _ejbAdmin;
  }

  /**
   * Returns the ejb config.
   */
  public EjbConfig getConfig()
  {
    return _ejbConfig;
  }

  /**
   * Adds a file-set.
   */
  public void addConfigFiles(FileSetType fileSet)
  {
    _ejbConfig.addFileSet(fileSet);
  }
      
  /**
   * Adds an EJB jar.
   */
  public void addEJBJar(Path path)
    throws Exception
  {
    JarPath jar = JarPath.create(path);
    
    Path descriptorPath = jar.lookup("META-INF/ejb-jar.xml");

    if (descriptorPath.exists())
      addEJBPath(descriptorPath);
    
    descriptorPath = jar.lookup("META-INF/resin-ejb-jar.xml");

    if (descriptorPath.exists())
      addEJBPath(descriptorPath);

    Path metaInf = jar.lookup("META-INF");
    if (metaInf.isDirectory()) {
      String []metaList = metaInf.list();
      for (int j = 0; j < metaList.length; j++) {
	String metaName = metaList[j];
	if (metaName.endsWith(".ejb")) {
	  Path metaPath = metaInf.lookup(metaName);
	
	  addEJBPath(metaPath);
	}
      }
    }
  }

  /**
   * Adds an EJB configuration file.
   */
  public void addEJBPath(Path path)
    throws ConfigException
  {
    _ejbConfig.addEJBPath(path);
  }

  /**
   * interface callback.
   */
  public void initEJBs()
    throws Exception
  {
    init();
  }

  /**
   * interface callback.
   */
  public void init()
    throws Exception
  {
    build();

    Environment.addEnvironmentListener(this);
  }
  
  /**
   * Initialize the manager after all the configuration files have been read.
   */
  public void build()
    throws ConfigException
  {
    try {
      _amberManager.init();
      
      _entityCache = new LruCache<EntityKey,QEntityContext>(_entityCacheSize);
      // _persistentManager.setDataSource(_dataSource);

      _protocolManager.init();

      _ejbConfig.configure();

      // initJdbc();
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  public void start()
    throws Exception
  {
    _ejbConfig.deploy();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    for (AbstractServer server : _serverMap.values()) {
      try {
	thread.setContextClassLoader(server.getClassLoader());

	log.fine(server + " starting");
	
	server.start();
      } finally {
	thread.setContextClassLoader(oldLoader);
      }
    }
  }

  public AmberEntityHome getAmberEntityHome(String name)
  {
    return _amberManager.getEntityHome(name);
  }

  public AmberManager getAmberManager()
  {
    return _amberManager;
  }

  public JClassLoader getJClassLoader()
  {
    return getAmberManager().getJClassLoader();
  }

  /**
   * Invalidates the caches for all the beans.
   */
  public void invalidateCache()
  {
  }

  /**
   * Adds a server.
   */
  public void addServer(AbstractServer server)
  {
    _serverMap.put(server.getEJBName(), server);

    try {
      _protocolManager.addServer(server);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the server specified by the serverId.
   */
  public AbstractServer getServer(String serverId)
  {
    if (serverId.startsWith("/"))
      return _serverMap.get(serverId);
    else
      return _serverMap.get("/" + serverId);
  }
  
  /**
   * Returns the server specified by the serverId.
   */
  public AbstractServer getServerByEJBName(String ejbName)
  {
    if (! ejbName.startsWith("/"))
      ejbName = "/" + ejbName;
    
    return _serverMap.get(ejbName);
  }

  /**
   * Adds a new entity.
   */
  public QEntityContext getEntity(EntityServer server, Object key)
  {
    synchronized (_entityKey) {
      _entityKey.init(server, key);
      
      return _entityCache.get(_entityKey);
    }
  }

  /**
   * Adds a new entity.
   */
  public QEntityContext putEntityIfNew(EntityServer server, Object key,
				       QEntityContext context)
  {
    return _entityCache.putIfNew(new EntityKey(server, key), context);
  }

  /**
   * Adds a new entity.
   */
  public void removeEntity(EntityServer server, Object key)
  {
    synchronized (_entityKey) {
      _entityKey.init(server, key);
      _entityCache.remove(_entityKey);
    }
  }

  /**
   * Adds a new entity.
   */
  public void removeBeans(ArrayList<QEntityContext> beans, EntityServer server)
  {
    synchronized (_entityCache) {
      Iterator<LruCache.Entry<EntityKey,QEntityContext>> iter;

      iter = _entityCache.iterator();

      while (iter.hasNext()) {
	LruCache.Entry<EntityKey,QEntityContext> entry = iter.next();

	beans.add(entry.getValue());

	iter.remove();
      }
    }
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    try {
      start();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }

  /**
   * Closes the container.
   */
  public void destroy()
  {
    if (! _lifecycle.toDestroy())
      return;

    try {
      ArrayList<AbstractServer> servers;
      servers = new ArrayList<AbstractServer>(_serverMap.values());
      _serverMap.clear();

      /*
	for (int i = 0; i < _serverNames.size(); i++)
	_staticServerMap.remove(_serverNames.get(i));
      */
    
      // only purpose of the sort is to make the qa order consistent
      Collections.sort(servers, new ServerCmp());

      for (AbstractServer server : servers) {
	try {
	  _protocolManager.removeServer(server);
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      for (AbstractServer server : servers) {
	try {
	  server.destroy();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      _serverMap = null;
      _ejbConfig = null;
      _protocolManager.destroy();
      _protocolManager = null;
      _ejbTransactionManager.destroy();
      _ejbTransactionManager = null;
      _amberManager.destroy();
      _amberManager = null;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Sorts the servers so they can be destroyed in a consistent order.
   * (To make QA sane.)
   */
  static class ServerCmp implements Comparator<AbstractServer> {
    public int compare(AbstractServer a, AbstractServer b)
    {
      return a.getEJBName().compareTo(b.getEJBName());
    }
  }
}

