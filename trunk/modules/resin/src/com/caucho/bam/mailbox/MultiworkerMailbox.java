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

package com.caucho.bam.mailbox;

import java.io.Closeable;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.BamException;
import com.caucho.bam.QueueFullException;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.packet.Message;
import com.caucho.bam.packet.MessageError;
import com.caucho.bam.packet.Packet;
import com.caucho.bam.packet.Query;
import com.caucho.bam.packet.QueryError;
import com.caucho.bam.packet.QueryResult;
import com.caucho.bam.stream.MessageStream;
import com.caucho.cloud.bam.BamQueueFullHandler;
import com.caucho.env.actor.AbstractActorProcessor;
import com.caucho.env.actor.ActorProcessor;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.L10N;

/**
 * mailbox for BAM messages waiting to be sent to the Actor.
 */
public class MultiworkerMailbox implements Mailbox, Closeable
{
  private static final L10N L = new L10N(MultiworkerMailbox.class);
  private static final Logger log
    = Logger.getLogger(MultiworkerMailbox.class.getName());

  private final String _name;
  private final String _address;
  private final Broker _broker;
  private final MessageStream _actorStream;
  
  private final int _queueSize = 16 * 1024;

  private final MailboxQueue2 []_queues;
  
  private final Lifecycle _lifecycle = new Lifecycle();
  
  private final AtomicInteger _roundRobin = new AtomicInteger();

  public MultiworkerMailbox(MessageStream actorStream,
                            Broker broker,
                            int threadMax)
  {
    this(null, actorStream, broker, threadMax);
  }
  
  public MultiworkerMailbox(String address, 
                            MessageStream actorStream,
                            Broker broker,
                            int threadMax)
  {
    _address = address;
    
    if (broker == null)
      throw new NullPointerException(L.l("broker must not be null"));

    if (actorStream == null)
      throw new NullPointerException(L.l("actorStream must not be null"));

    _broker = broker;
    _actorStream = actorStream;
    
    if (_actorStream.getAddress() == null) {
      _name = _actorStream.getClass().getSimpleName();
    }
    else
      _name = _actorStream.getAddress();
    
    _queues = new MailboxQueue2[threadMax];
    
    for (int i = 0; i < threadMax; i++) {
      _queues[i] = createWorker();
    }

    /*
    int maxDiscardSize = -1;
    int maxBlockSize = 1024;
    long expireTimeout = -1;
    */
    
    _lifecycle.toActive();
  }
  
  protected MailboxQueue2 createWorker()
  {
    return new MailboxQueue2(_queueSize, createProcessor());
  }
  
  protected ActorProcessor<Packet> createProcessor()
  {
    return new PacketProcessor();
  }
  
  public int getThreadMax()
  {
    return _queues.length;
  }
  
  public int getSize()
  {
    int size = 0;
    
    for (MailboxQueue2 mailbox : _queues) {
      size += mailbox.getSize();
    }
    
    return size;
  }

  /**
   * Returns the actor's address
   */
  @Override
  public String getAddress()
  {
    if (_address != null)
      return _address;
    else
      return _actorStream.getAddress();
  }

  /**
   * Returns true if a message is available.
   */
  public boolean isPacketAvailable()
  {
    // return ! _queue.isEmpty();
    
    return false;
  }
  
  /**
   * Returns the stream back to the link for error packets
   */
  @Override
  public Broker getBroker()
  {
    return _broker;
  }

  @Override
  public MessageStream getActorStream()
  {
    return _actorStream;
  }

