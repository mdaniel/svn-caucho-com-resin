/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.nautilus.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.nautilus.broker.ReceiverBrokerBase;
import com.caucho.nautilus.broker.ReceiverMessageHandler;

/**
 * Subscription to a destination
 */
public class ReceiverBrokerNautilus extends ReceiverBrokerBase
{
  private static final Logger log
    = Logger.getLogger(ReceiverBrokerNautilus.class.getName());
  
  private final long _id;
  private final QueueService _queue;
  
  private final ReceiverMessageHandler _subscriberHandler;
  
  // NautilusMultiQueueActor variables
  private ArrayList<PendingMessage> _pendingMessages
    = new ArrayList<>();
  
  private long _sequence;
  private long _flow;
  
  ReceiverBrokerNautilus(long id,
                         QueueService queue,
                         ReceiverMessageHandler subscriberHandler)
  {
    Objects.requireNonNull(subscriberHandler);
    
    _id = id;
    _queue = queue;
    
    _subscriberHandler = subscriberHandler;
  }
  
  @Override
  public String getId()
  {
    return _queue.getName();
  }
   
  @Override
  public void accepted(long xid, long mid)
  {
    onAccepted(mid);

    //_queue.accepted(this, mid);
    /*
    NautilusJournalItem entry = new JournalAccepted(_actorReceiver, mid);
    
    _journalQueue.offer(entry);
    */
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
  public void flow(long flow)
  {
    long oldFlow = _flow;
    
    _flow = flow;
    
    if (_sequence < _flow && oldFlow <= _sequence) {
      _queue.onFlowUpdate();
    }
  }
  
  boolean isAvailable()
  {
    return _sequence < _flow;
  }
  
  long getCredit()
  {
    return Math.max(0, _flow - _sequence);
  }

  void receive(MessageNautilus msg, InputStream is, long len)
  {
    long seq = ++_sequence;
    
    PendingMessage pending = new PendingMessage(seq, msg);
    
    _pendingMessages.add(pending);
    
    try {
      _subscriberHandler.onMessage(seq, is, len);
    } catch (Exception e) {
      e.printStackTrace();
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  void onFlow(long flow)
  {
    _flow = flow;
  }

  public void onAccepted(long mid)
  {
    PendingMessage pending = remove(mid);
    
    if (pending != null) {
      MessageNautilus msg = pending.getMessage();
      
      _queue.accepted(this, msg);
    }
    else {
      System.out.println("FAILED-ACCEPTED: " + pending);
    }
    
  }
  
  private PendingMessage remove(long mid)
  {
    ArrayList<PendingMessage> pending = _pendingMessages;
    
    int len = pending.size();
    
    for (int i = 0; i < len; i++) {
      PendingMessage msg = pending.get(i);
      
      if (mid == msg.getSequence()) {
        pending.remove(i);
        
        return msg;
      }
    }
    
    return null;
    
  }

  @Override
  public void close()
  {
    stop();
  }
  
  private void stop()
  {
    _queue.unregister(_id, this);
    
    //_journalQueue.offer(entry);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "," + _queue + "]";
  }
  
  class PendingMessage {
    private final long _sequence;
    private final MessageNautilus _msg;
    
    PendingMessage(long sequence, MessageNautilus msg)
    {
      _sequence = sequence;
      _msg = msg;
    }
    
    long getSequence()
    {
      return _sequence;
    }
    
    MessageNautilus getMessage()
    {
      return _msg;
    }
  }
}
