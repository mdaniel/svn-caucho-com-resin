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

import com.caucho.jaxb.*;
import com.caucho.soap.marshall.*;
import com.caucho.soap.wsdl.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Invokes a SOAP request on a Java POJO method
 */
public class PojoMethodSkeleton {
  private final static Logger log = 
    Logger.getLogger(PojoMethodSkeleton.class.getName());
  private static final L10N L = new L10N(PojoMethodSkeleton.class);

  private static final String TARGET_NAMESPACE_PREFIX = "tns";
  protected static final String SOAP_ENCODING_STYLE 
    = "http://schemas.xmlsoap.org/soap/encoding/";

  protected final Method _method;
  protected final int _arity;

  protected String _responseName;
  protected String _operationName;
  protected QName _requestName;
  protected QName _resultName;

  protected final LinkedHashMap<String,ParameterMarshall> _bodyArguments
    = new LinkedHashMap<String,ParameterMarshall>();

  protected final LinkedHashMap<String,ParameterMarshall> _headerArguments
    = new LinkedHashMap<String,ParameterMarshall>();

  protected ParameterMarshall _retMarshal;

  protected int _headerOutputs;
  protected int _bodyOutputs;

  protected final JAXBContextImpl _jaxbContext;

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

  protected PojoMethodSkeleton(Method method, 
                               MarshallFactory factory,
                               JAXBContextImpl jaxbContext, 
                               String targetNamespace)
    throws JAXBException, WebServiceException
  {
    _method = method;
    _arity = _method.getParameterTypes().length;
    _jaxbContext = jaxbContext;

    // set the names for the input/output messages, portType/operation, and
    // binding/operation.
    _operationName = method.getName();
    _responseName = _operationName + "Response";

    if (method.isAnnotationPresent(WebMethod.class)) {
      WebMethod webMethod = (WebMethod) method.getAnnotation(WebMethod.class);

      if (! "".equals(webMethod.operationName()))
        _operationName = webMethod.operationName();
    }

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

  public static PojoMethodSkeleton 
    createMethodSkeleton(Method method, MarshallFactory factory,
                         JAXBContextImpl jaxbContext, String targetNamespace,
                         Map<String,String> elements)
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
    if (soapBinding == null) {
      return new DocumentWrappedAction(method, 
				       factory, 
				       jaxbContext, 
				       targetNamespace,
				       elements);
    }

    if (soapBinding.use() == javax.jws.soap.SOAPBinding.Use.ENCODED)
      throw new UnsupportedOperationException(L.l("SOAP encoded style is not supported by JAX-WS"));

    if (soapBinding.style() == javax.jws.soap.SOAPBinding.Style.DOCUMENT) {
      if (soapBinding.parameterStyle() == 
          javax.jws.soap.SOAPBinding.ParameterStyle.WRAPPED) {
        return new DocumentWrappedPojoMethodSkeleton(method, 
                                                     factory, 
                                                     jaxbContext, 
                                                     targetNamespace,
                                                     elements);
      }
      else {
        return new DocumentBarePojoMethodSkeleton(method, 
                                                  factory, 
                                                  jaxbContext, 
                                                  targetNamespace,
                                                  elements);
      }
    }
    else {
      if (soapBinding.parameterStyle() != 
          javax.jws.soap.SOAPBinding.ParameterStyle.WRAPPED)
        throw new UnsupportedOperationException(L.l("SOAP RPC bare style not supported"));

      return new RpcPojoMethodSkeleton(method, 
                                       factory, 
                                       jaxbContext, 
                                       targetNamespace,
                                       elements);
    }
  }

