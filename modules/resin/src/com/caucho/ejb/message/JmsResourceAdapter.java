/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;
import javax.inject.Named;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.XAConnection;
import javax.jms.XASession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.InjectManager;
import com.caucho.ejb.cfg.JmsActivationConfig;
import com.caucho.env.thread.ThreadPool;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.transaction.UserTransactionProxy;
import com.caucho.util.L10N;

public class JmsResourceAdapter implements ResourceAdapter {
  private static final L10N L = new L10N(JmsResourceAdapter.class);
  private static final Logger log
    = Logger.getLogger(JmsResourceAdapter.class.getName());

  private final static Method _onMessageMethod;
  
  private final JmsActivationConfig _config;
  
  private final String _ejbName;
  
  
  private ConnectionFactory _connectionFactory;
  private Destination _destination;
  
  private UserTransactionProxy _ut;

  private int _consumerMax = 5;
  private int _acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
  
  private String _subscriptionName;
  private String _selector;

  private Connection _connection;
  private MessageEndpointFactory _endpointFactory;

  private ArrayList<Consumer> _consumers;
  
  private Lifecycle _lifecycle = new Lifecycle();

  public JmsResourceAdapter(String ejbName,
                            JmsActivationConfig config)
  {
    _ejbName = ejbName;
    
    _config = config;
    /*
    _connectionFactory = factory;
    */
    
    _destination = config.getDestinationObject();
    
    _ut = UserTransactionProxy.getCurrent();
  }

  public void setMessageSelector(String selector)
  {
    _selector = selector;
  }

  public void setSubscriptionName(String subscriptionName)
  {
    _subscriptionName = subscriptionName;
  }
  
  public void setConsumerMax(int consumerMax)
  {
    _consumerMax = consumerMax;
  }
  
  public void setAcknowledgeMode(int acknowledgeMode)
  {
    _acknowledgeMode = acknowledgeMode;
  }
  
  @Override
  public void start(BootstrapContext ctx)
    throws ResourceAdapterInternalException
  {
  }
  
  private void init()
  {
    if (! _lifecycle.toActive())
      return;
    
    _connectionFactory = getResource(ConnectionFactory.class,
                                     _config.getConnectionFactoryName());
    
    if (_connectionFactory == null)
      throw new ConfigException(L.l("connection-factory must be specified for @MessageDriven bean"));
    
    if (_config.getDestinationType() == null)
      throw new ConfigException(L.l("destination-type must be specified for @MessageDriven bean"));

    if (_destination == null) {
      _destination = getResource(_config.getDestinationType(),
                                 _config.getDestinationName());
    }
    
    if (_destination== null)
      throw new ConfigException(L.l("destination must be specified for @MessageDriven bean"));
  }
  
  /**
   * Called when the resource adapter is stopped.
   */
  @Override
  public void stop()
  {
  }
  
  @SuppressWarnings("unchecked")
  private <T> T getResource(Class<T> type, String name)
  {
    if (name == null) {
    }    
    else if (name.startsWith("java:comp")) {
      try {
        Context ic = new InitialContext();
        
        return (T) ic.lookup(name);
      } catch (NamingException e) {
        throw ConfigException.create(L.l("{0} is an unknown JNDI name for {1}\n  {2}",
                                         name, type.getName(), e.toString()),
                                     e);
      }
    }
    else {
      String jndiName = "java:comp/env/" + name;
      
      try {
        Context ic = new InitialContext();
        
        T value = (T) ic.lookup(jndiName);
        
        if (value != null)
          return value;
      } catch (NamingException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    InjectManager beanManager = InjectManager.create();
    
    Set<Bean<?>> beans;

    if (name != null) {
      Named named = Names.create(name);

      beans = beanManager.getBeans(type, named);
    }
    else {
      beans = beanManager.getBeans(type);
    }
    
    Bean<?> bean = beanManager.resolve(beans);
    
    if (bean == null) {
      throw new ConfigException(L.l("'{0}' with name='{1}' is an unknown JMS resource",
                                    type.getName(), name));
    }
    
    return (T) beanManager.getReference(bean);
  }

  /**
   * Called during activation of a message endpoint.
   */
  @Override
  public void endpointActivation(MessageEndpointFactory endpointFactory,
                                 ActivationSpec spec)
    throws NotSupportedException, ResourceException
  {
    init();
    
    synchronized (this) {
      if (_consumers != null)
        throw new java.lang.IllegalStateException();
      _consumers = new ArrayList<Consumer>();
    }

    try {
      assert(_connectionFactory != null);
      assert(_destination != null);
      assert(_consumerMax > 0);

      _endpointFactory = endpointFactory;
    
      Connection connection = _connectionFactory.createConnection();
      _connection = connection;

      if (_destination instanceof Topic)
        _consumerMax = 1;

      _connection.start();

      for (int i = 0; i < _consumerMax; i++) {
        Consumer consumer = new Consumer(_connection, _destination);

        _consumers.add(consumer);

        consumer.start();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Called during deactivation of a message endpoint.
   */
  @Override
  public void endpointDeactivation(MessageEndpointFactory endpointFactory,
                                   ActivationSpec spec)
  {
    try {
      ArrayList<Consumer> consumers = _consumers;
      _consumers = null;

      if (consumers != null) {
        consumers = new ArrayList<Consumer>(consumers);

        for (Consumer consumer : consumers) {
          consumer.destroy();
        }
      }

      if (_connection != null)
        _connection.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Called during crash recovery.
   */
  public XAResource []getXAResources(ActivationSpec []specs)
    throws ResourceException
  {
    return new XAResource[0];
  }

  public String toString()
  {
    return getClass().getName() +  "[" + _ejbName + "," + _destination + "]";
  }

  class Consumer implements Runnable {
    private Session _session;
    
    private XAResource _xaResource;
    private MessageConsumer _consumer;
    
    private MessageEndpoint _endpoint;
    private MessageListener _listener;
    
    private boolean _isActive;

    Consumer(Connection conn, Destination destination)
      throws Exception
    {
      if (false && conn instanceof XAConnection) {
        XASession xaSession = ((XAConnection) conn).createXASession();
        _session = xaSession;
        _xaResource = xaSession.getXAResource();

        /*
        // ejb/09a0 - needs to be auto-ack because if processing throws
        // an exception because of a bug, can't just replay
        boolean transacted = true;
        _session = conn.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
        */
      }
      else {
        boolean transacted = false;
        _session = conn.createSession(transacted, _acknowledgeMode);
      }

      _endpoint = _endpointFactory.createEndpoint(_xaResource);
      
      _listener = (MessageListener) _endpoint;
    }

    /**
     * Creates the session.
     */
    void start()
      throws Exception
    {
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
      
      _isActive = true;
      ThreadPool.getCurrent().start(this);
      
      // _consumer.setMessageListener(_listener);
    }
    
    @Override
    public void run()
    {
      while (_isActive) {
        try {
          _endpoint.beforeDelivery(_onMessageMethod);
          try {
            handleMessage();
          } finally {
            _endpoint.afterDelivery();
          }
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }
    
    private void handleMessage()
    {
      try {
        Message msg = _consumer.receive();

        if (msg != null) {
          _listener.onMessage(msg);
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
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
      _isActive = false;
      
      _endpoint.release();
    }
  }
  
  static {
    Method method = null;
    
    try {
      method = MessageListener.class.getMethod("onMessage", Message.class);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    _onMessageMethod = method;
  }
}
