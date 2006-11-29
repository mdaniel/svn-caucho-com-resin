/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.protocol;

import com.caucho.ejb.AbstractServer;
import com.caucho.iiop.IiopRemoteService;
import com.caucho.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EjbIiopRemoteService extends IiopRemoteService {
  private static final Logger log = Log.open(EjbIiopRemoteService.class);
  
  private AbstractServer _server;

  public EjbIiopRemoteService(AbstractServer server)
  {
    _server = server;
  }
  
  /**
   * Returns the context class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _server.getClassLoader();
  }
  
  /**
   * Returns the home API class.
   */
  public Class getHomeAPI()
  {
    if (_server.getRemoteHomeClass() != null)
      return _server.getRemoteHomeClass();
    else
      return _server.getRemoteObjectClass();
  }

  /**
   * Returns the object API class.
   */
  public Class getObjectAPI()
  {
    return _server.getRemoteObjectClass();
  }
  
  /**
   * Returns the home object.
   */
  public Object getHome()
  {
    Object obj = _server.getHomeObject();

    if (obj != null)
      return obj;
    else
      return _server.getRemoteObject();
  }

  /**
   * Returns the object interface.
   */
  public Object getObject(String local)
  {
    try {
      return _server.getEJBObject(local);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    }
  }
}
