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

package com.caucho.ejb.session;

import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.xa.EjbTransactionManager;
import com.caucho.config.j2ee.Inject;

import java.util.ArrayList;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.SessionContext;

import javax.xml.rpc.handler.MessageContext;

/**
 * Abstract base class for an session context
 */
abstract public class AbstractStatelessContext extends AbstractContext
  implements SessionContext
{
  protected final StatelessServer _server;

  private EJBObject _remote;

  protected AbstractStatelessContext(StatelessServer server)
  {
    _server = server;
  }

  /**
   * Returns the local object in the context.
   */
  // XXX public EJBLocalObject createLocalObject()
  public Object createLocalObject()
    throws IllegalStateException
  {
    throw new IllegalStateException(L.l("`{0}' has no local interface.  Local beans need a local-home and a local interface.  Remote beans must be called with a remote context.",
                                        getServer()));
  }

  /**
   * Returns the owning server.
   */
  public AbstractServer getServer()
  {
    return _server;
  }

  public EjbTransactionManager getTransactionManager()
  {
    return _server.getTransactionManager();
  }

  /**
   * Returns the owning server.
   */
  public StatelessServer getStatelessServer()
  {
    return _server;
  }

  /**
   * Returns the object's handle.
   */
  public Handle getHandle()
  {
    return getStatelessServer().createHandle(this);
  }

  /**
   * For session beans, returns the object.  For the home, return an
   * illegal state exception.
   */
  /*
  public EJBObject getEJBObject()
    throws IllegalStateException
  {
    if (_remote == null)
      _remote = getStatelessServer().createEJBObject(getPrimaryKey());

    return _remote;
  }
  */

  public Object getPrimaryKey()
  {
    return "::ejb:stateless";
  }

  public <T> T getBusinessObject(Class<T> businessInterface)
    throws IllegalStateException
  {
    validateBusinessInterface(businessInterface);

    Object obj = getStatelessServer().getRemoteObject(businessInterface);

    if (validateObject(obj, businessInterface))
      return (T) obj;

    // TCK: ejb30/bb/session/stateless/sessioncontext/descriptor/getInvokedBusinessInterfaceLocal1, needs QA
    obj = getStatelessServer().getLocalObject(businessInterface);

    if (validateObject(obj, businessInterface))
      return (T) obj;

    obj = getStatelessServer().getClientObject(businessInterface);

    if (validateObject(obj, businessInterface))
      return (T) obj;

    //getStatelessServer().setBusinessInterface(obj, businessInterface);

    //return (T) validateObject(obj, businessInterface);

    throw new IllegalStateException(L.l("Trying to get business object with invalid business interface: {0}",
                                        businessInterface.getName()));
  }

  private boolean validateObject(Object obj, Class businessInterface)
  {
    if (obj == null)
      return false;

    if (businessInterface == null)
      return true;

    if (businessInterface.isAssignableFrom(obj.getClass()))
      return true;

    return false;
  }

  private void validateBusinessInterface(Class businessInterface)
  {
    if (businessInterface == null)
      throw new IllegalStateException("SessionContext.getBusinessObject(null) is not allowed");

    ArrayList<Class> apiList = getStatelessServer().getRemoteApiList();

    if (apiList.contains(businessInterface))
      return;

    apiList = getStatelessServer().getLocalApiList();

    if (apiList.contains(businessInterface))
      return;

    throw new IllegalStateException(L.l("Trying to get business object with invalid business interface: {0}",
                                        businessInterface.getName()));
  }

  @Override
  public Class getInvokedBusinessInterface()
  {
    return super.getInvokedBusinessInterface();
  }

  /**
   * Obtain a reference to the JAX-RPC MessageContext
   */
  public MessageContext getMessageContext()
    throws IllegalStateException
  {
    throw new IllegalStateException("Operation not supported");
  }

  /**
   * Looks up an object in the current JNDI context.
   */
  public Object lookup(String name)
  {
    if (name == null)
      throw new IllegalArgumentException("Cannot call SessionContext.lookup(null)");

    return super.lookup(name);
  }
}
