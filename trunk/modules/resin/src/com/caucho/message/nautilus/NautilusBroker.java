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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Singleton;

import com.caucho.config.ConfigException;
import com.caucho.db.block.BlockStore;
import com.caucho.env.actor.ActorQueue;
import com.caucho.env.service.RootDirectorySystem;
import com.caucho.loader.Environment;
import com.caucho.message.DistributionMode;
import com.caucho.message.broker.AbstractMessageBroker;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.message.broker.ReceiverMessageHandler;
import com.caucho.message.journal.JournalRecoverListener;
import com.caucho.message.journal.JournalFile;
import com.caucho.message.journal.JournalWriteActor;
import com.caucho.message.journal.JournalResult;
import com.caucho.message.nautilus.NautilusBrokerStore.BrokerQueue;
import com.caucho.util.L10N;
import com.caucho.util.RingItemFactory;
import com.caucho.vfs.Path;

/**
 * Simple stomp broker.
 */
@Startup
@Singleton
public class NautilusBroker extends AbstractMessageBroker implements Closeable
{
  private static final Logger log = Logger.getLogger(NautilusBroker.class.getName());
  private static final L10N L = new L10N(NautilusBroker.class);
  
  private Path _path;
  private JournalFile _journalFile;
  private NautilusMultiQueueActor _nautilusActor;

  private ActorQueue<NautilusRingItem> _nautilusActorQueue;

  private NautilusBrokerStore _brokerStore = new NautilusBrokerStore();
  
  public void setPath(Path path)
  {
    _path = path;
  }
  
  @PostConstruct
  public void init()
  {
    if (_path == null)
      throw new ConfigException(L.l("'path' is required for a journal broker."));
    
    initImpl();
    
    Environment.addCloseListener(this);
    
    registerSelf();
  }
  
  public static NautilusBroker getCurrent()
  {
    EnvironmentMessageBroker envBroker = EnvironmentMessageBroker.create();
    
    NautilusBroker broker = (NautilusBroker) envBroker.findBroker(NautilusBroker.class);
    
    if (broker == null) {
      broker = new NautilusBroker();
      
      RootDirectorySystem dirSystem = RootDirectorySystem.getCurrent();
      
      Path path = dirSystem.getDataDirectory().lookup("msg.journal");
      
      broker.setPath(path);
      broker.init();
      
      envBroker.addBroker(broker);
    }
    
    return broker;
  }
  
  @Override
  public BrokerSender createSender(String name,
                                   Map<String,Object> nodeProperties)
  {
    BrokerQueue queue = getQueue(name);
    
    return new NautilusBrokerPublisher(queue.getId(), getActorQueue());
  }
  
  @Override
  public BrokerReceiver createReceiver(String name,
                                       DistributionMode distMode,
                                       Map<String,Object> nodeProperties,
                                       ReceiverMessageHandler handler)
  {
    BrokerQueue queue = getQueue(name);
    
    return new NautilusBrokerSubscriber(queue.getName(),
                                        queue.getId(),
                                        _nautilusActorQueue, 
                                        handler);
  }
  
  private BrokerQueue getQueue(String name)
  {
    BrokerQueue queue = _brokerStore.addQueue(name);
    
    return queue;
  }

  private void initImpl()
  {
    _nautilusActor = new NautilusMultiQueueActor();
  
    JournalRecoverListener recover = new RecoverListener();
    _journalFile = new JournalFile(_path, recover);
  
    _nautilusActorQueue = new ActorQueue<NautilusRingItem>(8192,
        new NautilusItemFactory(),
        new JournalWriteActor(_journalFile),
        _nautilusActor);
    
    NautilusCheckpointPublisher pub
      = new NautilusCheckpointPublisher(_nautilusActorQueue);
    
    _nautilusActor.setNautilusCheckpointPublisher(pub);
  }

  public int getSize()
  {
    return _nautilusActor.getSize();
  }

  public long getEnqueueCount()
  {
    return _nautilusActor.getEnqueueCount();
  }

  public long getDequeueCount()
  {
    return _nautilusActor.getDequeueCount();
  }

  ActorQueue<NautilusRingItem> getActorQueue()
  {
    return _nautilusActorQueue;
  }

  @Override
  public void close()
  {
    System.out.println("CLOSE-ME:");
    _nautilusActorQueue.wake(); // XXX: close
    _journalFile.close();
  }

  static class NautilusItemFactory implements RingItemFactory<NautilusRingItem> {
    @Override
    public NautilusRingItem createItem(int index)
    {
      return new NautilusRingItem(index);
    }

  }

  class RecoverListener implements JournalRecoverListener {
    private NautilusRingItem _entry = new NautilusRingItem(0);

    @Override
    public void onEntry(long code,
                        boolean isInit,
                        boolean isFin, 
                        long xid,
                        long qid,
                        long mid,
                        BlockStore store,
                        long blockAddress,
                        int blockOffset,
                        int length)
      throws IOException
    {
      _entry.init(code, xid, qid, mid, null, 0, 0, null);
      
      JournalResult result = _entry.getResult();

      result.init1(store, blockAddress, blockOffset, length);

      try {
        _nautilusActor.process(_entry);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      _entry.clear();
    }
  }
}
