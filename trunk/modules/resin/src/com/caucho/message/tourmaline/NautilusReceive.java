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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Logger;

import com.caucho.message.DistributionMode;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.MessageBroker;
import com.caucho.message.broker.ReceiverMessageHandler;
import com.caucho.message.broker.SenderSettleHandler;
import com.caucho.vfs.TempBuffer;
import com.caucho.websocket.WebSocketContext;

/**
 * Custom serialization for the cache
 */
class NautilusReceive implements ReceiverMessageHandler
{
  private static final Logger log
    = Logger.getLogger(NautilusReceive.class.getName());
  
  private NautilusServerEndpoint _endpoint;
  private MessageBroker _broker;
  private String _name;
  
  private BrokerReceiver _sub;
  
  private TempBuffer _tBuf = TempBuffer.allocate();
  
  private long _sequence;
  
  NautilusReceive(NautilusServerEndpoint endpoint)
  {
    _endpoint = endpoint;
    _broker = endpoint.getBroker();
  }
  
  void add(String key, String value)
  {
    if ("name".equals(key)) {
      _name = value;
    }
  }
  
  void init()
  {
    if (_name == null) {
      throw new IllegalStateException("'name' is required");
    }
    
    Map<String,Object> properties = null;
    
    _sub = _broker.createReceiver(_name,
                                  DistributionMode.MOVE,
                                  properties, 
                                  this);

    System.out.println("SUB: " + _sub);
    
    if (_sub == null)
      throw new IllegalStateException(_name + " is an unknown queue");
    
    flow(0, 1);
  }
  
  public void flow(long count, int credit)
  {
    _sub.flow(count, credit);
  }

  @Override
  public void onMessage(long messageId, 
                        InputStream is,
                        long contentLength)
    throws IOException
  {
    WebSocketContext wsContext = _endpoint.getContext();
    
    OutputStream os = wsContext.startBinaryMessage();
    
    System.out.println("ONMSG: " + messageId);
    
    try {
      os.write(NautilusCode.SEND.ordinal());
    
      byte []buffer = _tBuf.getBuffer();
      int len;

      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        os.write(buffer, 0, len);
      }
    } finally {
      os.close();
    }
  }

  public void close()
  {
    _sub.close();
  }
}
