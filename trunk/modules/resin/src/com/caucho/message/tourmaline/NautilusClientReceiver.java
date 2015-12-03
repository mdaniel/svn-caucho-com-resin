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

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.message.MessageDecoder;
import com.caucho.message.common.AbstractMessageReceiver;
import com.caucho.message.common.BasicMessageReceiver;
import com.caucho.remote.websocket.WebSocketClient;
import com.caucho.util.L10N;

/**
 * local connection to the message store
 */
public class NautilusClientReceiver<T> extends BasicMessageReceiver<T> {
  private static final L10N L = new L10N(NautilusClientReceiver.class);
  
  private static final Logger log
    = Logger.getLogger(NautilusClientReceiver.class.getName());
  
  private final String _queue;
  private final MessageDecoder<T> _decoder;
  
  private NautilusClientReceiverEndpoint<T> _endpoint;
  
  NautilusClientReceiver(NautilusReceiverFactory factory)
  {
    super(factory);
    
    String address = getAddress();
    
    int q = address.indexOf("?queue=");
    
    _queue = address.substring(q + "?queue=".length());

    _decoder = (MessageDecoder) factory.getMessageDecoder();
    
    Map<String,Object> nodeProperties = null;
/*    
    _linkCredit = _prefetch;
    if (_prefetch > 0) {
      _sub.flow(-1, _prefetch);
    }
    */
    
    connect();
  }
  
  MessageDecoder<T> getDecoder()
  {
    return _decoder;
  }

  private void connect()
  {
    try {
      _endpoint = new NautilusClientReceiverEndpoint(this);
      
      WebSocketClient client = new WebSocketClient(getAddress(), _endpoint);
      
      client.connect();

      _endpoint.sendReceive(_queue);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void updateFlow(int credit, long endpointSequence)
  {
    // XXX: threading issue here
    _endpoint.updateFlow(credit, endpointSequence);
  }
}
