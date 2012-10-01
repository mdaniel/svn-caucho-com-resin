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

package com.caucho.env.thread2;

import com.caucho.util.RingItemFactory;
import com.caucho.util.RingQueue;

public class ThreadTaskRing2 extends RingQueue<ThreadTaskItem2> {
  private static final int RING_SIZE = 16 * 1024;
  
  public ThreadTaskRing2()
  {
    super(RING_SIZE, new ThreadTaskItemFactory());
  }
  
  public boolean offer(Runnable task, ClassLoader loader)
  {
    ThreadTaskItem2 item = beginOffer(true);
    
    item.init(task, loader);
    
    completeOffer(item);
    
    return true;
  }
  
  boolean takeAndSchedule(ResinThread2 thread)
  {
    ThreadTaskItem2 item = beginPoll();
    
    if (item == null)
      return false;
    
    item.schedule(thread);
    
    completePoll(item);
    
    return true;
  }
  
  private static class ThreadTaskItemFactory
    implements RingItemFactory<ThreadTaskItem2> {
    
    public ThreadTaskItem2 createItem(int index)
    {
      return new ThreadTaskItem2(index);
    }
  }
}