  /**
   * Client-side invocation.
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
      //
      // Send the request
      //

      httpConnection.setRequestMethod("POST");
      httpConnection.setDoInput(true);
      httpConnection.setDoOutput(true);

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLStreamWriter out =
        outputFactory.createXMLStreamWriter(httpConnection.getOutputStream());

      writeRequest(out, args, name, namespace);
      out.flush();

      //
      // Parse the response
      // 

      if (httpConnection.getResponseCode() != 200)
        return null; // XXX more meaningful error

      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      XMLStreamReader in = 
        inputFactory.createXMLStreamReader(httpConnection.getInputStream());

      Object ret = readResponse(in, args);

      return ret;
    } 
    finally {
      if (httpConnection != null)
        httpConnection.disconnect();
    }
  }

  protected void writeRequest(XMLStreamWriter out, Object []args,
			      String name, String namespace)
    throws IOException, XMLStreamException
  {
    out.writeStartDocument();
    out.writeStartElement("env", "Envelope", Skeleton.SOAP_ENVELOPE);
    out.writeNamespace("env", Skeleton.SOAP_ENVELOPE);

    out.writeStartElement("env", "Header", Skeleton.SOAP_ENVELOPE);

    for (ParameterMarshall marshall : _headerArguments.values()) {
      Object arg = args[marshall._arg];

      if (marshall._mode == WebParam.Mode.IN) {
	marshall.serialize(arg, out);
      }
      else if (marshall._mode == WebParam.Mode.INOUT) {
	Holder holder = (Holder) arg;

	marshall.serialize(holder.value, out);
      }
    }

    out.writeEndElement(); // Header

    out.writeStartElement("env", "Body", Skeleton.SOAP_ENVELOPE);

    out.writeStartElement("m", name, namespace);
    out.writeNamespace("m", namespace);

    for (ParameterMarshall marshall : _bodyArguments.values()) {
      Object arg = args[marshall._arg];

      if (marshall._mode == WebParam.Mode.IN) {
	marshall.serialize(arg, out);
      }
      else if (marshall._mode == WebParam.Mode.INOUT) {
	Holder holder = (Holder) arg;

	marshall.serialize(holder.value, out);
      }
    }

    out.writeEndElement(); // name
    out.writeEndElement(); // Body
    out.writeEndElement(); // Envelope
  }

  protected Object readResponse(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException
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

	ParameterMarshall marshall = _headerArguments.get(tagName);

	if (marshall == null)
	  throw new IOException(L.l("Unknown output in header <{0}>", tagName));

	if (marshall._mode == WebParam.Mode.IN)
	  throw new IOException(L.l("Received value for input parameter <{0}>", tagName));

	Object value = marshall._marshall.deserialize(in);

	if (marshall._arg < 0)
	  ret = value;
	else
	  ((Holder) args[marshall._arg]).value = value;

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

	ParameterMarshall marshall = _headerArguments.get(tagName);

	if (marshall == null)
	  throw new IOException(L.l("Unknown output in header <{0}>", tagName));

	if (marshall._mode == WebParam.Mode.IN)
	  throw new IOException(L.l("Received value for input parameter <{0}>", tagName));

	Object value = marshall._marshall.deserialize(in);

	if (marshall._arg < 0)
	  ret = value;
	else
	  ((Holder) args[marshall._arg]).value = value;

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
    throws IOException, XMLStreamException
  {
    /*
    // We're starting out at the point in the input stream where the 
    // arguments are listed and the point in the output stream where
    // the results are to be written.
    
    Object[] args = new Object[_arity];

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
      out.writeStartElement(_responseName);

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
    */
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

  protected static Class getHolderValueType(Type holder)
  {
    // XXX Generics and arrays
    Type holderParams[] = ((ParameterizedType) holder).getActualTypeArguments();
    return (Class) holderParams[0];
  }

  protected static class ParameterMarshall {
    public int _arg;
    public Marshall _marshall;
    public QName _name;
    public WebParam.Mode _mode = WebParam.Mode.IN;

    public ParameterMarshall(int arg,
			     Marshall marshall,
			     QName name,
                             WebParam.Mode mode) 
    {
      _arg = arg;
      _marshall = marshall;
      _name = name;
      _mode = mode;
    }

    public void serialize(Object o, XMLStreamWriter out)
      throws IOException, XMLStreamException
    {
      _marshall.serialize(out, o, _name);
    }
  }
}
