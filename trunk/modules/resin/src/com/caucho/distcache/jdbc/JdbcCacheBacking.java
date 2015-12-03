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

package com.caucho.distcache.jdbc;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import com.caucho.config.ConfigException;
import com.caucho.server.distcache.AbstractCacheBacking;
import com.caucho.server.distcache.CacheLoaderCallback;
import com.caucho.server.distcache.DistCacheEntry;


/**
 * Manages backing for the cache map.
 */
public class JdbcCacheBacking extends AbstractCacheBacking<Object,Object> {
  private JdbcCacheBacking _delegate;
  
  public JdbcCacheBacking()
  {
    try {
      Class<?> cl = Class.forName("com.caucho.distcache.jdbc.JdbcCacheBackingImpl");
      
      _delegate = (JdbcCacheBacking) cl.newInstance();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  protected JdbcCacheBacking(boolean dummy)
  {
  }
  
  public void setDatabase(DataSource database)
  {
    _delegate.setDatabase(database);
  }
  
  public void setTableName(String tableName)
  {
    _delegate.setTableName(tableName);
  }
  
  @PostConstruct
  public void init()
  {
    _delegate.init();
  }
  
  @Override
  public void load(DistCacheEntry entry, CacheLoaderCallback cb)
  {
    _delegate.load(entry, cb);
  }
  
  @Override
  public void write(DistCacheEntry entry)
  {
    _delegate.write(entry);
  }

  @Override
  public void delete(DistCacheEntry entry)
  {
    _delegate.delete(entry);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _delegate + "]";
  }
}
