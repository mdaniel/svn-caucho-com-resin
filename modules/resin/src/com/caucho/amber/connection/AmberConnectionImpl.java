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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.connection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.caucho.log.Log;

import com.caucho.util.L10N;
import com.caucho.util.LruCache;

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
import com.caucho.amber.query.QueryCacheKey;
import com.caucho.amber.query.ResultSetCacheChunk;

import com.caucho.amber.entity.AmberCompletion;
import com.caucho.amber.entity.TableInvalidateCompletion;
import com.caucho.amber.entity.RowInvalidateCompletion;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.Table;

import com.caucho.amber.collection.AmberCollection;


/**
 * Implementation of the AmberConnection.
 */
public class AmberConnectionImpl {
  private static final L10N L = new L10N(AmberConnectionImpl.class);
  private static final Logger log = Log.open(AmberConnectionImpl.class);

  protected AmberManager _amberManager;
  
  private ArrayList<Entity> _entities = new ArrayList<Entity>();
  
  private ArrayList<Entity> _txEntities = new ArrayList<Entity>();
  
  private ArrayList<AmberCompletion> _completionList =
    new ArrayList<AmberCompletion>();
  
  private ArrayList<AmberCollection> _queries =
    new ArrayList<AmberCollection>();

  private long _xid;
  private boolean _isInTransaction;
  private boolean _isXA;

  private Connection _conn;
  private boolean _isAutoCommit = true;

  private LruCache<String,PreparedStatement> _preparedStatementMap =
    new LruCache<String,PreparedStatement>(32);
  
  private ArrayList<Statement> _statements = new ArrayList<Statement>();

  private QueryCacheKey _queryKey = new QueryCacheKey();

  public AmberConnectionImpl(AmberManager amberManager)
  {
    _amberManager = amberManager;
  }

  /**
   * Returns the amber manaber.
   */
  public AmberManager getAmberManager()
  {
    return _amberManager;
  }

  /**
   * Registers a collection.
   */
  public void register(AmberCollection query)
  {
    _queries.add(query);
  }

  /**
   * Adds a completion
   */
  public void addCompletion(AmberCompletion completion)
  {
    if (! _completionList.contains(completion))
      _completionList.add(completion);
  }

  /**
   * Returns true if a transaction is active.
   */
  public boolean isInTransaction()
  {
    return _isInTransaction;
  }

