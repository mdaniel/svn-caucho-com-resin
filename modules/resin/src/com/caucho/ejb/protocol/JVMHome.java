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
 */

package com.caucho.ejb.protocol;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.RemoveException;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.AbstractEJBObject;

/**
 * Interface for stubs inside the same JVM.
 */
public abstract class JVMHome extends AbstractEJBObject implements EJBHome {
  protected AbstractServer _server;
  protected Object _object;

  /**
   * Initialization code.
   */
  void _init(AbstractServer server)
  {
    _server = server;
  }
  
  /**
   * Returns the implemented class.
   */
  public Class getAPIClass()
  {
    return getServer().getRemoteHomeClass();
  }

  /**
   * Returns the URL
   */
  public String getURL(String protocol)
  {
    return getServer().getHandleEncoder(protocol).getServerId();
  }
  
  /**
   * Returns the serializable handle for the remote object
   */
  public HomeHandle getHomeHandle()
    throws RemoteException
  {
    return getServer().getHomeHandle();
  }

  /**
   * Returns the remote view of the object
   */
  protected Object _caucho_getObject()
    throws RemoteException
  {
    if (_server == null || _server.isDead()) {
      _object = null;
      _server = getServer();
    }

    if (_object == null) {
      _object = _server.getHomeObject();

      if (_object == null)
        throw new RemoteException("missing home object");

      _caucho_init_methods(_object.getClass());
    }

    return _object;
  }

  protected void _caucho_init_methods(Class cl)
  {
  }

  /**
   * Returns the server's class loader
   */
  protected ClassLoader _caucho_getClassLoader()
    throws RemoteException
  {
    return getServer().getClassLoader();
  }

  public EJBMetaData getEJBMetaData()
  {
    return getServer().getEJBMetaData();
  }

  /**
   * Returns the currently active server.  The server can change over time
   * because of class reloading.
   */
  private AbstractServer getServer()
  {
    if (_server.isDead()) {
      String serverId = _server.getHandleServerId();
      
      _object = null;
      _server = EjbProtocolManager.getJVMServer(serverId);
    }

    return _server;
  }

  public void remove(Object o)
    throws RemoveException, RemoteException
  {
  }

  public void remove(Handle handle)
    throws RemoveException, RemoteException
  {
  }
}
