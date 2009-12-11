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

package com.caucho.ejb.protocol;

import com.caucho.ejb.server.AbstractServer;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBObject;
import java.util.logging.Logger;

/**
 * Container for EJB stubs.
 */
public abstract class ClientContainer {
  protected static L10N L = new L10N(ClientContainer.class);
  protected static Logger log
    = Logger.getLogger(ClientContainer.class.getName());
  
  static final String CLIENT_KEY = "caucho.ejb.client";

  // the unique server id
  String _serverId;
  // the home stub
  EJBLocalHome _localHomeStub;
  // the home stub
  EJBHome _remoteHomeStub;
  // cache of stubs for remote objects
  LruCache<String,EJBObject> _stubMap = new LruCache<String,EJBObject>(1024);
  // handler encoder
  protected HandleEncoder _handleEncoder;

  /**
   * Creates a new client container
   *
   * @param serverId the server's unique ID
   */
  protected ClientContainer(String serverId)
  {
    _serverId = serverId;
  }
  
  /**
   * Returns the bean's url prefix
   */
  protected String getServerId()
  {
    return _serverId;
  }

  /**
   * Returns the home stub for the container.
   *
   * @return the bean's home stub
   */
  public EJBHome getHomeStub()
    throws Exception
  {
    if (_remoteHomeStub != null)
      return _remoteHomeStub;

    _remoteHomeStub = createHomeStub();

    return _remoteHomeStub;
  }

  /**
   * Returns the home stub for the container.
   *
   * @return the bean's home stub
   */
  public Object getEJBLocalHome()
  {
    AbstractServer jvmServer = EjbProtocolManager.getJVMServer(_serverId);

    return jvmServer.getLocalObject(jvmServer.getLocalHomeClass());
  }

  /**
   * Creates the home stub for the container.
   *
   * @return the bean's home stub
   */
  abstract protected EJBHome createHomeStub()
    throws Exception;

  public HandleEncoder getHandleEncoder(AbstractHandle handle)
  {
    if (_handleEncoder == null)
      _handleEncoder = new HandleEncoder("foo");

    return _handleEncoder;
  }
  /**
   * Returns the object key from a handle.
   */
  public Class getPrimaryKeyClass()
  {
    return String.class;
  }

  /**
   * Returns a remote stub for the given handle
   *
   * @param handle the handle for the remote bean
   *
   * @return the bean's remote stub
   */
  public EJBObject getObjectStub(String url)
    throws Exception
  {
    EJBObject stub = _stubMap.get(url);
    if (stub != null)
      return stub;

    stub = createObjectStub(url);

    _stubMap.put(url, stub);

    return stub;
  }

  /**
   * Creates the stub for the remote object for the given Handle.
   *
   * @param handle the handle for the remote object
   *
   * @return the bean's remote stub
   */
  abstract protected EJBObject createObjectStub(String url)
    throws Exception;

  /**
   * Returns a printable version of the client container
   */
  public String toString()
  {
    return "ClientContainer[" + _serverId + "]";
  }
}
