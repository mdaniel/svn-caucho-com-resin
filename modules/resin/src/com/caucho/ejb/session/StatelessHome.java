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

package com.caucho.ejb.session;

import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractEJBHome;
import com.caucho.ejb.protocol.HomeSkeletonWrapper;
import com.caucho.ejb.server.AbstractServer;
import com.caucho.util.Log;

import javax.ejb.*;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Abstract base class for a SessionHome.
 */
abstract public class StatelessHome extends AbstractEJBHome
  implements EJBHome, EJBLocalHome, Serializable
{
  protected static final Logger log
    = Logger.getLogger(StatelessHome.class.getName());

  protected final StatelessServer _server;

  protected StatelessHome(StatelessServer server)
  {
    _server = server;
  }

  /**
   * Returns the owning server.
   */
  public StatelessServer getStatelessServer()
  {
    return _server;
  }

  /**
   * Returns the owning server.
   */
  public StatelessServer getServer()
  {
    return _server;
  }

  /**
   * Returns the owning server.
   */
  public AbstractServer __caucho_getServer()
  {
    return _server;
  }

  /**
   * Returns the EJB's meta data.
   */
  public EJBMetaData getEJBMetaData()
  {
    return getServer().getEJBMetaData();
  }

  /**
   * Returns the home handle for the home bean.
   */
  public HomeHandle getHomeHandle()
  {
    return getServer().getHomeHandle();
  }

  /**
   * Returns the handle for the home bean.
   */
  public Handle getHandle()
  {
    //return getServer().getHandle();
    return null;
  }

  /**
   * Returns null, since primary key isn't available to the home.
   */
  public Object getPrimaryKey()
  {
    return null;
  }

  /**
   * Returns null, since the ejb object isn't available to the home.
   */
  public EJBObject getEJBObject()
  {
    return null;
  }

  /**
   * Remove the object specified by the handle.
   */
  public void remove(Handle handle)
    throws RemoveException
  {
    getServer().remove(handle);
  }

  /**
   * Remove the object specified by the primary key.
   */
  public void remove(Object primaryKey)
    throws RemoveException
  {
    getServer().remove(primaryKey);
  }

  /**
   * Serialize the HomeSkeletonWrapper in place of this object.
   *
   * @return the matching skeleton wrapper.
   */
  public Object writeReplace() throws ObjectStreamException
  {
    return new HomeSkeletonWrapper(getHomeHandle());
  }
}


