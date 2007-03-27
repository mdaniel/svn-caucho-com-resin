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

package com.caucho.amber.manager;

import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.cfg.*;
import com.caucho.amber.gen.AmberEnhancer;
import com.caucho.amber.gen.AmberGenerator;
import com.caucho.amber.type.*;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.enhancer.EnhancerManager;
import com.caucho.vfs.Path;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Environment-based container.
 */
public class AmberContainer {
  private static final Logger log
    = Logger.getLogger(AmberContainer.class.getName());

  private static final EnvironmentLocal<AmberContainer> _localContainer
    = new EnvironmentLocal<AmberContainer>();

  private ClassLoader _parentLoader;
  // private EnhancingClassLoader _enhancedLoader;

  private JClassLoader _jClassLoader;

  private AmberEnhancer _enhancer;

  private DataSource _dataSource;
  private DataSource _readDataSource;
  private DataSource _xaDataSource;

  private boolean _createDatabaseTables;

  private HashMap<String,AmberPersistenceUnit> _unitMap
    = new HashMap<String,AmberPersistenceUnit>();

  private HashMap<String,EmbeddableType> _embeddableMap
    = new HashMap<String,EmbeddableType>();

  private HashMap<String,EntityType> _entityMap
    = new HashMap<String,EntityType>();

  private HashMap<String,MappedSuperclassType> _mappedSuperclassMap
    = new HashMap<String,MappedSuperclassType>();

  private HashMap<String,ListenerType> _defaultListenerMap
    = new HashMap<String,ListenerType>();

  private HashMap<String, ArrayList<ListenerType>>
    _entityListenerMap = new HashMap<String, ArrayList<ListenerType>>();

  private Throwable _exception;

  private HashMap<String,Throwable> _embeddableExceptionMap
    = new HashMap<String,Throwable>();

  private HashMap<String,Throwable> _entityExceptionMap
    = new HashMap<String,Throwable>();

  private HashMap<String,Throwable> _listenerExceptionMap
    = new HashMap<String,Throwable>();

  private HashSet<Path> _persistenceRootSet
    = new HashSet<Path>();

