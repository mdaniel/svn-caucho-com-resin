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

package com.caucho.esb.client;

import java.util.HashMap;
import java.util.logging.Logger;

import java.net.MalformedURLException;

import com.caucho.esb.encoding.UnknownServiceEncodingException;

public class ProxyManager {
  private static final Logger log 
    = Logger.getLogger(WebServiceClient.class.getName());

  private static final HashMap<String,EncodingProxyFactory> _factories =
    new HashMap<String,EncodingProxyFactory>();

  public static void addEncodingProxyFactory(String name, 
                                             EncodingProxyFactory factory)
  {
    _factories.put(name, factory);
  }

  public static Object getWebServiceProxy(Class serviceInterface, String url)
    throws MalformedURLException, UnknownServiceEncodingException
  {
    String[] components = url.split(":", 2);

    if (components.length < 2)
      throw new MalformedURLException(url);

    String factoryName = components[0];
    EncodingProxyFactory factory = _factories.get(factoryName);

    if (factory == null)
      throw new UnknownServiceEncodingException(factoryName);

    return factory.getProxy(serviceInterface, components[1]);
  }

  static {
    addEncodingProxyFactory("hessian", new HessianEncodingProxyFactory());
    addEncodingProxyFactory("rest", new RestEncodingProxyFactory());
    addEncodingProxyFactory("soap", new SoapEncodingProxyFactory());
    addEncodingProxyFactory("vm", new VMEncodingProxyFactory());
  }
}
