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

package com.caucho.jms2.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms2.message.*;
import com.caucho.jms2.listener.*;
import com.caucho.jms2.queue.*;
import com.caucho.jms2.connection.*;

/**
 * Implements a memory topic.
 */
public class MemoryTopic extends AbstractTopic
{
  private static final Logger log
    = Logger.getLogger(MemoryTopic.class.getName());

  private HashMap<String,MemoryQueue> _durableSubscriptionMap
    = new HashMap<String,MemoryQueue>();
    
  private ArrayList<AbstractQueue> _subscriptionList
    = new ArrayList<AbstractQueue>();

  private int _id;

  @Override
  public AbstractQueue createSubscriber(SessionImpl session,
                                        String name,
                                        boolean noLocal)
  {
    MemoryQueue queue;

    if (name != null) {
      queue = _durableSubscriptionMap.get(name);

      if (queue == null) {
	queue = new MemorySubscriberQueue(session, noLocal);
	queue.setName(getName() + ":sub-" + name);

	_subscriptionList.add(queue);
	_durableSubscriptionMap.put(name, queue);
      }

      return queue;
    }
    else {
      queue = new MemorySubscriberQueue(session, noLocal);
      queue.setName(getName() + ":sub-" + _id++);

      _subscriptionList.add(queue);
    }

    return queue;
  }

  @Override
  public void closeSubscriber(AbstractQueue queue)
  {
    if (! _durableSubscriptionMap.values().contains(queue))
      _subscriptionList.remove(queue);
  }
  
  public void send(SessionImpl session, Message msg, long timeout)
    throws JMSException
  {
    System.out.println("SEND: " + msg + " " + _subscriptionList);
    
    for (int i = 0; i < _subscriptionList.size(); i++) {
      _subscriptionList.get(i).send(session, msg, timeout);
    }
  }

  public String toString()
  {
    return "MemoryTopic[" + getTopicName() + "]";
  }
}

