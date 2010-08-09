/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import com.caucho.util.L10N;

import javax.ejb.EJBLocalHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Special class for dealing with serialization.
 */
public class LocalSkeletonWrapper implements Serializable {
  static L10N L = new L10N(LocalSkeletonWrapper.class);
  
  protected String serverId;
  protected Object localId;

  /**
   * Null-arg constructor for serialization
   */
  public LocalSkeletonWrapper()
  {
  }

  /**
   * Constructor for ObjectSkeleton
   */
  public LocalSkeletonWrapper(String serverId, Object localId)
  {
    this.serverId = serverId;
    this.localId = localId;
  }

  /**
   * Replace with the real skeleton.
   */
  public Object readResolve()
    throws ObjectStreamException
  {
    try {
      Context cmp = (Context) new InitialContext().lookup("java:comp/env/cmp");

      EJBLocalHome home = (EJBLocalHome) cmp.lookup(serverId);

      /*
      AbstractServer server = serverContainer.getServer(serverId);
        
      if (server == null)
        throw new ObjectExceptionWrapper("no local ejb " + serverId);

      return server.getContext(localId).getEJBLocalObject();
      */
      throw new UnsupportedOperationException();
    } catch (Exception e) {
      throw new ObjectExceptionWrapper(e);
    }
  }
}


