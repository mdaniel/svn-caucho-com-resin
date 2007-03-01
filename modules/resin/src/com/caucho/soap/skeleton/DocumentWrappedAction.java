/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.skeleton.Property;

import com.caucho.util.L10N;

import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Document wrapped action
 */
public class DocumentWrappedAction extends AbstractAction {
  private final static Logger log = 
    Logger.getLogger(DocumentWrappedAction.class.getName());
  public static final L10N L = new L10N(DocumentWrappedAction.class);

  /*
   * Document wrapped arguments are in an xsd:sequence -- in other words, 
   * there is a prescribed order in which the arguments are expected to be
   * given.  (Any arguments from the header are excepted; headers are 
   * key/value pairs.)  
   *
   */

  public DocumentWrappedAction(Method method, 
                               JAXBContextImpl jaxbContext, 
                               String targetNamespace,
                               Marshaller marshaller,
                               Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    super(method, jaxbContext, targetNamespace, marshaller, unmarshaller);

    _isOneway = (method.getAnnotation(Oneway.class) != null);

    Class[] params = method.getParameterTypes();
    Type[] genericParams = method.getGenericParameterTypes();
    Annotation[][] paramAnn = method.getParameterAnnotations();

    ArrayList<ParameterMarshal> headerList = new ArrayList<ParameterMarshal>();
    ArrayList<ParameterMarshal> bodyList = new ArrayList<ParameterMarshal>();
    
    for (int i = 0; i < params.length; i++) {
      boolean isHeader = false;

      String localName = "arg" + i; // As per JAX-WS spec

      QName name = null;
      WebParam.Mode mode = WebParam.Mode.IN;

      for (Annotation ann : paramAnn[i]) {
        if (ann instanceof WebParam) {
          WebParam webParam = (WebParam) ann;

          if (! "".equals(webParam.name()))
            localName = webParam.name();

          if ("".equals(webParam.targetNamespace()))
            name = new QName(localName);
          else 
            name = new QName(localName, webParam.targetNamespace());

          if (params[i].equals(Holder.class)) {
            mode = webParam.mode();

            if (_isOneway) {
              throw new WebServiceException(L.l("Method {0} annotated with @Oneway, but contains output argument", method.getName()));
            }
          }
        }
      }

      if (name == null) 
        name = new QName(localName);

      Type type = JAXBUtil.getActualParameterType(genericParams[i]);
      Property property = _jaxbContext.createProperty(type);

      ParameterMarshal pMarshal
        = ParameterMarshal.create(i, property, name, mode,
                                  marshaller, unmarshaller);

      if (isHeader) {
        if (pMarshal instanceof InParameterMarshal)
          _headerInputs++;
        else if (pMarshal instanceof OutParameterMarshal)
          _headerOutputs++;
        else {
          _headerInputs++;
          _headerOutputs++;
        }

        headerList.add(pMarshal);
        _headerArguments.put(localName, pMarshal);
      }
      else {
        if (pMarshal instanceof InParameterMarshal)
          _bodyInputs++;
        else if (pMarshal instanceof OutParameterMarshal)
          _bodyOutputs++;
        else {
          _bodyInputs++;
          _bodyOutputs++;
        }

        bodyList.add(pMarshal);
        _bodyArguments.put(localName, pMarshal);
      }
    }

    _bodyArgs = new ParameterMarshal[bodyList.size()];
    bodyList.toArray(_bodyArgs);

    if (! Void.TYPE.equals(method.getReturnType())) {
      if (_isOneway)
        throw new WebServiceException(L.l("Method {0} annotated with @Oneway, but has non-void return", method.getName()));

      Property property = 
        _jaxbContext.createProperty(method.getGenericReturnType());

      if (method.isAnnotationPresent(WebResult.class)) {
        WebResult webResult = (WebResult) method.getAnnotation(WebResult.class);

        String localName = webResult.name();

        if ("".equals(localName))
          localName = "return";

        _resultName = new QName(webResult.targetNamespace(), localName);
      }
      else
        _resultName = new QName("return");

      _returnMarshal = ParameterMarshal.create(0, property, _resultName, 
                                               WebParam.Mode.OUT,
                                               marshaller, unmarshaller);

      // XXX header return?
      _bodyOutputs++;
    }
  }

