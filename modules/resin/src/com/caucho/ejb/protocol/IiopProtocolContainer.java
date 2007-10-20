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

package com.caucho.ejb.protocol;

import com.caucho.config.ConfigException;
import com.caucho.ejb.AbstractServer;
import com.caucho.iiop.IiopContext;
import com.caucho.iiop.IiopRemoteService;
import com.caucho.util.L10N;

import javax.ejb.EJBHome;
import java.util.logging.Logger;

/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class IiopProtocolContainer extends ProtocolContainer {
  private static final Logger log
    = Logger.getLogger(IiopProtocolContainer.class.getName());
  private static final L10N L = new L10N(IiopProtocolContainer.class);

  private IiopContext _context;

  private IiopProtocolContainer(IiopContext context)
  {
    _context = context;
  }

  /**
   * Creates the IIOP protocol server if IIOP is available.
   */
  public static IiopProtocolContainer createProtocolContainer()
  {
    IiopContext context = IiopContext.getLocalContext();

    if (context != null)
      return new IiopProtocolContainer(context);
    else
      return null;
  }

  /**
   * Creates the IIOP protocol server if IIOP is available.
   */
  public static EJBHome findRemoteEJB(String ejbName)
  {
    IiopContext context = IiopContext.getLocalContext();

    if (context == null)
      return null;

    if (! ejbName.startsWith("/"))
      ejbName = "/" + ejbName;

    IiopRemoteService service = context.getService(ejbName);

    if (service != null)
      return (EJBHome) service.getHome();
    else
      return null;
  }

  public String getName()
  {
    return "iiop";
  }

  /**
   * Exports a server to iiop.
   */
  @Override
  public void addServer(AbstractServer server)
  {
    if (server.getRemoteObject() == null)
      return;

    String name = getName(server);

    log.fine("iiop: add server " + name);

    EjbIiopRemoteService service = new EjbIiopRemoteService(server);

    _context.setService(name, service);

    String remoteJndiName = name;

    // TCK (ejb/0f6f)
    // Multiple remote interfaces
    for (Class cl : server.getRemoteObjectList()) {
      String s = cl.getName().replace(".", "_");

      name = remoteJndiName + "#" + s;

      log.fine("iiop: add server " + name);

      service = new EjbIiopRemoteService(server, cl);

      boolean isEJB3 = true;

      if (server.getRemote21() != null) {
        if (server.getRemote21().getName().equals(cl.getName())) {
          isEJB3 = false;
        }
      }

      service.setEJB3(isEJB3);

      _context.setService(name, service);
    }
  }

  private String getName(AbstractServer server)
  {
    String name = server.getProtocolId();
    if (name == null)
      name = server.getEJBName();

    name = name.replace('.', '_'); // XXX:

    if (! name.startsWith("/"))
      name = "/" + name;

    return name;
  }

  /**
   * Removes a server from iiop.
   */
  public void removeServer(AbstractServer server)
  {
    if (server.getRemoteObject() == null)
      return;

    String name = getName(server);

    _context.removeService(name);
  }

  protected HandleEncoder createHandleEncoder(AbstractServer server,
                                              Class primaryKeyClass)
    throws ConfigException
  {
    String name = getName(server);

    if (_urlPrefix != null)
      return new HandleEncoder(server, _urlPrefix + name);
    else
      return new HandleEncoder(server, name);
  }

  /**
   * Returns the skeleton
   */
  public Skeleton getSkeleton(String uri, String queryString)
    throws Exception
  {
    throw new UnsupportedOperationException();
  }
}
