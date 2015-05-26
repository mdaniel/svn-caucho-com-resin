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
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.caucho.nautilus.DecoderMessage;
import com.caucho.nautilus.ReceiverConfig;
import com.caucho.nautilus.ReceiverListener;
import com.caucho.nautilus.ReceiverMode;
import com.caucho.nautilus.broker.BrokerNautilus;
import com.caucho.nautilus.broker.ReceiverBroker;
import com.caucho.nautilus.broker.ReceiverMessageHandler;
import com.caucho.nautilus.common.ReceiverQueueImpl;
import com.caucho.nautilus.encode.NautilusDecoder;
import com.caucho.util.L10N;

/**
 * basic message receiver includes a linked queue.
 */
public class ReceiverQueueLocal<T> extends ReceiverQueueImpl<T>
{
  private static final L10N L = new L10N(ReceiverQueueLocal.class);
  
  private static final Logger log
    = Logger.getLogger(ReceiverQueueLocal.class.getName());
  
  private BrokerNautilus _broker;

  private int _prefetch;

  private DecoderMessage<T> _decoder;

  private ReceiverBroker _receiver;

  private int _flow;
  
  public ReceiverQueueLocal(String address,
                                 ReceiverConfig<T> config,
                                 ReceiverListener<T> listener,
                                 BrokerNautilus broker)
  {
    super(address, config, listener);

    _broker = broker;

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
    _receiver = broker.createReceiver(address, distMode, nodeProperties, handler);
    
    if (_receiver == null) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown queue",
                                             address));
    }
    
    _flow = _prefetch;
    if (_flow > 0) {
      _receiver.flow(_flow);
    }
  }

  @Override
  protected void clientReceiveAck(long mid)
  {
    _receiver.accepted(0, mid);
  }
  
  @Override
  protected void updateFlow(long creditSequence)
  {
    _receiver.flow(creditSequence);
  }
  
  protected void sendClose()
  {
    
  }
  
  class LocalMessageHandler implements ReceiverMessageHandler {
    @Override
    public void onMessage(long seq, 
                          InputStream is,
                          long contentLength)
        throws IOException
    {
      T value = _decoder.decode(is);

      receiveEntry(value);
    }
  }
}
