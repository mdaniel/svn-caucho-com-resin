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

import java.io.IOException;
import java.io.InputStream;

import com.caucho.amqp.io.FrameAttach;
import com.caucho.amqp.io.FrameFlow;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.ReceiverMessageHandler;

/**
 * link session management
 */
public class AmqpServerReceiverLink extends AmqpServerLink implements ReceiverMessageHandler
{
  private BrokerReceiver _sub;
  
  public AmqpServerReceiverLink(AmqpServerSession session,
                                FrameAttach attach)
  {
    super(session, attach);
  }
  
  void setReceiver(BrokerReceiver sub)
  {
    _sub = sub;
  }

  /**
   * @param flow
   */
  @Override
  public void onFlow(FrameFlow flow)
  {
    long xid = 0;
    
    _sub.flow(xid, flow.getDeliveryCount(), flow.getLinkCredit());
  }

  @Override
  public void onMessage(long messageId, 
                        InputStream bodyIs, 
                        long contentLength)
    throws IOException
  {
    AmqpServerSession session = getSession();
    
    session.writeMessage(this, messageId, bodyIs, contentLength);
  }
  
  @Override
  public void onAccept(long xid, long messageId)
  {
    _sub.accept(xid, messageId);
  }
  
  @Override
  public void reject(long xid, long messageId, String message)
  {
    _sub.reject(xid, messageId, message);
  }
  
  @Override
  public void modified(long xid,
                       long mid, 
                       boolean isFailed, 
                       boolean isUndeliverableHere)
  {
    _sub.modified(xid, mid, isFailed, isUndeliverableHere);
  }
  
  @Override
  public void release(long xid, long messageId)
  {
    _sub.release(xid, messageId);
  }
}
