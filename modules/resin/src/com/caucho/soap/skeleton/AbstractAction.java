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

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.skeleton.Property;

import com.caucho.soap.wsdl.SOAPOperation;
import com.caucho.soap.wsdl.WSDLBindingOperation;
import com.caucho.soap.wsdl.WSDLBindingOperationMessage;
import com.caucho.soap.wsdl.WSDLMessage;
import com.caucho.soap.wsdl.WSDLOperation;
import com.caucho.soap.wsdl.WSDLOperationFault;
import com.caucho.util.L10N;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Invokes a SOAP request on a Java POJO method
 */
public abstract class AbstractAction {
  private final static Logger log = 
    Logger.getLogger(AbstractAction.class.getName());
  private static final L10N L = new L10N(AbstractAction.class);

  private static final String TARGET_NAMESPACE_PREFIX = "tns";
  protected static final String SOAP_ENCODING_STYLE 
    = "http://schemas.xmlsoap.org/soap/encoding/";

  protected final XMLOutputFactory _xmlOutputFactory 
    = XMLOutputFactory.newInstance();
  protected final XMLInputFactory _xmlInputFactory
    = XMLInputFactory.newInstance();

  protected final Method _method;
  protected final int _arity;

  protected String _responseName;
  protected String _operationName;
  protected QName _requestName;
  protected QName _resultName;

  // XXX: add array for efficiency
  protected final LinkedHashMap<String,ParameterMarshal> _bodyArguments
    = new LinkedHashMap<String,ParameterMarshal>();
  protected ParameterMarshal[] _bodyArgs;

  protected final LinkedHashMap<String,ParameterMarshal> _headerArguments
    = new LinkedHashMap<String,ParameterMarshal>();

  protected ParameterMarshal _retMarshal;

  protected int _headerOutputs;
  protected int _bodyOutputs;

  protected final JAXBContextImpl _jaxbContext;
  protected final String _targetNamespace;

  // 
  // WSDL Constructs
  //
  
  protected final WSDLMessage _inputMessage = new WSDLMessage();
  protected final WSDLMessage _outputMessage = new WSDLMessage();
  protected final ArrayList<WSDLMessage> _faultMessages 
    = new ArrayList<WSDLMessage>();

  protected final WSDLOperation _wsdlOperation = new WSDLOperation();
  protected final ArrayList<WSDLOperationFault> _wsdlFaults 
    = new ArrayList<WSDLOperationFault>();

  protected final WSDLBindingOperation _wsdlBindingOperation 
    = new WSDLBindingOperation();

  protected AbstractAction(Method method, JAXBContextImpl jaxbContext, 
                           String targetNamespace)
    throws JAXBException, WebServiceException
  {
    _method = method;
    _arity = _method.getParameterTypes().length;
    _jaxbContext = jaxbContext;
    _targetNamespace = targetNamespace;  // XXX introspect this from the method

    // set the names for the input/output messages, portType/operation, and
    // binding/operation.
    _operationName = getWebMethodName(method);
    _responseName = _operationName + "Response";

      _inputMessage.setName(_operationName);
    _outputMessage.setName(_responseName);

    _wsdlOperation.setName(_operationName);
    _wsdlBindingOperation.setName(_operationName);

    // initialize the binding operation

    _wsdlBindingOperation.setInput(new WSDLBindingOperationMessage());
    _wsdlBindingOperation.setOutput(new WSDLBindingOperationMessage());

    // SOAP action (URI where SOAP messages are sent)
    SOAPOperation soapOperation = new SOAPOperation();
    soapOperation.setSoapAction(""); // XXX
    _wsdlBindingOperation.addAny(soapOperation);
  }

