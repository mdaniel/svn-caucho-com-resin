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
public final class Lock {
  private final static L10N L = new L10N(Lock.class);
  private final static Logger log = Log.open(Lock.class);

  private final String _id;

  // count of threads trying to get a write lock
  private int _tryWriteCount;
  
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

    long expire = Alarm.getCurrentTime() + timeout;

    synchronized (this) {
      long now;

      do {
	if (! _isWrite && _tryWriteCount == 0) {
	  _readCount++;
	  return;
	}

	if (Alarm.isTest())
	  break;

	try {
	  wait(expire - Alarm.getCurrentTime());
	} catch (InterruptedException e) {
	  throw new LockTimeoutException(e);
	}
      } while (Alarm.getCurrentTime() <= expire);

      Thread.dumpStack();
      throw new LockTimeoutException(L.l("Lock {0} timed out ({1}ms) try-writers:{2} is-write:{3}",
					 this, timeout,
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

      try {
	do {
	  if (! _isWrite && _readCount == 0) {
	    _readCount++;
	    _isWrite = true;
	    _owner = Thread.currentThread();
	    return;
	  }

	  if (Alarm.isTest())
	    break;

	  try {
	    wait(expire - Alarm.getCurrentTime());
	  } catch (InterruptedException e) {
	    throw new LockTimeoutException(e);
	  }
	} while (Alarm.getCurrentTime() < expire);

	Thread.dumpStack();
	throw new LockTimeoutException(L.l("{0} lockReadAndWrite timed out ({1}ms) try-writers:{2}",
					   this,
					   (Alarm.getCurrentTime() - start),
					   _tryWriteCount));
      } finally {
	_tryWriteCount--;

	notifyAll();
      }
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

    long expire = Alarm.getCurrentTime() + timeout;
    boolean isOkay = false;

    synchronized (this) {
      _tryWriteCount++;

      try {
	do {
	  if (! _isWrite && _readCount == 1) {
	    _isWrite = true;
	    return;
	  }

	  if (Alarm.isTest())
	    break;

	  try {
	    wait(expire - Alarm.getCurrentTime());
	  } catch (InterruptedException e) {
	    throw new LockTimeoutException(e);
	  }
	} while (Alarm.getCurrentTime() < expire);

	Thread.dumpStack();
	throw new LockTimeoutException(L.l("{0} lockWrite timed out ({1}ms) try-writers:{2}",
					   this,
					   timeout,
					   _tryWriteCount));
      } finally {
	_tryWriteCount--;

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
      _readCount--;
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
  
  public String toString()
  {
    return "Lock[" + _id + "]";
  }
}
