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
 * @author Scott Ferguson
 */

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.JAXBContextImpl;
import static com.caucho.soap.wsdl.WSDLConstants.*;
import com.caucho.soap.jaxws.HandlerChainInvoker;
import com.caucho.util.L10N;
import com.caucho.xml.XmlPrinter;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Node;
import javax.jws.WebService;
import static javax.xml.XMLConstants.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.namespace.QName;
import static javax.xml.soap.SOAPConstants.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invokes a SOAP request on a Java POJO
 */
public class DirectSkeleton extends Skeleton {
  private static final Logger log =
    Logger.getLogger(DirectSkeleton.class.getName());
  public static final L10N L = new L10N(DirectSkeleton.class);

  private static final String TARGET_NAMESPACE_PREFIX = "m";

  private boolean _separateSchema = true;
  private JAXBContextImpl _context;
  private Marshaller _marshaller;
  private Node _wsdlNode;

  private HashMap<String,AbstractAction> _actionNames
    = new HashMap<String,AbstractAction>();

  private HashMap<Method,AbstractAction> _actionMethods
    = new HashMap<Method,AbstractAction>();

  private Class _api;
  
  private String _namespace;
  private String _portType;
  private String _portName;
  private String _serviceName;
  private String _wsdlLocation = "REPLACE_WITH_ACTUAL_URL";

  // The URI in SOAPBinding is wrong, but matches that of JAVAEE
  private String _soapNamespaceURI = "http://schemas.xmlsoap.org/wsdl/soap/";
  private String _soapTransport = "http://schemas.xmlsoap.org/soap/http";
  private String _soapStyle = "document";

  private CharArrayWriter _wsdlBuffer = new CharArrayWriter();
  private boolean _wsdlGenerated = false;

  private CharArrayWriter _schemaBuffer = new CharArrayWriter();
  private boolean _schemaGenerated = false;

  private static XMLInputFactory _inputFactory;
  private static XMLOutputFactory _outputFactory;

  private static XMLInputFactory getXMLInputFactory()
    throws XMLStreamException
  {
    if (_inputFactory == null)
      _inputFactory = XMLInputFactory.newInstance();

    return _inputFactory;
  }

  private static XMLOutputFactory getXMLOutputFactory()
    throws XMLStreamException
  {
    if (_outputFactory == null) { 
      _outputFactory = XMLOutputFactory.newInstance();
      _outputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                                 Boolean.TRUE);
    }

