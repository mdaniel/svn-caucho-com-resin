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
package com.caucho.config.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.IllegalLoopbackException;

import com.caucho.util.L10N;

/**
 * Utilities to manage locks.
 */
public class LockUtil {
  private static final L10N L = new L10N(LockUtil.class);
  
  public static void lockRead(Lock readLock, long timeout)
  {
    try {
      if (! readLock.tryLock(timeout, TimeUnit.MILLISECONDS))
        throw new ConcurrentAccessTimeoutException(L.l("Timed out acquiring read lock {0}ms.",
                                                       timeout));
    } catch (InterruptedException e) {
      throw new ConcurrentAccessTimeoutException(L.l("Thread interruption acquiring read lock: {0}", e.getMessage()));
    }
  }
  
  public static void lockWrite(ReentrantReadWriteLock lock)
  {
    if (lock.getReadHoldCount() > 0
        && lock.getWriteHoldCount() == 0) {
      throw new IllegalLoopbackException(L.l("Cannot attempt a nested write lock without an existing write lock."));
    }
    
    lock.writeLock().lock();
  }
  
  public static void lockWrite(ReentrantReadWriteLock lock,
                               long timeout)
  {
    if (lock.getReadHoldCount() > 0
        && lock.getWriteHoldCount() == 0) {
      throw new IllegalLoopbackException(L.l("Cannot attempt a nested write lock without an existing write lock."));
    }
    
    try {
      if (! lock.writeLock().tryLock(timeout, TimeUnit.MILLISECONDS))
        throw new ConcurrentAccessTimeoutException(L.l("Timed out acquiring write lock {0}ms.",
                                                       timeout));
    } catch (InterruptedException e) {
      throw new ConcurrentAccessTimeoutException(L.l("Thread interruption acquiring write lock: " + e.getMessage()));
    }
  }
}
