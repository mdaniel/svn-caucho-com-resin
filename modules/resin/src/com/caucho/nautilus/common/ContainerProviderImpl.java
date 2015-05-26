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

package com.caucho.nautilus.common;

import java.util.ArrayList;
import java.util.Objects;
import java.util.ServiceLoader;

import com.caucho.nautilus.MessageException;
import com.caucho.nautilus.ReceiverBuilder;
import com.caucho.nautilus.ReceiverConfig;
import com.caucho.nautilus.ReceiverController;
import com.caucho.nautilus.ReceiverListener;
import com.caucho.nautilus.ReceiverQueue;
import com.caucho.nautilus.SenderBuilder;
import com.caucho.nautilus.SenderQueue;
import com.caucho.nautilus.SenderQueue.Settler;
import com.caucho.nautilus.SenderQueueConfig;
import com.caucho.nautilus.spi.BrokerProvider;
import com.caucho.nautilus.spi.ContainerProvider;
import com.caucho.util.L10N;

/**
 * Container for the nautilus implementation.
 */
public class ContainerProviderImpl implements ContainerProvider
{
  private static final L10N L = new L10N(ContainerProviderImpl.class);
  
  private ArrayList<BrokerProvider> _providers
    = new ArrayList<>();
  
  public ContainerProviderImpl()
  {
    for (BrokerProvider provider
         : ServiceLoader.load(BrokerProvider.class)) {
      _providers.add(provider);
    }
  }
  
  @Override
  public <M> ReceiverBuilder<M> receiver()
  {
    return new ReceiverBuilderImpl<M>(this);
  }

  <M> ReceiverQueue<M> receiver(String address,
                                ReceiverConfig<M> config)
  {
    return findProvider(address).receiver(address, config);
  }

  <M> ReceiverController receiver(String address,
                                 ReceiverConfig<M> config,
                                 ReceiverListener<M> listener)
  {
    return findProvider(address).receiver(address, config, listener);
  }
  
  @Override
  public <M> SenderBuilder<M> sender()
  {
    return new SenderBuilderImpl<M>(this);
  }

  <M> SenderQueue<M> sender(String address,
                            SenderQueueConfig<M> config)
  {
    Objects.requireNonNull(address);
    
    return findProvider(address).sender(address, getConfig(config));
  }

  <M> SenderQueue<M> sender(String address,
                            SenderQueueConfig<M> config,
                            Settler settler)
  {
    return findProvider(address).sender(address, getConfig(config), settler);
  }
  
  private <M> SenderQueueConfig<M> getConfig(SenderQueueConfig<M> config)
  {
    if (config != null) {
      return config;
    }
    else {
      SenderQueueConfig.Builder<M> builder;
      builder = SenderQueueConfig.Builder.create();
      
      return builder.build();
    }
  }
  
  /*
  private <M> ReceiverConfig<M> getConfig(ReceiverConfig<M> config)
  {
    if (config != null) {
      return config;
    }
    else {
      ReceiverConfig.Builder<M> builder;
      builder = ReceiverConfig.Builder.create();
      
      return builder.build();
    }
  }
  */
  
  private BrokerProvider findProvider(String address)
  {
    for (BrokerProvider provider : _providers) {
      if (provider.isAddressSupported(address)) {
        return provider;
      }
    }
    
    throw new MessageException(L.l("'{0}' is an unsupported message URL.",
                                   address));
  }
}
