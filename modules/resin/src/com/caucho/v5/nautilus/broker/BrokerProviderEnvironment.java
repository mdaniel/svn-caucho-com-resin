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

package com.caucho.v5.nautilus.broker;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.nautilus.ReceiverMode;
import com.caucho.v5.nautilus.impl.BrokerNautilusImpl;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.util.LruCache;

/**
 * Custom serialization for the cache
 */
public class BrokerProviderEnvironment implements BrokerNautilus
{
  private static final EnvironmentLocal<BrokerProviderEnvironment> _localBroker
    = new EnvironmentLocal<BrokerProviderEnvironment>();
  
  private ClassLoader _loader;
  private String _id;
  
  private ConcurrentArrayList<BrokerNautilus> _brokerList
    = new ConcurrentArrayList<>(BrokerNautilus.class);
  
  private LruCache<String,BrokerNautilus> _brokerMap
    = new LruCache<>(1024);
    
  private AtomicReference<BrokerNautilus> _brokerDefault
    = new AtomicReference<>();
  
  private BrokerProviderEnvironment()
  {
    _loader = Thread.currentThread().getContextClassLoader();
    _id = EnvLoader.getEnvironmentName(_loader);
    
    _localBroker.set(this);
  }
  
  public static BrokerProviderEnvironment getCurrent()
  {
    return _localBroker.get();
  }
  
  public static BrokerProviderEnvironment create()
  {
    synchronized (_localBroker) {
      BrokerProviderEnvironment broker = _localBroker.getLevel();
      
      if (broker == null) {
        broker = new BrokerProviderEnvironment();
        _localBroker.set(broker);
      }
      
      return broker;
    }
  }
  
  public void addBroker(BrokerNautilus broker)
  {
    _brokerList.add(broker);
  }
  
  public void removeBroker(BrokerNautilus broker)
  {
    _brokerList.remove(broker);
  }
  
  public BrokerNautilus findBroker(Class<?> brokerClass)
  {
    for (BrokerNautilus broker : _brokerList) {
      if (brokerClass.isAssignableFrom(broker.getClass())) {
        return broker;
      }
    }
    
    return null;
  }
  
  @Override
  public SenderBroker createSender(String name,
                                   Map<String,Object> properties)
  {
    BrokerNautilus broker = _brokerMap.get(name);
    
    if (broker != null) {
      return broker.createSender(name, properties);
    }

    for (BrokerNautilus registeredBroker : _brokerList) {
      SenderBroker dest = registeredBroker.createSender(name, properties);

      if (dest != null) {
        _brokerMap.put(name, registeredBroker);
        return dest;
      }
    }
    
    BrokerNautilus brokerDefault = getBrokerDefault();
    
    SenderBroker dest = brokerDefault.createSender(name, properties);

    if (dest != null) {
      _brokerMap.put(name, brokerDefault);
      return dest;
    }
    
    return null;
  }
  
  @Override
  public ReceiverBroker createReceiver(String name,
                                       ReceiverMode distributionMode,
                                       Map<String,Object> properties,
                                       ReceiverMessageHandler listener)
  {
    for (BrokerNautilus registeredBroker : _brokerList) {
      ReceiverBroker sub
        = registeredBroker.createReceiver(name, 
                                          distributionMode,
                                          properties,
                                          listener);
      
      if (sub != null) {
        return sub;
      }
    }
    
    BrokerNautilus brokerDefault = getBrokerDefault();
    
    ReceiverBroker dest = brokerDefault.createReceiver(name, 
                                                       distributionMode,
                                                       properties,
                                                       listener);

    if (dest != null) {
      _brokerMap.put(name, brokerDefault);
      return dest;
    }
    
    return null;
  }
  
  private BrokerNautilus getBrokerDefault()
  {
    if (_brokerDefault.get() == null) {
      _brokerDefault.compareAndSet(null, new BrokerNautilusImpl());
    }
    
    return _brokerDefault.get();
  }
  
  @Override
  public String toString()
  {
    return getClass().getName() + "[" + _id + "]";
  }
}
