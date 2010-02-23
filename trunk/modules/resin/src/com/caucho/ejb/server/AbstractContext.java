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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */
package com.caucho.ejb.server;

import java.rmi.RemoteException;
import java.security.Identity;
import java.security.Principal;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.ejb.EJBContext;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBMetaData;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.RemoveException;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

import com.caucho.security.SecurityContext;
import com.caucho.security.SecurityContextException;
import com.caucho.transaction.*;
import com.caucho.util.L10N;

/**
 * Base class for an abstract context
 */
abstract public class AbstractContext implements EJBContext {
  private static final L10N L = new L10N(AbstractContext.class);
  private static final Logger log = Logger.getLogger(AbstractContext.class
      .getName());

  private boolean _isDead;
  private String []_declaredRoles;

  @SuppressWarnings("unchecked")
  private Class _invokedBusinessInterface;

  public void setDeclaredRoles(String[] roles)
  {
    _declaredRoles = roles;
  }
  
  /**
   * Returns true if the context is dead.
   */
  public boolean isDead()
  {
    return _isDead;
  }

  /**
   * Returns the server which owns this bean.
   */
  public abstract AbstractServer getServer();

  /**
   * Returns the EJB's meta data.
   */
  public EJBMetaData getEJBMetaData()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the EJBHome stub for the container.
   */
  public EJBHome getEJBHome()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the EJBLocalHome stub for the container.
   */
  public EJBLocalHome getEJBLocalHome()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the object's handle.
   */
  public Handle getHandle()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the object's home handle.
   */
  public HomeHandle getHomeHandle()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the local object in the context.
   */
  public EJBLocalObject getEJBLocalObject() throws IllegalStateException
  {
    throw new IllegalStateException(
        L
            .l(
                "`{0}' has no local interface.  Local beans need a local-home and a local interface.  Remote beans must be called with a remote context.",
                getServer()));
  }

  /**
   * Looks up an object in the current JNDI context.
   */
  public Object lookup(String name)
  {
    return getServer().lookup(name);
  }

  /**
   * Returns the EJBObject stub for the container.
   */
  public EJBObject getEJBObject()
  {
    EJBObject obj = getRemoteView();

    if (obj == null)
      throw new IllegalStateException(
          "getEJBObject() is only allowed through EJB 2.1 interfaces");

    return obj;

    /*
     * throw newIllegalStateException(L.l(
     * "`{0}' has no remote interface.  Remote beans need a home and a remote interface.  Local beans must be called with a local context."
     * , getServer().getEJBName()));
     */
  }

  /**
   * Returns the underlying bean
   */
  public EJBObject getRemoteView()
  {
    return null;

    /*
     * throw newIllegalStateException(L.l(
     * "`{0}' has no remote interface.  Remote beans need a home and a remote interface.  Local beans must be called with a local context."
     * , getServer()));
     */
  }

  /**
   * Create the home view.
   */
  public EJBHome createRemoteHomeView()
  {
    return null;
    /*
     * throw newIllegalStateException(L.l(
     * "`{0}' has no remote interface.  Remote beans need a home and a remote interface.  Local beans must be called with a local context."
     * , getServer().getEJBName()));
     */
  }

  /**
   * Create the local home view.
   */
  public EJBLocalHome createLocalHome()
  {
    return null;
    /*
     * throw newIllegalStateException(L.l(
     * "`{0}' has no local interface.  Local beans need a local-home and a local interface.  Remote beans must be called with a remote context."
     * , getServer().getEJBName()));
     */
  }

  /**
   * Create the 2.1 remote view.
   */
  public Object createRemoteView21()
  {
    return null;
  }

  /**
   * Create the 3.0 remote view.
   */
  public Object createRemoteView()
  {
    return null;
  }

  /**
   * Obsolete method which returns the EJB 1.0 environment.
   */
  public Properties getEnvironment()
  {
    return new Properties();
  }

