/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.db.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * Locking for tables/etc.
 */
public final class DatabaseLock implements ReadWriteLock {
  private final static L10N L = new L10N(DatabaseLock.class);
  private final static Logger log
    = Logger.getLogger(DatabaseLock.class.getName());
  
  private final static 
  AtomicLongFieldUpdater<DatabaseLock> _lockCountUpdater;
  private final static 
  AtomicReferenceFieldUpdater<DatabaseLock,LockNode> _headUpdater;
  private final static 
  AtomicReferenceFieldUpdater<LockNode,LockNode> _nextUpdater;

  private static final long NODE_LOCK = 1L << 32;
  private static final long NODE_LOCK_MASK = 0xffffffffL << 32;
  private static final long READ = 1L;
  private static final long READ_MASK = 0xffffffffL;

  private final String _id;
  
  private final Lock _readLock = new ReadLockImpl();
  private final Lock _writeLock = new WriteLockImpl();

  private volatile long _lockCount;
  
  private volatile LockNode _lockHead;

  public DatabaseLock(String id)
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
  
  @Override
  public Lock readLock()
  {
    return _readLock;
  }
  
  @Override
  public Lock writeLock()
  {
    return _writeLock;
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
      log.finest(this + " lockRead 0x" + Long.toHexString(_lockCount));
    }

    long lock;

