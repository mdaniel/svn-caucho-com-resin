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

import com.caucho.amqp.broker.AmqpMessageListener;
import com.caucho.amqp.broker.AmqpBrokerReceiver;
import com.caucho.amqp.broker.AmqpBrokerSender;
import com.caucho.amqp.io.FrameAttach;
import com.caucho.amqp.io.FrameFlow;

/**
 * link session management
 */
public class AmqpReceiverLink extends AmqpLink implements AmqpMessageListener
{
  private AmqpServerConnection _conn;
  
  private AmqpBrokerReceiver _sub;
  
  public AmqpReceiverLink(AmqpServerConnection conn, FrameAttach attach)
  {
    super(attach);
    
    _conn = conn;
  }
  
  void setReceiver(AmqpBrokerReceiver sub)
  {
    _sub = sub;
  }

  /**
   * @param flow
   */
  @Override
  public void onFlow(FrameFlow flow)
  {
    _sub.flow(flow.getDeliveryCount(), flow.getLinkCredit());
  }

  @Override
  public void onMessage(long messageId, 
                        InputStream bodyIs, 
                        long contentLength)
    throws IOException
  {
    _conn.writeMessage(this, messageId, bodyIs, contentLength);
  }
  
  @Override
  public void accept(long messageId)
  {
    _sub.accept(messageId);
  }
  
  @Override
  public void reject(long messageId)
  {
    _sub.reject(messageId);
  }
  
  @Override
  public void release(long messageId)
  {
    _sub.release(messageId);
  }
}
