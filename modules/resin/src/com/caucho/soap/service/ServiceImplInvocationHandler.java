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
import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.ws.*;
import javax.xml.stream.*;
import com.caucho.vfs.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ServiceImplInvocationHandler implements InvocationHandler {

  private Class _api;
  private DirectSkeleton _skeleton;
  private String _url;

  public ServiceImplInvocationHandler(Class api, String url)
    throws com.caucho.config.ConfigException, JAXBException
  {
    _api = api;
    _url = url;
    _skeleton = new WebServiceIntrospector().introspect(api, url);
  }

  public Object invoke(Object proxy, Method method, Object[] args)
    throws IOException, XMLStreamException, MalformedURLException
  {
    Object ret = _skeleton.invoke(method.getName(), _url, args);

    return ret == null ? new Integer(12) : ret;
  }
}
