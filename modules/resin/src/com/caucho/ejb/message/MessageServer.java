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

import com.caucho.config.ConfigException;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.cfg.Interceptor;
import com.caucho.ejb.interceptor.InvocationContextImpl;
import com.caucho.ejb.xa.*;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.naming.Jndi;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.interceptor.InvocationContext;
import javax.jms.*;
import javax.naming.*;
import javax.transaction.*;
import javax.transaction.xa.*;
import java.lang.reflect.InvocationTargetException;
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

  private ConnectionFactory _connectionFactory;
  private Connection _connection;
  private Destination _destination;
  private String _messageDestinationLink;
  private Class _messageListenerType;
  private String _aroundInvokeMethodName;
  private ArrayList<Interceptor> _interceptors;

  private boolean _isContainerTransaction;

  private String _subscriptionName;
  private String _selector;
  private int _acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

  private int _consumerMax = 5;

  private MessageDrivenContext _context;

  private UserTransaction _ut;

  private ArrayList<Consumer> _consumers = new ArrayList<Consumer>();

  public MessageServer(EjbContainer ejbContainer)
  {
    super(ejbContainer);

    // ejb/0fbl
    _context = new MessageDrivenContextImpl(this, null, true);

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
   * Sets the around invoke method name.
   */
  public void setAroundInvokeMethodName(String aroundInvokeMethodName)
  {
    _aroundInvokeMethodName = aroundInvokeMethodName;
  }

  /**
   * Sets the interceptor listeners.
   */
  public void setInterceptors(ArrayList<Interceptor> interceptors)
  {
    _interceptors = interceptors;
  }

  /**
   * Sets the connection factory
   */
  public void setConnectionFactory(ConnectionFactory factory)
  {
    _connectionFactory = factory;
  }

  /**
   * Sets the destination.
   */
  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  /**
   * Sets the message selector.
   */
  public void setMessageSelector(String messageSelector)
  {
    _selector = messageSelector;
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

      WebBeansContainer webBeans = WebBeansContainer.create();

      SingletonComponent comp
        = new SingletonComponent(webBeans, _context);
      comp.setTargetType(MessageDrivenContext.class);
      comp.init();
      webBeans.addComponent(comp);

      log.config("initialized message bean: " + this);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Starts the server.
   */
  @Override
  public boolean start()
    throws Exception
  {
    if (! super.start())
      return false;

    ConnectionFactory factory = _connectionFactory;

    /*
     * XXX: not correct way, waiting for webbeans integration
     *
     if (_destination == null && _messageDestinationLink != null) {
     _destination = getContainer().getMessageDestination(_messageDestinationLink).getResolvedDestination();
     }
    */

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

    return true;
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
    else
      throw new ConfigException(L.l("No valid message listener interface found"));
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

      Object listener = _listener;

      if (_listener instanceof MessageListenerAdapter) {
        listener = ((MessageListenerAdapter) _listener).getListener();
      }

      initInstance(listener, null);
      
      // getInitProgram().configure(listener);

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
        XAResource xaResource = ((XASession) _session).getXAResource();

        XAListener xaListener = new XAListener(_listener,
                                               _context,
                                               _ut,
                                               _session,
                                               xaResource);

        xaListener.setAroundInvokeMethodName(_aroundInvokeMethodName);
        xaListener.setInterceptors(_interceptors);

        _consumer.setMessageListener(xaListener);
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
    private XAResource _xaResource;
    private EjbTransactionManager _xaManager;
    private String _aroundInvokeMethodName;
    private Method _aroundInvokeMethod;
    private ArrayList<Interceptor> _interceptors;

    XAListener(MessageListener listener,
               MessageDrivenContextImpl context,
               UserTransaction ut,
               Session session,
               XAResource xaResource)
    {
      _listener = listener;
      _context = context;
      _ut = ut;
      _session = session;
      _xaResource = xaResource;

      if (_ut != null)
        _xaManager = _context.getServer().getTransactionManager();
    }

    public void setAroundInvokeMethodName(String aroundInvokeMethodName)
    {
      _aroundInvokeMethodName = aroundInvokeMethodName;
    }

    public void setInterceptors(ArrayList<Interceptor> interceptors)
    {
      _interceptors = interceptors;
    }

    private boolean hasInterceptors()
    {
      if (_aroundInvokeMethodName != null)
        return true;

      if (_interceptors != null && _interceptors.size() > 0)
        return true;

      return false;
    }

    private void callInterceptors(Message msg)
    {
      if (_aroundInvokeMethodName != null && _aroundInvokeMethod == null) {
        _aroundInvokeMethod =
          Interceptor.getAroundInvokeMethod(_listener.getClass(),
                                            _aroundInvokeMethodName);
      }

      int c = 0;

      if (_aroundInvokeMethod != null)
        c++;

      if (_interceptors != null)
        c += _interceptors.size();

      Object interceptors[] = new Object[c];
      Method interceptorMethods[] = new Method[c];

      try {
        int i = 0;

        // ejb/0fbm
        for (Interceptor interceptor : _interceptors) {
          String name = interceptor.getAroundInvokeMethodName();

          if (name == null)
            continue;

          Method method = interceptor.getAroundInvokeMethod();
          Class cl = interceptor.getInterceptorJClass().getJavaClass();

          interceptors[i] = cl.newInstance();
          interceptorMethods[i] = method;

          i++;
        }

        // ejb/0fbl, ejb/0fbm
        if (_aroundInvokeMethod != null) {
          interceptors[i] = _listener;
          interceptorMethods[i] = _aroundInvokeMethod;
        }

        InvocationContext invocationContext;

        // ejb/0fbl, ejb/0fbm
        invocationContext = new InvocationContextImpl(_listener,
                                                      _listener,
                                                      "onMessage",
                                                      new Class[] { Message.class },
                                                      interceptors,
                                                      interceptorMethods);

        invocationContext.setParameters(new Object[] { msg });

        invocationContext.proceed();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void onMessage(Message msg)
    {
      try {
        TransactionContext xa = null;
        Transaction trans = null;

        if (_context.isCMT()) {
          xa = _xaManager.beginRequired();
          trans = xa.getTransaction();
        }

        if (trans != null)
          trans.enlistResource(_xaResource);

        try {
          if (hasInterceptors())
            callInterceptors(msg);
          else
            _listener.onMessage(msg);
        } finally {
          if (trans != null)
            trans.delistResource(_xaResource, 0);

          /*
            if (xa.getRollbackOnly())
            _session.recover();
          */

          if (xa != null) {
            xa.commit();
          }
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
