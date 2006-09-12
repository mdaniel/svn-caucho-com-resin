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

import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.lang.annotation.Annotation;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.jws.WebMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class RestProxy implements InvocationHandler {
  private Class _api;
  private String _restEncoding = "path";
  private String _url;
  private RestSkeleton _skeleton;

  public RestProxy(Class api, String url)
  {
    _api = api;
    _url = url;

    if (_api.isAnnotationPresent(RestEncoding.class)) {
      RestEncoding restEncoding = 
        (RestEncoding) _api.getAnnotation(RestEncoding.class);

      _restEncoding = restEncoding.value();
    }
  }

  public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable
  {
    String httpMethod = "GET";

    if (method.isAnnotationPresent(Delete.class))
      httpMethod = "DELETE";

    if (method.isAnnotationPresent(Get.class))
      httpMethod = "GET";

    if (method.isAnnotationPresent(Post.class))
      httpMethod = "POST";

    if (method.isAnnotationPresent(Put.class))
      httpMethod = "PUT";

    if (method.isAnnotationPresent(Head.class))
      httpMethod = "HEAD";

    Class postClass = null;

    if (httpMethod.equals("POST")) {
      Annotation[][] allAnnotations = method.getParameterAnnotations();
      Class[] parameterTypes = method.getParameterTypes();

      if (allAnnotations.length > 0) {
        Annotation[] annotations = allAnnotations[allAnnotations.length-1];

        for (int i = 0; i < allAnnotations.length; i++) {
          Annotation annotation = annotations[i];

          if (annotation.annotationType().equals(PostData.class)) {
            postClass = parameterTypes[i];
            break;
          }
        }
      }
    }

    String methodName = method.getName();

    if (method.isAnnotationPresent(WebMethod.class)) {
      WebMethod webMethod = (WebMethod) method.getAnnotation(WebMethod.class);

      if (webMethod.operationName().length() > 0)
        methodName = webMethod.operationName();
    }

    StringBuilder urlBuilder = new StringBuilder(_url);
    int nonPostArgs = 0;
    
    if (args != null)
      nonPostArgs = args.length - (postClass == null ? 0 : 1);

    if (_restEncoding.equals("path")) {
      if (! _url.endsWith("/"))
        urlBuilder.append("/");

      urlBuilder.append(methodName);
      urlBuilder.append("/");

      for (int i = 0; i < nonPostArgs; i++) {
        urlBuilder.append(args[i].toString());
        urlBuilder.append("/");
      }
    } 
    else if (_restEncoding.equals("query")) {
      urlBuilder.append("?");

      urlBuilder.append("method=");
      urlBuilder.append(methodName);

      if (nonPostArgs > 0)
        urlBuilder.append("&");

      for (int i = 0; i < nonPostArgs; i++) {
        urlBuilder.append(args[i].toString());

        if (nonPostArgs - i > 1)
          urlBuilder.append("&");
      }
    }
    else
      throw new RestException("Unknown REST encoding: " + _restEncoding);

    URL url = new URL(urlBuilder.toString());
    URLConnection connection = url.openConnection();

    if (connection instanceof HttpURLConnection) {
      HttpURLConnection httpConnection = (HttpURLConnection) connection;

      httpConnection.setRequestMethod(httpMethod);
      httpConnection.setDoInput(true);

      if (postClass != null) {
        httpConnection.setDoOutput(true);

        OutputStream out = httpConnection.getOutputStream();

        JAXBContext context = 
          JAXBContext.newInstance(new Class[] { postClass });
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(args[args.length - 1], out);

        out.flush();
      }

      int code = httpConnection.getResponseCode();

      if (code == 200) {
        if (method.getReturnType() == null)
          return null;

        JAXBContext context = 
          JAXBContext.newInstance(new Class[] { method.getReturnType() });
        Unmarshaller unmarshaller = context.createUnmarshaller();

        return unmarshaller.unmarshal(httpConnection.getInputStream());
      } 
    }

    throw new RestException();
  }
}
