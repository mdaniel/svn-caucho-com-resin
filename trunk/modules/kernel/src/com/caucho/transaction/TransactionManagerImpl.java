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

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.caucho.config.inject.SingletonBindingHandle;
import com.caucho.config.types.Period;
import com.caucho.env.meter.MeterService;
import com.caucho.env.meter.TimeSensor;
import com.caucho.loader.Environment;
import com.caucho.transaction.xalog.AbstractXALogManager;
import com.caucho.transaction.xalog.AbstractXALogStream;
import com.caucho.util.Alarm;
import com.caucho.util.Crc64;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

/**
 * Implementation of the transaction manager.
 */
public class TransactionManagerImpl
  implements TransactionManager,
             Serializable
{
  private static final long serialVersionUID = 1L;
  private static L10N L = new L10N(TransactionManagerImpl.class);
  private static Logger log
    = Logger.getLogger(TransactionManagerImpl.class.getName());

  private static TransactionManagerImpl _tm = new TransactionManagerImpl();

  private long _serverId;

  private long _randomId = RandomUtil.getRandomLong();

  private AtomicLong _sequence = new AtomicLong(CurrentTime.getCurrentTime());

  private AbstractXALogManager _xaLogManager;

  private TransactionSynchronizationRegistry _syncRegistry
    = new TransactionSynchronizationRegistryImpl(this);

  // Thread local is dependent on the transaction manager.
  private ThreadLocal<TransactionImpl> _threadTransaction = new ThreadLocal<TransactionImpl>();

  private ArrayList<WeakReference<TransactionImpl>> _transactionList
    = new ArrayList<WeakReference<TransactionImpl>>();

  private long _timeout = -1;

  // statistics and counters
  // private TransactionManagerAdmin _admin;

  private TimeSensor _commitSensor
    = MeterService.createTimeMeter("Resin|XA|Commit");

  private TimeSensor _rollbackSensor
    = MeterService.createTimeMeter("Resin|XA|Rollback");

  private AtomicInteger _transactionCount
    = new AtomicInteger();

  private AtomicLong _commitCount
    = new AtomicLong();

  private AtomicLong _commitResourceFailCount
    = new AtomicLong();

  private AtomicLong _rollbackCount
    = new AtomicLong();

  private AtomicLong _unclosedResourceCount
    = new AtomicLong();

  private String _lastUnclosedResourceMessage;

  private AtomicLong _unclosedTransactionCount
    = new AtomicLong();

  public TransactionManagerImpl()
  {
    new TransactionManagerAdmin(this);
  }

  /**
   * Returns the local transaction manager.
   */
  public static TransactionManagerImpl getInstance()
  {
    return _tm;
  }

  /**
   * Returns the local transaction manager.
   */
  public static TransactionManagerImpl getLocal()
  {
    return _tm;
  }

  /**
   * Sets the timeout.
   */
  public void setTimeout(Period timeout)
  {
    _timeout = timeout.getPeriod();
  }

  /**
   * Gets the timeout.
   */
  public long getTimeout()
  {
    return _timeout;
  }

  /**
   * Sets the XA log manager.
   */
  public void setXALogManager(AbstractXALogManager xaLogManager)
  {
    _xaLogManager = xaLogManager;
  }

  /**
   * Returns the synchronization registry
   */
  public TransactionSynchronizationRegistry getSyncRegistry()
  {
    return _syncRegistry;
  }

  /**
   * Create a new transaction and associate it with the thread.
   */
  @Override
  public void begin() throws NotSupportedException, SystemException
  {
    getCurrent().begin();
  }

  /**
   * Creates a new transaction id.
   */
  XidImpl createXID()
  {
    return new XidImpl(getServerId(), _randomId, _sequence.incrementAndGet());
  }

  /**
   * Creates a new transaction id.
   */
  AbstractXALogStream getXALogStream()
  {
    if (_xaLogManager != null)
      return _xaLogManager.getStream();
    else
      return null;
  }

  /**
   * Returns the server id.
   */
  private long getServerId()
  {
    if (_serverId == 0) {
      String server = (String) Environment.getAttribute("caucho.server-id");

      if (server == null)
        _serverId = 1;
      else
        _serverId = Crc64.generate(server);
    }

    return _serverId;
  }

  /**
   * Returns the transaction for the current thread.
   */
  @Override
  public TransactionImpl getTransaction()
  {
    TransactionImpl trans = _threadTransaction.get();

    if (trans == null || trans.getStatus() == Status.STATUS_NO_TRANSACTION
        || trans.getStatus() == Status.STATUS_UNKNOWN || trans.isSuspended()) {
      return null;
    } else {
      return trans;
    }
  }

  /**
   * Suspend the transaction.
   */
  @Override
  public Transaction suspend() throws SystemException
  {
    TransactionImpl trans = _threadTransaction.get();

    if (trans == null
        || (!trans.hasResources() && (trans.getStatus() == Status.STATUS_NO_TRANSACTION || trans
            .getStatus() == Status.STATUS_UNKNOWN)))
      return null;

    _threadTransaction.set(null);
    trans.suspend();

    return trans;
  }

  /**
   * Resume the transaction.
   */
  @Override
  public void resume(Transaction tobj) throws InvalidTransactionException,
      SystemException
  {
    Transaction old = _threadTransaction.get();

    if (old != null && old.getStatus() != Status.STATUS_NO_TRANSACTION)
      throw new SystemException(L.l(
          "can't resume transaction with active transaction {0}", String
              .valueOf(old)));

    TransactionImpl impl = (TransactionImpl) tobj;

    impl.resume();

    _threadTransaction.set(impl);
  }

  /**
   * Force any completion to be a rollback.
   */
  @Override
  public void setRollbackOnly() throws SystemException
  {
    getCurrent().setRollbackOnly();
  }

  /**
   * Force any completion to be a rollback.
   */
  public void setRollbackOnly(Exception e)
  {
    getCurrent().setRollbackOnly(e);
  }

  /**
   * Returns the transaction's status
   */
  @Override
  public int getStatus() throws SystemException
  {
    return getCurrent().getStatus();
  }

  /**
   * sets the timeout for the transaction
   */
  public void setTransactionTimeout(int seconds) throws SystemException
  {
    getCurrent().setTransactionTimeout(seconds);
  }

  /**
   * Commit the transaction.
   */
  @Override
  public void commit() throws RollbackException, HeuristicMixedException,
      HeuristicRollbackException, SystemException
  {
    getCurrent().commit();
  }

  /**
   * Rollback the transaction.
   */
  @Override
  public void rollback()
  {
    getCurrent().rollback();
  }

  /**
   * Returns the current TransactionImpl, creating if necessary.
   *
   * <p/>
   * The TransactionImpl is not an official externally visible Transaction if
   * the status == NO_TRANSACTION.
   */
  public TransactionImpl getCurrent()
  {
    TransactionImpl trans = _threadTransaction.get();

    if (trans == null || trans.isDead()) {
      trans = new TransactionImpl(this);
      _threadTransaction.set(trans);

      addTransaction(trans);
    }

    return trans;
  }

  private void addTransaction(TransactionImpl trans)
  {
    synchronized (_transactionList) {
      for (int i = _transactionList.size() - 1; i >= 0; i--) {
        WeakReference<TransactionImpl> ref = _transactionList.get(i);

        if (ref.get() == null)
          _transactionList.remove(i);
      }

      _transactionList.add(new WeakReference<TransactionImpl>(trans));
    }
  }

  /**
   * Returns the corresponding user transaction.
   */
  public void recover(XAResource xaRes) throws XAException
  {
    Xid [] xids = null;

    try {
      xids = xaRes.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    } catch (XAException e) {
      int code = e.errorCode;

      if (e.getMessage() == null || e.getMessage().isEmpty()) {
        XAException e1 = new XAException(L.l("Error during recovery (code=" + code + ")", e));
        e1.errorCode = code;

        throw e1;
      }

      throw e;
    }

    if (xids == null)
      return;

    for (int i = 0; i < xids.length; i++) {
      byte []global = xids[i].getGlobalTransactionId();

      if (global.length != XidImpl.GLOBAL_LENGTH)
        continue;

      XidImpl xidImpl = new XidImpl(xids[i].getGlobalTransactionId());

      if (! xidImpl.isSameServer(getServerId()))
        continue;

      if (_xaLogManager != null && _xaLogManager.hasCommittedXid(xidImpl)) {
        log.fine(L.l("XAResource {0} commit xid {1}", xaRes, xidImpl));

        try {
          xaRes.commit(xids[i], false);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      } else {
        log.fine(L.l("XAResource {0} rollback/forget xid {1}", xaRes, xidImpl));

        boolean isValid = false;
        
        try {
          // #4748
          xaRes.rollback(xids[i]);
          // xaRes.forget(xids[i]);
          isValid = true;
        } catch (Throwable e) {
          if (log.isLoggable(Level.FINER))
            log.log(Level.FINER, e.toString(), e);
          else
            log.fine(e.toString());
        }
        
        if (! isValid) {
          try {
            // #4748
            xaRes.forget(xids[i]);
            isValid = true;
          } catch (Throwable e) {
            if (log.isLoggable(Level.FINER))
              log.log(Level.FINER, e.toString(), e);
            else
              log.fine(e.toString());
          }
        }
        
        if (! isValid) {
          // Oracle: If forget fails, try committing so we don't stay in a stuck
          // state
          try {
            xaRes.commit(xids[i], false);
            isValid = true;
          } catch (Throwable e1) {
            log.log(Level.WARNING, e1.toString(), e1);
          }
        }
      }
    }
  }

  /**
   * Flushes the log stream (primarily for QA).
   */
  public void flush()
  {
    if (_xaLogManager != null)
      _xaLogManager.flush();
  }

  /**
   * Statistics
   */

  long beginTransactionTime()
  {
    _transactionCount.incrementAndGet();

    return CurrentTime.getCurrentTime();
  }

  int getTransactionCount()
  {
    return _transactionCount.get();
  }

  void addCommitResourceFail()
  {
    _commitResourceFailCount.incrementAndGet();
  }

  long getCommitResourceFailCount()
  {
    return _commitResourceFailCount.get();
  }

  long getCommitCount()
  {
    return _commitCount.get();
  }

  void endCommitTime(long startTime)
  {
    _transactionCount.decrementAndGet();

    _commitCount.incrementAndGet();
    _commitSensor.add(startTime);
  }

  long getRollbackCount()
  {
    return _rollbackCount.get();
  }

  void endRollbackTime(long startTime)
  {
    _transactionCount.decrementAndGet();

    _rollbackCount.incrementAndGet();
    _rollbackSensor.add(startTime);
  }

  void addUnclosedResource(String message)
  {
    _unclosedResourceCount.incrementAndGet();
    _lastUnclosedResourceMessage = message;
  }

  long getUnclosedResourceCount()
  {
    return _unclosedResourceCount.get();
  }

  String getLastUnclosedResourceMessage()
  {
    return _lastUnclosedResourceMessage;
  }

  void addUnclosedTransaction(String message)
  {
    _unclosedTransactionCount.incrementAndGet();
    // _lastUnclosedTransactionMessage = message;
  }

  long getUnclosedTransactionCount()
  {
    return _unclosedTransactionCount.get();
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void destroy()
  {
    AbstractXALogManager xaLogManager = _xaLogManager;
    _xaLogManager = null;

    if (xaLogManager != null)
      xaLogManager.close();

    _serverId = 0;

    ArrayList<TransactionImpl> xaList = new ArrayList<TransactionImpl>();

    synchronized (_transactionList) {
      for (int i = _transactionList.size() - 1; i >= 0; i--) {
        WeakReference<TransactionImpl> ref = _transactionList.get(i);
        TransactionImpl xa = ref.get();

        if (xa != null)
          xaList.add(xa);
      }
    }

    for (TransactionImpl xa : xaList) {
      try {
        xa.rollback();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Clearing for test purposes.
   */
  public void testClear()
  {
    _serverId = 0;
    _timeout = -1;
    AbstractXALogManager logManager = _xaLogManager;
    _xaLogManager = null;
    _sequence.set(CurrentTime.getCurrentTime());
    // _randomId = RandomUtil.getRandomLong();

    if (logManager != null)
      logManager.close();
  }

  /**
   * Serialize to a handle
   */
  private Object writeReplace()
  {
    return new SingletonBindingHandle(TransactionManager.class);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
