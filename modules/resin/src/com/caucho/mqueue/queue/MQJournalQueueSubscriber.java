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

import java.io.InputStream;

import com.caucho.env.thread.ActorQueue;
import com.caucho.env.thread.ActorQueue.ItemProcessor;
import com.caucho.util.Friend;
import com.caucho.util.RingItemFactory;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public class MQJournalQueueSubscriber
{
  private MQJournalQueue _queue;
  private ActorQueue<JournalQueueEntry> _journalQueue;
  
  private ActorQueue<SubscriberEntry> _subscriberQueue;
  private SubscriberProcessor _processor;
  
  MQJournalQueueSubscriber(MQJournalQueue queue, SubscriberProcessor processor)
  {
    _queue = queue;
    
    _journalQueue = queue.getDisruptor();
    _processor = processor;
    
    if (processor == null)
      throw new NullPointerException();
    
    _subscriberQueue = new ActorQueue<SubscriberEntry>(64, 
                                    new SubscriberEntryFactory(),
                                    new SubscriberItemProcessor());
  }
  
  public void start()
  {
    JournalQueueEntry entry = _journalQueue.startOffer(true);
    
    entry.initSubscribe(this);
    
    _journalQueue.finishOffer(entry);
    _journalQueue.wake();
  }
  
  public void stop()
  {
    JournalQueueEntry entry = _journalQueue.startOffer(true);
    
    entry.initUnsubscribe(this);
    
    _journalQueue.finishOffer(entry);
    _journalQueue.wake();
  }
  
  @Friend(JournalQueueActor.class)
  boolean offerEntry(long sequence, JournalDataNode dataHead)
  {
    SubscriberEntry entry = _subscriberQueue.startOffer(false);
    
    if (entry == null)
      return false;
    
    entry.initQueueData(sequence, dataHead);
    
    _subscriberQueue.finishOffer(entry);
    _subscriberQueue.wake();
    
    return true;
  }
  
  public void ack(long sequence)
  {
    _queue.ack(sequence, this);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _queue + "]";
  }
  
  static class SubscriberEntryFactory implements RingItemFactory<SubscriberEntry> {
    @Override
    public SubscriberEntry createItem(int index)
    {
      return new SubscriberEntry(index);
    }
  }
  
  class SubscriberItemProcessor implements ItemProcessor<SubscriberEntry> {
    @Override
    public void process(SubscriberEntry item) throws Exception
    {
      InputStream is = new DataNodeInputStream(item.getDataHead());
      
      try {
        _processor.process(item.getSequence(), is, item.getLength());
      } finally {
        ack(item.getSequence());
      }
    }

    @Override
    public void onEmpty() throws Exception
    {
    }
  }
}
