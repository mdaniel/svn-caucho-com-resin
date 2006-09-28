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

package com.caucho.soa.rest;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.bind.JAXBException;

import com.caucho.config.ConfigException;

import com.caucho.soa.WebService;

import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.webapp.ServletContextImpl;

import com.caucho.util.CauchoSystem;

/**
 * An HTTP Servlet to dispatch REST requests
 */
public class RestServletMapping extends ServletMapping {
  private static final Logger log = 
      Logger.getLogger(RestServletMapping.class.getName());

  private static final String SERVICE = "REST_SERVICE";
  private static final String SKELETON = "REST_SKELETON";

  private Object _service;
  private WebService _webService;
  private RestSkeleton _skeleton;

  private StringBuilder _jaxbPackages = new StringBuilder();
  private ArrayList<Class> _jaxbClasses = new ArrayList<Class>();

  public RestServletMapping(WebService webService)
    throws ServletException
  {
    super.setServletClass(RestServlet.class.getName());

    _webService = webService;
  }

  public void setService(Object service)
    throws ClassNotFoundException, JAXBException
  {
    _service = service;

    if (_jaxbClasses.size() > 0)
      _skeleton = new RestSkeleton(_service, 
                                   _jaxbClasses.toArray(new Class[0]));
    else if (_jaxbPackages.length() > 0)
      _skeleton = new RestSkeleton(_service, _jaxbPackages.toString());
    else
      _skeleton = new RestSkeleton(_service);
  }

  public void addJaxbPackage(String jaxbPackage)
    throws ConfigException
  {
    if (_jaxbClasses.size() > 0) {
      throw new ConfigException("Cannot set <jaxb-package> and " +
                                "<jaxb-class> simultaneously");
    }

    if (_jaxbPackages.length() > 0)
      _jaxbPackages.append(':');

    _jaxbPackages.append(jaxbPackage);
  }

  public void addJaxbClass(Class jaxbClass)
    throws ConfigException
  {
    if (_jaxbPackages.length() > 0) {
      throw new ConfigException("Cannot set <jaxb-package> and " +
                                "<jaxb-class> simultaneously");
    }

    _jaxbClasses.add(jaxbClass);
  }

  @PostConstruct
  public void init()
    throws ServletException
  {
    _webService.getWebApp().addServletMapping(this);
  }

  public ServletContext getServletContext()
  {
    ServletContext context = super.getServletContext();

    if (context == null)
      context = new ServletContextImpl();

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
      RestSkeleton skeleton = 
        (RestSkeleton) getServletContext().getAttribute(SKELETON);

      Object service = getServletContext().getAttribute(SERVICE);

      Map<String,String> queryArguments = new HashMap<String,String>();

      if (req.getQueryString() != null)
        queryToMap(req.getQueryString(), queryArguments);

      String[] pathArguments = null;

      if (req.getPathInfo() != null) {
        String pathInfo = req.getPathInfo();

        // remove the initial and final slashes
        int startPos = 0;
        int endPos = pathInfo.length();

        if (pathInfo.length() > 0 && pathInfo.charAt(0) == '/')
          startPos = 1;

        if (pathInfo.length() > startPos && 
            pathInfo.charAt(pathInfo.length() - 1) == '/')
          endPos = pathInfo.length() - 1;

        pathInfo = pathInfo.substring(startPos, endPos);

        pathArguments = pathInfo.split("/");

        if (pathArguments.length == 1 && pathArguments[0].length() == 0)
          pathArguments = new String[0];
      }
      else
        pathArguments = new String[0];

      try {
        skeleton.invoke(service, req.getMethod(), pathArguments, queryArguments,
                        req, req.getInputStream(), resp.getOutputStream());
      } catch (Throwable e) {
        throw new ServletException(e);
      }
    }

    private static void queryToMap(String query, 
                                   Map<String,String> queryArguments)
    {
      String[] entries = query.split("&");

      for (String entry : entries) {
        if (entry.indexOf("=") < 0)
          continue;

        String[] nameValue = entry.split("=", 2);

        queryArguments.put(nameValue[0], nameValue[1]);
      }
    }
  }
}
