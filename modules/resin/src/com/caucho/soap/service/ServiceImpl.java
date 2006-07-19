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
 * @author Scott Ferguson
 */

package com.caucho.soap.service;

import com.caucho.soap.reflect.*;
import java.lang.reflect.*;
import javax.jws.*;
import com.caucho.soap.marshall.*;
import com.caucho.soap.skeleton.*;
import com.caucho.util.*;
import java.io.*;
import javax.xml.namespace.*;
import javax.xml.ws.*;
import javax.xml.stream.*;
import com.caucho.vfs.*;

public class ServiceImpl extends Service {

  private QName _serviceName;

  public ServiceImpl(QName serviceName)
  {
    super(null, serviceName);
    this._serviceName = serviceName;
  }

  public <T> T getPort(Class<T> sei)
  {
    return getPort(sei, null);
  }

  public <T> T getPort(Class<T> sei, String url)
  {
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

  private class ServiceImplInvocationHandler implements InvocationHandler {

    private Class _class;
    private DirectSkeleton _skeleton;
    private String _url;

    public ServiceImplInvocationHandler(Class c, String url)
      throws com.caucho.config.ConfigException
    {
      this._class = c;
      this._url = url;
      this._skeleton = new WebServiceIntrospector().introspect(c);
    }
    
    public Object invoke(Object proxy, Method method, Object[] args)
      throws IOException, XMLStreamException
    {
      Path path = Vfs.lookup(_url);
      ReadWritePair rwp = path.openReadWrite();
      XMLStreamReader reader =
	XMLInputFactory.newInstance()
	.createXMLStreamReader(rwp.getReadStream());

      return _skeleton.invoke(reader, rwp.getWriteStream(), args);
    }

  }

}

