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

package com.caucho.message.local;

import com.caucho.amqp.transform.AmqpMessageEncoder;
import com.caucho.amqp.transform.AmqpStringEncoder;
import com.caucho.message.MessageConnection;
import com.caucho.message.MessageReceiver;
import com.caucho.message.MessageReceiverFactory;
import com.caucho.message.MessageSender;
import com.caucho.message.MessageSenderFactory;

/**
 * local connection to the message store
 */
public class LocalSenderFactory implements MessageSenderFactory {
  private String _address;
  
  private AmqpMessageEncoder<?> _encoder = new AmqpStringEncoder();

  @Override
  public LocalSenderFactory setAddress(String address)
  {
    _address = address;
    
    return this;
  }

  @Override
  public String getAddress()
  {
    return _address;
  }
  
  public AmqpMessageEncoder<?> getEncoder()
  {
    return (AmqpMessageEncoder) _encoder;
  }

  @Override
  public MessageSender<?> build()
  {
    return new LocalSender(this);
  }
}
