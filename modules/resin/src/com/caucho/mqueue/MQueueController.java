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

package com.caucho.mqueue;

import com.caucho.env.thread.TaskWorker;
import com.caucho.vfs.TempBuffer;

/**
 * Interface for the transaction log.
 */
public final class MQueueController extends TaskWorker
{
  private final MQueueDisruptor _disruptor;
  private final MQueueItem []_ring;
  
  private final int _controllerId;
  
  private final MQueueController _prev;
  private MQueueController _next;
  
  private MQueueTask _task;
  
  private volatile int _index;
  
  MQueueController(MQueueDisruptor disruptor,
                   MQueueController prev)
  {
    _disruptor = disruptor;
    _ring = null;
    _controllerId = 0;
    _prev = prev;
  }
  
  private int getIndex()
  {
    return _index;
  }
  
  private boolean doStuff()
  {
    int tail = _index;
   
    int head = _prev.getIndex();
    
    if (head != tail) {
      MQueueItem item = _ring[tail];
    }
    
    return false;
  }

  @Override
  public long runTask()
  {
    while (doStuff()) {
    }
    
    return 0;
  }
}
