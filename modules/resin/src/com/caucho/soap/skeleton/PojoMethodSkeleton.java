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

package com.caucho.soap.skeleton;

import java.lang.reflect.*;
import java.lang.annotation.*;

import java.io.*;
import java.util.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;

import javax.jws.*;
import javax.jws.soap.*;

import com.caucho.vfs.*;

import com.caucho.soap.marshall.*;

/**
 * Invokes a SOAP request on a Java POJO method
 */
public class PojoMethodSkeleton {

  private final Method _method;
  private final QName _resultName;
  private boolean  _wrapped = true;

  private final ParameterMarshall []_argMarshall;
  private final HashMap<String,ParameterMarshall> argNames =
    new HashMap<String,ParameterMarshall>();

  private final Marshall _retMarshall;

  private static class ParameterMarshall {
    public int _arg;
    public Marshall _marshall;
    public String _name = "arg";
    public ParameterMarshall(int arg, Marshall marshall) {
      this._arg = arg;
      this._marshall = marshall;
    }
    public void serialize(Object o, WriteStream out)
      throws IOException, XMLStreamException
    {
      _marshall.serialize(out, o, new QName(_name));
    }
  }

  public PojoMethodSkeleton(Method method, MarshallFactory factory)
  {
    Class clazz = method.getDeclaringClass();
    if (clazz.isAnnotationPresent(SOAPBinding.class))
      _wrapped =
        ((SOAPBinding)clazz.getAnnotation(SOAPBinding.class)).parameterStyle()
        == SOAPBinding.ParameterStyle.WRAPPED;
    
    if (method.isAnnotationPresent(SOAPBinding.class))
      _wrapped =
        ((SOAPBinding)method.getAnnotation(SOAPBinding.class)).parameterStyle()
        == SOAPBinding.ParameterStyle.WRAPPED;
    
    _method = method;

    Class []param = _method.getParameterTypes();
    Annotation [][]annotations = _method.getParameterAnnotations();

    _argMarshall = new ParameterMarshall[param.length];

    for (int i = 0; i < param.length; i++) {

      _argMarshall[i] =
        new ParameterMarshall(i, factory.createDeserializer(param[i]));

      for(Annotation a : annotations[i]) {

        if (a instanceof WebParam) {
          WebParam webParam = (WebParam)a;

          if (!webParam.name().equals("")) {
            _argMarshall[i]._name = webParam.name();
            argNames.put(webParam.name(), _argMarshall[i]);
            //_argMarshall[i] = null;
          }

        }
      }
    }


    _retMarshall = factory.createSerializer(method.getReturnType());

    if (method.isAnnotationPresent(WebResult.class))
      _resultName =
        new QName(method.getAnnotation(WebResult.class).targetNamespace(),
                  method.getAnnotation(WebResult.class).name());
    else
      _resultName = new QName("Result");
  }
  
  /**
   * Invokes the request for a call.
   */
  public Object invoke(String name, XMLStreamReader in, WriteStream out,
                       Object[] args, String namespace)
    throws IOException, XMLStreamException
  {
    out.println("<env:Envelope");
    out.println("    xmlns:env='http://www.w3.org/2003/05/soap-enveloper'>");
    out.println("<env:Body>");
    out.println("<"+name+" xmlns='"+namespace+"'>");
    if (args != null)
      for(int i=0; i<args.length; i++) {
        if (_argMarshall[i] != null)
          _argMarshall[i].serialize(args[i], out);
      }
    out.println("</"+name+"'>");
    out.println("</env:Body>");
    out.println("</env:Envelope>");
    out.flush();
    out.close();
    return null;
  }

  /**
   * Invokes the request for a call.
   */
  public void invoke(Object service, XMLStreamReader in, WriteStream out)
    throws IOException, XMLStreamException
  {
    Object []args = new Object[_argMarshall.length];

    for (int i = 0; i < args.length; i++) {

      if (in.nextTag() != in.START_ELEMENT)
        throw new IOException("expected <argName>");

      String tagName = in.getLocalName();

      ParameterMarshall marshall = argNames.get(tagName);
      if (marshall == null)
        marshall = _argMarshall[i];

      args[marshall._arg] = marshall._marshall.deserialize(in);

      if (in.nextTag() != in.END_ELEMENT)
          throw new IOException("expected close-tag");
    }

    Object value = null;

    try {
      value = _method.invoke(service, args);
    } catch (IllegalAccessException e) {
      throw new MarshallException(e);
    } catch (InvocationTargetException e) {
      throw new MarshallException(e.getCause());
    }

    if (_wrapped) {
      out.print('<');
      out.print(getResponseName());
      out.print('>');
    }    

    _retMarshall.serialize(out, value, _resultName);
    
    if (_wrapped) {
      out.print("</");
      out.print(getResponseName());
      out.print('>');
    }
  }

  private String getResponseName()
  {
    return _method.getName() + "Response";
  }
}


