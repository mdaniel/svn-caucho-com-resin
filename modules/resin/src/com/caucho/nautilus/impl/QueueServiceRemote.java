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

package com.caucho.nautilus.impl;

import io.baratine.core.Direct;
import io.baratine.core.ServiceExceptionUnavailable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.caucho.amp.AmpSystem;
import com.caucho.amp.ServiceManagerAmp;
import com.caucho.bartender.BartenderSystem;
import com.caucho.bartender.ServerBartender;
import com.caucho.bartender.pod.PodBartender;
import com.caucho.nautilus.broker.SenderSettleHandler;
import com.caucho.util.ConcurrentArrayList;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.StreamSourceInputStream;
import com.caucho.vfs.TempBuffer;

/**
 * Service for an individual queue, managing its messages.
 * 
 * The QueueService is a central nautilus service.
 */
class QueueServiceRemote extends QueueServiceBase
{
  private MessageNautilus _head;
  private MessageNautilus _tail;
  
  private ConcurrentArrayList<ReceiverBrokerNautilus> _receiverList
    = new ConcurrentArrayList<>(ReceiverBrokerNautilus.class);
  
  private long _enqueueCount;
  private long _dequeueCount;
  
  private int _size;
  private PodBartender _pod;
  private NautilusService _nautilusService;
  
  private final long _flowInitial;
  
  private long _flow;
  private long _sequenceReceive;
  private ServerBartender _serverSelf;
  
  QueueServiceRemote(BrokerServiceImpl broker,
                     String name,
                     long qid,
                     PodBartender pod)
  {
    super(broker, name, qid);
    
    Objects.requireNonNull(pod);
    
    _pod = pod;
    _serverSelf = BartenderSystem.getCurrentSelfServer();
    
    _flowInitial = CurrentTime.getCurrentTime() << 16;
  }
  
  private ServerBartender getServerSelf()
  {
    return _serverSelf;
  }
  
  @Direct
  public int getSize()
  {
    return _size;
  }
  
  @Direct
  public long getEnqueueCount()
  {
    return _enqueueCount;
  }
  
  @Direct
  public long getDequeueCount()
  {
    return _dequeueCount;
  }
  
  public void start()
  {
    //getBroker().loadQueue(this);
  }
  
  public void message(long sid,
                      long mid, 
                      byte[] buffer, int offset, int length,
                      TempBuffer tBuf,
                      SenderSettleHandler settleHandler)
  {
    ByteArrayInputStream bis = new ByteArrayInputStream(buffer, offset, length);
    StreamSourceInputStream ssIs = new StreamSourceInputStream(bis);
    StreamSource ss = new StreamSource(ssIs);
    
    getService().send(getName(), ss);
  }

  public void subscribe(ReceiverBrokerNautilus receiver)
  {
    _receiverList.add(receiver);
    
    updateFlow();
  }
  
  public void unregister(long id, ReceiverBrokerNautilus receiver)
  {
    _receiverList.remove(receiver);
  }
  
  
  /*
  void register(ReceiverBrokerNautilus receiver)
  {
    _receiverList.add(receiver);
  }
  */
  
  public void accepted(ReceiverBrokerNautilus receiver,
                       MessageNautilus msg)
  {
    //removeEntry(msg);
    
    //msg.accepted(this);
    
    
    // receiver.onAccepted(msg)
  }
  
  public void onFlowUpdate()
  {
    updateFlow();
  }
  
  void updateFlow()
  {
    long newCredit = 0;
    
    for (ReceiverBrokerNautilus receiver : _receiverList) {
      newCredit += Math.max(0, receiver.getCredit());
    }
    
    
    long oldCredit = _flow - _sequenceReceive;
    
    if (oldCredit != newCredit) {
      _flow = _sequenceReceive + newCredit;
      
      getService().flow(getName(), getServerSelf().getId(),
                        _flow + _flowInitial,
                        _flowInitial);
    }
  }
  
  /**
   * Receive from remote.
   */
  @Override
  public void onReceive(long messageId, StreamSource ss)
  {
    _sequenceReceive++;
    
    for (ReceiverBrokerNautilus receiver : _receiverList) {
      if (receiver.isAvailable()) {
        int len = 0;
        
        MessageNautilus msg = new MessageNautilus(messageId, 0, 0, 0);
        
        try {
          receiver.receive(msg, ss.getInputStream(), len);
        } catch (IOException e) {
          e.printStackTrace();
        }
        
        return;
      }
    }
  }
  
  private NautilusService getService()
  {
    if (_nautilusService == null) {
      
      ServerBartender server = _pod.getNode(0).getServer(0);
      
      if (server == null) {
        System.out.println("MISSING_NAUTILUS_SERVER: " + server + " " + _pod.getNode(0).getServer(0));
        throw new ServiceExceptionUnavailable(getClass().getSimpleName());
      }
      
      ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
      
      String url = "champ://" + server.getId() + NautilusService.PATH;
      
      _nautilusService = rampManager.lookup(url).as(NautilusService.class);
    }
    
    return _nautilusService;
  }
}