  /**
   * Obsolete method returns null.
   */
  public Identity getCallerIdentity()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the principal
   */
  public Principal getCallerPrincipal()
  {
    try {
      return SecurityContext.getUserPrincipal();
    } catch (SecurityContextException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Obsolete method returns false.
   */
  public boolean isCallerInRole(Identity role)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if the caller is in the named role.
   */
  public boolean isCallerInRole(String roleName)
  {
    for (String role : _declaredRoles) {
      if (roleName.equals(role))
        return SecurityContext.isUserInRole(roleName);
    }
    
    return false;
  }

  public void remove() throws RemoveException
  {
    EJBObject obj = null;
    try {
      obj = getEJBObject();
    } catch (Exception e) {
    }

    try {
      if (obj != null) {
        obj.remove();
        return;
      }
    } catch (RemoteException e) {
    }

    EJBLocalObject local = null;
    try {
      local = getEJBLocalObject();
    } catch (Exception e) {
    }

    if (local != null) {
      local.remove();
      return;
    }
  }

  /**
   * Returns the current UserTransaction. Only Session beans with bean-managed
   * transactions may use this.
   */
  public UserTransaction getUserTransaction() throws IllegalStateException
  {
    if (getServer().isContainerTransaction())
      throw new IllegalStateException(
          "getUserTransaction() is not allowed with container-managed transaction");

    return getServer().getUserTransaction();
  }

  /**
   * Looks the timer service.
   */
  public TimerService getTimerService() throws IllegalStateException
  {
    return getServer().getTimerService();
  }

  /**
   * Forces a rollback of the current transaction.
   */
  public void setRollbackOnly() throws IllegalStateException
  {
    if (! getServer().isContainerTransaction()) {
      throw new IllegalStateException(L.l("setRollbackOnly() is only allowed with container-managed transaction"));
    }

    try {
      getServer().getUserTransaction().setRollbackOnly();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns true if the current transaction will rollback.
   */
  public boolean getRollbackOnly() throws IllegalStateException
  {
    if (! getServer().isContainerTransaction())
      throw new IllegalStateException("getRollbackOnly() is only allowed with container-managed transaction");

    TransactionImpl xa = TransactionManagerImpl.getLocal().getCurrent();

    if (xa != null)
      return xa.isRollbackOnly();
    else
      throw new IllegalStateException(L.l("getRollbackOnly() called with no active transaction."));
  }

  /**
   * Destroy the context.
   */
  public void destroy() throws Exception
  {
    _isDead = true;
  }

  public Class getInvokedBusinessInterface() throws IllegalStateException
  {
    if (_invokedBusinessInterface == null)
      throw new IllegalStateException(
          "SessionContext.getInvokedBusinessInterface() is only allowed through EJB 3.0 interfaces");

    return _invokedBusinessInterface;
  }

  public void __caucho_setInvokedBusinessInterface(
      Class invokedBusinessInterface)
  {
    _invokedBusinessInterface = invokedBusinessInterface;
  }

  /**
   * Runs the timeout callbacks.
   */
  public void __caucho_timeout_callback(javax.ejb.Timer timer)
  {
    throw new IllegalStateException(L.l(
        "'{0}' does not have a @Timeout callback", getClass().getName()));
  }

  /**
   * Runs the timeout callbacks.
   */
  public void __caucho_timeout_callback(Method method)
    throws IllegalAccessException, InvocationTargetException
  {
    throw new IllegalStateException(L.l(
        "'{0}' does not have a @Timeout callback", getClass().getName()));
  }

  /**
   * Runs the timeout callbacks.
   */
  public void __caucho_timeout_callback(Method method,
                                        javax.ejb.Timer timer)
    throws IllegalAccessException, InvocationTargetException
  {
    throw new IllegalStateException(L.l(
        "'{0}' does not have a @Timeout callback", getClass().getName()));
  }
}
