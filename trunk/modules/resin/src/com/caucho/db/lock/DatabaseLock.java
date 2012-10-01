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

package com.caucho.db.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;

/**
 * Locking for tables/etc.
 */
public final class DatabaseLock implements ReadWriteLock {
  private final static 
    AtomicLongFieldUpdater<DatabaseLock> _lockCountUpdater;

  private static final long LOCK_WRITE = 1L << 60;
  private static final long LOCK_WRITE_MASK = 1L << 60;
  
  private static final long LOCK_WRITE_WAIT = 1L << 40;
  private static final long LOCK_WRITE_WAIT_MASK = 0xfffffL << 40;
  
  private static final long LOCK_READ_WAIT = 1L << 20;
  private static final long LOCK_READ_WAIT_MASK = 0xfffffL << 20;
  
  private static final long LOCK_READ = 1L;
  private static final long LOCK_READ_MASK = 0xfffffL;
  
  private static final long LOCK_MASK = LOCK_WRITE_MASK | LOCK_READ_MASK;
  private static final long LOCK_WAIT_MASK
    = LOCK_READ_WAIT_MASK|LOCK_WRITE_WAIT_MASK;
  
  private final Lock _readLock = new ReadLockImpl();
  private final Lock _writeLock = new WriteLockImpl();
  
  private volatile long _lockCount;

  public DatabaseLock(String id)
  {
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
  /*
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
  */
  
  public void lockRead(long timeout)
    throws LockTimeoutException
  {
    if (lockReadCounter())
      return;
    
    long expires = CurrentTime.getCurrentTimeActual() + timeout;
    
    boolean isValid = false;
    
    try {
      lockReadWait(expires);
      isValid = true;
    } finally {
      if (! isValid)
        unlockReadWait();
    }
  }
  
  /**
   * Attempts to get an exclusive write lock.
   */
  public void lockWrite(long timeout)
  {
    if (lockWriteCounter())
      return;
    
    long expires = CurrentTime.getCurrentTimeActual() + timeout;
    
    boolean isValid = false;
    
    try {
      lockWriteWait(expires);
      isValid = true;
    } finally {
      if (! isValid)
        unlockWriteWait();
    }
  }
 
  public void unlockRead()
  {
    unlockReadCounter();
  }

  /**
   * Unlocks the write
   */
  public void unlockWrite()
  {
    unlockWriteCounter();
  }
  
  private boolean lockReadCounter()
  {
    long lock;
    long newLock;
    boolean isQuickLock;
    
    do {
      lock = _lockCount;
      
      isQuickLock = isLockCanReadQuick(lock);
      
      if (isQuickLock)
        newLock = lock + LOCK_READ;
      else
        newLock = lock + LOCK_READ_WAIT;
    } while (! _lockCountUpdater.compareAndSet(this, lock, newLock));
    
    return isQuickLock;
  }
  
  private boolean lockWriteCounter()
  {
    long lock;
    long newLock;
    boolean isQuickLock;
    
    do {
      lock = _lockCount;
      
      isQuickLock = isLockCanWriteQuick(lock);
      
      if (isQuickLock)
        newLock = lock + LOCK_WRITE;
      else
        newLock = lock + LOCK_WRITE_WAIT;
    } while (! _lockCountUpdater.compareAndSet(this, lock, newLock));
    
    return isQuickLock;
  }
  
  private void unlockReadWait()
  {
    long lock;
    long newLock;
    
    do {
      lock = _lockCount;
      newLock = lock - LOCK_READ_WAIT;
    } while (! _lockCountUpdater.compareAndSet(this, lock, newLock));
  }
  
  private void unlockWriteWait()
  {
    long lock;
    long newLock;
    
    do {
      lock = _lockCount;
      newLock = lock - LOCK_WRITE_WAIT;
    } while (! _lockCountUpdater.compareAndSet(this, lock, newLock));
  }
  
  private void unlockReadCounter()
  {
    long lock;
    long newLock;
    
    boolean isWaiter;
    
    do {
      lock = _lockCount;
      
      isWaiter = ((lock & LOCK_WAIT_MASK) != 0);
      
      newLock = lock - LOCK_READ;
    } while (! _lockCountUpdater.compareAndSet(this, lock, newLock));

    if (isWaiter) {
      synchronized (this) {
        notify();
      }
    }
  }
  
  private void unlockWriteCounter()
  {
    long lock;
    long newLock;
    
    boolean isWaiter;
    
    do {
      lock = _lockCount;
      
      isWaiter = ((lock & LOCK_WAIT_MASK) != 0);
      
      newLock = lock - LOCK_WRITE;
    } while (! _lockCountUpdater.compareAndSet(this, lock, newLock));

    if (isWaiter) {
      synchronized (this) {
        notify();
      }
    }
  }
  
  private void lockReadWait(long expires)
  {
    synchronized (this) {
      while (true) {
        long lock;

        while (isLockCanRead(lock = _lockCount)) {
          long newLock = lock + LOCK_READ - LOCK_READ_WAIT;
          
          if (_lockCountUpdater.compareAndSet(this, lock, newLock)) {
            return;
          }
        }

        long delta = expires - CurrentTime.getCurrentTimeActual();
        
        if (delta <= 0) {
          throw new LockTimeoutException();
        }
        
        try {
          wait(delta);
        } catch (Exception e) {
        }
      }
    }
  }
  
  private void lockWriteWait(long expires)
  {
    synchronized (this) {
      while (true) {
        long lock;

        while (isLockCanWrite(lock = _lockCount)) {
          long newLock = lock + LOCK_WRITE - LOCK_WRITE_WAIT;
          
          if (_lockCountUpdater.compareAndSet(this, lock, newLock)) {
            return;
          }
        }

        long delta = expires - CurrentTime.getCurrentTimeActual();
        
        if (delta <= 0) {
          throw new LockTimeoutException("write timeout 0x" + Long.toHexString(lock));
        }
        
        try {
          wait(delta);
        } catch (Exception e) {
        }
      }
    }
  }
  
  private static boolean isLockCanRead(long lock)
  {
    return ((lock & LOCK_MASK) == 0)
            || (lock & (LOCK_WRITE_MASK|LOCK_WRITE_WAIT_MASK)) == 0;
  }
  
  private static boolean isLockCanWrite(long lock)
  {
    return (lock & LOCK_MASK) == 0;
  }
  
  private static boolean isLockCanReadQuick(long lock)
  {
    return ((lock & LOCK_MASK) == 0)
            || (lock & (LOCK_WRITE_MASK|LOCK_WRITE_WAIT_MASK)) == 0;
  }
  
  private static boolean isLockCanWriteQuick(long lock)
  {
    return lock == 0;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
  
  enum LockType {
    READ,
    WRITE
  };
  
  final class ReadLockImpl implements Lock {
    @Override
    public final boolean tryLock(long timeout, TimeUnit unit)
        throws InterruptedException
    {
      lockRead(unit.toMillis(timeout));
      
      return true;
    }

    @Override
    public final void unlock()
    {
      unlockRead();
    }
    
    @Override
    public final void lock()
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
      lockWrite(unit.toMillis(time));
      
      return true;
    }

    @Override
    public void unlock()
    {
      unlockWrite();
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
  }
}
