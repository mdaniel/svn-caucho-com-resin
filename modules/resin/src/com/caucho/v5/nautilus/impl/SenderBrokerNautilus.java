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

import java.util.Objects;

import com.caucho.v5.nautilus.broker.SenderBrokerBase;
import com.caucho.v5.nautilus.broker.SenderSettleHandler;
import com.caucho.v5.vfs.TempBuffer;

/**
 * Custom serialization for the cache
 */
public class SenderBrokerNautilus extends SenderBrokerBase
{
  // private final ServiceQueue<NautilusJournalItem> _journalQueue;
  
  private QueueService _queue;
  private long _sid;
  private long _sequence;

  private int _prefetch;

  public SenderBrokerNautilus(long sid, QueueService queue)
  {
    Objects.requireNonNull(queue);
    
    _sid = sid;
    _queue = queue;
    
    _prefetch = 16;
  }
  
  public String getId()
  {
    return _queue.getName();
  }

  @Override
  public int getPrefetch()
  {
    return _prefetch; // _journalQueue.size();
  }
  
  @Override
  public long nextMessageId()
  {
    return ++_sequence;
  }
  
  @Override
  public void message(long xid,
                      long mid,
                      boolean isDurable,
                      int priority,
                      long expireTime,
                      byte []buffer,
                      int offset,
                      int length,
                      TempBuffer tBuf,
                      SenderSettleHandler settleHandler)
  {
    /*
    JournalSend entry
      = new JournalSend(_actorSender,
                        (int) _actorSender.getId(), 0, mid, priority, expireTime,
                        buffer, offset, length, tBuf);
                        */
    
    _queue.message(_sid, mid, buffer, offset, length, tBuf, settleHandler);
    
    //_journalQueue.offer(entry);
  }

  @Override
  public void close()
  {
  }
}
