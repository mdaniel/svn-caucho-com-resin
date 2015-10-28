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

package com.caucho.v5.nautilus.local;

import com.caucho.v5.nautilus.ReceiverConfig;
import com.caucho.v5.nautilus.ReceiverController;
import com.caucho.v5.nautilus.ReceiverListener;
import com.caucho.v5.nautilus.ReceiverQueue;
import com.caucho.v5.nautilus.SenderQueue;
import com.caucho.v5.nautilus.SenderQueueConfig;
import com.caucho.v5.nautilus.SenderQueue.Settler;
import com.caucho.v5.nautilus.broker.BrokerNautilus;
import com.caucho.v5.nautilus.broker.BrokerProviderEnvironment;
import com.caucho.v5.nautilus.spi.BrokerProvider;

/**
 * Container for the nautilus implementation.
 */
public class BrokerProviderLocal implements BrokerProvider
{
  private BrokerNautilus _broker;
  
  public BrokerProviderLocal()
  {
    _broker = BrokerProviderEnvironment.create();
  }
  
  @Override
  public boolean isAddressSupported(String address)
  {
    return address.startsWith("local:");
  }

  @Override
  public <M> ReceiverQueue<M> receiver(String address,
                                       ReceiverConfig<M> config)
  {
    return new ReceiverLocal<M>(getQueueName(address), config, _broker);
  }

  @Override
  public <M> ReceiverController receiver(String address,
                                      ReceiverConfig<M> config,
                                      ReceiverListener<M> consumer)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public <M> SenderQueue<M> sender(String address,
                                     SenderQueueConfig<M> config)
  {
    return new SenderQueueLocal<M>(getQueueName(address), config, null, _broker);
  }

  @Override
  public <M> SenderQueue<M> sender(String address,
                                     SenderQueueConfig<M> config,
                                     Settler settler)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  private String getQueueName(String address)
  {
    if (address.startsWith("local:")) {
      return address.substring("local:".length());
    }
    else {
      return address;
    }
  }
}
