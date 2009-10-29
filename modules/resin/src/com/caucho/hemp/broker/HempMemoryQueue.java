/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.hemp.broker;

import com.caucho.hemp.packet.*;
import com.caucho.bam.Broker;
import com.caucho.bam.ActorStream;
import com.caucho.bam.ActorError;
import com.caucho.loader.Environment;
import com.caucho.server.resin.*;
import com.caucho.server.util.*;
import com.caucho.util.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;
import java.io.*;

/**
 * Queue of hmtp packets
 */
public class HempMemoryQueue implements ActorStream, Runnable, Closeable
{
  private static final Logger log
    = Logger.getLogger(HempMemoryQueue.class.getName());
  private static final L10N L = new L10N(HempMemoryQueue.class);

  private static long _gid;

  // how long the thread should wait for a new request before exiting
  private long _queueIdleTimeout = 500L;

  private final ThreadPool _threadPool = ThreadPool.getCurrent();
  private final ClassLoader _loader
    = Thread.currentThread().getContextClassLoader();

  private final Broker _broker;
  private final ActorStream _brokerStream;
  private final ActorStream _actorStream;

  private int _threadMax;
  private AtomicInteger _threadCount = new AtomicInteger();
  private AtomicInteger _dequeueCount = new AtomicInteger();
  private Object _idleLock = new Object();
  private final WaitQueue _wait = new WaitQueue();
  private long _lastExitTime;

  private PacketQueue _queue;
  private String _name;

  private volatile boolean _isClosed;

  public HempMemoryQueue(Broker broker,
                         ActorStream actorStream)
  {
    this(null, broker, actorStream);
  }

  public HempMemoryQueue(String name,
                         Broker broker,
                         ActorStream actorStream)
  {
    if (broker == null)
      throw new NullPointerException();

    if (actorStream == null)
      throw new NullPointerException();

    _broker = broker;
    _brokerStream = broker.getBrokerStream();
    _actorStream = actorStream;

    if (Alarm.isTest())
      _threadMax = 1;
    else
      _threadMax = 5;

    if (name == null)
      name = _actorStream.getJid();

    _name = name;

    int maxDiscardSize = -1;
    int maxBlockSize = 1024;
    long expireTimeout = -1;

    _queue = new PacketQueue(name, maxDiscardSize, maxBlockSize, expireTimeout);

    Environment.addCloseListener(this);
  }

  /**
   * Returns the actor's jid
   */
  public String getJid()
  {
    return _actorStream.getJid();
  }

  /**
   * Returns true if a message is available.
   */
  public boolean isPacketAvailable()
  {
    return ! _queue.isEmpty();
  }

  /**
   * Sends a message
   */
  public void message(String to, String from, Serializable value)
  {
    enqueue(new Message(to, from, value));
  }

  /**
   * Sends a message
   */
  public void messageError(String to,
                               String from,
                               Serializable value,
                               ActorError error)
  {
    enqueue(new MessageError(to, from, value, error));
  }

  /**
   * Query an entity
   */
  public void queryGet(long id,
                       String to,
                       String from,
                       Serializable query)
  {
    if (from == null) {
      throw new NullPointerException();
    }

    enqueue(new QueryGet(id, to, from, query));
  }

  /**
   * Query an entity
   */
  public void querySet(long id,
                       String to,
                       String from,
                       Serializable query)
  {
    enqueue(new QuerySet(id, to, from, query));
  }

  /**
   * Query an entity
   */
  public void queryResult(long id,
                              String to,
                              String from,
                              Serializable value)
  {
    enqueue(new QueryResult(id, to, from, value));
  }

  /**
   * Query an entity
   */
  public void queryError(long id,
                             String to,
                             String from,
                             Serializable query,
                             ActorError error)
  {
    enqueue(new QueryError(id, to, from, query, error));
  }

  /**
   * Presence
   */
  public void presence(String to, String from, Serializable data)
  {
    enqueue(new Presence(to, from, data));
  }

  /**
   * Presence unavailable
   */
  public void presenceUnavailable(String to,
                                      String from,
                                      Serializable data)
  {
    enqueue(new PresenceUnavailable(to, from, data));
  }

  /**
   * Presence probe
   */
  public void presenceProbe(String to,
                                String from,
                                Serializable data)
  {
    enqueue(new PresenceProbe(to, from, data));
  }

  /**
   * Presence subscribe
   */
  public void presenceSubscribe(String to,
                                    String from,
                                    Serializable data)
  {
    enqueue(new PresenceSubscribe(to, from, data));
  }

  /**
   * Presence subscribed
   */
  public void presenceSubscribed(String to,
                                     String from,
                                     Serializable data)
  {
    enqueue(new PresenceSubscribed(to, from, data));
  }

  /**
   * Presence unsubscribe
   */
  public void presenceUnsubscribe(String to,
                                      String from,
                                      Serializable data)
  {
    enqueue(new PresenceUnsubscribe(to, from, data));
  }

