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

import javax.servlet.ServletException;

/**
 * A child of &lt;web-service&gt; that describes a published interface to
 * the service.
 */
public class HessianInterface implements ServiceInterface {
  private Class _apiClass;
  private HessianJmsTransport _hessianJmsTransport;
  private HessianServletMapping _servlet;
  private WebService _webService;

  public HessianInterface(WebService webService)
  {
    _webService = webService;
  }

  WebService getWebService()
  {
    return _webService;
  }

  public void setApi(String apiClassName)
    throws ClassNotFoundException
  {
    _apiClass = _webService.loadClass(apiClassName);
  }

  Class getAPIClass()
  {
    return _apiClass;
  }

  public HessianServletMapping createServlet()
    throws ServletException
  {
    _servlet = new HessianServletMapping(this);

    return _servlet;
  }

  public void setServlet(HessianServletMapping servlet)
    throws ServletException
  {
    servlet.setStrictMapping(_webService.getWebApp().getStrictMapping());

    _webService.getWebApp().addServletMapping(servlet);

    _servlet = servlet;
  }

  public HessianJmsTransport createJms()
  {
    _hessianJmsTransport = new HessianJmsTransport(this);

    return _hessianJmsTransport;
  }
}
