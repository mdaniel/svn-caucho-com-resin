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

package com.caucho.v5.nautilus.impl;

import io.baratine.service.Direct;

import java.io.InputStream;

import com.caucho.v5.io.TempBuffer;
import com.caucho.v5.nautilus.broker.SenderSettleHandler;
import com.caucho.v5.util.ConcurrentArrayList;

/**
 * Service for an individual queue, managing its messages.
 * 
 * The QueueService is a central nautilus service.
 */
class QueueServiceLocal extends QueueServiceBase
{
  private MessageNautilus _head;
  private MessageNautilus _tail;
  
  private ConcurrentArrayList<ReceiverBrokerNautilus> _receiverList
    = new ConcurrentArrayList<>(ReceiverBrokerNautilus.class);
  
  private long _enqueueCount;
  private long _dequeueCount;
  
  private int _size;
  
  QueueServiceLocal(BrokerServiceImpl broker,
                     String name,
                     long qid)
  {
    super(broker, name, qid);
  }
  
  @Direct
  public int getSize()
  {
    return _size;
  }
  
  @Direct
  public long getEnqueueCount()
  {
    return _enqueueCount;
  }
  
  @Direct
  public long getDequeueCount()
  {
    return _dequeueCount;
  }
  
  public void start()
  {
    getBroker().loadQueue(this);
  }
  
  public void message(long sid,
                      long mid, 
                      byte[] buffer, int offset, int length,
                      TempBuffer tBuf,
                      SenderSettleHandler settleHandler)
  {
    long qMid = nextSequence();
    
    MessageNautilus message
      = new MessageNautilus(qMid, buffer, offset, length); 

    addMessage(message);
    
    getBroker().saveMessage(this, qMid, message.openInputStream());
    
    deliver();
    
    // checkpointMessage();
  }

  /**
   * Restore a message from the checkpoint store.
   */
  public void restoreMessage(long jdbcId,
                                       long mid, 
                                       int priority,
                                       long expireTime)
  {
    MessageNautilus msg;
    msg = new MessageNautilus(mid, priority, expireTime, jdbcId);
    
    addMessage(msg);
  }
  
  void restoreJdbcComplete()
  {
    deliver();
  }
  
  private void addMessage(MessageNautilus entry)
  {
    if (_tail != null) {
      _tail.setNext(entry);
    }
    else {
      _head = entry;
    }
  
    _tail = entry;
  
    _size++;
    _enqueueCount++;
  }
  
  /*
  private void checkpointMessage()
  {
    for (MessageNautilus entry = _head; 
        entry != null; 
        entry = entry.getNext()) {
      if (! entry.toSaving()) {
        continue;
      }
      
      InputStream is = null; // new MessageDataInputStream(entry.getData());
      
      _broker.saveMessage(this, entry.getSequence(), is);
    }
  }
  */

  public void onMessageRestore(long qid, long mid)
  {
    MessageNautilus message = new MessageNautilus(mid, 0, 0, 0);
    
    addMessage(message);
  }

  public void onSaveComplete(long mid, long oid)
  {
    MessageNautilus entry = findEntry(mid);
    
    if (entry == null) {
      return;
    }
    
    entry.toSaved(oid);
  }
  
  private MessageNautilus findEntry(long mid)
  {
    for (MessageNautilus entry = _head; entry != null; entry = entry.getNext()) {
      if (entry.getSequence() == mid) {
        return entry;
      }
    }
    
    return null;
  }

  public void subscribe(ReceiverBrokerNautilus receiver)
  {
    _receiverList.add(receiver);
    
    deliver();
  }
  
  public void unregister(long id, ReceiverBrokerNautilus receiver)
  {
    _receiverList.remove(receiver);
  }
  
  
  /*
  void register(ReceiverBrokerNautilus receiver)
  {
    _receiverList.add(receiver);
  }
  */
  
  public void accepted(ReceiverBrokerNautilus receiver,
                       MessageNautilus msg)
  {
    removeEntry(msg);
    
    msg.accepted(this);
    
    
    // receiver.onAccepted(msg)
  }
  
  private boolean removeEntry(MessageNautilus msg)
  {
    MessageNautilus entry = _head;
    MessageNautilus prev = null;
    
    for (; entry != null; entry = entry.getNext()) {
      if (entry == msg) {
        if (prev != null) {
          prev.setNext(entry.getNext());
        }
        else {
          _head = entry.getNext();
          
          if (_head == null)
            _tail = null;
        }
        
        return true;
      }
    }
    
    return false;
  }
  
  void ack(long sequence)
  {
    MessageNautilus entry = _head;
    MessageNautilus prev = null;
    
    for (; entry != null; entry = entry.getNext()) {
      if (entry.getSequence() == sequence) {
        if (prev != null) {
          prev.setNext(entry.getNext());
        }
        else {
          _head = entry.getNext();
          
          if (_head == null)
            _tail = null;
        }
        
        return;
      }
    }
  }
  
  void ack(ReceiverBrokerNautilus subscriber)
  {
  }
  
  public void onFlowUpdate()
  {
    deliver();
  }
  
  void deliver()
  {
    ReceiverBrokerNautilus []subList = _receiverList.toArray();
    // XXX: round robin

    while (_head != null) {
      boolean isDeliver = false;
      
      for (ReceiverBrokerNautilus sub : subList) {
        if (! sub.isAvailable()) {
          continue;
        }
      
        MessageNautilus entry = _head;
        _head = _head.getNext();
        
        if (_head == null) {
          _tail = null;
        }
      
        _size--;
        _dequeueCount++;

        entry.receive(this, sub);
      
        isDeliver = true;
      }
      
      if (! isDeliver) {
        return;
      }
    }
  }

  public void receiveFromStore(long mid, 
                               MessageNautilus message,
                               ReceiverBrokerNautilus receiver)
  {
    int len = 0;
    
    InputStream is = getBroker().loadMessage(getId(), mid);
    
    receiver.receive(message, is, len);
  }
}
