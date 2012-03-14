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

import java.io.IOException;

import com.caucho.amqp.AmqpException;
import com.caucho.amqp.io.AmqpStreamWriter;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.MessageProperties;
import com.caucho.amqp.marshal.AmqpMessageEncoder;
import com.caucho.message.MessagePropertiesFactory;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.message.common.AbstractMessageSender;
import com.caucho.util.L10N;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;

/**
 * local connection to the message store
 */
public class LocalSender<T> extends AbstractMessageSender<T> {
  private static final L10N L = new L10N(LocalSender.class);
  
  private String _address;
  private AmqpMessageEncoder<T> _encoder;
  
  private BrokerSender _publisher;
  private long _lastMessageId;
  
  private WriteStream _os;
  
  LocalSender(LocalSenderFactory factory)
  {
    super(factory);
    
    _address = factory.getAddress();
    _encoder = (AmqpMessageEncoder) factory.getEncoder();
    
    EnvironmentMessageBroker broker = EnvironmentMessageBroker.getCurrent();
        
    _publisher = broker.createSender(_address);
    
    if (_publisher == null) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown queue",
                                             _address));
    }
    
    _os = new WriteStream();
    _os.setReuseBuffer(true);
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  @Override
  protected boolean offerMicros(MessagePropertiesFactory<T> factory,
                                T value,
                                long timeoutMicros)
  {
    try {
      TempOutputStream tOut = new TempOutputStream();
      WriteStream os = _os;
      os.init(new VfsStream(null, tOut));
      AmqpStreamWriter sout = new AmqpStreamWriter(os);
      AmqpWriter aout = new AmqpWriter();
      aout.initBase(sout);
      
      String contentType = _encoder.getContentType(value);
      
      if (contentType != null) {
        MessageProperties properties = new MessageProperties();
        
        properties.setContentType(contentType);
        
        properties.write(aout);
      }
    
      _encoder.encode(aout, value);
      
      sout.flush();
      os.flush();
      
      tOut.flush();
      tOut.close();

      long xid = 0;
      long mid = _publisher.nextMessageId();
      boolean isDurable = false;
      int priority = 4;
      long expireTime = 0;
      
      _lastMessageId = mid;
      
      _publisher.message(xid, mid, isDurable, priority, expireTime,
                         tOut.getHead().getBuffer(), 0, tOut.getLength(), 
                         tOut.getHead(), null);
      
      return true;
    } catch (IOException e) {
      throw new AmqpException(e);
    }
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
    BrokerSender pub = _publisher;
    _publisher = null;
    
    if (pub != null) {
      pub.close();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}
