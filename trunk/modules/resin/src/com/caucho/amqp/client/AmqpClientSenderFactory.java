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

import java.util.HashMap;
import java.util.Map;

import com.caucho.amqp.AmqpReceiverFactory;
import com.caucho.amqp.AmqpSender;
import com.caucho.amqp.AmqpSenderFactory;
import com.caucho.amqp.marshal.AmqpMessageEncoder;
import com.caucho.amqp.marshal.AmqpStringEncoder;
import com.caucho.message.MessageSettleListener;
import com.caucho.message.SettleMode;
import com.caucho.message.common.AbstractMessageSenderFactory;


/**
 * AMQP client
 */
class AmqpClientSenderFactory extends AbstractMessageSenderFactory
  implements AmqpSenderFactory
{
  private AmqpClientConnectionImpl _client;
  
  private AmqpMessageEncoder<?> _encoder = AmqpStringEncoder.ENCODER;
  
  private HashMap<String,Object> _attachProperties;
  private HashMap<String,Object> _sourceProperties;
  private HashMap<String,Object> _targetProperties;
  
  AmqpClientSenderFactory(AmqpClientConnectionImpl client)
  {
    _client = client;
  }

  @Override
  public AmqpClientSenderFactory setAddress(String address)
  {
    super.setAddress(address);
    
    return this;
  }

  @Override
  public AmqpClientSenderFactory setSettleMode(SettleMode settleMode)
  {
    super.setSettleMode(settleMode);
    
    return this;
  }

  @Override
  public AmqpClientSenderFactory setSettleListener(MessageSettleListener listener)
  {
    super.setSettleListener(listener);
    
    return this;
  }
  
  @Override
  public AmqpClientSenderFactory setEncoder(AmqpMessageEncoder<?> encoder)
  {
    if (encoder == null) {
      throw new NullPointerException();
    }
    
    _encoder = encoder;
    
    return this;
  }
  
  @Override
  public AmqpMessageEncoder<?> getEncoder()
  {
    return _encoder;
  }

  @Override
  public AmqpSenderFactory setAttachProperty(String key, Object value)
  {
    if (_attachProperties == null) {
      _attachProperties = new HashMap<String,Object>();
    }
    
    _attachProperties.put(key, value);
    
    return this;
  }
  
  Map<String,Object> getAttachProperties()
  {
    return _attachProperties;
  }

  @Override
  public AmqpSenderFactory setSourceProperty(String key, Object value)
  {
    if (_sourceProperties == null) {
      _sourceProperties = new HashMap<String,Object>();
    }
    
    _sourceProperties.put(key, value);
    
    return this;
  }
  
  Map<String,Object> getSourceProperties()
  {
    return _sourceProperties;
  }

  @Override
  public AmqpSenderFactory setTargetProperty(String key, Object value)
  {
    if (_targetProperties == null) {
      _targetProperties = new HashMap<String,Object>();
    }
    
    _targetProperties.put(key, value);
    
    return this;
  }
  
  Map<String,Object> getTargetProperties()
  {
    return _targetProperties;
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
