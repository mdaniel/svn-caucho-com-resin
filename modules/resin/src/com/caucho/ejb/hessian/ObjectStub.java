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

package com.caucho.ejb.hessian;

import java.io.*;
import java.beans.*;
import java.lang.reflect.*;
import java.rmi.*;

import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;
import com.caucho.ejb.*;

/**
 * Base class for generated object stubs.
 */
public abstract class ObjectStub extends HessianStub implements EJBObject {
  protected transient Handle _handle;

  abstract public String getHessianType();
  
  /**
   * Returns the serializable handle for the remote object
   */
  public Handle getHandle()
    throws RemoteException
  {
    if (_handle == null)
      _handle = _client.createHandle(_url);
    
    return _handle;
  }

  /**
   * Returns the EJBHome stub for the remote object.
   */
  public EJBHome getEJBHome()
    throws RemoteException
  {
    try {
      return _client.getHomeStub();
    } catch (Exception e) {
      throw new RemoteExceptionWrapper(e);
    }
  }

  /**
   * Returns true if the test object is identical to this object.
   *
   * @param obj the object to test for identity
   */
  public boolean isIdentical(EJBObject obj)
    throws RemoteException
  {
    return getHandle().equals(obj.getHandle()) || _ejb_isIdentical(obj);
  }

  /**
   * Remove this object from the server.
   */
  public void remove()
    throws RemoteException, RemoveException
  {
    _ejb_remove();
  }

  /**
   * For entity beans, returns the primary key
   */
  public Object getPrimaryKey()
    throws RemoteException
  {
    // XXX: the primary key could be cached
    return _ejb_getPrimaryKey();
  }

  protected EJBHome _ejb_getEJBHome() throws RemoteException
  {
    return null;
  }

  protected boolean _ejb_isIdentical(EJBObject obj) throws RemoteException
  {
    return false;
  }

  protected void _ejb_remove() throws RemoteException, RemoveException
  {
  }

  protected Object _ejb_getPrimaryKey() throws RemoteException
  {
    return null;
  }
}
