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

import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.session.MessageConsumerImpl;
import com.caucho.jms.session.SessionImpl;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import java.util.logging.Logger;

/**
 * Represents a memory queue consumer.
 */
public class MemoryQueueConsumer extends MessageConsumerImpl
  implements QueueReceiver
{
  static final Logger log = Log.open(MemoryQueueConsumer.class);
  static final L10N L = new L10N(MemoryQueueConsumer.class);

  private MemoryQueue _queue;

  private int _consumerId;

  private boolean _autoAck;

  public MemoryQueueConsumer(SessionImpl session, String messageSelector,
			     MemoryQueue queue)
    throws JMSException
  {
    super(session, messageSelector, queue, false);

    if (queue == null)
      throw new NullPointerException();
    
    _queue = queue;

    _consumerId = queue.generateConsumerId();

    if (session.getAcknowledgeMode() == session.AUTO_ACKNOWLEDGE ||
	session.getAcknowledgeMode() == session.DUPS_OK_ACKNOWLEDGE)
      _autoAck = true;
  }

  /**
   * Returns the queue.
   */
  public Queue getQueue()
    throws JMSException
  {
    return (Queue) getDestination();
  }

  /**
   * Receives a message from the queue.
   */
  protected MessageImpl receiveImpl()
    throws JMSException
  {
    if (isClosed())
      throw new javax.jms.IllegalStateException(L.l("QueueConsumer: receive called after close."));
    
    // purgeExpiredConsumers();
    // _queue.purgeExpiredMessages();

    return _queue.receive(_selector, _consumerId, _autoAck);
  }

  /**
   * Acknowledges all received messages from the session.
   */
  public void acknowledge()
    throws JMSException
  {
    if (_autoAck)
      return;

    _queue.acknowledge(_consumerId, Long.MAX_VALUE);
  }

  /**
   * Rollback all received messages from the session.
   */
  public void rollback()
    throws JMSException
  {
    if (_autoAck)
      return;

    _queue.rollback(_consumerId);
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "MemoryQueueConsumer[" + _queue + "," + _consumerId + "]";
  }
}

