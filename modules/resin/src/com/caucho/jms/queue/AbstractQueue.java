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

package com.caucho.jms.queue;

import java.util.*;
import java.util.logging.*;

import javax.annotation.*;
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

  private QueueAdmin _admin;

  private ArrayList<MessageAvailableListener> _consumerList
    = new ArrayList<MessageAvailableListener>();

  private int _roundRobin;

  // stats
  private long _listenerFailCount;
  private long _listenerFailLastTime;

  protected AbstractQueue()
  {
  }

  public void setQueueName(String name)
  {
    setName(name);
  }

  //
  // JMX statistics
  //

  /**
   * Returns the number of active message consumers
   */
  public int getConsumerCount()
  {
    return _consumerList.size();
  }

  /**
   * Returns the queue size
   */
  public int getQueueSize()
  {
    return -1;
  }

  /**
   * Returns the number of listener failures.
   */
  public long getListenerFailCountTotal()
  {
    return _listenerFailCount;
  }

  /**
   * Returns the number of listener failures.
   */
  public long getListenerFailLastTime()
  {
    return _listenerFailLastTime;
  }

  protected void init()
  {
  }

  @PostConstruct
  public void postConstruct()
  {
    init();

    _admin = new QueueAdmin(this);
    _admin.register();
  }
  
  /**
   * Adds a MessageAvailableListener to receive notifications for new
   * messages.  The listener will spawn or wake a thread to process
   * the message.  The listener MUST NOT use the event thread to
   * process the message.
   * 
   * @param listener notification listener
   */
  @Override
  public void addMessageAvailableListener(MessageAvailableListener listener)
  {
    synchronized (_consumerList) {
      if (! _consumerList.contains(listener))
	_consumerList.add(listener);

      startPoll();
    }
  }
  
  @Override
  public void removeMessageAvailableListener(MessageAvailableListener consumer)
  {
    synchronized (_consumerList) {
      _consumerList.remove(consumer);

      // force a poll to avoid missing messages
      for (int i = 0; i < _consumerList.size(); i++) {
	_consumerList.get(i).notifyMessageAvailable();
      }

      if (_consumerList.size() == 0)
        stopPoll();
    }
  }

  protected void notifyMessageAvailable()
  {
    synchronized (_consumerList) {
      if (_consumerList.size() > 0) {
	MessageAvailableListener consumer;
	int count = _consumerList.size();

	// notify until one of the consumers signals readiness to read
	do {
	  int roundRobin = _roundRobin++ % _consumerList.size();
	  
	  consumer = _consumerList.get(roundRobin);
	} while (! consumer.notifyMessageAvailable() && count-- > 0);
      }
    }
  }

  public ArrayList<MessageImpl> getBrowserList()
  {
    return new ArrayList<MessageImpl>();
  }

  protected void startPoll()
  {
  }

  protected void stopPoll()
  {
  }

  /**
   * Called when a listener throws an excepton
   */
  public void addListenerException(Exception e)
  {
    synchronized (this) {
      _listenerFailCount++;
      _listenerFailLastTime = Alarm.getCurrentTime();
    }
  }

  @PreDestroy
  @Override
  public void close()
  {
    stopPoll();
    
    super.close();
  }
}

