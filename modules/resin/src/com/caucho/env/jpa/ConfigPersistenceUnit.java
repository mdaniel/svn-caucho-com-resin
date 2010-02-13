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

package com.caucho.env.jpa;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;

/**
 * <persistence-unit> tag in the persistence.xml
 */
public class ConfigPersistenceUnit implements PersistenceUnitInfo {
  private static final L10N L = new L10N(ConfigPersistenceUnit.class);
  
  private String _name;
  private String _description;
  private Class<?> _provider;
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
  private HashMap<String,Class<?>> _classMap
    = new HashMap<String,Class<?>>();

  private ArrayList<String> _mappingFiles
    = new ArrayList<String>();

  private ArrayList<String> _jarFiles
    = new ArrayList<String>();

  private ArrayList<URL> _jarFileUrls
    = new ArrayList<URL>();

  public ConfigPersistenceUnit(URL rootUrl)
  {
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
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets the provider class name.
   */
  public void setProvider(Class<?> provider)
  {
    _provider = provider;

    Config.validate(provider, PersistenceProvider.class);
  }

  /**
   * Sets the provider class name.
   */
  public Class<?> getProvider()
  {
    return _provider;
  }

  /**
   * Sets the provider class name.
   */
  @Override
  public String getPersistenceProviderClassName()
  {
    if (_provider != null)
      return _provider.getName();
    else
      return null;
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
   * Sets the transactional data source.
   */
  public void setJtaDataSource(String ds)
  {
    _jtaDataSourceName = ds;
  }

  /**
   * Gets the transactional data source.
   */
  @Override
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
  @Override
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
  public void addAllClasses(Map<String,Class<?>> classMap)
  {
    for (Map.Entry<String,Class<?>> entry : classMap.entrySet()) {
      String k = entry.getKey();
      Class<?> v = entry.getValue();

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
  }
  
  //
  // PersistenceUnitInfo api
  //

  /**
   * Returns the name.
   */
  @Override
  public String getPersistenceUnitName()
  {
    return getName();
  }

  /**
   * Returns the transaction handling.
   */
  @Override
  public PersistenceUnitTransactionType getTransactionType()
  {
    return _transactionType;
  }

  /**
   * Returns the mapping file names.  The files are resource-loadable
   * from the classpath.
   */
  @Override
  public List<String> getMappingFileNames()
  {
    return _mappingFiles;
  }

  /**
   * Returns the list of jars for the managed classes.
   */
  @Override
  public List<URL> getJarFileUrls()
  {
    return _jarFileUrls;
  }

  /**
   * Returns the root persistence unit.
   */
  @Override
  public URL getPersistenceUnitRootUrl()
  {
    return _rootUrl;
  }

  /**
   * Returns the list of managed classes.
   */
  @Override
  public List<String> getManagedClassNames()
  {
    ArrayList<String> names = new ArrayList<String>();
    names.addAll(_classMap.keySet());

    return names;
  }

  /**
   * Returns true if only listed classes are allowed.
   */
  @Override
  public boolean excludeUnlistedClasses()
  {
    return _isExcludeUnlistedClasses;
  }

  /**
   * Returns a properties object.
   */
  @Override
  public Properties getProperties()
  {
    return _properties;
  }

  /**
   * Returns the classloader the provider should use to load classes,
   * resources or URLs.
   */
  @Override
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Adds a class transformer.
   */
  @Override
  public void addTransformer(ClassTransformer transformer)
  {
    _loader.addTransformer(new TransformerAdapter(transformer));
  }

  /**
   * Returns a temporary class loader.
   */
  @Override
  public ClassLoader getNewTempClassLoader()
  {
    return _loader.getNewTempClassLoader();
  }

  protected DataSource loadDataSource(String name)
  {
    DataSource ds = (DataSource) Jndi.lookup(name);

    if (ds != null)
      return ds;

    return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
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
