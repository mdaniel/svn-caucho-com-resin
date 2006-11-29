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

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.soap.marshall.Marshall;
import com.caucho.soap.marshall.MarshallException;
import com.caucho.soap.marshall.MarshallFactory;
import com.caucho.util.L10N;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.bind.JAXBException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Document wrapped action
 */
public class DocumentWrappedAction extends PojoMethodSkeleton {
  private final static Logger log = 
    Logger.getLogger(DocumentWrappedAction.class.getName());
  public static final L10N L = new L10N(DocumentWrappedAction.class);

  private static final String TARGET_NAMESPACE_PREFIX = "tns";

  private HashMap<String,ParameterMarshal> _headerMap
    = new HashMap<String,ParameterMarshal>();

  private ParameterMarshal []_headerArgs;
  
  private HashMap<String,ParameterMarshal> _bodyMap
    = new HashMap<String,ParameterMarshal>();

  private ParameterMarshal []_bodyArgs;

  private Marshall _retMarshal;

  private boolean _isWrapped;

  public DocumentWrappedAction(Method method, 
			       MarshallFactory factory,
			       JAXBContextImpl jaxbContext, 
			       String targetNamespace,
			       Map<String,String> elements)
    throws JAXBException, WebServiceException
  {
    super(method, factory, jaxbContext, targetNamespace);

    _isWrapped = true;
    
    Class []params = method.getParameterTypes();
    Annotation [][]paramAnn = method.getParameterAnnotations();

    //
    // Create wrappers for input and output parameters
    //

    ArrayList<ParameterMarshal> headerList
      = new ArrayList<ParameterMarshal>();

    ArrayList<ParameterMarshal> bodyList
      = new ArrayList<ParameterMarshal>();
    
    for (int i = 0; i < params.length; i++) {
      boolean isInput = true;
      boolean isHeader = false;

      String localName = "arg" + i; // As per JAX-WS spec

      QName name = new QName(localName);
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

            if (mode == WebParam.Mode.OUT)
              isInput = false;
          }
        }
      }

      Marshall marshal = factory.createDeserializer(params[i],
						    _jaxbContext);

      ParameterMarshal pMarshal
	= ParameterMarshal.create(i, marshal, name, mode);

      if (isHeader) {
	headerList.add(pMarshal);
	_headerMap.put(localName, pMarshal);
      }
      else {
	bodyList.add(pMarshal);
	_bodyMap.put(localName, pMarshal);
      }
    }

    _headerArgs = new ParameterMarshal[headerList.size()];
    headerList.toArray(_headerArgs);
    
    _bodyArgs = new ParameterMarshal[bodyList.size()];
    bodyList.toArray(_bodyArgs);
    
    // XXX Can input/output messages have no parts?

    _retMarshal = factory.createSerializer(method.getReturnType(),
					   _jaxbContext);

    if (method.isAnnotationPresent(WebResult.class))
      _resultName =
        new QName(method.getAnnotation(WebResult.class).targetNamespace(),
                  method.getAnnotation(WebResult.class).name());
    else
      _resultName = new QName("return");

    //
    // Exceptions -> Faults
    //
    
    // XXX
    /*Class[] exceptions = getExceptionTypes();

    for (Class exception : exceptions)
      exceptionToFault(exception);
      */
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
    
    Object []args = new Object[_arity];

    while (in.nextTag() == in.START_ELEMENT) {
      String tagName = in.getLocalName();

      ParameterMarshal marshal = _bodyMap.get(tagName);

      if (marshal == null)
        throw new IOException("Unknown parameter <" + tagName + "/>"); // ???

      marshal.deserializeCall(in, args);
    }

    for (int i = 0; i < _bodyArgs.length; i++) {
      _bodyArgs[i].deserializeCallDefault(args);
    }

    Object value = null;

    try {
      value = _method.invoke(service, args);
    } catch (IllegalAccessException e) {
      throw new MarshallException(e);
    } catch (InvocationTargetException e) {
      throw new MarshallException(e.getCause());
    }

    if (_isWrapped)
      out.writeStartElement(_responseName);

    if (_retMarshal != null)
      _retMarshal.serialize(out, value, _resultName);

    for (int i = 0; i < _bodyArgs.length; i++) {
      _bodyArgs[i].serializeReply(out, args);
    }

    if (_isWrapped)
      out.writeEndElement(); // response name
  }

  protected void writeRequest(XMLStreamWriter out, Object []args,
			      String name, String namespace)
    throws IOException, XMLStreamException
  {
    out.writeStartDocument();
    out.writeStartElement("env", "Envelope", Skeleton.SOAP_ENVELOPE);
    out.writeNamespace("env", Skeleton.SOAP_ENVELOPE);

    out.writeStartElement("env", "Header", Skeleton.SOAP_ENVELOPE);

    for (int i = 0; i < _headerArgs.length; i++) {
      ParameterMarshal marshal = _headerArgs[i];

      marshal.serializeCall(out, args);
    }

    out.writeEndElement(); // Header

    out.writeStartElement("env", "Body", Skeleton.SOAP_ENVELOPE);

    out.writeStartElement("m", name, namespace);
    out.writeNamespace("m", namespace);

    for (int i = 0; i < _bodyArgs.length; i++) {
      ParameterMarshal marshal = _bodyArgs[i];

      marshal.serializeCall(out, args);
    }

    out.writeEndElement(); // name
    out.writeEndElement(); // Body
    out.writeEndElement(); // Envelope
  }

  protected Object readResponse(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException
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

	ParameterMarshal marshal = _headerMap.get(tagName);

	if (marshal != null) {
	  marshal.deserializeReply(in, args);
	}
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

    while (in.nextTag() == XMLStreamReader.START_ELEMENT) {
      String tagName = in.getLocalName();
      ParameterMarshal marshal = _bodyMap.get(tagName);

      if (marshal != null) {
	marshal.deserializeReply(in, args);
      }
      else {
	ret = _retMarshal.deserialize(in);
      }
    }

    if (! "Body".equals(in.getLocalName()))
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
