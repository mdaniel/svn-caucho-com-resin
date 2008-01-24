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
import com.caucho.jca.*;
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
import javax.resource.spi.*;
import javax.resource.spi.endpoint.*;
import javax.transaction.*;
import javax.transaction.xa.*;
import javax.webbeans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JCA activation-spec server container for a message bean.
 */
public class ActivationMessageServer extends AbstractServer
  implements MessageEndpointFactory
{
  private static final L10N L = new L10N(ActivationMessageServer.class);
  protected static final Logger log
    = Logger.getLogger(ActivationMessageServer.class.getName());

  private ResourceArchive _ra;
  private ActivationSpec _activationSpec;

  private boolean _isContainerTransaction;

  private int _consumerMax = 5;

  private MessageDrivenContext _context;

  private ResourceAdapter _ra;

  private UserTransaction _ut;

  private ArrayList<Consumer> _consumers = new ArrayList<Consumer>();

  public ActivationMessageServer(EjbContainer ejbContainer)
  {
    super(ejbContainer);

    // ejb/0fbl
    //_context = new MessageDrivenContextImpl(this, null, true);
  }

  protected String getType()
  {
    return "message:";
  }
  
  /**
   * Sets the activation spec
   */
  public void setActivationSpec(ActivationSpec activationSpec)
  {
    _activationSpec = activationSpec;
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

      if (_activationSpec == null)
	throw error(L.l("ActivationSpec is missing from message-driven bean '{0}'.",
			getEJBName()));


      String specType = _activationSpec.getClass().getName();

      ResourceArchive raCfg = ResourceArchiveManager.findResourceArchive(specType);

      if (raCfg == null)
	throw error(L.l("'{0}' is an unknown activation-spec.  Make sure the .rar file for the driver is properly installed.",
			specType));

      Class raClass = raCfg.getResourceAdapterClass();

      if (raClass == null)
	throw error(L.l("resource-adapter class does not exist for activation-spec '{0}'.  Make sure the .rar file for the driver is properly installed.",
			raClass.getName()));

      WebBeansContainer webBeans = WebBeansContainer.create();

      ComponentFactory raFactory = webBeans.resolveByType(raClass);

      if (raFactory == null) {
	throw error(L.l("resource-adapter '{0}' must be configured in a <connector> tag.",
			raClass.getName()));
      }

      _ra = (ResourceAdapter) raFactory.get();

      if (_ra == null)
	throw new NullPointerException();

      /*
      SingletonComponent comp
        = new SingletonComponent(webBeans, _context);
      comp.setTargetType(MessageDrivenContext.class);
      comp.init();
      webBeans.addComponent(comp);
      */

      // log.fine(this + " init");
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
    
    _ra.endpointActivation(this, _activationSpec);

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
  
  /**
   * Creates an endpoint with the associated XA resource.
   */
  public MessageEndpoint createEndpoint(XAResource xaResource)
    throws UnavailableException
  {
    try {
      Object listener = createMessageListener();

      ((CauchoMessageEndpoint) listener).__caucho_setXAResource(xaResource);
      
      return (MessageEndpoint) listener;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns true to find out whether message deliveries to the
   * message endpoint will be transacted.  This is only a hint.
   */
  public boolean isDeliveryTransacted(Method method)
    throws NoSuchMethodException
  {
    return false;
  }

  private Object createMessageListener()
    throws Exception
  {
    Class beanClass = getBeanSkelClass();

    Object listener = beanClass.newInstance();

    return listener;
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
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  class Consumer {
    private MessageDrivenContextImpl _context;

    Consumer()
      throws Exception
    {
      /*
      _context = new MessageDrivenContextImpl(ActivationMessageServer.this, _ut,
                                              _isContainerTransaction);
      */
    }

    /**
     * Creates the session.
     */
    void start()
      throws Exception
    {
      
      /*
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
      */
    }

    /**
     * Destroys the listener.
     */
    private void destroy()
      throws JMSException
    {
      /*
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
      */
    }
  }

  /*
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
*/
}
