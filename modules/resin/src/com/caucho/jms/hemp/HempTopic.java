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

package com.caucho.jms.hemp;

import com.caucho.bam.ActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.Broker;
import com.caucho.bam.SimpleActor;
import java.util.ArrayList;
import java.util.logging.*;

import java.io.Serializable;
import javax.annotation.*;
import javax.inject.Inject;
import javax.jms.*;

import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.jms.memory.*;
import com.caucho.jms.message.*;
import com.caucho.jms.queue.*;
import com.caucho.jms.connection.*;
import com.caucho.util.*;

/**
 * Implements an hemp topic.
 */
public class HempTopic extends AbstractTopic
{
  private static final L10N L = new L10N(HempTopic.class);
  
  private static final Logger log
    = Logger.getLogger(HempTopic.class.getName());

  private ArrayList<AbstractQueue> _subscriptionList
    = new ArrayList<AbstractQueue>();

  private @Inject Broker _broker;
  private ActorStream _brokerStream;

  private TopicResource _resource = new TopicResource();

  private int _id;
  private boolean _isInit;

  //
  // JMX configuration
  //

  /**
   * Returns the configuration URL.
   */
  @Override
  public String getUrl()
  {
    return "xmpp:name=" + getName();
  }

  @PostConstruct
  @Override
  public void init()
  {
    super.init();

    String jid = getName();

    /*
    if (jid.indexOf('/') < 0 && jid.indexOf('@') < 0)
      jid = getName() + "@" + _broker.getDomain();
    */

    if (! _isInit) {
      _isInit = true;
      _resource.setJid(jid);
      _broker.addActor(_resource);
    }
  }

  @Override
  public AbstractQueue createSubscriber(JmsSession session,
                                        String name,
                                        boolean noLocal)
  {
    MemoryQueue queue;

    if (name != null) {
      queue = new MemorySubscriberQueue(session, noLocal);
      queue.setName(getName() + ":sub-" + name);

      _subscriptionList.add(queue);
    }
    else {
      queue = new MemorySubscriberQueue(session, noLocal);
      queue.setName(getName() + ":sub-" + _id++);

      _subscriptionList.add(queue);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " create-subscriber(" + queue + ")");

    return queue;
  }

  @Override
  public void closeSubscriber(AbstractQueue queue)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close-subscriber(" + queue + ")");
    
    _subscriptionList.remove(queue);
  }

  public void sendMessage(String to, String from, Serializable value)
  {
    try {
      javax.jms.Message msg = null;

      if (value instanceof javax.jms.Message)
	msg = (javax.jms.Message) value;
      else {
	msg = new ObjectMessageImpl(value);
      }

      synchronized (_subscriptionList) {
	for (int i = 0; i < _subscriptionList.size(); i++) {
	  MemorySubscriberQueue queue
	    = (MemorySubscriberQueue) _subscriptionList.get(i);

	  queue.offer(msg);
	}
      }
    } catch (JMSException e) {
      throw new RuntimeException(e);
    }
  }

  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       ActorError error)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " sendMessageError to=" + to + " from=" + from +
		" error=" + error);
  }

  @Override
  public void send(String msgId,
		   Serializable payload,
		   int priority,
		   long timeout)
  {
    // _xmppNode.send(session, msg, timeout);
  }

  class TopicResource extends SimpleActor {
    public void setJid(String jid)
    {
      super.setJid(jid);
    }
    
    public void message(String to, String from, Serializable msg)
    {
      HempTopic.this.sendMessage(to, from, msg);
    }
  }
}

