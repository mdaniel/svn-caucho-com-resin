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
 * @author Adam Megacz, Emil Ong
 */

package com.caucho.server.dispatch;

import java.util.ArrayList;

import com.caucho.config.types.InitProgram;
import com.caucho.server.webapp.WebApp;

/**
 * A Web Service entry in web.xml (Caucho-specific)
 */
public class WebService {
  private WebApp _webApp;
  private String _serviceClass;
  private ArrayList<ServiceInterface> _interfaces 
    = new ArrayList<ServiceInterface>();
  private InitProgram _init;
  private Object _service;

  /**
   * Creates a new web service object.
   */
  public WebService(WebApp webApp)
  {
    _webApp = webApp;
  }

  WebApp getWebApp()
  {
    return _webApp;
  }

  public void setClass(String serviceClass)
  {
    _serviceClass = serviceClass;
  }

  public void setInit(InitProgram init)
  {
    _init = init;
  }

  public HessianInterface createHessian()
  {
    HessianInterface hessianInterface = new HessianInterface(this);
    _interfaces.add(hessianInterface);

    return hessianInterface;
  }

  public SoapInterface createSoap()
  {
    SoapInterface soapInterface = new SoapInterface(this);
    _interfaces.add(soapInterface);

    return soapInterface;
  }

  public RestInterface createRest()
  {
    RestInterface restInterface = new RestInterface(this);
    _interfaces.add(restInterface);

    return restInterface;
  }

  Object getServiceInstance()
  {
    if (_service != null)
      return _service;

    if (_serviceClass == null)
      return null;

    try {
      Class cl = loadClass(_serviceClass);

      if (_init != null)
        _service = _init.create(cl);
      else
        _service = cl.newInstance();

      return _service;
    } catch (Throwable e) {
      return null;
    }
  }

  Class loadClass(String className)
    throws ClassNotFoundException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    if (loader != null)
      return Class.forName(className, false, loader);
    else
      return Class.forName(className);
  }
}
