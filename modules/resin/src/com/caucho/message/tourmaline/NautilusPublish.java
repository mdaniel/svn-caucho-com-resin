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
import java.util.Map;
import java.util.logging.Logger;

import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.MessageBroker;
import com.caucho.message.broker.SenderSettleHandler;
import com.caucho.vfs.TempBuffer;

/**
 * Custom serialization for the cache
 */
class NautilusPublish implements SenderSettleHandler
{
  private static final Logger log
    = Logger.getLogger(NautilusPublish.class.getName());
  
  private NautilusServerEndpoint _endpoint;
  private MessageBroker _broker;
  private String _name;
  
  private BrokerSender _pub;
  
  private long _sequence;
  
  NautilusPublish(NautilusServerEndpoint endpoint)
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
    
    _pub = _broker.createSender(_name, properties);

    if (_pub == null)
      throw new IllegalStateException(_name + " is an unknown queue");
  }
  
  void publish(InputStream is)
    throws IOException
  {
    long xid = 0;
    long mid = _pub.nextMessageId();
    boolean isDurable = false;
    int priority = 4;
    long expireTime = 0;
    
    TempBuffer tBuf = null;
    boolean isFinal;
    
    do {
      int avail = is.available();

      if (avail <= 256)
        tBuf = TempBuffer.allocateSmall();
      else
        tBuf = TempBuffer.allocate();
      
      byte []buffer = tBuf.getBuffer();
      int len = buffer.length;
      
      len = is.read(buffer, 0, len);
      
      if (len <= 0) {
        System.out.println("UNExPECTED EOF:");
        return;
      }
      
      isFinal = is.available() < 0;
      
      System.out.println("MSG: " + is + " " + isFinal + " " + new String(buffer, 0, len) + " " + _pub);
      
      _pub.message(xid, mid, isDurable, priority, expireTime, 
                   buffer, 0, len,
                   tBuf, this);
    } while (! isFinal);
  }

  @Override
  public boolean isSettled()
  {
    return true;
  }

  @Override
  public void onAccepted(long mid)
  {
  }

  @Override
  public void onRejected(long mid, String msg)
  {
  }

  public void close()
  {
    _pub.close();
  }
}
