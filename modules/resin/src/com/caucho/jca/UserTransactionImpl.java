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

package com.caucho.jca;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.transaction.UserTransaction;
import javax.transaction.*;
import javax.transaction.xa.*;

import javax.security.auth.Subject;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ConnectionRequestInfo;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.TransactionImpl;

/**
 * Implementation of the UserTransactionImpl for a thread instance.
 */
public class UserTransactionImpl implements UserTransaction {
  private static final Logger log = Log.open(UserTransactionImpl.class);
  private static final L10N L = new L10N(UserTransactionImpl.class);

  private TransactionManagerImpl _transactionManager;

  private ArrayList<UserPoolItem> _resources = new ArrayList<UserPoolItem>();
  private ArrayList<PoolItem> _poolItems = new ArrayList<PoolItem>();
  private ArrayList<BeginResource> _beginResources
    = new ArrayList<BeginResource>();
  private ArrayList<CloseResource> _closeResources
    = new ArrayList<CloseResource>();

  private boolean _isTransaction;
  
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
  public void setTransactionTimeout(int seconds)
    throws SystemException
  {
    _transactionManager.setTransactionTimeout(seconds);
  }
  
  /**
   * Gets the transaction's status
   */
  public int getStatus()
    throws SystemException
  {
    return _transactionManager.getStatus();
  }

  /**
   * Enlist a resource.
   */
  void enlistResource(UserPoolItem resource)
    throws SystemException, RollbackException
  {
    if (_resources.contains(resource))
      return;
    
    Transaction xa = _transactionManager.getTransaction();
    if (xa != null) {
      PoolItem poolItem = resource.getXAPoolItem();

      enlistPoolItem(xa, poolItem);
    }
    
    _resources.add(resource);
  }

  private void enlistPoolItem(Transaction xa, PoolItem poolItem)
    throws SystemException, RollbackException
  {
    if (poolItem == null)
      return;
    else if (! poolItem.supportsTransaction()) {
      // server/164j
      return;
    }
    
    // XXX: new
    if (_poolItems.contains(poolItem))
      return;
	
    poolItem.setTransaction(this);

    if (xa instanceof TransactionImpl) {
      TransactionImpl xaImpl = (TransactionImpl) xa;
      
      // server/164l
      if (xaImpl.allowLocalTransactionOptimization())
	poolItem.enableLocalTransactionOptimization(true);
    }

    if (poolItem.getXid() == null)
      xa.enlistResource(poolItem);
    
    _poolItems.add(poolItem);
  }

  /**
   * Delist a pool item
   */
  void delistPoolItem(PoolItem poolItem, int flags)
    throws SystemException, RollbackException
  {
    Transaction xa = _transactionManager.getTransaction();

    try {
      if (xa != null)
	xa.delistResource(poolItem, flags);
    } finally {
      _poolItems.remove(poolItem);
    }
  }

  /**
   * Delist a resource.
   */
  void delistResource(UserPoolItem resource)
  {
    _resources.remove(resource);
  }

