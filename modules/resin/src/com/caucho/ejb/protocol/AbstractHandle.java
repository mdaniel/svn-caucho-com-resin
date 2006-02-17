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

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.ejb.Handle;
import javax.ejb.EJBObject;

import com.caucho.log.Log;

/**
 * Handle implementation for EJB Objects.
 *
 * <code><pre>
 * String url = "http://localhost:8080/ejb/houses/Gryffindor";
 * HandleImpl handle = new HandleImpl(url);
 *
 * test.RemoteBean bean = (test.RemoteBean) handle.getEJBObject();
 * </pre></code>
 */
abstract public class AbstractHandle implements Handle {
  protected static final Logger log = Log.open(AbstractHandle.class);
  
  /**
   * Returns the server id for the handle.
   */
  // public abstract String getServerId();
  
  /**
   * Returns the object id for the handle.
   */
  public abstract String getObjectId();

  /**
   * Returns the remote API class.
   */
  public Class getType()
  {
    try {
      EJBObject obj = getEJBObject();
    
      return obj.getEJBHome().getEJBMetaData().getRemoteInterfaceClass();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      return null;
    }
  }

  /**
   * Returns the URL for a particular protocol.
   */
  abstract public String getURL(String protocol);
}
