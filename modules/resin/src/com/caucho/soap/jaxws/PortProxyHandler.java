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

package com.caucho.soap.jaxws;

import com.caucho.soap.skeleton.Skeleton;
import com.caucho.util.L10N;

import javax.xml.ws.BindingProvider;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Port handler
 */
public class PortProxyHandler implements InvocationHandler {
  private final static Logger log
    = Logger.getLogger(PortProxyHandler.class.getName());
  private final static L10N L = new L10N(PortProxyHandler.class);

  private final static HashMap<Method,SpecialMethod> _specialMethods
    = new HashMap<Method,SpecialMethod>();
  
  private Skeleton _skeleton;

  private HashMap<String,Object> _requestContext
    = new HashMap<String,Object>();

  private HashMap<String,Object> _responseContext
    = new HashMap<String,Object>();

  public PortProxyHandler(Skeleton skeleton)
  {
    _skeleton = skeleton;
  }

  public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable
  {
    SpecialMethod specialMethod = _specialMethods.get(method);

    if (specialMethod != null) {
      switch (specialMethod) {
        case TO_STRING:
          return "PortProxyHandler[]";
        case EQUALS:
          return false;
        case HASH_CODE:
          return System.identityHashCode(this);

        case GET_REQUEST_CONTEXT: 
          return _requestContext;

        case GET_RESPONSE_CONTEXT:
          return _responseContext;
      }
    }

    Object url = _requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);

    if (url == null)
      throw new ProtocolException("No service endpoint address specified");
    
    if (! (url instanceof String))
      throw new IllegalArgumentException("Invalid service endpoint address specified");
    
    return _skeleton.invoke(method, (String) url, args);
  }

  static {
    try {
      _specialMethods.put(Object.class.getMethod("toString",
                                                 new Class[0]),
                          SpecialMethod.TO_STRING);
    
      _specialMethods.put(Object.class.getMethod("equals",
                                                 new Class[] { Object.class }),
                          SpecialMethod.EQUALS);
    
      _specialMethods.put(Object.class.getMethod("hashCode",
                                                 new Class[0]),
                          SpecialMethod.HASH_CODE);
      
      _specialMethods.put(BindingProvider.class.getMethod("getRequestContext",
                                                          new Class[0]),
                          SpecialMethod.GET_REQUEST_CONTEXT);
      
      _specialMethods.put(BindingProvider.class.getMethod("getResponseContext",
                                                          new Class[0]),
                          SpecialMethod.GET_RESPONSE_CONTEXT);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  enum SpecialMethod {
    TO_STRING,
    EQUALS,
    HASH_CODE,
    
    GET_REQUEST_CONTEXT,
    GET_RESPONSE_CONTEXT
  };
}
