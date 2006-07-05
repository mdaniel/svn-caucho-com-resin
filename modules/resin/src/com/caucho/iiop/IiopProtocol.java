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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.server.connection.Connection;

import com.caucho.server.port.Protocol;
import com.caucho.server.port.ServerRequest;

import com.caucho.server.webapp.Application;

import com.caucho.server.e_app.EarDeployController;

import com.caucho.server.host.Host;

import com.caucho.server.resin.ServletServer;

import com.caucho.server.dispatch.Invocation;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.EjbServerManager;

//import com.caucho.ejb.naming.LocalModel;
//import com.caucho.ejb.naming.LocalContext;

import com.caucho.ejb.protocol.HandleEncoder;
import com.caucho.mbeans.j2ee.J2EEAdmin;
import com.caucho.mbeans.j2ee.RMI_IIOPResource;

/**
 * The main class for the HTTP server.
 *
 * <p>TcpServer handles the main thread control.  HttpServer just needs
 * to create the right kind of request when a new thread is spawned.
 *
 * @see com.caucho.server.TcpServer
 */
public class IiopProtocol extends Protocol {
  private static final Logger log = Log.open(IiopProtocol.class);

  static final String COPYRIGHT =
    "Copyright (c) 1998-2006 Caucho Technology.  All rights reserved.";

  private String _protocolName = "iiop";
  private CosServer _cos;

  private IiopContext _iiopContext;

  /**
   * Creates the IIOP protocol.
   */
  public IiopProtocol()
  {
    _iiopContext = new IiopContext();

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

  public void init()
  {
    J2EEAdmin.register(new RMI_IIOPResource(this));

  }

  public CosServer getCos()
  {
    return _cos;
  }

  public IiopSkeleton getService(String host, int port, String oid)
  {
    return lookupService(host, port, oid);
  }

  /**
   * Returns the service with the given URL.
   */
  private IiopSkeleton lookupService(String host, int port, String oid)
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
      return new IiopSkeleton(service.getHome(), service.getHomeAPI(),
                              service.getClassLoader(),
                              host, port, url);
    }
    else {
      Object obj = service.getObject(local);

      if (obj == null)
        return null;

      return new IiopSkeleton(obj, service.getObjectAPI(),
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
