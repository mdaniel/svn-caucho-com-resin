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

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.AmqpException;
import com.caucho.amqp.AmqpSender;
import com.caucho.amqp.io.AmqpAbstractComposite;
import com.caucho.amqp.io.AmqpAbstractPacket;
import com.caucho.amqp.io.AmqpFrameHandler;
import com.caucho.amqp.io.AmqpStreamWriter;
import com.caucho.amqp.io.FrameBegin;
import com.caucho.amqp.io.FrameOpen;
import com.caucho.amqp.io.AmqpConstants;
import com.caucho.amqp.io.AmqpError;
import com.caucho.amqp.io.AmqpAbstractFrame;
import com.caucho.amqp.io.AmqpFrameReader;
import com.caucho.amqp.io.AmqpFrameWriter;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.MessageHeader;
import com.caucho.amqp.io.MessageProperties;
import com.caucho.amqp.marshal.AmqpMessageEncoder;
import com.caucho.amqp.marshal.AmqpStringEncoder;
import com.caucho.message.MessageFactory;
import com.caucho.message.common.AbstractMessageSender;
import com.caucho.util.L10N;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.QSocketWrapper;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;


/**
 * AMQP client
 */
class AmqpClientSender<T> extends AbstractMessageSender<T> implements AmqpSender<T> {
  private static final long TIMEOUT_INFINITY = Long.MAX_VALUE / 2;
  
  private static final Logger log
    = Logger.getLogger(AmqpClientSender.class.getName());
  
  private AmqpClientConnectionImpl _client;
  
  private final String _address;
  private boolean _isAutoSettle;
  private AmqpClientLink _link;
  
  private AmqpMessageEncoder<T> _encoder;
  
  AmqpClientSender(AmqpClientConnectionImpl client,
                   AmqpClientSenderFactory factory,
                   AmqpClientLink link)
  {
    _client = client;
    _address = factory.getAddress();
    _isAutoSettle = factory.isAutoSettle();
    
    _link = link;
    _encoder = (AmqpMessageEncoder) factory.getEncoder();
  }

  @Override
  protected boolean offerMicros(MessageFactory<T> factory,
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
      
      _client.transfer(_link, _isAutoSettle, tOut.getInputStream());
      
      return true;
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  @Override
  public void close()
  {
    _client.closeSender(_link);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "," + _link.getName() + "]";
  }
}
