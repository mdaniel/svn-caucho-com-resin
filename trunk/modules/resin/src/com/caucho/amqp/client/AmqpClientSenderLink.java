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

import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.common.AmqpSenderLink;
import com.caucho.message.SettleMode;

/**
 * link session management
 */
public class AmqpClientSenderLink extends AmqpSenderLink
{
  private static final Logger log
    = Logger.getLogger(AmqpClientSenderLink.class.getName());
  
  private final AmqpClientSender<?> _sender;
  
  public AmqpClientSenderLink(String name,
                              String address,
                              AmqpClientSender<?> sender)
  {
    super(name, address);
    
    _sender = sender;
  }
  
  public long nextMessageId()
  {
    return 3;
  }

  public AmqpClientSender<?> getSender()
  {
    return _sender;
  }
  
  @Override
  public Map<String,Object> getAttachProperties()
  {
    return _sender.getAttachProperties();
  }
  
  @Override
  public Map<String,Object> getSourceProperties()
  {
    return _sender.getSourceProperties();
  }
  
  @Override
  public Map<String,Object> getTargetProperties()
  {
    return _sender.getTargetProperties();
  }

  /**
   * Transfers a message, returning the message id.
   */
  long transfer(SettleMode settleMode, InputStream is)
  {
    long mid = nextMessageId();

    transfer(mid, settleMode, is);
    
    return mid;
  }

  /**
   * @param messageId
   */
  @Override
  public void onAccepted(long xid, long messageId)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " onAccepted(" + messageId + ")");
    }
    
    // super.onAccepted(xid, messageId);
    // getSession().accepted(messageId);
    
    getSender().onAccepted(messageId);
  }

  /**
   * 
   */
  public void detach()
  {
    // TODO Auto-generated method stub
    
  }
}
