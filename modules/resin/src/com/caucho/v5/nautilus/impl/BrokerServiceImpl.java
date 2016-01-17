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

import io.baratine.service.Result;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.kraken.KrakenSystem;
import com.caucho.v5.nautilus.ReceiverMode;
import com.caucho.v5.nautilus.broker.ReceiverBroker;
import com.caucho.v5.nautilus.broker.ReceiverMessageHandler;
import com.caucho.v5.nautilus.broker.SenderBroker;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.Murmur64;
import com.caucho.v5.vfs.PathImpl;

/**
 * Interface for the transaction log.
 *
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public final class BrokerServiceImpl
{
  private static final Logger log
    = Logger.getLogger(BrokerServiceImpl.class.getName());

  private PathImpl _path;
  
  // private DataStoreNautilus _db;

  private long _initialSequence;
  
  private HashMap<Long,QueueService> _queueIdMap
    = new HashMap<>();
  
  private HashMap<String,QueueService> _queueNameMap
    = new HashMap<>();

  // private NautilusCheckpointPublisher _checkpointPub;
  
  private String _threadName;

  private long _enqueueCount;
  private long _dequeueCount;

  private long _messageCount;
  private long _lastAddress;
  private long _lastCheckpoint;

  private int _size;

  // private JournalFile _journalFile;

  // private ServiceQueue<NautilusJournalItem> _actorQueue;

  private long _queueIdSequence;

  private long _senderIdSequence;

  private long _receiverIdSequence;

  private BrokerDataStore _dataStore;

  private ServiceManagerAmp _rampManager;

  private KrakenSystem _kraken;

  private BartenderSystem _bartender;

  private PodBartender _pod;

  BrokerServiceImpl(KrakenSystem kraken)
  {
    Objects.requireNonNull(kraken);
    _kraken = kraken;
    
    _bartender = BartenderSystem.current();
    Objects.requireNonNull(_bartender);
    
    _pod = _bartender.getLocalPod();
    
    // _path = path;
    _threadName = toString();
    
    _rampManager = AmpSystem.currentManager();
    Objects.requireNonNull(_rampManager);
    
    initImpl();
  }

  public long getInitialSequence()
  {
    return _initialSequence;
  }

  /*
  void setNautilusCheckpointPublisher(NautilusCheckpointPublisher pub)
  {
    _checkpointPub = pub;
  }
  */

  public int getSize()
  {
    return _size;
  }

  public long getEnqueueCount()
  {
    return _enqueueCount;
  }

  public long getDequeueCount()
  {
    return _dequeueCount;
  }

  /*
  ServiceQueue<NautilusJournalItem> getActorQueue()
  {
    return _actorQueue;
  }
  */
  
  private void initImpl()
  {
    KrakenSystem kraken = KrakenSystem.getCurrent();
    
    Objects.requireNonNull(kraken);
    
    _dataStore = new BrokerDataStore(kraken);
    /*
    try {
      _path.mkdirs();
    } catch (IOException e) {
      e.printStackTrace();
    }
    */
    
    _initialSequence = (CurrentTime.getCurrentTime() & 0xffff_ffffL) << 24L;
    _queueIdSequence = _initialSequence;
    _senderIdSequence = _initialSequence;
    _receiverIdSequence = _initialSequence;
    
    // JournalRecoverListener recover = new RecoverListener();
    
    // Path journalPath = _path.lookup("journal_0");
    
    // _journalFile = new JournalFile(journalPath, recover);
    
  
    /*
    _nautilusActorQueue = new ActorQueue<NautilusRingItem>(8192,
        new NautilusItemFactory(),
        new JournalWriteActor(_journalFile),
        _nautilusActor);
        */
    /*
    ActorQueueBuilderImpl<NautilusJournalItem> builder = new ActorQueueBuilderImpl<>();
    ActorProcessorBuilder<NautilusJournalItem> processorBuilder = builder.createProcessorBuilder();
    */
    /* XXX: types
    processorBuilder.processors(new JournalWriteActor(_journalFile),
                                this);
                                */
    
    //_actorQueue = processorBuilder.build();
    
    //NautilusCheckpointPublisher pub
    //  = new NautilusCheckpointPublisher(_actorQueue);
    
    // setNautilusCheckpointPublisher(pub);
    
    /*
    try {
      _db = new DataStoreNautilus(_path);
      _db.init();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    */
  }

  /*
  @Override
  public void deliver(NautilusJournalItem entry)
    throws Exception
  {
    if (true) {
      entry.invoke(this);
      return;
    }
  }
  */
  
  public void createSender(String queueName, 
                           Map<String, Object> properties,
                           Result<SenderBroker> result)
  {
    QueueService queue = getQueue(queueName);
    
    long sid = ++_senderIdSequence;
    
    /*
    SenderServiceNautilus senderService
      = new SenderServiceNautilus(sid, queue);
      */
    
    SenderBrokerNautilus sender = new SenderBrokerNautilus(sid, queue);
    
    result.ok(sender);
  }

  public ReceiverBroker createReceiver(String queueName,
                                       ReceiverMode distMode,
                                       Map<String, Object> queueProperties,
                                       ReceiverMessageHandler handler)
  {
    QueueService queue = getQueue(queueName);
    
    long rid = ++_receiverIdSequence;
    
    ReceiverBrokerNautilus receiver;

    receiver = new ReceiverBrokerNautilus(rid, queue, handler);

    queue.subscribe(receiver);
        
    return receiver;
  }

  /*
  SenderServiceNautilus registerSender(String queueName,
                                     Map<String, Object> properties)
  {
    long sid = ++_senderIdSequence;
    
    QueueService queue = getQueue(queueName); 
        
    return new SenderServiceNautilus(sid, queue);
  }
  */

  /*
  ReceiverServiceNautilus registerReceiver(String queueName,
                                         Map<String, Object> properties,
                                         ReceiverMessageHandler handler)
 {
    long rid = ++_receiverIdSequence;
    
    QueueService queue = getQueue(queueName); 
        
    ReceiverServiceNautilus actorReceiver;
    
    actorReceiver = new ReceiverServiceNautilus(rid, queue, handler);
    
    queue.register(actorReceiver);
    
    return actorReceiver;
  }
  */
  
  /*
  private void updateCheckpoint(long tailAddress)
  {
    long checkpointAddress = tailAddress;

    for (NautilusActorQueue queue : _queueIdMap.values()) {
      if (checkpointAddress > 0) {
        checkpointAddress = queue.updateCheckpoint(checkpointAddress);
      }
    }

    if (checkpointAddress > 0 && checkpointAddress != _lastCheckpoint) {
      NautilusCheckpointPublisher checkpointPub = _checkpointPub;
      if (checkpointPub != null
          && checkpointPub.checkpoint(checkpointAddress)) {
        _lastCheckpoint = checkpointAddress;
      }
    }
  }
  */
  
  public QueueService getQueueForRemote(String name)
  {
    return getQueue(name);
  }

  QueueService getQueue(String name)
  {
    // local name include the local pod
    if (name.startsWith("///")) {
      String tail = name.substring(2);
      
      name = "//" + _pod.getName() + tail;
    }
    else if (name.startsWith("//")) {
    }
    else if (name.startsWith("/")) {
      name = "//" + _pod.getName() + name;
    }
    else {
      name = "//" + _pod.getName() + "/" + name;
    }
    
    QueueService queue = _queueNameMap.get(name);

    if (queue == null) {
      /*
      if (! _db.findQueue(name, this)) {
        long qid = ++_queueIdSequence;
      
        queue = new QueueServiceNautilus(this, name, qid);
      
        addQueue(queue);
        
        _db.addQueue(queue.getId(), queue.getName());
      }
      */
      
      queue = createQueue(name);
    }

    return queue;
  }
  
  private QueueService createQueue(String name)
  {
    long qid = Murmur64.generate(0, name);
    
    int p = name.indexOf('/', 2);
    
    String podName = name.substring(2, p);
    
    PodBartender pod = _bartender.findPod(podName);
    
    ServerBartender server = pod.getNode(0).getServer(0);
    
    if (server == _bartender.serverSelf()) {
      QueueServiceLocal queueImpl = new QueueServiceLocal(this, name, qid);
    
      addQueue(queueImpl);
    
      // _db.addQueue(queue.getId(), queue.getName());
    
      return _queueNameMap.get(name);
    }
    else {
      QueueServiceRemote queueImpl
        = new QueueServiceRemote(this, name, qid, pod);
    
      addQueue(queueImpl);

      return _queueNameMap.get(name);
    }
  }
  
  void addQueue(QueueServiceBase queueImpl)
  {
    QueueService queue = _rampManager.newService(queueImpl)
                                     .as(QueueService.class);
    
    _queueNameMap.put(queueImpl.getName(), queue);
    _queueIdMap.put(queueImpl.getId(), queue);
    
    queue.start();
  }

  QueueService getQueue(Long id)
  {
    QueueService queue = _queueIdMap.get(id);

    if (queue == null) {
      QueueServiceLocal queueImpl = new QueueServiceLocal(this, "unknown", id);
      
      addQueue(queueImpl);
      
      queue = _queueIdMap.get(id);
    }

    return queue;
  }

  void loadQueue(QueueServiceLocal queue)
  {
    _dataStore.restoreMessage(queue);
  }

  void saveMessage(QueueServiceLocal queue, long mid, InputStream is)
  {
    _dataStore.saveMessage(queue.getId(), mid, is);
    //System.out.println("SAVE: " + queue +  " " + mid + " " + is);
    //_db.saveMessage(queue, mid, is);
  }

  InputStream loadMessage(long qid, long mid)
  {
    return _dataStore.loadMessage(qid, mid);
  }

  /*
  void receiveJdbc(long jdbcOid, 
                          MessageNautilus msg,
                          ReceiverServiceNautilus receiver)
  {
    //_db.receiveMessage(jdbcOid, msg, receiver);
  }
  */

  void acceptJdbc(long jdbcOid, MessageNautilus msg)
  {
    //_db.acceptMessage(jdbcOid, msg);
  }

  void accepted(long qid, long mid)
  {
    _dataStore.deleteMessage(qid, mid);
    //_db.acceptMessage(jdbcOid, msg);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
