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

package com.caucho.amqp.client;

import com.caucho.amqp.common.AmqpLink;
import com.caucho.amqp.common.AmqpSession;
import com.caucho.amqp.io.FrameAttach;

/**
 * link session management
 */
public class AmqpClientLink extends AmqpLink
{
  private AmqpClientReceiver<?> _receiver;
  private AmqpClientSender<?> _sender;
  
  public AmqpClientLink(AmqpClientSession session, FrameAttach attach)
  {
    super(session, attach);
  }
  
  @Override
  public AmqpClientSession getSession()
  {
    return (AmqpClientSession) super.getSession();
  }
  
  public long nextMessageId()
  {
    return 3;
  }
  
  public void write(long xid, long mid, boolean isSettled,
             boolean isDurable, int priority, long expireTime,
             byte []buffer, int offset, int length)
  {
  }

  public AmqpClientReceiver<?> getReceiver()
  {
    return _receiver;
  }

  public void setReceiver(AmqpClientReceiver<?> receiver)
  {
    _receiver = receiver;
  }

  public AmqpClientSender<?> getSender()
  {
    return _sender;
  }

  public void setSender(AmqpClientSender<?> sender)
  {
    _sender = sender;
  }

  /**
   * @param messageId
   */
  @Override
  public void onAccept(long xid, long messageId)
  {
    getSession().accepted(messageId);
    
    getSender().onAccept(messageId);
  }
}