  /**
   * Presence unsubscribed
   */
  public void presenceUnsubscribed(String to,
                                       String from,
                                       Serializable data)
  {
    enqueue(new PresenceUnsubscribed(to, from, data));
  }

  /**
   * Presence error
   */
  public void presenceError(String to,
                                String from,
                                Serializable data,
                                ActorError error)
  {
    enqueue(new PresenceError(to, from, data, error));
  }

  protected ActorStream getActorStream()
  {
    return _actorStream;
  }

  protected final void enqueue(Packet packet)
  {
    if (log.isLoggable(Level.FINEST)) {
      int size = _queue.getSize();
      log.finest(this + " enqueue(" + size + ") " + packet);
    }

    _queue.enqueue(packet);

    /*
    if (_dequeueCount.get() > 0)
      return;
    */

    wakeConsumer(packet);

    if (Alarm.isTest()) {
      // wait a millisecond for the dequeue to avoid spawing extra
      // processing threads
      packet.waitForDequeue(100);
    }
  }

  private void wakeConsumer(Packet packet)
  {
    long lastExitTime = _lastExitTime;
    _lastExitTime = Alarm.getCurrentTime();

    if (_wait.wake()) {
      return;
    }

    while (true) {
      int size = _queue.getSize();
      int threadCount = _threadCount.get();
      long now = Alarm.getCurrentTime();

      if (size == 0) {
        // empty queue
        return;
      }
      else if (threadCount == _threadMax) {
        // thread max
        return;
      }
      else if (threadCount >= 2 && size / 3 < threadCount) {
        // too little work to spawn a new thread
        return;
      }
      else if (threadCount > 0 && now <= lastExitTime + 10) {
        // last spawn too recent
        return;
      }

      if (isClosed()) {
        return;
      }
      else if (_threadCount.compareAndSet(threadCount, threadCount + 1)) {
        if (! _threadPool.start(this, 10)) {
          _threadPool.schedule(this);
        }
        return;
      }
      else if (_threadCount.get() > 0) {
        // other thread already added
        return;
      }
    }
  }

  /**
   * Dispatches the packet to the stream
   */
  protected void dispatch(Packet packet, WaitQueue.Item item)
  {
    packet.dispatch(getActorStream(), _brokerStream);
  }

  protected Packet dequeue(WaitQueue.Item item, long timeout)
  {
    item.startPark();

    try {
      Packet packet = _queue.dequeue();

      if (packet == null) {
        if (timeout <= 0)
          return null;

        item.park(timeout);

        packet = _queue.dequeue();
      }

      if (packet != null)
        packet.unparkDequeue();

      return packet;
    } finally {
      item.endPark();
    }
  }

  private void consumeQueue(WaitQueue.Item item)
  {
    while (! isClosed()) {
      try {
        Packet packet;

        // _dequeueCount.incrementAndGet();
        packet = _queue.dequeue();
        // _dequeueCount.decrementAndGet();

        if (packet != null) {
          // reset last exit with a new packet
          _lastExitTime = Alarm.getCurrentTime();

          if (log.isLoggable(Level.FINEST))
            log.finest(this + " dequeue " + packet);

          packet.unparkDequeue();

          dispatch(packet, item);
        }
        else if (! waitForQueue(item)) {
          return;
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  private boolean waitForQueue(WaitQueue.Item item)
  {
    long expires = Alarm.getCurrentTimeActual() + _queueIdleTimeout;

    while (! isClosed()) {
      if (_queue.getSize() > 0) {
        // check for queue values
        return true;
      }

      long now = Alarm.getCurrentTimeActual();

      if (now < expires) {
        _wait.parkUntil(item, expires);
      }
      else if (_lastExitTime + _queueIdleTimeout < now
               || Alarm.isTest()) {
        // if thread hasn't exited recently
        _lastExitTime = now;

        int threadCount = _threadCount.decrementAndGet();

        if (_queue.getSize() > 0
            && threadCount < _threadMax
            && _threadCount.compareAndSet(threadCount, threadCount + 1)) {
          // second check needed in case a packet arrived
          return true;
        }
        else {
          // exit the thread
          return false;
        }
      }
    }

    return true;
  }

  public void run()
  {
    Thread thread = Thread.currentThread();

    String name = _name;

    thread.setName(name + "-" + _gid++);

    thread.setContextClassLoader(_loader);

    boolean isValid = false;
    WaitQueue.Item item = _wait.create();

    try {
      if (log.isLoggable(Level.FINEST)) {
        log.finest(this + " spawn {threadCount:" + _threadCount.get()
                 + ", queueSize:" + _queue.getSize() + "}");
      }

      consumeQueue(item);

      isValid = true;
    } finally {
      // fix semaphore in case of thread death
      if (! isValid) {
        _threadCount.decrementAndGet();
      }

      item.remove();
    }
  }

  public void close()
  {
    _isClosed = true;

    _wait.wakeAll();
  }

  public boolean isClosed()
  {
    return _isClosed || _broker.isClosed();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
