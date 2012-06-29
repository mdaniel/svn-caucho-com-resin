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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Item for the disruptor.
 */
public class RingItem
{
  // private static final AtomicIntegerFieldUpdater<RingItem> _isRingValueUpdater;

  private final int _index;
  // private volatile int _isRingValue;
  private volatile long _ringValue;
  
  protected RingItem(int index)
  {
    _index = index;
    _ringValue = index;
  }
  
  public int getIndex()
  {
    return _index;
  }
  
  public final long getRingValue()
  {
    return _ringValue;
  }
  
  public final long nextRingValue(int size)
  {
    long value = _ringValue + size;
    
    _ringValue = value;
    
    return value;
  }
  
  public final boolean isRingValue()
  {
    // return _isRingValue != 0;
    return _ringValue != 0;
  }
  
  public final void setRingValue()
  {
    _ringValue = 1;

    /*
    int oldValue = _isRingValueUpdater.getAndSet(this, 1);
    
    if (oldValue != 0) {
      System.out.println("BAD-SET-RING:");
    }
    */
  }
  
  public final void clearRingValue()
  {
    _ringValue = 0;

    /*
    int oldValue = _isRingValueUpdater.getAndSet(this, 0);
    
    if (oldValue == 0) {
      System.out.println("BAD-CLEAR-RING:");
    }
    */
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
  /*
  static {
    AtomicIntegerFieldUpdater<RingItem> isRingValueUpdater
      = AtomicIntegerFieldUpdater.newUpdater(RingItem.class, "_isRingValue");
    
    _isRingValueUpdater = isRingValueUpdater;
  }
  */
}
