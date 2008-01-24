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
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JCA activation-spec server container for a message bean.
 */
public class MessageServer extends AbstractServer
  implements MessageEndpointFactory
{
  private static final L10N L = new L10N(MessageServer.class);
  protected static final Logger log
    = Logger.getLogger(MessageServer.class.getName());

  private ResourceAdapter _ra;
  private ActivationSpec _activationSpec;

  private MessageDrivenContext _context;

  public MessageServer(EjbContainer ejbContainer)
  {
    super(ejbContainer);

    // ejb/0fbl
    _context = new MessageDrivenContextImpl(this, null, true);
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

    Constructor ctor = beanClass.getConstructor(new Class[] { MessageServer.class });
    
    Object listener = ctor.newInstance(this);

    return listener;
  }

  /**
   * Cleans up the entity server nicely.
   */
  @Override
  public void destroy()
  {
    _ra.endpointDeactivation(this, _activationSpec);
  }
}
