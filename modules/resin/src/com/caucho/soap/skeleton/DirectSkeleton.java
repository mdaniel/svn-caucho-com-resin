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
import com.caucho.util.L10N;
import com.caucho.xml.XmlPrinter;

import org.w3c.dom.Node;
import javax.jws.WebService;
import static javax.xml.XMLConstants.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.namespace.QName;
import static javax.xml.soap.SOAPConstants.*;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
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

  private HashMap<String,AbstractAction> _actionMap
    = new HashMap<String,AbstractAction>();

  private Class _api;
  
  private String _namespace;
  private String _name;
  private String _typeName;
  private String _portName;
  private String _portType;
  private String _serviceName;
  private String _wsdlLocation = "REPLACE_WITH_ACTUAL_URL";

  // The URI in SOAPBinding is wrong, but matches that of JAVAEE
  private String _soapNamespaceURI = "http://schemas.xmlsoap.org/wsdl/soap/";
  private String _soapTransport = SOAP11_BINDING_NAMESPACE;
  private String _soapStyle = "document";

  private CharArrayWriter _wsdlBuffer = new CharArrayWriter();
  private boolean _wsdlGenerated = false;

  private CharArrayWriter _schemaBuffer = new CharArrayWriter();
  private boolean _schemaGenerated = false;

  private static XMLOutputFactory _outputFactory;

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
  {
    WebService webService = (WebService) type.getAnnotation(WebService.class);
    setNamespace(type);

    _name = getWebServiceName(type);
    _typeName = _name + "PortType";

    _portType = webService != null && 
                ! webService.endpointInterface().equals("")
                ? JAXBUtil.classBasename(webService.endpointInterface())
                : JAXBUtil.classBasename(type);

    _serviceName = webService != null && ! webService.serviceName().equals("")
      ? webService.serviceName()
      : _name + "HttpBinding";

    _portName =
      webService != null && ! webService.portName().equals("")
      ? webService.portName()
      : _name + "HttpPort";

    _wsdlLocation =
      webService != null && ! webService.wsdlLocation().equals("")
      ? webService.wsdlLocation()
      : null;

    _context = context;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  private void setNamespace(Class type) 
  {
    WebService webService = (WebService) type.getAnnotation(WebService.class);

    if (webService != null && ! webService.targetNamespace().equals(""))
      _namespace = webService.targetNamespace();
    else {
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
  }

  static String getWebServiceName(Class type) 
  {
    WebService webService = (WebService) type.getAnnotation(WebService.class);

    if (webService != null && !webService.name().equals(""))
      return webService.name();
    else
      return JAXBUtil.classBasename(type);
  }

  public void addAction(String name, AbstractAction action)
  {
    _actionMap.put(name, action);
  }

  /**
   * Invokes the request on a remote object using an outbound XML stream.
   */
  public Object invoke(Method method, String url, Object[] args)
    throws IOException, XMLStreamException, MalformedURLException, 
           JAXBException, Throwable
  {
    String actionName = AbstractAction.getWebMethodName(method);
    AbstractAction action = _actionMap.get(actionName);

    if (action != null)
      return action.invoke(url, args);
    else if ("toString".equals(actionName))
      return "SoapStub[" + (_api != null ? _api.getName() : "") + "]";
    else
      throw new RuntimeException("no such method: " + actionName);
  }
  
  /**
   * Invokes the request on a local object using an inbound XML stream.
   */
  public void invoke(Object service, XMLStreamReader in, XMLStreamWriter out)
    throws IOException, XMLStreamException, Throwable
  {
    in.nextTag();

    if (in.getEventType() != XMLStreamReader.START_ELEMENT)
      throw new IOException(L.l("expected start element, not {0}", 
                                in.getEventType()));
    else if (! "Envelope".equals(in.getName().getLocalPart()))
      throw new IOException(L.l("expected Envelope at {0}", in.getName()));

    in.nextTag();

    if ("Header".equals(in.getName().getLocalPart())) {
      int depth = 1;

      while (depth > 0) {
        switch (in.nextTag()) {
          case XMLStreamReader.START_ELEMENT:
            depth++;
            break;
          case XMLStreamReader.END_ELEMENT:
            depth--;
            break;
        }
      }

      in.nextTag();
    }

    if (! "Body".equals(in.getName().getLocalPart()))
      throw new IOException(L.l("expected Body at {0}", in.getName()));

    in.nextTag();

    String actionName = in.getName().getLocalPart();
    in.nextTag();

    out.writeStartDocument();
    out.writeStartElement(SOAP_ENVELOPE_PREFIX, "Envelope", SOAP_ENVELOPE);
    out.writeNamespace(SOAP_ENVELOPE_PREFIX, SOAP_ENVELOPE);
    //out.writeNamespace("xsi", XMLNS_XSI);
    out.writeNamespace("xsd", XMLNS_XSD);

    out.writeStartElement(SOAP_ENVELOPE_PREFIX, "Body", SOAP_ENVELOPE);

    AbstractAction action = _actionMap.get(actionName);

    // XXX: exceptions<->faults
    if (action != null)
      action.invoke(service, in, out);
    else {
      // XXX: fault
    }

    if (in.getEventType() != in.END_ELEMENT)
      throw new IOException("expected </" + actionName + ">, " + 
                            "not event of type " + in.getEventType());
    else if (! actionName.equals(in.getLocalName()))
      throw new IOException("expected </" + actionName + ">, " +
                            "not </" + in.getLocalName() + ">");

    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException("expected </Body>, got: " + in.getName());
    else if (! "Body".equals(in.getLocalName()))
      throw new IOException("expected </Body>, got: " + in.getName());
    /*
    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException("expected </Envelope>");
    else if (! "Envelope".equals(in.getName().getLocalPart()))
      throw new IOException("expected </Envelope>");
    */

    out.writeEndElement(); // Body
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
    out.writeAttribute("name", _name);
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

    for (AbstractAction action : _actionMap.values())
      action.writeWSDLMessages(out, _soapNamespaceURI);

    // <portType>

    out.writeStartElement(WSDL_NAMESPACE, "portType");
    out.writeAttribute("name", _portType);

    for (AbstractAction action : _actionMap.values())
      action.writeWSDLOperation(out, _soapNamespaceURI);

    out.writeEndElement(); // portType

    // <binding>

    out.writeStartElement(WSDL_NAMESPACE, "binding");
    out.writeAttribute("name", _portName + "Binding");
    out.writeAttribute("type", TARGET_NAMESPACE_PREFIX + ':' + _portType);

    out.writeEmptyElement(_soapNamespaceURI, "binding");
    out.writeAttribute("transport", _soapTransport);
    out.writeAttribute("style", _soapStyle);

    for (AbstractAction action : _actionMap.values())
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

    for (AbstractAction action : _actionMap.values())
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
}
