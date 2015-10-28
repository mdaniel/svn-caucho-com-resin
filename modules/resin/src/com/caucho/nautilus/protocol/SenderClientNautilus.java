/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.nautilus.protocol;

import com.caucho.nautilus.EncoderMessage;
import com.caucho.nautilus.MessagePropertiesFactory;
import com.caucho.nautilus.broker.SenderBroker;
import com.caucho.nautilus.common.SenderQueueBase;
import com.caucho.v5.vfs.WriteStream;

/**
 * local connection to the message store
 */
public class SenderClientNautilus<T> extends SenderQueueBase<T> {
  private final String _address;
  private final String _queue;
  private final EncoderMessage<T> _encoder;
  
  private SenderBroker _publisher;
  private long _lastMessageId;
  
  private WriterNautilus _writer;
  
  private WriteStream _os;

  private EndpointSenderClientNautilus<T> _endpoint;

  private ConnectionClientNautilus _conn;
  
  SenderClientNautilus(SenderFactoryNautilus factory,
                       ConnectionClientNautilus conn)
  {
    super(factory.getSettleMode(), null); // settleListener
    
    _conn = conn;
    
    _address = factory.getAddress();
    _encoder = (EncoderMessage) factory.getMessageEncoder();
    
    int q = _address.indexOf("?queue=");
    
    _queue = _address.substring(q + "?queue=".length());

    connect();
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  EncoderMessage<T> getEncoder()
  {
    return _encoder;
  }
  
  private void connect()
  {
    try {
      _endpoint = new EndpointSenderClientNautilus<T>(this);
      
      _writer = _conn.connect(_address, _endpoint);

      _writer.sendSender(_queue);
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
    try {
      _writer.sendValueMessage(factory, value, _encoder, timeoutMicros);
      
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      
      return false;
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
    WriterNautilus writer = _writer;
    _writer = null;
    
    if (writer != null) {
      writer.close();
    }
  }

  /**
   * @param mid
   */
  void onAccept(long mid)
  {
    _lastMessageId = mid;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}
