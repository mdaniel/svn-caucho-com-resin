/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.jms.session;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Destination;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.JMSException;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;

import com.caucho.log.Log;

import com.caucho.jms.AbstractDestination;

import com.caucho.jms.message.MessageImpl;

import com.caucho.jms.selector.Selector;
import com.caucho.jms.selector.SelectorParser;

/**
 * A basic message consumer.
 */
public class MessageConsumerImpl
  implements MessageConsumer, MessageAvailableListener {
  static final Logger log = Log.open(MessageConsumerImpl.class);
  static final L10N L = new L10N(MessageConsumerImpl.class);

  private final Object _consumerLock = new Object();
  
  protected SessionImpl _session;
  private AbstractDestination _queue;
  private MessageListener _messageListener;
  private String _messageSelector;
  protected Selector _selector;
  private boolean _noLocal;
  
  private volatile boolean _isClosed;

  protected MessageConsumerImpl(SessionImpl session,
                                String messageSelector,
				AbstractDestination queue,
                                boolean noLocal)
    throws JMSException
  {
    _session = session;
    _queue = queue;
    _messageSelector = messageSelector;
    if (_messageSelector != null) {
      SelectorParser parser = new SelectorParser();
      _selector = parser.parse(messageSelector);
    }
    _noLocal = noLocal;

    _queue.addListener(this);
  }

  /**
   * Returns true if local messages are not sent.
   */
  public boolean getNoLocal()
  {
    return _noLocal;
  }
  
  /**
   * Returns the message listener
   */
  public MessageListener getMessageListener()
  {
    return _messageListener;
  }

  /**
   * Sets the message listener
   */
  public void setMessageListener(MessageListener listener)
  {
    _messageListener = listener;
    _session.setAsynchronous();
  }

  /**
   * Returns the message consumer's selector.
   */
  public String getMessageSelector()
  {
    return _messageSelector;
  }

  /**
   * Returns the parsed selector.
   */
  public Selector getSelector()
  {
    return _selector;
  }

  /**
   * Returns true if active
   */
  public boolean isActive()
  {
    return _session.isActive() && ! _isClosed;
  }

  /**
   * Returns true if closed
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Receives the next message, blocking until a message is available.
   */
  public Message receive()
    throws JMSException
  {
    return receive(Long.MAX_VALUE);
  }

  /**
   * Called to synchronously receive a message.
   */
  public Message receive(long timeout)
    throws JMSException
  {
    _session.checkOpen();
    
    if (Long.MAX_VALUE / 2 < timeout || timeout < 0)
      timeout = Long.MAX_VALUE / 2;
    
    long now = Alarm.getCurrentTime();
    long expireTime = Alarm.getCurrentTime() + timeout;
    
    // 4.4.1 user's reponsibility
    // checkThread();

    while (! isClosed()) {
      Message msg = receiveNoWait();
      if (msg != null)
	return msg;
      
      long delta = expireTime - Alarm.getCurrentTime();

      if (delta <= 0 || _isClosed || Alarm.isTest())
	return null;

      synchronized (_consumerLock) {
	try {
	  _consumerLock.wait(delta);
	} catch (Throwable e) {
	}
      }
    }

    return null;
  }

  /**
   * Receives a message from the queue.
   */
  public Message receiveNoWait()
    throws JMSException
  {
    if (_isClosed)
      throw new javax.jms.IllegalStateException(L.l("can't receive when consumer is closed"));
    
    if (! _session.isActive())
      return null;

    MessageImpl msg = receiveImpl();

    if (msg == null)
      return null;

    switch (_session.getAcknowledgeMode()) {
    case Session.CLIENT_ACKNOWLEDGE:
      msg.setSession(_session);
      break;
	
    case Session.AUTO_ACKNOWLEDGE:
    case Session.DUPS_OK_ACKNOWLEDGE:
      acknowledge();
      break;

    default:
      // transacted
      break;
    }

    return msg;
  }

  /**
   * Receives the next message, if one is available
   */
  protected MessageImpl receiveImpl()
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * acknowledge any received messages.
   */
  public void acknowledge()
    throws JMSException
  {
  }

  /**
   * rollback any received messages.
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
    _isClosed = true;
    _queue.removeListener(this);
    // XXX: remove session?
    // _session.removeListener(this);
  }

  /**
   * Called when a new message is available.
   */
  public void messageAvailable()
  {
    _session.notifyListener();
    synchronized (_consumerLock) {
      try {
	_consumerLock.notify();
      } catch (Throwable e) {
      }
    }
  }
}

