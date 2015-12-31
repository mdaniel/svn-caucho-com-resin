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

package com.caucho.v5.message.stomp;

import java.util.Map;

import javax.annotation.PostConstruct;

import com.caucho.v5.nautilus.broker.BrokerNautilus;
import com.caucho.v5.nautilus.broker.BrokerProviderEnvironment;
import com.caucho.v5.nautilus.broker.SenderBroker;
import com.caucho.v5.network.port.ConnectionTcp;
import com.caucho.v5.network.port.ProtocolBase;
import com.caucho.v5.network.port.ConnectionProtocol;

/**
 * Custom serialization for the cache
 */
public class StompProtocol extends ProtocolBase
{
  private BrokerNautilus _broker;
  
  public void setBroker(BrokerNautilus broker)
  {
    _broker = broker;
  }
  
  @PostConstruct
  public void init()
  {
    if (_broker == null) {
      _broker = BrokerProviderEnvironment.create();
    }
  }
  
  public BrokerNautilus getBroker()
  {
    return _broker;
  }
  
  @Override
  public ConnectionProtocol newConnection(ConnectionTcp link)
  {
    return new StompConnection(this, link);
  }

  @Override
  public String name()
  {
    return "stomp";
  }
  
  public SenderBroker createDestination(String name)
  {
    Map<String,Object> nodeProperties = null;
    
    return _broker.createSender(name, nodeProperties);
  }
}
