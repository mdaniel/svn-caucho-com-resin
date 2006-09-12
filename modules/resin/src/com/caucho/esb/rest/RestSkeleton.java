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

import java.io.InputStream;
import java.io.IOException;

import java.lang.annotation.Annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.caucho.util.CauchoSystem;

/**
 * A binding for REST services.
 */
public class RestSkeleton {
  private static final Logger log = 
    Logger.getLogger(RestSkeleton.class.getName());

  public static final String DELETE = "DELETE";
  public static final String GET = "GET";
  public static final String HEAD = "HEAD";
  public static final String POST = "POST";
  public static final String PUT = "PUT";

  private Map<String,Map<String,Method>> _methods 
    = new HashMap<String,Map<String,Method>>();

  public RestSkeleton(Object object)
    throws ClassNotFoundException
  {
    Class cl = object.getClass();

    if (cl.isAnnotationPresent(WebService.class)) {
      WebService webService = (WebService) cl.getAnnotation(WebService.class);

      cl = CauchoSystem.loadClass(webService.endpointInterface());
    }

    init(cl);
  }

  public RestSkeleton(Class cl)
  {
    init(cl);
  }

  private void init(Class cl)
  {
    _methods.put(null, new HashMap<String,Method>());
    _methods.put(DELETE, new HashMap<String,Method>());
    _methods.put(GET, new HashMap<String,Method>());
    _methods.put(HEAD, new HashMap<String,Method>());
    _methods.put(POST, new HashMap<String,Method>());
    _methods.put(PUT, new HashMap<String,Method>());

    Method[] methodArray = cl.getMethods();

    for (Method method : methodArray) {
      int modifiers = method.getModifiers();

      // Allow abstract for interfaces
      if (! Modifier.isStatic(modifiers) &&
          ! Modifier.isFinal(modifiers) &&
          Modifier.isPublic(modifiers)) {

        String methodName = method.getName();

        if (method.isAnnotationPresent(WebMethod.class)) {
          WebMethod webMethod = 
            (WebMethod) method.getAnnotation(WebMethod.class);

          if (webMethod.operationName().length() > 0)
            methodName = webMethod.operationName();
        }

        boolean hasHTTPMethod = false;

        if (method.isAnnotationPresent(Delete.class)) {
          _methods.get(DELETE).put(methodName, method);
          hasHTTPMethod = true;
        }

        if (method.isAnnotationPresent(Get.class)) {
          _methods.get(GET).put(methodName, method);
          hasHTTPMethod = true;
        }

        if (method.isAnnotationPresent(Post.class)) {
          _methods.get(POST).put(methodName, method);
          hasHTTPMethod = true;
        }
        
        if (method.isAnnotationPresent(Put.class)) {
          _methods.get(PUT).put(methodName, method);
          hasHTTPMethod = true;
        }

        if (method.isAnnotationPresent(Head.class)) {
          _methods.get(HEAD).put(methodName, method);
          hasHTTPMethod = true;
        }

        if (! hasHTTPMethod)
          _methods.get(null).put(methodName, method);
      }
    }
  }

  public Object invoke(Object object, String httpMethod,
                       String methodName, String[] stringArguments,
                       InputStream postData)
    throws Throwable
  {
    Method method = _methods.get(httpMethod).get(methodName);

    if (method == null)
      method = _methods.get(null).get(methodName);

    if (method == null)
      throw new NoSuchMethodException(methodName);

    boolean hasPostData = false;

    Class[] parameterTypes = method.getParameterTypes();

    if (httpMethod.equals(POST) && parameterTypes.length > 0) {
      Annotation[][] allAnnotations = method.getParameterAnnotations();

      Annotation[] annotations = allAnnotations[allAnnotations.length-1];

      for (Annotation annotation : annotations) {
        if (annotation.annotationType().equals(PostData.class)) {
          hasPostData = true;
          break;
        }
      }
    }

    if ((hasPostData && stringArguments.length != (parameterTypes.length-1)) ||
        (! hasPostData && stringArguments.length != parameterTypes.length))
      throw new IllegalArgumentException("Incorrect number of arguments");

    ArrayList arguments = new ArrayList();

    for (int i = 0; i < stringArguments.length; i++) {
      Class type = parameterTypes[i];
      String stringArgument = stringArguments[i];

      arguments.add(stringToType(type, stringArgument));
    }

    if (hasPostData) {
      Class cl = parameterTypes[parameterTypes.length - 1];

      JAXBContext context = JAXBContext.newInstance(new Class[] { cl });
      Unmarshaller unmarshaller = context.createUnmarshaller();

      arguments.add(unmarshaller.unmarshal(postData));
    }

    // XXX var args

    return method.invoke(object, arguments.toArray());
  }

  public Object invoke(Object object, String httpMethod,
                       String methodName, Map<String,String> argumentMap,
                       InputStream postData)
    throws Throwable
  {
    Method method = _methods.get(httpMethod).get(methodName);

    if (method == null)
      method = _methods.get(null).get(methodName);

    if (method == null)
      throw new NoSuchMethodException(methodName);

    ArrayList arguments = new ArrayList();
    Class[] parameterTypes = method.getParameterTypes();
    Annotation[][] allAnnotations = method.getParameterAnnotations();

    for (int i = 0; i < parameterTypes.length; i++) {
      boolean hasWebParam = false;

      Class type = parameterTypes[i];
      Annotation[] annotations = allAnnotations[i];

      for (Annotation annotation : annotations) {
        if (annotation.annotationType().equals(WebParam.class)) {
          String name = ((WebParam) annotation).name();

          if (! argumentMap.containsKey(name)) {
            throw new IllegalArgumentException("Parameter " + name + 
                                               " not found");
          }

          arguments.add(stringToType(type, argumentMap.get(name)));
          hasWebParam = true;
          break;
        }
      }

      if (! hasWebParam)
        throw new IllegalArgumentException("Unannotated parameter");
    }

    return method.invoke(object, arguments.toArray());
  }

  private static Object stringToType(Class type, String arg)
    throws Throwable
  {
    if (type.equals(Boolean.class)) {
      return new Boolean(arg);
    } 
    else if (type.equals(Byte.class)) {
      return new Byte(arg);
    } 
    else if (type.equals(Character.class)) {
      if (arg.length() != 1) {
        throw new IllegalArgumentException("Cannot convert String to type " +
                                           type.getName());
      }

      return new Character(arg.charAt(0));
    } 
    else if (type.equals(Double.class)) {
      return new Double(arg);
    } 
    else if (type.equals(Float.class)) {
      return new Float(arg);
    } 
    else if (type.equals(Integer.class)) {
      return new Integer(arg);
    } 
    else if (type.equals(Long.class)) {
      return new Long(arg);
    } 
    else if (type.equals(Short.class)) {
      return new Short(arg);
    } 
    else if (type.equals(String.class)) {
      return arg;
    }
    else 
      throw new IllegalArgumentException("Cannot convert String to type " +
                                         type.getName());
  }
}
