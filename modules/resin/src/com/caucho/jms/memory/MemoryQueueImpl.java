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

package com.caucho.jms.memory;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.jms.connection.JmsSession;
import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.queue.AbstractMemoryQueue;
import com.caucho.jms.queue.QueueEntry;

/**
 * Implements a memory queue.
 */
public class MemoryQueueImpl extends AbstractMemoryQueue
{
  private static final Logger log
    = Logger.getLogger(MemoryQueueImpl.class.getName());
  
  /**
   * Returns the configuration URL.
   */
  @Override
  public String getUrl()
  {
    return "memory:name=" + getName();
  }

  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   */
  @Override
  public void send(JmsSession session,
		   MessageImpl msg,
		   int priority,
		   long expires)
  {
    addEntry(msg.getJMSMessageID(), -1, priority, expires, msg);

    notifyMessageAvailable();
  }
  
  /**
   * Polls the next message.
   */
  @Override
  public MessageImpl receive(boolean isAutoAck)
  {
    QueueEntry entry = receiveImpl(isAutoAck);

    if (entry != null) {
      try {
        MessageImpl msg = (MessageImpl) entry.getPayload();

        if (log.isLoggable(Level.FINER))
          log.finer(this + " receive " + msg + " auto-ack=" + isAutoAck);

        if (isAutoAck || msg == null) {
          synchronized (_queueLock) {
            removeEntry(entry);
          }
        }

        return msg;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return null;
  }  

  /**
   * Adds a message entry from startup.
   */
  QueueEntry addEntry(String msgId,
                          long leaseTimeout,
                          int priority,
                          long expire,
                          MessageImpl payload)
  {
    if (priority < 0)
      priority = 0;
    else if (_head.length <= priority)
      priority = _head.length;

    MemoryQueueEntry entry
      = new MemoryQueueEntry(msgId, leaseTimeout, priority, expire, payload);
    
    return addEntry(entry);
  }  

}

