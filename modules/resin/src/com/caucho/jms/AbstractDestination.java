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

package com.caucho.jms;

import java.lang.ref.SoftReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;

import java.util.logging.Logger;

import java.io.Serializable;

import javax.jms.*;

import com.caucho.util.*;

import com.caucho.log.Log;

import com.caucho.loader.Environment;

import com.caucho.jms.selector.Selector;

import com.caucho.jms.message.*;

import com.caucho.jms.session.*;

import com.caucho.services.message.MessageSender;
import com.caucho.services.message.MessageServiceException;

/**
 * An abstract destination, including the needed send/receive.
 */
abstract public class AbstractDestination implements Destination, MessageSender
{
  protected static Logger log
    = Logger.getLogger(AbstractDestination.class.getName());

  private String _idPrefix;
  private long _idCount;

  private volatile long _consumerSequenceId;
  
  private ArrayList<SoftReference<MessageAvailableListener>> _listenerRefs =
    new ArrayList<SoftReference<MessageAvailableListener>>();

  /**
   * Creates the destination.
   */
  protected AbstractDestination()
  {
    CharBuffer cb = new CharBuffer();

    cb.append("ID:");
    Object serverId = Environment.getAttribute("caucho.server-id");
    if (serverId != null)
      cb.append(serverId);
    Base64.encode(cb, RandomUtil.getRandomLong());
    Base64.encode(cb, Alarm.getCurrentTime());

    _idPrefix = cb.toString();
  }

  /**
   * Generates a message id.
   */
  public synchronized String generateMessageID()
  {
    return _idPrefix + _idCount++;
  }

  /**
   * Returns the current received sequence id.
   */
  public long getConsumerSequenceId()
  {
    return _consumerSequenceId;
  }

  /**
   * Returns the current received sequence id.
   */
  protected synchronized long nextConsumerSequenceId()
  {
    return ++_consumerSequenceId;
  }

  public MessageProducer createProducer(SessionImpl session)
  {
    if (this instanceof Queue)
      return new QueueSenderImpl(session, (Queue) this);
    else if (this instanceof Topic)
      return new TopicPublisherImpl(session, (Topic) this);
    else
      return new MessageProducerImpl(session, (Destination) this);
  }
  
  /**
   * Send a message to the destination.
   *
   * @param message the message to add.
   */
  public void send(Message message)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds a message available listener.
   */
  public void addListener(MessageAvailableListener listener)
  {
    synchronized (_listenerRefs) {
      _listenerRefs.add(new SoftReference<MessageAvailableListener>(listener));
    }

    listener.messageAvailable();
  }

  /**
   * Removes a message available listener.
   */
  public void removeListener(MessageAvailableListener listener)
  {
    synchronized (_listenerRefs) {
      for (int i = _listenerRefs.size() - 1; i >= 0; i--) {
	SoftReference<MessageAvailableListener> ref = _listenerRefs.get(i);

	MessageAvailableListener oldListener = ref.get();
	
	if (oldListener == null)
	  _listenerRefs.remove(i);
	else if (oldListener == listener) {
	  _listenerRefs.remove(i);
	  return;
	}
      }
    }
  }

  /**
   * Called when a new message is available.
   */
  protected void messageAvailable()
  {
    synchronized (_listenerRefs) {
      for (int i = _listenerRefs.size() - 1; i >= 0; i--) {
	SoftReference<MessageAvailableListener> ref = _listenerRefs.get(i);

	MessageAvailableListener listener = ref.get();

	if (listener != null) {
	  listener.messageAvailable();
	}
	else
	  _listenerRefs.remove(i);
      }
    }
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
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Creates a queue browser
   *
   * @param session the owning session
   * @param selector the browser's selector
   */
  public QueueBrowser createBrowser(SessionImpl session,
				    String selector)
    throws JMSException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Creates a subscriber.
   *
   * @param session the owning session
   * @param selector the consumer's selector
   * @param noLocal true if pub/sub should not send local requests
   * @param name the durable subscriber's name
   */
  public TopicSubscriber createDurableSubscriber(SessionImpl session,
						 String selector,
						 boolean noLocal,
						 String name)
    throws JMSException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Lists the available messages.
   */
  public Enumeration getEnumeration(Selector selector)
    throws JMSException
  {
    return NullEnumeration.create();
  }

  // com.caucho.services.message API
  
  /**
   * Sends a message to the destination.
   */
  public void send(HashMap headers, Object data)
    throws MessageServiceException
  {
    try {
      Message message;
      
      if (data instanceof String) {
        TextMessage msg = new TextMessageImpl();
        msg.setText((String) data);
        
        message = msg;
      }
      else if (data instanceof Serializable) {
        ObjectMessage msg = new ObjectMessageImpl();
        msg.setObject((Serializable) data);
        message = msg;
      }
      else
        throw new MessageServiceException("not a serializable object: " + data);

      if (headers != null) {
        Iterator<String> iter = headers.keySet().iterator();
        while (iter.hasNext()) {
          String key = iter.next();
          Object value = headers.get(key);

          message.setObjectProperty(key, value);
        }
      }

      send(message);
    } catch (MessageServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new MessageServiceException(e);
    }
  }
}

