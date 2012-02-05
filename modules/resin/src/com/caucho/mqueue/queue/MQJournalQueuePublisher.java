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

import com.caucho.mqueue.MQueueDisruptor;
import com.caucho.mqueue.journal.MQueueJournalEntry;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempBuffer;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public class MQJournalQueuePublisher
{
  private MQJournalQueue _queue;
  private MQueueDisruptor<MQueueJournalEntry> _disruptor;
  
  private long _id = 3;
  private long _sequence;
  
  MQJournalQueuePublisher(MQJournalQueue queue)
  {
    _queue = queue;
    _disruptor = queue.getDisruptor();
  }
  
  public void write(byte []buffer, int offset, int length, TempBuffer tBuf)
  {
    long sequence = _sequence++;
    
    MQueueJournalEntry entry = _disruptor.startProducer(true);
    
    entry.init('D', _id, sequence, buffer, offset, length, null, tBuf);
    
    _disruptor.finishProducer(entry);
    
    _disruptor.wake();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _queue + "]";
  }
}
