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

package com.caucho.v5.nautilus.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.nautilus.DecoderMessage;
import com.caucho.v5.nautilus.common.ReceiverQueueImpl;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;

/**
 * local connection to the message store
 */
public class ReceiverClientNautilus<T> extends ReceiverQueueImpl<T> {
  private static final Logger log
    = Logger.getLogger(ReceiverClientNautilus.class.getName());
  
  private final String _queue;
  private final DecoderMessage<T> _decoder;
  
  private EndpointReceiverClientNautilus<T> _endpoint;

  private WriterNautilus _writer;

  private ConnectionClientNautilus _conn;
  
  private String _sessionId;
  private Reconnect _reconnect;
  private Alarm _reconnectAlarm;
  private int _reconnectCount;
  
  private ArrayList<Long> _ackList = new ArrayList<>();
  
  ReceiverClientNautilus(ReceiverFactoryNautilus factory, 
                         ConnectionClientNautilus conn)
  {
    super(factory.getAddress(),
          null, // config
          null); // factory.getListener());
    
    _conn = conn;
    
    _reconnect = new Reconnect();
    _reconnectAlarm = new Alarm(_reconnect);
    
    String address = getAddress();
    
    int q = address.indexOf("?queue=");
    
    _queue = address.substring(q + "?queue=".length());

    _decoder = (DecoderMessage) factory.getMessageDecoder();
    
    Map<String,Object> nodeProperties = null;
/*    
    _linkCredit = _prefetch;
    if (_prefetch > 0) {
      _sub.flow(-1, _prefetch);
    }
    */
    
    connect();
    
    updateCredit();
  }
  
  DecoderMessage<T> getDecoder()
  {
    return _decoder;
  }

  private void connect()
  {
    try {
      _endpoint = new EndpointReceiverClientNautilus(this);
      
      initCredits();
      
      _writer = _conn.connect(getAddress(), _endpoint);
      
      HashMap<String,String> properties = new HashMap<>();
      
      if (_sessionId != null) {
        properties.put("id", _sessionId);
      }
      
      if (_ackList.size() > 0) {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < _ackList.size(); i++) {
          if (i > 0) {
            sb.append(" ");
          }
          
          sb.append(_ackList.get(i));
        }
        
        properties.put("acks", sb.toString());
        
        _ackList.clear();
      }

      _writer.sendReceiver(_queue, properties);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void setSessionId(String id)
  {
    _sessionId = id;
  }

  @Override
  protected void clientReceiveAck(long messageId)
  {
    try {
      _ackList.add(messageId);
      
      _writer.sendAck(messageId);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void acceptedAck(long mid)
  {
    _ackList.remove(mid);
  }

  @Override
  protected void updateFlow(long creditSequence)
  {
    try {
      _writer.sendFlow(creditSequence);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void sendClose()
  {
    try {
      _writer.sendClose();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  //
  // received events
  //
  public void onDisconnect()
  {
    clearQueue();
    
    if (! isClosed()) {
      _reconnectCount = 0;
      _reconnectAlarm.runAfter(10);
    }
  }
  
  class Reconnect implements AlarmListener {
    @Override
    public void handleAlarm(Alarm alarm)
    {
      boolean isValid = false;
      
      try {
        connect();
        
        updateCredit();
        
        isValid = true;
      } catch (Exception e) {
        log.fine(e.toString());
        log.log(Level.FINEST, e.toString(), e);
      } finally {
        if (! isClosed() && ! isValid && _reconnectCount++ <= 120) {
          alarm.runAfter(1000);
        }
      }
    }
  }
}
