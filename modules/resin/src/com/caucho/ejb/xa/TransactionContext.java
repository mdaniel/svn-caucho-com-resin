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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.xa;

import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.Entity;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.entity.EntityServer;
import com.caucho.ejb.entity.QEntity;
import com.caucho.log.Log;
import com.caucho.transaction.TransactionImpl;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

import javax.ejb.EJBException;
import javax.ejb.SessionSynchronization;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.UserTransaction;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles entity beans for a single transaction.
 */
public class TransactionContext implements Synchronization {
  static final L10N L = new L10N(TransactionContext.class);
  protected static final Logger log = Log.open(TransactionContext.class);

  public static final TransactionContext NULL_TRANSACTION =
    new TransactionContext(null);

  private EjbTransactionManager _container;
  private AmberConnection _amberConn;

  private UserTransaction _userTransaction;
  private Transaction _transaction;

  // the previous TransactionContext when this one completes
  TransactionContext _old;
  // the previous Transaction when this one completes
  Transaction _oldTrans;

  private long _startTime;

  private TransactionObject []_objects = new TransactionObject[16];
  private int _objectTop;

  private SessionSynchronization []_sessions = new SessionSynchronization[16];
  private int _sessionTop;

  private boolean _isRowLocking;
  private boolean _rollbackOnly;

  private boolean _isUserTransaction;
  private int _depth;
  private boolean _isAlive;
  private boolean _isCommitting;

  TransactionContext(EjbTransactionManager container)
  {
    _container = container;

    if (container != null)
      _userTransaction = container.getUserTransaction();
  }

  void init(boolean pushDepth)
  {
    if (_isAlive) {
      log.warning(L.l("Transaction {0} nested start.  This is an internal Resin error, please report it as a bug at bugs@caucho.com.", this));

      throw new IllegalStateException(L.l("nested transaction start"));
    }

    _transaction = null;
    _old = null;
    _oldTrans = null;
    _objectTop = 0;
    _sessionTop = 0;
    _rollbackOnly = false;
    _depth = pushDepth ? 1 : 0;
    _isUserTransaction = false;
    _isAlive = true;
    _isRowLocking = false;
    _startTime = Alarm.getCurrentTime();
  }

  public void setTransaction(Transaction transaction)
  {
    if (_transaction != null && _transaction != transaction)
      throw new IllegalStateException("can't set transaction twice.");
    _transaction = transaction;

    if (transaction != null) {
      try {
        transaction.registerSynchronization(this);
      } catch (Exception e) {
        throw new EJBExceptionWrapper(e);
      }
    }
  }

  public Transaction getTransaction()
  {
    return _transaction;
  }

  /**
   * Returns true for a read-only transaction.
   */
  public boolean isReadOnly()
  {
    return _transaction == null;
  }

  /**
   * Returns true for a row-locking transaction.
   */
  public boolean isRowLocking()
  {
    return _transaction != null && _isRowLocking;
  }

  /**
   * Set true for a row-locking transaction.
   */
  public void setRowLocking(boolean isRowLocking)
  {
    _isRowLocking = isRowLocking;
  }

  TransactionContext getOld()
  {
    return _old;
  }

  void setOld(TransactionContext old)
  {
    _old = old;
  }

  Transaction getOldTrans()
  {
    return _oldTrans;
  }

  void setOldTrans(Transaction old)
  {
    if (old == _transaction && old != null)
      throw new IllegalStateException();

    _oldTrans = old;
  }

  public void pushDepth()
  {
    if (_depth == 0) {
      _isAlive = true;
    }

    _depth++;
  }

  public void setUserTransaction(boolean isUserTransaction)
  {
    if (_depth == 0)
      _isAlive = true;

    _isUserTransaction = isUserTransaction;
  }

  /**
   * Returns true if the transaction must rollback.
   */
  public boolean getRollbackOnly()
  {
    return _rollbackOnly;
  }

  /**
   * Forces the transaction to rollback.
   */
  public void setRollbackOnly()
  {
    _rollbackOnly = true;

    if (_transaction != null) {
      try {
        _transaction.setRollbackOnly();
      } catch (Exception e) {
        throw new EJBExceptionWrapper(e);
      }
    }
  }

