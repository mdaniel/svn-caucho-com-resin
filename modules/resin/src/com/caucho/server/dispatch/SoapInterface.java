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

package com.caucho.server.dispatch;

import com.caucho.soap.servlets.WebServiceServlet;
import com.caucho.server.dispatch.*;
import com.caucho.server.webapp.*;

import com.caucho.config.types.InitParam;
import com.caucho.config.types.InitProgram;
import com.caucho.config.types.RawString;

import javax.servlet.*;
import javax.servlet.Servlet;

import javax.servlet.jsp.el.ELException;

/**
 * A child of &lt;web-service&gt; that describes a SOAP interface to
 * the service.
 */
public class SoapInterface implements ServiceInterface {
  private SoapServletMapping _servlet;
  private SoapJmsTransport _soapJmsTransport;
  private Class _interfaceClass;
  private WebService _webService;

  public SoapInterface(WebService webService)
  {
    _webService = webService;
  }

  WebService getWebService()
  {
    return _webService;
  }

  public void setInterfaceClass(String interfaceClassName)
    throws ClassNotFoundException
  {
    _interfaceClass = _webService.loadClass(interfaceClassName);
  }

  Class getInterfaceClass()
  {
    return _interfaceClass;
  }

  public SoapServletMapping createServlet()
    throws ServletException
  {
    _servlet = new SoapServletMapping(this);
    return _servlet;
  }

  public void setServlet(SoapServletMapping servlet)
    throws ServletException
  {
    servlet.setStrictMapping(_webService.getWebApp().getStrictMapping());

    _webService.getWebApp().addServletMapping(servlet);

    _servlet = servlet;
  }

  public SoapJmsTransport createJms()
  {
    _soapJmsTransport = new SoapJmsTransport(this);
    return _soapJmsTransport;
  }
}
