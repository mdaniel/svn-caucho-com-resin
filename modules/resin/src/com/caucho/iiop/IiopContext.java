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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.Log;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * IIOP EJB binding for the local context.
 */
public class IiopContext {
  private static final Logger log = Log.open(IiopContext.class);

  private static final EnvironmentLocal<IiopContext> _localIiop =
    new EnvironmentLocal<IiopContext>();

  private HashMap<String,IiopRemoteService> _serviceMap
    = new HashMap<String,IiopRemoteService>();

  /**
   * Returns the local context.
   */
  public static IiopContext getLocalContext()
  {
    return _localIiop.get();
  }

  /**
   * Sets the local context.
   */
  public static void setLocalContext(IiopContext context)
  {
    _localIiop.set(context);
  }

  /**
   * Sets the service.
   */
  public void setService(String url, IiopRemoteService service)
  {
    log.fine("IIOP: add-service " + url);
    
    _serviceMap.put(url, service);
  }

  /**
   * Sets the service.
   */
  public void removeService(String url)
  {
    log.fine("IIOP: remove-service " + url);
    _serviceMap.remove(url);
  }

  /**
   * Returns the service.
   */
  public IiopRemoteService getService(String url)
  {
    return _serviceMap.get(url);
  }
}
