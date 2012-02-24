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

package com.caucho.amqp.broker;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.ConcurrentArrayList;
import com.caucho.util.LruCache;

/**
 * Custom serialization for the cache
 */
public class AmqpEnvironmentBroker implements AmqpBroker
{
  private static final EnvironmentLocal<AmqpEnvironmentBroker> _localBroker
    = new EnvironmentLocal<AmqpEnvironmentBroker>();
  
  private ClassLoader _loader;
  private String _id;
  
  private ConcurrentArrayList<AmqpBroker> _brokerList
    = new ConcurrentArrayList<AmqpBroker>(AmqpBroker.class);
  
  private LruCache<String,AmqpBroker> _brokerMap
    = new LruCache<String,AmqpBroker>(1024);
  
  private AmqpEnvironmentBroker()
  {
    _loader = Thread.currentThread().getContextClassLoader();
    _id = Environment.getEnvironmentName(_loader);
    
    _localBroker.set(this);
  }
  
  public static AmqpEnvironmentBroker getCurrent()
  {
    return _localBroker.get();
  }
  
  public static AmqpEnvironmentBroker create()
  {
    synchronized (_localBroker) {
      AmqpEnvironmentBroker broker = _localBroker.getLevel();
      
      if (broker == null) {
        broker = new AmqpEnvironmentBroker();
        _localBroker.set(broker);
      }
      
      return broker;
    }
  }
  
  public void addBroker(AmqpBroker broker)
  {
    _brokerList.add(broker);
  }
  
  public void removeBroker(AmqpBroker broker)
  {
    _brokerList.remove(broker);
  }
  
  @Override
  public AmqpBrokerSender createSender(String name)
  {
    AmqpBroker broker = _brokerMap.get(name);
    
    if (broker != null)
      return broker.createSender(name);
    
    for (AmqpBroker registeredBroker : _brokerList) {
      AmqpBrokerSender dest = registeredBroker.createSender(name);
      
      if (dest != null) {
        _brokerMap.put(name, registeredBroker);
        return dest;
      }
    }
    
    return null;
  }
  
  @Override
  public AmqpBrokerReceiver createReceiver(String name,
                                              AmqpMessageListener listener)
  {
    for (AmqpBroker registeredBroker : _brokerList) {
      AmqpBrokerReceiver sub
        = registeredBroker.createReceiver(name, listener);
      
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
