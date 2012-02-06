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

package com.caucho.mqueue.queue;

import java.util.logging.Logger;

import com.caucho.db.block.BlockStore;
import com.caucho.env.thread.DisruptorQueue.ItemProcessor;
import com.caucho.mqueue.journal.MQueueJournalResult;
import com.caucho.util.ConcurrentArrayList;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
class JournalQueueActor
  extends ItemProcessor<JournalQueueEntry>
{
  private static final Logger log
    = Logger.getLogger(ItemProcessor.class.getName());
  
  private QueueEntry _head;
  private QueueEntry _tail;
  
  private ConcurrentArrayList<SubscriberNode> _subscriberList
    = new ConcurrentArrayList<SubscriberNode>(SubscriberNode.class);
  
  private long _enqueueCount;
  private long _dequeueCount;
  
  private int _size;
  
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
  public void process(JournalQueueEntry entry)
    throws Exception
  {
    MQueueJournalResult result = entry.getResult();
    
    int code = entry.getCode();
    long sequence = entry.getSequence();

    switch (code) {
    case 'D':
      processData(sequence,
                  entry.isInit(), entry.isFin(),
                  result.getBlockStore(),
                  result.getBlockAddr1(),
                  result.getOffset1(),
                  result.getLength1());
      
      if (result.getLength2() > 0) {
        processData(sequence, false, entry.isFin(),
                    result.getBlockStore(),
                    result.getBlockAddr2(),
                    result.getOffset2(),
                    result.getLength2());
      }
      break;
      
    case 'S':
      subscribe(entry.getSubscriber());
      break;
      
    case 'U':
      unsubscribe(entry.getSubscriber());
      break;
      
    case 'A':
      ack(entry.getSequence(), entry.getSubscriber());
      break;
      
    default:
      System.out.println("UNKNOWN: " + (char) code);
      log.warning("Unknown code: " + (char) code
                  + " " + Integer.toHexString(code));
    }
  }
  
  private void processData(long sequence, boolean isInit, boolean isFinal,
                           BlockStore blockStore,
                           long blockAddr, int offset, int length)
  {
    QueueEntry entry = new QueueEntry(sequence);
    
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
    
    send();
  }
  
  private void subscribe(MQJournalQueueSubscriber subscriber)
  {
    SubscriberNode node = new SubscriberNode(subscriber);
    
    _subscriberList.add(node);
    
    send();
  }
  
  private void unsubscribe(MQJournalQueueSubscriber subscriber)
  {
    for (SubscriberNode node : _subscriberList.toArray()) {
      if (node.getSubscriber() == subscriber) {
        _subscriberList.remove(node);
      }
    }
    // _subscriberList.remove(subscriber);
  }
  
  private void ack(long sequence, MQJournalQueueSubscriber subscriber)
  {
    for (SubscriberNode node : _subscriberList.toArray()) {
      if (node.getSubscriber() == subscriber) {
        node.setAvailable(true);
      }
    }
    
    send();
  }
  
  private void send()
  {
    if (_head == null) {
      return;
    }
    
    for (SubscriberNode node : _subscriberList.toArray()) {
      MQJournalQueueSubscriber sub = node.getSubscriber();
      
      if (! node.isAvailable()) {
        continue;
      }
      
      QueueEntry entry = _head;
      _head = _head.getNext();
      if (_head == null)
        _tail = null;
      
      _size--;
      _dequeueCount++;
      
      sub.offerEntry(entry.getSequence(), entry.getDataHead());
      
      node.setAvailable(false);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
  
  public static class QueueEntry {
    private final long _sequence;
    
    private QueueEntry _next;
    
    private JournalDataNode _head;
    private JournalDataNode _tail;
    
    QueueEntry(long sequence)
    {
      _sequence = sequence;
    }
    
    long getSequence()
    {
      return _sequence;
    }
    
    QueueEntry getNext()
    {
      return _next;
    }
    
    void setNext(QueueEntry next)
    {
      _next = next;
    }
    
    JournalDataNode getDataHead()
    {
      return _head;
    }
    
    void addData(BlockStore blockStore, 
                 long blockAddress, 
                 int offset, 
                 int length)
    {
      JournalDataNode dataNode
        = new JournalDataNode(blockStore, blockAddress, offset, length);

      if (_tail != null) {
        _tail.setNext(dataNode);
      }
      else {
        _head = dataNode;
        
        _tail = dataNode;
      }
    }
  }
  
  public static class SubscriberNode {
    private final MQJournalQueueSubscriber _subscriber;
    
    private boolean _isAvailable = true;
    
    SubscriberNode(MQJournalQueueSubscriber subscriber)
    {
      _subscriber = subscriber;
    }
    
    public MQJournalQueueSubscriber getSubscriber()
    {
      return _subscriber;
    }
    
    public boolean isAvailable()
    {
      return _isAvailable;
    }
    
    public void setAvailable(boolean isAvailable)
    {
      _isAvailable = isAvailable;
    }
  }
}
