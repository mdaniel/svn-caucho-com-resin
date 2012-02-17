/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.env.meter;

import java.util.concurrent.atomic.AtomicLong;

import com.caucho.util.ConcurrentArrayList;


public final class CountMeter extends AbstractMeter implements CountSensor {
  // sample data
  private final AtomicLong _totalCount = new AtomicLong();
  
  private ConcurrentArrayList<CountSensor> _listeners;
  
  private long _lastTotal;
  
  private long _value;

  public CountMeter(String name)
  {
    super(name);
  }

  @Override
  public final void start()
  {
    _totalCount.incrementAndGet();
    
    if (_listeners != null) {
      for (CountSensor sensor : _listeners.toArray()) {
        sensor.start();
      }
    }
  }

  /**
   * Sample the total count
   */
  @Override
  public final void sample()
  {
    long totalCount = _totalCount.get();
    long lastTotal = _lastTotal;
    _lastTotal = totalCount;

    _value = totalCount - lastTotal;
  }
  
  public final double calculate()
  {
    return _value;
  }
  
  /**
   * Listeners
   */
  
  public void addListener(CountSensor sensor)
  {
    synchronized (this) {
      if (_listeners == null)
        _listeners = new ConcurrentArrayList<CountSensor>(CountSensor.class);
      
      _listeners.add(sensor);
    }
  }
  
  public void removeListener(CountSensor sensor)
  {
    if (_listeners != null)
        _listeners.remove(sensor);
  }
}
