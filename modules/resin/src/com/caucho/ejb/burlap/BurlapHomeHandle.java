/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 * Implementation for a home handle.
 */
public class BurlapHomeHandle extends AbstractHomeHandle {
  private String _url;
  private transient EJBHome _home;

  /**
   * Null arg constructor for serialization.
   */
  public BurlapHomeHandle() {}

  /**
   * Creates a new HomeHandle.
   *
   * @param url the url for the bean
   */
  public BurlapHomeHandle(String url)
  {
    _url = url;
  }

  /**
   * Creates a new HomeHandle.
   *
   * @param url the url for the bean
   */
  public BurlapHomeHandle(EJBHome home, String url)
  {
    _url = url;

    _home = home;
  }

  /**
   * Returns the handle's server id.
   */
  public String getServerId()
  {
    return _url;
  }
  
  /**
   * Returns the EJBHome object associated with the handle.
   */
  public EJBHome getEJBHome()
    throws RemoteException
  {
    if (_home == null) {
      try {
        _home = BurlapClientContainer.find(_url).getHomeStub();
      } catch (Exception e) {
        throw RemoteExceptionWrapper.create(e);
      }
    }
    
    return _home;
  }

  /**
   * Returns the URL for a particular protocol.
   */
  public String getURL(String protocol)
  {
    return _url;
  }

  /**
   * Returns the full URL
   */
  public String getURL()
  {
    return _url;
  }

  /**
   * Returns true if the test handle refers to the same object.
   * In this implementation, the handles are identical iff the urls
   * are identical.
   */
  public boolean equals(Object b)
  {
    if (! (b instanceof BurlapHomeHandle))
      return false;

    BurlapHomeHandle handle = (BurlapHomeHandle) b;

    return _url.equals(handle._url);
  }

  /**
   * The handle's hashcode is the same as the url's hashcode
   */
  public int hashCode()
  {
    return _url.hashCode();
  }

  /**
   * The printed representation of the handle is the url.
   */
  public String toString()
  {
    return _url;
  }
}
