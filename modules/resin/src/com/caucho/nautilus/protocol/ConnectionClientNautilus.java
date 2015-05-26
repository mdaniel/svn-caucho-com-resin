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

import java.net.URI;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import com.caucho.nautilus.ReceiverBuilder;
import com.caucho.nautilus.ReceiverQueue;
import com.caucho.nautilus.SenderQueue;
import com.caucho.nautilus.SenderQueueFactory;

/**
 * local connection to the message store
 */
class ConnectionClientNautilus
{
  private WebSocketContainer _wsClient
    = ContainerProvider.getWebSocketContainer();
  
  /**
   * @param nautilusConnectionFactoryImpl
   */
  public ConnectionClientNautilus(BrokerProviderNautilusProtocol factory)
  {
    // TODO Auto-generated constructor stub
  }

  public ReceiverQueue<?> createReceiver(String queueName)
  {
    ReceiverBuilder factory = createReceiverFactory();
    factory.address(queueName);
    
    return factory.build();
  }

  public ReceiverFactoryNautilus createReceiverFactory()
  {
    return new ReceiverFactoryNautilus(this);
  }

  public SenderQueue<?> createSender(String queueName)
  {
    SenderQueueFactory factory = createSenderFactory();
    factory.setAddress(queueName);
    
    return factory.build();
  }

  public SenderFactoryNautilus createSenderFactory()
  {
    return new SenderFactoryNautilus(this);
  }
  
  WriterNautilus connect(String address,
                         EndpointNautilusBase endpoint)
  {
    try {
      _wsClient.connectToServer(endpoint, new URI(address));
    
      return endpoint.getWriter();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