  /**
   * Returns the cache chunk size.
   */
  public int getCacheChunkSize()
  {
    return 25;
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(Class cl, Object key)
    throws AmberException
  {
    Entity entity = getEntity(cl.getName(), key);

    if (entity != null)
      return entity;

    AmberEntityHome entityHome = _amberManager.getEntityHome(cl.getName());

    if (entityHome == null)
      return null;
    else {
      try {
	entityHome.init();
      } catch (ConfigException e) {
	throw new AmberException(e);
      }
      
      entity = entityHome.load(this, key);

      addEntity(entity);

      return entity;
    }
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(String entityName, Object key)
    throws AmberException
  {
    AmberEntityHome entityHome = _amberManager.getEntityHome(entityName);

    if (entityHome == null)
      return null;

    Entity entity = getEntity(entityName, key);

    if (entity != null)
      return entity;

    try {
      entityHome.init();
    } catch (ConfigException e) {
      throw new AmberException(e);
    }
      
    entity = entityHome.load(this, key);

    addEntity(entity);

    return entity;
  }

  /**
   * Returns the entity for the connection.
   */
  public Entity getEntity(EntityItem item)
  {
    Entity itemEntity = item.getEntity();
    EntityType entityType = itemEntity.__caucho_getEntityType();

    Entity entity = getEntity(entityType.getBeanClass().getName(),
			      itemEntity.__caucho_getPrimaryKey());
    
    if (entity != null)
      return entity;
    else {
      entity = item.copy(this);

      addEntity(entity);

      return entity;
    }
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
    
    // Entity oldEntity = getEntity(cl, entity.__caucho_getPrimaryKey());

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
    return loadLazy(cl.getName(), name, key);
  }

  /**
   * Loads the object with the given class.
   */
  public Entity loadLazy(String className, String name, Object key)
  {
    if (key == null)
      return null;
    
    Entity entity = getEntity(className, key);
    
    try {
      AmberEntityHome home = _amberManager.getEntityHome(name);
      
      if (home == null)
	throw new RuntimeException(L.l("no matching home for {0}", className));

      home.init();

      Object obj = home.loadLazy(this, key);

      entity = (Entity) obj;

      addEntity(entity);
    
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
  public EntityItem findEntityItem(String name, Object key)
  {
    try {
      AmberEntityHome home = _amberManager.getEntityHome(name);
      
      if (home == null)
	throw new RuntimeException(L.l("no matching home for {0}", name));

      home.init();

      return home.findEntityItem(_amberManager.getCacheConnection(), key, false);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   */
  public EntityItem setEntityItem(String name, Object key, EntityItem item)
  {
    try {
      AmberEntityHome home = _amberManager.getEntityHome(name);
      
      if (home == null)
	throw new RuntimeException(L.l("no matching home for {0}", name));

      home.init();

      return home.setEntityItem(key, item);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
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
    
    AmberEntityHome home = _amberManager.getEntityHome(name);
      
    if (home == null)
      throw new RuntimeException(L.l("no matching home for {0}", name));

    return loadProxy(home.getEntityType(), key);
  }

  /**
   * Loads the object with the given class.
   */
  public Object loadProxy(EntityType type, Object key)
  {
    if (key == null)
      return null;
    
    try {
      AmberEntityHome home = type.getHome();

      EntityItem item = home.findEntityItem(this, key, false);

      if (item == null)
	return null;

      EntityFactory factory = home.getEntityFactory();

      Object entity = factory.getEntity(this, item);

      return entity;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
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
  public Entity getEntity(String className, Object key)
  {
    for (int i = _entities.size() - 1; i >= 0; i--) {
      Entity entity = _entities.get(i);

      if (entity.__caucho_match(className, key))
	return entity;
    }

    return null;
  }

  /**
   * Adds an entity.
   */
  public void addEntity(Entity entity)
  {
    if (! _entities.contains(entity)) {
      _entities.add(entity);

      if (_isInTransaction)
	_txEntities.add(entity);
    }
  }

  /**
   * Starts a transaction.
   */
  public void beginTransaction()
    throws SQLException
  {
    _isInTransaction = true;

    if (_conn != null && _isAutoCommit) {
      _isAutoCommit = false;
      _conn.setAutoCommit(false);
    }
    
    // _xid = _factory.getXid();
  }

  /**
   * Sets XA.
   */
  public void setXA(boolean isXA)
  {
    _isXA = isXA;
    _isInTransaction = isXA;
  }

  /**
   * Commits a transaction.
   */
  public void commit()
    throws SQLException
  {
    System.out.println("COMMIT:");
    try {
      flush();

      _xid = 0;
      if (_conn != null) {
	_conn.commit();
      }
    } finally {
      if (! _isXA)
	_isInTransaction = false;

      for (int i = 0; i < _txEntities.size(); i++) {
	Entity entity = _txEntities.get(i);

	entity.__caucho_afterCommit();
      }

      _txEntities.clear();
    }
  }

  /**
   * Commits a transaction.
   */
  public void beforeCommit()
    throws SQLException
  {
    for (int i = 0; i < _txEntities.size(); i++) {
      Entity entity = _txEntities.get(i);

      entity.__caucho_flush();
    }
  }

  /**
   * Commits a transaction.
   */
  public void afterCommit(boolean isCommit)
  {
    if (! _isXA)
      _isInTransaction = false;

    if (isCommit) {
      if (_completionList.size() > 0) {
	_amberManager.complete(_completionList);
      }
    }
    _completionList.clear();

    for (int i = 0; i < _txEntities.size(); i++) {
      Entity entity = _txEntities.get(i);

      try {
	if (isCommit)
	  entity.__caucho_afterCommit();
	else
	  entity.__caucho_afterRollback();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
      
    _txEntities.clear();
  }

  /**
   * Commits a transaction.
   */
  public void rollback()
    throws SQLException
  {
    try {
      flush();
    
      _xid = 0;
      if (_conn != null) {
	_conn.rollback();
      }
    } finally {
      if (! _isXA)
	_isInTransaction = false;
      
      _completionList.clear();
      
      for (int i = 0; i < _txEntities.size(); i++) {
	Entity entity = _txEntities.get(i);

	entity.__caucho_afterRollback();
      }
      
      _txEntities.clear();
    }
  }

  /**
   * Commits a transaction.
   */
  public void flush()
    throws SQLException
  {
    for (int i = 0; i < _txEntities.size(); i++) {
      Entity entity = _txEntities.get(i);

      entity.__caucho_flush();
    }

    if (! isInTransaction()) {
      if (_completionList.size() > 0) {
	_amberManager.complete(_completionList);
      }
      _completionList.clear();
	
      for (int i = 0; i < _txEntities.size(); i++) {
	Entity entity = _txEntities.get(i);

	entity.__caucho_afterCommit();
      }
      
      _txEntities.clear();
    }
  }

  /**
   * Expires the entities
   */
  public void expire()
    throws SQLException
  {
    for (int i = 0; i < _entities.size(); i++) {
      Entity entity = _entities.get(i);

      entity.__caucho_expire();
    }
  }

  /**
   * Returns the connection.
   */
  public Connection getConnection()
    throws SQLException
  {
    if (_conn == null) {
      _conn = _amberManager.getDataSource().getConnection();
      _isAutoCommit = true;
    }
    else if (_conn.isClosed()) {
      closeConnectionImpl();
      _conn = _amberManager.getDataSource().getConnection();
      _isAutoCommit = true;
    }

    if (_isXA) {
    }
    else if (_isInTransaction && _isAutoCommit) {
      _isAutoCommit = false;
      _conn.setAutoCommit(false);
    }
    else if (! _isInTransaction && ! _isAutoCommit) {
      _isAutoCommit = true;
      _conn.setAutoCommit(true);
    }

    return _conn;
  }

  /**
   * Prepares a statement.
   */
  public PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    try {
      Connection conn = getConnection();

      PreparedStatement pstmt = _preparedStatementMap.get(sql);
    
      if (pstmt == null) {
	pstmt = conn.prepareStatement(sql);

	_statements.add(pstmt);

	_preparedStatementMap.put(sql, pstmt);
      }

      return pstmt;
    } catch (SQLException e) {
      closeConnectionImpl();
      
      throw e;
    }
  }

  /**
   * Prepares an insert statement.
   */
  public PreparedStatement prepareInsertStatement(String sql)
    throws SQLException
  {
    PreparedStatement pstmt = _preparedStatementMap.get(sql);

    if (pstmt == null) {
      Connection conn = getConnection();

      if (_amberManager.hasReturnGeneratedKeys())
	pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      else
	pstmt = conn.prepareStatement(sql);

      _statements.add(pstmt);

      _preparedStatementMap.put(sql, pstmt);
    }

    return pstmt;
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
    /*
    if (! isInTransaction())
      throw new AmberRuntimeException(L.l("makePersistent must be called from within a transaction."));

    if (! (obj instanceof Entity)) {
      throw new AmberRuntimeException(L.l("`{0}' is not a known entity class.",
				   obj.getClass().getName()));
    }
    */
  }
  
  /**
   * Updates the database with the values in object.  If the object does
   * not exist, throws an exception.
   *
   * @param obj the object to update
   */
  public void update(Object obj)
  {
    /*
    for (int i = _entities.size() - 1; i >= 0; i--) {
      Entity entity = _entities.get(i);

      if (entity.__caucho_match(obj)) {
	entity.__caucho_load(obj);
	
	return entity;
      }
    }
    */

    /*
    Class cl = obj.getClass();
    
    EntityHome home = _factory.getHome(cl);

    if (home == null)
      throw new AmberException(L.l("no matching home for {0}", cl.getName()));

    Object key = home.getKeyFromEntity(obj);

    Entity entity = getEntity(cl, key);

    if (entity == null) {
      entity = home.load(this, key);

      addEntity(entity);
    }

    entity.__caucho_loadFromObject(obj);
    
    return entity;
    */
  }
  
  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(Object obj)
    throws SQLException
  {
    AmberEntityHome home = null;

    Class cl = obj.getClass();

    for (; home == null && cl != null; cl = cl.getSuperclass()) {
      home = _amberManager.getHome(cl);
    }

    if (home == null)
      throw new AmberException(L.l("`{0}' is not a known entity class.",
				   obj.getClass().getName()));

    create(home, obj);
  }
  
  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(String homeName, Object obj)
    throws SQLException
  {
    AmberEntityHome home = _amberManager.getEntityHome(homeName);

    if (home == null)
      throw new AmberException(L.l("`{0}' is not a known entity class.",
				   obj.getClass().getName()));

    create(home, obj);
  }
  
  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(AmberEntityHome home, Object obj)
    throws SQLException
  {
    home.save(this, (Entity) obj);

    _txEntities.add((Entity) obj);

    Table table = home.getEntityType().getTable();
    addCompletion(new TableInvalidateCompletion(table.getName()));
  }

  /**
   * Loads the object with the given class.
   */
  public void update(Entity entity)
  {
    Table table = entity.__caucho_getEntityType().getTable();
    
    addCompletion(new RowInvalidateCompletion(table.getName(), entity.__caucho_getPrimaryKey()));

    if (! _txEntities.contains(entity))
      _txEntities.add(entity);
  }
  
  /**
   * Deletes the object.
   *
   * @param obj the object to delete
   */
  public void delete(Object obj)
    throws SQLException
  {
    if (! (obj instanceof Entity))
      return;

    Entity entity = (Entity) obj;

    entity.__caucho_delete();
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
    AbstractQuery queryProgram = parseQuery(queryString, isLazy);
    
    UserQuery query = new UserQuery(queryProgram);
    
    query.setSession(this);

    return query;
  }
  
  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AbstractQuery parseQuery(String queryString, boolean isLazy)
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

    return parser.parse();
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
   * Returns the cache chunk.
   *
   * @param sql the SQL for the cache chunk
   * @param args the filled parameters for the cache chunk
   * @param startRow the starting row for the cache chunk
   */
  public ResultSetCacheChunk getQueryCacheChunk(String sql,
						Object []args,
						int startRow)
  {
    _queryKey.init(sql, args, startRow);
    
    return _amberManager.getQueryChunk(_queryKey);
  }

  /**
   * Sets the cache chunk.
   *
   * @param sql the SQL for the cache chunk
   * @param args the filled parameters for the cache chunk
   * @param startRow the starting row for the cache chunk
   * @param cacheChunk the new value of the cache chunk
   */
  public void putQueryCacheChunk(String sql,
				 Object []args,
				 int startRow,
				 ResultSetCacheChunk cacheChunk)
  {
    QueryCacheKey key = new QueryCacheKey();
    Object []newArgs = new Object[args.length];

    System.arraycopy(args, 0, newArgs, 0, args.length);

    key.init(sql, newArgs, startRow);

    _amberManager.putQueryChunk(key, cacheChunk);
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
    AmberQuery query = prepareUpdate(hsql);

    return query.executeUpdate();
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
    try {
      flush();
    } catch (SQLException e) {
      throw new AmberRuntimeException(e);
    } finally {
      _entities.clear();
      _txEntities.clear();
      _completionList.clear();

      freeConnection();
    }
  }

  /**
   * Pushes the depth.
   */
  public void pushDepth()
  {
  }

  /**
   * Pops the depth.
   */
  public void popDepth()
  {
  }
  
  /**
   * Frees the connection.
   */
  public void freeConnection()
  {
    closeConnectionImpl();
  }
  
  /**
   * Frees the connection.
   */
  private void closeConnectionImpl()
  {
    Connection conn = _conn;
    _conn = null;

    boolean isAutoCommit = _isAutoCommit;
    _isAutoCommit = true;

    try {
      if (conn != null && ! isAutoCommit)
	conn.setAutoCommit(true);
    } catch (SQLException e) {
    }
    
    try {
      _preparedStatementMap.clear();
      _statements.clear();
      
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
    return "AmberConnectionImpl[]";
  }

  /**
   * Finalizer.
   */
  public void finalize()
  {
    cleanup();
  }
}
