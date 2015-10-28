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

package com.caucho.v5.nautilus.impl;

import io.baratine.core.Startup;

import java.io.Closeable;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.loader.Environment;
import com.caucho.v5.nautilus.ReceiverMode;
import com.caucho.v5.nautilus.broker.BrokerNautilusBase;
import com.caucho.v5.nautilus.broker.BrokerProviderEnvironment;
import com.caucho.v5.nautilus.broker.ReceiverBroker;
import com.caucho.v5.nautilus.broker.ReceiverMessageHandler;
import com.caucho.v5.nautilus.broker.SenderBroker;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

/**
 * Simple stomp broker.
 */
@Startup
@Singleton
public class BrokerNautilusImpl extends BrokerNautilusBase implements Closeable
{
  private static final L10N L = new L10N(BrokerNautilusImpl.class);

  private final NautilusSystem _nautilus;
  
  public BrokerNautilusImpl()
  {
    _nautilus = NautilusSystem.getCurrent();
    
    Objects.requireNonNull(_nautilus);
  }
  
  
  public void setPath(Path path)
  {
  }
  
  @PostConstruct
  public void init()
  {
    /*
    if (_path == null) {
      throw new ConfigException(L.l("'path' is required for a journal broker."));
    }
    */
    
    initImpl();
    
    Environment.addCloseListener(this);
    
    registerSelf();
  }
  
  public static BrokerNautilusImpl getCurrent()
  {
    BrokerProviderEnvironment envBroker = BrokerProviderEnvironment.create();
    
    synchronized (envBroker) {
      BrokerNautilusImpl broker = (BrokerNautilusImpl) envBroker.findBroker(BrokerNautilusImpl.class);
    
      if (broker == null) {
        broker = new BrokerNautilusImpl();
        broker.init();
      
        envBroker.addBroker(broker);
      }
      
      return broker;
    }
  }

  private void initImpl()
  {
    /*
    try {
      _path.mkdirs();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    if (! _path.isDirectory()) {
      throw new ConfigException(L.l("NautilusBroker path '{0}' must be a directory",
                                    _path.getNativePath()));
    }
    */
    
    NautilusSystem nautilus = NautilusSystem.getCurrent();
    
    if (nautilus == null) {
      throw new ConfigException(L.l("{0} requires an active {1}",
                                    getClass().getSimpleName(),
                                    NautilusSystem.class.getSimpleName()));
      
    }
    
    //_brokerServiceImpl = new BrokerServiceNautilus(nautilus);
    
    // XXX: _journalQueue = _brokerActor.getActorQueue();
  }
  
  private BrokerService getBrokerService()
  {
    return _nautilus.getBrokerService();
  }
  
  @Override
  public SenderBroker createSender(String queueName,
                                   Map<String,Object> properties)
  {
    return getBrokerService().createSender(queueName, properties);
    /*
    JournalSenderRegister entry;
    
    entry = new JournalSenderRegister(queueName, properties);
    
    //_journalQueue.offer(entry);
    
    return entry.get(_journalQueue);
    */
  }
  
  @Override
  public ReceiverBroker createReceiver(String queueName,
                                       ReceiverMode distMode,
                                       Map<String,Object> queueProperties,
                                       ReceiverMessageHandler handler)
  {
    return getBrokerService().createReceiver(queueName, 
                                             distMode,
                                             queueProperties,
                                             handler);
    
    /*
    JournalReceiverRegister entry;
    
    entry = new JournalReceiverRegister(queueName, queueProperties, handler);
    
    //_journalQueue.offer(entry);
    
    return entry.get(_journalQueue);
    */
  }

  public int getSize()
  {
    return 0;//_brokerService.getSize();
  }

  public long getEnqueueCount()
  {
    return 0;//_brokerService.getEnqueueCount();
  }

  public long getDequeueCount()
  {
    return 0;//_brokerService.getDequeueCount();
  }

  @Override
  public void close()
  {
  }
}
