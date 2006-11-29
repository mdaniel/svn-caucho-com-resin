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

package com.caucho.jms.amq;

import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.session.SessionImpl;
import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JDBC queue consumer.
 */
public class AmqQueueConsumer extends AmqMessageConsumer
  implements QueueReceiver
{
  private static final Logger log
    = Logger.getLogger(AmqQueueConsumer.class.getName());
  private static final L10N L = new L10N(AmqQueueConsumer.class);
  
  private AmqQueue _queue;

  public AmqQueueConsumer(SessionImpl session, String messageSelector,
			  AmqQueue queue)
    throws JMSException
  {
    super(session, messageSelector, queue, false);
    
    _queue = queue;

    if (log.isLoggable(Level.FINE))
      log.fine("AmqQueueConsumer[" + queue + "] created");
  }

  /**
   * Returns the queue.
   */
  public Queue getQueue()
  {
    return _queue;
  }

  /**
   * Receives a message from the queue.
   */
  protected MessageImpl receiveImpl()
    throws JMSException
  {
    return null;
  }

  /**
   * Acknowledges all received messages from the session.
   */
  public void acknowledge()
    throws JMSException
  {
  }

  /**
   * Rollback all received messages from the session.
   */
  public void rollback()
    throws JMSException
  {
  }

  /**
   * Closes the consumer.
   */
  public void close()
    throws JMSException
  {
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "AmqQueueConsumer[" + _queue + "]";
  }
}

