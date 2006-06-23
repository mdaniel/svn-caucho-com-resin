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

import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.bytecode.JClassLoader;
import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.ejb.admin.EJBAdmin;
import com.caucho.ejb.cfg.EjbConfig;
import com.caucho.ejb.entity.EntityKey;
import com.caucho.ejb.entity.EntityServer;
import com.caucho.ejb.entity.QEntityContext;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.xa.EjbTransactionManager;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.mbeans.j2ee.EJBModule;
import com.caucho.mbeans.j2ee.J2EEAdmin;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the EJBs.
 */
public class EjbServerManager implements EJBServerInterface, EnvironmentListener {
  private static final L10N L = new L10N(EjbServerManager.class);
  protected static final Logger log = Log.open(EjbServerManager.class);

  private EnvironmentClassLoader _classLoader;

  private EnvServerManager _envServerManager;

  private boolean _autoCompile = true;

  private boolean _entityLoadLazyOnTransaction = true;

  protected boolean _allowJVMCall = true;
  protected boolean _allowReferenceCall = true;

  protected boolean _createDatabaseSchema;
  protected boolean _validateDatabaseSchema;

  private boolean _hasInitJdbc;
  private ConfigException _initException;

  private EjbConfig _ejbConfig;
  
  private AmberContainer _amberContainer;
  private AmberPersistenceUnit _amberPersistenceUnit;

  protected ConnectionFactory _jmsConnectionFactory;
  private int _messageConsumerMax = 5;

  private EntityKey _entityKey = new EntityKey();

  private final Lifecycle _lifecycle = new Lifecycle(log, "ejb-manager");

  private Map<String, J2EEAdmin> _ejbModuleAdminMap = new LinkedHashMap<String, J2EEAdmin>();

