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

package com.caucho.ejb.message;

import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;

import javax.jms.*;
import javax.resource.*;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.*;
import javax.resource.spi.work.*;
import javax.transaction.xa.*;

import com.caucho.config.*;

public class JmsResourceAdapter implements ResourceAdapter {
  private static final Logger
    log = Logger.getLogger(JmsResourceAdapter.class.getName());
  
  private static final Method _onMessage;

  private final String _ejbName;
  private final ConnectionFactory _connectionFactory;
  private final Destination _destination;

  private int _consumerMax = 5;
  private int _acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
  
  private String _subscriptionName;
  private String _selector;

  private Connection _connection;
  private MessageEndpointFactory _endpointFactory;

  private ArrayList<Consumer> _consumers;

  public JmsResourceAdapter(String ejbName,
			    ConnectionFactory factory,
			    Destination destination)
  {
    assert(factory != null);
    assert(destination != null);
    
    _ejbName = ejbName;
    _connectionFactory = factory;
    
    _destination = destination;
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
  
  public void start(BootstrapContext ctx)
    throws ResourceAdapterInternalException
  {
  }
  
  /**
   * Called when the resource adapter is stopped.
   */
  public void stop()
  {
  }

  /**
   * Called during activation of a message endpoint.
   */
  public void endpointActivation(MessageEndpointFactory endpointFactory,
				 ActivationSpec spec)
    throws NotSupportedException, ResourceException
  {
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

      for (int i = 0; i < _consumerMax; i++) {
	Consumer consumer = new Consumer(_connection, _destination);

	_consumers.add(consumer);

	consumer.start();
      }

      _connection.start();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Called during deactivation of a message endpoint.
   */
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

  class Consumer {
    private Session _session;
    
    private XAResource _xaResource;
    private MessageConsumer _consumer;
    
    private MessageEndpoint _endpoint;
    private MessageListener _listener;

    Consumer(Connection conn, Destination destination)
      throws Exception
    {
      if (conn instanceof XAConnection) {
	XASession xaSession = ((XAConnection) conn).createXASession();
	_session = xaSession;
	_xaResource = xaSession.getXAResource();
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
      _endpoint.release();
    }
  }

  static {
    try {
      _onMessage = MessageListener.class.getMethod("onMessage",
						   new Class[] { Message.class });
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
