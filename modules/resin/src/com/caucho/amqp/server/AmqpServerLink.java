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

import com.caucho.amqp.common.AmqpLink;
import com.caucho.amqp.common.AmqpSession;
import com.caucho.amqp.io.FrameAttach;
import com.caucho.amqp.io.FrameFlow;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.SenderSettleHandler;

/**
 * link session management
 */
public class AmqpServerLink extends AmqpLink
{
  private BrokerSender _pub;
  private MessageSettleHandler _settleHandler;
  
  public AmqpServerLink(AmqpServerSession session,
                        FrameAttach attach,
                        BrokerSender pub)
  {
    super(session, attach);
    
    _pub = pub;
  }
  
  @Override
  public AmqpServerSession getSession()
  {
    return (AmqpServerSession) super.getSession();
  }
  
  public long nextMessageId()
  {
    return _pub.nextMessageId();
  }
  
  public void write(long xid, long mid, boolean isSettled,
             boolean isDurable, int priority, long expireTime,
             byte []buffer, int offset, int length)
  {
    SenderSettleHandler handler = null;
    
    if (! isSettled) {
      handler = new MessageSettleHandler(mid);
    }
    
    System.out.println("SETTLE: " + isSettled);
    _pub.message(xid, mid, isDurable, priority, expireTime,
                 buffer, offset, length, null, handler);
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
  
  class MessageSettleHandler implements SenderSettleHandler {
    private final long _deliveryId;
    
    MessageSettleHandler(long deliveryId)
    {
      _deliveryId = deliveryId;
    }
    
    @Override
    public void onAccepted(long mid)
    {
      getSession().onAccepted(_deliveryId);
    }

    @Override
    public void onRejected(long mid, String msg)
    {
      getSession().onRejected(_deliveryId, msg);
    }
  }
}
