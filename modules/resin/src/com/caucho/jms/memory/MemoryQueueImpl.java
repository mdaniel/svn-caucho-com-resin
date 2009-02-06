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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms.connection.*;
import com.caucho.jms.message.*;
import com.caucho.jms.queue.*;

/**
 * Implements a memory queue.
 */
public class MemoryQueueImpl extends AbstractQueue
{
  private static final Logger log
    = Logger.getLogger(MemoryQueueImpl.class.getName());

  private PriorityQueue<MessageImpl> _queueList
    = new PriorityQueue<MessageImpl>(64, new JmsPriorityComparator());
  
  // messages waiting for an ack
  private ArrayList<MessageImpl> _readList = new ArrayList<MessageImpl>();

  private long _sequence;

  //
  // JMX configuration
  //

  /**
   * Returns the configuration URL.
   */
  @Override
  public String getUrl()
  {
    return "memory:name=" + getName();
  }

  //
  // JMX statistics
  //

  /**
   * Returns the queue size
   */
  @Override
  public int getQueueSize()
  {
    synchronized (_queueList) {
      return _queueList.size();
    }
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
    synchronized (_queueList) {
      msg.setSequence(_sequence++);
      
      _queueList.add(msg);
    }

    notifyMessageAvailable();
  }

  /**
   * Returns true if a message is available.
   */
  @Override
  public boolean hasMessage()
  {
    return _queueList.size() > 0;
  }
  
  /**
   * Polls the next message from the store.
   */
  @Override
  public MessageImpl receive(boolean isAutoAck)
  {
    synchronized (_queueList) {
      MessageImpl msg = _queueList.poll();

      if (msg == null)
	return null;
      
      if (log.isLoggable(Level.FINE))
	log.fine(this + " receive " + msg + (isAutoAck ? " (auto-ack)" : ""));
      
      if (isAutoAck) {
	return msg;
      }
      else {
	_readList.add(msg);
	return msg;
      }
    }
  }

  @Override
  public ArrayList<MessageImpl> getBrowserList()
  {
    synchronized (_queueList) {
      return new ArrayList<MessageImpl>(_queueList);
    }
  }

  /**
   * Acknowledges the receipt of a message
   */
  @Override
  public void acknowledge(String msgId)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " acknowledge " + msgId);
    
    synchronized (_queueList) {
      for (int i = _readList.size() - 1; i >= 0; i--) {
        MessageImpl msg = _readList.get(i);

        if (msg.getJMSMessageID().equals(msgId))
          _readList.remove(i);
      }
    }
  }

  /**
   * Rolls back the receipt of a message
   */
  @Override
  public void rollback(String msgId)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " rollback " + msgId);
    
    synchronized (_queueList) {
      for (int i = _readList.size() - 1; i >= 0; i--) {
        MessageImpl msg = _readList.get(i);

        if (msg.getJMSMessageID().equals(msgId)) {
          _readList.remove(i);
          msg.setJMSRedelivered(true);
          _queueList.add(msg);
	  notifyMessageAvailable();
        }
      }
    }
  }

  static class JmsPriorityComparator implements Comparator<MessageImpl> {
    /**
     * Compares the priority.
     */
    public int compare(MessageImpl msg1, MessageImpl msg2)
    {
      try {
	int cmp = msg2.getJMSPriority() - msg1.getJMSPriority();

	if (cmp != 0)
	  return cmp;

	long seqCmp = (msg1.getSequence() - msg2.getSequence());

	if (seqCmp < 0)
	  return -1;
	else if (seqCmp > 0)
	  return 1;
	else
	  return 0;
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }
}

