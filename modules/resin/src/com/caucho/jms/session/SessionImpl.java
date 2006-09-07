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

package com.caucho.jms.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.BytesMessage;
import javax.jms.ObjectMessage;
import javax.jms.MapMessage;
import javax.jms.StreamMessage;
import javax.jms.MessageListener;
import javax.jms.Destination;
import javax.jms.Topic;
import javax.jms.TemporaryTopic;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueBrowser;
import javax.jms.TopicSubscriber;
import javax.jms.Session;
import javax.jms.JMSException;
import javax.jms.IllegalStateException;

import com.caucho.util.L10N;
import com.caucho.util.ThreadTask;
import com.caucho.util.ThreadPool;
import com.caucho.util.Alarm;

import com.caucho.log.Log;

import com.caucho.jms.AbstractDestination;
import com.caucho.jms.AbstractTopic;
import com.caucho.jms.JMSExceptionWrapper;

import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.message.TextMessageImpl;
import com.caucho.jms.message.BytesMessageImpl;
import com.caucho.jms.message.ObjectMessageImpl;
import com.caucho.jms.message.MapMessageImpl;
import com.caucho.jms.message.StreamMessageImpl;

import com.caucho.jms.selector.Selector;

import com.caucho.jms.memory.MemoryQueue;
import com.caucho.jms.memory.MemoryTopic;

/**
 * Manages the JMS session.
 */
public class SessionImpl implements Session, ThreadTask {
  protected static final Logger log = Log.open(SessionImpl.class);
  protected static final L10N L = new L10N(SessionImpl.class);

  private static final long SHUTDOWN_WAIT_TIME = 10000;

  private boolean _isTransacted;
  private int _acknowledgeMode;

  private ClassLoader _classLoader;
  
  private ConnectionImpl _connection;
  private ArrayList<MessageConsumerImpl> _consumers =
    new ArrayList<MessageConsumerImpl>();
  private MessageListener _messageListener;
  private boolean _isAsynchronous;

  // 4.4.1 - client's responsibility
  private Thread _thread;

  // transacted messages
  private ArrayList<TransactedMessage> _transactedMessages;

  private volatile boolean _isRunning;
  private volatile boolean _isClosed;
  private volatile boolean _hasMessage;

  public SessionImpl(ConnectionImpl connection,
		     boolean isTransacted, int ackMode)
    throws JMSException
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
    
    _connection = connection;
    _isTransacted = isTransacted;
    if (isTransacted)
      _acknowledgeMode = SESSION_TRANSACTED;
    else {
      switch (ackMode) {
      case CLIENT_ACKNOWLEDGE:
      case DUPS_OK_ACKNOWLEDGE:
      case AUTO_ACKNOWLEDGE:
	_acknowledgeMode = ackMode;
	break;
      default:
	throw new JMSException(L.l("{0} is an illegal acknowledge mode",
				   ackMode));
      }
    }
    