  /**
   * Invokes the request for a call.
   */
  public void invoke(Object service, XMLStreamReader in, XMLStreamWriter out)
    throws IOException, XMLStreamException, Throwable
  {
    // We're starting out at the point in the input stream where the 
    // arguments are listed and the point in the output stream where
    // the results are to be written.
    
    Object[] args = new Object[_arity];

    // document wrapped => everything must be in order
    for (int i = 0; i < _bodyArgs.length; i++) {
      // while loop for arrays/lists
      while (in.getEventType() == in.START_ELEMENT &&
             _bodyArgs[i].getName().equals(in.getName()))
        _bodyArgs[i].deserializeCall(in, args);
    }

    Object value = null;

    try {
      value = _method.invoke(service, args);
    } 
    catch (IllegalAccessException e) {
      throw new Throwable(e);
    } 
    catch (IllegalArgumentException e) {
      throw new Throwable(e);
    }
    catch (InvocationTargetException e) {
      writeFault(out, e.getCause());
      return;
    }

    if (! _isOneway) {
      out.writeStartElement(TARGET_NAMESPACE_PREFIX, 
                            _responseName, 
                            _targetNamespace);
      out.writeNamespace(TARGET_NAMESPACE_PREFIX, _targetNamespace);

      if (_returnMarshal != null)
        _returnMarshal.serializeReply(out, value);

      for (int i = 0; i < _bodyArgs.length; i++)
        _bodyArgs[i].serializeReply(out, args);

      out.writeEndElement(); // response name
    }
  }

  protected Object readResponse(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException, Throwable
  {
    Object ret = null;

    if (in.nextTag() != XMLStreamReader.START_ELEMENT
        || ! "Envelope".equals(in.getLocalName()))
      throw expectStart("Envelope", in);

    if (in.nextTag() != XMLStreamReader.START_ELEMENT)
      throw expectStart("Header", in);

    if ("Header".equals(in.getLocalName())) {
      while (in.nextTag() == XMLStreamReader.START_ELEMENT) {
        String tagName = in.getLocalName();

        ParameterMarshal marshal = _headerArguments.get(tagName);

        if (marshal != null)
          marshal.deserializeReply(in, args);
        else {
          int depth = 1;

          while (depth > 0) {
            switch (in.nextTag()) {
              case XMLStreamReader.START_ELEMENT:
                depth++;
                break;
              case XMLStreamReader.END_ELEMENT:
                depth--;
                break;
              default:
                throw new IOException("expected </Header>");
            }
          }
        }
      }

      if (! "Header".equals(in.getLocalName()))
        throw expectEnd("Header", in);

      if (in.nextTag() != XMLStreamReader.START_ELEMENT)
        throw expectStart("Body", in);
    }

    if (! "Body".equals(in.getLocalName()))
      throw expectStart("Body", in);

    in.nextTag();

    if (in.getEventType() == XMLStreamReader.START_ELEMENT &&
        "Fault".equals(in.getLocalName())) {
      Throwable fault = readFault(in);

      if (fault == null)
        throw new WebServiceException(); // XXX

      throw fault;
    }

    if (in.getEventType() != XMLStreamReader.START_ELEMENT &&
        ! _responseName.equals(in.getLocalName()))
      throw expectStart(_responseName, in);

    in.nextTag();

    if (_returnMarshal != null) {
      // while loop for arrays/lists
      while (in.getEventType() == in.START_ELEMENT &&
             _returnMarshal.getName().equals(in.getName()))
        ret = _returnMarshal.deserializeReply(in, ret);
    }

    // document wrapped => everything must be in order
    for (int i = 0; i < _bodyArgs.length; i++) {
      // while loop for arrays/lists
      while (in.getEventType() == in.START_ELEMENT &&
             _bodyArgs[i].getName().equals(in.getName()))
        _bodyArgs[i].deserializeReply(in, args);
    }

    if (in.getEventType() != XMLStreamReader.END_ELEMENT &&
        ! _responseName.equals(in.getLocalName()))
      throw expectEnd(_responseName, in);

    if (in.nextTag() != XMLStreamReader.END_ELEMENT &&
        ! "Body".equals(in.getLocalName()))
      throw expectEnd("Body", in);

    if (in.nextTag() != in.END_ELEMENT)
      throw expectEnd("Envelope", in);

    return ret;
  }

  private IOException expectStart(String expect, XMLStreamReader in)
  {
    return new IOException("expected <" + expect + "> at " + in.getName());
  }

  private IOException expectEnd(String expect, XMLStreamReader in)
  {
    return new IOException("expected </" + expect + "> at " + in.getName());
  }
}
