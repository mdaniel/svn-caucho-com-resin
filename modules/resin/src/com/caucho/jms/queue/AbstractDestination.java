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

import java.util.logging.*;

import javax.jms.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.caucho.jms.JmsRuntimeException;
import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;

import com.caucho.util.*;
import com.caucho.config.Configurable;
import com.caucho.config.inject.HandleAware;

/**
 * Implements an abstract queue.
 */
abstract public class AbstractDestination
  extends java.util.AbstractQueue
  implements javax.jms.Destination, BlockingQueue,
	     java.io.Serializable, HandleAware
{
  private static final L10N L = new L10N(AbstractDestination.class);
  private static final Logger log
    = Logger.getLogger(AbstractDestination.class.getName());

  private static long _idRandom;
  private static final AtomicLong _idCount = new AtomicLong();
  
  private String _name = "default";

  protected MessageFactory _messageFactory = new MessageFactory();
  
  // queue api
  private ConnectionFactoryImpl _connectionFactory;
  private Connection _conn;

  private Object _readLock = new Object();
  private Object _writeLock = new Object();

  private JmsSession _writeSession;
  private JmsSession _readSession;
  private MessageConsumerImpl _consumer;

  // serialization
  private Object _serializationHandle;

  protected AbstractDestination()
  {
    if (_idRandom == 0 || Alarm.isTest()) {
      _idCount.set(Alarm.getCurrentTime() << 16);
      _idRandom = RandomUtil.getRandomLong();
    }
  }

  /**
   * Sets the name of the destination
   */
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }
  
  public String getName()
  {
    return _name;
  }

  public String getQueueName()
  {
    return getName();
  }
  
  public String getTopicName()
  {
    return getName();
  }

  /**
   * Serialization callback from Resin-IoC to set the handle
   */
  public void setSerializationHandle(Object handle)
  {
    _serializationHandle = handle;
  }

  //
  // JMX configuration data
  //

  /**
   * Returns a descriptive URL for the queue.
   */
  public String getUrl()
  {
    return getClass().getSimpleName() + ":";
  }

  //
  // runtime methods
  //
  
  /**
   * Adds a new listener to receive message available events.  The listener
   * will wake or spawn a thread to handle the new message.  It MUST NOT
   * handle the message on the event thread.
   * 
   * Each listener should be associated with a single thread, i.e. multiple
   * notifyMessageAvailable() calls MUST NOT spawn multiple threads.  This
   * single-thread restriction is necessary to properly manage round-robin
   * behavior.
   * 
   * @param consumer
   */
  public void addMessageAvailableListener(MessageAvailableListener consumer)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Removes the consumer receiving messages.
   */
  public void removeMessageAvailableListener(MessageAvailableListener consumer)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sends a message to the queue
   */
  abstract public void send(JmsSession session,
			    MessageImpl msg,
			    int priority,
			    long expires)
    throws JMSException;
  
  /**
   * Polls the next message from the store.  Returns null if no message
   * is available.
   *
   * @param isAutoAcknowledge if true, automatically acknowledge the message
   */
  public MessageImpl receive(boolean isAutoAcknowledge)
    throws JMSException
  {
    return null;
  }

  /**
   * Returns true if the queue has at least one message available
   */
  public boolean hasMessage()
  {
    return false;
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

  /**
   * Creates a new random message identifier.
   */
  public final String generateMessageID()
  {
    StringBuilder cb = new StringBuilder();

    cb.append("ID:");
    generateMessageID(cb);

    return cb.toString();
  }

  protected void generateMessageID(StringBuilder cb)
  {
    long id = _idCount.getAndIncrement();

    Base64.encode(cb, _idRandom);
    Base64.encode(cb, id);
  }

  public Destination getJMSDestination()
  {
    return new DestinationHandle(toString());
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
    try {
      synchronized (_writeLock) {
	JmsSession session = getWriteSession();

	Message msg;

	if (value instanceof Message)
	  msg = (Message) value;
	else
	  msg = session.createObjectMessage((Serializable) value);
	
	session.send(this, msg, 0, 0, Integer.MAX_VALUE);

	return true;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JmsRuntimeException(e);
    }
  }

  public Object poll(long timeout, TimeUnit unit)
  {
    try {
      synchronized (_readLock) {
	MessageConsumerImpl consumer = getReadConsumer();

	long msTimeout = unit.toMillis(timeout);

	Message msg = consumer.receive(msTimeout);

	if (msg instanceof ObjectMessage) {
	  return ((ObjectMessage) msg).getObject();
	}
	else if (msg instanceof TextMessage) {
	  return ((TextMessage) msg).getText();
	}
	else if (msg == null)
	  return null;
	else
	  throw new JmsRuntimeException(L.l("'{0}' is an unsupported message for the BlockingQueue API.",
					    msg));
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JmsRuntimeException(e);
    }
  }

  public boolean offer(Object value)
  {
    return offer(value, 0, TimeUnit.SECONDS);
  }

  public void put(Object value)
  {
    offer(value, Integer.MAX_VALUE, TimeUnit.SECONDS);
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

  protected JmsSession getWriteSession()
    throws JMSException
  {
    if (_conn == null) {
      _connectionFactory = new ConnectionFactoryImpl();
      _conn = _connectionFactory.createConnection();
      _conn.start();
    }
    
    if (_writeSession == null) {
      _writeSession =
	(JmsSession) _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    return _writeSession;
  }

  protected MessageConsumerImpl getReadConsumer()
    throws JMSException
  {
    if (_conn == null) {
      _connectionFactory = new ConnectionFactoryImpl();
      _conn = _connectionFactory.createConnection();
      _conn.start();
    }
    
    if (_readSession == null) {
      _readSession =
	(JmsSession) _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }
    
    if (_consumer == null) {
      _consumer = (MessageConsumerImpl) _readSession.createConsumer(this);
    }

    return _consumer;
  }

  /**
   * Serialization handle
   */
  private Object writeReplace()
  {
    return _serializationHandle;
  }

  public void close()
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}