  /**
   * Sends a message
   */
  @Override
  public void message(String to, String from, Serializable value)
  {
    try {
      enqueue(new Message(to, from, value));
    } catch (QueueFullException e) {
      log.finer(e.toString());
    } catch (RuntimeException e) {
      log.warning(this + ": message "
          + value + " {to:" + to + ", from:" + from + "}"
          + "\n  " + e.toString());
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Sends a message
   */
  @Override
  public void messageError(String to,
                               String from,
                               Serializable value,
                               BamError error)
  {
    try {
      enqueue(new MessageError(to, from, value, error));
    } catch (QueueFullException e) {
      log.finer(e.toString());
    } catch (RuntimeException e) {
      log.warning(this + ": messageError "
          + value + " {to:" + to + ", from:" + from + "}"
          + "\n  " + e.toString());
      
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Query an entity
   */
  @Override
  public void query(long id,
                       String to,
                       String from,
                       Serializable query)
  {
    if (! _lifecycle.isActive()) {
      BamException exn = new RemoteConnectionFailedException(L.l("{0} is closed",
                                                                 this));
      exn.fillInStackTrace();
      
      getBroker().queryError(id, from, to, query,
                             BamError.create(exn));
      return;
    }

    try {
      enqueue(new Query(id, to, from, query));
    } catch (QueueFullException e) {
      log.finer(e.toString());
      
      getBroker().queryError(id, from, to, query, BamError.create(e));
    } catch (RuntimeException e) {
      log.warning(this + ": query "
          + query + " {to:" + to + ", from:" + from + "}"
          + "\n  " + e.toString());
      
      getBroker().queryError(id, from, to, query, BamError.create(e));
    }
  }

  /**
   * Query an entity
   */
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable value)
  {
    try {
      enqueue(new QueryResult(id, to, from, value));
    } catch (QueueFullException e) {
      log.finer(e.toString());
    } catch (RuntimeException e) {
      log.warning(this + ": queryResult "
                  + value + " {to:" + to + ", from:" + from + "}"
                  + "\n  " + e.toString());
    
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Query an entity
   */
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable query,
                         BamError error)
  {
    try {
      enqueue(new QueryError(id, to, from, query, error));
    } catch (QueueFullException e) {
      log.finer(e.toString());
    } catch (RuntimeException e) {
      log.warning(this + ": queryError "
          + error + " {to:" + to + ", from:" + from + "}"
          + "\n  " + e.toString());
  
      log.log(Level.FINE, e.toString(), e);
    }
  }

  protected final void enqueue(Packet packet)
  {
    if (! _lifecycle.isActive())
      throw new IllegalStateException(L.l("{0} cannot accept packets because it's no longer active",
                                          this));
    
    MailboxQueue2 workerQueue = findWorker();
    
    if (log.isLoggable(Level.FINEST)) {
      int size = workerQueue.getSize();
      log.finest(this + " enqueue(" + size + ") " + packet);
    }
    
    if (! workerQueue.offer(packet, false)) {
      workerQueue.wake();

      if (! workerQueue.offer(packet, true)) {
        workerQueue.wake();
        
        BamQueueFullHandler handler = _broker.getQueueFullHandler();

        handler.onQueueFull(this, workerQueue.getSize(), 
                            100, TimeUnit.MILLISECONDS, 
                            packet);
      }
    }
    
    workerQueue.wake();
  }
  

  private MailboxQueue2 findWorker()
  {
    int minSize = Integer.MAX_VALUE;
    MailboxQueue2 bestQueue = _queues[0];
    
    for (MailboxQueue2 queue : _queues) {
      int size = queue.getSize();
      
      if (size < minSize) {
        bestQueue = queue;
        minSize = size;
      }
    }
    
    return bestQueue;
  }

  @Override
  public void close()
  {
    _lifecycle.toStop();

    for (MailboxQueue2 worker : _queues) {
      worker.wake();
    }
    
    long expires = getCurrentTimeActual() + 2000;
    
    while (! isQueueEmpty()
           && getCurrentTimeActual() < expires) {
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        
      }
    }

    for (MailboxQueue2 queue : _queues) {
      queue.close();
    }
    
    _lifecycle.toDestroy();
  }
  
  private boolean isQueueEmpty()
  {
    for (MailboxQueue2 queue : _queues) {
      if (queue.isEmpty()) {
        return true;
      }
    }
    
    return false;
  }
  
  protected long getCurrentTimeActual()
  {
    return System.currentTimeMillis();
  }

  @Override
  public boolean isClosed()
  {
    return _lifecycle.isDestroying() || _broker.isClosed();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
  
  private class PacketProcessor extends AbstractActorProcessor<Packet> {
    PacketProcessor()
    {
    }

    @Override
    public String getThreadName()
    {
      return _name + "-" + Thread.currentThread().getId();
    }
    
    @Override
    public void process(Packet packet) throws Exception
    {
      packet.dispatch(getActorStream(), _broker);
    }

    @Override
    public void onProcessComplete() throws Exception
    {
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _name + "]";
    }
  }
}
