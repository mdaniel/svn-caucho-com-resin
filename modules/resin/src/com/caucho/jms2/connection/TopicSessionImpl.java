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

package com.caucho.jms2.connection;

import javax.jms.*;

import com.caucho.jms2.queue.*;

/**
 * A sample topic session.  Lets the client create topics, browsers, etc.
 */
public class TopicSessionImpl extends SessionImpl implements TopicSession
{
  public TopicSessionImpl(ConnectionImpl conn,
			  boolean isTransacted, int ackMode)
    throws JMSException
  {
    super(conn, isTransacted, ackMode);
  }

  /**
   * Creates a TopicSender to send messages to a topic.
   *
   * @param topic the topic to send messages to.
   */
  public TopicPublisher createPublisher(Topic topic)
    throws JMSException
  {
    checkOpen();

    return new TopicPublisherImpl(this, (AbstractTopic) topic);
  }

  /**
   * Creates a subscriber to receive messages.
   *
   * @param topic the topic to receive messages from.
   */
  public TopicSubscriber createSubscriber(Topic topic)
    throws JMSException
  {
    checkOpen();
    
    return createSubscriber(topic, null, false);
  }

  /**
   * Creates a subscriber to receive messages.
   *
   * @param topic the topic to receive messages from.
   * @param messageSelector topic to restrict the messages.
   * @param noLocal if true, don't receive messages we've sent
   */
  public TopicSubscriber createSubscriber(Topic topic,
                                          String messageSelector,
                                          boolean noLocal)
    throws JMSException
  {
    checkOpen();

    if (topic == null)
      throw new InvalidDestinationException(L.l("topic is null.  Destination may not be null for Session.createSubscriber"));
    
    if (! (topic instanceof AbstractTopic))
      throw new InvalidDestinationException(L.l("'{0}' is an unknown destination.  The destination must be a Resin JMS Destination.",
						topic));

    AbstractTopic dest = (AbstractTopic) topic;

    TopicSubscriber subscriber
      = new TopicSubscriberImpl(this, dest, messageSelector, noLocal);
    
    // addConsumer((MessageConsumerImpl) consumer);

    return subscriber;
  }

  /**
   * Creates a QueueBrowser to browse messages in the queue.
   *
   * @param queue the queue to send messages to.
   */
  @Override
  public QueueBrowser createBrowser(Queue queue)
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("TopicSession: createBrowser() is invalid."));
  }

  /**
   * Creates a QueueBrowser to browse messages in the queue.
   *
   * @param queue the queue to send messages to.
   */
  public QueueBrowser createBrowser(Queue queue, String messageSelector)
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("TopicSession: createBrowser() is invalid."));
  }

  /**
   * Creates a new queue.
   */
  public Queue createQueue(String queueName)
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("TopicSession: createQueue() is invalid."));
  }

  /**
   * Creates a temporary queue.
   */
  public TemporaryQueue createTemporaryQueue()
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("TopicSession: createTemporaryQueue() is invalid."));
  }
}
