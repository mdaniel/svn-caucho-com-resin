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

import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.RemoteExceptionWrapper;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.ejb.protocol.HandleEncoder;

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * Handle implementation for Hessian Objects.
 *
 * <code><pre>
 * String url = "http://localhost:8080/ejb/houses/Gryffindor";
 * HessianHandle handle = new HessianHandle(url);
 *
 * test.RemoteBean bean = (test.RemoteBean) handle.getEJBObject();
 * </pre></code>
 */
public class HessianHandle extends AbstractHandle {
  private String url;

  private transient String serverId;
  private transient String objectId;
  private transient Object objectKey;
  private transient EJBObject object;
  
  /**
   * Null-arg constructor for serialization.
   */
  public HessianHandle() {}
  
  /**
   * Create a new handle.
   */
  public HessianHandle(String url)
  {
    this.url = url;
  }
  
  /**
   * Create a new handle.
   */
  public HessianHandle(String url, Object key)
  {
    this.url = url;
    this.objectKey = key;
  }

  /**
   * Returns the server id
   */
  public String getServerId()
  {
    if (serverId == null) {
      int p = url.lastIndexOf('?');
      serverId = url.substring(0, p);
    }

    return serverId;
  }
  
  /**
   * Returns the object id
   */
  public String getObjectId()
  {
    if (objectId == null) {
      int p = url.lastIndexOf('?');
      objectId = url.substring(p + 7);
    }

    return objectId;
  }

  void setEJBObject(EJBObject obj)
  {
    this.object = obj;
  }

  public EJBObject getEJBObject()
    throws RemoteException
  {
    if (object == null) {
      try {
        HessianClientContainer client;
        client = HessianClientContainer.find(getServerId());
        object = client.createObjectStub(url);
      } catch (Exception e) {
        throw RemoteExceptionWrapper.create(e);
      }
    }
    
    return object;
  }
  
  /**
   * Returns the object id
   */
  public Object getObjectKey()
  {
    if (objectKey == null) {
      try {
        HandleEncoder encoder = HessianClientContainer.find(getServerId()).getHandleEncoder();
        objectKey = encoder.objectIdToKey(getObjectId());
      } catch (Exception e) {
        throw new EJBExceptionWrapper(e);
      }
    }

    return objectKey;
  }

  /**
   * Returns the url
   */
  public String getURL()
  {
    return url;
  }

  /**
   * Returns the url
   */
  public String getURL(String protocol)
  {
    return url;
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return url.hashCode();
  }

  /**
   * Returns true if equals.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof HessianHandle))
      return false;

    HessianHandle handle = (HessianHandle) obj;

    return url.equals(handle.url);
  }

  /**
   * Returns the string.
   */
  public String toString()
  {
    return url;
  }
}
