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

package com.caucho.ejb.message;

import com.caucho.config.ConfigException;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.xa.*;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.naming.Jndi;

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.*;
import javax.naming.*;
import javax.transaction.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server container for a message bean.
 */
public class MessageServer extends AbstractServer {
  private static final L10N L = new L10N(MessageServer.class);
  protected static final Logger log = Log.open(MessageServer.class);

  private Connection _connection;
  private Destination _destination;
  private String _messageDestinationLink;
  private Class _messageListenerType;

  private boolean _isContainerTransaction;

  private String _subscriptionName;
  private String _selector;
  private int _acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

  private int _consumerMax = 5;

  private MessageDrivenContext _context;

  private UserTransaction _ut;

  private ArrayList<Consumer> _consumers = new ArrayList<Consumer>();

  public MessageServer(EjbServerManager manager)
  {
    super(manager);
    
    try {
      InitialContext ic = new InitialContext();
      
      _ut = (UserTransaction) ic.lookup("java:comp/UserTransaction");
    } catch (NamingException e) {
      throw new RuntimeException(e);
    }
  }

  protected String getType()
  {
    return "message:";
  }

  /**
   * Sets the destination.
   */
  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  /**
   * Sets the message-destination-link, and alternative to setting the destination directly.
   */
  public void setMessageDestinationLink(String messageDestinationLink)
  {
    _messageDestinationLink = messageDestinationLink;
  }

  /**
   * Sets the type of the listener.
   */
  public void setMessageListenerType(Class messageListenerType)
  {
    _messageListenerType = messageListenerType;
  }

  /**
   * Sets the consumer max
   */
  public void setConsumerMax(int consumer)
  {
    _consumerMax = consumer;
  }

  /**
   * Set true for container transaction.
   */
  public void setContainerTransaction(boolean isContainerTransaction)
  {
    _isContainerTransaction = isContainerTransaction;
  }

  /**
   * Initialize the server
   */
  @Override
  public void init()
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      super.init();

      // XXX:
      // Should be a resin-specific name, like
      // java:comp/env/resin-ejb/messageDrivenContext, since storing it in
      // JNDI is a resin-specific implementation
      // It needs to match InjectIntrospector
      Jndi.rebindDeep("java:comp/env/ejbContext", _context);
      Jndi.rebindDeep("java:comp/env/messageDrivenContext", _context);

      log.config("initialized message bean: " + this);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Starts the server.
   */
  @Override
  public void start()
    throws Exception
  {
    super.start();

    ConnectionFactory factory = null;//_config.getConnectionFactory();
    if (factory == null)
      factory = _ejbManager.getConnectionFactory();

    if (_destination == null && _messageDestinationLink != null) {
      _destination = getContainer().getMessageDestination(_messageDestinationLink).getResolvedDestination();
    }

    if (_destination == null) {
      try {
        _destination = (Destination) new InitialContext().lookup("java:comp/env/" + getMappedName());
      } catch (Exception e) {
      }
    }

    if (_destination == null)
      throw new ConfigException(L.l("No destination is configured for {0} '{1}'.",
                                    "<message-driven>", getEJBName()));

    if (_consumerMax <= 0)
      throw new ConfigException(L.l("No listeners are configured for {0} '{1}.",
                                    "<message-driven>", getEJBName()));

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

  @Override
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

  private MessageListener createMessageListener()
    throws Exception
  {
    Class cl = _contextImplClass;

    Object listener = cl.newInstance();

    if (listener instanceof MessageListener)
      return (MessageListener) listener;
    else if (_messageListenerType == MessageListener.class)
      return new MessageListenerAdapter(listener);
    else {
      throw new ConfigException(L.l("No valid message listener interface found"));
    }
  }

  /**
   * Cleans up the entity server nicely.
   */
  @Override
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
    private MessageDrivenContextImpl _context;
    
    Consumer()
      throws Exception
    {
      _context = new MessageDrivenContextImpl(MessageServer.this, _ut,
					      _isContainerTransaction);
    }

    /**
     * Creates the session.
     */
    void start()
      throws Exception
    {
      _listener = createMessageListener();

      if (_listener instanceof MessageDrivenBean) {
	MessageDrivenBean bean = (MessageDrivenBean) _listener;
	bean.setMessageDrivenContext(_context);
      }

      getInitProgram().configure(_listener);

      Method create = null;

      if (_listener instanceof MessageDrivenBean) {
        try {
          create = _listener.getClass().getMethod("ejbCreate", new Class[0]);
          create.invoke(_listener, new Object[0]);
        }
        catch (NoSuchMethodException e) {
        }
      }

      // XXX: ejb/090c
      // XXX: doesn't seem to be properly handling the sessions
      boolean transacted = false; 

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

      if (_isContainerTransaction) {
        _consumer.setMessageListener(new XAListener(_listener,
                                                    _context,
                                                    _ut,
                                                    _session));
      }
      else
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

  static class XAListener implements MessageListener {
    private MessageListener _listener;
    private MessageDrivenContextImpl _context;
    private UserTransaction _ut;
    private Session _session;
    private EjbTransactionManager _xaManager;

    XAListener(MessageListener listener,
               MessageDrivenContextImpl context,
               UserTransaction ut,
               Session session)
    {
      _listener = listener;
      _context = context;
      _ut = ut;
      _session = session;

      if (_ut != null)
        _xaManager = _context.getServer().getTransactionManager();
    }

    public void onMessage(Message msg)
    {
      try {
        _xaManager.beginRequired();

        try {
          _listener.onMessage(msg);
        } finally {
          Transaction xa = _xaManager.getTransaction();
          
          if (xa != null && xa.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            _session.recover();
          }
          
          _xaManager.commitTransaction();
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
