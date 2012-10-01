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

import com.caucho.message.journal.JournalFile;
import com.caucho.message.journal.JournalRingItem;
import com.caucho.vfs.TempBuffer;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public class NautilusRingItem extends JournalRingItem
{
  public static final int JE_CHECKPOINT = JournalFile.OP_CHECKPOINT;
  public static final int JE_MESSAGE = 0x02;
  public static final int JE_FLOW = 0x03;
  public static final int JE_SUBSCRIBE = 0x04;
  public static final int JE_UNSUBSCRIBE = 0x05;
  public static final int JE_ACCEPTED = 0x06;
  
  private static final byte []EMPTY_BUFFER = new byte[0];
  
  private NautilusBrokerSubscriber _subscriber;
  private long _deliveryCount;
  private int _credit;
  
  NautilusRingItem(int index)
  {
    super(index);
  }
  
  public void initAck(long xid,
                      long qid,
                      long mid,
                      NautilusBrokerSubscriber sub)
  {
    init(JE_ACCEPTED, xid, qid, mid, EMPTY_BUFFER, 0, 0, null);
    
    _subscriber = sub;
  }
  
  public void initSubscribe(long qid,
                            NautilusBrokerSubscriber subscriber)
  {
    init(JE_SUBSCRIBE, qid);
    
    _subscriber = subscriber;
  }
  
  public void initUnsubscribe(long qid,
                              NautilusBrokerSubscriber subscriber)
  {
    init(JE_UNSUBSCRIBE, qid);
    
    _subscriber = subscriber;
  }
  
  public void initFlow(long qid,
                       NautilusBrokerSubscriber subscriber,
                       long deliveryCount,
                       int credit)
  {
    init(JE_FLOW, qid);
    
    _subscriber = subscriber;
    _deliveryCount = deliveryCount;
    _credit = credit;
  }
  
  public NautilusBrokerSubscriber getSubscriber()
  {
    return _subscriber;
  }
  
  public long getDeliveryCount()
  {
    return _deliveryCount;
  }
  
  public int getCredit()
  {
    return _credit;
  }
  
  public void clear()
  {
    _subscriber = null;
  }

  public void initMessage(long xid,
                          long qid,
                          long mid,
                          boolean isDurable,
                          int priority,
                          long expireTime,
                          byte[] buffer, int offset, int length,
                          TempBuffer tBuf)
  {
    long code = NautilusMultiQueueActor.encode(JE_MESSAGE, isDurable, priority,
                                               expireTime);
    
    super.init(code, xid, qid, mid, buffer, offset, length, tBuf);
  }
}
