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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RingValueQueue<T> extends RingQueue<RingValueItem<T>> {
  
  public RingValueQueue(int capacity)
  {
    super(capacity, new RingValueItemFactory());
  }
  
  public boolean offer(T value)
  {
    RingValueItem<T> item = beginOffer(true);
    
    item.setValue(value);
    
    completeOffer(item);
    
    return true;
  }
  
  public boolean offer(T value, boolean isWait)
  {
    RingValueItem<T> item = beginOffer(isWait);
    
    if (item == null) {
      return false;
    }
    
    item.setValue(value);
    
    completeOffer(item);
    
    return true;
  }
 
  public final T poll()
  {
    final RingValueItem<T> item = beginPoll();
    
    if (item == null)
      return null;
    
    final T value = item.getAndClearValue();
    
    completePoll(item);
    
    return value;
  }
  
  static class RingValueItemFactory<T> implements RingItemFactory<RingValueItem<T>> {
    @Override
    public RingValueItem<T> createItem(int index)
    {
      return new RingValueItem<T>(index);
    }
  }
}
