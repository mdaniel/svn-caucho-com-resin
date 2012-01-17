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

package com.caucho.mqueue.stomp;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.ConcurrentArrayList;
import com.caucho.util.LruCache;

/**
 * Custom serialization for the cache
 */
public class StompEnvironmentBroker implements StompBroker
{
  private static final EnvironmentLocal<StompEnvironmentBroker> _localBroker
    = new EnvironmentLocal<StompEnvironmentBroker>();
  
  private ClassLoader _loader;
  private String _id;
  
  private ConcurrentArrayList<StompBroker> _brokerList
    = new ConcurrentArrayList<StompBroker>(StompBroker.class);
  
  private LruCache<String,StompBroker> _brokerMap
    = new LruCache<String,StompBroker>(1024);
  
  private StompEnvironmentBroker()
  {
    _loader = Thread.currentThread().getContextClassLoader();
    _id = Environment.getEnvironmentName(_loader);
    
    _localBroker.set(this);
  }
  
  public static StompEnvironmentBroker getCurrent()
  {
    return _localBroker.get();
  }
  
  public static StompEnvironmentBroker create()
  {
    synchronized (_localBroker) {
      StompEnvironmentBroker broker = _localBroker.getLevel();
      
      if (broker == null) {
        broker = new StompEnvironmentBroker();
        _localBroker.set(broker);
      }
      
      return broker;
    }
  }
  
  public void addBroker(StompBroker broker)
  {
    _brokerList.add(broker);
  }
  
  public void removeBroker(StompBroker broker)
  {
    _brokerList.remove(broker);
  }
  
  @Override
  public StompDestination createDestination(String name)
  {
    StompBroker broker = _brokerMap.get(name);
    
    if (broker != null)
      return broker.createDestination(name);
    
    for (StompBroker registeredBroker : _brokerList) {
      StompDestination dest = registeredBroker.createDestination(name);
      
      if (dest != null) {
        _brokerMap.put(name, registeredBroker);
        return dest;
      }
    }
    
    return null;
  }
  
  @Override
  public StompSubscription createSubscription(String name,
                                              StompMessageListener listener)
  {
    for (StompBroker registeredBroker : _brokerList) {
      StompSubscription sub
        = registeredBroker.createSubscription(name, listener);
      
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
