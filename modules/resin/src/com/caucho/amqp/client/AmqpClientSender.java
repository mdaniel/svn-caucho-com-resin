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
import java.util.logging.Logger;

import com.caucho.amqp.AmqpException;
import com.caucho.amqp.AmqpSender;
import com.caucho.amqp.common.AmqpSession;
import com.caucho.amqp.io.AmqpStreamWriter;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.MessageHeader;
import com.caucho.amqp.io.MessageProperties;
import com.caucho.amqp.marshal.AmqpMessageEncoder;
import com.caucho.message.MessagePropertiesFactory;
import com.caucho.message.MessageSettleListener;
import com.caucho.message.common.AbstractMessageSender;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;


/**
 * AMQP client
 */
class AmqpClientSender<T> extends AbstractMessageSender<T> implements AmqpSender<T> {
  private static final Logger log
    = Logger.getLogger(AmqpClientSender.class.getName());
  
  private AmqpClientConnectionImpl _client;
  private AmqpSession _session;
  
  private final String _address;
  private final AmqpMessageEncoder<T> _encoder;
  
  private AmqpClientSenderLink _link;
  
  private long _lastMessageId;
  
  AmqpClientSender(AmqpClientConnectionImpl client,
                   AmqpSession session,
                   AmqpClientSenderFactory factory)
  {
    super(factory);
    
    _client = client;
    _session = session;
    _address = factory.getAddress();
    
    _encoder = getMessageEncoder(factory);
    
    int linkId = _client.nextLinkId();
    
    _link = new AmqpClientSenderLink("client-" + _address + "-" + linkId, 
                                     _address, 
                                     this);
    
    _session.addSenderLink(_link, factory.getSettleMode());
  }
  
  @SuppressWarnings("unchecked")
  private AmqpMessageEncoder<T> getMessageEncoder(AmqpClientSenderFactory factory)
  {
    return (AmqpMessageEncoder) factory.getEncoder();
  }

  @Override
  protected boolean offerMicros(MessagePropertiesFactory<T> factory,
                                T value,
                                long timeoutMicros)
  {
    try {
      TempOutputStream tOut = new TempOutputStream();
      WriteStream os = Vfs.openWrite(tOut);
      AmqpStreamWriter sout = new AmqpStreamWriter(os);
      AmqpWriter aout = new AmqpWriter();
      aout.initBase(sout);
      
      MessageHeader header = new MessageHeader();
      
      header.setDurable(_encoder.isDurable(factory, value));
      header.setPriority(_encoder.getPriority(factory, value));
      header.setTimeToLive(_encoder.getTimeToLive(factory, value));
      header.setFirstAcquirer(_encoder.isFirstAcquirer(factory, value));
      header.setDeliveryCount(0);
      
      header.write(aout);
      
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
      
      _lastMessageId = _link.transfer(getSettleMode(), tOut.getInputStream());
      
      return true;
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }
  
  public long getLastMessageId()
  {
    return _lastMessageId;
  }
  
  public void accepted(long messageId)
  {
    
  }

  void onAccepted(long messageId)
  {
    MessageSettleListener listener = getSettleListener();
    
    if (listener != null) {
      listener.onAccept(messageId);
    }
  }

  @Override
  public void close()
  {
    _link.detach();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "," + _link.getName() + "]";
  }
}
