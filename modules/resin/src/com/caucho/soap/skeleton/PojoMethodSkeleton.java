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
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.ws.*;

import javax.jws.*;
import javax.jws.soap.*;

import com.caucho.util.JAXBUtil;

import com.caucho.vfs.*;

import com.caucho.soap.marshall.*;

/**
 * Invokes a SOAP request on a Java POJO method
 */
public class PojoMethodSkeleton {
  private final static Logger log = 
    Logger.getLogger(PojoMethodSkeleton.class.getName());

  private final Method _method;
  private final QName _resultName;
  private boolean  _wrapped = true;

  private int _inputArgumentCount = 0;
  private final ParameterMarshall []_argMarshall;
  private final HashMap<String,ParameterMarshall> argNames =
    new HashMap<String,ParameterMarshall>();

  private final JAXBContext _jaxbContext;

  private final Marshall _retMarshall;

  private static class ParameterMarshall {
    public int _arg;
    public Marshall _marshall;
    public QName _name;
    public WebParam.Mode _mode = WebParam.Mode.IN;

    public ParameterMarshall(int arg, Marshall marshall) 
    {
      this._arg = arg;
      this._marshall = marshall;
    }

    public void serialize(Object o, XMLStreamWriter out)
      throws IOException, XMLStreamException
    {
      _marshall.serialize(out, o, _name);
    }
  }

  public PojoMethodSkeleton(Method method, MarshallFactory factory)
    throws JAXBException
  {
    Class cl = method.getDeclaringClass();

    // Determine whether this method is wrapped or not

    if (cl.isAnnotationPresent(SOAPBinding.class)) {
      SOAPBinding soapBinding = 
        (SOAPBinding) cl.getAnnotation(SOAPBinding.class);

      _wrapped = 
        soapBinding.parameterStyle() == SOAPBinding.ParameterStyle.WRAPPED;
    }
    
    if (method.isAnnotationPresent(SOAPBinding.class)) {
      SOAPBinding soapBinding = 
        (SOAPBinding) method.getAnnotation(SOAPBinding.class);

      _wrapped =
        soapBinding.parameterStyle() == SOAPBinding.ParameterStyle.WRAPPED;
    }
    
    _method = method;

    // Deep introspection to find all JAXB classes that might be necessary,
    // then create appropriate JAXB context

    HashSet<Class> jaxbClasses = new HashSet<Class>();

    JAXBUtil.introspectMethod(method, jaxbClasses);

    _jaxbContext = JAXBContext.newInstance(jaxbClasses.toArray(new Class[0]));

    // Get marshallers for each parameter

    Class[] params = _method.getParameterTypes();
    Annotation [][]annotations = _method.getParameterAnnotations();

    _inputArgumentCount = params.length;
    _argMarshall = new ParameterMarshall[params.length];

    for (int i = 0; i < params.length; i++) {
      Marshall marshall = factory.createDeserializer(params[i], _jaxbContext);
      _argMarshall[i] = new ParameterMarshall(i, marshall);

      String localName = "arg" + i; // As per JAX-WS spec

      for(Annotation a : annotations[i]) {
        if (a instanceof WebParam) {
          WebParam webParam = (WebParam) a;

          if (! "".equals(webParam.name()))
            localName = webParam.name();

          if ("".equals(webParam.targetNamespace()))
            _argMarshall[i]._name = new QName(localName);
          else 
            _argMarshall[i]._name = 
              new QName(localName, webParam.targetNamespace());

          if (params[i].equals(Holder.class)) {
            _argMarshall[i]._mode = webParam.mode();

            if (_argMarshall[i]._mode == WebParam.Mode.OUT)
              _inputArgumentCount--;
          }
        }
      }

      argNames.put(localName, _argMarshall[i]);
    }

    _retMarshall = 
      factory.createSerializer(method.getReturnType(), _jaxbContext);

    if (method.isAnnotationPresent(WebResult.class))
      _resultName =
        new QName(method.getAnnotation(WebResult.class).targetNamespace(),
                  method.getAnnotation(WebResult.class).name());
    else
      _resultName = new QName("return");
  }
 
