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

import java.util.HashMap;
import java.util.logging.Logger;

import com.caucho.env.thread.ActorQueue.ItemProcessor;
import com.caucho.message.journal.JournalResult;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
class NautilusMultiQueueActor
  implements ItemProcessor<NautilusRingItem>
{
  private static final Logger log
    = Logger.getLogger(ItemProcessor.class.getName());
  
  private HashMap<Long,NautilusQueue> _queueMap
    = new HashMap<Long,NautilusQueue>();
  
  private NautilusCheckpointPublisher _checkpointPub;
  
  private long _enqueueCount;
  private long _dequeueCount;
  
  private long _messageCount;
  private long _lastAddress;
  private long _lastCheckpoint;
  
  private int _size;

  void setNautilusCheckpointPublisher(NautilusCheckpointPublisher pub)
  {
    _checkpointPub = pub;
  }
  
  public int getSize()
  {
    return _size;
  }
  
  public long getEnqueueCount()
  {
    return _enqueueCount;
  }
  
  public long getDequeueCount()
  {
    return _dequeueCount;
  }

  @Override
  public void process(NautilusRingItem entry)
    throws Exception
  {
    NautilusQueue queue = getQueue(entry.getId());
    JournalResult result = entry.getResult();
    
    int code = entry.getCode();
    long mid = entry.getSequence();
    
    switch (code) {
    case NautilusRingItem.JE_MESSAGE:
      queue.processData(mid,
                        entry.isInit(), entry.isFin(),
                        result.getBlockStore(),
                        result.getBlockAddr1(),
                        result.getOffset1(),
                        result.getLength1());
      
      _lastAddress = result.getBlockAddr1();
      
      if (result.getLength2() > 0) {
        queue.processData(mid, false, entry.isFin(),
                          result.getBlockStore(),
                          result.getBlockAddr2(),
                          result.getOffset2(),
                          result.getLength2());
      }
      
      long count = ++_messageCount;
      
      if ((count & 0x3fff) == 0) {
        updateCheckpoint(_lastAddress);
      }
      break;
      
    case NautilusRingItem.JE_SUBSCRIBE:
      queue.subscribe(entry.getSubscriber());
      break;
      
    case NautilusRingItem.JE_UNSUBSCRIBE:
      queue.unsubscribe(entry.getSubscriber());
      break;
      
    case NautilusRingItem.JE_ACCEPTED:
      queue.ack(entry.getSequence());
      queue.ack(entry.getSubscriber());
      break;
      
    case NautilusRingItem.JE_FLOW:
      if (entry.getSubscriber().onFlow(entry.getDeliveryCount(), 
                                       entry.getCredit())) {
        queue.deliver();
      }
      break;
      
    case NautilusRingItem.JE_CHECKPOINT:
      break;
      
    default:
      System.out.println("UNKNOWN: " + (char) code);
      log.warning("Unknown code: " + (char) code
                  + " " + Integer.toHexString(code));
    }
  }
  
  private void updateCheckpoint(long tailAddress)
  {
    long checkpointAddress = tailAddress;
    
    for (NautilusQueue queue : _queueMap.values()) {
      if (checkpointAddress > 0) {
        checkpointAddress = queue.updateCheckpoint(checkpointAddress);
      }
    }
    
    if (checkpointAddress > 0 && checkpointAddress != _lastCheckpoint) {
      NautilusCheckpointPublisher checkpointPub = _checkpointPub;
      if (checkpointPub != null
          && checkpointPub.checkpoint(checkpointAddress)) {
        _lastCheckpoint = checkpointAddress;
      }
    }
  }
  
  private NautilusQueue getQueue(Long id)
  {
    NautilusQueue queue = _queueMap.get(id);
    
    if (queue == null) {
      queue = new NautilusQueue(id);
      _queueMap.put(id, queue);
    }
    
    return queue;
  }

  @Override
  public void onProcessComplete() throws Exception
  {
//    updateCheckpoint(_lastAddress);
  }
   
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
