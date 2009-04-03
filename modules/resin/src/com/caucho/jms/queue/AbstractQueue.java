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

package com.caucho.jms.queue;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import javax.annotation.*;
import javax.jms.*;

import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;

import com.caucho.util.*;

/**
 * Implements an abstract queue.
 */
abstract public class AbstractQueue extends AbstractDestination
  implements javax.jms.Queue, MessageQueue, BlockingQueue
{
  private static final L10N L = new L10N(AbstractQueue.class);
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
  public boolean listen(MessageCallback callback)
    throws MessageException
  {
    return false;
  }

  /**
   * Removes the callback from the listening list.
   */
  public void removeMessageCallback(MessageCallback callback)
  {
  }
  
  /**
   * Acknowledge receipt of the message.
   * 
   * @param msgId message to acknowledge
   */
  public void acknowledge(String msgId)
  {
  }
  
  /**
   * Rollback the message read.
   */
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
  public Serializable receive(long expireTime)
    throws MessageException
  {
    BlockingReceiveCallback cb = new BlockingReceiveCallback();

    return cb.receive(this, true, expireTime);
  }

  /**
   * Receives a message, blocking until expireTime if no message is
   * available.
   */
  public Serializable receive(long expireTime,
			      boolean isAutoAcknowledge)
    throws MessageException
  {
    BlockingReceiveCallback cb = new BlockingReceiveCallback();

    return cb.receive(this, isAutoAcknowledge, expireTime);
  }

  public ArrayList<MessageImpl> getBrowserList()
  {
    return new ArrayList<MessageImpl>();
  }

  //
  // BlockingQueue api
  //

  public int size()
  {
    return 0;
  }

  public Iterator iterator()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds the item to the queue, waiting if necessary
   */
  public boolean offer(Object value, long timeout, TimeUnit unit)
  {
    int priority = 0;
      
    timeout = unit.toMillis(timeout);

    long expires = Alarm.getCurrentTime() + timeout;
      
    send(generateMessageID(), (Serializable) value, priority, expires);

    return true;
  }

  public boolean offer(Object value)
  {
    return offer(value, 0, TimeUnit.SECONDS);
  }

  public void put(Object value)
  {
    offer(value, Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  public Object poll(long timeout, TimeUnit unit)
  {
    long msTimeout = unit.toMillis(timeout);
    
    Serializable payload = receive(msTimeout);

    try {
      if (payload == null)
	return null;
      else if (payload instanceof ObjectMessage)
	return ((ObjectMessage) payload).getObject();
      else if (payload instanceof TextMessage)
	return ((TextMessage) payload).getText();
      else if (payload instanceof Serializable)
	return payload;
      else
	throw new MessageException(L.l("'{0}' is an unsupported message for the BlockingQueue API.",
				       payload));
    } catch (JMSException e) {
      throw new MessageException(e);
    }
  }

  public int remainingCapacity()
  {
    return Integer.MAX_VALUE;
  }

  public Object peek()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Object poll()
  {
    return poll(0, TimeUnit.MILLISECONDS);
  }

  public Object take()
  {
    return poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  public int drainTo(Collection c)
  {
    throw new UnsupportedOperationException();
  }

  public int drainTo(Collection c, int max)
  {
    throw new UnsupportedOperationException();
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
      _listenerFailLastTime = Alarm.getCurrentTime();
    }
  }

  protected void startPoll()
  {
  }

  protected void stopPoll()
  {
  }

  @PreDestroy
  @Override
  public void close()
  {
    stopPoll();
    
    super.close();
  }
}

