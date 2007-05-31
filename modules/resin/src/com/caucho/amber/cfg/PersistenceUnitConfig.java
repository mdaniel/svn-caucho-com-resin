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

package com.caucho.amber.cfg;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.bytecode.JClass;
import com.caucho.loader.*;
import com.caucho.vfs.Path;

import javax.sql.DataSource;
import javax.persistence.spi.*;
import java.net.URL;
import java.util.*;

/**
 * <persistence-unit> tag in the persistence.xml
 */
public class PersistenceUnitConfig implements PersistenceUnitInfo {
  private String _name;
  private String _provider;
  private DataSource _jtaDataSource;
  private DataSource _nonJtaDataSource;
  private boolean _isExcludeUnlistedClasses;

  private URL _rootUrl;
  private DynamicClassLoader _loader;

  // className -> type
  private HashMap<String, JClass> _classMap
    = new HashMap<String, JClass>();

  private ArrayList<String> _mappingFiles
    = new ArrayList<String>();

  public PersistenceUnitConfig()
  {
    Thread thread = Thread.currentThread();

    _loader = (DynamicClassLoader) thread.getContextClassLoader();
  }

  /**
   * Returns the unit name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the unit name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the transaction type.
   */
  public void setTransactionType(String type)
  {
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the provider class name.
   */
  public void setProvider(String provider)
  {
    _provider = provider;
  }

  /**
   * Sets the transactional data source.
   */
  public void setJtaDataSource(DataSource ds)
  {
    _jtaDataSource = ds;
  }

  /**
   * Gets the transactional data source.
   */
  public DataSource getJtaDataSource()
  {
    return _jtaDataSource;
  }

  /**
   * Sets the non-transactional data source.
   */
  public void setNonJtaDataSource(DataSource ds)
  {
    _nonJtaDataSource = ds;
  }

  /**
   * Sets the non-transactional data source.
   */
  public DataSource getNonJtaDataSource()
  {
    return _nonJtaDataSource;
  }

  /**
   * Sets the mapping file.
   */
  public void addMappingFile(String fileName)
  {
    _mappingFiles.add(fileName);
  }

  /**
   * Returns the mapping files.
   */
  public ArrayList<String> getMappingFiles()
  {
    return _mappingFiles;
  }

  /**
   * Sets the jars with classes.
   */
  public void addJarFile(Path file)
  {
  }

  /**
   * Adds a configured class.
   */
  public void addClass(String cl)
  {
    // null means the class is not yet verified as:
    // Entity | Embeddable | MappedSuperclass

    _classMap.put(cl, null);
  }

  /**
   * Adds a map of configured classes.
   */
  public void addAllClasses(Map<String, JClass> classMap)
  {
    for (Map.Entry<String, JClass> entry : classMap.entrySet()) {
      String k = entry.getKey();
      JClass v = entry.getValue();

      if (! _classMap.containsKey(k))
        _classMap.put(k, v);
    }
  }

  /**
   * Returns true if only listed classes should be used.
   */
  public boolean isExcludeUnlistedClasses()
  {
    return _isExcludeUnlistedClasses;
  }

  /**
   * Sets true if only listed classes should be used.
   */
  public void setExcludeUnlistedClasses(boolean isExclude)
  {
    _isExcludeUnlistedClasses = isExclude;
  }

  /**
   * Adds the properties.
   */
  public PropertiesConfig createProperties()
  {
    return new PropertiesConfig();
  }

  public AmberPersistenceUnit init(AmberContainer container,
                                   ArrayList<EntityMappingsConfig> entityMappings)
    throws Exception
  {
    AmberPersistenceUnit unit
      = new AmberPersistenceUnit(container, _name);

    unit.setJPA(true);

    unit.setJtaDataSource(_jtaDataSource);
    unit.setNonJtaDataSource(_nonJtaDataSource);

    unit.setEntityMappingsList(entityMappings);

    unit.init();

    for (Map.Entry<String, JClass> entry : _classMap.entrySet()) {
      String className = entry.getKey();
      JClass type = entry.getValue();

      unit.addEntityClass(className, type);
    }

    unit.generate();

    return unit;
  }

  //
  // PersistenceUnitInfo api
  //

  /**
   * Returns the name.
   */
  public String getPersistenceUnitName()
  {
    return getName();
  }

  /**
   * Returns the full class name of the persistence provider.
   */
  public String getPersistenceProviderClassName()
  {
    return _provider;
  }

  /**
   * Returns the transaction handling.
   */
  public PersistenceUnitTransactionType getTransactionType()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the mapping file names.  The files are resource-loadable
   * from the classpath.
   */
  public List<String> getMappingFileNames()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the list of jars for the managed classes.
   */
  public List<URL> getJarFileUrls()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the root persistence unit.
   */
  public URL getPersistenceUnitRootUrl()
  {
    return _rootUrl;
  }

  /**
   * Returns the list of managed classes.
   */
  public List<String> getManagedClassNames()
  {
    ArrayList<String> names = new ArrayList<String>();
    names.addAll(_classMap.keySet());

    return names;
  }

  /**
   * Returns true if only listed classes are allowed.
   */
  public boolean excludeUnlistedClasses()
  {
    return _isExcludeUnlistedClasses;
  }

  /**
   * Returns a properties object.
   */
  public Properties getProperties()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the classloader the provider should use to load classes,
   * resources or URLs.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Adds a class transformer.
   */
  public void addTransformer(ClassTransformer transformer)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a temporary class loader.
   */
  public ClassLoader getNewTempClassLoader()
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "PersistenceUnitConfig[" + _name + "]";
  }

  public class PropertiesConfig {
    public PropertyConfig createProperty()
    {
      return new PropertyConfig();
    }
  }

  public class PropertyConfig {
    public void setName(String name)
    {
    }

    public void setValue(String name)
    {
    }
  }
}
