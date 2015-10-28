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

package com.caucho.v5.nautilus.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.nautilus.ReceiverMode;
import com.caucho.v5.nautilus.broker.ReceiverBroker;
import com.caucho.v5.nautilus.broker.ReceiverMessageHandler;
import com.caucho.v5.nautilus.broker.SenderSettleHandler;
import com.caucho.v5.vfs.StreamSource;
import com.caucho.v5.vfs.StreamSourceInputStream;
import com.caucho.v5.vfs.TempBuffer;

/**
 * Service for remote queues.
 */
public class NautilusServiceImpl
{
  private final NautilusSystem _nautilus;
  
  private final HashMap<String,Receiver> _receiverMap = new HashMap<>();

  private ServiceManagerAmp _rampManager;
  
  public NautilusServiceImpl(NautilusSystem nautilus)
  {
    _nautilus = nautilus;
    
    _rampManager = AmpSystem.getCurrentManager();
  }
  
  public void send(String queueName, StreamSource ss)
  {
    QueueService queue
      = _nautilus.getBrokerService().getQueueForRemote(queueName);
    
    try (InputStream is = ss.getInputStream()) {
      TempBuffer tBuf = TempBuffer.allocate();
      
      byte []buffer = tBuf.getBuffer();
      
      int len = is.read(buffer, 0, buffer.length);
      
      if (len <= 0) {
        return;
      }
      
      long sid = 0;
      long mid = 0;
      SenderSettleHandler settleHandler = null;
      
      queue.message(sid, mid, buffer, 0, len, tBuf, settleHandler);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void flow(String queueName, 
                   String serverId,
                   long flowSequence,
                   long flowInitial)
  {
    Receiver receiver = getReceiver(queueName, serverId, flowInitial);
    
    receiver.flow(flowSequence, flowInitial);
  }

  public void receive(String queueName,
                      long messageId, 
                      StreamSource ss)
  {
    QueueService queue = _nautilus.getBrokerService().getQueueForRemote(queueName);
    //_sequenceReceive++;
    
    queue.onReceive(messageId, ss);
  }

  private Receiver getReceiver(String queueName, 
                               String serverId, 
                               long flowInitial)
  {
    String key = queueName + ";" + serverId;
    
    Receiver receiver = _receiverMap.get(key);
    
    if (receiver == null) {
      receiver = new Receiver(queueName, serverId);
      
      _receiverMap.put(key, receiver);
    }
    
    return receiver;
  }
  
  private class Receiver implements ReceiverMessageHandler
  {
    private final String _queueName;
    private final String _serverId;
    
    private NautilusService _nautilusRemote;
    
    private long _sequence;
    
    private ReceiverBroker _receiverBroker;
    
    
    Receiver(String queueName, 
             String serverId)
    {
      _queueName = queueName;
      _serverId = serverId;
      
      String url = "champ://" + _serverId + NautilusService.PATH;
      
      _nautilusRemote = _rampManager.lookup(url)
                                    .as(NautilusService.class);
      
      ReceiverMode mode = ReceiverMode.CONSUME;
      Map<String,Object> properties = null;
      
      BrokerService broker = _nautilus.getBrokerService();
      
      _receiverBroker = broker.createReceiver(_queueName, 
                                              mode, 
                                              properties, 
                                              this);
    }

    public void flow(long flowSequence, long flowInitial)
    {
      long credit = flowSequence - flowInitial;
      
      _receiverBroker.flow(_sequence + credit);
    }

    /* (non-Javadoc)
     * @see com.caucho.nautilus.broker.ReceiverMessageHandler#onMessage(long, java.io.InputStream, long)
     */
    @Override
    public void onMessage(long messageId, InputStream bodyIs, long contentLength)
        throws IOException
    {
      _sequence++;
      
      StreamSourceInputStream ssIs = new StreamSourceInputStream(bodyIs);
      StreamSource ss = new StreamSource(ssIs);
      
      _nautilusRemote.receive(_queueName, messageId, ss);
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _queueName + "," + _serverId + "]";
    }
  }
}
