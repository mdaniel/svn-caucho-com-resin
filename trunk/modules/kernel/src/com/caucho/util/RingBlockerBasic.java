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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.CurrentTime;

/**
 * Ring blocking algorithm.
 */
public class RingBlockerBasic implements RingBlocker {
  private static final Logger log
    = Logger.getLogger(RingBlockerBasic.class.getName());

  private final AtomicLong _offerWaitSequence = new AtomicLong();
  private final AtomicLong _offerWakeSequence = new AtomicLong();

  private final AtomicLong _pollWaitSequence = new AtomicLong();
  private final AtomicLong _pollWakeSequence = new AtomicLong();

  @Override
  public final boolean offerWait(long tail,
                                 AtomicLong tailRef,
                                 int reservedSpace,
                                 long timeout,
                                 TimeUnit unit)
  {
    if (timeout <= 0) {
      return false;
    }

    long now = CurrentTime.getCurrentTimeActual();
    long expires = now + unit.toMillis(timeout);

    long sequence = _offerWaitSequence.incrementAndGet();

    pollWake();

    synchronized (_offerWaitSequence) {
      // sequence = _offerWaitSequence.incrementAndGet();
      while (_offerWakeSequence.get() < sequence
             && tail == tailRef.get()
             && (now = CurrentTime.getCurrentTimeActual()) < expires) {
        try {
          // long nanos = unit.toNanos(timeout);

          long millis = Math.max(0, expires - now);
          // nanos = nanos % 1000000L;

          _offerWaitSequence.wait(millis);
        } catch (Exception e) {
          Thread.interrupted();

          log.log(Level.FINER, e.toString(), e);
        }
      }
    }

    return sequence <= _offerWakeSequence.get() || tail != tailRef.get();
  }

  @Override
  public final boolean isOfferWait()
  {
    return _offerWakeSequence.get() < _offerWaitSequence.get();
  }

  @Override
  public final void offerWake()
  {
    long wakeSequence = _offerWakeSequence.get();
    long waitSequence = _offerWaitSequence.get();
    
    while (wakeSequence < waitSequence) {
      if (_offerWakeSequence.compareAndSet(wakeSequence, waitSequence)) {
        synchronized (_offerWaitSequence) {
          _offerWaitSequence.notifyAll();
        }
        break;
      }
      
      wakeSequence = _offerWakeSequence.get();
    }
  }

  @Override
  public final void wake()
  {
    offerWake();
  }

  @Override
  public final boolean pollWait(long timeout,
                                TimeUnit unit)
  {
    if (timeout <= 0) {
      return false;
    }

    long expires = CurrentTime.getCurrentTime() + unit.toMillis(timeout);

    offerWake();

    long sequence;

    synchronized (_pollWaitSequence) {
      sequence = _pollWaitSequence.incrementAndGet();
      long now;

      while (_pollWakeSequence.get() < sequence
             && (now = CurrentTime.getCurrentTime()) < expires) {
        try {
          // long nanos = unit.toNanos(timeout);

          long millis = Math.max(0, expires - now);
          // nanos = nanos % 1000000L;

          // Thread.interrupted();

          _pollWaitSequence.wait(millis);
        } catch (Exception e) {
          Thread.interrupted();
          log.log(Level.FINER, e.toString(), e);
        }
      }
    }

    return sequence <= _pollWakeSequence.get();
  }

  @Override
  public final boolean isPollWait()
  {
    return _pollWakeSequence.get() < _pollWaitSequence.get();
  }

  @Override
  public final void pollWake()
  {
    if (isPollWait()) {
      pollWakeImpl();
    }
  }

  private void pollWakeImpl()
  {
    synchronized (_pollWaitSequence) {
      _pollWakeSequence.set(_pollWaitSequence.get());
      _pollWaitSequence.notifyAll();
    }
  }

  @Override
  public final void close()
  {

  }

  /*
  private void waitForAvailable(long headAlloc, long tail)
  {
  }

  private void wakeAvailable()
  {
  }
  */
}