    _connection.addSession(this);
  }

  /**
   * Returns the connection.
   */
  ConnectionImpl getConnection()
  {
    return _connection;
  }

  /**
   * Returns the connection's clientID
   */
  public String getClientID()
    throws JMSException
  {
    return _connection.getClientID();
  }

  /**
   * Returns true if the connection is active.
   */
  public boolean isActive()
  {
    return ! _isClosed && _connection.isActive();
  }

  /**
   * Returns true if the connection is active.
   */
  boolean isStopping()
  {
    return _connection.isStopping();
  }

  /**
   * Returns true if the session is in a transaction.
   */
  public boolean getTransacted()
    throws JMSException
  {
    checkOpen();
    
    return _isTransacted;
  }

  /**
   * Returns the acknowledge mode for the session.
   */
  public int getAcknowledgeMode()
    throws JMSException
  {
    checkOpen();
    
    return _acknowledgeMode;
  }

  /**
   * Returns the message listener
   */
  public MessageListener getMessageListener()
    throws JMSException
  {
    checkOpen();
    
    return _messageListener;
  }

  /**
   * Sets the message listener
   */
  public void setMessageListener(MessageListener listener)
    throws JMSException
  {
    checkOpen();
    
    _messageListener = listener;
    setAsynchronous();
  }

  /**
   * Set true for a synchronous session.
   */
  void setAsynchronous()
  {
    boolean oldAsynchronous = _isAsynchronous;
    
    _isAsynchronous = true;

    notifyListener();
  }

  /**
   * Set true for a synchronous session.
   */
  boolean isAsynchronous()
  {
    return _isAsynchronous;
  }

  /**
   * Creates a new byte[] message.
   */
  public BytesMessage createBytesMessage()
    throws JMSException
  {
    checkOpen();
    
    return new BytesMessageImpl();
  }

  /**
   * Creates a new map message.
   */
  public MapMessage createMapMessage()
    throws JMSException
  {
    checkOpen();
    
    return new MapMessageImpl();
  }

  /**
   * Creates a message.  Used when only header info is important.
   */
  public Message createMessage()
    throws JMSException
  {
    checkOpen();
    
    return new MessageImpl();
  }

  /**
   * Creates an object message.
   */
  public ObjectMessage createObjectMessage()
    throws JMSException
  {
    checkOpen();
    
    return new ObjectMessageImpl();
  }

  /**
   * Creates an object message.
   *
   * @param obj a serializable message.
   */
  public ObjectMessage createObjectMessage(Serializable obj)
    throws JMSException
  {
    checkOpen();
    
    ObjectMessage msg = createObjectMessage();

    msg.setObject(obj);

    return msg;
  }

  /**
   * Creates a stream message.
   */
  public StreamMessage createStreamMessage()
    throws JMSException
  {
    checkOpen();
    
    return new StreamMessageImpl();
  }

  /**
   * Creates a text message.
   */
  public TextMessage createTextMessage()
    throws JMSException
  {
    checkOpen();
    
    return new TextMessageImpl();
  }

  /**
   * Creates a text message.
   */
  public TextMessage createTextMessage(String message)
    throws JMSException
  {
    checkOpen();
    
    TextMessage msg = createTextMessage();

    msg.setText(message);

    return msg;
  }

  /**
   * Creates a consumer to receive messages.
   *
   * @param destination the destination to receive messages from.
   */
  public MessageConsumer createConsumer(Destination destination)
    throws JMSException
  {
    checkOpen();

    return createConsumer(destination, null, false);
  }

  /**
   * Creates a consumer to receive messages.
   *
   * @param destination the destination to receive messages from.
   * @param messageSelector query to restrict the messages.
   */
  public MessageConsumer createConsumer(Destination destination,
                                        String messageSelector)
    throws JMSException
  {
    checkOpen();
    
    return createConsumer(destination, messageSelector, false);
  }

  /**
   * Creates a consumer to receive messages.
   *
   * @param destination the destination to receive messages from.
   * @param messageSelector query to restrict the messages.
   */
  public MessageConsumer createConsumer(Destination destination,
                                        String messageSelector,
                                        boolean noLocal)
    throws JMSException
  {
    checkOpen();

    AbstractDestination dest = (AbstractDestination) destination;
    
    MessageConsumer consumer;
    consumer = dest.createConsumer(this, messageSelector, noLocal);
    
    addConsumer((MessageConsumerImpl) consumer);

    return consumer;
  }

  /**
   * Creates a producer to produce messages.
   *
   * @param destination the destination to send messages from.
   */
  public MessageProducer createProducer(Destination destination)
    throws JMSException
  {
    checkOpen();

    AbstractDestination dest = (AbstractDestination) destination;

    return dest.createProducer(this);
  }

  /**
   * Creates a QueueBrowser to browse messages in the queue.
   *
   * @param queue the queue to send messages to.
   */
  public QueueBrowser createBrowser(Queue queue)
    throws JMSException
  {
    checkOpen();
    
    return createBrowser(queue, null);
  }

  /**
   * Creates a QueueBrowser to browse messages in the queue.
   *
   * @param queue the queue to send messages to.
   */
  public QueueBrowser createBrowser(Queue queue, String messageSelector)
    throws JMSException
  {
    checkOpen();
    
    return ((AbstractDestination) queue).createBrowser(this, messageSelector);
  }

  /**
   * Creates a new queue.
   */
  public Queue createQueue(String queueName)
    throws JMSException
  {
    checkOpen();

    return _connection.getConnectionFactory().createQueue(queueName);
  }

  /**
   * Creates a temporary queue.
   */
  public TemporaryQueue createTemporaryQueue()
    throws JMSException
  {
    checkOpen();
    
    return new TemporaryQueueImpl();
  }

  /**
   * Creates a new topic.
   */
  public Topic createTopic(String topicName)
    throws JMSException
  {
    checkOpen();

    return _connection.getConnectionFactory().createTopic(topicName);
  }

  /**
   * Creates a temporary topic.
   */
  public TemporaryTopic createTemporaryTopic()
    throws JMSException
  {
    checkOpen();
    
    return new TemporaryTopicImpl();
  }

  /**
   * Creates a durable subscriber to receive messages.
   *
   * @param topic the topic to receive messages from.
   */
  public TopicSubscriber createDurableSubscriber(Topic topic, String name)
    throws JMSException
  {
    checkOpen();
    
    if (getClientID() == null)
      throw new JMSException(L.l("connection may not create a durable subscriber because it does not have an assigned ClientID."));

    return createDurableSubscriber(topic, name, null, false);
  }

  /**
   * Creates a subscriber to receive messages.
   *
   * @param topic the topic to receive messages from.
   * @param messageSelector topic to restrict the messages.
   * @param noLocal if true, don't receive messages we've sent
   */
  public TopicSubscriber createDurableSubscriber(Topic topic,
                                                 String name,
                                                 String messageSelector,
                                                 boolean noLocal)
    throws JMSException
  {
    checkOpen();
    
    AbstractDestination topicImpl = (AbstractDestination) topic;

    if (_connection.getDurableSubscriber(name) != null)
      throw new JMSException(L.l("'{0}' is already an active durable subscriber",
				 name));
    
    TopicSubscriber consumer;
    consumer = topicImpl.createDurableSubscriber(this, messageSelector,
						 noLocal, name);
    
    _connection.putDurableSubscriber(name, consumer);
    
    addConsumer((MessageConsumerImpl) consumer);

    return consumer;
  }

  /**
   * Unsubscribe from a durable subscription.
   */
  public void unsubscribe(String name)
    throws JMSException
  {
    checkOpen();

    _connection.removeDurableSubscriber(name);
  }

  /**
   * Starts the session.
   */
  void start()
  {
    notifyListener();
  }

  /**
   * Stops the session.
   */
  void stop()
  {
    synchronized (_consumers) {
      _consumers.notifyAll();

      long timeout = Alarm.getCurrentTime() + SHUTDOWN_WAIT_TIME;
      while (_isRunning && Alarm.getCurrentTime() < timeout) {
	try {
	  _consumers.wait(SHUTDOWN_WAIT_TIME);
	
	  if (Alarm.isTest()) {
	    return;
	  }
	} catch (Throwable e) {
	}
      }
    }
  }
  
  /**
   * Commits the messages.
   */
  public void commit()
    throws JMSException
  {
    checkOpen();

    if (! _isTransacted)
      throw new IllegalStateException(L.l("commit() can only be called on a transacted session."));


    ArrayList<TransactedMessage> messages = _transactedMessages;
    if (messages != null) {
      try {
	for (int i = 0; i < messages.size(); i++) {
	  messages.get(i).send();
	}
      } finally {
	messages.clear();
      }
    }

    acknowledge();
  }
  
  /**
   * Commits the messages.
   */
  public void acknowledge()
    throws JMSException
  {
    checkOpen();

    for (int i = 0; i < _consumers.size(); i++) {
      MessageConsumerImpl consumer = _consumers.get(i);

      try {
	consumer.acknowledge();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
  
  /**
   * Rollsback the messages.
   */
  public void rollback()
    throws JMSException
  {
    checkOpen();

    if (! _isTransacted)
      throw new IllegalStateException(L.l("rollback() can only be called on a transacted session."));
    
    if (_transactedMessages != null)
      _transactedMessages.clear();

    
    for (int i = 0; i < _consumers.size(); i++) {
      MessageConsumerImpl consumer = _consumers.get(i);

      try {
	consumer.rollback();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
  
  /**
   * Recovers the messages.
   */
  public void recover()
    throws JMSException
  {
    checkOpen();

    if (_isTransacted)
      throw new IllegalStateException(L.l("recover() may not be called on a transacted session."));
    
    for (int i = 0; i < _consumers.size(); i++) {
      MessageConsumerImpl consumer = _consumers.get(i);

      try {
	consumer.rollback();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
  
  /**
   * Closes the session
   */
  public void close()
    throws JMSException
  {
    if (_isClosed)
      return;

    try {
      stop();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    for (int i = 0; i < _consumers.size(); i++) {
      MessageConsumerImpl consumer = _consumers.get(i);

      try {
	consumer.rollback();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      try {
	consumer.close();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    try {
      _connection.removeSession(this);
    } finally {
      _isClosed = true;
    }

    _classLoader = null;
  }

  protected void addConsumer(MessageConsumerImpl consumer)
  {
    if (_consumers == null)
      _consumers = new ArrayList<MessageConsumerImpl>();

    _consumers.add(consumer);

    notifyListener();
  }

  protected void removeConsumer(MessageConsumerImpl consumer)
  {
    if (_consumers != null)
      _consumers.remove(consumer);
  }

  /**
   * Notifies the receiver.
   */
  void notifyListener()
  {
    _hasMessage = true;

    synchronized (_consumers) {
      _consumers.notifyAll();
    }
    
    if (_isAsynchronous) {
      ThreadPool.getThreadPool().schedule(this);
      // the yield is only needed for the regressions
      Thread.yield();
    }
  }

  /**
   * Adds a message to the session message queue.
   */
  public void send(AbstractDestination queue,
                   MessageImpl message,
                   int deliveryMode,
                   int priority,
                   long expiration)
    throws JMSException
  {
    checkOpen();
    
    message.setJMSMessageID(queue.generateMessageID());
    message.setJMSDestination(queue);
    message.setJMSDeliveryMode(deliveryMode);
    message.setJMSTimestamp(Alarm.getCurrentTime());
    message.setJMSExpiration(expiration);
    message.setJMSPriority(priority);
    
    MessageImpl destMessage = ((MessageImpl) message).copy();
    destMessage.setSession(this);
    destMessage.setReceive();

    if (_isTransacted) {
      if (_transactedMessages == null)
	_transactedMessages = new ArrayList<TransactedMessage>();

      TransactedMessage transMsg = new TransactedMessage(queue, message);
      
      _transactedMessages.add(transMsg);
    }
    else
      queue.send(destMessage);
  }

  /**
   * Called to synchronously receive a message.
   */
  protected Message receive(MessageConsumerImpl consumer,
			    long timeout)
    throws JMSException
  {
    throw new UnsupportedOperationException();
    /*
    checkOpen();
    
    if (Long.MAX_VALUE / 2 < timeout || timeout < 0)
      timeout = Long.MAX_VALUE / 2;
    
    long now = Alarm.getCurrentTime();
    long failTime = Alarm.getCurrentTime() + timeout;
    
    Selector selector = consumer.getSelector();
    AbstractDestination queue;
    queue = (AbstractDestination) consumer.getDestination();

    // 4.4.1 user's reponsibility
    // checkThread();

    Thread oldThread = Thread.currentThread();
    try {
      // _thread = Thread.currentThread();
      
      while (! consumer.isClosed()) {
	if (isActive()) {
	  Message msg = queue.receive(selector);
	  if (msg != null)
	    return msg;
	  _hasMessage = false;
	}
      
	long delta = failTime - Alarm.getCurrentTime();

	if (delta <= 0 || _isClosed || Alarm.isTest())
	  return null;

	synchronized (_consumers) {
	  if (! _hasMessage || ! isActive()) {
	    try {
	      _consumers.wait(delta);
	    } catch (Throwable e) {
	    }
	  }
	}
      }
    } finally {
      // _thread = oldThread;
    }

    return null;
    */
  }

  /**
   * Called to synchronously receive messages
   */
  public void run()
  {
    _hasMessage = true;
    Thread thread = Thread.currentThread();

    while (_hasMessage && isActive() && ! isStopping()) {
      synchronized (_consumers) {
	if (_isRunning)
	  return;

	_isRunning = true;
      }

      try {
	// _thread = Thread.currentThread();
	_hasMessage = false;

	for (int i = 0; i < _consumers.size(); i++) {
	  MessageConsumerImpl consumer = _consumers.get(i);
	  //AbstractDestination queue;
	  //queue = (AbstractDestination) consumer.getDestination();
	  //Selector selector = consumer.getSelector();
	  MessageListener listener = consumer.getMessageListener();
	    
	  if (_messageListener != null)
	    listener = _messageListener;
	    
	  if (consumer.isActive() && ! isStopping() && listener != null) {
	    try {
	      Message msg = consumer.receiveNoWait();

	      if (msg != null) {
		_hasMessage = true;

		if (log.isLoggable(Level.FINE))
		  log.fine("JMS " + msg + " delivered to " + listener);

		ClassLoader oldLoader = thread.getContextClassLoader();
		try {
		  thread.setContextClassLoader(_classLoader);
		  listener.onMessage(msg);
		} finally {
		  thread.setContextClassLoader(oldLoader);
		}
	      }
	    } catch (Throwable e) {
	      log.log(Level.WARNING, e.toString(), e);
	    }
	  }
	}
      } finally {
	// _thread = null;
	
	synchronized (_consumers) {
	  _isRunning = false;
	  
	  _consumers.notifyAll();
	}
      }
    }
  }

  /**
   * Checks that the session is open.
   */
  public void checkOpen()
    throws IllegalStateException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("session is closed"));
  }

  /**
   * Verifies that multiple threads aren't using the session.
   *
   * 4.4.1 the client takes the responsibility.  There's no
   * validation check.
   */
  void checkThread()
    throws JMSException
  {
    Thread thread = _thread;
    
    if (thread != Thread.currentThread() && thread != null) {
      Exception e = new IllegalStateException(L.l("Can't use session from concurrent threads."));
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  static class TransactedMessage {
    private AbstractDestination _queue;
    private MessageImpl _message;

    TransactedMessage(AbstractDestination queue, MessageImpl message)
    {
      _queue = queue;
      _message = message;
    }

    void send()
      throws JMSException
    {
      _queue.send(_message);
    }
  }
}