  public static AbstractAction createAction(Method method, 
                                            JAXBContextImpl jaxbContext, 
                                            String targetNamespace,
                                            Marshaller marshaller,
                                            Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    // There are three valid modes in JAX-WS:
    //
    //  1. Document wrapped -- all the parameters and return values 
    //  are encapsulated in a single encoded object (i.e. the document).  
    //  This is selected by
    //    javax.jws.soap.SOAPBinding.style() == DOCUMENT
    //    javax.jws.soap.SOAPBinding.use() == LITERAL
    //    javax.jws.soap.SOAPBinding.parameterStyle() == WRAPPED
    //
    //  2. Document bare -- the method must have at most one input and
    //  one output parameter.  No wrapper objects are created.
    //  This is selected by
    //    javax.jws.soap.SOAPBinding.style() == DOCUMENT
    //    javax.jws.soap.SOAPBinding.use() == LITERAL
    //    javax.jws.soap.SOAPBinding.parameterStyle() == BARE
    //
    //  3. RPC style -- parameters and return values are mapped to
    //  wsdl:parts.  This is selected by:
    //    javax.jws.soap.SOAPBinding.style() == RPC
    //    javax.jws.soap.SOAPBinding.use() == LITERAL
    //    javax.jws.soap.SOAPBinding.parameterStyle() == WRAPPED
    //
    // It seems that "use" is never ENCODED in JAX-WS and is not allowed
    // by WS-I, so we don't allow it either.
    //

    Class cl = method.getDeclaringClass();
    javax.jws.soap.SOAPBinding soapBinding = null;

    if (cl.isAnnotationPresent(javax.jws.soap.SOAPBinding.class)) {
      soapBinding = (javax.jws.soap.SOAPBinding)
                    cl.getAnnotation(javax.jws.soap.SOAPBinding.class);
    }
    
    if (method.isAnnotationPresent(javax.jws.soap.SOAPBinding.class)) {
      soapBinding = (javax.jws.soap.SOAPBinding)
                    method.getAnnotation(javax.jws.soap.SOAPBinding.class);
    }

    // Document wrapped is the default for methods w/o a @SOAPBinding
    if (soapBinding == null)
      return new DocumentWrappedAction(method, jaxbContext, targetNamespace,
                                       marshaller, unmarshaller);

    if (soapBinding.use() == javax.jws.soap.SOAPBinding.Use.ENCODED)
      throw new UnsupportedOperationException(L.l("SOAP encoded style is not supported by JAX-WS"));

    if (soapBinding.style() == javax.jws.soap.SOAPBinding.Style.DOCUMENT) {
      if (soapBinding.parameterStyle() == 
          javax.jws.soap.SOAPBinding.ParameterStyle.WRAPPED)
        return new DocumentWrappedAction(method, jaxbContext, targetNamespace,
                                         marshaller, unmarshaller);
      else {
        return new DocumentBareAction(method, jaxbContext, targetNamespace,
                                      marshaller, unmarshaller);
      }
    }
    else {
      if (soapBinding.parameterStyle() != 
          javax.jws.soap.SOAPBinding.ParameterStyle.WRAPPED)
        throw new UnsupportedOperationException(L.l("SOAP RPC bare style not supported"));

      return new RpcAction(method, jaxbContext, targetNamespace,
                           marshaller, unmarshaller);
    }
  }

  /**
   * Client-side invocation.
   */
  public Object invoke(String url, Object[] args)
    throws IOException, XMLStreamException, MalformedURLException, JAXBException
  {
    URL urlObject = new URL(url);
    URLConnection connection = urlObject.openConnection();

    // XXX HTTPS
    if (! (connection instanceof HttpURLConnection))
      return null;

    HttpURLConnection httpConnection = (HttpURLConnection) connection;

    try {
      //
      // Send the request
      //

      httpConnection.setRequestMethod("POST");
      httpConnection.setDoInput(true);
      httpConnection.setDoOutput(true);

      OutputStream httpOut = httpConnection.getOutputStream();
      XMLStreamWriter out = _xmlOutputFactory.createXMLStreamWriter(httpOut);

      writeRequest(out, args);
      out.flush();

      //
      // Parse the response
      // 

      if (httpConnection.getResponseCode() != 200)
        return null; // XXX more meaningful error

      InputStream httpIn = httpConnection.getInputStream();
      XMLStreamReader in = _xmlInputFactory.createXMLStreamReader(httpIn);

      Object ret = readResponse(in, args);

      return ret;
    } 
    finally {
      if (httpConnection != null)
        httpConnection.disconnect();
    }
  }

