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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.jws.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.adapters.*;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.*;

import org.w3c.dom.*;

import com.caucho.jaxb.*;
import com.caucho.soap.wsdl.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;
import com.caucho.xpath.*;

/**
 * Invokes a SOAP request on a Java POJO
 */
public class DirectSkeleton extends Skeleton {
  private static final Logger log =
    Logger.getLogger(DirectSkeleton.class.getName());
  public static final L10N L = new L10N(DirectSkeleton.class);

  private JAXBContext _context;
  private Marshaller _marshaller;
  private Node _wsdlNode;

  private HashMap<String,PojoMethodSkeleton> _actionMap
    = new HashMap<String,PojoMethodSkeleton>();

  private Class _api;
  
  private String _namespace;
  private String _name;
  private String _typeName;
  private String _portName;
  private String _serviceName;
  private String _wsdlLocation;

  private final WSDLDefinitions _wsdl = new WSDLDefinitions();
  private final WSDLTypes _wsdlTypes = new WSDLTypes();
  private final WSDLPortType _wsdlPortType = new WSDLPortType();
  private final WSDLBinding _wsdlBinding = new WSDLBinding();
  private final WSDLService _wsdlService = new WSDLService();
  private final SOAPAddress _soapAddress = new SOAPAddress();

  public DirectSkeleton(Class type, String wsdlAddress)
  {
    WebService webService = (WebService)type.getAnnotation(WebService.class);
    setNamespace(type);

    _name = getWebServiceName(type);
    _typeName = _name + "PortType";

    _serviceName = webService!=null && !webService.serviceName().equals("")
      ? webService.serviceName()
      : _name + "HttpBinding";

    _portName =
      webService!=null && !webService.portName().equals("")
      ? webService.portName()
      : _name + "HttpPort";

    _wsdlLocation =
      webService!=null && !webService.wsdlLocation().equals("")
      ? webService.wsdlLocation()
      : null;

    _wsdl.setTargetNamespace(_namespace);

    _wsdl.addDefinition(_wsdlTypes);

    _wsdlPortType.setName(_typeName);
    _wsdl.addDefinition(_wsdlPortType);

    javax.jws.soap.SOAPBinding sbAnnotation = 
      (javax.jws.soap.SOAPBinding) 
      type.getAnnotation(javax.jws.soap.SOAPBinding.class);

    com.caucho.soap.wsdl.SOAPBinding soapBinding = 
      new com.caucho.soap.wsdl.SOAPBinding();
    soapBinding.setTransport("http://schemas.xmlsoap.org/soap/http");

    if (sbAnnotation != null && 
        sbAnnotation.style() == javax.jws.soap.SOAPBinding.Style.RPC)
      soapBinding.setStyle(SOAPStyleChoice.RPC);
    else
      soapBinding.setStyle(SOAPStyleChoice.DOCUMENT);

    if (sbAnnotation != null && 
        sbAnnotation.use() == javax.jws.soap.SOAPBinding.Use.ENCODED)
      throw new WebServiceException(L.l("Encoded SOAP style not supported by JAX-WS"));

    _wsdlBinding.addAny(soapBinding);
    _wsdlBinding.setName(_name + "Binding");
    _wsdlBinding.setType(new QName(_namespace, _typeName, "tns"));

    _wsdl.addDefinition(_wsdlBinding);

    _wsdlService.setName(_serviceName);

    WSDLPort port = new WSDLPort();
    port.setName(_name + "Port");
    port.setBinding(new QName(_namespace, _name + "Binding", "tns"));

    _soapAddress.setLocation(wsdlAddress);
    port.addAny(_soapAddress);

    _wsdlService.addPort(port);

    _wsdl.addDefinition(_wsdlService);
  }

  public String getNamespace()
  {
    return _namespace;
  }

  private void setNamespace(Class type) {
    WebService webService = (WebService)type.getAnnotation(WebService.class);

    if (webService != null && !webService.targetNamespace().equals("")) {
      _namespace = webService.targetNamespace();
    }
    else {
      _namespace = null;
      String packageName = type.getPackage().getName();
      StringTokenizer st = new StringTokenizer(packageName, ".");

      while (st.hasMoreTokens()) { _namespace = st.nextToken() +
          (_namespace == null ? "" : ("."+_namespace));
      }

      _namespace = "http://"+_namespace+"/";

    }
  }

  static String getWebServiceName(Class type) 
  {
    WebService webService = (WebService)type.getAnnotation(WebService.class);

    if (webService != null && !webService.name().equals(""))
      return webService.name();
    else
      return JAXBUtil.classBasename(type);
  }

  public void addAction(String name, PojoMethodSkeleton action)
  {
    _actionMap.put(name, action);

    _wsdl.addDefinition(action.getInputMessage());
    _wsdl.addDefinition(action.getOutputMessage());

    _wsdlPortType.addOperation(action.getOperation());
    _wsdlBinding.addOperation(action.getBindingOperation());
  }

