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

package com.caucho.db.store;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.ClockCacheItem;

import com.caucho.log.Log;

import com.caucho.sql.SQLExceptionWrapper;

/**
 * Locking for tables/etc.
 */
public class Lock implements ClockCacheItem {
  private final static L10N L = new L10N(Lock.class);
  private final static Logger log = Log.open(Lock.class);

  private final long _id;
  
  private int _readCount;
  private int _upgradeCount;
  private int _writeCount;

  private boolean _isUsed;

  private ArrayList<Transaction> _queue;
  private Transaction _xa;
  
  public Lock(long id)
  {
    _id = id;
  }

  /**
   * Returns the lock identifier.
   */
  public long getId()
  {
    return _id;
  }

  /**
   * Tries to get a read lock.
   *
   * @param timeout how long to wait for a timeout
   */
  synchronized void lockRead(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockRead$" + System.identityHashCode(this) + "[" + _id + "] read:" + _readCount +
		 " upgrade:" + _upgradeCount);

    if (_upgradeCount == 0)
      _readCount++;
    else if (queue(xa, Alarm.getCurrentTime() + timeout)) {
      _readCount++;

      wake();
    }
    else
      throw new SQLException(L.l("can't obtain read only lock"));
  }

  /**
   * Tries to get a read lock.
   *
   * @param timeout how long to wait for a timeout
   */
  synchronized void lockReadOnly(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockRead[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);

    if (_writeCount == 0)
      _readCount++;
    else if (queue(xa, Alarm.getCurrentTime() + timeout)) {
      _readCount++;

      wake();
    }
    else
      throw new SQLException(L.l("can't obtain read only lock"));
  }

  /**
   * Clears a read lock.
   */
  synchronized void unlockRead()
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("UnlockRead[" + _id + "] read:" + _readCount +
		" write:" + _writeCount);
    
    _readCount--;

    if (_readCount < 0)
      Thread.dumpStack();

    wake();
  }

  /**
   * Tries to get a write lock.
   *
   * @param timeout how long to wait for a timeout
   */
  synchronized void lockUpgrade(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockUpgrade[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);

    _upgradeCount++;

    long expire = Alarm.getCurrentTime() + timeout;

    try {
      boolean isFirst = true;
      
      while (_readCount != 1) {
	queue(xa, expire); // , ! isFirst);
	isFirst = false;
      }
    } finally {
      if (_readCount != 1)
	unlockUpgradeInt();
    }
  }

  /**
   * Tries to get a write lock.
   *
   * @param timeout how long to wait for a timeout
   */
  synchronized void lockReadAndUpgrade(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockUpgrade[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);

    _upgradeCount++;

    long expire = Alarm.getCurrentTime() + timeout;
    boolean isOkay = false;

    try {
      while (_readCount != 0) {
	queue(xa, expire); // , ! isFirst);
      }

      isOkay = true;
    } finally {
      if (isOkay)
	_readCount = 1;
      else
	unlockUpgradeInt();
    }
  }

  /**
   * Clears a read lock.
   */
  synchronized void unlockUpgrade()
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("UnlockUpgrade[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);

    unlockUpgradeInt();
  }

  /**
   * Clears a write lock.
   */
  private void unlockUpgradeInt()
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("UnlockUpgradeInt[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);
    
    _upgradeCount--;

    if (_upgradeCount < 0) {
      RuntimeException e = new IllegalStateException("Illegal upgrade count: " + _upgradeCount);
      e.fillInStackTrace();
      log.log(Level.WARNING, e.toString(), e);
      
      _upgradeCount = 0;
    }

    wake();
  }

  /**
   * Tries to get a write lock.
   *
   * @param timeout how long to wait for a timeout
   */
  synchronized void lockWrite(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockWrite[" + _id + "] read:" + _readCount +
		" write:" + _writeCount);

    if (_writeCount > 0)
      throw new IllegalStateException(L.l("multiple write locks"));
    else if (_upgradeCount == 0)
      throw new IllegalStateException(L.l("no upgrade obtained for write"));

    _writeCount++;

    long expire = Alarm.getCurrentTime() + timeout;

    boolean isOkay = false;
    try {
      boolean isFirst = true;
      
      while (_readCount != 1) {
	queue(xa, expire); // , ! isFirst);
	isFirst = false;
      }

      isOkay = true;
    } finally {
      if (! isOkay)
	unlockWriteInt();
    }
  }

  /**
   * Clears a read lock.
   */
  synchronized void unlockWrite()
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("UnlockWrite[" + _id + "] read:" + _readCount +
		" write:" + _writeCount);
    
    unlockWriteInt();
  }

  /**
   * Clears a write lock.
   */
  private void unlockWriteInt()
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("UnlockWriteInt[" + _id + "] read:" + _readCount +
		" write:" + _writeCount);
    
    // _upgradeCount--;
    _writeCount--;

    if (_writeCount < 0) {
      _writeCount = 0;
      Thread.dumpStack();
    }

    wake();
  }

  /**
   * Queues the transaction for the lock.
   */
  private boolean queue(Transaction xa, long expireTime)
    throws SQLException
  {
    return queue(xa, expireTime, false);
  }

  /**
   * Queues the transaction for the lock.
   */
  private boolean queue(Transaction xa, long expireTime, boolean isLifo)
    throws SQLException
  {
    if (_queue == null)
      _queue = new ArrayList<Transaction>();

    if (isLifo)
      _queue.add(0, xa);
    else
      _queue.add(xa);

    long startTime = Alarm.getCurrentTime();

    try {
      do {
	long delta = expireTime - Alarm.getCurrentTime();

	if (delta > 0)
	  wait(delta);

	if (xa == _xa) {
	  _xa = null;
	  return true;
	}
      } while (Alarm.getCurrentTime() < expireTime && ! Alarm.isTest()); 

      log.fine(L.l("transaction timed out waiting for lock"));
      throw new SQLException(L.l("transaction timed out waiting for lock {0}",
				 Alarm.getCurrentTime() - startTime));
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      throw new SQLExceptionWrapper(e);
    } finally {
      _queue.remove(xa);
    }
  }

  /**
   * Wakes any transaction for the lock.
   */
  private boolean wake()
  {
    try {
      if (_queue == null || _queue.size() == 0 || _xa != null)
	return false;

      // fifo
      _xa = _queue.remove(0);

      notifyAll();

      return true;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      return true;
    }
  }

  // clock cache events

  public void clearUsed()
  {
    _isUsed = false;
  }

  public void setUsed()
  {
    _isUsed = true;
  }

  public synchronized boolean isUsed()
  {
    return _isUsed || _readCount > 0 || _upgradeCount > 0;
  }

  public void removeEvent()
  {
  }
  
  public String toString()
  {
    return "Lock$" + System.identityHashCode(this) + "[" + _id + "]";
  }
}
