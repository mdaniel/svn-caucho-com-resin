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

import com.caucho.nautilus.ReceiverBuilder;
import com.caucho.nautilus.ReceiverListener;
import com.caucho.nautilus.ReceiverController;
import com.caucho.nautilus.ReceiverConfig;
import com.caucho.nautilus.ReceiverQueue;
import com.caucho.nautilus.SenderQueue;
import com.caucho.nautilus.SenderQueue.Settler;
import com.caucho.nautilus.SenderQueueConfig;
import com.caucho.nautilus.broker.BrokerProviderEnvironment;
import com.caucho.nautilus.broker.BrokerNautilus;
import com.caucho.nautilus.local.ReceiverLocal;
import com.caucho.nautilus.local.ReceiverQueueLocal;
import com.caucho.nautilus.local.SenderQueueLocal;
import com.caucho.nautilus.spi.BrokerProvider;

/**
 * Container for the nautilus implementation.
 */
public class BrokerProviderNautilus implements BrokerProvider
{
  private BrokerNautilus _broker;

  public BrokerProviderNautilus()
  {
    _broker = BrokerProviderEnvironment.create();
  }
  
  @Override
  public boolean isAddressSupported(String address)
  {
    return address.startsWith("queue:") || address.startsWith("nautilus:");
  }

  @Override
  public <M> ReceiverQueue<M> receiver(String address,
                                              ReceiverConfig<M> config)
  {
    if (isLocalAddress(address)) {
      return new ReceiverLocal<M>(getLocalQueueName(address), config, _broker);
    }
    else {
      throw new UnsupportedOperationException(address);
    }
  }

  @Override
  public <M> ReceiverController receiver(String address,
                                      ReceiverConfig<M> config,
                                      ReceiverListener<M> consumer)
  {
    if (isLocalAddress(address)) {
      return new ReceiverQueueLocal<M>(getLocalQueueName(address), 
                                         config,
                                         consumer,
                                         _broker);
    }
    else {
      throw new UnsupportedOperationException(address);
    }
  }
  
  /* (non-Javadoc)
   * @see com.caucho.nautilus.spi.MessageContainer#sender(java.lang.String, com.caucho.nautilus.MessageSenderConfig)
   */
  @Override
  public <M> SenderQueue<M> sender(String address,
                                     SenderQueueConfig<M> config)
  {
    if (isLocalAddress(address)) {
      return new SenderQueueLocal<M>(getLocalQueueName(address), config, null, _broker);
    }
    else {
      throw new UnsupportedOperationException(address);
    }
  }

  /* (non-Javadoc)
   * @see com.caucho.nautilus.spi.MessageContainer#sender(java.lang.String, com.caucho.nautilus.MessageSenderConfig, com.caucho.nautilus.MessageSender.Settler)
   */
  @Override
  public <M> SenderQueue<M> sender(String address,
                                     SenderQueueConfig<M> config,
                                     Settler settler)
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  private boolean isLocalAddress(String address)
  {
    return address.startsWith("queue:///") || address.startsWith("nautilus:///");
  }
  
  private String getLocalQueueName(String address)
  {
    int p = address.lastIndexOf('/');
    
    if (p >= 0) {
      return address.substring(p + 1);
    }
    else {
      return address;
    }
  }
}
