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

package com.caucho.jms.queue;

import java.util.*;
import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;

import com.caucho.util.*;

/**
 * Implements an abstract queue.
 */
abstract public class AbstractQueue extends AbstractDestination
  implements javax.jms.Queue
{
  private static final L10N L = new L10N(AbstractQueue.class);
  private static final Logger log
    = Logger.getLogger(AbstractQueue.class.getName());

  private ArrayList<MessageConsumerImpl> _messageConsumerList
    = new ArrayList<MessageConsumerImpl>();

  private int _roundRobin;
  
  private int _enqueueCount;

  protected AbstractQueue()
  {
  }
  
  public void addConsumer(MessageConsumerImpl consumer)
  {
    synchronized (_messageConsumerList) {
      if (! _messageConsumerList.contains(consumer))
	_messageConsumerList.add(consumer);
    }
  }
  
  public void removeConsumer(MessageConsumerImpl consumer)
  {
    synchronized (_messageConsumerList) {
      _messageConsumerList.remove(consumer);

      // force a poll to avoid missing messages
      for (int i = 0; i < _messageConsumerList.size(); i++) {
	_messageConsumerList.get(i).notifyMessageAvailable();
      }
    }
  }

  @Override
  public void send(JmsSession session, Message msg, long timeout)
    throws JMSException
  {
    if (log.isLoggable(Level.FINE))
      log.fine(L.l("{0} sending message {1}", this, msg));
    
    long expires = Alarm.getCurrentTime() + timeout;
    
    MessageImpl queueMsg = _messageFactory.copy(msg);

    enqueue(queueMsg, expires);
    
    synchronized (_messageConsumerList) {
      if (_messageConsumerList.size() > 0) {
	MessageConsumerImpl consumer;
	int count = _messageConsumerList.size();

	// notify until one of the consumers signals readiness to read
	do {
	  int roundRobin = _roundRobin++ % _messageConsumerList.size();
	  
	  consumer = _messageConsumerList.get(roundRobin);
	} while (! consumer.notifyMessageAvailable() && count-- > 0);
      }
    }
  }

  /**
   * Adds the message to the persistent store.
   */
  protected void enqueue(MessageImpl msg, long expires)
    throws JMSException
  {
  }

  public void close()
  {
    super.close();
  }
  
  /**
   * Creates a QueueBrowser to browse messages in the queue.
   *
   * @param queue the queue to send messages to.
   */
  public QueueBrowser createBrowser(JmsSession session,
				    String messageSelector)
    throws JMSException
  {
    return new MessageBrowserImpl(this, messageSelector);
  }

  public String toString()
  {
    String className = getClass().getName();

    int p = className.lastIndexOf('.');
    
    return className.substring(p + 1) + "[" + getName() + "]";
  }
}

