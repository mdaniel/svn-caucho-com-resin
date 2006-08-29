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
 * @author Emil Ong
 */

package com.caucho.esb.transport;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.esb.WebService;
import com.caucho.esb.encoding.ServiceEncoding;

import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.webapp.ServletContextImpl;

import com.caucho.util.NullOutputStream;

public class HttpTransport extends ServletMapping implements ServiceTransport {
  private static final String SERVICE_ENCODING = "service-encoding";
  private ServiceEncoding _encoding;
  private WebService _webService;

  public HttpTransport()
    throws ServletException
  {
    super.setServletClass(HttpTransportServlet.class.getName());
  }

  public void setWebService(WebService webService)
  {
    _webService = webService;
  }

  public void setEncoding(ServiceEncoding encoding)
  {
    _encoding = encoding;
  }

  public ServletContext getServletContext()
  {
    ServletContext context = super.getServletContext();

    if (context == null)
      context = new ServletContextImpl();

    context.setAttribute(SERVICE_ENCODING, _encoding);

    return context;
  }

  public void init()
    throws ServletException
  {
    _webService.getWebApp().addServletMapping(this);
  }

  public static class HttpTransportServlet extends HttpServlet {
    public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
    {
      ServiceEncoding encoding = 
        (ServiceEncoding) getServletContext().getAttribute(SERVICE_ENCODING);

      if (encoding != null)
        encoding.invoke(request.getInputStream(), response.getOutputStream());
    }
  }
}
