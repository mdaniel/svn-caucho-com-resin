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

package com.caucho.transaction;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.UserTransaction;
import javax.transaction.xa.Xid;

import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * Implementation of the UserTransactionImpl for a thread instance.
 */
public class UserTransactionImpl
  implements UserTransaction
{
  private static final Logger log
    = Logger.getLogger(UserTransactionImpl.class.getName());
  private static final L10N L = new L10N(UserTransactionImpl.class);

  private TransactionManagerImpl _transactionManager;

  private ArrayList<ManagedResource> _resources
    = new ArrayList<ManagedResource>();
  private ArrayList<ManagedXAResource> _xaResources
    = new ArrayList<ManagedXAResource>();

  private boolean _isInContext;
  private boolean _isTransactionActive;
  
  /**
   * Creates the proxy.
  */
  public UserTransactionImpl(TransactionManagerImpl tm)
  {
    _transactionManager = tm;
  }
  
  /**
   * Sets the transaction's timeout.
   */
  @Override
  public void setTransactionTimeout(int seconds)
    throws SystemException
  {
    _transactionManager.setTransactionTimeout(seconds);
  }
  
  /**
   * Gets the transaction's status
   */
  @Override
  public int getStatus()
    throws SystemException
  {
    return _transactionManager.getStatus();
  }

  /**
   * inContext is valid within a managed UserTransactionImpl context, e.g
   * in a webApp, but not in a cron job.
   */
  public boolean isInContext()
  {
    return _isInContext;
  }

  /**
   * inContext is valid within a managed UserTransactionImpl context, e.g
   * in a webApp, but not in a cron job.
   */
  public void setInContext(boolean isInContext)
  {
    _isInContext = isInContext;
  }
  
  public boolean isActive()
  {
    return _isTransactionActive;
  }

  /**
   * Enlist a resource.
   */
  public void enlistResource(ManagedResource resource)
    throws SystemException, RollbackException
  {
    if (resource == null)
      throw new NullPointerException();
    
    if (_resources.contains(resource))
      return;
    
    TransactionImpl xa = _transactionManager.getTransaction();
    if (xa != null && xa.isActive()) {
      ManagedXAResource xaResource = resource.getXAResource();

      if (xaResource != null)
        enlistXaResource(xa, xaResource);
    }
    
    _resources.add(resource);
  }

  private void enlistXaResource(Transaction xa,
                                ManagedXAResource xaResource)
    throws SystemException, RollbackException
  {
    if (xaResource == null)
      return;
    else if (! xaResource.supportsTransaction()) {
      // server/164j
      return;
    }
    
    if (_xaResources.contains(xaResource))
      return;
    
    xaResource.setTransaction(this);

    if (xa instanceof TransactionImpl) {
      TransactionImpl xaImpl = (TransactionImpl) xa;
      
      // server/164l
      if (xaImpl.allowLocalTransactionOptimization())
        xaResource.enableLocalTransactionOptimization(true);
    }

    if (xaResource.getXid() == null)
      xa.enlistResource(xaResource);
    
    _xaResources.add(xaResource);
  }

  /**
   * Delist a resource.
   */
  public void delistResource(ManagedResource resource)
  {
    _resources.remove(resource);
  }
  
  @Module
  public ArrayList<ManagedXAResource> getXaResources()
  {
    return _xaResources;
  }

  /**
   * Returns the XID.
   */
  public Xid getXid()
    throws SystemException, RollbackException
  {
    TransactionImpl xa = (TransactionImpl) _transactionManager.getTransaction();

    if (xa != null)
      return xa.getXid();
    else
      return null;
  }

  /**
   * Returns the number of currently enlisted resources.
   */
  public int getEnlistedResourceCount()
    throws SystemException, RollbackException
  {
    TransactionImpl xa = (TransactionImpl) _transactionManager.getTransaction();

    if (xa != null)
      return xa.getEnlistedResourceCount();
    else
      return 0;
  }
  
  /**
   * Start the transaction.
   */
  @Override
  public void begin()
    throws NotSupportedException, SystemException
  {
    if (_isTransactionActive)
      throw new NotSupportedException(L.l("UserTransaction.begin() is not allowed because an active transaction already exists.  This may be caused by either a missing commit/rollback or a nested begin().  Nested transactions are not supported."));
    
    _transactionManager.begin();
    _isTransactionActive = true;
    boolean isOkay = false;

    try {
      TransactionImpl xa = (TransactionImpl) _transactionManager.getTransaction();
      xa.setUserTransaction(this);
    
      _xaResources.clear();
    
      // enlist "cached" resources
      int length = _resources.size();

      for (int i = 0; i < length; i++) {
        ManagedResource resource = _resources.get(i);

        for (int j = _xaResources.size() - 1; j >= 0; j--) {
          ManagedXAResource xaResource = _xaResources.get(j);

          if (xaResource.share(resource)) {
            break;
          }
        }

        ManagedXAResource xaResource = resource.getXAResource();
        if (xaResource != null && ! _xaResources.contains(xaResource))
          _xaResources.add(xaResource);
      }

      for (int i = 0; i < _xaResources.size(); i++) {
        ManagedXAResource xaResource = _xaResources.get(i);

        xaResource.enableLocalTransactionOptimization(_xaResources.size() == 1);

        try {
          xa.enlistResource(xaResource);
        } catch (Exception e) {
          String message = L.l("Failed to begin UserTransaction due to: {0}", e);
          log.log(Level.SEVERE, message, e);

          throw new SystemException(message);
        }
      }

      isOkay = true;
    } finally {
      if (! isOkay) {
        Exception e1 = new IllegalStateException(L.l("Rolling back transaction from failed begin."));
        e1.fillInStackTrace();
        log.log(Level.WARNING, e1.toString(), e1);

        // something has gone very wrong
        _isTransactionActive = false;

        ArrayList<ManagedXAResource> xaResources
          = new ArrayList<ManagedXAResource>(_xaResources);
        _xaResources.clear();
        
        // XXX: need to free _resources as well
        _resources.clear();

        for (int i = 0; i < xaResources.size(); i++) {
          try {
            ManagedXAResource item = xaResources.get(i);

            item.abortConnection();

            item.destroy();
          } catch (Throwable e) {
            log.log(Level.FINE, e.toString(), e);
          }
        }

        _transactionManager.rollback();
      }
    }
  }

  /**
   * Suspends the transaction.
   */
  public UserTransactionSuspendState userSuspend()
  {
    if (! _isTransactionActive)
      throw new IllegalStateException(L.l("UserTransaction.suspend may only be called in a transaction, but no transaction is active."));

    _isTransactionActive = false;
    
    UserTransactionSuspendState state;
    state = new UserTransactionSuspendState(_xaResources);
    _xaResources.clear();

    return state;
  }

  /**
   * Resumes the transaction.
   */
  public void userResume(UserTransactionSuspendState state)
  {
    if (_isTransactionActive)
      throw new IllegalStateException(L.l("UserTransaction.resume may only be called outside of a transaction, because the resumed transaction must not conflict with an active transaction."));

    _isTransactionActive = true;

    _xaResources.addAll(state.getXAResources());
  }
  
  /**
   * Marks the transaction as rollback only.
   */
  @Override
  public void setRollbackOnly()
    throws IllegalStateException, SystemException
  {
    _transactionManager.setRollbackOnly();
  }
  
  /**
   * Marks the transaction as rollback only.
   */
  public void setRollbackOnly(Exception e)
    throws IllegalStateException
  {
    _transactionManager.setRollbackOnly(e);
  }
  
  /**
   * Commits the transaction
   */
  @Override
  public void commit()
    throws IllegalStateException, RollbackException, HeuristicMixedException,
           HeuristicRollbackException, SecurityException, SystemException
  {
    try {
      if (! _isTransactionActive)
        throw new IllegalStateException("UserTransaction.commit() requires an active transaction.  Either the UserTransaction.begin() is missing or the transaction has already been committed or rolled back.");

      _transactionManager.commit();
    } finally {
      _xaResources.clear();

      _isTransactionActive = false;
    }
  }
  
  /**
   * Rolls the transaction back
   */
  @Override
  public void rollback()
    throws IllegalStateException, SecurityException, SystemException
  {
    try {
      _transactionManager.rollback();
    } finally {
      _isTransactionActive = false;
      
      _xaResources.clear();
    }
  }

  /**
   * Aborts the transaction.
   */
  public void abortTransaction()
    throws IllegalStateException
  {
    IllegalStateException exn = null;

    _isInContext = false;
    
    boolean isTransactionActive = _isTransactionActive;
    _isTransactionActive = false;

    if (! isTransactionActive && _xaResources.size() > 0) {
      Exception e = new IllegalStateException("Internal error: user transaction pool broken because poolItems exist, but no transaction is active.");
      log.log(Level.WARNING, e.toString(), e);
    }
    
    _xaResources.clear();

    if (isTransactionActive) {
      try {
        exn = new IllegalStateException(L.l("Transactions must have a commit() or rollback() in a finally block."));

        log.warning("Rolling back unclosed transaction.  All transactions must have a commit() or rollback() in a finally block.");

        _transactionManager.addUnclosedTransaction("Rolling back unclosed transaction.");
        _transactionManager.rollback();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString());
      }

    }

    _xaResources.clear();
    
    exn = clearDanglingResources(exn);

    try {
      _transactionManager.setTransactionTimeout(0);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (exn != null)
      throw exn;
  }
  
  private IllegalStateException clearDanglingResources(IllegalStateException exn)
  {
    if (_resources.size() == 0)
      return exn;

    ArrayList<ManagedResource> resourceList
      = new ArrayList<ManagedResource>(_resources);
    
    _resources.clear();

    boolean hasWarning = false;
    for (ManagedResource resource : resourceList) {
      if (! resource.isCloseDanglingConnections())
        continue;

      if (! hasWarning) {
        hasWarning = true;

        log.warning("Closing dangling resources.  Applications must close all resources in a finally block.");
      }

      try {
        IllegalStateException stackTrace = resource.getAllocationStackTrace();

        if (stackTrace != null)
          log.log(Level.WARNING, stackTrace.getMessage(), stackTrace);
        else {
          // start saving the allocation stack trace.
          resource.setSaveAllocationStackTrace(true);
        }
        
        String msg = L.l("Resource {0} was not closed. Applications must close all resources in a finally block.",
                         resource.getUserConnection());

        if (exn == null)
          exn = new IllegalStateException(msg);
        
        _transactionManager.addUnclosedResource(msg);

        resource.abortConnection();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return exn;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

