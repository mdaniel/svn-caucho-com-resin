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
package com.caucho.amber.ejb3;

import java.sql.Connection;

import javax.ejb.EntityManager;
import javax.ejb.Query;

import com.caucho.amber.AmberManager;
import com.caucho.amber.EnvAmberManager;

import com.caucho.amber.connection.AmberConnectionImpl;

import com.caucho.amber.query.AbstractQuery;

import com.caucho.ejb.EJBExceptionWrapper;

import com.caucho.jca.UserTransactionProxy;
import com.caucho.jca.CloseResource;

import com.caucho.util.L10N;

/**
 * The entity manager for the current thread.
 */
public class EntityManagerImpl implements EntityManager, CloseResource {
  private static final L10N L = new L10N(EntityManagerImpl.class);

  private EntityManagerProxy _entityManagerProxy;
  
  private AmberManager _amberManager;

  private boolean _isRegistered;
  private AmberConnectionImpl _aConn;

  /**
   * Creates a manager instance.
   */
  EntityManagerImpl(AmberManager amberManager, EntityManagerProxy proxy)
  {
    _amberManager = amberManager;
    _entityManagerProxy = proxy;
  }
  
  /**
   * Makes the instance managed.
   */
  public void persist(Object entity)
  {
    try {
      AmberConnectionImpl aConn = getAmberConnection();
    
      aConn.create(entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }
  
  /**
   * Merges the state of the entity into the current context.
   */
  public <T> T merge(T entity)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Remove the instance.
   */
  public void remove(Object entity)
  {
    try {
      AmberConnectionImpl aConn = getAmberConnection();
    
      aConn.delete(entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }
  
  /**
   * Find by the primary key.
   */
  public Object find(String entityName, Object primaryKey)
  {
    try {
      AmberConnectionImpl aConn = getAmberConnection();

      return aConn.load(entityName, primaryKey);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }
  
  /**
   * Find by the primary key.
   */
  public <T> T find(Class<T> entityClass, Object primaryKey)
  {
    try {
      AmberConnectionImpl aConn = getAmberConnection();
    
      return (T) aConn.load(entityClass, primaryKey);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Synchronize with the database.
   */
  public void flush()
  {
    try {
      AmberConnectionImpl aConn = getAmberConnection();
    
      aConn.flush();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates a query.
   */
  public Query createQuery(String sql)
  {
    try {
      AmberConnectionImpl aConn = getAmberConnection();

      AbstractQuery queryProgram = aConn.parseQuery(sql, false);
      
      return new QueryImpl(queryProgram, aConn);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNamedQuery(String sql)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNativeQuery(String sql)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNativeQuery(String sql, String map)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNativeQuery(String sql, Class type)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Refresh the state of the instance from the database.
   */
  public void refresh(Object entity)
  {
  }

  /**
   * Returns true if the entity belongs to the current context.
   */
  public boolean contains(Object entity)
  {
    try {
      AmberConnectionImpl aConn = getAmberConnection();
    
      return aConn.contains(entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Returns the current amber connection.
   */
  private AmberConnectionImpl getAmberConnection()
  {
    if (_aConn == null) {
      /*
      if (_depth == 0)
	throw new IllegalStateException(L.l("EntityContext required when using Entity beans."));
      */

      _aConn = _amberManager.createAmberConnection();
    }

    return _aConn;
  }

  /**
   * Registers with the local transaction.
   */
  void register()
  {
    if (! _isRegistered)
      UserTransactionProxy.getInstance().enlistCloseResource(this);
    
    _isRegistered = true;
  }

  /**
   * Closes the context.
   */
  public void close()
  {
    AmberConnectionImpl aConn = _aConn;
    _aConn = null;
    _isRegistered = false;

    if (aConn != null)
      aConn.cleanup();

    _entityManagerProxy.unset(this);
  }
}
