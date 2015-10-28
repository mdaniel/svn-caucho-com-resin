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

import io.baratine.core.Direct;

import java.io.InputStream;

import com.caucho.nautilus.broker.SenderSettleHandler;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.vfs.StreamSource;
import com.caucho.v5.vfs.TempBuffer;

/**
 * Service for an individual queue, managing its messages.
 * 
 * The QueueService is a central nautilus service.
 */
abstract class QueueServiceBase
{
  private final BrokerServiceImpl _broker;
  private final String _name;
  private final long _qid;
  
  private long _sequence;
  
  QueueServiceBase(BrokerServiceImpl broker,
                     String name,
                     long qid)
  {
    _broker = broker;
    _name = name;
    _qid = qid;
    
    _sequence = broker.getInitialSequence();
  }

  @Direct
  public String getName()
  {
    return _name;
  }

  @Direct
  public long getId()
  {
    return _qid;
  }

  public BrokerServiceImpl getBroker()
  {
    return _broker;
  }
  
  protected long nextSequence()
  {
    return ++_sequence;
  }
  
  /**
   * Receive from remote.
   */
  public void onReceive(long messageId, StreamSource ss)
  {
    System.out.println("UNEXPECTED_ON_RECV: " + messageId + " " + ss + " " + this);
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + getName() + "," + Long.toHexString(getId()) + "]");
  }
}
