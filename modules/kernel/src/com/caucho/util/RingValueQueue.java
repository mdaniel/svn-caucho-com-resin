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

package com.caucho.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.caucho.env.thread.TaskWorker;
import com.caucho.util.L10N;

/**
 * Value queue with atomic reference.
 */
public final class RingValueQueue<M>
  extends AbstractActorQueue<M>
{
  private static final L10N L = new L10N(RingValueQueue.class);

  private final RingValueArray<M> _ring;

  // private final RingUnsafeArray<T> _ring;
  private final int _capacity;

  private final AtomicLong _headAlloc = new AtomicLong();

  private final AtomicLong _tail = new AtomicLong();

  private final RingBlocker _blocker;

  private volatile boolean _isWriteClosed;

  public RingValueQueue(int capacity)
  {
    this(capacity, new RingBlockerBasic());
  }

  public RingValueQueue(int capacity,
                        RingBlocker blocker)
  {
    if (Integer.bitCount(capacity) != 1 || capacity < 2) {
      throw new IllegalArgumentException(L.l("Invalid ring capacity {0}",
                                             Long.toHexString(capacity)));
    }

    if (blocker == null) {
      throw new NullPointerException(L.l("RingBlocker is required"));
    }

    _capacity = capacity;

    _ring = new RingValueArray<M>(capacity);
    // _ring = new RingUnsafeArray<T>(capacity);

    _blocker = blocker;
  }

  public int getCapacity()
  {
    return _capacity;
  }

  @Override
  public int getOfferReserve()
  {
    return _capacity / 2;
  }
  
  public int remainingCapacity()
  {
    return _capacity - size() - 1;
  }

  @Override
  public final boolean isEmpty()
  {
    return _headAlloc.get() == _tail.get();
  }

  @Override
  public final int size()
  {
    long head = _headAlloc.get();
    long tail = _tail.get();

    return (int) (head - tail);
  }

  public final long getHead()
  {
    return _headAlloc.get();
  }

  public final long getHeadAlloc()
  {
    return _headAlloc.get();
  }

  public final long getTail()
  {
    return _tail.get();
  }

  @Override
  public TaskWorker getOfferTask()
  {
    return _blocker;
  }

  public final M getValue(long ptr)
  {
    return get(ptr);
  }

  private final M get(long ptr)
  {
    return _ring.get(ptr);
  }

  @Override
  public final boolean offer(final M value,
                             final long timeout,
                             final TimeUnit unit)
  {
    return offer(value, timeout, unit, 0);
  }

  @Override
  public final boolean offer(final M value,
                             final long timeout,
                             final TimeUnit unit,
                             final int reservedSpace)
  {
    if (value == null) {
      throw new NullPointerException();
    }

    final AtomicLong headRef = _headAlloc;
    final AtomicLong tailRef = _tail;
    final int capacity = _capacity;

    while (true) {
      final long tail = tailRef.get();
      final long head = headRef.get();
      final long nextHead = head + 1;

      if (capacity <= nextHead - tail + reservedSpace) {
        if (! _blocker.offerWait(tail, tailRef, reservedSpace, timeout, unit)) {
          return false;
        }
      }
      else if (headRef.compareAndSet(head, nextHead)) {
        _ring.set(head, value);

        return true;
      }
    }
  }

  @Override
  public final M poll(long timeout, TimeUnit unit)
  {
    // final AtomicLong tailAllocRef = _tailAlloc;                              
    final AtomicLong headRef = _headAlloc;
    final AtomicLong tailRef = _tail;

    final RingValueArray<M> ring = _ring;

    final RingBlocker blocker = _blocker;

    while (true) {
      final long tail = tailRef.get();
      final long head = headRef.get();

      M value;

      if (tail == head) {
        blocker.offerWake();

        if (timeout <= 0 || ! blocker.pollWait(timeout, unit)) {
          return null;
        }
      }
      else if ((value = ring.pollAndClear(tail)) != null) {
        if (tailRef.compareAndSet(tail, tail + 1)) {
          blocker.offerWake();

          return value;
        }
        else {
          // otherwise backout the value
          ring.set(tail, value);
        }
      }
    }
  }

  @Override
  public final M peek()
  {
    while (true) {
      long head = _headAlloc.get();
      long tail = _tail.get();
      
      if (head <= tail) {
        return null;
      }
      
      M value = get(tail);
      
      if (value != null) {
        return value;
      }
    }
  }

  public void wake()
  {
  }

  public final boolean isWriteClosed()
  {
    return _isWriteClosed;
  }

  public final void closeWrite()
  {
    _isWriteClosed = true;

    _blocker.offerWake();
    _blocker.pollWake();
  }

  public final void close()
  {
    closeWrite();

    _blocker.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getCapacity() + "]";
  }
}
