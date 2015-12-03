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

import com.caucho.util.RingItem;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public class SubscriberRingItem extends RingItem
{
  private static final byte []EMPTY_BUFFER = new byte[0];
  
  private long _sequence;
  private MessageDataNode _dataHead;
  
  SubscriberRingItem(int index)
  {
    super(index);
  }
  
  public final void initQueueData(long sequence, MessageDataNode dataHead)
  {
    _sequence = sequence;
    
    _dataHead = dataHead;
  }
  
  public final long getSequence()
  {
    return _sequence;
  }
  
  public final MessageDataNode getDataHead()
  {
    return _dataHead;
  }
  
  public final long getLength()
  {
    long length = 0;
    
    for (MessageDataNode ptr = _dataHead; ptr != null; ptr = ptr.getNext()) {
      length += ptr.getLength();
    }
    
    return length;
  }
  
  public void clear()
  {
    _sequence = 0;
    _dataHead = null;
  }
}
