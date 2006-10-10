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
public final class Lock implements ClockCacheItem {
  private final static L10N L = new L10N(Lock.class);
  private final static Logger log = Log.open(Lock.class);

  private final long _id;
  
  private int _readCount;
  private int _upgradeCount;
  private int _writeCount;

  private boolean _isUsed;

  private final ArrayList<Transaction> _queue = new ArrayList<Transaction>();
  
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
    if (log.isLoggable(Level.FINEST)) {
      log.finest("LockRead$" + System.identityHashCode(this) + "[" + _id + "] read:" + _readCount +
		 " upgrade:" + _upgradeCount);
    }

    if (_upgradeCount == 0) {
      _readCount++;
    }
    else if (queue(xa, Alarm.getCurrentTime() + timeout)) {
      _readCount++;

      wake();
    }
    else
      throw new LockTimeoutException(L.l("can't obtain read lock"));
  }

  /**
   * Clears a read lock.
   */
  synchronized void unlockRead()
    throws SQLException
  {
    _readCount--;

    if (log.isLoggable(Level.FINEST))
      log.finest("UnlockRead[" + _id + "] read:" + _readCount +
		" write:" + _writeCount);

    wake();
    
    if (_readCount < 0)
      Thread.dumpStack();
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

    boolean isOkay = false;
    try {
      long expire = Alarm.getCurrentTime() + timeout;
      
      while (_readCount != 1) {
	queue(xa, expire);
      }

      isOkay = _readCount == 1;
    } finally {
      if (! isOkay) {
	_upgradeCount--;

	wake();
      }
    }

    if (! isOkay)
      throw new IllegalStateException("Expected valid upgrade lock");
  }

  /**
   * Tries to get a upgrade lock.
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

      isOkay = _readCount == 0;
    } finally {
      if (isOkay)
	_readCount = 1;
      else {
	_upgradeCount--;

	wake();
      }
    }

    if (! isOkay)
      throw new IllegalStateException("Expected valid upgrade lock");
  }

  /**
   * Clears a read lock.
   */
  synchronized void unlockUpgrade()
    throws SQLException
  {
    unlockUpgradeInt();
  }

  /**
   * Clears a write lock.
   */
  private void unlockUpgradeInt()
    throws SQLException
  {
    _upgradeCount--;

    if (_upgradeCount < 0) {
      RuntimeException e = new IllegalStateException("Illegal upgrade count: " + _upgradeCount);
      e.fillInStackTrace();
      log.log(Level.WARNING, e.toString(), e);
      
      _upgradeCount = 0;
    }

    if (log.isLoggable(Level.FINEST))
      log.finest("UnlockUpgradeInt[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);
    
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

    boolean isOkay = false;
    try {
      long expire = Alarm.getCurrentTime() + timeout;

      while (_readCount != 1) {
	queue(xa, expire); // , ! isFirst);
      }

      isOkay = _readCount == 1;
    } finally {
      if (! isOkay) {
	_writeCount--;

	wake();
      }
    }

    if (! isOkay)
      throw new IllegalStateException("Expected valid write lock");
  }

  /**
   * Tries to get a write lock.
   *
   * @param timeout how long to wait for a timeout
   */
  synchronized void lockReadAndWrite(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockWrite[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);

    _upgradeCount++;

    long expire = Alarm.getCurrentTime() + timeout;
    boolean isOkay = false;

    try {
      while (_readCount != 0) {
	queue(xa, expire);

	if (_readCount != 0 && (expire < Alarm.getCurrentTime() ||
				Alarm.isTest()))
	  throw new LockTimeoutException(L.l("Can't obtain write lock."));
      }

      isOkay = _readCount == 0;
    } finally {
      if (isOkay) {
	_readCount = 1;
	_writeCount = 1;
      }
      else {
	_upgradeCount--;

	wake();
      }
    }

    if (! isOkay)
      throw new IllegalStateException("Expected valid upgrade lock");
  }

  /**
   * Clears a write lock.
   */
  synchronized void unlockWrite()
    throws SQLException
  {
    unlockWriteInt();
    
    if (log.isLoggable(Level.FINEST))
      log.finest("UnlockWrite[" + _id + "] read:" + _readCount +
		" write:" + _writeCount);
    
  }

  /**
   * Clears a write lock, including its upgrade and read locks.
   */
  private void unlockWriteInt()
    throws SQLException
  {
    _writeCount--;
    _upgradeCount--;
    _readCount--;

    if (_writeCount < 0) {
      _writeCount = 0;
      Thread.dumpStack();
    }

    if (_upgradeCount < 0) {
      _upgradeCount = 0;
      Thread.dumpStack();
    }

    if (_readCount < 0) {
      _readCount = 0;
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
    long startTime = Alarm.getCurrentTime();

    _queue.add(xa);

    try {
      do {
	long delta = expireTime - Alarm.getCurrentTime();

	if (delta > 0) {
	  wait(delta);
	}

	if (_queue.get(0) == xa)
	  return true;
      } while (Alarm.getCurrentTime() < expireTime && ! Alarm.isTest()); 

      notifyAll();
      
      log.fine(L.l("transaction timed out waiting for lock"));
      
      throw new LockTimeoutException(L.l("transaction timed out waiting for lock {0}ms, read:{1} upgrade:{2}, write:{3}",
					 Alarm.getCurrentTime() - startTime,
					 _readCount,
					 _upgradeCount,
					 _writeCount));
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
      notify();

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
