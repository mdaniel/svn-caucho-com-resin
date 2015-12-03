/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.cxf;

import java.io.IOException;

import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.servlet.AbstractCXFServlet;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.transport.servlet.ServletTransportFactory;

import com.caucho.server.webapp.WebServiceContextProxy;

class ResinCXFServlet extends CXFNonSpringServlet
{
  private final Class _serviceClass;
  private final Object _instance;
  private final WebServiceContext _context;

  public ResinCXFServlet(Class serviceClass, Object instance)
  {
    _serviceClass = serviceClass;
    _instance = instance;

    _context = new WebServiceContextImpl();
  }

  public void init(ServletConfig servletConfig)
    throws ServletException
  {
    super.init(servletConfig);

    Bus bus = getBus();
    BusFactory.setDefaultBus(bus);

    DestinationFactoryManager manager =
      bus.getExtension(DestinationFactoryManager.class);

    bus.setExtension(new ReadOnlyDestinationFactoryManager(manager),
                     DestinationFactoryManager.class);

    String uri = "/";

    Endpoint.publish(uri, _instance);
  }

  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException
  {
    WebServiceContext oldContext = WebServiceContextProxy.setContext(_context);

    try {
      super.service(req, res);
    } finally {
      WebServiceContextProxy.setContext(oldContext);
    }
  }
}
