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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.actor.ActorQueue;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.ReceiverMessageHandler;
import com.caucho.message.nautilus.NautilusQueue.QueueEntry;

/**
 * Subscription to a destination
 */
public class NautilusBrokerSubscriber implements BrokerReceiver
{
  private static final long MASK = 0xffffffffL;
  
  private static final Logger log
    = Logger.getLogger(NautilusBrokerSubscriber.class.getName());
  
  private final String _queueName;
  private final long _qid;
  
  private final ActorQueue<NautilusRingItem> _nautilusQueue;
  
  private final ReceiverMessageHandler _subscriberHandler;
  
  // NautilusMultiQueueActor variables
  private long _deliveryCount;
  private int _credit;
  
  NautilusBrokerSubscriber(String queueName,
                           long qid,
                           ActorQueue<NautilusRingItem> nautilusActorQueue,
                           ReceiverMessageHandler subscriberHandler)
  {
    _queueName = queueName;
    _qid = qid;
    
    _nautilusQueue = nautilusActorQueue;
    
    _subscriberHandler = subscriberHandler;
    
    if (subscriberHandler == null)
      throw new NullPointerException();
    
    start();
  }
   
  @Override
  public void accepted(long xid, long mid)
  {
    NautilusRingItem entry = _nautilusQueue.startOffer(true);
    
    entry.initAck(xid, _qid, mid, this);
    
    _nautilusQueue.finishOffer(entry);
    _nautilusQueue.wake();
  }
  
  @Override
  public void rejected(long xid, long mid, String message)
  {
    System.out.println("reject: " + mid);
  }
  
  public void modified(long xid, 
                       long mid, 
                       boolean isFailed, 
                       boolean isUndeliverableHere)
  {
    
  }
  

  @Override
  public void released(long xid, long mid)
  {
    System.out.println("releaseE: " + mid);
  }
  
  @Override
  public void flow(long deliveryCount, int linkCredit)
  {
    NautilusRingItem entry = _nautilusQueue.startOffer(true);
    
    entry.initFlow(_qid, this, deliveryCount, linkCredit);
    
    _nautilusQueue.finishOffer(entry);
    _nautilusQueue.wake();
  }
  
  @Override
  public void close()
  {
    stop();
  }
  
  private void start()
  {
    NautilusRingItem entry = _nautilusQueue.startOffer(true);
    
    entry.initSubscribe(_qid, this);
    
    _nautilusQueue.finishOffer(entry);
    _nautilusQueue.wake();
  }
  
  private void stop()
  {
    NautilusRingItem entry = _nautilusQueue.startOffer(true);
    
    entry.initUnsubscribe(_qid, this);
    
    _nautilusQueue.finishOffer(entry);
    _nautilusQueue.wake();
  }

  /**
   * @param entry
   */
  public void onTransfer(QueueEntry entry)
  {
    onTransfer(entry.getSequence(), entry.getDataHead());
  }
  
  private void onTransfer(long mid, MessageDataNode dataHead)
  {
    _deliveryCount = (_deliveryCount + 1) & MASK;
    _credit--;
    
    MessageDataInputStream is = new MessageDataInputStream(dataHead);
    
    long len = dataHead.getLength();

    try {
      _subscriberHandler.onMessage(mid, is, len);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  boolean isAvailable()
  {
    return _credit > 0;
  }
  
  boolean onFlow(long clientDeliveryCount, int credit)
  {
    if (clientDeliveryCount >= 0) {
      long delta = (_deliveryCount - clientDeliveryCount) & MASK;
      
      credit -= delta;
    }
    
    if (credit < 0) {
      credit = 0;
    }
      
    _credit = credit;
    
    return credit > 0;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _qid + "," + _queueName + "]";
  }
}
