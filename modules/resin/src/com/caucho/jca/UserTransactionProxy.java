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

package com.caucho.jca;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.transaction.UserTransaction;
import javax.transaction.*;
import javax.transaction.xa.*;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.loader.EnvironmentLocal;

import com.caucho.transaction.TransactionManagerImpl;

/**
 * Implementation of the UserTransactionImpl for a thread instance.
 */
public class UserTransactionProxy implements UserTransaction {
  private static final Logger log = Log.open(UserTransactionProxy.class);
  private static final L10N L = new L10N(UserTransactionProxy.class);

  /*
  private static final EnvironmentLocal<UserTransactionProxy> _localUT =
    new EnvironmentLocal<UserTransactionProxy>();
  */
  private static final UserTransactionProxy _proxy
    = new UserTransactionProxy();

  private static final ThreadLocal<UserTransactionImpl> _threadTransaction
    = new ThreadLocal<UserTransactionImpl>();

  /**
   * Creates the proxy.
   */
  private UserTransactionProxy()
  {
    /*
    if (_localUT.getLevel() == null)
      _localUT.set(this);
    */
  }

  /**
   * Returns the local UT proxy.
   */
  public static UserTransactionProxy getInstance()
  {
    //return _localUT.get();
    return _proxy;
  }

  /**
   * Gets the thread transaction.
   */
  public UserTransactionImpl getTransaction()
  {
    UserTransactionImpl xa = _threadTransaction.get();

    if (xa == null) {
      xa = new UserTransactionImpl(TransactionManagerImpl.getLocal());
      _threadTransaction.set(xa);
    }

    return xa;
  }
  
  /**
   * Sets the transaction's timeout.
   */
  public void setTransactionTimeout(int seconds)
    throws SystemException
  {
    getTransaction().setTransactionTimeout(seconds);
  }
  
  /**
   * Gets the transaction's status
   */
  public int getStatus()
    throws SystemException
  {
    return getTransaction().getStatus();
  }
  
  /**
   * Start the transaction.
   */
  public void begin()
    throws NotSupportedException, SystemException
  {
    getTransaction().begin();
  }
  
  /**
   * Marks the transaction as rollback only.
   */
  public void setRollbackOnly()
    throws IllegalStateException, SystemException
  {
    getTransaction().setRollbackOnly();
  }
  
  /**
   * Commits the transaction
   */
  public void commit()
    throws IllegalStateException, RollbackException, HeuristicMixedException,
	   HeuristicRollbackException, SecurityException, SystemException
  {
    getTransaction().commit();
  }
  
  /**
   * Rolls the transaction back
   */
  public void rollback()
    throws IllegalStateException, SecurityException, SystemException
  {
    getTransaction().rollback();
  }

  /**
   * Enlist a close resource
   */
  public void enlistCloseResource(CloseResource resource)
    throws IllegalStateException
  {
    getTransaction().enlistCloseResource(resource);
  }

  /**
   * Finish any transaction.
   */
  public void abortTransaction()
    throws IllegalStateException
  {
    UserTransactionImpl xa = _threadTransaction.get();

    if (xa == null)
      return;
    
    xa.abortTransaction();
  }

  /**
   * Recovers an XAResource
   */
  void recover(XAResource xaRes)
    throws XAException
  {
    TransactionManagerImpl.getLocal().recover(xaRes);
  }

  public String toString()
  {
    return "UserTransactionProxy[]";
  }
}

