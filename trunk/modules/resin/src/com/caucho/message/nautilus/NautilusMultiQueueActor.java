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

import com.caucho.env.actor.AbstractActorProcessor;
import com.caucho.env.actor.ActorProcessor;
import com.caucho.message.journal.JournalResult;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
class NautilusMultiQueueActor
  extends AbstractActorProcessor<NautilusRingItem>
{
  private static final Logger log
    = Logger.getLogger(NautilusMultiQueueActor.class.getName());
  
  private static final long C_DURABLE = (1L << 45);
  private static final int C_PRIORITY_OFF = 40; 
  private static final long C_PRIORITY_MASK = 0xfL;
  private static final long C_PRIORITY_DEFAULT = 4;
  
  private static final long C_EXPIRE_LOSS_BITS = 6;
  private static final long C_EXPIRE_LOSS_MASK = (1L << C_EXPIRE_LOSS_BITS) - 1;
  private static final long C_EXPIRE_BITS = 32 - C_EXPIRE_LOSS_BITS;
  private static final long C_EXPIRE_MASK = (1L << C_EXPIRE_BITS) - 1;
  private static final int C_EXPIRE_OFF = 6;
  
  private static final long C_OP = 0x1f;
  
  private String _threadName;
  
  private HashMap<Long,NautilusQueue> _queueMap
    = new HashMap<Long,NautilusQueue>();
  
  private NautilusCheckpointPublisher _checkpointPub;
  
  private long _enqueueCount;
  private long _dequeueCount;
  
  private long _messageCount;
  private long _lastAddress;
  private long _lastCheckpoint;
  
  private int _size;
  
  public NautilusMultiQueueActor()
  {
    _threadName = toString();
  }

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
  public String getThreadName()
  {
    return _threadName;
  }

  @Override
  public void process(NautilusRingItem entry)
    throws Exception
  {
    long qid = entry.getQid();
    NautilusQueue queue = getQueue(qid);
    
    long code = entry.getCode();
    
    int op = decodeOp(code);
    long mid = entry.getMid();
    
    switch (op) {
    case NautilusRingItem.JE_MESSAGE:
    {
      JournalResult result = entry.getResult();
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
    }
      
    case NautilusRingItem.JE_SUBSCRIBE:
      queue.subscribe(entry.getSubscriber());
      break;
      
    case NautilusRingItem.JE_UNSUBSCRIBE:
      queue.unsubscribe(entry.getSubscriber());
      break;
      
    case NautilusRingItem.JE_ACCEPTED:
      queue.ack(entry.getMid());
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
      System.out.println("UNKNOWN: " + Integer.toHexString(op));
      log.warning("Unknown code: " + " " + Integer.toHexString(op));
    }
  }
  
  private int decodeOp(long code)
  {
    return (int) (code & C_OP);
  }

  static long encode(int op, 
                     boolean isDurable, 
                     int priority,
                     long expireTime)
  {
    long code = op;
    
    if (isDurable) {
      code |= C_DURABLE;
    }
    
    long priorityBits;
    
    if (priority < 0) {
      priorityBits = C_PRIORITY_DEFAULT;
    }
    else if (C_PRIORITY_MASK < priority) {
      priorityBits = C_PRIORITY_MASK;
    }
    else {
      priorityBits = (priority & C_PRIORITY_MASK);
    }
    
    code |= priorityBits << C_PRIORITY_OFF;
    
    long expireBits = (expireTime + C_EXPIRE_LOSS_MASK) >> C_EXPIRE_LOSS_BITS;
    expireBits &= C_EXPIRE_MASK;
    
    code |= expireBits << C_EXPIRE_OFF;
    
    return code;
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