    while (true) {
      lock = _lockCount;
      
      if (lock < NODE_LOCK) {
        if (_lockCountUpdater.compareAndSet(this, lock, lock + READ)) {
          return;
        }
      }
      else {
        addReadLock(timeout);
        return;
      }
    }
  }
   
  private void addReadLock(long timeout)
  {
    long expires = Alarm.getCurrentTimeActual() + timeout;

    LockNode node = new LockNode(true);
    LockNode head;
    
    do {
      head = _lockHead;
      
      node.setNext(head);
    } while (! _headUpdater.compareAndSet(this, head, node));

    long lock;

    do {
      lock = _lockCount;
    } while (! _lockCountUpdater.compareAndSet(this, lock, lock + NODE_LOCK));

    try {
      node.park(expires);
    } finally {
      long nextLock;
    
      do {
        lock = _lockCount;
      
        nextLock = lock - NODE_LOCK + READ;
      } while (! _lockCountUpdater.compareAndSet(this, lock, nextLock));
    
      LockNode next = node.getNext();
    
      if (next != null)
        next.unpark();
    }
  }

  /**
   * Clears a read lock.
   */
  public void unlockRead()
  {
    long lock;

    do {
      lock = _lockCount;
    } while (! _lockCountUpdater.compareAndSet(this, lock, lock - READ));

    if ((lock & READ_MASK) == 1 && (lock & NODE_LOCK_MASK) != 0) {
      LockNode node = popNextNode();
      
      if (node != null)
        node.unpark();
    }
  }
  
  public void lockReadAndWrite(long timeout)
  {
    long expires = Alarm.getCurrentTimeActual() + timeout;

    long lock;

    do {
      lock = _lockCount;
    } while (! _lockCountUpdater.compareAndSet(this, lock, lock + NODE_LOCK));
    
    if (lock == 0) {
      return;
    }

    LockNode node = new LockNode(false);
    LockNode head;
    
    do {
      head = _lockHead;
      
      node.setNext(head);
    } while (! _headUpdater.compareAndSet(this, head, node));
    
    long currentLock = _lockCount;
    
    if ((lock & NODE_LOCK_MASK) == 0 && (currentLock & READ_MASK) == 0) {
      LockNode popNode = popNextNode();
      
      assert(node == popNode);
      return;
    }

    boolean isValid = false;
    
    try {
      node.park(expires);
      isValid = true;
    } finally {
      if (! isValid) {
        long nextLock;
    
        do {
          lock = _lockCount;
      
          nextLock = lock - NODE_LOCK;
        } while (! _lockCountUpdater.compareAndSet(this, lock, nextLock));
      }
    }
  }

  /**
   * Clears a read and write lock.
   */
  public void unlockReadAndWrite()
  {
    long lock;

    do {
      lock = _lockCount;
    } while (! _lockCountUpdater.compareAndSet(this, lock, lock - NODE_LOCK));
    
    LockNode node = popNextNode();

    if (node != null)
      node.unpark();
  }

  /**
   * Tries to get a write lock, but does not wait if other threads are
   * reading or writing.  insert() uses this call to avoid blocking when
   * allocating a new row.
   *
   * @return true if the write was successful
   */
  public boolean lockReadAndWriteNoWait()
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " lockReadAndWriteNoWait "
                 + "0x" + Long.toHexString(_lockCount));
    }

    if (_lockCountUpdater.compareAndSet(this, 0, NODE_LOCK))
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
  
  private LockNode popNextNode()
  {
    LockNode head;
    
    while (true) {
      head = _lockHead;
      
      if (head == null)
        return null;
      
      LockNode first = null;
      LockNode firstPrev = null;
      
      LockNode ptrPrev = null;
      
      LockNode ptr = head;
      for (; ptr != null; ptr = ptr.getNext()) {

        if (first == null || ! ptr.isRead() || ! first.isRead()) {
          first = ptr;
          firstPrev = ptrPrev;
        }
        
        ptrPrev = ptr;
      }
      
      if (head == first) {
        if (_headUpdater.compareAndSet(this, head, null)) {
          return first;
        }
      }
      else {
        if (_nextUpdater.compareAndSet(firstPrev, first, null))
          return first;
      }
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

    public volatile LockNode _next;

    private volatile boolean _isDead;
    private volatile boolean _isWake;

    LockNode(boolean isRead)
    {
      _thread = Thread.currentThread();
      _isRead = isRead;
    }

    LockNode getNext()
    {
      return _next;
    }

    void setNext(LockNode next)
    {
      _next = next;
    }

    boolean isRead()
    {
      return _isRead;
    }

    public void park(long expires)
    {
      while (! _isWake) {
        try {
          Thread.interrupted();

          LockSupport.parkUntil(expires);
          
          if (! _isWake && expires < Alarm.getCurrentTimeActual()) {
            _isDead = true;
            throw new LockTimeoutException();
          }
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void unpark()
    {
      _isWake = true;
      LockSupport.unpark(_thread);
    }
  }
  
  class ReadLockImpl implements Lock {
    @Override
    public boolean tryLock(long timeout, TimeUnit unit)
        throws InterruptedException
    {
      lockRead(unit.toMillis(timeout));
      
      return true;
    }

    @Override
    public void unlock()
    {
      unlockRead();
    }
    
    @Override
    public void lock()
    {
      try {
        if (! tryLock(Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS)) {
          throw new IllegalStateException();
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public boolean tryLock()
    {
      try {
        return tryLock(Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public Condition newCondition()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
  
  class WriteLockImpl implements Lock {
    @Override
    public boolean tryLock(long time, TimeUnit unit)
        throws InterruptedException
    {
      lockReadAndWrite(unit.toMillis(time));
      
      return true;
    }

    @Override
    public void unlock()
    {
      unlockReadAndWrite();
    }
    
    @Override
    public void lock()
    {
      try {
        if (! tryLock(Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS)) {
          throw new IllegalStateException();
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public boolean tryLock()
    {
      try {
        return tryLock(Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public Condition newCondition()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
  
  static {
    _lockCountUpdater
      = AtomicLongFieldUpdater.newUpdater(DatabaseLock.class, "_lockCount");
    
    _headUpdater
      = AtomicReferenceFieldUpdater.newUpdater(DatabaseLock.class, 
                                               LockNode.class, 
                                               "_lockHead");
    
    _nextUpdater
      = AtomicReferenceFieldUpdater.newUpdater(LockNode.class, 
                                               LockNode.class, 
                                               "_next");
  }
}
