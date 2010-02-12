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

package com.caucho.amber.cfg;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.naming.*;
import com.caucho.config.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.naming.*;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.*;

import java.lang.instrument.*;
import java.security.*;
import java.net.URL;
import java.util.*;
import java.util.logging.*;

/**
 * <persistence-unit> tag in the persistence.xml
 */
public class PersistenceUnitConfig implements PersistenceUnitInfo {
  private static final L10N L = new L10N(PersistenceUnitConfig.class);
  private static final Logger log
    = Logger.getLogger(PersistenceUnitConfig.class.getName());

  private AmberContainer _manager;
  
  private String _name;
  private Class _provider;
  private String _jtaDataSourceName;
  private String _nonJtaDataSourceName;
  private DataSource _jtaDataSource;
  private DataSource _nonJtaDataSource;
  private boolean _isExcludeUnlistedClasses;

  private URL _rootUrl;
  private DynamicClassLoader _loader;

  private PersistenceUnitTransactionType _transactionType
    = PersistenceUnitTransactionType.JTA;

  private Properties _properties = new Properties();

  // className -> type
  private HashMap<String,Class> _classMap
    = new HashMap<String,Class>();

  private ArrayList<String> _mappingFiles
    = new ArrayList<String>();

  private ArrayList<String> _jarFiles
    = new ArrayList<String>();

  private ArrayList<URL> _jarFileUrls
    = new ArrayList<URL>();

  public PersistenceUnitConfig(AmberContainer manager, URL rootUrl)
  {
    _manager = manager;
    
    Thread thread = Thread.currentThread();

    _loader = (DynamicClassLoader) thread.getContextClassLoader();

    _rootUrl = rootUrl;
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
    if ("JTA".equals(type))
      _transactionType = PersistenceUnitTransactionType.JTA;
    else if ("RESOURCE_LOCAL".equals(type))
      _transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
    else
      throw new ConfigException(L.l("'{0}' is an unknown JPA transaction-type.",
				    type));
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
  public void setProvider(Class provider)
  {
    _provider = provider;

    Config.validate(provider, PersistenceProvider.class);
  }

  /**
   * Sets the provider class name.
   */
  public Class getProvider()
  {
    return _provider;
  }

  /**
   * Sets the provider class name.
   */
  public String getPersistenceProviderClassName()
  {
    if (_provider != null)
      return _provider.getName();
    else
      return null;
  }

  /**
   * Sets the transactional data source.
   */
  public void setJtaDataSource(String ds)
  {
    _jtaDataSourceName = ds;
  }

  /**
   * Gets the transactional data source.
   */
  public DataSource getJtaDataSource()
  {
    if (_jtaDataSourceName == null)
      return null;
    
    if (_jtaDataSource == null)
      _jtaDataSource = loadDataSource(_jtaDataSourceName);
    
    return _jtaDataSource;
  }

  /**
   * Gets the transactional data source.
   */
  public String getJtaDataSourceName()
  {
    return _jtaDataSourceName;
  }

  /**
   * Sets the non-transactional data source.
   */
  public void setNonJtaDataSource(String ds)
  {
    _nonJtaDataSourceName = ds;
  }

  /**
   * Sets the non-transactional data source.
   */
  public DataSource getNonJtaDataSource()
  {
    if (_nonJtaDataSourceName == null)
      return null;
    
    if (_nonJtaDataSource == null)
      _nonJtaDataSource = loadDataSource(_nonJtaDataSourceName);
    
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
  public void addJarFile(String fileName)
  {
    _jarFiles.add(fileName);
    try {
      URL url = new URL(Vfs.lookup(fileName).getURL());
      
      _jarFileUrls.add(url);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the jars with classes.
   */
  public ArrayList<String> getJarFiles()
  {
    return _jarFiles;
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
  public void addAllClasses(Map<String,Class> classMap)
  {
    for (Map.Entry<String,Class> entry : classMap.entrySet()) {
      String k = entry.getKey();
      Class v = entry.getValue();

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

  @PostConstruct
  public void init()
  {
    ArrayList<ConfigProgram> defaultList = _manager.getProxyProgram(_name);

    if (defaultList != null) {
      for (ConfigProgram program : defaultList) {
	program.configure(this);
      }
    }
  }
    
  public AmberPersistenceUnit init(AmberContainer container,
                                   ArrayList<EntityMappingsConfig> entityMappings)
    throws Exception
  {
    try {
      AmberPersistenceUnit unit
	= new AmberPersistenceUnit(container, _name);

      unit.setJPA(true);

      if (_jtaDataSourceName != null)
	unit.setJtaDataSourceName(_jtaDataSourceName);
    
      if (_nonJtaDataSourceName != null)
	unit.setNonJtaDataSourceName(_nonJtaDataSourceName);

      unit.setEntityMappingsList(entityMappings);

      unit.init();

      for (Map.Entry<String,Class> entry : _classMap.entrySet()) {
	String className = entry.getKey();
	Class type = entry.getValue();

	unit.addEntityClass(className, type);
      }

      unit.generate();

      return unit;
    } catch (Exception e) {
      if (_rootUrl != null)
	throw ConfigException.createLine(_rootUrl.toString() + ":\n", e);
      else
	throw e;
    }
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
   * Returns the transaction handling.
   */
  public PersistenceUnitTransactionType getTransactionType()
  {
    return _transactionType;
  }

  /**
   * Returns the mapping file names.  The files are resource-loadable
   * from the classpath.
   */
  public List<String> getMappingFileNames()
  {
    return _mappingFiles;
  }

  /**
   * Returns the list of jars for the managed classes.
   */
  public List<URL> getJarFileUrls()
  {
    return _jarFileUrls;
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
    return _properties;
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
    _loader.addTransformer(new TransformerAdapter(transformer));
  }

  /**
   * Returns a temporary class loader.
   */
  public ClassLoader getNewTempClassLoader()
  {
    return _loader.getNewTempClassLoader();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }

  protected DataSource loadDataSource(String name)
  {
    DataSource ds = (DataSource) Jndi.lookup(name);

    if (ds != null)
      return ds;

    return null;
  }

  public class PropertiesConfig {
    public void addProperty(PropertyConfig prop)
    {
      _properties.put(prop.getName(), prop.getValue());
    }
  }

  public static class PropertyConfig {
    private String _name;
    private String _value;
    
    public void setName(String name)
    {
      _name = name;
    }
    
    public String getName()
    {
      return _name;
    }

    public void setValue(String value)
    {
      _value = value;
    }

    public String getValue()
    {
      return _value;
    }
  }

  public static class TransformerAdapter implements ClassFileTransformer {
    private ClassTransformer _transformer;

    TransformerAdapter(ClassTransformer transformer)
    {
      _transformer = transformer;
    }

    public byte[] transform(ClassLoader loader,
			    String className,
			    Class redefineClass,
			    ProtectionDomain domain,
			    byte []classFileBuffer)
      throws IllegalClassFormatException
    {
      return _transformer.transform(loader,
				    className,
				    redefineClass,
				    domain,
				    classFileBuffer);
    }
  }

  /* (non-Javadoc)
   * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceXMLSchemaVersion()
   */
  @Override
  public String getPersistenceXMLSchemaVersion()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
   */
  @Override
  public SharedCacheMode getSharedCacheMode()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
   */
  @Override
  public ValidationMode getValidationMode()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
