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
 * @author Scott Ferguson
 */

package com.caucho.soap.service;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class ServiceImpl extends Service {

  private QName _serviceName;

  public ServiceImpl(QName serviceName)
  {
    super(null, serviceName);
    this._serviceName = serviceName;
  }

  public <T> T getPort(Class<T> sei)
  {
    return getPort(_serviceName, sei);
  }

  public <T> T getPort(QName portName, Class<T> sei)
  {
    String url = portName.getNamespaceURI();
    try {
      InvocationHandler ih =
        new ServiceImplInvocationHandler(sei, url);
      Class proxyClass =
        Proxy.getProxyClass(sei.getClassLoader(),
                            new Class[] { sei });
      T t = (T) proxyClass
        .getConstructor(new Class[] { InvocationHandler.class })
        .newInstance(new Object[] { ih });
      
      return t;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

