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

package com.caucho.amqp.server;

import com.caucho.amqp.io.FrameAttach;
import com.caucho.amqp.io.FrameFlow;
import com.caucho.message.broker.BrokerSubscriber;
import com.caucho.message.broker.BrokerPublisher;

/**
 * link session management
 */
public class AmqpLink
{
  private int _handle;
  
  private FrameAttach _attach;
  
  private BrokerPublisher _pub;
  
  public AmqpLink(FrameAttach attach, BrokerPublisher pub)
  {
    this(attach);
    
    _pub = pub;
  }
  
  public AmqpLink(FrameAttach attach)
  {
    _handle = attach.getHandle();
    _attach = attach;
  }
  
  public int getHandle()
  {
    return _handle;
  }
  
  void write(long xid, byte []buffer, int offset, int length)
  {
    _pub.messageComplete(xid, buffer, offset, length, null);
  }

  /**
   * @param messageId
   */
  public void accept(long xid, long messageId)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @param messageId
   */
  public void reject(long xid, long messageId, String message)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @param messageId
   */
  public void release(long xid, long messageId)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void modified(long xid,
                       long mid, 
                       boolean isFailed, 
                       boolean isUndeliverableHere)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @param flow
   */
  public void onFlow(FrameFlow flow)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _handle + "," + _attach.getName() + "]";
  }
}
