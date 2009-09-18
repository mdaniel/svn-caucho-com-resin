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

  private static final long WRITE_LOCK = 1L << 48;
  private static final long WRITE = 1L << 32;
  private static final long READ_LOCK = 1L << 16;
  private static final long READ_LOCK_MASK = 0xffff0000L;
  private static final long READ = 1L;

  private static final long RW_LOCK = WRITE_LOCK|WRITE|READ_LOCK|READ;

  private final String _id;

  private final Object _lock = new Object();
  
  private final AtomicLong _lockCount = new AtomicLong();
  private final AtomicLong _activeLockCount = new AtomicLong();
  
  private LockNode _lockHead;
  private LockNode _lockTail;
  
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

    long lock;

    while (true) {
      lock = _lockCount.get();
      
      if (lock < WRITE) {
        if (_lockCount.compareAndSet(lock, lock + (READ|READ_LOCK)))
          return;
      }
      else if (_lockCount.compareAndSet(lock, lock + READ))
        break;
    }

    LockNode node = null;
    
    synchronized (_lock) {
      lock = _lockCount.get();

      if (lock < WRITE) {
        addLock(READ_LOCK);
        return;
      }

      //System.out.println("QR: " + this + " " + Long.toHexString(lock) + " " + _activeLockCount);
      node = queueLock(true);
    }
    
    long expires = Alarm.getCurrentTime() + timeout;
    lock(node, timeout, expires, READ);
  }

  /**
   * Clears a read lock.
   */
  public void unlockRead()
    throws SQLException
  {
    LockNode unparkNode = null;

    long lock;

    do {
      lock = _lockCount.get();

      // if read-only, or if not the last read-lock, unlock and exit
      if ((lock < WRITE
           || (lock < WRITE_LOCK && (lock & READ_LOCK_MASK) != READ_LOCK))
          && _lockCount.compareAndSet(lock, lock - (READ|READ_LOCK))) {
        return;
      }
    } while (lock < WRITE
             || (lock < WRITE_LOCK && (lock & READ_LOCK_MASK) != READ_LOCK));
    
    synchronized (_lock) {
      unparkNode = unlock();

      addLock(- (READ|READ_LOCK));
    }

    if (unparkNode != null)
      unparkNode(unparkNode);
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

    long lock;

    while (true) {
      lock = _lockCount.get();

      if (lock == 0) {
        if (_lockCount.compareAndSet(lock, lock + (WRITE|WRITE_LOCK)))
          return;
      }
      else
        break;
    }

    LockNode node = null;
    
    synchronized (_lock) {
      // mark the write lock
      while (true) {
        lock = _lockCount.get();
      
        if (lock == 0) {
          if (_lockCount.compareAndSet(lock, lock + (WRITE|WRITE_LOCK)))
            return;
        }
        else if (_lockCount.compareAndSet(lock, lock + WRITE))
          break;
      }

      //System.out.println("QW: " + this + " " + Long.toHexString(lock) + " " + _activeLockCount);
      node = queueLock(false);
    }
    
    long expires = Alarm.getCurrentTime() + timeout;
    lock(node, timeout, expires, WRITE);
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

    long lock = _lockCount.get();

    if (lock == (WRITE|WRITE_LOCK)
        && _lockCount.compareAndSet((WRITE|WRITE_LOCK), 0))
      return;

    LockNode unparkNode = null;

    synchronized (_lock) {
      unparkNode = unlock();
      
      addLock(- (WRITE|WRITE_LOCK));

      if (_lockCount.get() < WRITE && _lockHead != null)
        Thread.dumpStack();
    }

    if (unparkNode != null)
      unparkNode(unparkNode);
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

    if (_lockCount.compareAndSet(0, WRITE|WRITE_LOCK))
      return true;
    else
      return false;
  }

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

  /**
   * Queues for the lock, waiting until the write lock is available.
   *
   * Must be called from inside _lock
   */
  private LockNode queueLock(boolean isRead)
    throws LockTimeoutException
  {
    LockNode node = new LockNode(isRead);

    if (_lockTail != null) {
      _lockTail.setNext(node);
      _lockTail = node;
    }
    else {
      _lockHead = _lockTail = node;
    }

    return node;
  }

  /**
   * Queues for the lock, waiting until the write lock is available.
   *
   * Must be called from inside _lock
   */
  private void lock(LockNode node, long timeout, long expires, long value)
    throws LockTimeoutException
  {
    // System.out.println("LOCK: " + this + " " + Long.toHexString(_lockCount.get()));
    
    while (! node.isLock()) {
      long now = Alarm.getCurrentTime();

      if (expires < now) {
        node.setDead();
        addLock(- value);
          
        throw new LockTimeoutException(L.l("{0} lock timed out ({1}ms) 0x{2}",
                                           this, timeout,
                                           Long.toHexString(_lockCount.get())));
      }

      node.park(expires);
    }
  }

  private LockNode unlock()
  {
    LockNode head = _lockHead;

    LockNode ptr = head;
    LockNode tail = ptr;

    boolean isRead = false;
    boolean isWrite = false;
    
    for (; ptr != null; ptr = ptr.getNext()) {
      if (ptr.isDead()) {
        tail = ptr;
      }
      else if (isWrite) {
        break;
      }
      else if (ptr.isRead()) {
        isRead = true;

        addLock(READ_LOCK);

        tail = ptr;
      }
      else if (isRead) {
        break;
      }
      else {
        addLock(WRITE_LOCK);

        isWrite = true;
        tail = ptr;
      }
    }

    if (tail != null) {
      _lockHead = tail.getNext();
      if (_lockHead == null)
        _lockTail = null;
      
      tail.setNext(null);
    }
    else {
      _lockHead = _lockTail = null;
    }

    return head;
  }

  private long addLock(long value)
  {
    long lock;
    
    for (lock = _lockCount.get();
         ! _lockCount.compareAndSet(lock, lock + value);
         lock = _lockCount.get()) {
    }

    return lock;
  }
  
  private void unparkNode(LockNode node)
  {
    for (; node != null; node = node.getNext()) {
      if (! node.isDead())
        node.unpark();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }

  static final class LockNode {
    private final Thread _thread;
    private final boolean _isRead;
    
    private LockNode _next;
    
    private volatile boolean _isDead;
    private volatile boolean _isLock;

    LockNode(boolean isRead)
    {
      _thread = Thread.currentThread();
      _isRead = isRead;
    }

    public LockNode getNext()
    {
      return _next;
    }

    public void setNext(LockNode next)
    {
      _next = next;
    }

    public boolean isRead()
    {
      return _isRead;
    }

    public boolean isLock()
    {
      return _isLock;
    }

    public void setDead()
    {
      _isDead = true;
    }

    public boolean isDead()
    {
      return _isDead;
    }

    public void park(long expires)
    {
      try {
        if (_isLock)
          return;
        
        Thread.interrupted();
        
        if (Alarm.isTest())
          LockSupport.parkNanos(10000000L);
        else
          LockSupport.parkUntil(expires);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    public void unpark()
    {
      _isLock = true;

      LockSupport.unpark(_thread);
    }
  }
}