  /**
   * Invokes the request for a call.
   */
  public Object invoke(String name, String url, Object[] args, String namespace)
    throws IOException, XMLStreamException, MalformedURLException
  {
    URL urlObject = new URL(url);
    URLConnection connection = urlObject.openConnection();

    // XXX HTTPS
    if (! (connection instanceof HttpURLConnection))
      return null;

    HttpURLConnection httpConnection = (HttpURLConnection) connection;

    try {
      httpConnection.setRequestMethod("POST");
      httpConnection.setDoInput(true);
      httpConnection.setDoOutput(true);

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLStreamWriter out =
        outputFactory.createXMLStreamWriter(httpConnection.getOutputStream());

      out.writeStartDocument();
      out.writeStartElement("env", "Envelope", Skeleton.SOAP_ENVELOPE);
      out.writeNamespace("env", Skeleton.SOAP_ENVELOPE);

      out.writeStartElement("env", "Body", Skeleton.SOAP_ENVELOPE);

      out.writeStartElement("m", name, namespace);
      out.writeNamespace("m", namespace);

      if (args != null) {
        for(int i = 0; i < args.length; i++) {
          if (_argMarshall[i]._mode == WebParam.Mode.IN) {
            _argMarshall[i].serialize(args[i], out);
          }
          else if (_argMarshall[i]._mode == WebParam.Mode.INOUT) {
            Holder holder = (Holder) args[i];

            _argMarshall[i].serialize(holder.value, out);
          }
        }
      }

      out.writeEndElement(); // name
      out.writeEndElement(); // Body
      out.writeEndElement(); // Envelope
      out.flush();

      if (httpConnection.getResponseCode() != 200)
        return null; // XXX more meaningful error

      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      XMLStreamReader in = 
        inputFactory.createXMLStreamReader(httpConnection.getInputStream());

      in.nextTag();

      if (! "Envelope".equals(in.getName().getLocalPart()))
        throw new IOException("expected Envelope at " + in.getName());

      in.nextTag();

      // XXX: Header

      if (! "Body".equals(in.getName().getLocalPart()))
        throw new IOException("expected Body");

      if (_wrapped) {
        in.nextTag();

        if (! getResponseName().equals(in.getName().getLocalPart()))
          throw new IOException("expected " + getResponseName());
      }

      in.nextTag();

      if (! _resultName.equals(in.getName()))
        throw new IOException("expected '" + _resultName);

      Object ret = _retMarshall.deserialize(in);

      for (int i = 0; i < _argMarshall.length - _inputArgumentCount; i++) {
        String tagName = in.getLocalName();

        ParameterMarshall marshall = argNames.get(tagName);

        if (marshall == null)
          throw new IOException("Unknown parameter <" + tagName + "/>"); // ???

        if (marshall._mode == WebParam.Mode.IN)
          throw new IOException("Received value for input parameter " + 
                                "<" + tagName + "/>");

        Object value = marshall._marshall.deserialize(in);

        ((Holder) args[marshall._arg]).value = value;

        if (i + 1 < _argMarshall.length - _inputArgumentCount)
          in.nextTag();
      }

      if (_wrapped) {
        if (in.nextTag() != in.END_ELEMENT)
          throw new IOException("expected </" + getResponseName() + ">");
      }

      if (in.nextTag() != in.END_ELEMENT)
        throw new IOException("expected </Body>");

      if (in.nextTag() != in.END_ELEMENT)
        throw new IOException("expected </Envelope>");

      return ret;
    } 
    finally {
      if (httpConnection != null)
        httpConnection.disconnect();
    }
  }

  /**
   * Invokes the request for a call.
   */
  public void invoke(Object service, XMLStreamReader in, XMLStreamWriter out)
    throws IOException, XMLStreamException
  {
    // We're starting out at the point in the input stream where the 
    // arguments are listed and the point in the output stream where
    // the results are to be written.
    
    Object[] args = new Object[_argMarshall.length];

    in.nextTag();

    if (_inputArgumentCount > 0 && in.getEventType() != in.START_ELEMENT)
      throw new IOException("expected <argName>");

    for (int i = 0; i < args.length; i++) {
      if (_argMarshall[i]._mode == WebParam.Mode.OUT) {
        args[i] = new Holder();
        continue;
      }

      String tagName = in.getLocalName();

      ParameterMarshall marshall = argNames.get(tagName);

      if (marshall == null)
        throw new IOException("Unknown parameter <" + tagName + "/>"); // ???

      Object arg = marshall._marshall.deserialize(in);

      if (_argMarshall[i]._mode == WebParam.Mode.INOUT) {
        Holder holder = new Holder();
        holder.value = arg;
        args[marshall._arg] = holder;
      } 
      else
        args[marshall._arg] = arg;

      in.nextTag();

      // The logic is strange here because JAXB consumes upto the next
      // element in the stream.  E.g. if there is a stream like the following:
      //
      //  ...
      //  <foo>
      //    <x>15</x>
      //  </foo>
      //  <bar>
      //    ...
      //  </bar>
      //  ...
      //
      // and JAXB is unmarshalling a Foo, it will leave the stream at <bar>, 
      // not </foo> when it's done.  Worse yet, it may leave the stream at
      // the end of the enclosing tag.

      if (i + 1 == args.length) {
        if (in.getEventType() != in.END_ELEMENT)
          throw new IOException("expected close-tag");
      } 
      else if ((_argMarshall[i + 1]._mode == WebParam.Mode.IN ||
                _argMarshall[i + 1]._mode == WebParam.Mode.INOUT) &&
               in.getEventType() != in.START_ELEMENT) {
        throw new IOException("expected <argName>");
      }
    }

    Object value = null;

    try {
      value = _method.invoke(service, args);
    } catch (IllegalAccessException e) {
      throw new MarshallException(e);
    } catch (InvocationTargetException e) {
      throw new MarshallException(e.getCause());
    }

    if (_wrapped)
      out.writeStartElement(getResponseName());

    _retMarshall.serialize(out, value, _resultName);

    for (int i = 0; i < args.length; i++) {
      if (_argMarshall[i]._mode == WebParam.Mode.INOUT ||
          _argMarshall[i]._mode == WebParam.Mode.OUT) {
        log.info(_argMarshall[i]._name + " = " + args[i]);
        log.info(_argMarshall[i]._name + " = " + ((Holder) args[i]).value);
        _argMarshall[i].serialize(((Holder) args[i]).value, out);
      }
    }

    if (_wrapped)
      out.writeEndElement(); // response name
  }

  private String getResponseName()
  {
    return _method.getName() + "Response";
  }
}
