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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Locking for tables/etc.
 */
public final class Lock {
  private final static L10N L = new L10N(Lock.class);
  private final static Logger log
    = Logger.getLogger(Lock.class.getName());

  private static final long WRITE_LOCK = 1L << 32;
  private static final long READ_LOCK = 1L;

  private final String _id;

  private final AtomicLong _lockCount = new AtomicLong();

  private final Object _lock = new Object();
  
  private LockNode _lockHead;
  private LockNode _lockTail;
  private boolean _isWake;
  
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
  public void lockRead(long timeout)
    throws LockTimeoutException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " lockRead 0x" + Long.toHexString(_lockCount.get()));
    }

    long lock = 0;
    
    // add to the read lock
    for (lock = _lockCount.get();
         ! _lockCount.compareAndSet(lock, lock + READ_LOCK);
         lock = _lockCount.get()) {
    }
    
    if (lock < WRITE_LOCK) {
      return; // read-only locks
    }

    lock(timeout, READ_LOCK);
  }

  /**
   * Clears a read lock.
   */
  public void unlockRead()
    throws SQLException
  {
    long lock = 0;
    
    // release the read lock
    for (lock = _lockCount.get();
         ! _lockCount.compareAndSet(lock, lock - READ_LOCK);
         lock = _lockCount.get()) {
    }

    unlock();
  }

  /**
   * Tries to get a write lock.
   *
   * @param timeout how long to wait for a timeout
   */
  public void lockReadAndWrite(long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " lockReadAndWrite "
                 + "0x" + Long.toHexString(_lockCount.get()));
    }

    long lock = 0;
    
    // add to the read lock
    for (lock = _lockCount.get();
         ! _lockCount.compareAndSet(lock, lock + (READ_LOCK|WRITE_LOCK));
         lock = _lockCount.get()) {
    }

    if (lock == 0) {
      return; // if only locker, return
    }
    
    lock(timeout, READ_LOCK|WRITE_LOCK);
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
      log.finest(this + " lockReadAndWriteNoWait "
                 + "0x" + Long.toHexString(_lockCount.get()));
    }

    return _lockCount.compareAndSet(0, READ_LOCK|WRITE_LOCK);
  }

  /**
   * Clears a write lock.
   */
  public void unlockReadAndWrite()
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " unlockReadAndWrite "
                 + "0x" + Long.toHexString(_lockCount.get()));
    }

    long lock = 0;
    
    // release the read lock
    for (lock = _lockCount.get();
         ! _lockCount.compareAndSet(lock, lock - (READ_LOCK|WRITE_LOCK));
         lock = _lockCount.get()) {
    }

    unlock();
  }

  /**
   * Tries to get a write lock when already have a read lock.
   *
   * @param timeout how long to wait for a timeout
   */
  /*
  void lockWrite(Transaction xa, long timeout)
    throws SQLException
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " lockWrite "
                 "0x" + + Long.toHexString(_lockCount.get()));
    }

    long lock = 0;
    
    // add to the read lock
    for (lock = _lockCount.get();
         ! _lockCount.compareAndSet(lock, lock + WRITE_LOCK);
         lock = _lockCount.get()) {
    }

    if (lock == READ_LOCK) {
      return; // if only locker, return
    }
    
    lock(timeout, WRITE_LOCK);
  }
  */

  /**
   * Clears a write lock.
   */
  /*
  void unlockWrite()
    throws SQLException
  {
    long lock = 0;
    
    // add to the read lock
    for (lock = _lockCount.get();
         ! _lockCount.compareAndSet(lock, lock - WRITE_LOCK);
         lock = _lockCount.get()) {
    }

    unlock();
  }
  */

  /**
   * Waits until all the writers drain before committing, see Block.commit()
   */
  void waitForCommit()
  {
  }

  /*
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
  */

  private void lock(long timeout, long value)
    throws LockTimeoutException
  {
    long expires = Alarm.getCurrentTime() + timeout;

    LockNode node = new LockNode();

    synchronized (_lock) {
      if (_lockTail != null) {
        _lockTail.setNext(node);
        _lockTail = node;
      }
      else {
        _lockHead = node;
        _lockTail = node;
      }
    }

    while (true) {
      synchronized (_lock) {
        while (! _lockHead.isValid()) {
          _lockHead = _lockHead.getNext();
        }
        
        if (_lockHead == node) {
          return;
        }
      }

      if (expires < Alarm.getCurrentTime()) {
        node.setValid(false);

        long lock;
        for (lock = _lockCount.get();
             ! _lockCount.compareAndSet(lock, lock - value);
             lock = _lockCount.get()) {
        }

        unlock();

        throw new LockTimeoutException(L.l("{0} lock timed out ({1}ms) 0x{2}",
                                           this, timeout, Long.toHexString(_lockCount.get())));
      }

      long delta = expires - Alarm.getCurrentTime();
      if (delta > 0) {
        try {
          Thread.interrupted();
          LockSupport.parkNanos(delta * 1000000L);
        } catch (Exception e) {
        }
      }
    }
  }

  private void unlock()
  {
    if (_lockHead == null)
      return;
    
    Thread thread = Thread.currentThread();
    Thread nextThread = null;

    synchronized (_lock) {
      LockNode node;
      
      for (node = _lockHead; node != null; node = _lockHead) {
        if (! node.isValid() || node.getThread() == thread) {
          _lockHead = node.getNext();

          if (_lockHead == null)
            _lockTail = null;
        }
        else {
          nextThread = node.getThread();
          break;
        }
      }
    }

    if (nextThread != null) {
      LockSupport.unpark(nextThread);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }

  static final class LockNode {
    private LockNode _next;
    private Thread _thread;
    private boolean _isValid = true;

    LockNode()
    {
      _thread = Thread.currentThread();
    }

    public final Thread getThread()
    {
      return _thread;
    }

    public LockNode getNext()
    {
      return _next;
    }

    public void setNext(LockNode next)
    {
      _next = next;
    }

    public boolean isValid()
    {
      return _isValid;
    }

    public void setValid(boolean isValid)
    {
      _isValid = isValid;
    }
  }
}
