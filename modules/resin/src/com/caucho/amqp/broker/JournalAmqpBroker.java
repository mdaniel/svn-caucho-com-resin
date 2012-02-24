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

package com.caucho.amqp.broker;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Singleton;

import com.caucho.config.ConfigException;
import com.caucho.mqueue.queue.MQJournalQueue;
import com.caucho.mqueue.queue.MQJournalQueuePublisher;
import com.caucho.mqueue.queue.MQJournalQueueSubscriber;
import com.caucho.mqueue.queue.SubscriberProcessor;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Simple stomp broker.
 */
@Startup
@Singleton
public class JournalAmqpBroker extends AbstractAmqpBroker
{
  private static final L10N L = new L10N(JournalAmqpBroker.class);
  
  private Path _path;
  private MQJournalQueue _queue;
  
  public void setPath(Path path)
  {
    _path = path;
  }
  
  @PostConstruct
  public void init()
  {
    if (_path == null)
      throw new ConfigException(L.l("'path' is required for a journal broker."));
    
    _queue = new MQJournalQueue(_path);
    
    registerSelf();
  }
  
  @Override
  public AmqpBrokerSender createSender(String name)
  {
    MQJournalQueuePublisher jPublisher = _queue.createPublisher();
    
    return new AmqpJournalSender(jPublisher);
  }
  
  @Override
  public AmqpBrokerReceiver createReceiver(String name,
                                              AmqpMessageListener listener)
  {
    SubscriberProcessor processor = new AmqpReceiverProcessor(listener);
    System.out.println("SUB: " + name);
    MQJournalQueueSubscriber jSubscriber = _queue.createSubscriber(processor);
    
    return new AmqpJournalReceiver(jSubscriber);
  }
}