  /**
   * Enlist a resource automatically called when a transaction begins
   */
  public void enlistBeginResource(BeginResource resource)
  {
    _beginResources.add(resource);

    try {
      Transaction xa = _transactionManager.getTransaction();
      if (xa != null)
	resource.begin(xa);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Enlist a resource automatically closed when the context ends.
   */
  public void enlistCloseResource(CloseResource resource)
  {
    _closeResources.add(resource);
  }

  /**
   * Allocates a resource matching the parameters.  If none matches,
   * return null.
   */
  UserPoolItem allocate(ManagedConnectionFactory mcf,
			Subject subject,
			ConnectionRequestInfo info)
  {
    if (! _isTransaction)
      return null;
    
    ArrayList<PoolItem> poolItems = _poolItems;
    int length = poolItems.size();
    
    for (int i = 0; i < length; i++) {
      PoolItem poolItem = poolItems.get(i);

      UserPoolItem item = poolItem.allocateXA(mcf, subject, info);

      if (item != null)
	return item;
    }

    return null;
  }

  /**
   * Finds the pool item joined to this one.
   * return null.
   */
  PoolItem findJoin(PoolItem item)
  {
    if (! _isTransaction)
      return null;
    
    ArrayList<PoolItem> poolItems = _poolItems;
    int length = poolItems.size();
    
    for (int i = 0; i < length; i++) {
      PoolItem poolItem = poolItems.get(i);

      if (poolItem.isJoin(item))
	return poolItem;
    }

    return null;
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
   * Returns the XID.
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
  public void begin()
    throws NotSupportedException, SystemException
  {
    if (_isTransaction)
      throw new IllegalStateException(L.l("UserTransaction.begin() is not allowed when a transaction is active."));
    
    _transactionManager.begin();
    _isTransaction = true;
    boolean isOkay = false;

    try {
      TransactionImpl xa = (TransactionImpl) _transactionManager.getTransaction();
      xa.setUserTransaction(this);
    
      _poolItems.clear();
    
      // enlist "cached" connections
      int length = _resources.size();

      for (int i = 0; i < length; i++) {
	UserPoolItem userPoolItem = _resources.get(i);

	for (int j = _poolItems.size() - 1; j >= 0; j--) {
	  PoolItem poolItem = _poolItems.get(j);

	  if (poolItem.share(userPoolItem)) {
	    break;
	  }
	}

	PoolItem xaPoolItem = userPoolItem.getXAPoolItem();
	if (! _poolItems.contains(xaPoolItem))
	  _poolItems.add(xaPoolItem);
      }

      for (int i = 0; i < _poolItems.size(); i++) {
	PoolItem poolItem = _poolItems.get(i);

	poolItem.enableLocalTransactionOptimization(_poolItems.size() == 1);

	try {
	  xa.enlistResource(poolItem);
	} catch (Exception e) {
	  throw new SystemException(e);
	}
      }

      // enlist begin resources
      for (int i = 0; i < _beginResources.size(); i++) {
	try {
	  BeginResource resource = _beginResources.get(i);

	  resource.begin(xa);
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      isOkay = true;
    } finally {
      if (! isOkay) {
	log.warning("Rolling back transaction from failed begin()");
	
	// something has gone very wrong
	_isTransaction = false;

	ArrayList<PoolItem> recoveryList = new ArrayList<PoolItem>(_poolItems);
	_poolItems.clear();
	_resources.clear();

	for (int i = 0; i < recoveryList.size(); i++) {
	  try {
	    PoolItem item = recoveryList.get(i);

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
  public UserTransactionSuspendState suspend()
  {
    if (! _isTransaction)
      throw new IllegalStateException(L.l("suspend may only be called in a transaction."));

    _isTransaction = false;
    
    UserTransactionSuspendState state;
    state = new UserTransactionSuspendState(_poolItems);
    _poolItems.clear();

    return state;
  }

  /**
   * Resumes the transaction.
   */
  public void resume(UserTransactionSuspendState state)
  {
    /*
    if (_inTransaction)
      throw new IllegalStateException(L.l("resume may only be called outside of a transaction."));
    */

    _isTransaction = true;

    _poolItems.addAll(state.getPoolItems());
  }
  
  /**
   * Marks the transaction as rollback only.
   */
  public void setRollbackOnly()
    throws IllegalStateException, SystemException
  {
    _transactionManager.setRollbackOnly();
  }
  
  /**
   * Commits the transaction
   */
  public void commit()
    throws IllegalStateException, RollbackException, HeuristicMixedException,
	   HeuristicRollbackException, SecurityException, SystemException
  {
    try {
      /* XXX: interaction with hessian XA
      if (_xaDepth == 0)
	throw new IllegalStateException("Can't commit outside of a transaction.  Either the UserTransaction.begin() is missing or the transaction has already been committed or rolled back.");
      */
      
      _transactionManager.commit();
    } finally {
      _poolItems.clear();

      _isTransaction = false;
    }
  }
  
  /**
   * Rolls the transaction back
   */
  public void rollback()
    throws IllegalStateException, SecurityException, SystemException
  {
    try {
      _transactionManager.rollback();
    } finally {
      _poolItems.clear();

      _isTransaction = false;
    }
  }

  /**
   * Aborts the transaction.
   */
  public void abortTransaction()
    throws IllegalStateException
  {
    IllegalStateException exn = null;

    boolean inTransaction = _isTransaction;
    _isTransaction = false;

    if (! inTransaction && _poolItems.size() > 0) {
      Exception e = new IllegalStateException("user transaction pool broken");
      log.log(Level.WARNING, e.toString(), e);
    }
    
    _poolItems.clear();

    if (inTransaction) {
      try {
	TransactionImpl xa = (TransactionImpl) _transactionManager.getTransaction();

	exn = new IllegalStateException(L.l("Transactions must have a commit() or rollback() in a finally block."));
      
	log.warning("Rolling back dangling transaction.  All transactions must have a commit() or rollback() in a finally block.");
      
	_transactionManager.rollback();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString());
      }

    }

    _beginResources.clear();
    
    while (_closeResources.size() > 0) {
      try {
	CloseResource resource;

	resource = _closeResources.remove(_closeResources.size() - 1);
	resource.close();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    if (_resources.size() > 0) {
      log.warning("Closing dangling connections.  All connections must have a close() in a finally block.");
    }
    while (_resources.size() > 0) {
      UserPoolItem userPoolItem = _resources.remove(_resources.size() - 1);
      
      try {
	IllegalStateException stackTrace = userPoolItem.getAllocationStackTrace();

	if (stackTrace != null)
	  log.log(Level.WARNING, stackTrace.getMessage(), stackTrace);
	else {
	  // start saving the allocation stack trace.
	  userPoolItem.setSaveAllocationStackTrace(true);
	}
      
	if (exn == null)
	  exn = new IllegalStateException(L.l("Connection {0} was not closed. Connections must have a close() in a finally block.",
					      userPoolItem.getUserConnection()));

	userPoolItem.abortConnection();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    _poolItems.clear();

    try {
      _transactionManager.setTransactionTimeout(0);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (exn != null)
      throw exn;
  }
}

