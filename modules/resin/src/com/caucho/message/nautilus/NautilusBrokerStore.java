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

package com.caucho.message.nautilus;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Singleton;

import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.ConfigException;
import com.caucho.db.block.BlockStore;
import com.caucho.distcache.ClusterCache;
import com.caucho.env.actor.ActorQueue;
import com.caucho.env.service.RootDirectorySystem;
import com.caucho.message.broker.AbstractMessageBroker;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.message.broker.ReceiverMessageHandler;
import com.caucho.message.journal.JournalRecoverListener;
import com.caucho.message.journal.JournalFile;
import com.caucho.message.journal.JournalWriteActor;
import com.caucho.message.journal.JournalResult;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.util.RingItemFactory;
import com.caucho.vfs.Path;

/**
 * Backing store for the broker.
 */
class NautilusBrokerStore extends AbstractMessageBroker
{
  private static final Logger log
    = Logger.getLogger(NautilusBrokerStore.class.getName());
  
  private AtomicLong _qidGen = new AtomicLong();
  private ClusterCache _queueCache;

  NautilusBrokerStore()
  {
    long serverId = 0;
    
    try {
      CloudServer server = NetworkClusterSystem.getCurrentSelfServer();
    
      serverId = server.getIndex();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    // seq assigned to avoid conflict between servers and restarts
    long seq = (serverId << 56) + (CurrentTime.getCurrentTime() << 24);
    
    _qidGen.set(seq);
    
    _queueCache = new ClusterCache();
    _queueCache.setName("resin.message.nautilus");
    _queueCache.setAccessedExpireTimeoutMillis(Long.MAX_VALUE / 2);
    _queueCache.setModifiedExpireTimeoutMillis(Long.MAX_VALUE / 2);
    _queueCache.init();
  }
  
  BrokerQueue addQueue(String name)
  {
    BrokerQueue queue = (BrokerQueue) _queueCache.get(name);
    System.out.println("OLDQ: " + queue + " " + name);
    if (queue == null) {
      queue = new BrokerQueue(name, _qidGen.incrementAndGet());
      
      //_queueCache.putIfAbsent(name, queue);
      _queueCache.put(name, queue);
      
      queue = (BrokerQueue) _queueCache.get(name);
      
      if (queue == null) {
        throw new NullPointerException();
      }
    }
    
    return queue;
  }
  
  @SuppressWarnings("serial")
  public static class BrokerQueue implements Serializable {
    private String _name;
    private long _qid;
    
    public BrokerQueue()
    {
    }
    
    public BrokerQueue(String name, long qid)
    {
      _name = name;
      _qid = qid;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public long getId()
    {
      return _qid;
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _name + "," + Long.toHexString(_qid) + "]";
    }
  }
}
