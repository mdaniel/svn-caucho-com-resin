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
  
  private volatile int _readCount;
  private volatile int _upgradeCount;
  private volatile int _writeCount;

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
  void lockRead(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("LockRead$" + System.identityHashCode(this) + "[" + _id + "] read:" + _readCount +
		 " upgrade:" + _upgradeCount);
    }

    long expire = Alarm.getCurrentTime() + timeout;

    synchronized (_queue) {
      queue(xa, expire);

      _readCount++;

      dequeue(xa);

      wake();
    }
  }

  /**
   * Clears a read lock.
   */
  void unlockRead()
    throws SQLException
  {
    synchronized (_queue) {
      _readCount--;
    
      if (_readCount < 0)
	Thread.dumpStack();

      if (log.isLoggable(Level.FINEST))
	log.finest("UnlockRead[" + _id + "] read:" + _readCount +
		   " write:" + _writeCount);

      wake();
    }
  }

  /**
   * Tries to get a write lock.
   *
   * @param timeout how long to wait for a timeout
   */
  void lockUpgrade(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockUpgrade[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);

    boolean isOkay = false;

    synchronized (_queue) {
      _upgradeCount++;

      try {
	long expire = Alarm.getCurrentTime() + timeout;
      
	queue(xa, expire);

	isOkay = waitForRead(xa, expire, 1);
      } finally {
	if (isOkay) {
	  _readCount--;
	}
	else {
	  _upgradeCount--;

	  wake();
	}
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
  void lockReadAndUpgrade(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockUpgrade[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);

    long expire = Alarm.getCurrentTime() + timeout;
    boolean isOkay = false;

    synchronized (_queue) {
      _upgradeCount++;

      try {
	queue(xa, expire);

	isOkay = waitForRead(xa, expire, 0);
      } finally {
	if (! isOkay) {
	  _upgradeCount--;

	  wake();
	}
      }
    }

    if (! isOkay)
      throw new IllegalStateException("Expected valid upgrade lock");
  }

  /**
   * Clears a read lock.
   */
  void unlockUpgrade()
    throws SQLException
  {
    synchronized (_queue) {
      unlockUpgradeInt();
    }
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
  void lockWrite(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockWrite[" + _id + "] read:" + _readCount +
		" write:" + _writeCount);

    boolean isOkay = false;
    long expire = Alarm.getCurrentTime() + timeout;

    synchronized (_queue) {
      if (_writeCount > 0)
	throw new IllegalStateException(L.l("multiple write locks"));
      else if (_upgradeCount == 0)
	throw new IllegalStateException(L.l("no upgrade obtained for write"));

      _writeCount++;

      try {
	queue(xa, expire);

	isOkay = waitForRead(xa, expire, 1);
      } finally {
	if (isOkay) {
	  _readCount--;
	}
	else {
	  _writeCount--;

	  wake();
	}
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
  void lockReadAndWrite(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("LockWrite[" + _id + "] read:" + _readCount +
		" upgrade:" + _upgradeCount);

    long expire = Alarm.getCurrentTime() + timeout;
    boolean isOkay = false;

    synchronized (_queue) {
      _upgradeCount++;

      try {
	queue(xa, expire);

	isOkay = waitForRead(xa, expire, 0);
      } finally {
	if (isOkay) {
	  _writeCount = 1;
	}
	else {
	  _upgradeCount--;

	  wake();
	}
      }
    }

    if (! isOkay)
      throw new IllegalStateException("Expected valid upgrade lock");
  }

  /**
   * Clears a write lock.
   */
  void unlockWrite()
    throws SQLException
  {
    synchronized (_queue) {
      unlockWriteInt();
    }
    
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
    boolean isOkay = false;

    _queue.add(xa);

    if (_queue.size() == 1)
      return true;

    try {
      do {
	long delta = expireTime - Alarm.getCurrentTime();

	if (delta > 0) {
	  _queue.wait(delta);
	}

	if (_queue.get(0) == xa) {
	  isOkay = true;
	  return true;
	}
      } while (Alarm.getCurrentTime() < expireTime && ! Alarm.isTest()); 

      _queue.notifyAll();
      
      log.fine(L.l("{0}: transaction timed out waiting for lock", this));

      LockTimeoutException e;
      e = new LockTimeoutException(L.l("transaction timed out ({0}ms) waiting for lock {1}, read:{2} upgrade:{3}, write:{4}",
					 Alarm.getCurrentTime() - startTime,
					 this,
					 _readCount,
					 _upgradeCount,
					 _writeCount));

      e.fillInStackTrace();
      e.printStackTrace();

      throw e;
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      throw new SQLExceptionWrapper(e);
    } finally {
      if (! isOkay)
	_queue.remove(xa);
    }
  }

  /**
   * Waits for the reads to be a certain level.
   */
  private boolean waitForRead(Transaction xa, long expireTime, int readCount)
    throws SQLException
  {
    boolean isOkay = false;

    if (_queue.size() < 1 || _queue.get(0) != xa)
      throw new IllegalStateException(L.l("illegal lock state"));

    try {
      while (readCount < _readCount &&
	     Alarm.getCurrentTime() < expireTime && ! Alarm.isTest()) {
	long delta = expireTime - Alarm.getCurrentTime();

	if (delta > 0) {
	  _queue.wait(delta);
	}
      }

      if (_readCount <= readCount) {
	_readCount++;
	
	return true;
      }

      log.fine(L.l("transaction timed out waiting for read lock"));
      
      throw new LockTimeoutException(L.l("transaction timed out waiting for write lock, read:{0} upgrade:{1}, write:{2}",
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
  private void dequeue(Transaction xa)
  {
    _queue.remove(xa);
  }

  /**
   * Wakes any transaction for the lock.
   */
  private boolean wake()
  {
    try {
      _queue.notifyAll();

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

  public boolean isUsed()
  {
    synchronized (_queue) {
      return _isUsed || _readCount > 0 || _upgradeCount > 0;
    }
  }

  public void removeEvent()
  {
  }
  
  public String toString()
  {
    return "Lock$" + System.identityHashCode(this) + "[" + _id + "]";
  }
}
