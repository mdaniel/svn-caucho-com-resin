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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.env.jdbc;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;
import com.caucho.config.ConfigException;
import com.caucho.vfs.*;

import java.net.*;
import java.sql.*;
import javax.sql.*;
import java.util.*;
import java.util.logging.*;

/**
 * Manages databases in a local environment, e.g. for PHP dynamic
 * database lookup.
 */
abstract public class DatabaseFactory {
  private static final Logger log
    = Logger.getLogger(DatabaseFactory.class.getName());
  
  private static final Class<?> _databaseFactoryClass;
  
  private String _name;
  
  private Class<?> _driverClass;
  private String _url;
  private String _user;
  private String _password;
  
  public String _databaseName;
  
  public static DatabaseFactory createBuilder()
  {
    try {
      return (DatabaseFactory) _databaseFactoryClass.newInstance();
    } catch (Exception e) {
      throw new IllegalStateException();
    }
  }
  
  public void setName(String name)
  {
    _name = name;
  }
  
  public String getName()
  {
    return _name;
  }
  
  public void setDriverClass(Class<?> driverClass)
  {
    _driverClass = driverClass;
  }
  
  public Class<?> getDriverClass()
  {
    return _driverClass;
  }
  
  public void setUrl(String url)
  {
    _url = url;
  }
  
  public String getUrl()
  {
    return _url;
  }
  
  public void setUser(String user)
  {
    _user = user;
  }
  
  public String getUser()
  {
    return _user;
  }
  
  public void setPassword(String password)
  {
    _password = password;
  }
  
  public String getPassword()
  {
    return _password;
  }
  
  public void setDatabaseName(String databaseName)
  {
    _databaseName = databaseName;
  }
  
  public String getDatabaseName()
  {
    return _databaseName;
  }
  
  abstract public DataSource create();
  
  static {
    Class<?> factoryClass = null;
    
    try {
      String className = DatabaseFactory.class.getName() + "Impl";
      
      factoryClass = Class.forName(className);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    _databaseFactoryClass = factoryClass;
  }
}

