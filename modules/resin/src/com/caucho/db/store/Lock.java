/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.util.Alarm;
import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Locking for tables/etc.
 */
public final class Lock {
  private final static L10N L = new L10N(Lock.class);
  private final static Logger log
    = Logger.getLogger(Lock.class.getName());

  private final String _id;

  // count of threads trying to upgrade from a read lock
  private int _tryUpgradeCount;
  // count of threads trying to get a write lock
  private int _tryWriteCount;
  
  // count of threads with a read currently running
  private int _tryReadCount;
  // count of threads with a read currently running
  private int _readCount;
  // true if a thread has a write lock
  private boolean _isWrite;
  
  private Thread _owner;
  
  public Lock(String id)
  {
    _id = id;
  }

  /**
   * Returns the lock identifier.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Tries to get a read lock.
   *
   * @param timeout how long to wait for a timeout
   */
  void lockRead(Transaction xa, long timeout)
    throws LockTimeoutException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " lockRead (read:" + _readCount
		 + " write:" + _isWrite
		 + " try-write:" + _tryWriteCount + ")");
    }

    long start = Alarm.getCurrentTime();
    long expire = start + timeout;

    synchronized (this) {
      long now;

      while (true) {
	if (! _isWrite && _tryWriteCount == 0) {
	  _readCount++;
	  return;
	}

	long delta = expire - Alarm.getCurrentTime();

	if (delta < 0 || Alarm.isTest())
	  break;

	try {
	  wait(delta);
	} catch (InterruptedException e) {
	  throw new LockTimeoutException(e);
	}
      }

      if (! Alarm.isTest()) {
	printOwnerStack();
	Thread.dumpStack();
      }

      throw new LockTimeoutException(L.l("{0} lockRead timed out ({1}ms) read-count:{2} try-writers:{3} is-write:{4}",
					 this,
					 Alarm.getCurrentTime() - start,
					 _readCount,
					 _tryWriteCount,
					 _isWrite));
    }
  }

  /**
   * Clears a read lock.
   */
  void unlockRead()
    throws SQLException
  {
    synchronized (this) {
      _readCount--;
    
      if (_readCount < 0)
	Thread.dumpStack();

      if (log.isLoggable(Level.FINEST)) {
	log.finest(this + " unlockRead (read:" + _readCount
		   + " write:" + _isWrite
		   + " try-write:" + _tryWriteCount + ")");
      }


      notifyAll();
    }
  }

  /**
   * Tries to get a write lock.
   *
   * @param timeout how long to wait for a timeout
   */
  void lockReadAndWrite(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " lockReadAndWrite (read:" + _readCount
		 + " write:" + _isWrite
		 + " try-write:" + _tryWriteCount + ")");
    }

    long start = Alarm.getCurrentTime();
    long expire = start + timeout;
    boolean isOkay = false;

    synchronized (this) {
      _tryWriteCount++;

      // XXX: temp debug only
      if (_owner == null)
	_owner = Thread.currentThread();

      try {
	while (true) {
	  if (! _isWrite && _readCount == _tryUpgradeCount) {
	    _readCount++;
	    _isWrite = true;
	    _owner = Thread.currentThread();
	    return;
	  }

	  long delta = expire - Alarm.getCurrentTime();

	  if (delta < 0 || Alarm.isTest())
	    break;

	  try {
	    wait(delta);
	  } catch (InterruptedException e) {
	    throw new LockTimeoutException(e);
	  }
	}

	if (! Alarm.isTest()) {
	  printOwnerStack();
	  Thread.dumpStack();
	}
	
	throw new LockTimeoutException(L.l("{0} lockReadAndWrite timed out ({1}ms) readers:{2} is-write:{3} try-writers:{4} try-upgrade:{5}",
					   this,
					   (Alarm.getCurrentTime() - start),
					   _readCount,
					   _isWrite,
					   _tryWriteCount,
					   _tryUpgradeCount));
      } finally {
	_tryWriteCount--;

	notifyAll();
      }
    }
  }

  /**
   * Tries to get a write lock, but does not wait if other threads are
   * reading or writing.  insert() uses this call to avoid blocking when
   * allocating a new row.
   *
   * @return true if the write was successful
   */
  public boolean lockReadAndWriteNoWait()
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " lockReadAndWriteNoWait (read:" + _readCount
		 + " write:" + _isWrite
		 + " try-write:" + _tryWriteCount + ")");
    }

    synchronized (this) {
      // XXX: temp debug only
      if (_owner == null)
	_owner = Thread.currentThread();

      if (_readCount == 0 && ! _isWrite) {
	_owner = Thread.currentThread();
	_readCount++;
	_isWrite = true;
	return true;
      }
    }

    return false;
  }

  /**
   * Clears a write lock.
   */
  void unlockReadAndWrite()
    throws SQLException
  {
    synchronized (this) {
      _readCount--;
      _isWrite = false;
      _owner = null;

      notifyAll();
    }
    
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " unlockReadAndWrite (read:" + _readCount
		 + " write:" + _isWrite
		 + " try-write:" + _tryWriteCount + ")");
    }
  }

  /**
   * Tries to get a write lock when already have a read lock.
   *
   * @param timeout how long to wait for a timeout
   */
  void lockWrite(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " lockWrite (read:" + _readCount
		 + " write:" + _isWrite
		 + " try-write:" + _tryWriteCount + ")");
    }

    long start = Alarm.getCurrentTime();
    long expire = start + timeout;

    synchronized (this) {
      _tryWriteCount++;
      _tryUpgradeCount++;

      // XXX: temp debug only
      if (_owner == null)
	_owner = Thread.currentThread();

      try {
	while (true) {
	  if (! _isWrite && _readCount == _tryUpgradeCount) {
	    _isWrite = true;
	    _owner = Thread.currentThread();
	    return;
	  }

	  long delta = expire - Alarm.getCurrentTime();

	  if (delta < 0 || Alarm.isTest())
	    break;

	  try {
	    wait(delta);
	  } catch (InterruptedException e) {
	    throw new LockTimeoutException(e);
	  }
	}

	if (! Alarm.isTest()) {
	  printOwnerStack();
	  Thread.dumpStack();
	}
	
	throw new LockTimeoutException(L.l("{0} lockWrite timed out ({1}ms) readers:{2} try-writers:{3} upgrade:{4}",
					   this,
					   Alarm.getCurrentTime() - start,
					   _readCount,
					   _tryWriteCount,
					   _tryUpgradeCount));
      } finally {
	_tryWriteCount--;
	_tryUpgradeCount--;

	notifyAll();
      }
    }
  }

  /**
   * Clears a write lock.
   */
  void unlockWrite()
    throws SQLException
  {
    synchronized (this) {
      _isWrite = false;
      _owner = null;

      notifyAll();
    }
    
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " unlockWrite (read:" + _readCount
		 + " write:" + _isWrite
		 + " try-write:" + _tryWriteCount + ")");
    }
  }

  /**
   * Waits until all the writers drain before committing, see Block.commit()
   */
  void waitForCommit()
  {
    Thread.yield();
    
    synchronized (this) {
      while (true) {
	if (! _isWrite && _tryWriteCount == 0) {
	  return;
	}

	if (Alarm.isTest())
	  return;

	try {
	  wait(1000L);
	} catch (InterruptedException e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      }
    }
  }

  private void printOwnerStack()
  {
    Thread thread = _owner;

    if (thread == null)
      return;

    System.out.println("Owner-stack");
    StackTraceElement []stack = thread.getStackTrace();
    for (int i = 0; i < stack.length; i++)
      System.out.println(stack[i]);
  }
  
  public String toString()
  {
    return "Lock[" + _id + "]";
  }
}
