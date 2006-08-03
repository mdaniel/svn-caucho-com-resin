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

package com.caucho.amber.manager;

import java.sql.Connection;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.LockModeType;
import javax.persistence.FlushModeType;

import com.caucho.util.L10N;

import com.caucho.amber.manager.AmberPersistenceUnit;

/**
 * The Entity manager
 */
public class EntityManagerProxy implements EntityManager {
  private static final L10N L = new L10N(EntityManagerProxy.class);
  private static final Logger log
    = Logger.getLogger(EntityManagerProxy.class.getName());

  private AmberPersistenceUnit _persistenceUnit;

  public EntityManagerProxy(AmberPersistenceUnit persistenceUnit)
  {
    _persistenceUnit = persistenceUnit;
  }

  /**
   * Makes the instance managed.
   */
  public void persist(Object entity)
  {
    getCurrent().persist(entity);
  }

  /**
   * Merges the state of the entity into the current context.
   */
  public <T> T merge(T entity)
  {
    return getCurrent().merge(entity);
  }

  /**
   * Remove the instance.
   */
  public void remove(Object entity)
  {
    getCurrent().remove(entity);
  }

  /**
   * Find by the primary key.
   */
  /*
    public Object find(String entityName, Object primaryKey)
    {
    return getCurrent().find(entityName, primaryKey);
    }
  */

  /**
   * Find by the primary key.
   */
  public <T> T find(Class<T> entityClass, Object primaryKey)
  {
    return getCurrent().find(entityClass, primaryKey);
  }

  /**
   * Find by the primary key.
   */
  public <T> T getReference(Class<T> entityClass, Object primaryKey)
  {
    return getCurrent().getReference(entityClass, primaryKey);
  }

  /**
   * Returns the flush mode.
   */
  public FlushModeType getFlushMode()
  {
    return getCurrent().getFlushMode();
  }

  /**
   * Returns the flush mode.
   */
  public void setFlushMode(FlushModeType mode)
  {
    getCurrent().setFlushMode(mode);
  }

  /**
   * Locks the object.
   */
  public void lock(Object entity, LockModeType lockMode)
  {
    getCurrent().lock(entity, lockMode);
  }

  /**
   * Clears the manager.
   */
  public void clear()
  {
    getCurrent().clear();
  }

  /**
   * Synchronize with the database.
   */
  public void flush()
  {
    getCurrent().flush();
  }

  /**
   * Clears the manager.
   */
  public void close()
  {
    throw new IllegalStateException(L.l("Container-manager persistence context may not be closed."));
  }

  /**
   * Creates a query.
   */
  public Query createQuery(String sql)
  {
    return getCurrent().createQuery(sql);
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNamedQuery(String sql)
  {
    return getCurrent().createNamedQuery(sql);
  }

  /**
   * Creates a query.
   */
  public Query createNativeQuery(String sql)
  {
    return getCurrent().createNativeQuery(sql);
  }

  /**
   * Creates a query.
   */
  public Query createNativeQuery(String sql, String map)
  {
    return getCurrent().createNativeQuery(sql, map);
  }

  /**
   * Creates a query.
   */
  public Query createNativeQuery(String sql, Class retType)
  {
    return getCurrent().createNativeQuery(sql, retType);
  }

  /**
   * Refresh the state of the instance from the database.
   */
  public void refresh(Object entity)
  {
    getCurrent().refresh(entity);
  }

  /**
   * Returns true if the entity belongs to the current context.
   */
  public boolean contains(Object entity)
  {
    return getCurrent().contains(entity);
  }

  /**
   * Returns the entity manager transaction.
   */
  public EntityTransaction getTransaction()
  {
    return getCurrent().getTransaction();
  }

  /**
   * Returns true if open.
   */
  public boolean isOpen()
  {
    return true;
  }

  /**
   * Returns the current entity manager.
   */
  private AmberConnection getCurrent()
  {
    return _persistenceUnit.getThreadConnection();
  }

  public String toString()
  {
    AmberConnection aConn = getCurrent();

    if (aConn != null)
      return "EntityManagerProxy[" + aConn.getPersistenceUnit().getName() + "]";
    else
      return "EntityManagerProxy[closed]";
  }
}
