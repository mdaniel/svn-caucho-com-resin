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

package com.caucho.v5.env.jpa;

import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.caucho.v5.transaction.ManagedResource;
import com.caucho.v5.transaction.ManagedXAResource;
import com.caucho.v5.transaction.TransactionImpl;
import com.caucho.v5.transaction.UserTransactionProxy;
import com.caucho.v5.util.FreeList;
import com.caucho.v5.util.L10N;

/**
 * The @PersistenceContext, container managed entity manager proxy, used
 * for third-party providers.
 */
@SuppressWarnings("serial")
public class EntityManagerJtaProxy
  implements EntityManager, java.io.Serializable // , HandleAware
{
  private static final L10N L = new L10N(EntityManagerJtaProxy.class);
  private static final Logger log
    = Logger.getLogger(EntityManagerJtaProxy.class.getName());
  
  private final PersistenceUnitManager _persistenceUnit;
  
  private EntityManagerFactory _emf;

  private final UserTransactionProxy _ut;

  private final FreeList<EntityManager> _idleEntityManagerPool
    = new FreeList<EntityManager>(8);

  /*
  private final ThreadLocal<EntityManagerItem> _threadEntityManager
    = new ThreadLocal<EntityManagerItem>();
    */

  private Object _serializationHandle;
  
  public EntityManagerJtaProxy(PersistenceUnitManager pUnit)
  {
    _persistenceUnit = pUnit;
    _ut = UserTransactionProxy.getCurrent();
    
    if (_persistenceUnit == null)
      throw new NullPointerException();
    if (_ut == null)
      throw new NullPointerException();
  }
  
  void init()
  {
  }

  @Override
  public EntityManagerFactory getEntityManagerFactory()
  {
    EntityManager em = getCurrent();
    
    if (em != null)
      return em.getEntityManagerFactory();
    
    em = createEntityManager();
    
    try {
      return em.getEntityManagerFactory();
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public Map<String, Object> getProperties()
  {
    EntityManager em = getCurrent();
    
    if (em != null)
      return em.getProperties();
    
    em = createEntityManager();
    
    try {
      return em.getProperties();
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public void setProperty(String propertyName, Object value)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.setProperty(propertyName, value);
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.setProperty(propertyName, value);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Joins the transaction.
   */
  @Override
  public void joinTransaction()
  {
    throw new IllegalStateException(L.l("Container-manager @PersistenceContext may not use joinTransaction."));
  }

  /**
   * Gets the delegate.
   */
  @Override
  public Object getDelegate()
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em;
    }
    
    em = createEntityManager();
    
    try {
      // XXX: unclear if this should be allowed, because it's put back
      // into the pool
      
      return em;
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Sets the extended type.
   */
  public void setExtended(boolean isExtended)
  {
    throw new IllegalStateException(L.l("Container-managed @PersistenceContext may not be converted to extended"));
  }

  @Override
  public Metamodel getMetamodel()
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.getMetamodel();
    }
    
    em = createEntityManager();
    
    try {
      return em.getMetamodel();
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder()
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.getCriteriaBuilder();
    }
    
    em = createEntityManager();
    
    try {
      return em.getCriteriaBuilder();
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Find by the primary key.
   */
  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      try {
        T value = em.find(entityClass, primaryKey);

        return value;
      } catch (RuntimeException e) {
        throw e;
      }
    }
    
    em = createEntityManager();
    
    try {
      T value = em.find(entityClass, primaryKey);

      return value;
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey,
                    Map<String, Object> properties)
  {
    EntityManager em = getCurrent();
    
    if (em != null)
      return em.find(entityClass, primaryKey, properties);
    
    em = createEntityManager();
    
    try {
      return em.find(entityClass, primaryKey, properties);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey,
                    LockModeType lockMode)
  {
    EntityManager em = getCurrent();
    
    if (em != null)
      return em.find(entityClass, primaryKey, lockMode);
    
    em = createEntityManager();
    
    try {
      return em.find(entityClass, primaryKey, lockMode);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey,
                    LockModeType lockMode, Map<String, Object> properties)
  {
    EntityManager em = getCurrent();
    
    if (em != null)
      return em.find(entityClass, primaryKey, lockMode, properties);
    
    em = createEntityManager();
    
    try {
      return em.find(entityClass, primaryKey, lockMode, properties);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Find by the primary key.
   */
  @Override
  public <T> T getReference(Class<T> entityClass, Object primaryKey)
  {
    EntityManager em = getCurrent();
    
    if (em != null)
      return em.getReference(entityClass, primaryKey);
    
    em = createEntityManager();
    
    try {
      return em.getReference(entityClass, primaryKey);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Makes the instance managed.
   */
  @Override
  public void persist(Object entity)
  {
    EntityManager em = getCurrent();

    if (em != null) {
      em.persist(entity);
      return;
    }

    try {
      if (_ut.getStatus() == Status.STATUS_NO_TRANSACTION)
        throw new TransactionRequiredException(L.l("persist must be called within transaction."));
    } catch (SystemException e) {
      throw new RuntimeException(e);
    }

    em = createEntityManager();
    
    try {
      em.persist(entity);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Merges the state of the entity into the current context.
   */
  @Override
  public <T> T merge(T entity)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.merge(entity);
    }
    
    em = createEntityManager();
    
    try {
      return em.merge(entity);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Remove the instance.
   */
  @Override
  public void remove(Object entity)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.remove(entity);
      return;
    }

    try {
      if (_ut.getStatus() == Status.STATUS_NO_TRANSACTION)
        throw new TransactionRequiredException(L.l("remove must be called within transaction."));
    } catch (SystemException e) {
      throw new RuntimeException(e);
    }

    em = createEntityManager();
    
    try {
      em.remove(entity);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Refresh the state of the instance from the database.
   */
  @Override
  public void refresh(Object entity)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.refresh(entity);
      return;
    }

    try {
      if (_ut.getStatus() == Status.STATUS_NO_TRANSACTION)
        throw new TransactionRequiredException(L.l("refresh must be called within transaction."));
    } catch (SystemException e) {
      throw new RuntimeException(e);
    }

    em = createEntityManager();
    
    try {
      em.refresh(entity);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public void refresh(Object entity, Map<String, Object> properties)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.refresh(entity, properties);
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.refresh(entity, properties);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public void refresh(Object entity, LockModeType lockMode)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.refresh(entity, lockMode);
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.refresh(entity, lockMode);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public void refresh(Object entity, LockModeType lockMode,
                      Map<String, Object> properties)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.refresh(entity, lockMode, properties);
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.refresh(entity, lockMode, properties);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public void detach(Object entity)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.detach(entity);
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.detach(entity);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Returns true if the entity belongs to the current context.
   */
  @Override
  public boolean contains(Object entity)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.contains(entity);
    }
    
    em = createEntityManager();
    
    try {
      return em.contains(entity);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Returns the flush mode.
   */
  @Override
  public FlushModeType getFlushMode()
  {
    EntityManager em = getCurrent();
    
    if (em != null)
      return em.getFlushMode();
    
    em = createEntityManager();
    
    try {
      return em.getFlushMode();
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Sets the flush mode.
   */
  @Override
  public void setFlushMode(FlushModeType mode)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.setFlushMode(mode);
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.setFlushMode(mode);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Synchronize with the database.
   */
  @Override
  public void flush()
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.flush();
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.flush();
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public LockModeType getLockMode(Object entity)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.getLockMode(entity);
    }
    
    em = createEntityManager();
    
    try {
      return em.getLockMode(entity);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Locks the object.
   */
  @Override
  public void lock(Object entity, LockModeType lockMode)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.lock(entity, lockMode);
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.lock(entity, lockMode);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public void lock(Object entity, LockModeType lockMode,
                   Map<String, Object> properties)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      em.lock(entity, lockMode, properties);
      return;
    }
    
    em = createEntityManager();
    
    try {
      em.lock(entity, lockMode, properties);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Creates a query.
   */
  @Override
  public Query createQuery(String sql)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.createQuery(sql);
    }
    
    em = createEntityManager();
    
    try {
      return em.createQuery(sql);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Creates an instance of the named query
   */
  @Override
  public Query createNamedQuery(String sql)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.createNamedQuery(sql);
    }
    
    em = createEntityManager();
    
    try {
      return em.createNamedQuery(sql);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Creates a query.
   */
  @Override
  public Query createNativeQuery(String sql)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.createNativeQuery(sql);
    }
    
    em = createEntityManager();
    
    try {
      return em.createNativeQuery(sql);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Creates a query.
   */
  @Override
  public Query createNativeQuery(String sql, String map)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.createNativeQuery(sql, map);
    }
    
    em = createEntityManager();
    
    try {
      return em.createNativeQuery(sql, map);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Creates a query.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Query createNativeQuery(String sql, Class retType)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.createNativeQuery(sql, retType);
    }
    
    em = createEntityManager();
    
    try {
      return em.createNativeQuery(sql, retType);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.createNamedQuery(name, resultClass);
    }
    
    em = createEntityManager();
    
    try {
      return em.createNamedQuery(name, resultClass);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.createQuery(criteriaQuery);
    }
    
    em = createEntityManager();
    
    try {
      return em.createQuery(criteriaQuery);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public <T> TypedQuery<T> createQuery(String query,
                                       Class<T> resultClass)
  {
    EntityManager em = getCurrent();

    if (em != null) {
      return em.createQuery(query, resultClass);
    }

    em = createEntityManager();

    try {
      return em.createQuery(query, resultClass);
    } finally {
      freeEntityManager(em);
    }
  }

  @Override
  public <T> T unwrap(Class<T> cls)
  {
    EntityManager em = getCurrent();
    
    if (em != null) {
      return em.unwrap(cls);
    }
    
    em = createEntityManager();
    
    try {
      return em.unwrap(cls);
    } finally {
      freeEntityManager(em);
    }
  }

  /**
   * Returns the entity manager transaction.
   */
  @Override
  public EntityTransaction getTransaction()
  {
    throw new IllegalStateException(L.l("Container-manager @PersistenceContext may not use getTransaction."));
  }

  /**
   * Returns true if open.
   */
  @Override
  public boolean isOpen()
  {
    return true;
  }

  /**
   * Clears the manager.
   */
  @Override
  public void clear()
  {
    EntityManager em = getCurrent();

    if (em != null) {
      em.clear();
    }
  }

  /**
   * Clears the manager.
   */
  @Override
  public void close()
  {
    throw new IllegalStateException(L.l("Container-manager @PersistenceContext may not be closed."));
  }
  
  void closeImpl()
  {
    EntityManager em = null;
    
    while ((em = _idleEntityManagerPool.allocate()) != null) {
      em.close();
    }
  }

  /**
   * Returns the current EntityManager.
   */
  private EntityManager getCurrent()
  {
    try {
      TransactionImpl xa = (TransactionImpl) _ut.getTransaction();
      
      if (xa == null)
        return null;
      
      EntityManagerItem item;
      
      item = (EntityManagerItem) xa.getAttribute("resin.env.jpa.EntityManagerItem");
      
      if (item != null) {
        return item.getEntityManager();
      }

      if (_emf == null) {
        _emf = _persistenceUnit.getEntityManagerFactoryDelegate();
        
        if (_emf == null)
          throw new IllegalStateException(L.l("{0}: EntityManagerFactory cannot be found from {1}",
                                              this, _persistenceUnit));
      }

      EntityManager em;
      
      if (xa != null && xa.getStatus() == Status.STATUS_ACTIVE) {
        em = _emf.createEntityManager(_persistenceUnit.getProperties());

        item = new EntityManagerItem(item, em, xa);

        xa.setAttribute("resin.env.jpa.EntityManagerItem", item);
        
        // _threadEntityManager.set(item);

        xa.registerSynchronization(item);

        return em;
      }

      /*
      // env/0e70
      UserTransactionImpl ut = _ut.getCurrentUserTransactionImpl();

      if (ut != null && ut.isInContext()) {
        em = _emf.createEntityManager(_persistenceUnit.getProperties());

        item = new EntityManagerItem(null, em, xa);
        
        _threadEntityManager.set(item);

        ut.enlistResource(item);

        return em;
      }
      */
      
      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Creates a transienct EM for outside a transaction.
   */
  private EntityManager createEntityManager()
  {
    EntityManager em = _idleEntityManagerPool.allocate();
    
    if (em == null) {
      if (_emf == null) {
        _emf = _persistenceUnit.getEntityManagerFactoryDelegate();
      }
      
      if (_emf == null)
        throw new IllegalStateException(L.l("{0} does not have a valid delegate.",
                                            _persistenceUnit));
      
      em = _emf.createEntityManager(_persistenceUnit.getProperties());
    }
    
    return em;
  }
  
  private void freeEntityManager(EntityManager em)
  {
    em.clear();

    if (! em.isOpen() || ! _idleEntityManagerPool.free(em)) {
      em.close();
    }
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
    return getClass().getSimpleName() + "[" + _persistenceUnit + "]";
  }

  class EntityManagerItem implements Synchronization, ManagedResource {
    private final EntityManagerItem _prev;
    
    private final EntityManager _em;
    private final Transaction _xa;
    
    EntityManagerItem(EntityManagerItem prev,
                      EntityManager em,
                      Transaction xa)
    {
      _prev = prev;
      _em = em;
      _xa = xa;
    }
    
    EntityManager getEntityManager()
    {
      return _em;
    }
    
    Transaction getXa()
    {
      return _xa;
    }
    
    EntityManagerItem getPrev()
    {
      return _prev;
    }

    @Override
    public void beforeCompletion()
    {
    }

    @Override
    public void afterCompletion(int status)
    {
      close();
    }

    public void close()
    {
      // _threadEntityManager.set(_prev);

      freeEntityManager(_em);
    }

    @Override
    public void abortConnection()
    {
      close();
    }

    @Override
    public IllegalStateException getAllocationStackTrace()
    {
      return null;
    }

    @Override
    public Object getUserConnection()
    {
      return EntityManagerJtaProxy.this;
    }

    @Override
    public ManagedXAResource getXAResource()
    {
      return null;
    }

    @Override
    public boolean isCloseDanglingConnections()
    {
      return false;
    }

    @Override
    public void setSaveAllocationStackTrace(boolean isEnable)
    {

    }
  }
}
