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

package com.caucho.ramp.nautilus;

import io.baratine.core.ServiceRef;

import java.io.StringReader;

import com.caucho.amp.ServiceManagerAmp;
import com.caucho.amp.inbox.OutboxAmpDirect;
import com.caucho.amp.jamp.InJamp;
import com.caucho.amp.remote.ChannelServer;
import com.caucho.amp.remote.GatewayReply;
import com.caucho.amp.spi.OutboxAmp;
import com.caucho.amp.spi.ServiceRefAmp;
import com.caucho.nautilus.ReceiverController;
import com.caucho.nautilus.ReceiverListener;

/**
 * Method ref for a nautilus queue.
 */
class NautilusListener 
  implements ReceiverListener<String>, ChannelServer
{
  private ServiceRefAmp _consumer;
  private ServiceManagerAmp _rampManager;
  private InJamp _jIn;
  
  NautilusListener(ServiceRef consumer)
  {
    _consumer = (ServiceRefAmp) consumer;
    _rampManager = (ServiceManagerAmp) consumer.getManager();
    _jIn = new InJamp(this);
  }
  
  public ServiceManagerAmp getManager()
  {
    return _rampManager;
  }

  @Override
  public void onMessage(long mid, String value, ReceiverController receiver)
  {
    receiver.accepted(mid);
    
    try (StringReader sIn = new StringReader(value)) {
      _jIn.readMessage(sIn);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public ServiceRefAmp lookup(String address)
  {
    return _consumer;
  }

  /*
  @Override
  public RampMailbox getReadMailbox()
  {
    return null;
  }
  */

  @Override
  public OutboxAmp createOutbox()
  {
    /*
    OutboxAmpDirect outbox = new OutboxAmpDirect();
    
    outbox.setInbox(_consumer.getInbox());
    
    return outbox;
    */
    throw new UnsupportedOperationException();
  }

  /*
  @Override
  public String getReadAddress()
  {
    return null;
  }

  @Override
  public RampMailbox getWriteMailbox()
  {
    return null;
  }
  */

  /*
  @Override
  public void login(RampConnection conn)
  {
  }
  */

  @Override
  public void onLogin(String uid)
  {

  }

  /*
  @Override
  public void clearLogin()
  {
  }
  
  @Override
  public void waitForLogin()
  {
  }
  */

  /* (non-Javadoc)
   * @see com.caucho.ramp.remote.RampReadBroker#getGatewayServiceRef(java.lang.String)
   */
  @Override
  public GatewayReply createGatewayReply(String address)
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public ServiceRefAmp createGatewayRef(String address)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
