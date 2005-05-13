/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.connection;

import java.util.Iterator;
import java.util.List;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.caucho.log.Log;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.amber.AmberManager;
import com.caucho.amber.AmberQuery;
import com.caucho.amber.AmberException;
import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.EntityFactory;

import com.caucho.amber.query.UserQuery;
import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.query.CachedQueryKey;

import com.caucho.amber.collection.AmberCollection;

/**
 * Similar to the AmberConnection bug with no entities, or transactions
 */
public class CacheConnectionImpl extends AmberConnectionImpl {
  private static final L10N L = new L10N(CacheConnectionImpl.class);
  private static final Logger log = Log.open(CacheConnectionImpl.class);

  private Connection _conn;

  private int _depth;

  private CachedQueryKey _queryKey = new CachedQueryKey();

  public CacheConnectionImpl(AmberManager amberManager)
  {
    super(amberManager);
  }

  /**
   * Registers a collection.
   */
  public void register(AmberCollection query)
  {
  }

  /**
   * Returns true if a transaction is active.
   */
  public boolean isInTransaction()
  {
    return false;
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(Class cl, Object key)
    throws AmberException
  {
    AmberEntityHome entityHome = _amberManager.getEntityHome(cl.getName());

    if (entityHome == null)
      return null;
    else {
      try {
	entityHome.init();
      } catch (ConfigException e) {
	throw new AmberException(e);
      }
      
      Entity entity = entityHome.load(this, key);

      return entity;
    }
  }


  /**
   * Returns the entity for the connection.
   */
  public Entity getEntity(EntityItem item)
  {
    return item.copy(this);
  }

  /**
   * Loads the object based on itself.
   */
  public Object makePersistent(Object obj)
    throws SQLException
  {
    Entity entity = (Entity) obj;

    // check to see if exists

    if (entity == null)
      throw new NullPointerException();

    Class cl = entity.getClass();
    
    AmberEntityHome entityHome;
    entityHome = _amberManager.getEntityHome(entity.getClass().getName());

    if (entityHome == null)
      throw new AmberException(L.l("entity has no matching home"));
    
    entityHome.makePersistent(entity, this, false);

    return entity;
  }

  /**
   * Loads the object with the given class.
   */
  public Entity loadLazy(Class cl, String name, Object key)
  {
    if (key == null)
      return null;
    
    try {
      AmberEntityHome home = _amberManager.getEntityHome(name);
      
      if (home == null)
	throw new RuntimeException(L.l("no matching home for {0}", cl.getName()));

      home.init();

      Object obj = home.loadLazy(this, key);

      Entity entity = (Entity) obj;
    
      return entity;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    } catch (ConfigException e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   */
  public Object loadProxy(String name, Object key)
  {
    if (key == null)
      return null;
    
    try {
      AmberEntityHome home = _amberManager.getEntityHome(name);
      
      if (home == null)
	throw new RuntimeException(L.l("no matching home for {0}", name));

      home.init();

      EntityItem item = home.findEntityItem(this, key, false);

      if (item == null)
	return null;

      EntityFactory factory = home.getEntityFactory();

      Object entity = factory.getEntity(this, item);

      return entity;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    } catch (ConfigException e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(Class cl, long intKey)
    throws AmberException
  {
    AmberEntityHome entityHome = _amberManager.getEntityHome(cl.getName());
    
    if (entityHome == null)
      return null;

    Object key = entityHome.toObjectKey(intKey);

    return load(cl, key);
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object loadLazy(Class cl, long intKey)
    throws AmberException
  {
    AmberEntityHome entityHome = _amberManager.getEntityHome(cl.getName());
    
    if (entityHome == null)
      return null;

    Object key = entityHome.toObjectKey(intKey);

    return loadLazy(cl, cl.getName(), key);
  }

  /**
   * Matches the entity.
   */
  public Entity getEntity(Class cl, Object key)
  {
    return null;
  }

  /**
   * Starts a transaction.
   */
  public void beginTransaction()
    throws SQLException
  {
  }

  /**
   * Commits a transaction.
   */
  public void commit()
    throws SQLException
  {
  }

  /**
   * Commits a transaction.
   */
  public void rollback()
    throws SQLException
  {
  }

  /**
   * Commits a transaction.
   */
  public void flush()
    throws SQLException
  {
  }

  /**
   * Returns the connection.
   */
  public Connection getConnection()
    throws SQLException
  {
    DataSource dataSource = _amberManager.getReadDataSource();

    if (dataSource == null)
      _amberManager.getDataSource();
      
    if (_conn == null) {
      _conn = dataSource.getConnection();
    }
    else if (_conn.isClosed()) {
      closeConnection();
      _conn = dataSource.getConnection();
    }

    return _conn;
  }
  
  /**
   * Makes the object transactional.
   *
   * @param obj the object to save
   *
   * @return the proxy for the saved object
   */
  public void makeTransactional(Entity entity)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Updates the database with the values in object.  If the object does
   * not exist, throws an exception.
   *
   * @param obj the object to update
   */
  public void update(Object obj)
  {
  }
  
  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(Object obj)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Loads the object with the given class.
   */
  public void update(Entity entity)
  {
  }
  
  /**
   * Deletes the object.
   *
   * @param obj the object to delete
   */
  public void delete(Object obj)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the query key.
   */
  public CachedQueryKey getQueryKey()
  {
    if (_queryKey == null)
      _queryKey = new CachedQueryKey();
    
    return _queryKey;
  }
  
  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareQuery(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, false);
  }
  
  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareLazyQuery(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, true);
  }
  
  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareUpdate(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, true);
  }
  
  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  private AmberQuery prepareQuery(String queryString, boolean isLazy)
    throws AmberException
  {
    try {
      _amberManager.initEntityHomes();
    } catch (Exception e) {
      throw AmberRuntimeException.create(e);
    }
    
    QueryParser parser = new QueryParser(queryString);

    parser.setAmberManager(_amberManager);
    parser.setLazyResult(isLazy);

    AbstractQuery queryProgram = parser.parse();
    UserQuery query = new UserQuery(queryProgram);
    
    query.setSession(this);
    
    return query;
  }

  /**
   * Select a list of objects with a Hibernate query.
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public ResultSet query(String hsql)
    throws SQLException
  {
    AmberQuery query = prepareQuery(hsql);

    return query.executeQuery();
  }

  /**
   * Updates the database with a query
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public int update(String hsql)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Select a list of objects with a Hibernate query.
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public List find(String hsql)
    throws SQLException
  {
    AmberQuery query = prepareQuery(hsql);

    return query.list();
  }
			
  /**
   * Select a list of objects with a Hibernate query, using a single
   * JDBC "?" parameter.
   *
   * @param query the hibernate query
   * @param value the parameter value
   *
   * @return the query results.
   */
  public List find(String query, Object value)
    throws SQLException
  {
    return null;
  }

  /**
   * Select a list of objects with a Hibernate query, with a list of
   * JDBC "?" parameters.
   *
   * @param query the hibernate query
   * @param values the parameter values
   *
   * @return the query results.
   */
  public List find(String query, Object[] values)
    throws SQLException
  {
    return null;
  }

  /**
   * Returns the results of a query in an iterator.
   *
   * @param query the query string.
   */
  public Iterator iterate(String query)
    throws SQLException
  {
    return null;
  }
			
  /**
   * Select a list of objects with a Hibernate query, using a single
   * JDBC "?" parameter.
   *
   * @param query the hibernate query
   * @param value the parameter value
   *
   * @return the query results.
   */
  public Iterator iterate(String query, Object value)
    throws SQLException
  {
    return null;
  }

  /**
   * Select a list of objects with a Hibernate query, with a list of
   * JDBC "?" parameters.
   *
   * @param query the hibernate query
   * @param values the parameter values
   * @param types the parameter types
   *
   * @return the query results.
   */
  public Iterator iterate(String query, Object[] values)
    throws SQLException
  {
    return null;
  }
  /**
   * Cleans up the connection.
   */
  public void cleanup()
  {
    freeConnection();
  }

  /**
   * Pushes the allocation depth.
   */
  public void pushDepth()
  {
    _depth++;
  }

  /**
   * Pops the allocation depth.
   */
  public void popDepth()
  {
    _depth--;

    if (_depth == 0)
      closeConnection();
  }

  /**
   * Frees the connection.
   */
  public void freeConnection()
  {
    if (_depth != 0)
      return;

    closeConnection();
      
    _amberManager.freeCacheConnection(this);
  }

  /**
   * Closes the connection.
   */
  protected void closeConnection()
  {
    super.freeConnection();

    Connection conn = _conn;
    _conn = null;
    
    try {
      if (conn != null)
	conn.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Displays the connection as a string.
   */
  public String toString()
  {
    return "CacheConnectionImpl[]";
  }

  /**
   * Finalizer.
   */
  public void finalize()
  {
  }
}
