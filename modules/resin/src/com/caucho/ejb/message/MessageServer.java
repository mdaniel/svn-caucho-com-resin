/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.MessageDrivenContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.ejb.server.AbstractServer;
import com.caucho.ejb.server.EjbProducer;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * JCA activation-spec server container for a message bean.
 */
@Module
public class MessageServer<T> extends AbstractServer<T>
  implements MessageEndpointFactory
{
  private static final L10N L = new L10N(MessageServer.class);
  protected static final Logger log
    = Logger.getLogger(MessageServer.class.getName());

  private ResourceAdapter _ra;
  private ActivationSpec _activationSpec;

  private MessageDrivenContext _context;

  private Method _ejbCreate;

  public MessageServer(EjbContainer ejbContainer, 
                       AnnotatedType<T> annotatedType)
  {
    super(ejbContainer, annotatedType);

    InjectManager webBeans = InjectManager.create();
    
    UserTransaction ut = webBeans.getReference(UserTransaction.class);
    
    // ejb/0fbl
    _context = new MessageDrivenContextImpl(this, ut);
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
   * Sets the resource adapter
   */
  public void setResourceAdapter(ResourceAdapter ra)
  {
    _ra = ra;
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


      if (_ra == null)
	throw error(L.l("ResourceAdapter is missing from message-driven bean '{0}'.",
			getEJBName()));

      try {
	Class<?> beanClass = getBeanSkelClass();

	_ejbCreate = beanClass.getMethod("ejbCreate", new Class[0]);
	  
	// getProducer().bindInjection();
      } catch (Exception e) {
	log.log(Level.FINEST, e.toString(), e);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  @Override
  protected void bindContext()
  {
    super.bindContext();
    
    InjectManager manager = InjectManager.create();
    BeanFactory<?> factory = manager.createBeanFactory(_context.getClass());

    manager.addBean(factory.singleton(_context));
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
     
    // _ra.start(ResourceManagerImpl.create());
     
    _ra.endpointActivation(this, _activationSpec);

    return true;
  }

  /**
   * Returns the message driven context
   */
  public MessageDrivenContext getMessageContext()
  {
    return _context;
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
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
	throw (RuntimeException) e.getCause();
      if (e.getCause() != null)
	throw new UnavailableException(e.getCause());
      else
	throw new UnavailableException(e);
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

  private T createMessageListener()
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());
      
      Class<T> beanClass = getBeanSkelClass();

      Constructor<T> ctor = beanClass.getConstructor(new Class[] { MessageServer.class });
    
      T listener = ctor.newInstance(this);

      EjbProducer<T> producer = getProducer();
      
      producer.initInstance(listener);

      if (_ejbCreate != null)
	_ejbCreate.invoke(listener);

      return listener;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Cleans up the entity server nicely.
   */
  @Override
  public void destroy()
  {
    _ra.endpointDeactivation(this, _activationSpec);
  }

  @Override
  public Object getRemoteObject(Class api, String protocol)
  {
    return null;
  }

  @Override
  public Object getLocalObject(Class api)
  {
    return null;
  }

  @Override
  public Object getLocalProxy(Class api)
  {
    return null;
  }
}
