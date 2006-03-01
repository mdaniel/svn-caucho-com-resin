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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.jms.*;

import com.caucho.jms.AbstractDestination;
import com.caucho.jms.session.SessionImpl;
import com.caucho.log.Log;
import com.caucho.util.L10N;

/**
 * A basic topic.
 */
public class MemoryTopic extends AbstractDestination implements Topic {
  static final Logger log = Log.open(MemoryTopic.class);
  static final L10N L = new L10N(MemoryTopic.class);

  ArrayList<MemoryQueue> _subscribers = new ArrayList<MemoryQueue>();

  private HashMap<String,MemoryQueue> _durableSubscribers =
    new HashMap<String,MemoryQueue>();

  private String _topicName;

  public MemoryTopic()
  {
  }
                                            
  /**
   * Returns the topic's name.
   */
  public String getTopicName()
  {
    return _topicName;
  }

  /**
   * Sets the topic's name.
   */
  public void setTopicName(String name)
  {
    _topicName = name;
  }

  public void send(Message message)
    throws JMSException
  {
    for (int i = 0; i < _subscribers.size(); i++) {
      MemoryQueue queue = _subscribers.get(i);

      queue.send(message);
    }
  }
  
  /**
   * Creates a consumer.
   *
   * @param session the owning session
   * @param selector the consumer's selector
   * @param noLocal true if pub/sub should not send local requests
   */
  public MessageConsumer createConsumer(SessionImpl session,
					String selector,
					boolean noLocal)
    throws JMSException
  {
    return new MemoryTopicConsumer(session, selector, this);
  }

  /**
   * Creates a durable subscriber.
   */
  public TopicSubscriber createDurableSubscriber(SessionImpl session,
						 String selector,
						 boolean noLocal,
						 String name)
    throws JMSException
  {
    return new MemoryTopicConsumer(session, selector, this, name);
  }

  /**
   * finds/creates a durable subscriber.
   */
  public MemoryQueue createDurableSubscriber(String name)
    throws JMSException
  {
    MemoryQueue queue = _durableSubscribers.get(name);

    if (queue == null) {
      queue = createSubscriberQueue();
      _durableSubscribers.put(name, queue);
    }

    return queue;
  }

  MemoryQueue createSubscriberQueue()
    throws JMSException
  {
    MemoryQueue queue = new MemoryQueue();
    
    _subscribers.add(queue);

    return queue;
  }

  public void removeSubscriber(MemoryQueue queue)
  {
    _subscribers.remove(queue);
  }

  /**
   * Returns a printable view of the topic.
   */
  public String toString()
  {
    return "Topic[" + _topicName + "]";
  }
}

