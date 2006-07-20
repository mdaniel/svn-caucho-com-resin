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
import java.util.*;

import javax.xml.stream.*;

import com.caucho.vfs.*;
import javax.jws.*;

/**
 * Invokes a SOAP request on a Java POJO
 */
public class DirectSkeleton extends Skeleton {

  private HashMap<String,PojoMethodSkeleton> _actionMap
    = new HashMap<String,PojoMethodSkeleton>();

  private String _namespace;
  private String _name;
  private String _typeName;
  private String _portName;
  private String _serviceName;
  private String _wsdlLocation;

  public DirectSkeleton(Class type) {

    WebService webService = (WebService)type.getAnnotation(WebService.class);
    setNamespace(type);
    setName(type);

    _typeName    = _name + "PortType";

    _serviceName =
      webService!=null && !webService.serviceName().equals("")
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
      while(st.hasMoreTokens())
        _namespace = st.nextToken() +
          (_namespace==null ? "" : ("."+_namespace));

      _namespace = "http://"+_namespace+"/";

    }
  }

  private void setName(Class type) {
    WebService webService = (WebService)type.getAnnotation(WebService.class);
    if (webService != null && !webService.name().equals("")) {
      _name = webService.name();
    }
    else {
      _name = type.getClass().getName();
    }
  }

  public void addAction(String name, PojoMethodSkeleton action)
  {
    _actionMap.put(name, action);
  }

  /**
   * Invokes the request on a remote object using an outbound XML stream.
   */
  public Object invoke(String name,
                       XMLStreamReader in,
                       WriteStream out,
                       Object[] args)
    throws IOException, XMLStreamException
  {
    return _actionMap.get(name).invoke(name, in, out, args, _namespace);
  }
  
  /**
   * Invokes the request on a local object using an inbound XML stream.
   */
  public void invoke(Object service,
                     XMLStreamReader in,
                     WriteStream out)
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

    out.println("<?xml version=\"1.0\"?>");
    out.print("<env:Envelope xmlns:env=\"" + SOAP_ENVELOPE + "\"");
    out.print(" xmlns:xsi=\"" + XMLNS_XSI + "\"");
    out.print(" xmlns:xsd=\"" + XMLNS_XSD + "\">");
    out.println("<env:Body>");

    PojoMethodSkeleton method = _actionMap.get(action);

    // XXX: exceptions<->faults
    if (method != null)
      method.invoke(service, in, out);    
    else
      // XXX: fault
      out.println("no such action:" + action);

    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException("expected </" + action + ">");
    else if (! action.equals(in.getName().getLocalPart()))
      throw new IOException("expected </" + action + ">");

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
    out.print("\n</env:Body></env:Envelope>");
  }

  public void dumpWSDL(WriteStream w, String address)
    throws IOException {
    w.println("<?xml version='1.0'?>");
    w.println("<definitions name='StockQuote'");
    w.println("  targetNamespace='"+_namespace+"'");
    w.println("  xmlns:tns='"+_namespace+"'");
    w.println("  xmlns:soap='http://schemas.xmlsoap.org/wsdl/soap/'");
    w.println("  xmlns='http://schemas.xmlsoap.org/wsdl/'>");
    w.println("<wsdl:portType name='"+_typeName+"' />");
    w.println("<wsdl:binding name='"+_serviceName+
              "' type='tns:"+_typeName+"'>");
    w.println("<wsdlsoap:binding style='document' "+
              "transport='http://schemas.xmlsoap.org/soap/http' />");
    w.println("</wsdl:binding>");
    w.println("<wsdl:service name='"+_serviceName+"'>");
    w.println("<wsdl:port name='"+_portName+"' binding='tns:"+
              _serviceName+"'>");
    w.println("<wsdlsoap:address location='"+
              (_wsdlLocation == null ? address : _wsdlLocation)+
              "' />");
    w.println("</wsdl:port>");
    w.println("</wsdl:service>");
    w.println("</wsdl:definitions>");
    w.flush();
  }
}
