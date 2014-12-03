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

package com.caucho.cloud.bam;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.caucho.bam.QueueFullException;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownSystem;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

/**
 * Handler for queue full messages.
 */
public class BamQueueFullHandler // implements QueueFullHandler
{
  private static final Logger log
    = Logger.getLogger(BamQueueFullHandler.class.getName());

  private static final L10N L = new L10N(BamQueueFullHandler.class);

  public static final BamQueueFullHandler DEFAULT = new BamQueueFullHandler(null);

  private final ResinSystem _resinSystem;

  private final long _duplicateTimeout = 60 * 1000L;
  private final long _fatalTimeout = 180 * 1000L;

  private final AtomicLong _lastExceptionTime = new AtomicLong();
  private final AtomicLong _lastWarningTime = new AtomicLong();
  private final AtomicLong _firstSequenceTime = new AtomicLong();
  private final AtomicInteger _repeatCount = new AtomicInteger();

  BamQueueFullHandler(ResinSystem resinSystem)
  {
    _resinSystem = resinSystem;
  }

  // @Override
  public void onQueueFull(Mailbox service,
                          int queueSize,
                          long timeout,
                          TimeUnit unit,
                          Object message)
  {
    long lastExceptionTime = _lastExceptionTime.get();
    long firstSequenceTime = _firstSequenceTime.get();
    int repeatCount = _repeatCount.get();

    long now = CurrentTime.getCurrentTime();

    _lastExceptionTime.set(now);

    if (now - lastExceptionTime < _duplicateTimeout) {
      _repeatCount.incrementAndGet();
    }
    else {
      _repeatCount.set(0);
      _firstSequenceTime.set(now);

      firstSequenceTime = now;
    }

    _lastExceptionTime.set(now);

    String msg;

    msg = L.l("Queue full in service {0}: queue-size={1}, timeout={2}ms, full-duration={3}ms, message={4}",
              service,
              queueSize,
              unit.toMillis(timeout),
              now - firstSequenceTime,
              message);

    if (repeatCount > 0) {
      msg += L.l(" repeats {0} times.", repeatCount);
    }

    if (_fatalTimeout < now - firstSequenceTime) {
      ShutdownSystem.shutdownActive(ExitCode.NETWORK, msg);
      log.warning(msg);
    }
    else if (now - _lastWarningTime.get() > 5000L) {
      log.warning(msg);
      _lastWarningTime.set(now);
    }

    RuntimeException exn;

    exn = new QueueFullException(msg);

    throw exn;
  }
}
