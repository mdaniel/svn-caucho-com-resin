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

import javax.ejb.HomeHandle;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Special class for dealing with serialization.
 */
public class HomeSkeletonWrapper implements Serializable {
  protected HomeHandle handle;
  protected String localId;

  /**
   * Null-arg constructor for serialization
   */
  public HomeSkeletonWrapper()
  {
  }

  /**
   * Constructor for the HomeSkeleton
   *
   * @param handle the serializable handle for this home object.
   */
  public HomeSkeletonWrapper(HomeHandle handle)
  {
    this.handle = handle;
  }

  /**
   * Constructor for the HomeSkeleton
   *
   * @param localId the local id for this home object.
   */
  public HomeSkeletonWrapper(String localId)
  {
    this.localId = localId;
  }

  /**
   * Replace with the real skeleton.
   */
  public Object readResolve()
    throws ObjectStreamException
  {
    try {
      if (handle != null)
        return handle.getEJBHome();
      /*
      else {
        client = server.find(localId);
        if (client == null)
          throw new ObjectExceptionWrapper("no local ejb " + localId);
        else
          return client.getEJBLocalHome();
      }
      */

      throw new IllegalStateException(this + " expected a handle");
    } catch (RuntimeException e) {
      throw e;
      /*
    } catch (ObjectStreamException e) {
      throw e;
      */
    } catch (Exception e) {
      throw new ObjectExceptionWrapper(e);
    }
  }

  static class ObjectExceptionWrapper extends ObjectStreamException {
    ObjectExceptionWrapper(String msg)
    {
      super(msg);
    }
    
    ObjectExceptionWrapper(Exception e)
    {
      super(String.valueOf(e));
    }
  }
}


