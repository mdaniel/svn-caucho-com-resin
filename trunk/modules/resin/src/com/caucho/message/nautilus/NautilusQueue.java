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

import com.caucho.db.block.BlockStore;
import com.caucho.message.journal.JournalFile;
import com.caucho.util.ConcurrentArrayList;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
class NautilusQueue
{
  private final long _qid;
  
  private QueueEntry _head;
  private QueueEntry _tail;
  
  private ConcurrentArrayList<NautilusBrokerSubscriber> _subscriberList
    = new ConcurrentArrayList<NautilusBrokerSubscriber>(NautilusBrokerSubscriber.class);
  
  private long _enqueueCount;
  private long _dequeueCount;
  
  private int _size;
  
  NautilusQueue(long qid)
  {
    _qid = qid;
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
  
  void processData(long mid, boolean isInit, boolean isFinal,
                   BlockStore blockStore,
                   long blockAddr, int offset, int length)
  {
    QueueEntry entry = new QueueEntry(mid);

    entry.addData(blockStore, blockAddr, offset, length);
    
    if (_tail != null) {
      _tail.setNext(entry);
    }
    else {
      _head = entry;
    }
    
    _tail = entry;
    
    _size++;
    _enqueueCount++;
    
    deliver();
  }
  
  void subscribe(NautilusBrokerSubscriber subscriber)
  {
    _subscriberList.add(subscriber);

    deliver();
  }
  
  void unsubscribe(NautilusBrokerSubscriber subscriber)
  {
    _subscriberList.remove(subscriber);
  }
  
  void ack(long sequence)
  {
    QueueEntry entry = _head;
    QueueEntry prev = null;
    
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
  
  void ack(NautilusBrokerSubscriber subscriber)
  {
  }
  
  void deliver()
  {
    NautilusBrokerSubscriber []subList = _subscriberList.toArray();
    // XXX: round robin
    
    while (_head != null) {
      boolean isDeliver = false;
      
      for (NautilusBrokerSubscriber sub : subList) {
        if (! sub.isAvailable()) {
          continue;
        }
      
        QueueEntry entry = _head;
        _head = _head.getNext();
        if (_head == null)
          _tail = null;
      
        _size--;
        _dequeueCount++;
      
        sub.onTransfer(entry);
      
        isDeliver = true;
      }
      
      if (! isDeliver) {
        return;
      }
    }
  }

  /**
   * @param tailAddress
   * @return
   */
  public long updateCheckpoint(long tailAddress)
  {
    QueueEntry head = _head;
    
    if (head == null) {
      return tailAddress;
    }
    else {
      long queueHeadAddress = head.getDataHead().getBlockAddress();
      
      if (! JournalFile.isSamePage(queueHeadAddress, tailAddress)) {
        return -1;
      }
      else if (queueHeadAddress < tailAddress)
        return queueHeadAddress;
      else
        return tailAddress;
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _qid + "]";
  }
  
  public static class QueueEntry {
    private final long _mid;
    
    private QueueEntry _next;
    
    private MessageDataNode _head;
    private MessageDataNode _tail;
    
    QueueEntry(long sequence)
    {
      _mid = sequence;
    }
    
    long getSequence()
    {
      return _mid;
    }
    
    QueueEntry getNext()
    {
      return _next;
    }
    
    void setNext(QueueEntry next)
    {
      _next = next;
    }
    
    MessageDataNode getDataHead()
    {
      return _head;
    }
    
    void addData(BlockStore blockStore, 
                 long blockAddress, 
                 int offset, 
                 int length)
    {
      MessageDataNode dataNode
        = new MessageDataNode(blockStore, blockAddress, offset, length);

      if (_tail != null) {
        _tail.setNext(dataNode);
      }
      else {
        _head = dataNode;
        
        _tail = dataNode;
      }
    }
  }
}
