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

package com.caucho.message.nautilus;

import com.caucho.env.actor.AbstractActorProcessor;
import com.caucho.env.actor.ActorProcessor;
import com.caucho.env.actor.ActorQueue;
import com.caucho.env.actor.ValueActorQueue;

/**
 * Custom serialization for the cache
 */
class NautilusCheckpointPublisher
{
  private ActorQueue<NautilusRingItem> _nautilusQueue;
  private ValueActorQueue<Long> _checkpointQueue;
  
  NautilusCheckpointPublisher(ActorQueue<NautilusRingItem> nautilusQueue)
  {
    _nautilusQueue = nautilusQueue;
    
    _checkpointQueue = new ValueActorQueue<Long>(256, new CheckpointProcessor());
  }
  
  boolean checkpoint(long blockAddress)
  {
    boolean isValid = _checkpointQueue.offer(new Long(blockAddress), false);
    
    _checkpointQueue.wake();
    
    return isValid;
  }
  
  class CheckpointProcessor extends AbstractActorProcessor<Long> {
    @Override
    public String getThreadName()
    {
      return toString();
    }
    
    @Override
    public void process(Long blockAddress) throws Exception
    {
      NautilusRingItem entry = _nautilusQueue.startOffer(true);
      
      entry.initCheckpoint(blockAddress, 0, 0);
      
      _nautilusQueue.finishOffer(entry);
      
      _nautilusQueue.wake();
    }

    @Override
    public void onProcessComplete() throws Exception
    {

    }
    
  }
}
