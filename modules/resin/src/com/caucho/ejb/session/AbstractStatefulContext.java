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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.session;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.xa.*;
import com.caucho.naming.ObjectProxy;

import java.util.ArrayList;

import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.naming.NamingException;

/**
 * Abstract base class for an session context
 */
abstract public class AbstractStatefulContext extends AbstractSessionContext
{
  protected final StatefulServer _server;

  private String _primaryKey;
  private EJBObject _remote;

  protected AbstractStatefulContext(StatefulServer server)
  {
    _server = server;
  }

  /**
   * Returns the owning server.
   */
  public AbstractServer getServer()
  {
    return _server;
  }

  /**
   * Returns the owning server.
   */
  public SessionServer getSessionServer()
  {
    return _server;
  }

  /**
   * Returns the owning server.
   */
  public StatefulServer getStatefulServer()
  {
    return _server;
  }

  public EjbTransactionManager getTransactionManager()
  {
    return _server.getTransactionManager();
  }

  /**
   * Returns the object's handle.
   */
  @Override
  public Handle getHandle()
  {
    return getStatefulServer().createHandle(this);
  }

  /**
   * For session beans, returns the object.  For the home, return an
   * illegal state exception.
   */
  @Override
  public EJBObject getEJBObject()
    throws IllegalStateException
  {
    EJBObject obj = getRemoteView();

    if (obj == null)
      throw new IllegalStateException("getEJBObject() is only allowed through EJB 2.1 interfaces");

    return obj;
  }

  public String getPrimaryKey()
  {
    if (_primaryKey == null)
      _primaryKey = getStatefulServer().createSessionKey(this);

    return _primaryKey;
  }

  public <T> T getBusinessObject(Class<T> businessInterface)
    throws IllegalStateException
  {
    validateBusinessInterface(businessInterface);

    Object obj = getSessionServer().getRemoteObject(businessInterface);

    if (validateObject(obj, businessInterface))
      return (T) obj;

    obj = getSessionServer().getClientObject(businessInterface);

    if (obj == null)
      return null;

    if (obj instanceof ObjectProxy) {
      try {
        obj = ((ObjectProxy) obj).createObject(null);
      } catch (NamingException e) {
        throw new IllegalStateException(e);
      }
    }

    if (validateObject(obj, businessInterface))
      return (T) obj;

    //getSessionServer().setBusinessInterface(obj, businessInterface);

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

    ArrayList<Class> apiList = getSessionServer().getRemoteApiList();

    if (apiList.contains(businessInterface))
      return;

    apiList = getSessionServer().getLocalApiList();

    if (apiList.contains(businessInterface))
      return;

    throw new IllegalStateException(L.l("Trying to get business object with invalid business interface: {0}",
                                        businessInterface.getName()));
  }

  @Override
  public Class getInvokedBusinessInterface()
    throws IllegalStateException
  {
    return super.getInvokedBusinessInterface();
  }

  /**
   * Looks up an object in the current JNDI context.
   */
  @Override
  public Object lookup(String name)
  {
    // ejb/0ff2 TCK: ejb30/bb/session/stateful/sessioncontext/annotated/lookupIllegalArgumentException
    if (name == null)
      throw new IllegalArgumentException("Cannot call SessionContext.lookup(null)");

    return super.lookup(name);
  }
}
