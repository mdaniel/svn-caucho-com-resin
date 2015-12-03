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

import com.caucho.env.actor.ActorQueue;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.SenderSettleHandler;
import com.caucho.vfs.TempBuffer;

/**
 * Custom serialization for the cache
 */
public class NautilusBrokerPublisher implements BrokerSender
{
  private ActorQueue<NautilusRingItem> _nautilusQueue;
  
  private final long _qid;
  private long _mid;
  
  NautilusBrokerPublisher(long qid,
                          ActorQueue<NautilusRingItem> actorQueue)
  {
    _qid = qid;
    _nautilusQueue = actorQueue;
  }
  
  public int getPrefetch()
  {
    return _nautilusQueue.getAvailable();
  }
  
  @Override
  public long nextMessageId()
  {
    return ++_mid;
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
    NautilusRingItem entry = _nautilusQueue.startOffer(true);
    
    entry.initMessage(xid, _qid, mid,
                      isDurable, priority, expireTime,
                      buffer, offset, length, tBuf);
    
    _nautilusQueue.finishOffer(entry);
    
    _nautilusQueue.wake();
  }

  @Override
  public void close()
  {
  }
}
