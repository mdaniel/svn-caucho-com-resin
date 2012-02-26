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

import com.caucho.amqp.AmqpReceiver;
import com.caucho.amqp.AmqpReceiverFactory;
import com.caucho.amqp.AmqpSender;
import com.caucho.amqp.AmqpSenderFactory;
import com.caucho.amqp.transform.AmqpMessageDecoder;
import com.caucho.amqp.transform.AmqpMessageEncoder;
import com.caucho.amqp.transform.AmqpStringDecoder;
import com.caucho.amqp.transform.AmqpStringEncoder;


/**
 * AMQP client
 */
class AmqpClientSenderFactory implements AmqpSenderFactory {
  private AmqpConnectionImpl _client;
  
  private String _address;
  
  private AmqpMessageEncoder<?> _encoder = AmqpStringEncoder.ENCODER;
  
  AmqpClientSenderFactory(AmqpConnectionImpl client)
  {
    _client = client;
  }
  
  public String getAddress()
  {
    return _address;
  }

  @Override
  public AmqpSenderFactory setAddress(String address)
  {
    _address = address;
    
    return this;
  }
  
  @Override
  public AmqpSenderFactory setEncoder(AmqpMessageEncoder<?> encoder)
  {
    if (encoder == null)
      throw new NullPointerException();
    
    _encoder = encoder;
    
    return this;
  }
  
  @Override
  public AmqpMessageEncoder<?> getEncoder()
  {
    return _encoder;
  }

  @Override
  public AmqpSender<?> build()
  {
    return _client.buildSender(this);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _client + "]";
  }
}