  protected void writeRequest(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    out.writeStartDocument();
    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Envelope", 
                          Skeleton.SOAP_ENVELOPE);
    out.writeNamespace(Skeleton.SOAP_ENVELOPE_PREFIX, Skeleton.SOAP_ENVELOPE);

    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Header", 
                          Skeleton.SOAP_ENVELOPE);

    for (ParameterMarshal marshal : _headerArguments.values())
      marshal.serializeCall(out, args);

    out.writeEndElement(); // Header

    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Body", 
                          Skeleton.SOAP_ENVELOPE);

    out.writeStartElement(TARGET_NAMESPACE_PREFIX, 
                          _operationName, 
                          _targetNamespace);
    out.writeNamespace(TARGET_NAMESPACE_PREFIX, 
                       _targetNamespace);

    for (int i = 0; i < _bodyArgs.length; i++)
      _bodyArgs[i].serializeCall(out, args);

    out.writeEndElement(); // name

    out.writeEndElement(); // Body
    out.writeEndElement(); // Envelope
  }

  protected Object readResponse(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    Object ret = null;

    in.nextTag();

    if (! "Envelope".equals(in.getName().getLocalPart()))
      throw new IOException("expected Envelope at " + in.getName());

    // Header
    if (_headerOutputs > 0) {
      in.nextTag();

      if (! "Header".equals(in.getName().getLocalPart()))
        throw new IOException("expected <Header>");

      for (int i = 0; i < _headerOutputs; i++) {
        String tagName = in.getLocalName();

        ParameterMarshal marshal = _headerArguments.get(tagName);

        if (marshal == null)
          throw new IOException(L.l("Unknown output in header <{0}>", tagName));

        Object value = marshal.deserializeReply(in);

        if (marshal.getArg() < 0)
          ret = value;
        else
          ((Holder) args[marshal.getArg()]).value = value;

        if (i + 1 < _headerOutputs)
          in.nextTag();
      }

      if (in.nextTag() != in.END_ELEMENT)
        throw new IOException("expected </Header>");
    }

    // Body is manditory
    in.nextTag();
    if (! "Body".equals(in.getName().getLocalPart()))
      throw new IOException("expected Body");

    // Body
    if (_bodyOutputs > 0) {
      for (int i = 0; i < _headerOutputs; i++) {
        String tagName = in.getLocalName();

        ParameterMarshal marshal = _headerArguments.get(tagName);

        if (marshal == null)
          throw new IOException(L.l("Unknown output in header <{0}>", tagName));

        Object value = marshal.deserializeReply(in);

        if (marshal._arg < 0)
          ret = value;
        else
          ((Holder) args[marshal.getArg()]).value = value;

        if (i + 1 < _headerOutputs)
          in.nextTag();
      }
    }

    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException(L.l("expected </Body> at <{0}>",
            in.getName().getLocalPart()));


    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException(L.l("expected </Envelope> at {0}",
            in.getName().getLocalPart()));

    return ret;
  }

  /**
   * Invokes the request for a call.
   */
  public void invoke(Object service, XMLStreamReader in, XMLStreamWriter out)
    throws IOException, XMLStreamException, Throwable
  {
  }

  /**
   * returns the WSDLMessage for the input of this method.
   */
  public WSDLMessage getInputMessage()
  {
    return _inputMessage;
  }

  /**
   * returns the WSDLMessage for the output of this method.
   */
  public WSDLMessage getOutputMessage()
  {
    return _outputMessage;
  }

  public WSDLOperation getOperation()
  {
    return _wsdlOperation;
  }

  public WSDLBindingOperation getBindingOperation()
  {
    return _wsdlBindingOperation;
  }

  public boolean hasHeaderInput()
  {
    return false;
  }

  public int getArity()
  {
    return _arity;
  }

  protected static Class getHolderValueType(Type holder)
  {
    // XXX Generics and arrays
    Type holderParams[] = ((ParameterizedType) holder).getActualTypeArguments();
    return (Class) holderParams[0];
  }

  public static String getWebMethodName(Method method)
  {
    String name = method.getName();

    WebMethod webMethod = (WebMethod) method.getAnnotation(WebMethod.class);

    if (webMethod != null && ! "".equals(webMethod.operationName()))
      name = webMethod.operationName();
    
    return name;
  }
}
