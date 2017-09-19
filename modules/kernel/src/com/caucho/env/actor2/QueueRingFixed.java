/*
 * Copyright (c) 1998-2017 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.env.actor2;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.caucho.env.actor.ActorProcessor;
import com.caucho.util.L10N;

/**
 * Value queue with atomic reference.
 */
public final class QueueRingFixed<M >
  extends QueueRingBase<M>
{
  private static final L10N L = new L10N(QueueRingFixed.class);

  private final ArrayRing<M> _ring;

  private final int _capacity;

  private final AtomicLong _head;
  private final AtomicLong _tail;

  private final RingBlocker _blocker;

  private volatile boolean _isWriteClosed;

  public QueueRingFixed(int capacity)
  {
    this(capacity, new RingBlockerBasic());
  }

  public QueueRingFixed(int capacity,
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

    ArrayRing<M> ring = null;

    // ring = RingValueArrayUnsafe.create(capacity);

    if (ring == null) {
      ring = new ArrayRingAtomic<M>(capacity);
    }

    _ring = ring;

    _head = new AtomicLong();
    _tail = new AtomicLong();

    _blocker = blocker;
  }

  public int getCapacity()
  {
    return _capacity;
  }

  /*
  @Override
  public int getOfferReserve()
  {
    return _capacity / 2;
  }
  */

  @Override
  public final boolean isEmpty()
  {
    return _head.get() == _tail.get();
  }

  @Override
  public final int size()
  {
    long head = _head.get();
    long tail = _tail.get();

    return (int) (head - tail);
  }

  @Override
  public int remainingCapacity()
  {
    return getCapacity() - size() - 1;
  }

  public final long head()
  {
    return _head.get();
  }

  public final long getHeadAlloc()
  {
    return _head.get();
  }

  public final long getTail()
  {
    return _tail.get();
  }

  public final long getTailAlloc()
  {
    return _tail.get();
  }

  /*
  @Override
  public WorkerOutbox<M> worker()
  {
    return _blocker;
  }
  */

  @Override
  public void wake()
  {
    _blocker.offerWake();
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
    if (value == null) {
      throw new NullPointerException();
    }

    final AtomicLong headRef = _head;
    final AtomicLong tailRef = _tail;
    final int capacity = _capacity;

    while (true) {
      final long tail = tailRef.get();
      final long head = headRef.get();
      final long nextHead = head + 1;

      if (capacity <= nextHead - tail) {
        long sequence = _blocker.nextOfferSequence();

        if (capacity <= headRef.get() + 1 - tailRef.get()
            && ! _blocker.offerWait(sequence, timeout, unit)) {
          // retest the capacity after the sequence is allocated because of
          // wake timing

          return false;
        }
      }
      else if (headRef.compareAndSet(head, nextHead)) {
        // _ring.setLazy(head, value);
        _ring.set(head, value);

        return true;
      }
    }
  }

  @Override
  public final M poll(long timeout, TimeUnit unit)
  {
    // final AtomicLong tailAllocRef = _tailAlloc;
    final AtomicLong headRef = _head;
    final AtomicLong tailRef = _tail;

    final ArrayRing<M> ring = _ring;

    final RingBlocker blocker = _blocker;

    while (true) {
      long tail = tailRef.get();
      final long head = headRef.get();

      M value;

      if (tail == head) {
        blocker.offerWake();

        if (timeout <= 0) {
          return null;
        }

        long pollSequence = blocker.nextPollSequence();

        if (tailRef.get() == headRef.get()
            && ! blocker.pollWait(pollSequence, timeout, unit)) {
          return null;
        }
      }
      else if ((value = ring.pollAndClear(tail)) != null) {
        if (tailRef.compareAndSet(tail, tail + 1)) {
          blocker.offerWake();

          return value;
        }
        else {
          ring.set(tail, value);
        }
      }
    }
  }

  @Override
  public final M peek()
  {
    long head = _head.get();
    long tailAlloc = _tail.get();

    if (tailAlloc < head) {
      return get(tailAlloc);
    }

    return null;
  }

  @Override
  public void deliver(final ActorProcessor<? super M> deliver)
    throws Exception
  {
    final int tailChunk = 64;
    final ArrayRing<M> ring = _ring;
    final AtomicLong headRef = _head;
    final AtomicLong tailRef = _tail;

    long head = headRef.get();
    long tail = tailRef.get();
    long lastTail = tail;

    try {
      while (tail < head) {
        long tailChunkEnd = Math.min(head, tail + tailChunk);

        while (tail < tailChunkEnd) {
          M item = ring.takeAndClear(tail);

          tail++;

          deliver.process(item);
        }

        tailRef.set(tail);
        lastTail = tail;

        // XXX: verify
        _blocker.offerWake();

        head = headRef.get();
      }
    } finally {
      if (tail != lastTail) {
        tailRef.set(tail);
      }

      _blocker.offerWake();
    }
  }

  public final boolean isWriteClosed()
  {
    return _isWriteClosed;
  }

  public final void pollWake()
  {
    _blocker.pollWake();
  }

  public final void closeWrite()
  {
    _isWriteClosed = true;

    _blocker.offerWake();
    _blocker.pollWake();
  }

  public final void shutdown()
  {
    closeWrite();

    // _blocker.shutdown(mode);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getCapacity() + "]";
  }
}
