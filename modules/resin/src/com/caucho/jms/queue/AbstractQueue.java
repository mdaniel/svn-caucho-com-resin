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

package com.caucho.jms.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;

/**
 * Implements an abstract queue.
 */
abstract public class AbstractQueue<E> extends AbstractDestination<E>
  implements javax.jms.Queue, MessageQueue<E>, BlockingQueue<E>
{
  private static final Logger log
    = Logger.getLogger(AbstractQueue.class.getName());

  private QueueAdmin _admin;

  // stats
  private long _listenerFailCount;
  private long _listenerFailLastTime;

  protected AbstractQueue()
  {
  }

  public void setQueueName(String name)
  {
    setName(name);
  }

  protected void init()
  {
  }

  @PostConstruct
  public void postConstruct()
  {
    try {
      init();
    } catch (Exception e) {
      // XXX: issue with examples: iterating with closed table
      log.log(Level.WARNING, e.toString(), e);
    }

    _admin = new QueueAdmin(this);
    _admin.register();
  }

  /**
   * Primary message receiving, registers a callback for any new
   * message.
   */
  @Override
  public QueueEntry<E> receiveEntry(long expireTime, boolean isAutoAck)
  {
    return null;//receiveEntry(timeout, isAutoAck, null);
  }
  
  public QueueEntry<E> receiveEntry(long expireTime,
                                    boolean isAutoAck, 
                                    QueueEntrySelector selector)
    throws MessageException
  {
    return receiveEntry(expireTime, isAutoAck);
  }
  
  public void receive(long expireTime,
                      boolean isAutoAck, 
                      QueueEntrySelector selector,
                      MessageCallback callback)
    throws MessageException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Adds the callback to the listening list.
   */
  @Override
  public void addMessageCallback(MessageCallback<E> callback,
                                 boolean isAutoAck)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Removes the callback from the listening list.
   */
  @Override
  public void removeMessageCallback(MessageCallback<E> entryCallback)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Acknowledge receipt of the message.
   *
   * @param msgId message to acknowledge
   */
  @Override
  public void acknowledge(String msgId)
  {
  }

  /**
   * Rollback the message read.
   */
  @Override
  public void rollback(String msgId)
  {
  }

  //
  // convenience methods
  //

  /**
   * Receives a message, blocking until expireTime if no message is
   * available.
   */
  @Override
  public E receive(long expireTime,
                   boolean isAutoAcknowledge)
    throws MessageException
  {
    return receive(expireTime, isAutoAcknowledge, null);
  }
  
  public E receive(long expireTime,
                   boolean isAutoAcknowledge,
                   QueueEntrySelector selector)
    throws MessageException
  {
    QueueEntry<E> entry = receiveEntry(expireTime, 
                                       isAutoAcknowledge,
                                       selector);

    if (entry != null)
      return entry.getPayload();
    else
      return null;
  }

  public ArrayList<? extends QueueEntry<E>> getBrowserList()
  {
    return new ArrayList<QueueEntry<E>>();
  }

  //
  // BlockingQueue api
  //

  @Override
  public int size()
  {
    return 0;
  }

  @Override
  public Iterator<E> iterator()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds the item to the queue, waiting if necessary
   */
  @Override
  public boolean offer(E message, long timeout, TimeUnit unit)
  {
    int priority = 0;

    timeout = unit.toMillis(timeout);

    long expires = CurrentTime.getCurrentTime() + timeout;

    String publisherId = null;
    
    send(generateMessageID(), message, priority, expires, publisherId);

    return true;
  }

  @Override
  public boolean offer(E message)
  {
    return offer(message, 0, TimeUnit.SECONDS);
  }

  @Override
  public void put(E value)
  {
    offer(value, Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  @Override
  public E poll(long timeout, TimeUnit unit)
  {
    long msTimeout = unit.toMillis(timeout);
    
    long expireTime = msTimeout + CurrentTime.getCurrentTime();
    
    E payload = receive(expireTime, true);

    try {
      return payload;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MessageException(e);
    }
  }

  @Override
  public int remainingCapacity()
  {
    return Integer.MAX_VALUE;
  }

  @Override
  public E peek()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public E poll()
  {
    return poll(0, TimeUnit.MILLISECONDS);
  }

  @Override
  public E take()
  {
    return poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  @Override
  public int drainTo(Collection<? super E> c)
  {
    int count = 0;
    
    E msg;
    
    while ((msg = poll()) != null) {
      c.add(msg);
      count++;
    }
    
    return count;
  }

  @Override
  public int drainTo(Collection<? super E> c, int max)
  {
    int count = 0;
    
    E msg;
    
    while (count < max && (msg = poll()) != null) {
      c.add(msg);
      count++;
    }
    
    return count;
  }

  //
  // JMX statistics
  //

  /**
   * Returns the number of active message consumers
   */
  public int getConsumerCount()
  {
    return 0;
  }

  /**
   * Returns the number of receivers.
   *
   * @return
   */
  public int getReceiverCount()
  {
    return 0;
  }

  /**
   * Returns the queue size
   */
  public int getQueueSize()
  {
    return -1;
  }

  /**
   * Returns the number of listener failures.
   */
  public long getListenerFailCountTotal()
  {
    return _listenerFailCount;
  }

  /**
   * Returns the number of listener failures.
   */
  public long getListenerFailLastTime()
  {
    return _listenerFailLastTime;
  }

  /**
   * Called when a listener throws an excepton
   */
  public void addListenerException(Exception e)
  {
    synchronized (this) {
      _listenerFailCount++;
      _listenerFailLastTime = CurrentTime.getCurrentTime();
    }
  }

  /*
  protected void startPoll()
  {
  }

  protected void stopPoll()
  {
  }
  */

  @PreDestroy
  @Override
  public void close()
  {
    // stopPoll();

    super.close();
  }
}

