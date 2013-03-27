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
  private final RingTailGetter<M> _tailGetter;
  private final RingNonTailGetter<M> _nonTailGetter;

  // private final RingUnsafeArray<T> _ring;
  private final int _capacity;

  private final AtomicLong _head;

  private final AtomicLong _tailAlloc = new AtomicLong();
  private final AtomicLong _tail;

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

    _tailGetter = new RingTailGetter<M>(_ring);
    _nonTailGetter = new RingNonTailGetter<M>(_ring);

    _head = new AtomicLong();
    _tail = new AtomicLong();

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
    return _head.get() == _tail.get();
  }

  @Override
  public final int size()
  {
    long head = _head.get();
    long tail = _tail.get();

    return (int) (head - tail);
  }

  public final long getHead()
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
    return _tailAlloc.get();
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

  private final M getAndClear(long ptr)
  {
    RingValueArray<M> ring = _ring;

    M item;

    while ((item = ring.getAndSet(ptr, null)) == null) {
    }

    return item;
  }

  private final boolean isSet(long ptr)
  {
    return _ring.get(ptr) != null;
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

    // completePoll();

    final AtomicLong headRef = _head;
    final AtomicLong tailRef = _tail;
    final int capacity = _capacity;

    while (true) {
      // final AtomicReferenceArray<T> ring = _ring;
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
  public final M peek()
  {
    long head = _head.get();
    long tailAlloc = _tailAlloc.get();

    if (tailAlloc < head) {
      return get(tailAlloc);
    }

    return null;
  }

  @Override
  public final M poll(long timeout, TimeUnit unit)
  {
    final AtomicLong tailAllocRef = _tailAlloc;
    final AtomicLong headRef = _head;

    final RingBlocker blocker = _blocker;

    while (true) {
      final long tailAlloc = tailAllocRef.get();
      final long head = headRef.get();

      if (tailAlloc == head) {
        // completeOffer();
        blocker.offerWake();

        if (timeout <= 0 || ! blocker.pollWait(timeout, unit)) {
          return null;
        }
      }
      else if (tailAllocRef.compareAndSet(tailAlloc, tailAlloc + 1)) {
        M value;

        do {
          value = getAndClear(tailAlloc);
        } while (value == null);

        if (! _tail.compareAndSet(tailAlloc, tailAlloc + 1)) {
          completePoll();
          // blocker.wake();
        }

        /*
        if (_capacity <= head - tailAlloc + 4) {
          blocker.wake();
        }
        */
        
        blocker.offerWake();

        return value;
      }
    }
  }

  private void completePoll()
  {
    final AtomicLong tailRef = _tail;
    final AtomicLong tailAllocRef = _tailAlloc;

    while (true) {
      final long tail = tailRef.get();
      final long tailAlloc = tailAllocRef.get();

      if (tail == tailAlloc) {
        return;
      }

      if (isSet(tail)) {
        return;
      }

      tailRef.compareAndSet(tail, tail + 1);
    }
  }

  /*
  @Override
  public void deliver(final Actor<M> actor,
                      final MessageContext<M> actorContext,
                      M chainItem)
    throws Exception
  {
    final ActorCounter headCounter = _head;
    final ActorCounter tailCounter = _tail;

    long initialTail = tailCounter.get();
    long tail = initialTail;
    long head = headCounter.get();

    try {
      do {
        tail = deliver(head, tail, actor, actorContext);

        if (chainItem != null) {
          M item = chainItem;
          chainItem = null;

          // actorContext.setContextMessage(item);
          actor.deliver(item, actorContext);
        }

        head = headCounter.get();
      } while (tail < head);
    } finally {
      _blocker.offerWake();

      if (chainItem != null) {
        offer(chainItem);
        wake();
      }
    }
  }
  */

  public void wake()
  {
  }

  /*
  private long deliver(long head,
                       long tail,
                       final Actor<M> actor,
                       final MessageContext<M> actorContext)
    throws Exception
  {
    final int tailChunk = 32;
    final RingValueArray<M> ring = _ring;
    final ActorCounter tailCounter = _tail;

    long lastTail = tail;

    try {
      while (tail < head) {
        long tailChunkEnd = Math.min(head, tail + tailChunk);

        while (tail < tailChunkEnd) {
          M item = ring.getAndSet(tail, null);

          if (item != null) {
            tail++;

            // actorContext.setContextMessage(item);
            actor.deliver(item, actorContext);
          }
        }

        tailCounter.set(tail);
        lastTail = tail;
      }
    } finally {
      if (tail != lastTail) {
        tailCounter.set(tail);
      }
    }

    return lastTail;
  }

  @Override
  public void process(final Actor<M> processor,
                      final MessageContext<M> threadContext,
                      final ActorCounter headCounter,
                      final ActorCounter tailCounter,
                      final TaskWorker nextWorker,
                      boolean isTail)
    throws Exception
  {
    final RingGetter<M> ringGetter = isTail ? _tailGetter : _nonTailGetter;

    int tailChunk = 2;
    long initialTail = tailCounter.get();
    long tail = initialTail;
    long head = headCounter.get();

    try {
      do {
        long tailChunkEnd = Math.min(head, tail + tailChunk);

        while (tail < tailChunkEnd) {
          M item = ringGetter.get(tail);

          if (item != null) {
            tail++;

            processor.deliver(item, threadContext);
          }
        }

        tailCounter.set(tail);
        initialTail = tail;
        tailChunk = Math.min(256, 2 * tailChunk);
        nextWorker.wake();

        head = headCounter.get();
      } while (head != tail);
    } finally {
      if (tail != initialTail) {
        tailCounter.set(tail);
      }

      nextWorker.wake();
    }
  }
  */

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

  abstract private static class RingGetter<T> {
    abstract public T get(long index);
  }

  private final static class RingTailGetter<T> extends RingGetter<T> {
    private final RingValueArray<T> _ring;

    RingTailGetter(RingValueArray<T> ring)
    {
      _ring = ring;
    }

    @Override
    public final T get(long index)
    {
      return _ring.getAndSet(index, null);
    }
  }

  private final static class RingNonTailGetter<T> extends RingGetter<T> {
    private final RingValueArray<T> _ring;

    RingNonTailGetter(RingValueArray<T> ring)
    {
      _ring = ring;
    }

    @Override
    public final T get(long index)
    {
      return _ring.get(index);
    }
  }
}
