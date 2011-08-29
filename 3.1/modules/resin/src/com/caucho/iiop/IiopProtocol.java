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

package com.caucho.iiop;

import com.caucho.ejb.AbstractEJBObject;
import com.caucho.ejb.AbstractServer;
import com.caucho.log.Log;
import com.caucho.management.j2ee.J2EEManagedObject;
import com.caucho.management.j2ee.RMI_IIOPResource;
import com.caucho.naming.*;
import com.caucho.server.connection.Connection;
import com.caucho.server.port.Protocol;
import com.caucho.server.port.ServerRequest;
import com.caucho.iiop.orb.*;

import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.util.ArrayList;
import java.util.logging.*;
import javax.annotation.*;
import javax.naming.*;

/**
 * The main class for the HTTP server.
 *
 * <p>TcpServer handles the main thread control.  HttpServer just needs
 * to create the right kind of request when a new thread is spawned.
 *
 * @see com.caucho.server.TcpServer
 */
public class IiopProtocol extends Protocol {
  private static final Logger log
    = Logger.getLogger(IiopProtocol.class.getName());

  static final String COPYRIGHT =
    "Copyright (c) 1998-2008 Caucho Technology.  All rights reserved.";

  private String _protocolName = "iiop";

  private ORBImpl _orb = new ORBImpl();
  private boolean _isOrbInit;
  private CosServer _cos;

  private IiopContext _iiopContext;

  /**
   * Creates the IIOP protocol.
   */
  public IiopProtocol()
  {
    _iiopContext = new IiopContext();

    try {
      Jndi.rebindDeep("java:comp/ORB", _orb);
    } catch (NamingException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    _cos = new CosServer(this);

    IiopContext.setLocalContext(_iiopContext);
  }

  /**
   * Returns the protocol name.
   */
  public String getProtocolName()
  {
    return _protocolName;
  }

  /**
   * Sets the protocol name.
   */
  public void setProtocolName(String name)
  {
    _protocolName = name;
  }

  @PostConstruct
  public void init()
  {
    J2EEManagedObject.register(new RMI_IIOPResource(this));
  }

  public CosServer getCos()
  {
    return _cos;
  }

  public IiopSkeleton getService(String host, int port, String oid)
    throws NoSuchObjectException
  {
    return lookupService(host, port, oid);
  }

  ORBImpl getOrb(String host, int port)
  {
    if (! _isOrbInit) {
      _isOrbInit = true;
      _orb.setHost(host);
      _orb.setPort(port);
    }

    return _orb;
  }

  /**
   * Returns the service with the given URL.
   */
  private IiopSkeleton lookupService(String host, int port, String oid)
    throws NoSuchObjectException
  {
    String url;
    String local;

    int p = oid.indexOf('?');

    if (p < 0) {
      url = oid;
      local = null;
    }
    else {
      url = oid.substring(0, p);
      local = oid.substring(p + 1);
    }

    IiopRemoteService service = _iiopContext.getService(url);

    if (service == null)
      return null;
    else if (local == null) {
      // ArrayList<Class> apiList = new ArrayList<Class>();
      // apiList.add(service.getHomeAPI());

      // TCK: ejb30/bb/session/stateful/busifacedd/multipleInterfacesTest2
      // ejb/4012: multiple <business-remote> interfaces
      ArrayList<Class> apiList = service.getHomeAPI();

      Object obj = service.getHome();

      if (obj == null)
	throw new IllegalStateException(service + " does not have a valid EJB home interface");

      // Restrict the service API to the given business interface.
      // TCK: ejb30/getInvokedBusinessInterfaceRemoteIllegal, needs QA
      if (service.getRemoteInterface() != null) {
        apiList.clear();
        apiList.add(service.getRemoteInterface());

        for (Method method : obj.getClass().getDeclaredMethods()) {
          try {
            // XXX TCK
            if (method.getName().startsWith("create"))
              obj = method.invoke(obj, null);
          } catch (Exception e) {
            log.config("Remote home "+obj.getClass().getName()+" has no create method");
          }
        }
      }

      // XXX TCK: ejb30/.../remove
      if (obj instanceof AbstractEJBObject) {
        AbstractEJBObject ejb = (AbstractEJBObject) obj;

        AbstractServer server = ejb.__caucho_getServer();
        local = ejb.__caucho_getId();

        url = url + "?" + local;
      }

      return new IiopSkeleton(obj,
                              apiList,
                              service.getClassLoader(),
                              host, port, url);
    }
    else {
      Object obj = service.getObject(local);

      if (obj == null)
        return null;

      ArrayList<Class> apiList = service.getObjectAPI();

      // Restrict the service API to the given business interface.
      // TCK: ejb30/getInvokedBusinessInterfaceRemoteIllegal, needs QA
      if (service.getRemoteInterface() != null) {
        apiList.clear();
        apiList.add(service.getRemoteInterface());

        for (Method method : obj.getClass().getDeclaredMethods()) {
          try {
            // XXX TCK
            if (method.getName().startsWith("create"))
              obj = method.invoke(obj, null);
          } catch (Exception e) {
            log.config("Remote home "+obj.getClass().getName()+" has no create method");
          }
        }
      }

      return new IiopSkeleton(obj, apiList,
                              service.getClassLoader(),
                              host, port, url + '?' + local);
    }
  }

  /**
   * Create a HttpRequest object for the new thread.
   */
  public ServerRequest createRequest(Connection conn)
  {
    return new IiopRequest(this, conn);
  }
}