  /**
   * Invokes the request on a remote object using an outbound XML stream.
   */
  public Object invoke(String name, String url, Object[] args)
    throws IOException, XMLStreamException, MalformedURLException
  {
    PojoMethodSkeleton action = _actionMap.get(name);

    if (action != null)
      return action.invoke(name, url, args, _namespace);
    else if ("toString".equals(name))
      return "SoapStub[" + _api.getName() + "]";
    else
      throw new RuntimeException("no such method: " + name);
  }
  
  /**
   * Invokes the request on a local object using an inbound XML stream.
   */
  public void invoke(Object service,
                     XMLStreamReader in,
                     XMLStreamWriter out)
    throws IOException, XMLStreamException
  {
    in.nextTag();

    if (! "Envelope".equals(in.getName().getLocalPart()))
      throw new IOException("expected Envelope at " + in.getName());

    in.nextTag();

    // XXX: Header

    if (! "Body".equals(in.getName().getLocalPart()))
      throw new IOException("expected Body");

    in.nextTag();

    String action = in.getName().getLocalPart();

    out.writeStartDocument();
    out.writeStartElement("env:Envelope");
    out.writeNamespace("env", SOAP_ENVELOPE);
    out.writeNamespace("xsi", XMLNS_XSI);
    out.writeNamespace("xsd", XMLNS_XSD);

    out.writeStartElement("env:Body");

    PojoMethodSkeleton method = _actionMap.get(action);

    // XXX: exceptions<->faults
    if (method != null)
      method.invoke(service, in, out);    
    /*
    else
      // XXX: fault
      out.println("no such action:" + action);*/

    if (in.getEventType() != in.END_ELEMENT)
      throw new IOException("expected </" + action + ">, " + 
                            "not </" + in.getName().getLocalPart() + ">");
    else if (! action.equals(in.getName().getLocalPart()))
      throw new IOException("expected </" + action + ">, " +
                            "not </" + in.getName().getLocalPart() + ">");

    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException("expected </Body>");
    else if (! "Body".equals(in.getName().getLocalPart()))
      throw new IOException("expected </Body>");
    /*
    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException("expected </Envelope>");
    else if (! "Envelope".equals(in.getName().getLocalPart()))
      throw new IOException("expected </Envelope>");
    */

    out.writeEndElement(); // Body
    out.writeEndElement(); // Envelope
  }

  private Node getWSDLNode()
    throws JAXBException
  {
    if (_wsdlNode != null)
      return _wsdlNode;

    if (_context == null)
      _context = JAXBContext.newInstance("com.caucho.soap.wsdl");

    if (_marshaller == null) {
      _marshaller = _context.createMarshaller();

      try {
        _marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        _marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                                new WSDLPrefixMapper());
      } 
      catch(PropertyException e) {
        // Non fatal
        log.finer(L.l("Unable to set prefix mapper"));
      }
    }

    DOMResult result = new DOMResult();
    _marshaller.marshal(_wsdl, result);

    _wsdlNode = result.getNode();

    return _wsdlNode;
  }

  /**
   * Used by WebServiceIntrospector to append the schema for the WSDL.
   */
  public Node getTypesNode()
    throws JAXBException
  {
    Node wsdlNode = getWSDLNode();

    Node definitionsNode = wsdlNode.getFirstChild();

    if (definitionsNode == null ||
        ! "definitions".equals(definitionsNode.getLocalName()))
      throw new JAXBException(L.l("Unable to attach types node"));

    Node typesNode = definitionsNode.getFirstChild();

    if (typesNode == null || ! "types".equals(typesNode.getLocalName()))
      throw new JAXBException(L.l("Unable to attach types node"));

    return typesNode;
  }

  public void dumpWSDL(OutputStream w)
    throws IOException, JAXBException
  {
    XmlPrinter printer = new XmlPrinter(w);
    printer.setPrintDeclaration(true);
    printer.printPrettyXml(getWSDLNode());
  }

  // XXX Remove after switching to Caucho JAXB
  private class WSDLPrefixMapper 
    extends com.sun.xml.bind.marshaller.NamespacePrefixMapper
  {
    public String getPreferredPrefix(String namespaceUri, 
                                     String suggestion, 
                                     boolean requirePrefix) 
    {
      if ("http://schemas.xmlsoap.org/wsdl/".equals(namespaceUri))
        return requirePrefix ? "wsdl" : "";

      if ("http://schemas.xmlsoap.org/wsdl/soap/".equals(namespaceUri))
        return "soap";

      if ("http://www.w3.org/2001/XMLSchema".equals(namespaceUri)) 
        return "xsd";

      if (_namespace.equals(namespaceUri)) 
        return "tns";

      return suggestion;
    }

    public String[] getPreDeclaredNamespaceUris() {
      return new String[] {
        "http://schemas.xmlsoap.org/wsdl/",
        "http://schemas.xmlsoap.org/wsdl/soap/",
        "http://www.w3.org/2001/XMLSchema",
        _namespace
      };
    }
  }
}
