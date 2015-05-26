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

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import com.caucho.nautilus.ReceiverBuilder;
import com.caucho.nautilus.ReceiverListener;
import com.caucho.nautilus.ReceiverController;
import com.caucho.nautilus.ReceiverConfig;
import com.caucho.nautilus.ReceiverQueue;
import com.caucho.nautilus.SenderQueue;
import com.caucho.nautilus.SenderQueueConfig;
import com.caucho.nautilus.SenderQueue.Settler;
import com.caucho.nautilus.spi.BrokerProvider;

/**
 * Message facade for creating a connection
 */
public class BrokerProviderNautilusProtocol implements BrokerProvider
{
  private WebSocketContainer _wsContainer
    = ContainerProvider.getWebSocketContainer();
  
  WebSocketContainer getWebSocketContainer()
  {
    return _wsContainer;
  }

  /* (non-Javadoc)
   * @see com.caucho.message.MessageClientContainer#receiver(java.lang.String, com.caucho.message.MessageReceiverConfig)
   */
  @Override
  public <M> ReceiverQueue<M> receiver(String address,
                                              ReceiverConfig<M> config)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.message.MessageClientContainer#receiver(java.lang.String, com.caucho.message.MessageReceiverConfig, com.caucho.message.MessageConsumer)
   */
  @Override
  public <M> ReceiverController receiver(String address,
                                      ReceiverConfig<M> config,
                                      ReceiverListener<M> consumer)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.message.MessageClientContainer#sender(java.lang.String, com.caucho.message.MessageSenderConfig)
   */
  @Override
  public <M> SenderQueue<M> sender(String address,
                                     SenderQueueConfig<M> config)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.message.MessageClientContainer#sender(java.lang.String, com.caucho.message.MessageSenderConfig, com.caucho.message.MessageSender.Settler)
   */
  @Override
  public <M> SenderQueue<M> sender(String address,
                                     SenderQueueConfig<M> config,
                                     Settler settler)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.message.MessageContainer#isAddressSupported(java.lang.String)
   */
  @Override
  public boolean isAddressSupported(String address)
  {
    // TODO Auto-generated method stub
    return false;
  }
}
