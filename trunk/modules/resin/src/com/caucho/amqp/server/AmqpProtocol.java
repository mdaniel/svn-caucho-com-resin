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

package com.caucho.amqp.server;

import java.util.Map;

import javax.annotation.PostConstruct;

import com.caucho.amqp.common.AmqpReceiverLink;
import com.caucho.message.DistributionMode;
import com.caucho.message.SettleMode;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;

/**
 * Custom serialization for the cache
 */
public class AmqpProtocol implements Protocol
{
  private EnvironmentMessageBroker _broker;
  
  @PostConstruct
  public void init()
  {
    _broker = EnvironmentMessageBroker.create();
    
    if (_broker == null) {
      throw new NullPointerException();
    }
  }
  
  @Override
  public ProtocolConnection createConnection(SocketLink link)
  {
    return new AmqpServerConnection(this, link);
  }

  AmqpServerReceiverLink createReceiverLink(String name, 
                                            String address,
                                            Map<String,Object> nodeProperties)
  {
    BrokerSender sender = _broker.createSender(address, nodeProperties);
    
    if (sender != null) {
      return new AmqpServerReceiverLink(name, address, sender);
    }
    else {
      return null;
    }
  }

  AmqpServerSenderLink createSenderLink(String name, 
                                        String address,
                                        DistributionMode distMode,
                                        SettleMode settleMode,
                                        Map<String,Object> nodeProperties)
  {
    AmqpServerSenderLink link
      = new AmqpServerSenderLink(name, address, settleMode);

    BrokerReceiver receiver = _broker.createReceiver(address, 
                                                     distMode,
                                                     nodeProperties,
                                                     link.getBrokerHandler());
    
    if (receiver != null) {
      link.setReceiver(receiver);
      
      return link;
    }
    else {
      return null;
    }
  }

  @Override
  public String getProtocolName()
  {
    return "amqp";
  }
}
