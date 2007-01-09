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

package com.caucho.jms.memory;

import com.caucho.jms.AbstractDestination;
import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.selector.Selector;
import com.caucho.jms.session.SessionImpl;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A basic queue.
 */
public class MemoryQueue extends AbstractDestination
  implements Queue {
  static final Logger log = Log.open(MemoryQueue.class);
  static final L10N L = new L10N(MemoryQueue.class);

  ArrayList<Item> _queue = new ArrayList<Item>();

  private String _queueName;
  private Selector _selector;

  private long _queueId;

  private int _consumerId;

  public MemoryQueue()
  {
  }

  /**
   * Returns the queue's name.
   */
  public String getQueueName()
  {
    return _queueName;
  }

  /**
   * Sets the queue's name.
   */
  public void setQueueName(String name)
  {
    _queueName = name;
  }

  public void setSelector(Selector selector)
  {
    _selector = selector;
  }

  public Selector getSelector()
  {
    return _selector;
  }

  /**
   * Generates the next queue id.
   */
  public long generateQueueId()
  {
    return ++_queueId;
  }

  /**
   * Generates the next consumer id.
   */
  public int generateConsumerId()
  {
    return ++_consumerId;
  }

  public void send(Message message)
    throws JMSException
  {
    if (_selector != null && ! _selector.isMatch(message))
      return;

    long sequenceId = nextConsumerSequenceId();

    if (log.isLoggable(Level.FINE))
      log.fine("MemoryQueue[" + _queueName + "] send " + sequenceId);

    synchronized (_queue) {
      _queue.add(new Item(generateQueueId(), (MessageImpl) message));
      _queue.notify();
    }

    messageAvailable();
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
    return new MemoryQueueConsumer(session, selector, this);
  }

  /**
   * Removes the first message matching the selector.
   */
  MessageImpl receive(Selector selector, long consumerId, boolean autoAck)
    throws JMSException
  {
    synchronized (_queue) {
      int i;
      int size = _queue.size();

      for (i = 0; i < size; i++) {
	Item item = _queue.get(i);

	if (item.getConsumerId() >= 0)
	  continue;
	
	MessageImpl message = item.getMessage();

	if (selector == null || selector.isMatch(message)) {
	  message.setJMSRedelivered(item.getDelivered());

	  if (autoAck)
	    _queue.remove(i);
	  else
	    item.setConsumerId(consumerId);

	  return message;
	}
	else {
	  _queue.remove(i);
	  i--;
	  size = _queue.size();
	}
      }
    }

    return null;
  }

  /**
   * Rolls back messages for the consumer.
   */
  void rollback(long consumerId)
    throws JMSException
  {
    synchronized (_queue) {
      for (int i = _queue.size() -1; i >= 0; i--) {
	Item item = _queue.get(i);

	if (item.getConsumerId() == consumerId)
	  item.setConsumerId(-1);
      }
    }
  }

  /**
   * Acknowledge messages for the consumer.
   */
  void acknowledge(long consumerId, long messageId)
    throws JMSException
  {
    synchronized (_queue) {
      for (int i = _queue.size() -1; i >= 0; i--) {
	Item item = _queue.get(i);

	if (item.getConsumerId() == consumerId)
	  _queue.remove(i);
      }
    }
  }

  /**
   * Returns a browser.
   */
  public MemoryQueueBrowser createBrowser(SessionImpl session,
					  String selector)
    throws JMSException
  {
    return new MemoryQueueBrowser(session, this, selector);
  }

  /**
   * Returns an enumeration of the matching messages.
   */
  public Enumeration getEnumeration(Selector selector)
  {
    return new BrowserEnumeration(this, selector);
  }

  /**
   * Removes the first message matching the selector.
   */
  private boolean hasMessage(Selector selector)
    throws JMSException
  {
    synchronized (_queue) {
      int i;
      int size = _queue.size();

      for (i = 0; i < size; i++) {
	Item item = _queue.get(i);

	if (item.getConsumerId() >= 0)
	  continue;

	Message message = item.getMessage();

	if (selector == null || selector.isMatch(message))
	  return true;
      }
    }

    return false;
  }

  /**
   * Returns the id of the first message matching the selector.
   */
  private long nextId(Selector selector, long id)
    throws JMSException
  {
    synchronized (_queue) {
      int i;
      int size = _queue.size();

      for (i = 0; i < size; i++) {
	Item item = _queue.get(i);
	
	if (item.getConsumerId() >= 0)
	  continue;

	else if (item.getId() < id)
	  continue;

	Message message = item.getMessage();

	if (selector == null || selector.isMatch(message))
	  return item.getId();
      }
    }

    return Long.MAX_VALUE;
  }

  /**
   * Returns the id of the first message matching the selector.
   */
  private Message nextValue(Selector selector, long id)
    throws JMSException
  {
    synchronized (_queue) {
      int i;
      int size = _queue.size();

      for (i = 0; i < size; i++) {
	Item item = _queue.get(i);

	if (item.getConsumerId() >= 0)
	  continue;

	else if (item.getId() < id)
	  continue;

	Message message = item.getMessage();

	if (selector == null || selector.isMatch(message))
	  return message;
      }
    }

    return null;
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "MemoryQueue[" + _queueName + "]";
  }

  static class BrowserEnumeration implements Enumeration {
    private MemoryQueue _queue;
    private Selector _selector;
    private long _id = -1;

    BrowserEnumeration(MemoryQueue queue, Selector selector)
    {
      _queue = queue;
      _selector = selector;
    }

    public boolean hasMoreElements()
    {
      try {
	if (_id < 0)
	  _id = _queue.nextId(_selector, _id);
	
	return (_id < Long.MAX_VALUE);
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }

    public Object nextElement()
    {
      try {
	// ejb/6110
	if (_id < 0)
	  _id = _queue.nextId(_selector, _id);

	Object value = _queue.nextValue(_selector, _id);

	_id = _queue.nextId(_selector, _id + 1);
	
	return value;
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }

  static class Item {
    private MessageImpl _msg;
    private long _id;
    private long _consumerId = -1;
    private boolean _delivered;

    Item(long id, MessageImpl msg)
    {
      _id = id;
      
      _msg = msg;
    }

    MessageImpl getMessage()
    {
      return _msg;
    }

    long getId()
    {
      return _id;
    }

    long getConsumerId()
    {
      return _consumerId;
    }

    void setConsumerId(long consumerId)
    {
      _consumerId = consumerId;
      _delivered = true;
    }

    boolean getDelivered()
    {
      return _delivered;
    }
  }
}

