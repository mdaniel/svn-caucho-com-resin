/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.jms2.queue;

import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms2.message.*;
import com.caucho.jms2.listener.*;
import com.caucho.jms2.connection.*;

import com.caucho.util.Alarm;

/**
 * Implements an abstract queue.
 */
abstract public class AbstractQueue
  implements javax.jms.Queue
{
  private static final Logger log
    = Logger.getLogger(AbstractQueue.class.getName());

  private String _name = "default";

  protected MessageFactory _messageFactory = new MessageFactory();
  private ListenerManager _listenerManager = new ListenerManager();

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

  public TextMessage createTextMessage(String msg)
    throws JMSException
  {
    return _messageFactory.createTextMessage(msg);
  }

  public void addListener(MessageListener listener)
  {
    _listenerManager.addListener(listener);
  }

  public void send(Message msg, long timeout)
    throws JMSException
  {
    long expires = Alarm.getCurrentTime() + timeout;
    
    MessageImpl queueMsg = _messageFactory.copy(msg);
    
    SendStatus status = _listenerManager.send(queueMsg);

    if (status == SendStatus.FAIL) {
      enqueue(queueMsg, timeout);
    }
  }

  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   */
  protected void enqueue(MessageImpl msg, long expires)
  {
  }

  /**
   * Polls the next message from the store.  If no message is available,
   * wait for the timeout.
   */
  public MessageImpl receive(long timeout)
  {
    return null;
  }

  public void close()
  {
    ListenerManager listenerManager = _listenerManager;
    _listenerManager = null;

    if (listenerManager != null)
      listenerManager.close();
  }

  public String generateMessageID()
  {
    return "ook";
  }
  
  /**
   * Creates a QueueBrowser to browse messages in the queue.
   *
   * @param queue the queue to send messages to.
   */
  public QueueBrowser createBrowser(SessionImpl session,
				    String messageSelector)
    throws JMSException
  {
    return null;
  }

  public String toString()
  {
    return getClass().getName() + "[" + getName() + "]";
  }
}

