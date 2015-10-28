/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

import java.sql.Driver;

import javax.sql.DataSource;

import com.caucho.sql.DBPool;
import com.caucho.sql.DriverConfig;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.cf.QName;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;


/**
 * Manages databases in a local environment, e.g. for PHP dynamic
 * database lookup.
 */
public class DatabaseFactoryImpl extends DatabaseFactory {
  private static final QName URL = new QName("url");
  
  @Override
  public DataSource create()
  {
    Class<?> driverClass = getDriverClass();
    
    DBPool pool = new DBPool();
    pool.setName(getName());
    
    DriverConfig driver = pool.createDriver();
    driver.setType(driverClass);
    
    ConfigType<?> configType = TypeFactoryConfig.getType(driverClass);

    if (getUrl() != null) {
      if (configType.getAttribute(URL) != null
          || Driver.class.isAssignableFrom(driverClass)) {
        driver.setURL(getUrl());
      }
    }

    if (getDatabaseName() != null) {
      driver.setProperty("database-name", getDatabaseName());
    }
    
    if (getUser() != null)
      driver.setUser(getUser());
    
    if (getPassword() != null)
      driver.setPassword(getPassword());
    
    try {
      driver.init();
      
      pool.init();
    } catch (Exception e) {
      e.printStackTrace();
      throw ConfigException.create(e);
    }
    
    return pool;
  }
}

