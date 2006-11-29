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

import javax.ejb.EJBLocalHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Special class for dealing with serialization.
 */
class LocalHomeSkeletonWrapper implements Serializable {
  protected String serverId;

  /**
   * Null-arg constructor for serialization
   */
  public LocalHomeSkeletonWrapper()
  {
  }

  /**
   * Constructor for the HomeSkeleton
   *
   * @param localId the local id for this home object.
   */
  public LocalHomeSkeletonWrapper(String serverId)
  {
    this.serverId = serverId;
  }

  /**
   * Replace with the real skeleton.
   */
  public Object readResolve()
    throws ObjectStreamException
  {
    try {
      Context cmp = (Context) new InitialContext().lookup("java:comp/env/cmp");
      String name = serverId;
      if (serverId.startsWith("/"))
        name = serverId.substring(1);
      
      EJBLocalHome home = (EJBLocalHome) cmp.lookup(name);
        
      if (home == null)
        throw new ObjectExceptionWrapper("no local ejb " + serverId);
      
      return home;
    } catch (Exception e) {
      throw new ObjectExceptionWrapper(e);
    }
  }
}


