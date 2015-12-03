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

package com.caucho.message.broker;

import java.util.Map;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.message.DistributionMode;
import com.caucho.util.ConcurrentArrayList;
import com.caucho.util.LruCache;

/**
 * Custom serialization for the cache
 */
public class EnvironmentMessageBroker implements MessageBroker
{
  private static final EnvironmentLocal<EnvironmentMessageBroker> _localBroker
    = new EnvironmentLocal<EnvironmentMessageBroker>();
  
  private ClassLoader _loader;
  private String _id;
  
  private ConcurrentArrayList<MessageBroker> _brokerList
    = new ConcurrentArrayList<MessageBroker>(MessageBroker.class);
  
  private LruCache<String,MessageBroker> _brokerMap
    = new LruCache<String,MessageBroker>(1024);
  
  private EnvironmentMessageBroker()
  {
    _loader = Thread.currentThread().getContextClassLoader();
    _id = Environment.getEnvironmentName(_loader);
    
    _localBroker.set(this);
  }
  
  public static EnvironmentMessageBroker getCurrent()
  {
    return _localBroker.get();
  }
  
  public static EnvironmentMessageBroker create()
  {
    synchronized (_localBroker) {
      EnvironmentMessageBroker broker = _localBroker.getLevel();
      
      if (broker == null) {
        broker = new EnvironmentMessageBroker();
        _localBroker.set(broker);
      }
      
      return broker;
    }
  }
  
  public void addBroker(MessageBroker broker)
  {
    _brokerList.add(broker);
  }
  
  public void removeBroker(MessageBroker broker)
  {
    _brokerList.remove(broker);
  }
  
  public MessageBroker findBroker(Class<?> brokerClass)
  {
    for (MessageBroker broker : _brokerList) {
      if (brokerClass.isAssignableFrom(broker.getClass())) {
        return broker;
      }
    }
    
    return null;
  }
  
  @Override
  public BrokerSender createSender(String name,
                                   Map<String,Object> properties)
  {
    MessageBroker broker = _brokerMap.get(name);
    
    if (broker != null)
      return broker.createSender(name, properties);
    
    for (MessageBroker registeredBroker : _brokerList) {
      BrokerSender dest = registeredBroker.createSender(name, properties);
      
      if (dest != null) {
        _brokerMap.put(name, registeredBroker);
        return dest;
      }
    }
    
    return null;
  }
  
  @Override
  public BrokerReceiver createReceiver(String name,
                                       DistributionMode distributionMode,
                                       Map<String,Object> properties,
                                       ReceiverMessageHandler listener)
  {
    for (MessageBroker registeredBroker : _brokerList) {
      BrokerReceiver sub
        = registeredBroker.createReceiver(name, 
                                          distributionMode,
                                          properties,
                                          listener);
      
      if (sub != null) {
        return sub;
      }
    }
    
    return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getName() + "[" + _id + "]";
  }
}
