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

package com.caucho.jms.connection;

import com.caucho.jms2.message.*;
import com.caucho.jms.queue.*;
import com.caucho.jms.selector.Selector;
import com.caucho.jms.selector.SelectorParser;
import com.caucho.log.Log;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.AlarmListener;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A basic message consumer.
 */
public class MessageConsumerImpl
  implements MessageConsumer, MessageAvailableListener
{
  static final Logger log
    = Logger.getLogger(MessageConsumerImpl.class.getName());
  static final L10N L = new L10N(MessageConsumerImpl.class);

  private final Object _consumerLock = new Object();
  
  protected final JmsSession _session;
  private AbstractQueue _queue;
  private MessageListener _messageListener;
  private String _messageSelector;
  protected Selector _selector;
  private boolean _noLocal;

  private volatile boolean _isClosed;
  private Alarm _pollAlarm;

  MessageConsumerImpl(JmsSession session,
                      AbstractQueue queue,
                      String messageSelector,
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

    // XXX:
    // _queue.addListener(this);

    _queue.addConsumer(this);
  }

  /**
   * Returns the destination
   */
  protected AbstractDestination getDestination()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getDestination(): MessageConsumer is closed."));
    
    return _queue;
  }

  /**
   * Returns true if local messages are not sent.
   */
  public boolean getNoLocal()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getNoLocal(): MessageConsumer is closed."));
    
    return _noLocal;
  }
  
  /**
   * Returns the message listener
   */
  public MessageListener getMessageListener()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getNoLocal(): MessageConsumer is closed."));
    
    return _messageListener;
  }

  /**
   * Sets the message listener
   */
  public void setMessageListener(MessageListener listener)
    throws JMSException
  {
    setMessageListener(listener, -1);
  }

  /**
   * Sets the message listener with a poll interval
   */
  public void setMessageListener(MessageListener listener, long pollInterval)
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("setMessageListener(): MessageConsumer is closed."));

    if (_messageListener != null)
      _queue.removeListener(_messageListener);

    _messageListener = listener;
    _queue.addListener(listener);
  }

  /**
   * Returns the message consumer's selector.
   */
  public String getMessageSelector()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getMessageSelector(): MessageConsumer is closed."));
    
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
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("isActive(): MessageConsumer is closed."));
    
    return _session.isActive() && ! _isClosed;
  }

  /**
   * Returns true if closed
   */
  public boolean isClosed()
  {
    return _isClosed || _session.isClosed();
  }

  /**
   * Receives the next message, blocking until a message is available.
   */
  public Message receive()
    throws JMSException
  {
    return receive(Long.MAX_VALUE / 2);
  }

  /**
   * Receives a message from the queue.
   */
  public Message receiveNoWait()
    throws JMSException
  {
    return receive(0);
  }

  /**
   * Receives a message from the queue.
   */
  public Message receive(long timeout)
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("receiveNoWait(): MessageConsumer is closed."));
    
    if (Long.MAX_VALUE / 2 < timeout || timeout < 0)
      timeout = Long.MAX_VALUE / 2;
    
    long now = Alarm.getCurrentTime();
    long expireTime = timeout > 0 ? now + timeout : 0;
    
    if (! _session.isActive())
      return null;

    while (true) {
      MessageImpl msg = _queue.receive(expireTime);

      if (msg == null)
        return null;
    
      else if (_selector != null && ! _selector.isMatch(msg)) {
        msg.acknowledge();
        continue;
      }

      else {
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
    }
  }

  /**
   * Receives the next message, if one is available
   */
  protected MessageImpl receiveImpl(long expires)
    throws JMSException
  {
    return _queue.receive(expires);
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
    // XXX:
    // _queue.removeListener(this);
    // XXX: remove session?
    // _session.removeListener(this);

    _queue.removeConsumer(this);

    if (_messageListener != null)
      _queue.removeListener(_messageListener);
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

  private class PollAlarmListener implements AlarmListener {
    private final long _pollInterval;

    public PollAlarmListener(long pollInterval)
    {
      _pollInterval = pollInterval;
    }

    public void handleAlarm(Alarm alarm)
    {
      if (isClosed())
        return;

      MessageListener messageListener = _messageListener;

      if (messageListener == null)
        return;

      ClassLoader classLoader = _session.getClassLoader();

      try {
        Message msg = receiveNoWait();

        if (msg != null) {
          Thread thread = Thread.currentThread();

          ClassLoader oldLoader = thread.getContextClassLoader();

          try {
            thread.setContextClassLoader(classLoader);
            messageListener.onMessage(msg);
          }
          finally {
            thread.setContextClassLoader(oldLoader);
          }
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      finally {
        if (!isClosed())
          _pollAlarm.queue(_pollInterval);
      }
    }
  }

  public String toString()
  {
    return "MessageConsumerImpl[" + _queue + "]";
  }
}

