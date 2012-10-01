/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.env.jdbc.DatabaseFactory;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

/**
 * Configuration for the env-entry pattern.
 */
public class DataSourceRef extends ResourceGroupConfig {
  private static final L10N L = new L10N(DataSourceRef.class);
  private static final Logger log = Logger.getLogger(DataSourceRef.class.getName());

  private String _name;
  private Class<?> _className;
  private String _serverName;
  private String _databaseName;
  private int _portNumber;
  private String _url;
  private String _userName;
  private String _password;
  
  private Object _objectValue;
  
  private long _loginTimeout;
  private boolean _isTransactional;
  private int _isolation_level;
  
  public DataSourceRef()
  {
  }

  @Override
  public void setId(String id)
  {
  }
  
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }

  @Configurable
  public void setClassName(Class<?> cl)
  {
    _className = cl;
  }

  @Configurable
  public void setDatabaseName(String value)
  {
    _databaseName = value;
  }

  @Configurable
  public void setServerName(String value)
  {
    _serverName = value;
  }

  @Configurable
  public void setPortNumber(int value)
  {
    _portNumber = value;
  }

  @Configurable
  public void setUrl(String url)
  {
    _url = url;
  }

  @Configurable
  public void setUser(String userName)
  {
    _userName = userName;
  }

  @Configurable
  public void setPassword(String password)
  {
    _password = password;
  }
  
  @Configurable
  public void addProperty(Property prop)
  {
    
  }
  
  public void setLoginTimeout(int ms)
  {
    _loginTimeout = ms;
  }
  
  public void setTransactional(boolean isTransactional)
  {
    _isTransactional = isTransactional;
  }
  
  public void setIsolationLevel(String level)
  {
    
  }
  
  public void setInitialPoolSize(int size)
  {
    
  }
  
  public void setMinPoolSize(int size)
  {
    
  }
  
  public void setMaxPoolSize(int size)
  {
    
  }
  
  public void setMaxIdleTime(int time)
  {
    
  }
  
  public void setMaxStatements(int statements)
  {
    
  }

  /**
   * Gets the env-entry-value
   */
  // XXX: ejb/0fd0 vs ejb/0g03
  @PostConstruct
  public void init()
    throws Exception
  {
    if (_name == null)
      throw new ConfigException(L.l("data-sourceneeds 'name' attribute"));
    
    /*
    if (_type == null)
      throw new ConfigException(L.l("env-entry needs 'env-entry-type' attribute"));
      */
    super.init();

    // actually, should register for validation
    /*
    if (_value == null)
      return;
      */
    
    if (! isProgram())
      deploy();
  }
  
  /**
   * Configures the bean using the current program.
   * 
   * @param bean the bean to configure
   * @param env the Config environment
   */
  @Override
  public <T> void inject(T bean, CreationalContext<T> env)
  {
    
  }
  
  @Override
  public Object getValue()
  {
    if (getLookupName() != null) {
      try {
        return Jndi.lookup(getLookupName());
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    
    if (_objectValue == null)
      deploy();
    /*
    if (_objectValue == null)
      throw new NullPointerException(toString());
    */
    return _objectValue;
  }

  @Override
  public void deploy()
  {
    if (_objectValue != null)
      return;
    
    super.deploy();

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    try {
      // ejb/8220, tck 
      if (getJndiClassLoader() != null)
        thread.setContextClassLoader(getJndiClassLoader());

      Jndi.bindDeepShort(_name, this);
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(loader);
    }
    
    DatabaseFactory factory = DatabaseFactory.createBuilder();
    factory.setName(_name);
    factory.setDriverClass(_className);
    factory.setUrl(_url);
    factory.setUser(_userName);
    factory.setPassword(_password);
    factory.setDatabaseName(_databaseName);
    
    _objectValue = factory.create();
    
    /*
    InjectManager cdiManager = InjectManager.create();
    BeanBuilder<?> builder = cdiManager.createBeanFactory(DataSource.class);
    
    // CDI names can't have '.'
    builder.name(_name);
    
    // server/1516
    builder.qualifier(Names.create(_name));
    builder.qualifier(DefaultLiteral.DEFAULT);

    cdiManager.addBean(builder.singleton(_objectValue));
    */
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
  
  static class Property {
    private String _name;
    private String _value;
    
    public void setName(String name)
    {
      _name = name;
    }
    
    public void setValue(String value)
    {
      _value = value;
    }
  }
}

