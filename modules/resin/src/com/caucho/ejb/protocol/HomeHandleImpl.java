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

package com.caucho.ejb.protocol;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

/**
 * Implementation for a home handle.
 */
public class HomeHandleImpl extends AbstractHomeHandle {
  protected String serverId;
  protected transient EJBHome home;

  /**
   * Null arg constructor for serialization.
   */
  public HomeHandleImpl() {}

  /**
   * Creates a new HomeHandle.
   *
   * @param url the url for the bean
   */
  public HomeHandleImpl(String url)
  {
    if (url.endsWith("/"))
      url = url.substring(url.length() - 1);

    this.serverId = url;
  }

  /**
   * Creates a new HomeHandle.
   *
   * @param url the url for the bean
   */
  public HomeHandleImpl(EJBHome home, String url)
  {
    this(url);

    this.home = home;
  }

  public EJBHome getEJBHome()
    throws RemoteException
  {
    if (home == null)
      home = SameJVMClientContainer.find(serverId).getEJBHome();

    return home;
  }

  /**
   * Return the bean's server id.
   */
  public String getServerId()
  {
    return serverId;
  }

  public String getURL(String protocol)
  {
    return serverId;
  }

  /**
   * Returns the hash code for the container.
   */
  public int hashCode()
  {
    return serverId.hashCode();
  }

  /**
   * Returns true if the handle equals the object.
   */
  public boolean equals(Object o)
  {
    if (o == null || ! o.getClass().equals(getClass()))
      return false;

    HomeHandleImpl handle = (HomeHandleImpl) o;

    return serverId.equals(handle.serverId);
  }
}
