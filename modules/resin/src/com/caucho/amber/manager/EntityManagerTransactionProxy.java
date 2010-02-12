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

package com.caucho.amber.manager;

import com.caucho.amber.*;
import com.caucho.config.inject.HandleAware;
import com.caucho.jca.*;
import com.caucho.util.L10N;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;
import javax.transaction.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * The @PersistenceContext, container managed entity manager proxy, used
 * for third-party providers.
 */
public class EntityManagerTransactionProxy
  implements EntityManager, java.io.Serializable, HandleAware
{
  private static final L10N L = new L10N(EntityManagerTransactionProxy.class);
  private static final Logger log
    = Logger.getLogger(EntityManagerTransactionProxy.class.getName());

  private final AmberContainer _amber;
  private final String _unitName;
  private final Map _props;
  private EntityManagerFactory _emf;

  private final UserTransactionProxy _ut;

  private final ThreadLocal<EntityManager> _threadEntityManager
    = new ThreadLocal<EntityManager>();

  private Object _serializationHandle;

  public EntityManagerTransactionProxy(AmberContainer amber,
                                       String unitName,
                                       Map props)
  {
    _amber = amber;
    _unitName = unitName;
    _props = props;

    _ut = UserTransactionProxy.getCurrent();
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
   * Sets the flush mode.
   */
  public void setFlushMode(FlushModeType mode)
  {
    getCurrent().setFlushMode(mode);
  }

  /**
   * Sets the extended type.
   */
  public void setExtended(boolean isExtended)
  {
    throw new IllegalStateException(L.l("Container-managed @PersistenceContext may not be converted to extended"));
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
   * Joins the transaction.
   */
  public void joinTransaction()
  {
    throw new IllegalStateException(L.l("Container-manager @PersistenceContext may not use joinTransaction."));
  }

  /**
   * Gets the delegate.
   */
  public Object getDelegate()
  {
    return getCurrent();
  }

  /**
   * Clears the manager.
   */
  public void close()
  {
    throw new IllegalStateException(L.l("Container-manager @PersistenceContext may not be closed."));
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
    throw new IllegalStateException(L.l("Container-manager @PersistenceContext may not use getTransaction."));
  }

  /**
   * Returns true if open.
   */
  public boolean isOpen()
  {
    return true;
  }

  /**
   * Returns the current EntityManager.
   */
  private EntityManager getCurrent()
  {
    EntityManager em = _threadEntityManager.get();

    if (em != null)
      return em;

    try {
      if (_emf == null) {
        _emf = _amber.getEntityManagerFactory(_unitName);
      }

      Transaction xa = _ut.getTransaction();

      if (xa != null) {
        em = _emf.createEntityManager(_props);

        _threadEntityManager.set(em);

        xa.registerSynchronization(new EntityManagerSynchronization(em));

        return em;
      }

      UserTransactionImpl ut = _ut.getCurrentUserTransactionImpl();

      if (ut != null && ut.isInContext()) {
        em = _emf.createEntityManager(_props);

        _threadEntityManager.set(em);

        ut.enlistCloseResource(new EntityManagerCloseResource(em));

        return em;
      }

      throw new IllegalStateException(L.l("{0}: @PersistenceContext EntityManager may not be used outside of a transaction",
                                          this));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  private EntityManagerFactory getFactory()
  {
    if (_emf == null) {
      _emf = _amber.getEntityManagerFactory(_unitName);
    }

    return _emf;
  }

  /**
   * Serialization handle
   */
  public void setSerializationHandle(Object handle)
  {
    _serializationHandle = handle;
  }

  /**
   * Serialize to the handle.
   */
  private Object writeReplace()
  {
    return _serializationHandle;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _unitName + "," + getFactory() + "]";
  }

  class EntityManagerSynchronization implements Synchronization {
    private EntityManager _em;

    EntityManagerSynchronization(EntityManager em)
    {
      _em = em;
    }

    public void beforeCompletion()
    {
    }

    public void afterCompletion(int status)
    {
      _threadEntityManager.set(null);

      _em.close();
    }
  }

  class EntityManagerCloseResource implements CloseResource {
    private EntityManager _em;

    EntityManagerCloseResource(EntityManager em)
    {
      _em = em;
    }

    public void close()
    {
      _threadEntityManager.set(null);

      _em.close();
    }
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#createNamedQuery(java.lang.String, java.lang.Class)
   */
  @Override
  public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#createQuery(javax.persistence.criteria.CriteriaQuery)
   */
  @Override
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#createQuery(javax.persistence.criteria.CriteriaQuery, java.lang.Class)
   */
  @Override
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery,
                                       Class<T> resultClass)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#detach(java.lang.Object)
   */
  @Override
  public void detach(Object entity)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, java.util.Map)
   */
  @Override
  public <T> T find(Class<T> entityCLass, Object primaryKey,
                    Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, javax.persistence.LockModeType)
   */
  @Override
  public <T> T find(Class<T> entityCLass, Object primaryKey,
                    LockModeType lockMode)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, javax.persistence.LockModeType, java.util.Map)
   */
  @Override
  public <T> T find(Class<T> entityCLass, Object primaryKey,
                    LockModeType lockMode, Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getCriteriaBuilder()
   */
  @Override
  public CriteriaBuilder getCriteriaBuilder()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getEntityManagerFactory()
   */
  @Override
  public EntityManagerFactory getEntityManagerFactory()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getLockMode(java.lang.Object)
   */
  @Override
  public LockModeType getLockMode(Object entity)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getMetamodel()
   */
  @Override
  public Metamodel getMetamodel()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getProperties()
   */
  @Override
  public Map<String, Object> getProperties()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getSupportedProperties()
   */
  @Override
  public Set<String> getSupportedProperties()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#lock(java.lang.Object, javax.persistence.LockModeType, java.util.Map)
   */
  @Override
  public void lock(Object entity, LockModeType lockMode,
                   Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#refresh(java.lang.Object, java.util.Map)
   */
  @Override
  public void refresh(Object entity, Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#refresh(java.lang.Object, javax.persistence.LockModeType)
   */
  @Override
  public void refresh(Object entity, LockModeType lockMode)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#refresh(java.lang.Object, javax.persistence.LockModeType, java.util.Map)
   */
  @Override
  public void refresh(Object entity, LockModeType lockMode,
                      Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#setProperty(java.lang.String, java.lang.Object)
   */
  @Override
  public void setProperty(String propertyName, Object value)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#unwrap(java.lang.Class)
   */
  @Override
  public <T> T unwrap(Class<T> cls)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