  /**
   * Forces the transaction to rollback.
   */
  public RuntimeException setRollbackOnly(Throwable exn)
  {
    _rollbackOnly = true;

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, "rollback only: " + exn, exn);

    if (_transaction != null) {
      try {
        if (_transaction instanceof TransactionImpl)
          ((TransactionImpl) _transaction).setRollbackOnly(exn);
        else
          _transaction.setRollbackOnly();
      } catch (Exception e) {
        return EJBExceptionWrapper.createRuntime(exn);
      }
    }

    return EJBExceptionWrapper.createRuntime(exn);
  }

  /**
   * Add a new persistent object to the transaction context.
   *
   * @param object the new persistent object to be managed
   */
  public void addObject(TransactionObject object)
  {
    if (_objects.length <= _objectTop + 1) {
      TransactionObject []newObjects;
      newObjects = new TransactionObject[_objects.length * 2];
      for (int i = 0; i < _objectTop; i++)
        newObjects[i] = _objects[i];
      _objects = newObjects;
    }

    _objects[_objectTop++] = object;
  }

  /**
   * Remove a transaction object from the transaction context.
   *
   * @param object the transaction object to be removed
   */
  public void removeObject(TransactionObject object)
  {
    for (int i = 0; i < _objectTop; i++) {
      if (_objects[i] == object) {
        for (int j = i; j + 1 < _objectTop; j++)
          _objects[j] = _objects[j + 1];
        i--;
        _objectTop--;
      }
    }
  }

  /**
   * Returns the matching object.
   */
  public QEntity getEntity(EntityServer server, Object primaryKey)
  {
    for (int i = _objectTop - 1; i >= 0; i--) {
      TransactionObject obj = _objects[i];

      if (obj instanceof QEntity) {
        QEntity entity = (QEntity) obj;

        if (entity._caucho_isMatch(server, primaryKey)) {
          return entity;
        }
      }
    }

    return null;
  }

  public Entity getAmberEntity(EntityServer server, Object key)
  {
    QEntity entity = getEntity(server, key);

    if (entity != null)
      return (Entity) entity;

    AmberEntityHome entityHome = server.getAmberEntityHome();
    Class cl = entityHome.getRootType().getInstanceClass();

    return getAmberConnection().getEntity(cl, key);
  }

  public void addAmberEntity(EntityServer server, Entity entity)
  {
    try {
      AmberEntityHome entityHome = server.getAmberEntityHome();

      entity.__caucho_makePersistent(getAmberConnection(),
                                     entityHome.getEntityType());

      addObject((TransactionObject) entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the amber connection.
   */
  public AmberConnection getAmberConnection()
  {
    if (_amberConn == null) {
      _amberConn = _container.getEjbContainer().createEjbPersistenceUnit().getThreadConnection(false);
    }

    try {
      _amberConn.setXA(_transaction != null);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return _amberConn;
  }

  /**
   * Add a session to the transaction context.
   *
   * @param session the new session to be managed
   */
  public void addSession(SessionSynchronization session)
    throws EJBException
  {
    for (int i = _sessionTop - 1; i >= 0; i--)
      if (_sessions[i] == session)
        return;

    if (_sessionTop + 1 >= _sessions.length) {
      SessionSynchronization []newSessions;
      newSessions = new SessionSynchronization[_sessions.length * 2];

      for (int i = 0; i < _sessionTop; i++)
        newSessions[i] = _sessions[i];
      _sessions = newSessions;
    }

    _sessions[_sessionTop++] = session;
    try {
      session.afterBegin();
    } catch (RemoteException e) {
      EJBException exn = new EJBException(e.toString());
      exn.initCause(e);
      throw exn;
    }
  }

  /**
   * Synchronizes the objects in the context.
   */
  public void sync()
    throws EJBException
  {
    try {
      for (int i = _objectTop - 1; i >= 0; i--) {
        _objects[i]._caucho_sync();
      }
    } catch (Exception e) {
      throw setRollbackOnly(e);
    }
  }

  /**
   * Returns true if the transaction isn't used.
   */
  public boolean isEmpty()
  {
    if (! _isAlive)
      return true;
    else if (_transaction == null)
      return true;
    else if (! (_transaction instanceof TransactionImpl))
      return false;
    else
      return ((TransactionImpl) _transaction).isEmpty();
  }

  /**
   * Commit the transaction
   */
  public void commit()
    throws EJBException
  {
    if (_isCommitting || --_depth > 0)
      return;

    boolean hasCompletion = false;
    try {
      _isCommitting = true;

      if (! _isAlive) {
        log.warning(L.l("Transaction has died"));
      }
      else if (_transaction == null) {
        hasCompletion = true;
        try {
          beforeCompletion();
        } finally {
          afterCompletion(_rollbackOnly ?
                          Status.STATUS_ROLLEDBACK :
                          Status.STATUS_COMMITTED);
        }
      }
      else if (_rollbackOnly
               || _transaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
        hasCompletion = true;
        _userTransaction.rollback();
      }
      else if (_transaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
        hasCompletion = true;
        _userTransaction.commit();
      }
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    } finally {
      _isCommitting = false;

      if (! hasCompletion)
        afterCompletion(Status.STATUS_ROLLEDBACK);
    }
  }

  /**
   * Rollback the transaction (help?)
   */
  public void rollback()
    throws EJBException
  {
    if (_isCommitting || --_depth > 0)
      return;

    try {
      _isCommitting = true;

      if (! _rollbackOnly)
        setRollbackOnly();

      if (_transaction == null && _isAlive) {
        try {
          beforeCompletion();
        } finally {
          afterCompletion(Status.STATUS_ROLLEDBACK);
        }
      }
      else
        _userTransaction.rollback();
    } catch (EJBException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    } finally {
      _isCommitting = false;

      if (_depth <= 0 && _transaction != null)
        afterCompletion(Status.STATUS_ROLLEDBACK);
    }
  }

  /**
   * Called by the transaction before the transaction completes.
   */
  public void beforeCompletion()
  {
    try {
      if (_transaction != null &&
          _transaction.getStatus() == Status.STATUS_MARKED_ROLLBACK)
        _rollbackOnly = true;

      for (int i = _sessionTop - 1; i >= 0; i--)
        _sessions[i].beforeCompletion();

      for (int i = _objectTop - 1; i >= 0; i--) {
        _objects[i]._caucho_beforeCompletion(! _rollbackOnly);
      }

      // ejb/0600: com.caucho.transaction.TransactionImpl
      // will call _amberConn.beforeCompletion() which
      // already calls beforeCommit().
      //
      // if (_amberConn != null)
      //   _amberConn.beforeCommit();
    } catch (Throwable e) {
      throw setRollbackOnly(e);
    }
  }

  public boolean isDead()
  {
    return ! _isAlive;
  }

  /**
   * After the transaction completes, do any extra work.
   */
  public void afterCompletion(int status)
  {
    /*
      if (! _isUserTransaction && _depth > 0) {
      return;
      }
    */

    if (! _isAlive) {
      IllegalStateException e = new IllegalStateException("after completion called for dead transaction.");
      log.log(Level.WARNING, e.toString(), e);
      return;
    }

    boolean wasCommitted = status == Status.STATUS_COMMITTED;
    int sessionTop = _sessionTop;
    int objectTop = _objectTop;
    TransactionContext old = _old;
    Transaction transaction = _transaction;
    Transaction oldTrans = _oldTrans;

    _sessionTop = 0;
    _objectTop = 0;
    _old = null;
    if (oldTrans == transaction)
      oldTrans = null;
    _oldTrans = null;
    _rollbackOnly = false;

    Throwable exn = null;

    try {
      AmberConnection amberConn = _amberConn;
      _amberConn = null;
      if (amberConn != null) {
        amberConn.afterCommit(wasCommitted);
        amberConn.freeConnection();
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    for (int i = sessionTop - 1; i >= 0; i--) {
      try {
        _sessions[i].afterCompletion(wasCommitted);
        _sessions[i] = null;
      } catch (Throwable e) {
        exn = e;
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    for (int i = objectTop - 1; i >= 0; i--) {
      try {
        _objects[i]._caucho_afterCompletion(wasCommitted);
        _objects[i] = null;
      } catch (Throwable e) {
        exn = e;
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    _transaction = null;
    _isAlive = false;

    if (_depth == 0 || _isUserTransaction && _depth == 1) {
      _container.resume(old, oldTrans, transaction);
      _container.freeTransaction(this);
    }

    if (exn != null)
      throw EJBExceptionWrapper.createRuntime(exn);
  }
}