  /**
   * Create a server with the given prefix name.
   */
  EjbServerManager()
  {
    try {
      _amberContainer = AmberContainer.getLocalContainer();

      _amberPersistenceUnit = AmberContainer.getLocalContainer().createPersistenceUnit("resin-ejb");
      _amberPersistenceUnit.setBytecodeGenerator(false);

      _envServerManager = new EnvServerManager(_amberPersistenceUnit);

      _ejbConfig = new EjbConfig(this);

      _envServerManager.addEjbConfig(_ejbConfig);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the loader.
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _envServerManager.getClassLoader();
  }

  /**
   * Returns the environment server manager.
   */
  public EnvServerManager getEnvServerManager()
  {
    return _envServerManager;
  }

  /**
   * Returns the protocol manager.
   */
  public EjbProtocolManager getProtocolManager()
  {
    return _envServerManager.getProtocolManager();
  }

  /**
   * Returns the transaction manager.
   */
  public EjbTransactionManager getTransactionManager()
  {
    return _envServerManager.getTransactionManager();
  }

  public AmberContainer getAmberContainer()
  {
    return _amberContainer;
  }
  
  /**
   * Sets the data source for the container.
   */
  public void setDataSource(DataSource dataSource)
  {
    _amberContainer.setDataSource(dataSource);
    _amberPersistenceUnit.setDataSource(dataSource);
  }

  /**
   * Sets the data source for the container.
   */
  public DataSource getDataSource()
  {
    return _amberContainer.getDataSource();
  }

  /**
   * The read-only data source for the container.
   */
  public void setReadDataSource(DataSource dataSource)
  {
    _amberContainer.setReadDataSource(dataSource);
  }

  /**
   * The read-only data source for the container.
   */
  public DataSource getReadDataSource()
  {
    return _amberContainer.getReadDataSource();
  }

  /**
   * The xa data source for the container.
   */
  public void setXADataSource(DataSource dataSource)
  {
    _amberContainer.setXADataSource(dataSource);
  }

  /**
   * The read-only data source for the container.
   */
  public DataSource getXADataSource()
  {
    return _amberContainer.getXADataSource();
  }

  /**
   * Sets the Resin isolation.
   */
  public void setResinIsolation(int resinIsolation)
  {
    getTransactionManager().setResinIsolation(resinIsolation);
  }

  /**
   * Sets the Resin isolation for the container.
   */
  public int getResinIsolation()
  {
    return getTransactionManager().getResinIsolation();
  }

  /**
   * Sets the JDBC isolation.
   */
  public void setJDBCIsolation(int jdbcIsolation)
  {
    getTransactionManager().setJDBCIsolation(jdbcIsolation);
  }

  /**
   * Gets the JDBC isolation level.
   */
  public int getJDBCIsolation()
  {
    return getTransactionManager().getJDBCIsolation();
  }

  /**
   * Gets the transaction timeout
   */
  public long getTransactionTimeout()
  {
    return getTransactionManager().getTransactionTimeout();
  }

  /**
   * Sets the transaction timout.
   */
  public void setTransactionTimeout(long transactionTimeout)
  {
    getTransactionManager().setTransactionTimeout(transactionTimeout);
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
    return _envServerManager.getWorkPath();
  }

  /**
   * Sets the work path.
   */
  public void setWorkPath(Path workPath)
  {
    _envServerManager.setWorkPath(workPath);
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

    _amberPersistenceUnit.setCreateDatabaseTables(create);
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
    
    _amberPersistenceUnit.setValidateDatabaseTables(validate);
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
    return _envServerManager.getCacheTimeout();
  }

  /**
   * Sets the cache timeout.
   */
  public void setCacheTimeout(long cacheTimeout)
  {
    _envServerManager.setCacheTimeout(cacheTimeout);
  }

  /**
   * Gets the cache size.
   */
  public int getCacheSize()
  {
    return _envServerManager.getCacheSize();
  }

  /**
   * Sets the cache size.
   */
  public void setCacheSize(int cacheSize)
  {
    _envServerManager.setCacheSize(cacheSize);
  }

  /**
   * Returns the admin class.
   */
  public EJBAdmin getAdmin()
  {
    return _envServerManager.getAdmin();
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
    String ejbModuleName = path.toString();

    JarPath jar;

    if (path instanceof JarPath)
      jar = (JarPath) path;
    else
      jar = JarPath.create(path);

    Path descriptorPath = jar.lookup("META-INF/ejb-jar.xml");

    if (descriptorPath.exists())
      addEJBPath(ejbModuleName, descriptorPath);
    
    descriptorPath = jar.lookup("META-INF/resin-ejb-jar.xml");

    if (descriptorPath.exists())
      addEJBPath(ejbModuleName, descriptorPath);

    Path metaInf = jar.lookup("META-INF");
    if (metaInf.isDirectory()) {
      String []metaList = metaInf.list();
      for (int j = 0; j < metaList.length; j++) {
	String metaName = metaList[j];
	if (metaName.endsWith(".ejb")) {
	  Path metaPath = metaInf.lookup(metaName);
	
	  addEJBPath(ejbModuleName, metaPath);
	}
      }
    }
  }

  // callback
  public void addEJBModule(String ejbModuleName)
  {
    J2EEAdmin ejbModuleAdmin = _ejbModuleAdminMap.get(ejbModuleName);

    if (ejbModuleAdmin == null) {
      ejbModuleAdmin = new J2EEAdmin(new EJBModule(ejbModuleName));
      _ejbModuleAdminMap.put(ejbModuleName, ejbModuleAdmin);
    }
  }

  /**
   * Adds an EJB configuration file.
   */
  public void addEJBPath(String ejbModuleName, Path path)
    throws ConfigException
  {
    _ejbConfig.addEJBPath(ejbModuleName, path);
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
    _envServerManager.init();
    
    build();

    for (J2EEAdmin ejbModuleAdmin : _ejbModuleAdminMap.values())
      ejbModuleAdmin.start();
  }
  
  /**
   * Initialize the manager after all the configuration files have been read.
   */
  public void build()
    throws ConfigException
  {
    try {
      _amberPersistenceUnit.init();

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
    _envServerManager.start();
  }

  public AmberEntityHome getAmberEntityHome(String name)
  {
    return _amberPersistenceUnit.getEntityHome(name);
  }

  public AmberPersistenceUnit getAmberManager()
  {
    return _amberPersistenceUnit;
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
    _envServerManager.addServer(server);
  }

  /**
   * Returns the server specified by the serverId.
   */
  public AbstractServer getServer(String serverId)
  {
    return _envServerManager.getServer(serverId);
  }
  
  /**
   * Returns the server specified by the serverId.
   */
  public AbstractServer getServerByEJBName(String ejbName)
  {
    return _envServerManager.getServerByEJBName(ejbName);
  }

  /**
   * Adds a new entity.
   */
  public QEntityContext getEntity(EntityServer server, Object key)
  {
    return _envServerManager.getEntity(server, key);
  }

  /**
   * Adds a new entity.
   */
  public QEntityContext putEntityIfNew(EntityServer server, Object key,
				       QEntityContext context)
  {
    return _envServerManager.putEntityIfNew(server, key, context);
  }

  /**
   * Adds a new entity.
   */
  public void removeEntity(EntityServer server, Object key)
  {
    _envServerManager.removeEntity(server, key);
  }

  /**
   * Adds a new entity.
   */
  public void removeBeans(ArrayList<QEntityContext> beans, EntityServer server)
  {
    _envServerManager.removeBeans(beans, server);
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
    throws Throwable
  {
    start();
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

    for (J2EEAdmin ejbModuleAdmin : _ejbModuleAdminMap.values())
      ejbModuleAdmin.stop();

    _envServerManager.destroy();

    try {
      _ejbConfig = null;
      _envServerManager = null;
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

