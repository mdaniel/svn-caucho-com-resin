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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.caucho.nautilus.ReceiverMode;
import com.caucho.nautilus.broker.ReceiverBroker;
import com.caucho.nautilus.broker.BrokerNautilus;
import com.caucho.nautilus.broker.ReceiverMessageHandler;

/**
 * Custom serialization for the cache
 */
class ReceiverServerNautilus implements ReceiverMessageHandler
{
  private EndpointServerNautilus _endpoint;
  private BrokerNautilus _broker;
  private String _name;
  
  private WriterNautilus _writer;
  
  private HashMap<String,Object> _properties = new HashMap<>();
  
  private ReceiverBroker _receiver;
  
  ReceiverServerNautilus(EndpointServerNautilus endpoint)
  {
    _endpoint = endpoint;
    _writer = endpoint.getWriter();
    _broker = endpoint.getBroker();
  }
  
  void add(String key, String value)
  {
    if ("name".equals(key)) {
      _name = value;
    }
    else {
      _properties.put(key, value);
    }
  }
  
  void init()
  {
    if (_name == null) {
      throw new IllegalStateException("'name' is required");
    }
    
    _receiver = _broker.createReceiver(_name,
                                       ReceiverMode.CONSUME,
                                       _properties, 
                                       this);

    if (_receiver == null)
      throw new IllegalStateException(_name + " is an unknown queue");
    
    // flow(1);
  }
  
  public String getId()
  {
    return _receiver.getId();
  }
  
  public void flow(long credit)
  {
    _receiver.flow(credit);
  }
  
  public void accept(long messageId)
  {
    _receiver.accepted(0, messageId);
    
    try {
      _writer.sendAckAck(messageId);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onMessage(long messageId, 
                        InputStream is,
                        long contentLength)
    throws IOException
  {
      _writer.sendStreamMessage(messageId, is, contentLength);
  }
  
  public void disconnect()
  {
    ReceiverBroker receiver = _receiver;
    
    if (receiver != null) {
      receiver.disconnect();
    }
  }

  public void close()
  {
    ReceiverBroker receiver = _receiver;
    
    if (receiver != null) {
      receiver.close();
    }
  }
}
