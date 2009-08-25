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
   * Creates a new random message identifier.
   */
  public final String generateMessageID()
  {
    StringBuilder cb = new StringBuilder();

    cb.append("ID:");
    generateMessageID(cb);

    return cb.toString();
  }

  /**
   * Customization of the message id for different queue/topics
   */
  protected void generateMessageID(StringBuilder cb)
  {
    long id = _idCount.getAndIncrement();

    Base64.encode(cb, _idRandom);
    Base64.encode(cb, id);
  }

  /**
   * Sends a message to the queue
   */
  abstract public void send(String msgId,
                            Serializable msg,
                            int priority,
                            long expires)
    throws MessageException;

  /**
   * Sends a message to the queue
   */
  /*
  abstract public void send(String msgId,
                            Serializable msg,
                            int priority,
                            long expires,
                            Session sendingSession)
    throws MessageException;
  */

  public Serializable receive(long timeout)
    throws MessageException
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

  public Destination getJMSDestination()
  {
    return new DestinationHandle(toString());
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

