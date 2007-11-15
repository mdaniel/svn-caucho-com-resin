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
import com.caucho.bytecode.ByteCodeClassMatcher;
import com.caucho.bytecode.ByteCodeClassScanner;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;
import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.ejb.admin.EJBAdmin;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.entity.EntityKey;
import com.caucho.ejb.entity.EntityServer;
import com.caucho.ejb.entity.QEntityContext;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.xa.EjbTransactionManager;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.management.j2ee.EJBModule;
import com.caucho.management.j2ee.J2EEManagedObject;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempBuffer;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages the EJBs.
 */
public class EjbServerManager implements EnvironmentListener
{
  private static final L10N L = new L10N(EjbServerManager.class);
  protected static final Logger log
    = Logger.getLogger(EjbServerManager.class.getName());

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

  private Map<String, J2EEManagedObject> _ejbModuleManagedObjectMap
    = new LinkedHashMap<String, J2EEManagedObject>();

  /**
   * Create a server with the given prefix name.
   */
  EjbServerManager()
  {
    try {
      _amberContainer = AmberContainer.create();

      _envServerManager = new EnvServerManager(_amberContainer);
      
      // _ejbConfig = new EjbConfig(EjbContainer.create());

      _envServerManager.addEjbConfig(_ejbConfig);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static EjbServerManager getLocal()
  {
    return EJBServer.getLocalManager();
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

  public void setLocalJndiPrefix(String localJndiPrefix)
  {
    getProtocolManager().setLocalJndiPrefix(localJndiPrefix);
  }

  public String getLocalJndiPrefix()
  {
    return getProtocolManager().getLocalJndiPrefix();
  }

  public void setRemoteJndiPrefix(String remoteJndiPrefix)
  {
    getProtocolManager().setRemoteJndiPrefix(remoteJndiPrefix);
  }

  public String getRemoteJndiPrefix()
  {
    return getProtocolManager().getRemoteJndiPrefix();
  }

  /**
   * Sets the data source for the container.
   */
  public void setDataSource(DataSource dataSource)
  {
    _amberContainer.setDataSource(dataSource);

    if (_amberPersistenceUnit != null)
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

    if (_amberPersistenceUnit != null)
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

    if (_amberPersistenceUnit != null)
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

  // callback
  public void addEJBModule(String ejbModuleName)
  {
    J2EEManagedObject ejbModuleManagedObject = _ejbModuleManagedObjectMap.get(ejbModuleName);

    if (ejbModuleManagedObject == null) {
      ejbModuleManagedObject = J2EEManagedObject.register(new EJBModule(ejbModuleName));
      _ejbModuleManagedObjectMap.put(ejbModuleName, ejbModuleManagedObject);
    }
  }

  /**
   * Adds an EJB configuration file.
   */
  public void addEJBPath(Path ejbModulePath, Path path)
    throws ConfigException
  {
    _ejbConfig.addEJBPath(ejbModulePath, path);
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
  }
  
  /**
   * Initialize the manager after all the configuration files have been read.
   */
  public void build()
    throws ConfigException
  {
    try {
      if (_amberPersistenceUnit != null)
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

  /**
   * Return the ejb with the passed name.
   */
  public AbstractServer getServer(String ejbName)
  {
    return _envServerManager.getServer(ejbName);
  }

  /**
   * Return the ejb with the passed path and name.
   */
  public AbstractServer getServer(Path path, String ejbName)
  {
    return _envServerManager.getServer(path, ejbName);
  }

  public MessageDestination getMessageDestination(Path path, String name)
  {
    return _envServerManager.getMessageDestination(path, name);
  }

  public MessageDestination getMessageDestination(String name)
  {
    return _envServerManager.getMessageDestination(name);
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
    return getAmberContainer().getJClassLoader();
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
    EnvServerManager manager = _envServerManager;

    if (manager != null)
      manager.removeBeans(beans, server);
  }
  
  /**
   * Returns the bean by its interface.
   */
  public Object getLocalByInterface(Class type)
  {
    return _envServerManager.getLocalByInterface(type);
  }

  /**
   * Returns the bean by its interface.
   */
  public Object getRemoteByInterface(Class type)
  {
    return _envServerManager.getRemoteByInterface(type);
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

    ArrayList<J2EEManagedObject> ejbModuleManagedObjects;
    EnvServerManager envServerManager;
    
    // ejbModuleManagedObjects does not need destroy
    _ejbModuleManagedObjectMap.clear();

    envServerManager = _envServerManager;
    // ejb/0200
    //_envServerManager = null;

    // ejbConfig does not need destroy
    _ejbConfig = null;

    try {
      envServerManager.destroy();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  class EjbClassMatcher implements ByteCodeClassMatcher {
    public boolean isClassMatch(String className)
    {
      return false;
    }

    public boolean isMatch(CharBuffer annotationName)
    {
      if (annotationName.matches("javax.ejb.Stateless"))
        return true;
      else if (annotationName.matches("javax.ejb.Stateful"))
        return true;
      else if (annotationName.matches("javax.ejb.MessageDriven"))
        return true;
      else
        return false;
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

