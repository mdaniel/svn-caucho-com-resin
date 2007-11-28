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

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EjbIiopRemoteService extends IiopRemoteService {
  private static final Logger log = Log.open(EjbIiopRemoteService.class);

  private AbstractServer _server;

  private Class _remoteInterface;

  private boolean _isEJB3;

  public EjbIiopRemoteService(AbstractServer server)
  {
    _server = server;
  }

  public EjbIiopRemoteService(AbstractServer server,
                              Class remoteInterface)
  {
    _server = server;
    _remoteInterface = remoteInterface;
  }

  /**
   * Returns true for 3.0 when multiple 2.1/3.0 interfaces are available.
   */
  public boolean isEJB3()
  {
    return _isEJB3;
  }

  /**
   * Uses the 3.0 remote interface when multiple 2.1/3.0 interfaces are available.
   */
  public void setEJB3(boolean isEJB3)
  {
    _isEJB3 = isEJB3;
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
  public ArrayList<Class> getHomeAPI()
  {
    if (_server.getRemoteHomeClass() != null) {
      ArrayList<Class> list = new ArrayList<Class>();
      list.add(_server.getRemoteHomeClass());

      return list;
    }
    else if (getRemoteInterface() != null) {
      ArrayList<Class> list = new ArrayList<Class>();
      list.add(getRemoteInterface());

      return list;
    }
    else
      return _server.getRemoteApiList();
  }

  /**
   * Returns the object API class.
   */
  public ArrayList<Class> getObjectAPI()
  {
    if ((! _isEJB3) && (_server.getRemote21() != null)) {
      ArrayList<Class> list = new ArrayList<Class>();
      list.add(_server.getRemote21());

      return list;
    }

    return _server.getRemoteApiList();
  }

  /**
   * Returns the invoked API class.
   */
  @Override
  public Class getRemoteInterface()
  {
    return _remoteInterface;
  }

  /**
   * Returns the home object.
   */
  public Object getHome()
  {
    Object obj = _server.getHomeObject();

    if (obj != null)
      return obj;

    return _server.getRemoteObject(_remoteInterface);

    // this logic would be in server
    /*
    if (_isEJB3)
      return _server.getRemoteObject(_remoteInterface);

    Object obj = _server.getHomeObject();

    if (obj == null)
      obj = _server.getRemoteObject21();
    */

  }

  /**
   * Returns the object interface.
   */
  public Object getObject(String local)
    throws NoSuchObjectException
  {
    // Move this logic to the ejb SessionServer after refactoring the 2.1 / 3.0 remote interfaces.
    try {
      // XXX TCK: ejb30/.../remove return _server.getEJBObject(local);
      return _server.getRemoteObject(local);
    } catch (javax.ejb.NoSuchEJBException e) {
      // XXX TCK: ejb30/.../remove/removeBean2
      if ((_remoteInterface == null && ! _isEJB3)
	  || (_remoteInterface != null && Remote.class.isAssignableFrom(_remoteInterface))) {
        throw new NoSuchObjectException("no matching object: " + local);
      }

      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  public String toString()
  {
    return "EjbIiopRemoteService[" + _server + "]";
  }
}