  private AmberContainer()
  {
    _parentLoader = Thread.currentThread().getContextClassLoader();
    _jClassLoader = EnhancerManager.create(_parentLoader).getJavaClassLoader();

    _enhancer = new AmberEnhancer(this);

    EnhancerManager.create().addClassEnhancer(_enhancer);

    try {
      if (_parentLoader instanceof DynamicClassLoader)
        ((DynamicClassLoader) _parentLoader).make();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the local container.
   */
  public static AmberContainer getLocalContainer()
  {
    synchronized (_localContainer) {
      AmberContainer container = _localContainer.getLevel();

      if (container == null) {
        container = new AmberContainer();

        _localContainer.set(container);
      }

      return container;
    }
  }

  /**
   * Sets the primary data source.
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Gets the primary data source.
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Sets the read data source.
   */
  public void setReadDataSource(DataSource dataSource)
  {
    _readDataSource = dataSource;
  }

  /**
   * Gets the read data source.
   */
  public DataSource getReadDataSource()
  {
    return _readDataSource;
  }

  /**
   * Sets the xa data source.
   */
  public void setXADataSource(DataSource dataSource)
  {
    _xaDataSource = dataSource;
  }

  /**
   * Gets the XA data source.
   */
  public DataSource getXADataSource()
  {
    return _xaDataSource;
  }

  /**
   * True if database tables should be created automatically.
   */
  public boolean getCreateDatabaseTables()
  {
    return _createDatabaseTables;
  }

  /**
   * True if database tables should be created automatically.
   */
  public void setCreateDatabaseTables(boolean isCreate)
  {
    _createDatabaseTables = isCreate;
  }

  /**
   * Returns the parent loader
   */
  public ClassLoader getParentClassLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the parent loader
   */
  public ClassLoader getEnhancedLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the enhancer.
   */
  public AmberGenerator getGenerator()
  {
    return _enhancer;
  }

  /**
   * Returns the persistence unit JNDI context.
   */
  public String getPersistenceUnitJndiPrefix()
  {
    return "java:comp/env/persistence/_amber_PersistenceUnit/";
  }

  /**
   * Returns the persistence unit JNDI context.
   */
  public String getPersistenceContextJndiPrefix()
  {
    //return "java:comp/env/persistence/PersistenceContext/";
    return "java:comp/env/persistence/";
  }

  /**
   * Returns the JClassLoader.
   */
  public JClassLoader getJClassLoader()
  {
    return _jClassLoader;
  }

  public void init()
  {
  }
  
  /**
   * Returns the EmbeddableType for an introspected class.
   */
  public EmbeddableType getEmbeddable(String className)
  {
    Throwable e = _embeddableExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    return _embeddableMap.get(className);
  }

  /**
   * Returns the EntityType for an introspected class.
   */
  public EntityType getEntity(String className)
  {
    Throwable e = _entityExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    return _entityMap.get(className);
  }

  /**
   * Returns the MappedSuperclassType for an introspected class.
   */
  public MappedSuperclassType getMappedSuperclass(String className)
  {
    Throwable e = _entityExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    MappedSuperclassType type = _mappedSuperclassMap.get(className);

    return type;
  }

  /**
   * Returns the default ListenerType for an introspected class.
   */
  public ListenerType getDefaultListener(String className)
  {
    Throwable e = _listenerExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    return _defaultListenerMap.get(className);
  }

  /**
   * Returns the entity ListenerType for an introspected class.
   */
  public ListenerType getEntityListener(String className)
  {
    Throwable e = _listenerExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    ArrayList<ListenerType> listenerList;

    for (Map.Entry<String, ArrayList<ListenerType>>
           entry : _entityListenerMap.entrySet()) {

      listenerList = entry.getValue();

      if (listenerList == null)
        continue;

      for (ListenerType listener : listenerList) {
        if (className.equals(listener.getBeanClass().getName()))
          return listener;
      }
    }

    return null;
  }

  /**
   * Returns the listener for an introspected class.
   */
  public ListenerType getListener(String className)
  {
    ListenerType listener = getDefaultListener(className);

    if (listener == null)
      listener = getEntityListener(className);

    return listener;
  }

  /**
   * Returns the entity listeners for an entity.
   */
  public ArrayList<ListenerType>
  getEntityListeners(String entityClassName)
  {
    return _entityListenerMap.get(entityClassName);
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addEntityException(String className, Throwable e)
  {
    _entityExceptionMap.put(className, e);
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addException(Throwable e)
  {
    if (_exception == null) {
      _exception = e;

      Environment.setConfigException(e);
    }
  }

  public Throwable getConfigException()
  {
    return _exception;
  }

  /**
   * Adds an embeddable for an introspected class.
   */
  public void addEmbeddable(String className, EmbeddableType type)
  {
    _embeddableMap.put(className, type);
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addEntity(String className, EntityType type)
  {
    _entityMap.put(className, type);
  }

  /**
   * Adds a mapped superclass for an introspected class.
   */
  public void addMappedSuperclass(String className,
                                  MappedSuperclassType type)
  {
    _mappedSuperclassMap.put(className, type);
  }

  /**
   * Adds a default listener.
   */
  public void addDefaultListener(String className,
                                 ListenerType type)
  {
    _defaultListenerMap.put(className, type);
  }

  /**
   * Adds an entity listener.
   */
  public void addEntityListener(String entityClassName,
                                ListenerType listenerType)
  {
    ArrayList<ListenerType> listenerList
      = _entityListenerMap.get(entityClassName);

    if (listenerList == null) {
      listenerList = new ArrayList<ListenerType>();
      _entityListenerMap.put(entityClassName, listenerList);
    }

    listenerList.add(listenerType);
  }

  /**
   * Initialize the entity homes.
   */
  public void initEntityHomes()
  {
    throw new UnsupportedOperationException();
  }

  public AmberPersistenceUnit createPersistenceUnit(String name)
  {
    AmberPersistenceUnit unit = new AmberPersistenceUnit(this, name);

    _unitMap.put(unit.getName(), unit);

    return unit;
  }

  public AmberPersistenceUnit getPersistenceUnit(String name)
  {
    if (_exception != null)
      throw new AmberRuntimeException(_exception);

    return _unitMap.get(name);
  }

  /**
   * Adds a persistence root.
   */
  public void addPersistenceRoot(Path root)
  {
    if (_persistenceRootSet.contains(root))
      return;
    _persistenceRootSet.add(root);
    
    Path persistenceXml = root.lookup("META-INF/persistence.xml");
    InputStream is = null;

    try {
      Path ormXml = root.lookup("META-INF/orm.xml");

      EntityMappingsConfig entityMappings = null;

      if (ormXml.exists()) {
        is = ormXml.openRead();

        entityMappings = new EntityMappingsConfig();
        entityMappings.setRoot(root);

        new Config().configure(entityMappings, is,
                               "com/caucho/amber/cfg/mapping-30.rnc");
      }

      HashMap<String, JClass> classMap
        = new HashMap<String, JClass>();

      // XXX: This is not necessary when <exclude-unlisted-classes/>
      lookupClasses(root.getPath().length(), root, classMap, entityMappings);

      is = persistenceXml.openRead();

      PersistenceConfig persistence = new PersistenceConfig();
      persistence.setRoot(root);

      new Config().configure(persistence, is,
                             "com/caucho/amber/cfg/persistence-30.rnc");

      for (PersistenceUnitConfig unitConfig : persistence.getUnitList()) {
        try {
          unitConfig.addAllClasses(classMap);

          AmberPersistenceUnit unit = unitConfig.init(this, entityMappings);

          _unitMap.put(unit.getName(), unit);
        } catch (Throwable e) {
          addException(e);

          log.log(Level.WARNING, e.toString(), e);
        }
      }

    } catch (ConfigException e) {
      addException(e);

      log.warning(e.getMessage());
    } catch (Throwable e) {
      addException(e);

      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Throwable e) {
      }
    }
  }

  /**
   * Lookup *.class files and add the corresponding
   * fully qualified class names to the list.
   */
  public void lookupClasses(int rootNameLength,
                            Path curr,
                            HashMap<String, JClass> classMap,
                            EntityMappingsConfig entityMappings)
    throws Exception
  {
    Iterator<String> it = curr.iterator();

    while (it.hasNext()) {

      String s = it.next();

      Path path = curr.lookup(s);

      if (path.isDirectory()) {
        lookupClasses(rootNameLength, path, classMap, entityMappings);
      }
      else if (s.endsWith(".class")) {
        String packageName = curr.getPath().substring(rootNameLength);

        packageName = packageName.replace('/', '.');

        if (packageName.length() > 0)
          packageName = packageName + '.';

        String className = packageName + s.substring(0, s.length() - 6);

        JClass type = _jClassLoader.forName(className);

        if (type != null) {

          boolean isEntity
            = type.getAnnotation(javax.persistence.Entity.class) != null;
          boolean isEmbeddable
            = type.getAnnotation(javax.persistence.Embeddable.class) != null;
          boolean isMappedSuperclass
            = type.getAnnotation(javax.persistence.MappedSuperclass.class) != null;

          MappedSuperclassConfig mappedSuperclassOrEntityConfig = null;

          if (entityMappings != null) {
            mappedSuperclassOrEntityConfig = entityMappings.getEntityConfig(className);

            if (mappedSuperclassOrEntityConfig == null)
              mappedSuperclassOrEntityConfig = entityMappings.getMappedSuperclass(className);
          }

          if (isEntity || isEmbeddable || isMappedSuperclass ||
              (mappedSuperclassOrEntityConfig != null)) {
            classMap.put(className, type);
          }
        }
      }
    }
  }
}
