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

package com.caucho.v5.nautilus.local;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import com.caucho.v5.io.TempOutputStream;
import com.caucho.v5.nautilus.EncoderMessage;
import com.caucho.v5.nautilus.MessageException;
import com.caucho.v5.nautilus.MessagePropertiesFactory;
import com.caucho.v5.nautilus.SenderQueueConfig;
import com.caucho.v5.nautilus.broker.BrokerNautilus;
import com.caucho.v5.nautilus.broker.SenderBroker;
import com.caucho.v5.nautilus.common.SenderQueueBase;
import com.caucho.v5.nautilus.encode.StringEncoder;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.WriteStream;

/**
 * local connection to the message store
 */
public class SenderQueueLocal<T> extends SenderQueueBase<T>
{
  private static final L10N L = new L10N(SenderQueueLocal.class);
  
  private String _address;
  private EncoderMessage<T> _encoder;
  
  private SenderBroker _sender;
  private long _lastMessageId;
  
  private WriteStream _os;
  
  public SenderQueueLocal(String address,
              SenderQueueConfig<T> config,
              Settler settler,
              BrokerNautilus broker)
  {
    super(config.getSettleMode(), null); // settle listener
    
    _address = address;
    
    Supplier<EncoderMessage<T>> supplier = config.getEncoderSupplier();
    
    if (supplier != null) {
      _encoder = supplier.get();
    }
    else {
      _encoder = (EncoderMessage) new StringEncoder();
    }
    
    // EnvironmentMessageBroker broker = EnvironmentMessageBroker.getCurrent();
        
    Map<String,Object> nodeProperties = null;
    _sender = broker.createSender(_address, nodeProperties);
    
    if (_sender == null) {
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
     
      _encoder.encode(tOut, value);

      tOut.flush();
      tOut.close();

      long xid = 0;
      long mid = _sender.nextMessageId();
      boolean isDurable = false;
      int priority = factory.getPriority();
      long expireTime = 0;
      
      _lastMessageId = mid;
      
      _sender.message(xid, mid, isDurable, priority, expireTime,
                         tOut.getHead().buffer(), 0, tOut.getLength(), 
                         tOut.getHead(), null);
      
      return true;
    } catch (IOException e) {
      throw new MessageException(e);
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
    SenderBroker sender = _sender;
    _sender = null;
    
    if (sender!= null) {
      sender.close();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _sender.getId() + "]";
  }
}
