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

package com.caucho.message.tourmaline;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import com.caucho.message.MessageEncoder;
import com.caucho.message.MessageException;
import com.caucho.message.MessagePropertiesFactory;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.message.common.AbstractMessageSender;
import com.caucho.remote.websocket.WebSocketClient;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * local connection to the message store
 */
public class NautilusClientSender<T> extends AbstractMessageSender<T> {
  private static final L10N L = new L10N(NautilusClientSender.class);
  
  private final String _address;
  private final String _queue;
  private final MessageEncoder<T> _encoder;
  
  private BrokerSender _publisher;
  private long _lastMessageId;
  
  private NautilusClientSenderEndpoint<T> _endpoint;
  
  private WriteStream _os;
  
  NautilusClientSender(NautilusSenderFactory factory)
  {
    super(factory);
    
    _address = factory.getAddress();
    _encoder = (MessageEncoder) factory.getMessageEncoder();
    
    int q = _address.indexOf("?queue=");
    
    _queue = _address.substring(q + "?queue=".length());

    connect();
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  MessageEncoder<T> getEncoder()
  {
    return _encoder;
  }
  
  private void connect()
  {
    try {
      _endpoint = new NautilusClientSenderEndpoint(this);
      
      WebSocketClient client = new WebSocketClient(_address, _endpoint);
      
      client.connect();

      _endpoint.sendPublish(_queue);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    _os = new WriteStream();
    _os.setReuseBuffer(true);
  }
  
  @Override
  protected boolean offerMicros(MessagePropertiesFactory<T> factory,
                                T value,
                                long timeoutMicros)
  {
    _endpoint.send(factory, value, timeoutMicros);
      
    return true;
  }
  
  @Override
  public long getLastMessageId()
  {
    return _lastMessageId;
  }

  @Override
  public int remainingCapacity()
  {
    return 0;
  }
  
  @Override
  public void close()
  {
    AbstractNautilusEndpoint endpoint = _endpoint;
    _endpoint = null;
    
    if (endpoint != null) {
      endpoint.close();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}