    return _outputFactory;
  }

  public DirectSkeleton(Class type, JAXBContextImpl context, String wsdlAddress)
    throws WebServiceException
  {
    WebService webService = (WebService) type.getAnnotation(WebService.class);
    _api = type;

    if (webService != null && ! "".equals(webService.endpointInterface())) {
      try {
        ClassLoader loader = type.getClassLoader();
        _api = loader.loadClass(webService.endpointInterface());
      }
      catch (ClassNotFoundException e) {
        throw new WebServiceException(e);
      }
    }

    setNamespace(type, _api);

    _portType = getWebServiceName(type);

    if (webService != null && ! "".equals(webService.portName()))
      _portName = webService.portName();
    else
      _portName = _portType + "Port";

    if (webService != null && ! "".equals(webService.serviceName()))
      _serviceName = webService.serviceName();
    else
      _serviceName = JAXBUtil.classBasename(type) + "Service";

    if (webService != null && ! "".equals(webService.wsdlLocation()))
      _wsdlLocation = webService.wsdlLocation();
    else
      _wsdlLocation = null;

    _context = context;
  }

  public String getPortName()
  {
    return _portName;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  private void setNamespace(Class type, Class api) 
  {
    WebService webService = (WebService) type.getAnnotation(WebService.class);

    // try to get the namespace from the annotation first...
    if (webService != null) {
      if (! "".equals(webService.targetNamespace())) {
        _namespace = webService.targetNamespace();
        return;
      }
      else if (! api.equals(type)) {
        webService = (WebService) api.getAnnotation(WebService.class);

        if (! "".equals(webService.targetNamespace())) {
          _namespace = webService.targetNamespace();
          return;
        }
      }
    }

    // get the namespace from the package name
    _namespace = null;
    String packageName = type.getPackage().getName();
    StringTokenizer st = new StringTokenizer(packageName, ".");

    while (st.hasMoreTokens()) { 
      if (_namespace == null) 
        _namespace = st.nextToken();
      else
        _namespace = st.nextToken() + "." + _namespace;
    }

    _namespace = "http://"+_namespace+"/";
  }

  static String getWebServiceName(Class type) 
  {
    WebService webService = (WebService) type.getAnnotation(WebService.class);

    if (webService != null && !webService.name().equals(""))
      return webService.name();
    else
      return JAXBUtil.classBasename(type);
  }

  public void addAction(Method method, AbstractAction action)
  {
    _actionNames.put(action.getInputName(), action);
    _actionMethods.put(method, action);
  }

  public Object invoke(Method method, String url, Object[] args)
    throws IOException, XMLStreamException, MalformedURLException, 
           JAXBException, Throwable
  {
    return invoke(method, url, args, null);
  }

  /**
   * Invokes the request on a remote object using an outbound XML stream.
   */
  public Object invoke(Method method, String url, Object[] args, 
                       HandlerChainInvoker handlerChain)
    throws IOException, XMLStreamException, MalformedURLException, 
           JAXBException, Throwable
  {
    AbstractAction action = _actionMethods.get(method);

    if (action != null)
      return action.invoke(url, args, handlerChain);
    else if ("toString".equals(method.getName()))
      return "SoapStub[" + (_api != null ? _api.getName() : "") + "]";
    else
      throw new RuntimeException(L.l("not a web method: {0}", method.getName()));
  }
  
  /**
   * Invokes the request on a local object using an inbound XML stream.
   */
  public void invoke(Object service, XMLStreamReader in, XMLStreamWriter out)
    throws IOException, XMLStreamException, Throwable
  {
    in.nextTag();

    // XXX Namespace
    in.require(XMLStreamReader.START_ELEMENT, null, "Envelope");

    in.nextTag();

    XMLStreamReader header = null;

    if ("Header".equals(in.getName().getLocalPart())) {
      in.nextTag();

      XMLOutputFactory outputFactory = getXMLOutputFactory();
      CharArrayWriter writer = new CharArrayWriter();
      StreamResult result = new StreamResult(writer);
      XMLStreamWriter xmlWriter = outputFactory.createXMLStreamWriter(result);

      StaxUtil.copyReaderToWriter(in, xmlWriter);

      CharArrayReader reader = new CharArrayReader(writer.toCharArray());

      XMLInputFactory inputFactory = getXMLInputFactory();
      header = inputFactory.createXMLStreamReader(reader);

      in.nextTag();
    }

    // XXX Namespace?
    in.require(XMLStreamReader.START_ELEMENT, null, "Body");

    in.nextTag();

    String actionName = in.getName().getLocalPart();

    // services/1318: special corner case where no method name is given
    // May happen with Document BARE methods w/no arguments
    if ("Body".equals(actionName) && in.getEventType() == in.END_ELEMENT)
      actionName = "";

    out.writeStartDocument("UTF-8", "1.0");
    out.writeStartElement(SOAP_ENVELOPE_PREFIX, "Envelope", SOAP_ENVELOPE);
    out.writeNamespace(SOAP_ENVELOPE_PREFIX, SOAP_ENVELOPE);
    //out.writeNamespace("xsi", XMLNS_XSI);
    out.writeNamespace("xsd", XMLNS_XSD);

    AbstractAction action = _actionNames.get(actionName);

    // XXX: exceptions<->faults
    if (action != null)
      action.invoke(service, header, in, out);
    else {
      // XXX: fault
    }

    // XXX Namespace?
    in.require(XMLStreamReader.END_ELEMENT, null, "Body");
    in.nextTag();
    in.require(XMLStreamReader.END_ELEMENT, null, "Envelope");

    out.writeEndElement(); // Envelope
  }

  public void setSeparateSchema(boolean separateSchema) 
  {
    if (_separateSchema != separateSchema) {
      _separateSchema = separateSchema;
      _wsdlGenerated = false;
    }
  }

  public void dumpWSDL(OutputStream os)
    throws IOException, XMLStreamException, JAXBException
  {
    OutputStreamWriter out = null;

    try {
      out = new OutputStreamWriter(os);
      dumpWSDL(out);
    }
    finally {
      if (out != null)
        out.close();
    }
  }

  public void dumpWSDL(Writer w)
    throws IOException, XMLStreamException, JAXBException
  {
    generateWSDL();
    _wsdlBuffer.writeTo(w);
  }

  /**
   * To be accurate, all of the actions must have been added before this
   * method is run for the first time.
   **/
  public void generateWSDL()
    throws IOException, XMLStreamException, JAXBException
  {
    if (_wsdlGenerated)
      return;

    // We write to DOM so that we can pretty print it.  Since this only
    // happens once, it's not too much of a burden.
    DOMResult result = new DOMResult();
    XMLOutputFactory factory = getXMLOutputFactory();
    XMLStreamWriter out = factory.createXMLStreamWriter(result);

    out.writeStartDocument("UTF-8", "1.0");

    // <definitions>

    out.setDefaultNamespace(WSDL_NAMESPACE);
    out.writeStartElement(WSDL_NAMESPACE, "definitions");
    out.writeAttribute("targetNamespace", _namespace);
    out.writeAttribute("name", _serviceName);
    out.writeNamespace(TARGET_NAMESPACE_PREFIX, _namespace);
    out.writeNamespace("soap", _soapNamespaceURI);

    // <types>
    
    out.writeStartElement(WSDL_NAMESPACE, "types");

    if (_separateSchema) {
      out.writeStartElement(W3C_XML_SCHEMA_NS_URI, "schema");

      out.writeEmptyElement(W3C_XML_SCHEMA_NS_URI, "import");
      out.writeAttribute("namespace", _namespace);
      out.writeAttribute("schemaLocation",  _serviceName + "_schema1.xsd");

      out.writeEndElement(); // schema 
    }
    else
      writeSchema(out);

    out.writeEndElement(); // types

    // <messages>

    for (AbstractAction action : _actionNames.values())
      action.writeWSDLMessages(out, _soapNamespaceURI);

    // <portType>

    out.writeStartElement(WSDL_NAMESPACE, "portType");
    out.writeAttribute("name", _portType);

    for (AbstractAction action : _actionNames.values())
      action.writeWSDLOperation(out, _soapNamespaceURI);

    out.writeEndElement(); // portType

    // <binding>

    out.writeStartElement(WSDL_NAMESPACE, "binding");
    out.writeAttribute("name", _portName + "Binding");
    out.writeAttribute("type", TARGET_NAMESPACE_PREFIX + ':' + _portType);

    out.writeEmptyElement(_soapNamespaceURI, "binding");
    out.writeAttribute("transport", _soapTransport);
    out.writeAttribute("style", _soapStyle);

    for (AbstractAction action : _actionNames.values())
      action.writeWSDLBindingOperation(out, _soapNamespaceURI);

    out.writeEndElement(); // binding

    // <service>

    out.writeStartElement(WSDL_NAMESPACE, "service");
    out.writeAttribute("name", _serviceName);

    out.writeStartElement(WSDL_NAMESPACE, "port");
    out.writeAttribute("name", _portName);
    out.writeAttribute("binding",
                       TARGET_NAMESPACE_PREFIX + ':' + _portName + "Binding");

    out.writeEmptyElement(_soapNamespaceURI, "address");
    out.writeAttribute("location", _wsdlLocation);

    out.writeEndElement(); // port

    out.writeEndElement(); // service 

    out.writeEndElement(); // definitions

    _wsdlBuffer = new CharArrayWriter();

    XmlPrinter printer = new XmlPrinter(_wsdlBuffer);
    printer.setPrintDeclaration(true);
    printer.setStandalone("true");
    printer.printPrettyXml(result.getNode());
    
    _wsdlGenerated = true;
  }

  public void dumpSchema(OutputStream os)
    throws IOException, XMLStreamException, JAXBException
  {
    OutputStreamWriter out = null;

    try {
      out = new OutputStreamWriter(os);
      dumpSchema(out);
    }
    finally {
      if (out != null)
        out.close();
    }
  }

  public void dumpSchema(Writer w)
    throws IOException, XMLStreamException, JAXBException
  {
    generateSchema();
    _schemaBuffer.writeTo(w);
  }

  public void generateSchema()
    throws IOException, XMLStreamException, JAXBException
  {
    if (_schemaGenerated)
      return;

    // We write to DOM so that we can pretty print it.  Since this only
    // happens once, it's not too much of a burden.
    DOMResult result = new DOMResult();
    XMLOutputFactory factory = getXMLOutputFactory();
    XMLStreamWriter out = factory.createXMLStreamWriter(result);

    out.writeStartDocument("UTF-8", "1.0");

    writeSchema(out);

    _schemaBuffer = new CharArrayWriter();

    XmlPrinter printer = new XmlPrinter(_schemaBuffer);
    printer.setPrintDeclaration(true);
    printer.setStandalone("true");
    printer.printPrettyXml(result.getNode());
    
    _schemaGenerated = true;
  }

  public void writeSchema(XMLStreamWriter out)
    throws XMLStreamException, JAXBException
  {
    out.writeStartElement("xsd", "schema", W3C_XML_SCHEMA_NS_URI);
    out.writeAttribute("version", "1.0");
    out.writeAttribute("targetNamespace", _namespace);
    out.writeNamespace(TARGET_NAMESPACE_PREFIX, _namespace);

    _context.generateSchemaWithoutHeader(out);

    for (AbstractAction action : _actionNames.values())
      action.writeSchema(out, _soapNamespaceURI);

    out.writeEndElement(); // schema

    out.flush();
  }

  /**
   * Dumps a WSDL into the specified directory using the service name
   * annotation if present.  (Mainly for TCK, wsgen)
   */
  public void dumpWSDL(String dir)
    throws IOException, XMLStreamException, JAXBException
  {
    FileWriter wsdlOut = null;
    FileWriter xsdOut = null;
    
    try {
      wsdlOut = new FileWriter(new File(dir, _serviceName + ".wsdl"));
      dumpWSDL(wsdlOut);

      if (_separateSchema) {
        xsdOut = new FileWriter(new File(dir, _serviceName + "_schema1.xsd"));
        dumpSchema(xsdOut);
      }
    }
    finally {
      if (wsdlOut != null)
        wsdlOut.close();

      if (xsdOut != null)
        xsdOut.close();
    }
  }

  public String toString()
  {
    return "DirectSkeleton[" + _api + "]";
  }
}
