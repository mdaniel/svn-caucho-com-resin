/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.admin;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public final class TimeRangeProbe extends Probe implements TimeSample {
  private final AtomicLong _count = new AtomicLong();
  private final AtomicLong _time = new AtomicLong();
  private final AtomicLong _timeMax = new AtomicLong();
  
  private final AtomicLong _lastAvgCount = new AtomicLong();
  private final AtomicLong _lastAvgTime = new AtomicLong();
  private final AtomicLong _lastCount = new AtomicLong();
  

  public TimeRangeProbe(String name)
  {
    super(name);
  }

  public Probe createCount(String name)
  {
    return new TimeRangeCountProbe(name);
  }

  public Probe createMax(String name)
  {
    return new TimeRangeMaxProbe(name);
  }

  public final void add(long time)
  {
    _count.incrementAndGet();
    _time.addAndGet(time);

    long max = _timeMax.get();
    while (max < time) {
      _timeMax.compareAndSet(max, time);
      max = _timeMax.get();
    }
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sample()
  {
    long count = _count.get();
    long lastCount = _lastAvgCount.getAndSet(count);
    long time = _time.get();
    long lastTime = _lastAvgTime.getAndSet(time);

    if (count == lastCount)
      return 0;
    else
      return (time - lastTime) / (double) (count - lastCount);
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sampleCount()
  {
    long count = _count.get();
    long lastCount = _lastCount.getAndSet(count);

    return count - lastCount;
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sampleMax()
  {
    long max = _timeMax.getAndSet(0);

    return max;
  }

  class TimeRangeCountProbe extends Probe {
    TimeRangeCountProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleCount();
    }
  }

  class TimeRangeMaxProbe extends Probe {
    TimeRangeMaxProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleMax();
    }
  }
}
