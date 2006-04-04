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

import java.util.Hashtable;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.transaction.Transaction;
import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;

import javax.ejb.EJBException;

import com.caucho.util.L10N;
import com.caucho.util.FreeList;

import com.caucho.config.ConfigException;

import com.caucho.log.Log;

import com.caucho.transaction.TransactionManagerImpl;

import com.caucho.amber.manager.AmberConnection;

import com.caucho.ejb.EnvServerManager;
import com.caucho.ejb.EJBExceptionWrapper;

/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class EjbTransactionManager {
  private static final L10N L = new L10N(EjbTransactionManager.class);
  private static final Logger log = Log.open(EjbTransactionManager.class);

  public static final int RESIN_DATABASE = 0;
  public static final int RESIN_READ_ONLY = 1;
  public static final int RESIN_ROW_LOCKING = 2;

  protected static final ThreadLocal<TransactionContext> _threadTransaction
    = new ThreadLocal<TransactionContext>();

  private FreeList<TransactionContext> _freeTransactions
    = new FreeList<TransactionContext>(64);

  private Hashtable<Transaction,TransactionContext> _transactionMap
    = new Hashtable<Transaction,TransactionContext>();

  private Hashtable<String,TransactionContext> _foreignTransactionMap
    = new Hashtable<String,TransactionContext>();
  
  private final EnvServerManager _ejbManager;

  protected final TransactionManager _transactionManager;
  protected UserTransaction _userTransaction;

  private int _resinIsolation = -1;
  private int _jdbcIsolation = -1;
  
  private long _transactionTimeout = 0;

  private boolean _isClosed;

  /**
   * Create a server with the given prefix name.
   */
  public EjbTransactionManager(EnvServerManager ejbManager)
    throws ConfigException
  {
    _ejbManager = ejbManager;

    UserTransaction ut = null;
    TransactionManager tm = null;
    
    try {
      InitialContext ic = new InitialContext();
      
      ut = (UserTransaction) ic.lookup("java:comp/UserTransaction");
      
      tm = (TransactionManager) ic.lookup("java:comp/TransactionManager");
    } catch (NamingException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    _userTransaction = ut;
    _transactionManager = tm;

    if (_transactionManager == null)
      throw new ConfigException(L.l("Can't load TransactionManager."));
  }

  /**
   * Returns the manager.
   */
  public EnvServerManager getEJBManager()
  {
    return _ejbManager;
  }
  

  /**
   * Sets the Resin isolation.
   */
  public void setResinIsolation(int resinIsolation)
  {
    _resinIsolation = resinIsolation;
  }

  /**
   * Sets the Resin isolation for the container.
   */
  public int getResinIsolation()
  {
    return _resinIsolation;
  }

  /**
   * Sets the JDBC isolation.
   */
  public void setJDBCIsolation(int jdbcIsolation)
  {
    _jdbcIsolation = jdbcIsolation;
  }

  /**
   * Gets the JDBC isolation level.
   */
  public int getJDBCIsolation()
  {
    return _jdbcIsolation;
  }

  /**
   * Gets the transaction timeout
   */
  public long getTransactionTimeout()
  {
    return _transactionTimeout;
  }

  /**
   * Sets the transaction timout.
   */
  public void setTransactionTimeout(long transactionTimeout)
  {
    _transactionTimeout = transactionTimeout;
  }

  void setUserTransaction(UserTransaction userTransaction)
  {
    _userTransaction = userTransaction;
  }

  public UserTransaction getUserTransaction()
  {
    return _userTransaction;
  }

  /**
   * Returns a new AmberConnection.
   */
  public AmberConnection getAmberConnection()
  {
    TransactionContext xaContext = _threadTransaction.get();

    if (xaContext != null)
      return xaContext.getAmberConnection();
    else
      throw new IllegalStateException("can't get transaction outside of context");
  }

  public void commitTransaction()
    throws EJBException
  {
    try {
      if (_transactionManager.getTransaction() != null)
	_userTransaction.commit();
    } catch (Exception e) {
      throw EJBExceptionWrapper.create(e);
    }
  }

  public void rollbackTransaction()
    throws EJBException
  {
    try {
      _userTransaction.rollback();
    } catch (Exception e) {
      throw EJBExceptionWrapper.create(e);
    }
  }

  TransactionManager getTransactionManager()
    throws EJBException
  {
    return _transactionManager;
  }
  
  public Transaction getTransaction()
    throws EJBException
  {
    try {
      return _transactionManager.getTransaction();
    } catch (Exception e) {
      throw EJBExceptionWrapper.create(e);
    }
  }

  public TransactionContext getTransactionContext()
  {
    return _threadTransaction.get();
  }

  /**
   * Returns a transaction context for the "required" transaction.  If
   * there's already an active transaction, use it.  Otherwise create a
   * new transaction.
   *
   * @return the transaction context for the request
   */
  public TransactionContext beginRequired()
    throws EJBException
  {
    try {
      Transaction oldTrans = _transactionManager.getTransaction();
      TransactionContext oldCxt = _threadTransaction.get();

      // If this is within the same EJB transaction, just bump
      // the count
      if (oldCxt != null && oldTrans != null &&
          oldCxt.getTransaction() == oldTrans) {
        oldCxt.pushDepth();
        return oldCxt;
      }

      // If there's an old transaction, see if there's a transaction context
      // (only needed to support suspends)

      if (oldTrans != null) {
        TransactionContext cxt = _transactionMap.get(oldTrans);

        if (cxt != null) {
          _transactionMap.remove(oldTrans);

          _threadTransaction.set(cxt);
          cxt.pushDepth();
        
          return cxt;
        }
      }

      // Link the new context to any old context
      TransactionContext cxt = createTransaction();
      cxt.setOld(oldCxt);
        
      _threadTransaction.set(cxt);
      // If there was an old transaction, use it
      if (oldTrans != null) {
        setTransaction(cxt, oldTrans);
        // This context is controlled by a user transaction
        cxt.setUserTransaction(true);
	cxt.pushDepth();
      }
      else {
        _userTransaction.setTransactionTimeout((int) (_transactionTimeout / 1000L));
        _userTransaction.begin();
        Transaction trans = _transactionManager.getTransaction();

	setTransaction(cxt, trans);
      }
      
      if (_resinIsolation == RESIN_ROW_LOCKING)
        cxt.setRowLocking(true);

      return cxt;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw EJBExceptionWrapper.create(e);
    }
  }

  /**
   * Returns a transaction context for a single read call.  The single
   * read is like supports, but returns a null transaction context
   * if there's no transaction.
   *
   * @return the transaction context for the request
   */
  public TransactionContext beginSingleRead()
    throws EJBException
  {
    try {
      TransactionContext cxt = _threadTransaction.get();
      Transaction trans = _transactionManager.getTransaction();

      if (trans == null)
	return null;
      
      // If in the same EJB transaction, return it
      if (cxt != null && cxt.getTransaction() == trans) {
	cxt.pushDepth();
        return cxt;
      }

      // Check to see if there's an old EJB transaction to handle resume()
      TransactionContext newCxt = _transactionMap.get(trans);

      if (newCxt != null) {
	newCxt.pushDepth();
	return newCxt;
      }

      // Create a new EJB context and link to any old context
      newCxt = createTransaction();
      newCxt.pushDepth();
        
      _threadTransaction.set(newCxt);
      setTransaction(newCxt, trans);
      // The transaction is controlled by a user transaction
      newCxt.setUserTransaction(true);

      return newCxt;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw EJBExceptionWrapper.create(e);
    }
  }

  /**
   * Begins a new transaction, suspending the old one if necessary.
   *
   * @return the old transaction context
   */
  public TransactionContext beginRequiresNew()
    throws EJBException
  {
    try {
      TransactionContext oldCxt = _threadTransaction.get();
      Transaction oldTrans = _transactionManager.suspend();

      TransactionContext newCxt = createTransaction();
      newCxt.setOld(oldCxt);
      newCxt.setOldTrans(oldTrans);

      _threadTransaction.set(newCxt);
      
      if (_resinIsolation == RESIN_ROW_LOCKING)
        newCxt.setRowLocking(true);

      _userTransaction.setTransactionTimeout((int) (_transactionTimeout / 1000L));
      _userTransaction.begin();
      Transaction trans = _transactionManager.getTransaction();
      
      setTransaction(newCxt, trans);

      return newCxt;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw EJBExceptionWrapper.create(e);
    }
  }

  /**
   * Begins a new transaction, suspending the old one if necessary.
   *
   * @return the old transaction context
   */
  public TransactionContext beginSupports()
    throws EJBException
  {
    try {
      Transaction trans = _transactionManager.getTransaction();
      TransactionContext cxt = _threadTransaction.get();

      // Create a new EJB transaction if necessary
      if (cxt == null || cxt.getTransaction() != trans) {
	// check for a suspended transaction
	if (trans != null)
	  cxt = _transactionMap.get(trans);
	else
	  cxt = null;

	if (cxt == null) {
	  cxt = createTransactionNoDepth();
	  setTransaction(cxt, trans);
	}
	
        _threadTransaction.set(cxt);

	if (trans != null) {
	  cxt.setUserTransaction(true);
	  cxt.pushDepth();
	}
      }

      cxt.pushDepth();
      
      if (_resinIsolation == RESIN_ROW_LOCKING)
	cxt.setRowLocking(true);
      
      return cxt;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw EJBExceptionWrapper.create(e);
    }
  }

  /**
   * Suspends the current transaction
   *
   * @return the old transaction context
   */
  public TransactionContext suspend()
    throws EJBException
  {
    try {
      TransactionContext oldCxt = _threadTransaction.get();
      Transaction oldTrans = _transactionManager.suspend();

      if (oldTrans == null && oldCxt != null) {
        oldCxt.pushDepth();
        return oldCxt;
      }

      TransactionContext cxt = createTransaction();
      _threadTransaction.set(cxt);

      cxt.setOld(oldCxt);
      cxt.setOldTrans(oldTrans);

      return cxt;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw EJBExceptionWrapper.create(e);
    }
  }

  /**
   * Starts a Never transaction, i.e. there is no transaction.
   *
   * @return the old transaction context
   */
  public TransactionContext beginNever()
    throws EJBException
  {
    try {
      Transaction oldTrans = _transactionManager.getTransaction();

      if (oldTrans != null)
        throw new EJBException("Transaction forbidden in 'Never' method");

      TransactionContext cxt = _threadTransaction.get();

      if (cxt == null) {
        cxt = createTransaction();
        _threadTransaction.set(cxt);
      }
      else
        cxt.pushDepth();

      return cxt;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw EJBExceptionWrapper.create(e);
    }
  }

  /**
   * Begins a new transaction, the old one must exist.
   *
   * @return the old transaction context
   */
  public TransactionContext beginMandatory()
    throws EJBException
  {
    try {
      Transaction trans = _transactionManager.getTransaction();

      if (trans == null)
        throw new EJBException("Transaction required in 'Mandatory' method");

      return beginRequired();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw EJBExceptionWrapper.create(e);
    }
  }

  /**
   * Resumes a suspended transaction.
   *
   * @return the old transaction context
   */
  public void resume(TransactionContext oldCxt,
		     Transaction oldTrans,
		     Transaction completedTransaction)
    throws EJBException
  {
    try {
      if (completedTransaction != null)
	_transactionMap.remove(completedTransaction);
      _threadTransaction.set(oldCxt);

      if (oldTrans != null)
        _transactionManager.resume(oldTrans);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw EJBExceptionWrapper.create(e);
    }
  }

  /**
   * Binds the EJB transaction context to the TM transaction context.
   */
  private void setTransaction(TransactionContext cxt, Transaction trans)
  {
    if (cxt != null) {
      Transaction old = cxt.getTransaction();

      if (old != null)
	_transactionMap.remove(old);
    }
    else if (trans != null)
      _transactionMap.remove(trans);
    
    cxt.setTransaction(trans);
      
    if (trans != null)
      _transactionMap.put(trans, cxt);
  }

  /**
   * Create a transaction context.
   */
  TransactionContext createTransaction()
  {
    TransactionContext trans = _freeTransactions.allocate();

    if (trans == null)
      trans = new TransactionContext(this);
    trans.init(true);

    return trans;
  }

  /**
   * Create a transaction context.
   */
  TransactionContext createTransactionNoDepth()
  {
    TransactionContext trans = _freeTransactions.allocate();

    if (trans == null)
      trans = new TransactionContext(this);
    trans.init(false);

    return trans;
  }

  /**
   * Creates a transaction from an external xid.
   */
  public TransactionContext startTransaction(String xid)
  {
    try {
      TransactionContext xa = _foreignTransactionMap.get(xid);

      if (xa != null) {
	resume(xa, xa.getTransaction(), null);
	return xa;
      }

      xa = beginRequiresNew();

      _foreignTransactionMap.put(xid, xa);

      return xa;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      _foreignTransactionMap.remove(xid);

      return null;
    }
  }

  /**
   * Creates a transaction from an external xid.
   */
  public void finishTransaction(String xid)
  {
    try {
      TransactionContext xa = _foreignTransactionMap.get(xid);

      if (xa == null) {
      }
      else if (xa.isEmpty()) {
	_foreignTransactionMap.remove(xid);
	xa.commit();
      }
      else {
	suspend();
	//
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Commits a transaction from an external xid.
   */
  public void commitTransaction(String xid)
  {
    try {
      TransactionContext xa = _foreignTransactionMap.remove(xid);

      if (xa != null) {
	resume(xa, xa.getTransaction(), null);
	xa.commit();
      }
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Rolls-back a transaction from an external xid.
   */
  public void rollbackTransaction(String xid)
  {
    try {
      TransactionContext xa = _foreignTransactionMap.remove(xid);

      if (xa != null) {
	resume(xa, xa.getTransaction(), null);
	xa.rollback();
      }
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Free a transaction context.
   */
  void freeTransaction(TransactionContext trans)
  {
    _freeTransactions.free(trans);
  }

  /**
   * Closes the container.
   */
  public void destroy()
  {
    synchronized (this) {
      if (_isClosed)
	return;

      _isClosed = true;
    }
    
    _transactionMap = null;
  }
}

