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

package com.caucho.soa.client;

import com.caucho.soa.rest.JAXBRestProxy;

import javax.xml.bind.JAXBException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;

public class JAXBRestEncodingProxyFactory implements EncodingProxyFactory {
  public Object getProxy(Class serviceInterface, String url) 
    throws MalformedURLException, JAXBException
  {
    return Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                                  new Class[] { serviceInterface },
                                  new JAXBRestProxy(serviceInterface, url));
  }

  public Object getProxy(Class serviceInterface, String url, 
                         String jaxbPackages) 
    throws MalformedURLException, JAXBException
  {
    JAXBRestProxy proxy = 
      new JAXBRestProxy(serviceInterface, url, jaxbPackages);

    return Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                                  new Class[] { serviceInterface }, proxy);
  }

  public Object getProxy(Class serviceInterface, String url,
                         Class[] jaxbClasses)
    throws MalformedURLException, JAXBException
  {
    JAXBRestProxy proxy = new JAXBRestProxy(serviceInterface, url, jaxbClasses);

    return Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                                  new Class[] { serviceInterface }, proxy);
  }
}
