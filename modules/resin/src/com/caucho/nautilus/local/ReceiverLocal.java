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

package com.caucho.nautilus.local;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.nautilus.DecoderMessage;
import com.caucho.nautilus.ReceiverConfig;
import com.caucho.nautilus.ReceiverMode;
import com.caucho.nautilus.broker.BrokerNautilus;
import com.caucho.nautilus.broker.ReceiverBroker;
import com.caucho.nautilus.broker.ReceiverMessageHandler;
import com.caucho.nautilus.common.ReceiverQueueBase;
import com.caucho.nautilus.encode.NautilusDecoder;
import com.caucho.v5.util.L10N;

/**
 * local connection to the message store
 */
public class ReceiverLocal<T> extends ReceiverQueueBase<T> {
  private static final L10N L = new L10N(ReceiverLocal.class);
  
  private static final Logger log
    = Logger.getLogger(ReceiverLocal.class.getName());
  
  private String _address;
  
  private int _prefetch;
  
  private long _messageSequence;
  private long _flow;
  
  private DecoderMessage<T> _decoder;
  
  private LinkedBlockingQueue<LocalMessage> _queue
    = new LinkedBlockingQueue<LocalMessage>();
  
  private ReceiverBroker _receiver;
  
  public ReceiverLocal(String address,
                ReceiverConfig<T> config,
                BrokerNautilus broker)
  {
    _address = address;
    _prefetch = config.getPrefetch();
    
    Supplier<DecoderMessage<T>> supplier = config.getMessageDecoderSupplier();
    
    if (supplier != null) {
      _decoder = supplier.get();
    }
    else {
      _decoder = (DecoderMessage) new NautilusDecoder();
    }
    
    ReceiverMessageHandler handler = new LocalMessageHandler();
    
    ReceiverMode distMode = config.getDistributionMode();
    
    Map<String,Object> nodeProperties = new HashMap<>();
    _receiver = broker.createReceiver(_address, distMode, nodeProperties, handler);
    
    if (_receiver == null) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown queue",
                                             _address));
    }
    
    _flow = _prefetch;
    if (_flow > 0) {
      _receiver.flow(_flow);
    }
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  @Override
  protected T pollMicros(long timeoutMicros)
  {
    try {
      LocalMessage entry = _queue.poll(timeoutMicros, TimeUnit.MICROSECONDS);
      
      if (entry == null) {
        return null;
      }
      
      long sequence = entry.getSequence();
      
      _messageSequence = sequence;
      
      InputStream is = entry.getInputStream();
      
      T value = _decoder.decode(is);

      _receiver.accepted(0, sequence);
      
      return value;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    } catch (InterruptedException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    } finally {
      long remaining = _flow - _messageSequence;

      if (remaining <= (_prefetch >> 2)) {
        _flow = Math.max(_messageSequence + _prefetch, _flow);
        
        _receiver.flow(_flow);
      }
    }
  }
  
  @Override
  public void close()
  {
    _receiver.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _receiver.getId() + "]";
  }
  
  class LocalMessageHandler implements ReceiverMessageHandler {
    @Override
    public void onMessage(long seq, 
                          InputStream bodyIs, 
                          long contentLength)
        throws IOException
    {
      _queue.add(new LocalMessage(seq, bodyIs));
    }
  }
  
  static class LocalMessage {
    private long _seq;
    private InputStream _is;
    
    LocalMessage(long seq, InputStream is)
    {
      _seq = seq;
      _is = is;
    }
    
    public long getSequence()
    {
      return _seq;
    }
    
    public InputStream getInputStream()
    {
      return _is;
    }
  }
}
