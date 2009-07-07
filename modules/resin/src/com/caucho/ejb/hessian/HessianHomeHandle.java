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

package com.caucho.ejb.hessian;

import com.caucho.ejb.RemoteExceptionWrapper;
import com.caucho.ejb.protocol.AbstractHomeHandle;
import com.caucho.ejb.protocol.ClientContainer;

import javax.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * Implementation for a home handle.
 */
public class HessianHomeHandle extends AbstractHomeHandle {
  private transient ClientContainer client;
  private transient EJBHome home;

  private String url;

  /**
   * Null arg constructor for serialization.
   */
  private HessianHomeHandle()
  {
  }

  /**
   * Creates a new HomeHandle.
   *
   * @param url the url for the bean
   */
  public HessianHomeHandle(String url)
  {
    this.url = url;
  }

  /**
   * Creates a new HomeHandle.
   *
   * @param url the url for the bean
   */
  public HessianHomeHandle(EJBHome home, String url)
  {
    this.url = url;
    this.home = home;
  }
  
  /**
   * Returns the EJBHome object associated with the handle.
   */
  public EJBHome getEJBHome()
    throws RemoteException
  {
    if (home == null) {
      try {
        home = HessianClientContainer.find(url).getHomeStub();
      } catch (Exception e) {
        throw RemoteExceptionWrapper.create(e);
      }
    }
    
    return home;
  }

  /**
   * Returns the handle's server id.
   */
  public String getServerId()
  {
    return url;
  }

  /**
   * Returns the full URL
   */
  public String getURL(String protocol)
  {
    return url;
  }

  /**
   * The printed representation of the handle is the url.
   */
  public String toString()
  {
    return url;
  }
}
