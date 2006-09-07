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

package com.caucho.esb.rest;

import java.io.IOException;

import java.util.HashSet;

import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.esb.WebService;

import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.webapp.ServletContextImpl;

/**
 * An HTTP Servlet to dispatch REST requests
 */
public class RestServletMapping extends ServletMapping {
  private static final Logger log = 
      Logger.getLogger(RestServletMapping.class.getName());

  private static final String BINDINGS = "REST_BINDINGS";
  private static final String SERVICE = "REST_SERVICE";
  private static final String SKELETON = "REST_SKELETON";

  private Object _service;
  private WebService _webService;
  private RestSkeleton _skeleton;
  private HashSet<RestBinding> _bindings = new HashSet<RestBinding>();

  public RestServletMapping(WebService webService)
    throws ServletException
  {
    super.setServletClass(RestServlet.class.getName());

    _webService = webService;
  }

  public void setService(Object service)
    throws ClassNotFoundException
  {
    _service = service;
    _skeleton = new RestSkeleton(_service);
  }

  public void setQueryBinding(String empty)
  {
    _bindings.add(new QueryBinding());
  }

  public void setPathBinding(String empty)
  {
    _bindings.add(new PathBinding());
  }

  public void init()
    throws ServletException
  {
    if (_bindings.isEmpty())
      _bindings.add(new PathBinding());

    _webService.getWebApp().addServletMapping(this);
  }

  public ServletContext getServletContext()
  {
    ServletContext context = super.getServletContext();

    if (context == null)
      context = new ServletContextImpl();

    context.setAttribute(BINDINGS, _bindings);
    context.setAttribute(SERVICE, _service);
    context.setAttribute(SKELETON, _skeleton);

    return context;
  }

  public static class RestServlet extends HttpServlet {
    private static final Logger log = 
      Logger.getLogger(RestServlet.class.getName());

    protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
    {
      HashSet<RestBinding> bindings = 
        (HashSet<RestBinding>) getServletContext().getAttribute(BINDINGS);

      RestSkeleton skeleton = 
        (RestSkeleton) getServletContext().getAttribute(SKELETON);

      Object service = getServletContext().getAttribute(SERVICE);

      boolean bindingFound = false;

      for (RestBinding binding : bindings) {
        bindingFound = binding.invoke(req, resp, skeleton, service);

        if (bindingFound)
          break;
      }

      if (! bindingFound)
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
