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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.message;

import java.lang.reflect.Method;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.jms.ConnectionFactory;
import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Destination;
import javax.jms.Topic;
import javax.jms.MessageListener;
import javax.jms.JMSException;

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.config.ConfigException;

import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.AbstractContext;

/**
 * Server container for a message bean.
 */
public class MessageServer extends AbstractServer {
  private static final L10N L = new L10N(MessageServer.class);
  protected static final Logger log = Log.open(MessageServer.class);

  private Connection _connection;
  private Destination _destination;

  private String _subscriptionName;
  private String _selector;
  private int _acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

  private int _consumerMax = 5;

  private MessageDrivenContext _context;

  private ArrayList<Consumer> _consumers = new ArrayList<Consumer>();

  public MessageServer(EjbServerManager manager)
  {
    super(manager);
    
    _context = new MessageDrivenContextImpl(this);
  }

  /**
   * Sets the destination.
   */
  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  /**
   * Sets the consumer max
   */
  public void setConsumerMax(int consumer)
  {
    _consumerMax = consumer;
  }

  /**
   * Initialize the server
   */
  public void init()
    throws Exception
  {
  }

  /**
   * Starts the server.
   */
  public void start()
    throws Exception
  {
    ConnectionFactory factory = null;//_config.getConnectionFactory();
    if (factory == null)
      factory = _ejbManager.getConnectionFactory();

    if (_destination == null)
      throw new ConfigException(L.l("No destination is configured."));

    if (_consumerMax <= 0)
      throw new ConfigException(L.l("No listeners are configured."));

    if (factory == null)
      throw new ConfigException(L.l("Message beans need a jms-connection-factory.  The ConnectionFactory object must be configured."));

    Connection connection = factory.createConnection();
    _connection = connection;

    if (_destination instanceof Topic)
      _consumerMax = 1;

    for (int i = 0; i < _consumerMax; i++) {
      Consumer consumer = new Consumer();

      _consumers.add(consumer);

      consumer.start();
    }
    
    _connection.start();
  }
  
  void generate()
    throws Exception
  {
  }

  public AbstractContext getContext(Object obj, boolean foo)
  {
    throw new UnsupportedOperationException();
  }

  public Connection getJMSConnection()
  {
    return _connection;
  }

  public Destination getDestination()
  {
    return _destination;
  }
  
  /**
   * Cleans up the entity server nicely.
   */
  public void destroy()
  {
    try {
      ArrayList<Consumer> consumers = new ArrayList<Consumer>(_consumers);
      _consumers = null;
      
      for (Consumer consumer : consumers) {
	consumer.destroy();
      }
      
      if (_connection != null)
        _connection.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  class Consumer {
    private Session _session;
    private MessageConsumer _consumer;
    private MessageListener _listener;
    
    Consumer()
      throws Exception
    {
    }

    /**
     * Creates the session.
     */
    void start()
      throws Exception
    {
      Class cl = _contextImplClass;
    
      _listener = (MessageListener) cl.newInstance();

      if (_listener instanceof MessageDrivenBean) {
	MessageDrivenBean bean = (MessageDrivenBean) _listener;
	bean.setMessageDrivenContext(_context);
      }

      Method create = null;

      try {
	create = cl.getMethod("ejbCreate", new Class[0]);
	create.invoke(_listener, new Object[0]);
      } catch (NoSuchMethodException e) {
      }

      boolean transacted = true;
      _session = _connection.createSession(transacted, _acknowledgeMode);

      if (_subscriptionName != null) {
	Topic topic = (Topic) _destination;
	
	_consumer = _session.createDurableSubscriber(topic,
						     _subscriptionName,
						     _selector,
						     true);
      }
      else {
	_consumer = _session.createConsumer(_destination, _selector);
      }

      _consumer.setMessageListener(_listener);
    }

    /**
     * Returns the session.
     */
    public Session getSession()
      throws JMSException
    {
      return _session;
    }

    /**
     * Destroys the listener.
     */
    private void destroy()
      throws JMSException
    {
      Session session = _session;
      _session = null;

      try {
	if (session != null)
	  session.close();
      } finally {
	if (_listener instanceof MessageDrivenBean) {
	  MessageDrivenBean bean = (MessageDrivenBean) _listener;
	  _listener = null;
	  bean.ejbRemove();
	}
      }
    }
  }
}
