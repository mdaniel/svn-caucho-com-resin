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

package com.caucho.ejb.protocol;

import com.caucho.ejb.RemoteExceptionWrapper;
import com.caucho.ejb.server.AbstractServer;

import javax.ejb.EJBObject;
import java.rmi.RemoteException;
  
/**
 * Handle implementation for EJB Objects.
 *
 * <code><pre>
 * String url = "http://localhost:8080/ejb/houses?ejbid=Gryffindor";
 * HandleImpl handle = new HandleImpl(url);
 *
 * test.RemoteBean bean = (test.RemoteBean) handle.getEJBObject();
 * </pre></code>
 */
public class HandleImpl extends AbstractHandle {
  private String _serverId;
  private String _objectId;

  private transient EJBObject _object; 

  /**
   * Null constructor for serialization.
   */
  public HandleImpl() {}

  /**
   * Create a new HandleImpl
   *
   * @param serverId the object's server
   * @param objectKey the object id
   */
  public HandleImpl(String serverId, String objectId)
  {
    _serverId = serverId;
    _objectId = objectId;
  }

  /**
   * Return the URL prefix for the bean's home.
   */
  public String getServerId()
  {
    return _serverId;
  }

  /**
   * Returns the URL suffix used as a primary key.
   */
  public String getObjectId()
  {
    return _objectId;
  }

  public String getURL(String protocol)
  {
    AbstractServer server = EjbProtocolManager.getJVMServer(_serverId);
    
    return server.getHandleEncoder(protocol).getURL(_objectId);
  }

  public EJBObject getEJBObject()
    throws RemoteException
  {
    if (_object == null) {
      try {
        /* XXX: webbeans
        SameJVMClientContainer client;
        client = SameJVMClientContainer.find(_serverId);

        if (client != null)
          _object = client.createObjectStub(_objectId);
         */
      } catch (Exception e) {
        RemoteExceptionWrapper.create(e);
      }
    }

    return _object;
  }

  /**
   * Returns true if the test handle refers to the same object.
   * In this implementation, the handles are identical iff the urls
   * are identical.
   */
  public boolean equals(Object b)
  {
    if (! (b instanceof HandleImpl))
      return false;

    HandleImpl handle = (HandleImpl) b;

    return (_serverId.equals(handle._serverId) &&
            _objectId.equals(handle._objectId));
  }

  /**
   * The handle's hashcode is the same as the url's hashcode
   */
  public int hashCode()
  {
    return 65521 * _serverId.hashCode() + _objectId.hashCode();
  }

  /**
   * The printed representation of the handle is the url.
   */
  public String toString()
  {
    return "HandleImpl[" + _serverId + "," + _objectId + "]";
  }
}
