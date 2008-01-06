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

import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;

import com.caucho.util.Alarm;
import com.caucho.util.Base64;
import com.caucho.util.RandomUtil;

/**
 * Implements an abstract queue.
 */
abstract public class AbstractDestination
  implements javax.jms.Destination
{
  private static final Logger log
    = Logger.getLogger(AbstractDestination.class.getName());

  private static long _idRandom;
  private static long _idCount;
  
  private String _name = "default";

  protected MessageFactory _messageFactory = new MessageFactory();

  protected AbstractDestination()
  {
    synchronized (AbstractDestination.class) {
      if (_idRandom == 0 || Alarm.isTest()) {
        _idRandom = RandomUtil.getRandomLong();
        _idCount = Alarm.getCurrentTime() << 16;
      }
    }
  }

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
  
  public void addConsumer(MessageConsumerImpl consumer)
  {
  }
  
  public void removeConsumer(MessageConsumerImpl consumer)
  {
  }

  abstract public void send(JmsSession session, MessageImpl msg, long timeout)
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

  public boolean hasMessage()
  {
    return false;
  }
  
  /**
   * Acknowledge receipt of the message.
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

  public final String generateMessageID()
  {
    StringBuilder cb = new StringBuilder();

    cb.append("ID:");
    generateMessageID(cb);

    return cb.toString();
  }

  protected void generateMessageID(StringBuilder cb)
  {
    long id;
    
    synchronized (AbstractDestination.class) {
      id = _idCount++;
    }

    Base64.encode(cb, _idRandom);
    Base64.encode(cb, id);
  }

  public Destination getJMSDestination()
  {
    return new DestinationHandle(toString());
  }

  protected Object writeReplace()
  {
    return new DestinationHandle(toString());
  }

  public void close()
  {
  }

  public String toString()
  {
    return getClass().getName() + "[" + getName() + "]";
  }
}

