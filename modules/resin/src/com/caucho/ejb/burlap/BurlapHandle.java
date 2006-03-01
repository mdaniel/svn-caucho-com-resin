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

package com.caucho.ejb.burlap;

import java.io.*;
import java.util.*;
import java.rmi.*;

import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;
import com.caucho.ejb.*;
import com.caucho.ejb.protocol.*;

/**
 * Handle implementation for Burlap Objects.
 *
 * <code><pre>
 * String url = "http://localhost:8080/ejb/houses/Gryffindor";
 * BurlapHandle handle = new BurlapHandle(url);
 *
 * test.RemoteBean bean = (test.RemoteBean) handle.getEJBObject();
 * </pre></code>
 */
public class BurlapHandle extends AbstractHandle {
  private String url;

  private transient String serverId;
  private transient String objectId;
  private transient Object objectKey;
  private transient EJBObject object;
  
  /**
   * Null-arg constructor for serialization.
   */
  public BurlapHandle() {}
  
  /**
   * Create a new handle.
   */
  public BurlapHandle(String url)
  {
    this.url = url;
  }
  
  /**
   * Create a new handle.
   */
  public BurlapHandle(String url, Object key)
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
        BurlapClientContainer client;
        client = BurlapClientContainer.find(getServerId());
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
        HandleEncoder encoder = BurlapClientContainer.find(getServerId()).getHandleEncoder();
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
    if (! (obj instanceof BurlapHandle))
      return false;

    BurlapHandle handle = (BurlapHandle) obj;

    return this.url.equals(handle.url);
  }

  /**
   * Returns the string.
   */
  public String toString()
  {
    return url;
  }
}
