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

import java.io.IOException;
import java.util.Map;

import com.caucho.amqp.common.AmqpReceiverLink;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.FrameTransfer;

/**
 * link session management
 */
public class AmqpClientReceiverLink extends AmqpReceiverLink
{
  private final AmqpClientReceiver<?> _receiver;
  
  public AmqpClientReceiverLink(String name, 
                                String address,
                                AmqpClientReceiver<?> receiver)
  {
    super(name, address);
    
    _receiver = receiver;
  }

  public AmqpClientReceiver<?> getReceiver()
  {
    return _receiver;
  }
  
  @Override
  public Map<String,Object> getAttachProperties()
  {
    return _receiver.getAttachProperties();
  }
  
  @Override
  public Map<String,Object> getSourceProperties()
  {
    return _receiver.getSourceProperties();
  }
  
  @Override
  public Map<String,Object> getTargetProperties()
  {
    return _receiver.getTargetProperties();
  }
  
  //
  // transfer callbacks
  //
  
  /**
   * Receives the message from the network
   */
  @Override
  protected void onTransfer(FrameTransfer frameTransfer,
                            AmqpReader ain)
    throws IOException
  {
    super.onTransfer(frameTransfer, ain);
    
    _receiver.receive(frameTransfer.getDeliveryId(), ain);
  }

  /**
   * 
   */
  public void detach()
  {
    // TODO Auto-generated method stub
    
  }
}
